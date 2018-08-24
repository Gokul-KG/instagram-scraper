package kukkudu.streaming.instagram

import kukkudu.streaming.instagram.models.Media

package object client {

  trait Method {
    def params(): Map[String, String]
  }

  case class GetMediaMethod(
                             username: String,
                             userId: Option[String] = None,
                             cursor: Option[String] = None,
                             rhxGis: Option[String] = None
                           )

  case class MediaResponse(
                            total: Int,
                            medias: Seq[Media],
                            hasNextPage: Boolean,
                            nextCursor: Option[String]
                          )

}
