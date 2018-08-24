package kukkudu.streaming.instagram

import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}


class InstagramStreamingJob() {
  def run(userIds:Seq[Long]): Unit = {

    println("\n\n\n\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + userIds )

    val conf = new SparkConf().setAppName("Instagram Streaming Application").setMaster("local[*]")
    val sc = new SparkContext(conf)
    sc.setLogLevel("ERROR")
    val ssc = new StreamingContext(sc, Seconds(10))
    val posts = createIGStream(ssc, userIds.toList)

    posts.print()
    ssc.start()
    ssc.awaitTermination()
  }
}



