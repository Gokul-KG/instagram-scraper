package kukkudu.streaming.instagram


import kukkudu.streaming.instagram.models.IGItem
import kukkudu.streaming.instagram.service.{IGScraper, Services}
import kukkudu.streaming.util.{PollingReceiver, PollingSchedule}
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.ReceiverInputDStream
import org.apache.spark.streaming.receiver.Receiver


private class InstagramReceiver(ids: List[Long],
                                 pollingSchedule: PollingSchedule,
                                 storageLevel: StorageLevel,
                                 pollingWorkers: Int
                               ) extends PollingReceiver[IGItem](pollingSchedule, pollingWorkers, storageLevel)  {


  override protected def poll(): Unit = {
    Services.scraper.getPostFromUserIds(ids).foreach( x => {
      store(x)
    })
  }

}

class InstagramDStream(
                             ssc: StreamingContext,
                             ids: List[Long],
                             pollingSchedule: PollingSchedule,
                             pollingWorkers: Int,
                             storageLevel: StorageLevel
                           ) extends ReceiverInputDStream[IGItem](ssc) {

  override def getReceiver(): Receiver[IGItem] = {
    logDebug("Creating instagram receiver")
    new InstagramReceiver(ids, pollingSchedule, storageLevel, pollingWorkers)
  }
}
