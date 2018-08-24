package kukkudu.streaming.util

object Utils {

  implicit class StringResolver(data: String) {
    def removeQuotes(): String = {
      data.replaceAll("\"", "")
    }
  }

}
