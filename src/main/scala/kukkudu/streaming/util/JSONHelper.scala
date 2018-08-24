package kukkudu.streaming.util
import java.lang.reflect.{ParameterizedType, Type}

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

object JSONHelper {
  val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)
  mapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES)
  mapper.setSerializationInclusion(Include.NON_NULL)
  //mapper.setSerializationInclusion(Include.NON_EMPTY)
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  def fromJson[T: Manifest](json: String): T = {
    mapper.readValue[T](json, typeReference[T])
  }

  def readTree(json: String): JsonNode = Option(json) match {
    case Some(s) => mapper.readTree(s)
    case _ => null
  }

  def toJson[T](t: T, pretty: Boolean = true): String = {
    if (pretty) mapper.writerWithDefaultPrettyPrinter().writeValueAsString(t)
    else mapper.writeValueAsString(t)
  }

  private[this] def typeReference[T: Manifest] = new TypeReference[T] {
    override def getType: Type = typeFromManifest(manifest[T])
  }

  private[this] def typeFromManifest(m: Manifest[_]): Type = {
    if (m.typeArguments.isEmpty) {
      m.runtimeClass
    }
    else new ParameterizedType {
      def getRawType = m.runtimeClass

      def getActualTypeArguments = m.typeArguments.map(typeFromManifest).toArray

      def getOwnerType = null
    }
  }

  implicit class ObjectLike(str: String) {
    def asJsonObject[A: Manifest] = JSONHelper.fromJson[A](str)

    def asJsonNode = JSONHelper.readTree(str)
  }

  implicit class JsonLike(m: Any) {
    def toJsonString = JSONHelper.toJson(m)
  }


  def isValidJSON(json: String): Boolean = {
    try {
      val parser = mapper.getFactory.createParser(json)
      while (parser.nextToken() != null) {}
      true
    } catch {
      case e: Exception => false
    }
  }


}

class StringToJsonSerializer extends JsonSerializer[scala.Option[String]] {
  override def serialize(value: Option[String], gen: JsonGenerator, serializers: SerializerProvider) = {
    value match {
      case Some(x) if JSONHelper.isValidJSON(x) => gen.writeRawValue(x)
      case Some(x) => gen.writeString(x)
      case None => gen.writeNull()
    }
  }
}
