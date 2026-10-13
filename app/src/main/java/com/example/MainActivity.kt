package com.example

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.MainScreen
import com.example.ui.screens.ManageUrlsDialog
import com.example.ui.screens.SavedListsScreen
import com.example.ui.screens.SettingsDialog
import com.example.ui.screens.UpgradesDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.SiteCheckerViewModel
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppScreen {
  MAIN,
  SAVED_LISTS
}

class MainActivity : ComponentActivity() {

  private val viewModel: SiteCheckerViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      val uiState by viewModel.uiState.collectAsState()
      val savedLists by viewModel.savedLists.collectAsState()

      var currentScreen by remember { mutableStateOf(AppScreen.MAIN) }
      var showSettingsDialog by remember { mutableStateOf(false) }
      var showManageUrlsDialog by remember { mutableStateOf(false) }
      var showUpgradesDialog by remember { mutableStateOf(false) }

      // Content string queued to be written to a file via SAF
      var pendingExportContent by remember { mutableStateOf<String?>(null) }
      val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
      ) { uri: Uri? ->
        if (uri != null && pendingExportContent != null) {
          try {
            contentResolver.openOutputStream(uri)?.use { stream ->
              OutputStreamWriter(stream).use { writer ->
                writer.write(pendingExportContent)
              }
            }
            Toast.makeText(this, "Saved file successfully!", Toast.LENGTH_SHORT).show()
          } catch (e: Exception) {
            Toast.makeText(this, "Failed to save file: ${e.message}", Toast.LENGTH_SHORT).show()
          }
        }
        pendingExportContent = null
      }

      fun triggerExportToFile(content: String, defaultFileName: String = "upsite_list.txt") {
        pendingExportContent = content
        exportFileLauncher.launch(defaultFileName)
      }

      MyApplicationTheme(themeMode = uiState.themeMode) {
        Crossfade(targetState = currentScreen, label = "screen_transition") { screen ->
          when (screen) {
            AppScreen.MAIN -> {
              MainScreen(
                viewModel = viewModel,
                uiState = uiState,
                savedRecordsCount = savedLists.size,
                onNavigateToSavedLists = { currentScreen = AppScreen.SAVED_LISTS },
                onOpenSettings = { showSettingsDialog = true },
                onOpenManageUrls = { showManageUrlsDialog = true },
                onOpenUpgrades = { showUpgradesDialog = true }
              )
            }
            AppScreen.SAVED_LISTS -> {
              BackHandler {
                currentScreen = AppScreen.MAIN
              }
              SavedListsScreen(
                savedLists = savedLists,
                onBack = { currentScreen = AppScreen.MAIN },
                onReTestList = { urls ->
                  currentScreen = AppScreen.MAIN
                  viewModel.updateTargetUrls(urls)
                  viewModel.startTest(urls)
                },
                onExportToFile = { urlsJoined ->
                  val report = viewModel.prepareExportText(urlsJoined.split("\n").filter { it.isNotBlank() })
                  val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                  triggerExportToFile(report, "upsite_saved_record_$timeStamp.txt")
                },
                onDeleteList = { record ->
                  viewModel.deleteSavedList(record)
                }
              )
            }
          }
        }

        // Settings Dialog
        if (showSettingsDialog) {
          SettingsDialog(
            currentTheme = uiState.themeMode,
            currentConcurrency = uiState.concurrency,
            currentTimeout = uiState.timeoutSeconds,
            isUsingCustomList = uiState.isUsingCustomList,
            customListCount = uiState.customUrls.size,
            defaultListCount = com.example.data.DefaultWebsites.LIST.size,
            onThemeChanged = { viewModel.setThemeMode(it) },
            onConcurrencyChanged = { viewModel.setConcurrency(it) },
            onTimeoutChanged = { viewModel.setTimeoutSeconds(it) },
            onUseCustomListChanged = { viewModel.setUseCustomList(it) },
            onEditCustomList = {
              showSettingsDialog = false
              showManageUrlsDialog = true
            },
            onOpenUpgrades = {
              showSettingsDialog = false
              showUpgradesDialog = true
            },
            onDismiss = { showSettingsDialog = false }
          )
        }

        // Manage URLs Dialog
        if (showManageUrlsDialog) {
          ManageUrlsDialog(
            currentUrls = if (uiState.isUsingCustomList && uiState.customUrls.isNotEmpty()) uiState.customUrls else uiState.targetUrls,
            isUsingCustomList = uiState.isUsingCustomList,
            onSaveUrls = { newUrls ->
              viewModel.updateCustomUrls(newUrls)
            },
            onResetDefault = {
              viewModel.resetToDefaultUrls()
            },
            onDismiss = { showManageUrlsDialog = false }
          )
        }

        // Upgrades Dialog
        if (showUpgradesDialog) {
          UpgradesDialog(
            onDismiss = { showUpgradesDialog = false }
          )
        }
      }
    }
  }
}
