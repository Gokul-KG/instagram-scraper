package kukkudu.streaming.instagram.repository

import com.twitter.inject.Logging
import com.twitter.util.{Future, Promise}
import org.elasticsearch.action.{ActionRequest, ActionRequestBuilder, ActionResponse}
import org.elasticsearch.action.support.AbstractListenableActionFuture
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.threadpool.ThreadPool

import scala.annotation.tailrec

case class ESConfig(indexName: String, indexSettings: String, indexMappings: Map[String, String]) extends Serializable

abstract class ESRepository(val client: TransportClient, val config: ESConfig) extends Logging with Serializable {

  private val indexExisted = prepareExist.execute().get().isExists
  if (!indexExisted) {
    createIndexWithMapping()
  } else {
    mergeMapping()
  }

  private[this] def createIndexWithMapping(): Unit = {
    info(s"Prepare Create Index ${config.indexName}")
    val prepareIndex = client.admin().indices().prepareCreate(config.indexName)


    info(s"--> Index Settings ${config.indexSettings}")
    prepareIndex.setSettings(config.indexSettings)
    config.indexMappings.foreach(mapping => {
      info(s"--> Add type ${mapping._1}")
      info(s"--> With mapping $mapping")
      prepareIndex.addMapping(mapping._1, mapping._2)
    })
    if (!prepareIndex.execute().actionGet().isAcknowledged) {
      throw new Exception("prepare index environment failed")
    }
  }

  private[this] def mergeMapping(): Unit = {
    config.indexMappings.foreach(mapping => {
      info(s"--> Add type ${mapping._1}")
      info(s"--> With mapping ${mapping._2}")
      if (putMappingSync(mapping._1, mapping._2).isAcknowledged) {
        info(s"=> Mapping merged! Type ${mapping._1} with mapping ${mapping._2}")
      } else {
        throw new Exception("Merge mapping failed")
      }
    })
  }

  private[this] def putMappingSync(esType: String, src: String) = {
    client.admin().indices()
      .preparePutMapping(config.indexName)
      .setType(esType)
      .setSource(src)
      .execute().actionGet()
  }


  def prepareExist = client.admin().indices().prepareExists(config.indexName)

  def prepareScroll(scrollId: String) = client.prepareSearchScroll(scrollId)

  def prepareBulk = client.prepareBulk

  def prepareSearch = client.prepareSearch(config.indexName)

  def prepareCount = client.prepareCount(config.indexName)

  def prepareGet = client.prepareGet().setIndex(config.indexName)

  def prepareIndex = client.prepareIndex().setIndex(config.indexName)

  def prepareUpdate = client.prepareUpdate().setIndex(config.indexName)
  def prepareDelete = client.prepareDelete().setIndex(config.indexName)

}


object ESRepository {


  type Req[T <: ActionRequest[T]] = ActionRequest[T]
  type Resp = ActionResponse

  implicit class ZActionRequestBuilder[I <: Req[I], J <: Resp, K <: ActionRequestBuilder[I, J, K]](arb: ActionRequestBuilder[I, J, K]) {

    private[this] val promise = Promise[J]()

    val internalThreadPool: ThreadPool = internalThreadPool(arb, arb.getClass)

    @tailrec
    private[this] def internalThreadPool(arb: ActionRequestBuilder[I, J, K], cls: Class[_]): ThreadPool = {
      if (cls.getSimpleName.equals("ActionRequestBuilder")) {
        val f = cls.getDeclaredField("threadPool")
        f.setAccessible(true)
        f.get(arb).asInstanceOf[ThreadPool]
      }
      else
        internalThreadPool(arb, cls.getSuperclass)
    }

    def asyncGet(): Future[J] = {
      val listener = new AbstractListenableActionFuture[J, J](internalThreadPool) {
        override def onFailure(e: Throwable): Unit = promise.raise(e)

        override def onResponse(result: J): Unit = promise.setValue(result)

        override def convert(listenerResponse: J): J = listenerResponse
      }
      arb.execute(listener)
      promise
    }
  }

}
