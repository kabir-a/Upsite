package com.example.data

data class SiteCheckResult(
  val url: String,
  val isUp: Boolean,
  val statusCode: Int? = null,
  val statusMessage: String = "",
  val responseTimeMs: Long = 0L,
  val timestamp: Long = System.currentTimeMillis(),
  val errorDetail: String? = null
) {
  val displayDomain: String
    get() {
      return try {
        val trimmed = url.removePrefix("http://").removePrefix("https://").trimEnd('/')
        trimmed
      } catch (e: Exception) {
        url
      }
    }
}
