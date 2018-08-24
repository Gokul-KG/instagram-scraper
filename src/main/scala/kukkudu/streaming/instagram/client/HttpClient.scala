package kukkudu.streaming.instagram.client

import java.net.{HttpCookie, Proxy}

import scalaj.http.HttpResponse


trait HttpClient extends  Serializable {

  def hasCookie(name: String): Boolean

  def getCookie(name: String): String

  def setCookie(name: String, value: String): Unit

  def setCookie(name: String, c: HttpCookie): Unit


  def getAsStream(url: String,
                  headers: Map[String, String] = null,
                  getParams: Map[String, String] = null,
                  postParams: Map[String, String] = null,
                  proxy: Option[Proxy] = None): HttpResponse[Array[Byte]]

  def get(url: String,
          headers: Map[String, String] = null,
          getParams: Map[String, String] = null,
          postParams: Map[String, String] = null,
          proxy: Option[Proxy] = None): HttpResponse[String]


  def post(url: String,
           headers: Map[String, String] = null,
           getParams: Map[String, String] = null,
           postParams: Map[String, String] = null,
           proxy: Option[Proxy] = None): HttpResponse[String]

  def postAsList(url: String,
                 headers: Map[String, String] = null,
                 getParams: Map[String, String] = null,
                 postParams: Seq[(String, String)] = null,
                 proxy: Option[Proxy] = None): HttpResponse[String]

  def postData(url: String,
               headers: Map[String, String] = null,
               getParams: Map[String, String] = null,
               postData: String = null,
               proxy: Option[Proxy] = None): HttpResponse[String]

  def put(url: String,
          headers: Map[String, String] = null,
          getParams: Map[String, String] = null,
          postParams: Map[String, String] = null,
          proxy: Option[Proxy] = None): HttpResponse[String]

  def putAsList(url: String,
                headers: Map[String, String] = null,
                getParams: Map[String, String] = null,
                postParams: Seq[(String, String)] = null,
                proxy: Option[Proxy] = None): HttpResponse[String]

  def putData(url: String,
              headers: Map[String, String] = null,
              getParams: Map[String, String] = null,
              postData: String = null,
              proxy: Option[Proxy] = None): HttpResponse[String]


}
