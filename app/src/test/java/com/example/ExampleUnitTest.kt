package com.example

import com.example.data.DefaultWebsites
import com.example.data.SiteCheckResult
import com.example.data.local.SavedListEntity
import com.example.viewmodel.SiteCheckerViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testDefaultWebsitesListIsNonEmptyAndDeduplicated() {
    assertTrue(DefaultWebsites.LIST.isNotEmpty())
    val originalSize = DefaultWebsites.LIST.size
    val distinctSize = DefaultWebsites.LIST.distinct().size
    assertEquals(originalSize, distinctSize)
    assertTrue(DefaultWebsites.LIST.all { it.startsWith("http://") || it.startsWith("https://") })
  }

  @Test
  fun testSiteCheckResultDisplayDomain() {
    val result = SiteCheckResult(
      url = "http://server1.ftpbd.net/files/",
      isUp = true,
      statusCode = 200,
      responseTimeMs = 120
    )
    assertEquals("server1.ftpbd.net/files", result.displayDomain)
    assertTrue(result.isUp)
  }

  @Test
  fun testSavedListEntityUrlsParsing() {
    val entity = SavedListEntity(
      title = "Test Snapshot",
      totalTested = 10,
      onlineCount = 2,
      urlsJoined = "http://example.com\nhttp://google.com"
    )
    val parsed = entity.urlsList
    assertEquals(2, parsed.size)
    assertEquals("http://example.com", parsed[0])
    assertEquals("http://google.com", parsed[1])
  }

  @Test
  fun testParseUrlsFromTextWithCommentsAndWhitespace() {
    val rawText = """
      # This is a comment
      http://server1.ftpbd.net
      
      // Another comment
      103.58.73.6
      # Another comment
      https://cloudflare.com
      http://server1.ftpbd.net
    """.trimIndent()

    val parsed = SiteCheckerViewModel.parseUrlsFromText(rawText)
    assertEquals(3, parsed.size)
    assertEquals("http://server1.ftpbd.net", parsed[0])
    assertEquals("http://103.58.73.6", parsed[1])
    assertEquals("https://cloudflare.com", parsed[2])
  }
}
