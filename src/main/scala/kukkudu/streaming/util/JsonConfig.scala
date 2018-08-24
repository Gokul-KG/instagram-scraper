package kukkudu.streaming.util

import java.io.{BufferedWriter, File, FileWriter}

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.{JsonNodeFactory, MissingNode, ObjectNode, TextNode}
import com.twitter.inject.Logging

import scala.io.Source


case class MissingKeyException(key: String) extends Exception(s"Missing key $key")


case class JsonConfig(configFile: String) extends Logging {

  private val _objectNode = new ObjectNode(JsonNodeFactory.instance)

  _init()

  private def _init(): Unit = {
    val file = new java.io.File(configFile)
    if (!file.exists()) {
      file.getParentFile.mkdirs()
      file.createNewFile()
      saveConfig()
    }
    val objNode = JSONHelper.fromJson[ObjectNode](Source.fromFile(file).getLines().mkString)
    _objectNode.synchronized(_objectNode.setAll(objNode))
  }

  def getJsonNode(key: String): JsonNode = {
    if (key.startsWith("/")) _objectNode.at(key) else _objectNode.path(key)
  }

  def getOptJsonNode(key: String): Option[JsonNode] = {
    val node = if (key.startsWith("/")) _objectNode.at(key) else _objectNode.path(key)
    if (
      node.isInstanceOf[MissingNode] ||
        (node.isInstanceOf[TextNode] && node.asText().isEmpty)
    ) None else Option(node)
  }

  def get(key: String): String = getOptJsonNode(key).getOrElse(throw MissingKeyException(key)).asText

  def get(key: String, defaultVal: String): String = getJsonNode(key).asText(defaultVal)

  def getOpt(key: String): Option[String] = getOptJsonNode(key).map(_.asText)

  def getLong(key: String): Long = getOptJsonNode(key).getOrElse(throw MissingKeyException(key)).asLong

  def getLong(key: String, defaultVal: Long): Long = getJsonNode(key).asLong(defaultVal)

  def getOptLong(key: String): Option[Long] = getOptJsonNode(key).map(_.asLong)

  def getInt(key: String): Int = getOptJsonNode(key).getOrElse(throw MissingKeyException(key)).asInt

  def getInt(key: String, defaultVal: Int): Int = getJsonNode(key).asInt(defaultVal)

  def getOptInt(key: String): Option[Int] = getOptJsonNode(key).map(_.asInt)

  def getDouble(key: String): Double = getOptJsonNode(key).getOrElse(throw MissingKeyException(key)).asDouble

  def getDouble(key: String, defaultVal: Double): Double = getJsonNode(key).asDouble(defaultVal)

  def getOptDouble(key: String): Option[Double] = getOptJsonNode(key).map(_.asDouble)

  def getBoolean(key: String): Boolean = getOptJsonNode(key).getOrElse(throw MissingKeyException(key)).asBoolean

  def getBoolean(key: String, defaultVal: Boolean): Boolean = getJsonNode(key).asBoolean(defaultVal)

  def getOptBoolean(key: String): Option[Boolean] = getOptJsonNode(key).map(_.asBoolean)

  def set(key: String, value: Any, save: Boolean = true): Unit = {
    _objectNode.synchronized({
      val (objectNode, k) = if (key.startsWith("/")) {
        val split = key.split("/")
        var objectNode = _objectNode
        for (i <- 1 until split.length - 1) objectNode = getObject(objectNode, split(i), true)
        (objectNode, split(split.length - 1))
      } else (_objectNode, key)
      value match {
        case null => objectNode.putNull(k)
        case x: String => objectNode.put(k, x)
        case x: Long => objectNode.put(k, x)
        case x: Int => objectNode.put(k, x)
        case x: Double => objectNode.put(k, x)
        case x: Boolean => objectNode.put(k, x)
        case x: JsonNode => objectNode.set(k, x)
        case _ => objectNode.put(k, value.toString)
      }
      if (save) saveConfig()
    })
  }

  def remove(key: String, save: Boolean = true): Unit = {
    _objectNode.synchronized({
      val (objectNode, k) = if (key.startsWith("/")) {
        val split = key.split("/")
        var objectNode = _objectNode
        for (i <- 1 until split.length - 1) objectNode = getObject(objectNode, split(i), false)
        (objectNode, split(split.length - 1))
      } else (_objectNode, key)
      if (objectNode != null) {
        objectNode.remove(k)
        if (save) saveConfig()
      }
    })
  }

  private def getObject(objectNode: ObjectNode, key: String, createIfNotExist: Boolean): ObjectNode = {
    if (objectNode == null) return null
    if (createIfNotExist || objectNode.has(key)) objectNode.`with`(key)
    else null
  }

  private def saveConfig(): Unit = {
    var bw: BufferedWriter = null
    var jsonString: String = null
    try {
      jsonString = JSONHelper.toJson(_objectNode)
      bw = new BufferedWriter(new FileWriter(new File(configFile)))
      bw.write(jsonString)
    } catch {
      case e: Exception => error(s"Failed when JsonConfig.saveConfig($jsonString)", e)
    } finally {
      if (bw != null) bw.close()
    }
  }
}

//object JsonConfig extends JsonConfig("conf/conf.json")