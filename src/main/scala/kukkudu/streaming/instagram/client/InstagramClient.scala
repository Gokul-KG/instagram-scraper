package kukkudu.streaming.instagram.client

import java.net.URLEncoder
import java.util.regex.Pattern

import org.jsoup.nodes.Document
import com.fasterxml.jackson.databind.JsonNode
import kukkudu.streaming.instagram.models._
import kukkudu.streaming.instagram.models.Implicits._
import kukkudu.streaming.util.JSONHelper

import scala.util.Try

case class InstagramClient(
                            settings: ScraperSettings
                          )(private var browser: HttpClient) extends  Serializable {

  import InstagramClient._

  private val parser                 = IGv1Parser()
  private var loggedInUserId: String = _
  private var token         : String = _

  def getLoggedInUserId = loggedInUserId

  val defaultHeaders = Map(
    "User-Agent" -> "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.94 Safari/537.36"
  )

  def checkLoginState(document: Document) = browser.hasCookie("sessionid")

  def login(email: String, password: String): Boolean = {

    def processLogin(email: String, password: String, d: Document): Boolean = {

      val headers = defaultHeaders ++ Map(
        "x-csrftoken" -> browser.getCookie("csrftoken"),
        "x-instagram-ajax" -> "1",
        "x-requested-with" -> "XMLHttpRequest",
        "origin" -> "https://www.instagram.com",
        "referer" -> "https://www.instagram.com/",
        "content-type" -> "application/x-www-form-urlencoded"
      )

      val posts = Map(
        "username" -> email,
        "password" -> password,
        "next" -> "%2F")

      val resp = browser.post(LOGIN_URL, headers, null, posts)
      println(s"Code: ${resp.code} Body: ${resp.body}")
      resp.code == 200 && checkLoginState(null)
    }

    val document:Document = browser.get(BASE_URL, defaultHeaders, null, null).asDocument(BASE_URL)
    if (!checkLoginState(document)) {
      processLogin(email, password, document)
    } else true
  }

  def getProfileByUserName(userName: String) = {
    getProfile(s"${BASE_URL}$userName")
  }

  def getProfile(profileUrl: String): Option[Profile] = {
     val html = browser.get(url = profileUrl, headers = defaultHeaders, proxy = Some(settings.proxies.next))
      .body

    val matcher = Pattern.compile("window\\._sharedData\\s*=\\s*(.+});").matcher(html)
    matcher.find() match {
      case true =>
        val data = matcher.group(1)
        val pageData = JSONHelper.fromJson[JsonNode](data)
        val profile = parser.parseProfile(pageData.at("/entry_data/ProfilePage/0/graphql/user"))
        Some(profile)
      case _ => None
    }
  }

  def getUsernameById(id: Long): String = {
    val jsonString = browser.get(
      ID_DATA_URL.format(id),
      headers = defaultHeaders,
      proxy = Some(settings.proxies.next)).body
    println(jsonString)
    Try(JSONHelper.fromJson[JsonNode](jsonString).at("/user/username").asText(""))
      .getOrElse("")

  }

  def getMediaTimeline(method: GetMediaMethod): (MediaResponse, String) = {
    if (method.userId.isEmpty || method.cursor.isEmpty)
      initGetMedias(method.username)
    else
      getMedias(method)
  }

  private def initGetMedias(username: String): (MediaResponse, String) = {
    val requestUrl = FIRST_URL.format(username)
    val html = browser.get(url = requestUrl, headers = defaultHeaders, proxy = Some(settings.proxies.next)).body

    val matcher = Pattern.compile("window\\._sharedData\\s*=\\s*(.+});").matcher(html)
    matcher.find()
    val data = matcher.group(1)
    val pageData = JSONHelper.fromJson[JsonNode](data)
    val profileData = pageData.at("/entry_data/ProfilePage/0/graphql/user")
    (parser.parsePosts(username, profileData), pageData.at("/rhx_gis").asText())
  }

  private def getMedias(method: GetMediaMethod): (MediaResponse, String) = {

    val variablesParam = s"""{"id":"${method.userId.get}","first":50,"after":"${method.cursor.get}"}"""

    val igGis = generateIGGis(method.rhxGis.get, browser.getCookie("csrftoken"), variablesParam)
    val html = browser.get(url = PAGING_MEDIA_URL.format(encodeURIComponent(variablesParam)),
      headers = defaultHeaders ++ Map(
        "x-instagram-gis" -> igGis,
        "x-requested-with" -> "XMLHttpRequest"
      ),
      proxy = Some(settings.proxies.next)
    ).body

    val pageData = JSONHelper.fromJson[JsonNode](html)
    val profileData = pageData.at("/data/user")
    (parser.parsePosts(method.username, profileData), method.rhxGis)
  }

  def getLikers(media: Media): Seq[User] = {
    val likerData = JSONHelper.fromJson[JsonNode](browser.get(s"https://i.instagram.com/api/v1/media/${media.mediaId}/likers/").body)
    parser.parseLikedUsers(likerData)
  }

}



object InstagramClient {
  val BASE_URL          = "https://www.instagram.com/"
  val LOGIN_URL         = "https://www.instagram.com/accounts/login/ajax/"
  val FIRST_URL: String = "https://www.instagram.com/%s/"
  val PAGING_MEDIA_URL  = "https://www.instagram.com/graphql/query/?query_hash=42323d64886122307be10013ad2dcc44&variables=%s"
  val ID_DATA_URL       = "https://i.instagram.com/api/v1/users/%d/info/"

  def encodeURIComponent(s: String): String = {
    URLEncoder.encode(s, "UTF-8")
      .replaceAll("\\+", "%20")
      .replaceAll("\\%21", "!")
      .replaceAll("\\%27", "'")
      .replaceAll("\\%28", "(")
      .replaceAll("\\%29", ")")
      .replaceAll("\\%7E", "~")
  }

  def generateIGGis(rhxGis: String, csrfToken: String, variables: String): String = {
    val text = s"$rhxGis:$variables"
    val hash = java.security.MessageDigest.getInstance("MD5").digest(text.getBytes()).map(0xFF & _).map {
      "%02x".format(_)
    }.foldLeft("") {
      _ + _
    }
    hash
  }

}

