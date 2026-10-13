package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SiteCheckResult
import com.example.ui.components.SiteResultCard
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusErrorContainer
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessContainer
import com.example.viewmodel.ResultFilter
import com.example.viewmodel.SiteCheckerViewModel
import com.example.viewmodel.SortOption
import com.example.viewmodel.UiState
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
  viewModel: SiteCheckerViewModel,
  uiState: UiState,
  savedRecordsCount: Int,
  onNavigateToSavedLists: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenManageUrls: () -> Unit,
  onOpenUpgrades: () -> Unit
) {
  val context = LocalContext.current
  var showSaveDialog by remember { mutableStateOf(false) }
  var showSortMenu by remember { mutableStateOf(false) }
  var textToSaveToFile by remember { mutableStateOf<String?>(null) }

  // SAF File Creator
  val createDocumentLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("text/plain")
  ) { uri: Uri? ->
    if (uri != null && textToSaveToFile != null) {
      try {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
          OutputStreamWriter(outputStream).use { writer ->
            writer.write(textToSaveToFile)
          }
        }
        Toast.makeText(context, "Saved file successfully!", Toast.LENGTH_LONG).show()
      } catch (e: Exception) {
        Toast.makeText(context, "Error saving file: ${e.message}", Toast.LENGTH_LONG).show()
      }
    }
    textToSaveToFile = null
  }

  fun launchSaveFile() {
    val onlineUrls = uiState.onlineSites.map { it.url }
    if (onlineUrls.isEmpty()) {
      Toast.makeText(context, "No online websites to save yet!", Toast.LENGTH_SHORT).show()
      return
    }
    val content = viewModel.prepareExportText(onlineUrls)
    textToSaveToFile = content
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
    createDocumentLauncher.launch("upsite_online_sites_$timeStamp.txt")
  }

  // Filter & sort results
  val displayList by remember(uiState) {
    derivedStateOf {
      val baseList = when (uiState.filter) {
        ResultFilter.ONLINE_ONLY -> uiState.onlineSites
        ResultFilter.ALL -> uiState.allResults
        ResultFilter.OFFLINE_ONLY -> uiState.allResults.filter { !it.isUp }
      }

      val filtered = if (uiState.searchQuery.isBlank()) {
        baseList
      } else {
        val q = uiState.searchQuery.trim().lowercase()
        baseList.filter { it.url.lowercase().contains(q) }
      }

      when (uiState.sortOption) {
        SortOption.CHECK_ORDER -> filtered
        SortOption.LATENCY_ASC -> filtered.sortedBy { if (it.isUp && it.responseTimeMs > 0) it.responseTimeMs else Long.MAX_VALUE }
        SortOption.NAME_ASC -> filtered.sortedBy { it.displayDomain }
      }
    }
  }

  val progressPercent = remember(uiState.testedCount, uiState.totalCount) {
    if (uiState.totalCount > 0) {
      (uiState.testedCount.toFloat() / uiState.totalCount).coerceIn(0f, 1f)
    } else 0f
  }

  Scaffold(
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        ),
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              painter = painterResource(R.drawable.ic_upsite_globe),
              contentDescription = "Upsite Logo",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Upsite",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold
            )
          }
        },
        actions = {
          // Saved Lists Button: changes to cyan if any entry is saved, otherwise remains normal; no numbers
          val bookmarkTint = if (savedRecordsCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
          IconButton(
            onClick = onNavigateToSavedLists,
            modifier = Modifier.testTag("saved_lists_nav_btn")
          ) {
            Icon(
              imageVector = if (savedRecordsCount > 0) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
              contentDescription = "Saved Lists Records",
              tint = bookmarkTint
            )
          }

          // Manage URLs
          IconButton(
            onClick = onOpenManageUrls,
            modifier = Modifier.testTag("manage_urls_btn")
          ) {
            Icon(
              imageVector = Icons.Default.FormatListBulleted,
              contentDescription = "Manage Target Websites"
            )
          }

          // Settings
          IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.testTag("settings_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = "Settings & Theme"
            )
          }
        }
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // Top Controls Section
      Card(
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          // Start / Stop Main Button
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            if (!uiState.isTesting) {
              Button(
                onClick = { viewModel.startTest() },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                  .weight(1f)
                  .height(52.dp)
                  .testTag("start_test_button")
              ) {
                Icon(
                  imageVector = Icons.Default.PlayArrow,
                  contentDescription = null,
                  modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = if (uiState.testedCount > 0) "Restart Check" else "Start Test",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )
              }
            } else {
              Button(
                onClick = { viewModel.stopTest() },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                  .weight(1f)
                  .height(52.dp)
                  .testTag("stop_test_button")
              ) {
                Icon(
                  imageVector = Icons.Default.Stop,
                  contentDescription = null,
                  modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Stop Test",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            // Save to File button
            OutlinedButton(
              onClick = {
                if (uiState.onlineSites.isNotEmpty()) {
                  showSaveDialog = true
                } else {
                  Toast.makeText(context, "No online sites available to save yet", Toast.LENGTH_SHORT).show()
                }
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .height(52.dp)
                .testTag("save_list_main_btn")
            ) {
              Icon(
                imageVector = Icons.Default.SaveAlt,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Save List", fontWeight = FontWeight.SemiBold)
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Progress status bar
          Column(modifier = Modifier.fillMaxWidth()) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = if (uiState.isTesting) "Checking sites in real-time..." else if (uiState.testedCount > 0) "Check Finished" else "Ready to test",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (uiState.isTesting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "${uiState.testedCount} / ${uiState.totalCount} checked (${(progressPercent * 100).toInt()}%)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
              progress = { progressPercent },
              modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .testTag("check_progress_bar"),
              color = MaterialTheme.colorScheme.primary,
              trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Current URL probing info
            AnimatedVisibility(visible = uiState.isTesting && uiState.currentProbingUrl != null) {
              Text(
                text = "Probing: ${uiState.currentProbingUrl}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Summary Statistics Chips
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            StatPill(
              label = "Online",
              count = uiState.onlineSites.size,
              containerColor = StatusSuccessContainer,
              contentColor = StatusSuccess,
              modifier = Modifier.weight(1f)
            )

            val offlineCount = (uiState.testedCount - uiState.onlineSites.size).coerceAtLeast(0)
            StatPill(
              label = "Offline",
              count = offlineCount,
              containerColor = StatusErrorContainer,
              contentColor = StatusError,
              modifier = Modifier.weight(1f)
            )

            val remainingCount = (uiState.totalCount - uiState.testedCount).coerceAtLeast(0)
            StatPill(
              label = "Pending",
              count = remainingCount,
              containerColor = MaterialTheme.colorScheme.surfaceVariant,
              contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.weight(1f)
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Active List Source indicator
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onOpenManageUrls() }
              .testTag("active_list_source_banner")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = if (uiState.isUsingCustomList) "Target: Custom List (${uiState.targetUrls.size} sites)" else "Target: Default BDIX List (${uiState.targetUrls.size} sites)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
              )
              Text(
                text = "Edit / Switch ⚙",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
              )
            }
          }
        }
      }

      // Filter, Search, and Sort Row
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            FilterChip(
              selected = uiState.filter == ResultFilter.ONLINE_ONLY,
              onClick = { viewModel.setFilter(ResultFilter.ONLINE_ONLY) },
              label = { Text("Online (${uiState.onlineSites.size})") },
              leadingIcon = if (uiState.filter == ResultFilter.ONLINE_ONLY) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
              } else null,
              modifier = Modifier.testTag("filter_online_chip")
            )

            FilterChip(
              selected = uiState.filter == ResultFilter.ALL,
              onClick = { viewModel.setFilter(ResultFilter.ALL) },
              label = { Text("All (${uiState.testedCount})") },
              leadingIcon = if (uiState.filter == ResultFilter.ALL) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
              } else null,
              modifier = Modifier.testTag("filter_all_chip")
            )

            FilterChip(
              selected = uiState.filter == ResultFilter.OFFLINE_ONLY,
              onClick = { viewModel.setFilter(ResultFilter.OFFLINE_ONLY) },
              label = { Text("Offline (${(uiState.testedCount - uiState.onlineSites.size).coerceAtLeast(0)})") },
              leadingIcon = if (uiState.filter == ResultFilter.OFFLINE_ONLY) {
                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
              } else null,
              modifier = Modifier.testTag("filter_offline_chip")
            )
          }

          // Sort Menu Button
          Box {
            IconButton(
              onClick = { showSortMenu = true },
              modifier = Modifier.testTag("sort_menu_btn")
            ) {
              Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort Options")
            }
            DropdownMenu(
              expanded = showSortMenu,
              onDismissRequest = { showSortMenu = false }
            ) {
              DropdownMenuItem(
                text = { Text("Order Checked") },
                onClick = {
                  viewModel.setSortOption(SortOption.CHECK_ORDER)
                  showSortMenu = false
                }
              )
              DropdownMenuItem(
                text = { Text("Fastest Response (Lowest Latency)") },
                onClick = {
                  viewModel.setSortOption(SortOption.LATENCY_ASC)
                  showSortMenu = false
                }
              )
              DropdownMenuItem(
                text = { Text("Alphabetical (A - Z)") },
                onClick = {
                  viewModel.setSortOption(SortOption.NAME_ASC)
                  showSortMenu = false
                }
              )
            }
          }
        }

        // Quick search
        if (uiState.testedCount > 5) {
          OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search domain or IP...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            trailingIcon = if (uiState.searchQuery.isNotEmpty()) {
              {
                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                  Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                }
              }
            } else null,
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 4.dp)
              .testTag("search_sites_input")
          )
        }
      }

      // Realtime List of Websites
      if (displayList.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            if (uiState.isTesting) {
              Text(
                text = "Scanning websites...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Live available sites will appear here immediately as they reply.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
              )
            } else if (uiState.testedCount == 0) {
              Icon(
                painter = painterResource(R.drawable.ic_upsite_globe),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(54.dp)
              )
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "Ready to Check ${uiState.targetUrls.size} Websites",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Tap 'Start Test' above to probe servers and identify live connections.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
            } else {
              Text(
                text = "No websites found matching your filter/search.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
              )
            }
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .testTag("realtime_results_list")
        ) {
          items(displayList, key = { it.url }) { item ->
            SiteResultCard(result = item)
          }
          item {
            Spacer(modifier = Modifier.height(30.dp))
          }
        }
      }
    }
  }

  // Save Dialog
  if (showSaveDialog) {
    SaveListDialog(
      defaultTitle = "Online Sites (${uiState.onlineSites.size}) - ${SimpleDateFormat("MMM dd HH:mm", Locale.getDefault()).format(Date())}",
      onlineCount = uiState.onlineSites.size,
      onSaveToRecord = { title ->
        viewModel.saveCurrentOnlineList(title) { entity ->
          Toast.makeText(context, "Saved to Record: ${entity.title}", Toast.LENGTH_SHORT).show()
          showSaveDialog = false
        }
      },
      onSaveToFile = {
        showSaveDialog = false
        launchSaveFile()
      },
      onDismiss = { showSaveDialog = false }
    )
  }
}

@Composable
private fun StatPill(
  label: String,
  count: Int,
  containerColor: Color,
  contentColor: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = containerColor,
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "$count",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = contentColor
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = contentColor.copy(alpha = 0.85f)
      )
    }
  }
}
