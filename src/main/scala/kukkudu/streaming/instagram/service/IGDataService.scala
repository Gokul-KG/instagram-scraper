package kukkudu.streaming.instagram.service

import com.google.inject.Inject
import com.twitter.util.Future
import kukkudu.streaming.instagram.models._
import kukkudu.streaming.instagram.repository.IGRepository

trait IgDataService {
  def nonExistProfiles(userNames: Seq[String]): Future[Seq[String]]

  def indexProfile(profile: Profile): Future[Boolean]

  def indexMedias(medias: Seq[Media]): Future[Int]

  def saveCrawlingResult(profile: Profile, medias: Seq[Media]): Boolean
}

case class ElasticIGDataService@Inject()(repository: IGRepository) extends IgDataService with Serializable {

  override def nonExistProfiles(userNames: Seq[String]): Future[Seq[String]] = repository.missingProfiles(userNames)

  override def indexProfile(profile: Profile): Future[Boolean] = repository.indexProfile(profile)

  def indexMedias(medias: Seq[Media]): Future[Int] = repository.indexMedias(medias)

  override def saveCrawlingResult(profile: Profile, medias: Seq[Media]): Boolean = {
    indexProfile(profile)
    medias.grouped(50).foreach(indexMedias)
    true
  }
}
