package kukkudu.streaming.instagram.client

import java.net.URI
import java.util.regex.Pattern

import kukkudu.streaming.instagram.models.{Media, Profile, SocialInfo, User}
import com.fasterxml.jackson.databind.JsonNode
import kukkudu.streaming.instagram.client.MediaResponse
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import scalaj.http.HttpResponse

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer


trait ILinkResolver {
  def resolve(baseUrl: String, link: String): String
}

class LinkResolver extends ILinkResolver {
  private val ignorePatterns = Array[String]("^mailto:", "^javascript:", "^#")

  override def resolve(baseUrl: String, input: String): String = {
    var resolvedLink: String = null
    try {
      var inputLink = input
      if (!isIgnore(inputLink)) {
        if (!Pattern.compile("(^http)|(^[/\\\\.])").matcher(inputLink).find)
          inputLink = "/" + inputLink
        val uri = new URI(baseUrl)
        val result = uri.resolve(input)
        //Only get internal url (in the same alsp.ipd.domain)
        if (uri.getHost.endsWith(result.getHost) || result.getHost.endsWith(uri.getHost))
          resolvedLink = result.toString
      }
    }
    catch {
      case ex: Exception => //Do Nothing
    }
    return resolvedLink
  }

  private def isIgnore(href: String) = {
    var ignoreOK = false
    for (pattern <- ignorePatterns) {
      if (Pattern.compile(pattern).matcher(href).find) {
        ignoreOK = true
      }
    }
    ignoreOK
  }
}

case class HtmlResolver(html: String) {
  private val linkResolver = new LinkResolver
  val document = Jsoup.parse(html)

  def resolve(url: String): HtmlResolver = {
    document.select("*[href] , *[src]").iterator()
      .foreach((e: Element) => {
        val link = if (e.hasAttr("href")) e.attr("href") else e.attr("src")
        val resolvedLink = linkResolver.resolve(url, link)
        if (resolvedLink != null)
          if (e.hasAttr("href"))
            e.attr("href", resolvedLink)
          else
            e.attr("src", resolvedLink)

      })
    this
  }

  def select(cssSelector: String, node: Element = document): Elements = {
    node.select(cssSelector)
  }


}

object HtmlResolver {
  def formParams(cssSelector: String, node: Element): Array[Map[String, String]] = {
    node.select(cssSelector).iterator()
      .map((form: Element) => {
        form.select("input[name] , textarea[name]").iterator()
          .map((e: Element) => {
            val k = e.attr("name")
            val v = if (e.attributes().hasKey("value")) e.attr("value") else e.text()
            (k -> v)
          }).toMap
      }).toArray
  }
}


trait IGParser {

}

case class IGv1Parser() extends IGParser {
  import kukkudu.streaming.instagram.models.Implicits._

  def parseProfile(profileData: JsonNode): Profile = {
    val mediaData = profileData.at("/edge_owner_to_timeline_media")

    val profile = Profile(
      profileData.at("/username").asText(),
      profileData.at("/id").asText(),
      profileData.at("/full_name").asText(),
      profileData.at("/biography").asText(),
      profileData.at("/profile_pic_url_hd").asText(),
      profileData.at("/is_private").asBoolean(false),
      profileData.at("/edge_followed_by/count").asInt(0),
      profileData.at("/edge_follow/count").asInt(0),
      mediaData.at("/count").asInt(0)
    )

    val si = SocialInfo(
      parseFacebookSocialNetwork(profile.biography),
      parseTwitterSocialNetwork(profile.biography),
      parseLineSocialNetwork(profile.biography)
    )

    profile.socialInfo = if(si.fb.nonEmpty || si.twitter.nonEmpty || si.line.nonEmpty) Some(si) else None
    profile.phone = parsePhone(profile.biography)
    profile.emails = parseEmails(profile.biography)
    profile.websites = parseWebsites(profile.biography)

    profile
  }

  def parsePosts(userName: String, data: JsonNode): MediaResponse = {

    val mediaNode = data.at("/edge_owner_to_timeline_media")

    MediaResponse(
      mediaNode.at("/count").asInt(0),
      mediaNode.at("/edges").elements().map(e=> parsePost(userName, e.get("node"))).filter(_.nonEmpty).map(_.get).toSeq,
      mediaNode.at("/page_info/has_next_page").asBoolean(false),
      mediaNode.at("/page_info/end_cursor").asText("")
    )
  }

  def parseLikedUsers(likerData: JsonNode): Seq[User] = {
    likerData.at("/users").elements().map(e =>{
      try{
        User(id = e.at("/pk").asText(),
          userName = e.at("/username").asText(),
          fullName = e.at("/full_name").asText(),
          isPrivate = e.at("/is_private").asBoolean()
        )
      }catch{
        case ex => null
      }
    }).filter(_!=null).toSeq
  }

  private def parsePost(userName: String,node: JsonNode): Option[Media] = try {
    def getTags(s: String) : List[String] = {
      val tags = new ListBuffer[String]()
      val matcher = Pattern.compile("(#[^\\s]+)").matcher(s)

      while (matcher.find())
       tags.append( matcher.group(1))
      tags.toList
    }

    val caption = node.at("/edge_media_to_caption/edges").elements().map(e =>{
      e.at("/node/text").asText()
    }).mkString("\n")
    val tags = getTags(caption)

    Media(
     mediaId = node.at("/id").asText,
     userName =  userName,
     url = node.at("/display_url").asText,
     caption=  caption,
     tags= tags,
     numTags = tags.size,
     likes = node.at("/edge_media_preview_like/count").asInt(0),
     takenTime = node.at("/taken_at_timestamp").asLong()*1000L,
     isVideo= node.at("/is_video").asBoolean)
  } catch {
    case e: Exception => None
  }

  private def parsePhone(bio: String) : Option[String] = {
    val patterns = Seq(
      "\\+?\\d{10,12}",
      "(1\\s?)?((\\([0-9]{3}\\))|[0-9]{3})[\\s\\-]?[0-9]{3}[\\s\\-]?[0-9]{4}"
    )

    for (p <- patterns) {
      val matcher = Pattern.compile(p).matcher(bio)

      matcher.find() match {
        case true => return Option(matcher.group(0))
        case _ =>
      }
    }

    None

  }

  private def parseFacebookSocialNetwork(bio: String) : Option[String] = {
    val patterns = Seq(
      "Facebook\\s*:\\s*@(?<account>[\\s\\w]+)",
      "Facebook\\s*:\\s*@?(?<account>[\\s\\w]+)",
      "FB\\s*:\\s*@(?<account>[\\s\\w]+)",
      "FB\\s*:\\s*@?(?<account>[\\s\\w]+)"
    )

    for (p <- patterns) {
      val matcher = Pattern.compile(p,Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS).matcher(bio)

      matcher.find() match {
        case true => return Option(matcher.group("account"))
        case _ =>
      }
    }

    None

  }

  private def parseLineSocialNetwork(bio: String) : Option[String] = {
    val patterns = Seq(
      "Line(\\s*Id)?\\s*:\\s*@(?<account>\\w+)",
      "Line(\\s*Id)?\\s*:\\s*@?(?<account>\\w+)"
    )

    for (p <- patterns) {
      val matcher = Pattern.compile(p,Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS).matcher(bio)

      matcher.find() match {
        case true => return Option(matcher.group("account"))
        case _ =>
      }
    }

    None

  }

  private def parseTwitterSocialNetwork(bio: String) : Option[String] = {
    val patterns = Seq(
      "Twitter(\\s*Id)?\\s*:\\s*@(?<account>\\w+)",
      "Twitter(\\s*Id)?\\s*:\\s*@?(?<account>\\w+)"
    )

    for (p <- patterns) {
      val matcher = Pattern.compile(p,Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS).matcher(bio)

      matcher.find() match {
        case true => return Option(matcher.group("account"))
        case _ =>
      }
    }

    None

  }

  private def parseEmails(bio: String) : Option[List[String]] = {
    val emails = new ListBuffer[String]
    val matcher = Pattern.compile("[A-Za-z0-9\\._%\\+-]+@[A-Za-z0-9\\.-]+\\.[A-Za-z]{2,6}").matcher(bio)

    while (matcher.find()){
      emails.append(matcher.group(0))
    }

    if(emails.nonEmpty) Some(emails.toList) else None

  }

  private def parseWebsites(bio: String) : Option[List[String]] = {
    val websites = new ListBuffer[String]

    val matcher = Pattern.compile("(https?:\\/\\/)?(www\\.)?\\w+(\\.\\w+)+").matcher(bio)

    while (matcher.find()){
      websites.append(matcher.group(0))
    }

    if(websites.nonEmpty) Some(websites.toList) else None

  }

}
