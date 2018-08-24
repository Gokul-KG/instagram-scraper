package kukkudu.streaming.instagram.service

import scala.tools.nsc.io.File
import java.util.regex.Pattern

import kukkudu.streaming.instagram.client._
import kukkudu.streaming.instagram.models.Implicits.FutureImplicit
import kukkudu.streaming.instagram.models._
import com.google.inject.{Inject, Singleton}
import com.twitter.finatra.utils.FuturePools

import com.twitter.util.Future

import scala.collection.mutable.ListBuffer


@Singleton
case class IGScraper @Inject()(settings: ScraperSettings, service: IgDataService) extends  Serializable {

  private val crawler = new IGCrawler(settings, service)
  def getPostFromUserIds(ids: Seq[Long]): Seq[IGItem] = {
    val items = ids.map(crawler.crawlPostsByIdProfile1)
    Future.collect(items).get().flatten
  }

  private def getUserName(url: String): String = {
    val m = Pattern.compile("instagram\\.com/([^/]+)").matcher(url)
    m.find()
    m.group(1)
  }


}


case class IGCrawler(settings: ScraperSettings, service: IgDataService)  {

  private val pool      = FuturePools.fixedPool("main", settings.threads)
  private val likerPool = FuturePools.fixedPool("liker", settings.mediaThreads)

  private val client = new InstagramClient(settings)(new ApacheHttpClient())

  def crawlProfile(req: ProfileRequest): Future[Boolean] = pool {
    new InstagramClient(settings)(new ApacheHttpClient())
      .getProfileByUserName(req.userName) match {
      case Some(profile) =>
        val posts = getPosts(client, profile)
        service.saveCrawlingResult(profile, posts)
      case None => false
    }
  }

  def crawlUsernameById(id: Long): Future[String] = pool {
    val user = new InstagramClient(settings)(new ApacheHttpClient())
      .getUsernameById(id)
    if (user != "") {
      File(settings.usernameFile).appendAll(s"$user\n")
    }
    user
  }

  def crawlPostsByIdProfile1(id: Long): Future[IGItem] = pool {
    val client = new InstagramClient(settings)(new ApacheHttpClient())
    val user = client.getUsernameById(id)
    println(user);
    if (user != "") {
      File(settings.usernameFile).appendAll(s"$user\n")
      client.getProfileByUserName(user) match {
        case Some(profile) =>
          val posts = getPosts(client, profile)
          service.saveCrawlingResult(profile, posts)
          IGItem(id , Some(profile), posts)
        case None => IGItem(id, None, Seq())
      }
    } else {
      IGItem(id, None, Seq())
    }
  }

  def crawlUsernameByIdProfile1(id: Long): Boolean= {
    val client = new InstagramClient(settings)(new ApacheHttpClient())
    val user = client.getUsernameById(id)
    println(user);
    if (user != "") {
      File(settings.usernameFile).appendAll(s"$user\n")
      client.getProfileByUserName(user) match {
        case Some(profile) =>
          val posts = getPosts(client, profile)
          println(posts)
          service.saveCrawlingResult(profile, posts)
          true
        case None => false
      }
    } else {
      false
    }
  }


  def crawlUsernameByIdProfile(id: Long): Future[Boolean] = pool {
    val client = new InstagramClient(settings)(new ApacheHttpClient())
    val user = client.getUsernameById(id)
    println(user);
    if (user != "") {
      File(settings.usernameFile).appendAll(s"$user\n")
      client.getProfileByUserName(user) match {
        case Some(profile) =>
          val posts = getPosts(client, profile)
          service.saveCrawlingResult(profile, posts)
        case None => false
      }
    } else {
      false
    }
  }

  private def getPosts(client: InstagramClient, profile: Profile): Seq[Media] = {
    val posts = new ListBuffer[Media]()
    try {
      var response: MediaResponse = null
      var rhxGis: String = null
      val (r, rhx) = client.getMediaTimeline(GetMediaMethod(profile.userName))
      response = r
      rhxGis = rhx
      response.medias.foreach(m => posts.append(m))

      //not taking all the pages
//      while (response != null && response.medias.nonEmpty) {
//        response.medias.foreach(m => posts.append(m))
//        response.hasNextPage && response.nextCursor.nonEmpty match {
//          case true =>
//            val (r, rhx) = client.getMediaTimeline(GetMediaMethod(profile.userName,
//              Some(profile.userId),
//              response.nextCursor,
//              Some(rhxGis)))
//            response = r
//            rhxGis = rhx
//          case _ =>
//            response = null
//            rhxGis = null
//        }
//      }

    } catch {
      case ex: Throwable =>
    }
    posts
  }

}

@Singleton
case class Scraper @Inject()(settings: ScraperSettings, service: IgDataService) {

  private val crawler = new IGCrawler(settings, service)

  def run(): Unit = {

    def scrape(reqs: Seq[String]) = {
      val f = reqs.map(_.trim)
        .filter(_.nonEmpty)
        .map(url => ProfileRequest(getUserName(url)))
        .map(req => crawler.crawlProfile(req))

      Future.collect(f).get()
    }

    def scrape2(ids: Seq[Long]) = {
      val f = ids.map(crawler.crawlUsernameByIdProfile)

      Future.collect(f).get()
    }

    using(scala.io.Source.fromFile(settings.idsFile)) { source =>
      scrape2(
        (for (line <- source.getLines) yield line.toLong).toSeq
      )
    }
  }

  private def getUserName(url: String): String = {
    val m = Pattern.compile("instagram\\.com/([^/]+)").matcher(url)
    m.find()
    m.group(1)
  }

  private def using[A <: {def close() : Unit}, B](resource: A)(f: A => B): B =
    try {
      f(resource)
    } finally {
      resource.close()
    }
}