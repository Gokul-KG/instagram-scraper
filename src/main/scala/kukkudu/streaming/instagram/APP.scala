package kukkudu.streaming.instagram
import com.google.inject.Module
import com.twitter.inject.app.App
import com.twitter.inject.Logging
import kukkudu.streaming.instagram.models.IGItem
import kukkudu.streaming.instagram.service.{IGModule, Services}
import scala.io.Source

object MyAppMain extends MyApp

class MyApp extends App  {

  override val modules: Seq[Module] = Seq(
    IGModule)

  override protected def run(): Unit = {
    //TODO this has to chage read in spark job from any cluster filesystem or db

    val userIds = Source.fromFile(Services.scraper.settings.idsFile).getLines.map(_.toLong).toSeq


    (new InstagramStreamingJob()).run(userIds)
  }
}
