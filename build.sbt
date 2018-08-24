organization := "kukkudu.streaming"
name := "streaming-instagram"
description := "Instagram scraping using Spark Streaming."

scalaVersion := "2.11.7"

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature"
)

libraryDependencies ++= Seq(
  // spark
  "org.apache.spark" %% "spark-core" % "2.1.0" % "compile",
  "org.apache.spark" % "spark-streaming_2.11" % "2.1.0" % "compile",
  //other
  "com.twitter" %% "finatra-http" % "18.5.0",
  "org.elasticsearch" % "elasticsearch" % "2.4.0",
  "org.scalaj" %% "scalaj-http" % "2.4.0",
  "org.jsoup" % "jsoup" % "1.10.3",
  "com.typesafe" % "config" % "1.3.0",


// testing
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",

  // dependencies
  //"log4j" % "log4j" % "1.2.17",
  "net.liftweb" %% "lift-json" % "3.0.1"
).map(_.exclude("org.slf4j", "log4j-over-slf4j"))

assemblyMergeStrategy in assembly := {
  case PathList("javax", "inject", xs @ _*) => MergeStrategy.last
  case PathList("javax", "servlet", xs @ _*) => MergeStrategy.last
  case PathList("javax", "activation", xs @ _*) => MergeStrategy.last
  case PathList("org", "aopalliance", xs @ _*) => MergeStrategy.last
  case PathList("org", "apache", xs @ _*) => MergeStrategy.last
  case PathList("com", "google", xs @ _*) => MergeStrategy.last
  case PathList("com", "esotericsoftware", xs @ _*) => MergeStrategy.last
  case PathList("com", "codahale", xs @ _*) => MergeStrategy.last
  case PathList("com", "yammer", xs @ _*) => MergeStrategy.last
  case "about.html" => MergeStrategy.rename
  case "META-INF/ECLIPSEF.RSA" => MergeStrategy.last
  case "META-INF/mailcap" => MergeStrategy.last
  case "META-INF/mimetypes.default" => MergeStrategy.last
  case "plugin.properties" => MergeStrategy.last
  case "log4j.properties" => MergeStrategy.last
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
