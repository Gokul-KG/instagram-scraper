package kukkudu.streaming.instagram.client

import java.net.{HttpCookie, InetSocketAddress, Proxy}
import java.nio.charset.Charset
import java.util.Calendar

import org.apache.http.{HttpEntity, HttpHost, NameValuePair}
import org.apache.http.client.config.{CookieSpecs, RequestConfig}
import org.apache.http.client.entity.{GzipDecompressingEntity, UrlEncodedFormEntity}
import org.apache.http.client.methods.{HttpGet, HttpPost, HttpRequestBase}
import org.apache.http.client.utils.URLEncodedUtils
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.impl.client.{BasicCookieStore, HttpClientBuilder}
import org.apache.http.impl.cookie.BasicClientCookie
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import scalaj.http.HttpResponse

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer



case class ApacheHttpClient(connectTimeout: Int = 30000, readTimeout: Int = 60000) extends HttpClient {

  import scala.collection.JavaConverters._

  private val cookies = new BasicCookieStore
  private val client = HttpClientBuilder.create()
    .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.DEFAULT).build())
    .setDefaultCookieStore(cookies)
    .build()

  override def put(url: String,
                   headers: Map[String, String],
                   getParams: Map[String, String],
                   postParams: Map[String, String],
                   proxy: Option[Proxy] = None): HttpResponse[String] = ???

  override def putAsList(url: String, headers: Map[String, String], getParams: Map[String, String], postParams: Seq[(String, String)],
                         proxy: Option[Proxy] = None): HttpResponse[String] = ???

  override def putData(url: String, headers: Map[String, String], getParams: Map[String, String], postData: String,
                       proxy: Option[Proxy] = None): HttpResponse[String] = ???


  override def getAsStream(url: String,
                           headers: Map[String, String],
                           getParams: Map[String, String],
                           postParams: Map[String, String],
                           proxy: Option[Proxy] = None): HttpResponse[Array[Byte]] = {
    var entity: HttpEntity = null
    try {
      val getRequest = new HttpGet(buildQuery(url, getParams))
      setHeaders(getRequest, headers)
      entity = exec(getRequest, None)//proxy)
      HttpResponse[Array[Byte]](EntityUtils.toByteArray(entity), 200, headers = Map())
    } finally {
      EntityUtils.consumeQuietly(entity)
    }
  }

  override def get(url: String,
                   headers: Map[String, String],
                   getParams: Map[String, String],
                   postParams: Map[String, String] = null,
                   proxy: Option[Proxy] = None): HttpResponse[String] = {
    var entity: HttpEntity = null
    try {
      val getRequest = new HttpGet(buildQuery(url, getParams))
      setHeaders(getRequest, headers)
      entity = exec(getRequest, None) //proxy)
      HttpResponse[String](EntityUtils.toString(entity), 200, headers = Map())
    } finally {
      EntityUtils.consumeQuietly(entity)
    }
  }


  override def post(url: String, headers: Map[String, String], getParams: Map[String, String], postParams: Map[String, String],
                    proxy: Option[Proxy] = None) = {
    makePost(url, headers, getParams, postParams, proxy)
  }

  override def postAsList(url: String, headers: Map[String, String], getParams: Map[String, String], postParams: Seq[(String, String)],
                          proxy: Option[Proxy] = None) = {
    makePost(url, headers, getParams, postParams, proxy)
  }

  override def postData(url: String, headers: Map[String, String], getParams: Map[String, String], postData: String,
                        proxy: Option[Proxy] = None) = {
    makePost(url, headers, getParams, postData, proxy)
  }


  private def makePost(url: String,
                       headers: Map[String, String],
                       getParams: Map[String, String],
                       postParams: Any,
                       proxy: Option[Proxy] = None): HttpResponse[String] = {

    var entity: HttpEntity = null
    try {
      val postRequest = new HttpPost(buildQuery(url, getParams))
      postParams match {
        case null =>
        case x: Map[_, _] =>
          val postParams = x.asInstanceOf[Map[String, String]]
          val postData = new ListBuffer[NameValuePair]
          postParams.foreach(e => {
            postData.append(new BasicNameValuePair(e._1, e._2))
          })
          postRequest.setEntity(new UrlEncodedFormEntity(postData.asJava, Charset.forName("utf-8")))
        case x: Seq[_] =>
          val params = x.asInstanceOf[Seq[(String, String)]]
          val postData = new ListBuffer[NameValuePair]
          params.foreach(e => {
            postData.append(new BasicNameValuePair(e._1, e._2))
          })
          postRequest.setEntity(new UrlEncodedFormEntity(postData.asJava, Charset.forName("utf-8")))
        case postData: String =>
          //postRequest.setEntity(new StringEntity(postData, Charset.forName("utf-8")))
          postRequest.setEntity(new StringEntity(postData, ContentType.create(ContentType.APPLICATION_JSON.getMimeType, Charset.forName("utf-8"))))
      }
      setHeaders(postRequest, headers)
      entity = exec(postRequest, proxy)

      HttpResponse[String](EntityUtils.toString(entity), 200, headers = Map())
    } finally {
      EntityUtils.consumeQuietly(entity)
    }

  }


  private def exec(request: HttpRequestBase, proxy: Option[Proxy] = None): HttpEntity = {


    val confBuilder = RequestConfig.custom()
      .setCircularRedirectsAllowed(true)
      .setRedirectsEnabled(true)
      .setConnectTimeout(connectTimeout)
      .setConnectionRequestTimeout(connectTimeout)
      .setSocketTimeout(readTimeout)
      .setCookieSpec(CookieSpecs.STANDARD)
      .setProxy(null)


    proxy match {
      case Some(p) =>
        val address = p.address().asInstanceOf[InetSocketAddress]
        p.`type`() match {
          case Proxy.Type.DIRECT =>
            System.clearProperty("socksProxyHost")
            System.clearProperty("socksProxyPort")
          case Proxy.Type.HTTP =>
            System.clearProperty("socksProxyHost")
            System.clearProperty("socksProxyPort")

            val proxyRequest = new HttpHost(address.getHostName, address.getPort, "http")
            confBuilder.setProxy(proxyRequest)
          case Proxy.Type.SOCKS =>
            System.setProperty("socksProxyHost", address.getHostName)
            System.setProperty("socksProxyPort", address.getPort.toString)
        }
      case None =>
        System.clearProperty("socksProxyHost")
        System.clearProperty("socksProxyPort")
    }


    request.setConfig(confBuilder.build())
    val response = client.execute(request)


    response.getEntity.getContentEncoding match {
      case null => response.getEntity
      case ec if ec.getValue.equalsIgnoreCase("gzip") => new GzipDecompressingEntity(response.getEntity)
      case _ => response.getEntity

    }
  }


  private def buildQuery(url: String, getParams: Map[String, String]) = {
    getParams match {
      case null => url
      case params if params.isEmpty => url
      case _ =>
        val params = new ListBuffer[NameValuePair]
        getParams.foreach(e => {
          params.append(new BasicNameValuePair(e._1, e._2))
        })
        val queries = URLEncodedUtils.format(params.asJava, Charset.forName("UTF-8"))
        s"$url?$queries"
    }
  }

  private def setHeaders(req: HttpRequestBase,
                         headers: Map[String, String]): Unit = {
    headers match {
      case null =>
      case headers if headers.isEmpty =>
      case _ =>
        headers.foreach(e => {
          req.addHeader(e._1, e._2)
        })
    }
  }

  def clearCookies(): Unit = {
    cookies.clear

  }

  override def getCookie(name: String) = {
    cookies.getCookies.filter(_.getName.equals(name)) match {
      case cc if cc.nonEmpty => cc.head.getValue
      case _ => null
    }
  }

  override def hasCookie(name: String) = {

    cookies.getCookies.filter(_.getName.equals(name)) match {
      case cc if cc.nonEmpty => true
      case _ => false
    }
  }

  override def setCookie(name: String, value: String): Unit = {
    cookies.addCookie(new BasicClientCookie(name, value))
  }

  override def setCookie(name: String, c: HttpCookie): Unit = {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_MONTH, 1)
    val bc = new BasicClientCookie(c.getName, c.getValue)
    bc.setDomain(c.getDomain)
    bc.setPath(c.getPath)
    bc.setExpiryDate(cal.getTime)
    cookies.addCookie(bc)
  }
}
