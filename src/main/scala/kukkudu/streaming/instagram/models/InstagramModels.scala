  package kukkudu.streaming.instagram.models

  import java.net.Proxy
  case class Page[T](data: Seq[T], from: Long, total: Long)

  case class ProfileRequest(userName: String, recursiveLiker: Option[Boolean] = None)

  case class GenericResponse[T](ok: Boolean, data: T)

  object Status extends Enumeration {
    val PENDING        = Value(1)
    val CRAWLING       = Value(2)
    val DONE_PROFILE   = Value(3)
    val DONE_OK        = Value(4)
    val DONE_CANCELLED = Value(5)
    val DONE_FAILED    = Value(6)
  }
  case class IGItem(userid:Long, userProfile:Option[Profile], medias: Seq[Media])

  case class Media(
                    mediaId: String,
                    userName: String,
                    url: String,
                    caption: String,
                    tags: List[String],
                    numTags: Int,
                    likes: Int,
                    takenTime: Long,
                    isVideo: Boolean
                  )

  case class SocialInfo(
                         fb: Option[String] = None,
                         twitter: Option[String] = None,
                         line: Option[String] = None
                       )

  case class User(
                   id: String,
                   userName: String,
                   fullName: String,
                   isPrivate: Boolean
                 )

  case class Profile(
                      userName: String,
                      userId: String,
                      fullName: String,
                      biography: String,
                      profilePhotoHd: String,
                      isPrivate: Boolean,
                      status: Int,
                      followers: Int = 0,
                      followings: Int = 0,
                      totalPosts: Int = 0,
                      var socialInfo: Option[SocialInfo] = None,
                      var phone: Option[String] = None,
                      var emails: Option[Seq[String]] = None,
                      var websites: Option[Seq[String]] = None,
                      var scrapedTime: Long = System.currentTimeMillis()
                    )

  case class ScraperSettings(
                              inputFile: String,
                              proxies: Iterator[Proxy],
                              threads: Int = 2,
                              mediaThreads: Int = 2,
                              idsFile: String,
                              usernameFile: String
                            )
