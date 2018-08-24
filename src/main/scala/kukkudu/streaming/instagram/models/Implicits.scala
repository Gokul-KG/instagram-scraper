package kukkudu.streaming.instagram.models

import com.twitter.util.{Await, Future}
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import kukkudu.streaming.instagram.client.HtmlResolver
import com.twitter.util.{Await, Future}
//import javafx.application.Platform
//import javafx.collections.ObservableList
//import javafx.scene.control.ComboBox
import scalaj.http.HttpResponse

object Implicits {

  implicit class FutureImplicit[T](f: Future[T]) {
    def get(): T = Await.result(f)
  }

  implicit def value2Opt[A](f: A): Option[A] = Option(f)

  implicit def opt2Value[A](f: Option[A]): A = f.get

//  def runLater(f: => Any): Unit = Platform.runLater(new Runnable {
//    override def run(): Unit = f
//  })

  implicit def long2String(l: Long): String = l.toString

  implicit class ImplOptString(value: Option[String]) {
    def orEmpty: String = value.getOrElse("")
  }

  implicit class HttpResponseImplicit(response: HttpResponse[String]) {
    def asDocument(url: String) = HtmlResolver(response.body).resolve(url).document
  }

  implicit class ImplicitJsonNode(jsonNode: JsonNode) {
    def asOptString(field: String): Option[String] = if (jsonNode.has(field)) Some(jsonNode.get(field).asText) else None

    def asString(field: String): String = jsonNode.get(field).asText

    def asOptJsonNode(field: String): Option[JsonNode] = if (jsonNode.has(field)) Some(jsonNode.get(field)) else None

    def atOpt(jsonPtrExpr: String): Option[JsonNode] = {
      jsonNode.at(jsonPtrExpr) match {
        case x if x.isMissingNode || x.size() == 0 => None
        case x => Option(x)
      }
    }

    def opt(fieldName: String): Option[JsonNode] = {
      Option(jsonNode.get(fieldName)) match {
        case Some(x) if x.isMissingNode || (x.isInstanceOf[ArrayNode] && x.size() == 0) => None
        case Some(x) => Some(x)
        case _ => None
      }
    }
  }


  implicit class ImplAny(value: Any) {
    def asOptInt: Option[Int] = value.asOptAny.map(_.asInstanceOf[Int])

    def asOptLong: Option[Long] = value.asOptAny.map(_.asInstanceOf[Long])

    def asOptString: Option[String] = value.asOptAny.map(_.asInstanceOf[String])

    def asOptDouble: Option[Double] = value.asOptAny.map(_.asInstanceOf[Double])

    def asOptFloat: Option[Float] = value.asOptAny.map(_.asInstanceOf[Float])

    def asOptBoolean: Option[Boolean] = value.asOptAny.map(_.asInstanceOf[Boolean])

    def asOptShort: Option[Short] = value.asOptAny.map(_.asInstanceOf[Short])

    def asOptAny: Option[Any] = value match {
      case s: Option[_] => s
      case _ => Option(value)
    }
  }

  implicit class StringImplicit(value: String){
    def asOpt:Option[String] = Option(value).filter(_.nonEmpty)
  }

//  implicit class ComboBoxImplicit[T](control: ComboBox[T]){
//    def optValue: Option[T] = Option(control.getValue)
//  }
//
//  implicit class ObservableListImplicit[T](list: ObservableList[T]){
//    def toSeq: Seq[T] = {
//      val r = scala.collection.mutable.ListBuffer.empty[T]
//      for (i <- 0 until list.size()) {
//        r += list.get(i)
//      }
//      r
//    }
//  }

}


