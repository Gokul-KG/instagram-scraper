package kukkudu.streaming

import java.util.concurrent.TimeUnit

import kukkudu.streaming.util.PollingSchedule
import kukkudu.streaming.instagram.models.IGItem
import kukkudu.streaming.instagram.service.IGScraper
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.ReceiverInputDStream
package object instagram {
  def createIGStream(
                            ssc: StreamingContext,
                            ids:List[Long],
                            pollingSchedule: PollingSchedule = PollingSchedule(30, TimeUnit.SECONDS),
                            pollingWorkers: Int = 1,
                            storageLevel: StorageLevel = StorageLevel.MEMORY_ONLY
                          ): ReceiverInputDStream[IGItem] = {

    new InstagramDStream(
      ssc = ssc,
      ids =ids,
      pollingSchedule = pollingSchedule,
      pollingWorkers = pollingWorkers,
      storageLevel = storageLevel)
  }
}
