package kukkudu.streaming.instagram.repository

import com.google.inject.Inject
import com.google.inject.name.Named
import com.twitter.util.Future
import kukkudu.streaming.instagram.models._
import kukkudu.streaming.util.JSONHelper
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.SearchHits
import kukkudu.streaming.instagram.repository.ESRepository.ZActionRequestBuilder

import scala.collection.mutable


trait IGRepository extends Serializable {

  def existProfiles(userNames: Seq[String]): Future[Seq[String]]

  def missingProfiles(userNames: Seq[String]): Future[Seq[String]]

  def indexProfile(profile: Profile): Future[Boolean]

  def indexMedias(medias: Seq[Media]): Future[Int]

}


class ElasticIGRepository @Inject()(@Named("profile_type") profileType: String,
                                    @Named("media_type") mediaType: String,
                                    cli: TransportClient,
                                    conf: ESConfig)
  extends ESRepository(cli, conf) with IGRepository {

  private def createIndexRequest(media: Media) = {
    prepareIndex
      .setType(mediaType)
      .setId(media.mediaId)
      .setSource(JSONHelper.toJson(media))
  }

  override def indexMedias(medias: Seq[Media]): Future[Int] = {
    val request = medias.foldLeft(prepareBulk)((builder,m) =>{
      builder.add(createIndexRequest(m))
      builder
    })

    request.asyncGet().map(response => response.getItems.map(r => if(r.isFailed) 0 else 1 ).sum)
  }

  private def createIndexRequest(profile: Profile) = {
    prepareIndex
      .setType(profileType)
      .setId(profile.userName)
      .setSource(JSONHelper.toJson(profile))
  }

  override def indexProfile(profile: Profile): Future[Boolean] = {
    createIndexRequest(profile)
      .asyncGet()
      .map(_.getId != null)
  }

  override def existProfiles(userNames: Seq[String]): Future[Seq[String]]  = {
    prepareSearch.setTypes(profileType)
      .setQuery(QueryBuilders.idsQuery(profileType).ids(userNames:_*))
      .setSize(userNames.size)
      .asyncGet()
      .map(response => response.getHits.getHits.map(d => d.getId))
  }

  override def missingProfiles(userNames: Seq[String]): Future[Seq[String]] = {
    prepareSearch.setTypes(profileType)
      .setQuery(QueryBuilders.idsQuery(profileType).ids(userNames:_*))
      .setSize(userNames.size)
      .asyncGet()
      .map(response => {
        val users = new mutable.HashSet[String]()
        userNames.foreach(u => users.add(u))
        response.getHits.getHits.foreach(d => users.remove(d.getId))
        users.toSeq
      })
  }


  private def parseProfiles(hits: SearchHits): Page[Profile] = {
    val data = hits.getHits.map(document => JSONHelper.fromJson[Profile](document.sourceAsString()))
    val total = hits.getTotalHits
    Page[Profile](data, 0, total)
  }


}

