package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DefaultWebsites
import com.example.data.SiteCheckResult
import com.example.data.SiteCheckerEngine
import com.example.data.local.AppDatabase
import com.example.data.local.SavedListEntity
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ResultFilter {
  ONLINE_ONLY,
  ALL,
  OFFLINE_ONLY
}

enum class SortOption {
  CHECK_ORDER,
  LATENCY_ASC,
  NAME_ASC
}

data class UiState(
  val isTesting: Boolean = false,
  val testedCount: Int = 0,
  val totalCount: Int = 0,
  val currentProbingUrl: String? = null,
  val onlineSites: List<SiteCheckResult> = emptyList(),
  val allResults: List<SiteCheckResult> = emptyList(),
  val targetUrls: List<String> = DefaultWebsites.LIST,
  val isUsingCustomList: Boolean = false,
  val customUrls: List<String> = emptyList(),
  val filter: ResultFilter = ResultFilter.ONLINE_ONLY,
  val searchQuery: String = "",
  val sortOption: SortOption = SortOption.CHECK_ORDER,
  val concurrency: Int = 12,
  val timeoutSeconds: Long = 4L,
  val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
  val lastFinishedTimestamp: Long? = null
)

class SiteCheckerViewModel(application: Application) : AndroidViewModel(application) {

  private val engine = SiteCheckerEngine()
  private val dao = AppDatabase.getDatabase(application).savedListDao()

  private val _uiState = MutableStateFlow(
    UiState(totalCount = DefaultWebsites.LIST.size)
  )
  val uiState: StateFlow<UiState> = _uiState.asStateFlow()

  val savedLists: StateFlow<List<SavedListEntity>> = dao.getAllSavedLists()
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  private var testJob: Job? = null

  fun startTest(urlsToTest: List<String>? = null) {
    if (_uiState.value.isTesting) return

    val list = (urlsToTest ?: _uiState.value.targetUrls)
      .map { it.trim() }
      .filter { it.isNotBlank() && !it.startsWith("#") }
      .distinct()

    if (list.isEmpty()) return

    testJob?.cancel()

    _uiState.value = _uiState.value.copy(
      isTesting = true,
      testedCount = 0,
      totalCount = list.size,
      currentProbingUrl = null,
      onlineSites = emptyList(),
      allResults = emptyList(),
      lastFinishedTimestamp = null
    )

    val client = engine.createClient(_uiState.value.timeoutSeconds)
    val semaphore = Semaphore(_uiState.value.concurrency)

    testJob = viewModelScope.launch {
      val onlineAcc = mutableListOf<SiteCheckResult>()
      val allAcc = mutableListOf<SiteCheckResult>()

      val deferredList = list.map { rawUrl ->
        async {
          semaphore.withPermit {
            _uiState.value = _uiState.value.copy(currentProbingUrl = rawUrl)
            val result = engine.checkSite(rawUrl, client)

            synchronized(this@SiteCheckerViewModel) {
              allAcc.add(result)
              if (result.isUp) {
                onlineAcc.add(result)
              }
              _uiState.value = _uiState.value.copy(
                testedCount = allAcc.size,
                onlineSites = onlineAcc.toList(),
                allResults = allAcc.toList(),
                currentProbingUrl = rawUrl
              )
            }
            result
          }
        }
      }

      deferredList.awaitAll()

      _uiState.value = _uiState.value.copy(
        isTesting = false,
        currentProbingUrl = null,
        lastFinishedTimestamp = System.currentTimeMillis()
      )
    }
  }

  fun stopTest() {
    testJob?.cancel()
    testJob = null
    _uiState.value = _uiState.value.copy(
      isTesting = false,
      currentProbingUrl = null
    )
  }

  fun resetTest() {
    stopTest()
    _uiState.value = _uiState.value.copy(
      testedCount = 0,
      onlineSites = emptyList(),
      allResults = emptyList(),
      lastFinishedTimestamp = null
    )
  }

  fun setFilter(filter: ResultFilter) {
    _uiState.value = _uiState.value.copy(filter = filter)
  }

  fun setSearchQuery(query: String) {
    _uiState.value = _uiState.value.copy(searchQuery = query)
  }

  fun setSortOption(sortOption: SortOption) {
    _uiState.value = _uiState.value.copy(sortOption = sortOption)
  }

  fun setThemeMode(mode: AppThemeMode) {
    _uiState.value = _uiState.value.copy(themeMode = mode)
  }

  fun setConcurrency(concurrency: Int) {
    _uiState.value = _uiState.value.copy(concurrency = concurrency.coerceIn(4, 30))
  }

  fun setTimeoutSeconds(seconds: Long) {
    _uiState.value = _uiState.value.copy(timeoutSeconds = seconds.coerceIn(2L, 15L))
  }

  fun setUseCustomList(useCustom: Boolean) {
    if (useCustom) {
      val activeUrls = if (_uiState.value.customUrls.isNotEmpty()) {
        _uiState.value.customUrls
      } else {
        parseUrlsFromText(SAMPLE_URL_TEXT)
      }
      _uiState.value = _uiState.value.copy(
        isUsingCustomList = true,
        customUrls = activeUrls,
        targetUrls = activeUrls,
        totalCount = activeUrls.size,
        testedCount = 0,
        onlineSites = emptyList(),
        allResults = emptyList()
      )
    } else {
      _uiState.value = _uiState.value.copy(
        isUsingCustomList = false,
        targetUrls = DefaultWebsites.LIST,
        totalCount = DefaultWebsites.LIST.size,
        testedCount = 0,
        onlineSites = emptyList(),
        allResults = emptyList()
      )
    }
  }

  fun updateCustomUrls(rawLines: List<String>) {
    val parsed = parseUrlsFromText(rawLines.joinToString("\n"))
    _uiState.value = _uiState.value.copy(
      customUrls = parsed,
      isUsingCustomList = true,
      targetUrls = parsed,
      totalCount = parsed.size,
      testedCount = 0,
      onlineSites = emptyList(),
      allResults = emptyList()
    )
  }

  fun updateTargetUrls(urls: List<String>) {
    val cleaned = urls.map { it.trim() }.filter { it.isNotBlank() && !it.startsWith("#") }.distinct()
    _uiState.value = _uiState.value.copy(
      targetUrls = cleaned,
      customUrls = cleaned,
      isUsingCustomList = true,
      totalCount = cleaned.size,
      testedCount = 0,
      onlineSites = emptyList(),
      allResults = emptyList()
    )
  }

  fun resetToDefaultUrls() {
    setUseCustomList(false)
  }

  fun saveCurrentOnlineList(
    customTitle: String? = null,
    onSuccess: (SavedListEntity) -> Unit
  ) {
    val online = _uiState.value.onlineSites
    if (online.isEmpty()) return

    viewModelScope.launch {
      val defaultDateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
      val title = customTitle?.takeIf { it.isNotBlank() }
        ?: "Online Sites - ${defaultDateFormat.format(Date())}"

      val avgLatency = if (online.isNotEmpty()) {
        online.map { it.responseTimeMs }.average().toLong()
      } else 0L

      val entity = SavedListEntity(
        title = title,
        timestamp = System.currentTimeMillis(),
        totalTested = _uiState.value.testedCount,
        onlineCount = online.size,
        urlsJoined = online.joinToString("\n") { it.url },
        averageLatencyMs = avgLatency
      )

      val newId = dao.insertSavedList(entity)
      onSuccess(entity.copy(id = newId))
    }
  }

  fun deleteSavedList(entity: SavedListEntity) {
    viewModelScope.launch {
      dao.deleteSavedList(entity)
    }
  }

  fun prepareExportText(specificList: List<String>? = null): String {
    val urls = specificList ?: _uiState.value.onlineSites.map { it.url }
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val dateStr = dateFormat.format(Date())

    val sb = StringBuilder()
    sb.appendLine("========================================")
    sb.appendLine("Upsite - Online Websites Report")
    sb.appendLine("Generated: $dateStr")
    sb.appendLine("Total Online Sites: ${urls.size}")
    sb.appendLine("========================================")
    sb.appendLine()
    urls.forEachIndexed { index, url ->
      sb.appendLine("${index + 1}. $url")
    }
    sb.appendLine()
    sb.appendLine("========================================")
    sb.appendLine("Generated by Upsite")
    return sb.toString()
  }

  override fun onCleared() {
    super.onCleared()
    testJob?.cancel()
  }

  companion object {
    fun parseUrlsFromText(text: String): List<String> {
      return text.lines()
        .map { it.trim() }
        .filter { line ->
          line.isNotBlank() && !line.startsWith("#") && !line.startsWith("//")
        }
        .map { line ->
          if (!line.startsWith("http://") && !line.startsWith("https://")) {
            "http://$line"
          } else {
            line
          }
        }
        .distinct()
    }

    const val SAMPLE_URL_TEXT = """# Upsite Custom List Sample
# Format: One URL or IP address per line
# 'http://' or 'https://' prefix is optional
# Lines starting with # are comments

http://server1.ftpbd.net
http://103.58.73.6
http://discoveryftp.net
http://172.16.50.4
http://10.100.100.10
https://cloudflare.com
http://circleftp.net"""
  }
}
