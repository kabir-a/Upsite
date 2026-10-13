package com.example.data

import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

class SiteCheckerEngine {

  fun createClient(timeoutSeconds: Long = 4L): OkHttpClient {
    return OkHttpClient.Builder()
      .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
      .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
      .callTimeout(timeoutSeconds + 2, TimeUnit.SECONDS)
      .followRedirects(true)
      .followSslRedirects(true)
      .retryOnConnectionFailure(false)
      .build()
  }

  suspend fun checkSite(
    rawUrl: String,
    client: OkHttpClient
  ): SiteCheckResult = withContext(Dispatchers.IO) {
    if (!currentCoroutineContext().isActive) {
      return@withContext SiteCheckResult(
        url = rawUrl,
        isUp = false,
        statusMessage = "Cancelled"
      )
    }

    val normalizedUrl = if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
      "http://$rawUrl"
    } else {
      rawUrl
    }

    val startTime = SystemClock.elapsedRealtime()

    try {
      // First try HEAD for speed and minimal bandwidth
      val headRequest = Request.Builder()
        .url(normalizedUrl)
        .head()
        .header("User-Agent", "Mozilla/5.0 (Android; Mobile) Upsite/1.0")
        .build()

      var response = try {
        client.newCall(headRequest).execute()
      } catch (e: Exception) {
        null
      }

      // If HEAD is rejected (405 Method Not Allowed / 501 / etc), fallback to GET
      if (response == null || response.code == 405 || response.code == 501) {
        response?.close()
        if (!currentCoroutineContext().isActive) {
          return@withContext SiteCheckResult(
            url = rawUrl,
            isUp = false,
            statusMessage = "Cancelled"
          )
        }
        val getRequest = Request.Builder()
          .url(normalizedUrl)
          .get()
          .header("User-Agent", "Mozilla/5.0 (Android; Mobile) Upsite/1.0")
          .build()
        response = client.newCall(getRequest).execute()
      }

      val latencyMs = SystemClock.elapsedRealtime() - startTime
      val code = response.code
      val message = response.message.ifBlank {
        when (code) {
          200 -> "OK"
          301, 302 -> "Redirected"
          401 -> "Auth Required"
          403 -> "Forbidden (Alive)"
          404 -> "Not Found (Alive)"
          500 -> "Server Error (Alive)"
          502 -> "Bad Gateway"
          503 -> "Unavailable"
          else -> "Status $code"
        }
      }
      response.close()

      // Any HTTP response means server accepted connection and responded!
      val isUp = code in 200..499 || code in 500..599
      SiteCheckResult(
        url = rawUrl,
        isUp = isUp,
        statusCode = code,
        statusMessage = "$code $message",
        responseTimeMs = latencyMs,
        timestamp = System.currentTimeMillis()
      )
    } catch (e: SocketTimeoutException) {
      val latencyMs = SystemClock.elapsedRealtime() - startTime
      SiteCheckResult(
        url = rawUrl,
        isUp = false,
        statusMessage = "Connection timed out",
        responseTimeMs = latencyMs,
        errorDetail = e.message
      )
    } catch (e: ConnectException) {
      val latencyMs = SystemClock.elapsedRealtime() - startTime
      SiteCheckResult(
        url = rawUrl,
        isUp = false,
        statusMessage = "Connection refused",
        responseTimeMs = latencyMs,
        errorDetail = e.message
      )
    } catch (e: UnknownHostException) {
      val latencyMs = SystemClock.elapsedRealtime() - startTime
      SiteCheckResult(
        url = rawUrl,
        isUp = false,
        statusMessage = "DNS lookup failed",
        responseTimeMs = latencyMs,
        errorDetail = e.message
      )
    } catch (e: NoRouteToHostException) {
      val latencyMs = SystemClock.elapsedRealtime() - startTime
      SiteCheckResult(
        url = rawUrl,
        isUp = false,
        statusMessage = "No route to host",
        responseTimeMs = latencyMs,
        errorDetail = e.message
      )
    } catch (e: SSLException) {
      // SSL error often means the server answered but SSL certificate failed, server is online!
      val latencyMs = SystemClock.elapsedRealtime() - startTime
      SiteCheckResult(
        url = rawUrl,
        isUp = true,
        statusCode = 495,
        statusMessage = "SSL Handshake error (Server Alive)",
        responseTimeMs = latencyMs,
        errorDetail = e.message
      )
    } catch (e: IOException) {
      val latencyMs = SystemClock.elapsedRealtime() - startTime
      SiteCheckResult(
        url = rawUrl,
        isUp = false,
        statusMessage = e.localizedMessage ?: "Network error",
        responseTimeMs = latencyMs,
        errorDetail = e.message
      )
    } catch (e: Exception) {
      val latencyMs = SystemClock.elapsedRealtime() - startTime
      SiteCheckResult(
        url = rawUrl,
        isUp = false,
        statusMessage = "Unreachable",
        responseTimeMs = latencyMs,
        errorDetail = e.message
      )
    }
  }
}
