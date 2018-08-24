package kukkudu.streaming.instagram.service

import java.net.{InetAddress, InetSocketAddress, Proxy}

import kukkudu.streaming.instagram.models.Implicits.FutureImplicit
import kukkudu.streaming.instagram.models.{ProfileRequest, ScraperSettings}
import kukkudu.streaming.instagram.repository.{ESConfig, ElasticIGRepository, IGRepository}
import kukkudu.streaming.instagram.service._
import kukkudu.streaming.util.ZConfig
import com.google.inject.name.Named
import com.google.inject.{Provides, Singleton}
import com.twitter.inject.{Injector, TwitterModule}
import com.typesafe.config.{Config, ConfigFactory, ConfigRenderOptions}
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress


object Services {
  var service: IgDataService = _
  var scraper: IGScraper = _
}

object IGModule extends TwitterModule {

  override def configure: Unit = {

    bind[String]
      .annotatedWithName("profile_type")
      .toInstance(ZConfig.getString("repository.es.profile_type"))

    bind[String]
      .annotatedWithName("media_type")
      .toInstance(ZConfig.getString("repository.es.media_type"))

    bind[IGRepository].to[ElasticIGRepository].asEagerSingleton()
    bind[IgDataService].to[ElasticIGDataService].asEagerSingleton()
  }

  override def singletonStartup(injector: Injector): Unit = {
    Services.service = injector.instance[IgDataService]
    Services.scraper = injector.instance[IGScraper]
  }

  @Provides
  @Singleton
  def providesSettings(): ScraperSettings = {

    val inputFile = ZConfig.getString("scraper.profile_file")
    val proxyFile = ZConfig.getString("scraper.proxy_file")
    val threads = ZConfig.getInt("scraper.threads", 8)
    val mediaThreads = ZConfig.getInt("scraper.media_threads", 16)
    val idsFile = ZConfig.getString("scraper.ids_file")
    val usernameFile = ZConfig.getString("scraper.username_file")

    val proxies = Iterator.continually(
      scala.io.Source.fromFile(proxyFile)
        .getLines()
        .map(_.trim)
        .filter(_.nonEmpty)
        .map(_.split(":"))
        .map(p => new Proxy(Proxy.Type.HTTP, new InetSocketAddress(p(0), p(1).toInt)))
    ).flatten

    ScraperSettings(inputFile, proxies, threads, mediaThreads, idsFile, usernameFile)
  }

  @Provides
  @Singleton
  def providesESConfig(): ESConfig = {
    import scala.collection.JavaConversions._
    val indexSettings = ZConfig.getConfig("elasticsearch.settings")
      .root()
      .render(ConfigRenderOptions.concise())
    val indexMappings = ZConfig.getConfig("elasticsearch.mappings")
      .root()
      .entrySet()
      .map(t => t.getKey -> t.getValue.render(ConfigRenderOptions.concise()))
      .toMap
    val indexName = ZConfig.getString("elasticsearch.index_name")
    ESConfig(indexName, indexSettings, indexMappings)
  }

  @Provides
  @Singleton
  def providesElasticsearch(): TransportClient = {

    val clusterName = ZConfig.getString("elasticsearch.cluster_name")
    val client = TransportClient.builder()
      .settings(Settings.builder()
        .put("cluster.name", clusterName)
        .put("client.transport.sniff", "false")
        .build())
      .build()

    ZConfig.getStringList("elasticsearch.servers")
      .map(s => {
        val hostPort = s.split(":")
        (hostPort(0), hostPort(1).toInt)
      }).foreach(hostPort => {
      info(s"Add ${hostPort} to transport address")
      client.addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(hostPort._1), hostPort._2))
    })

    client
  }
}