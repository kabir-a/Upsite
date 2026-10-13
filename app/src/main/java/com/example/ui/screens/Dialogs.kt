package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DefaultWebsites
import com.example.ui.theme.AppThemeMode
import com.example.viewmodel.SiteCheckerViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsDialog(
  currentTheme: AppThemeMode,
  currentConcurrency: Int,
  currentTimeout: Long,
  isUsingCustomList: Boolean,
  customListCount: Int,
  defaultListCount: Int,
  onThemeChanged: (AppThemeMode) -> Unit,
  onConcurrencyChanged: (Int) -> Unit,
  onTimeoutChanged: (Long) -> Unit,
  onUseCustomListChanged: (Boolean) -> Unit,
  onEditCustomList: () -> Unit,
  onOpenUpgrades: () -> Unit,
  onDismiss: () -> Unit
) {
  var concurrencyVal by remember { mutableFloatStateOf(currentConcurrency.toFloat()) }
  var timeoutVal by remember { mutableFloatStateOf(currentTimeout.toFloat()) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text("Settings & List Options", style = MaterialTheme.typography.titleLarge)
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
      ) {
        // Website List Source Section
        Text(
          text = "Website List Source",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(6.dp))

        // Default List Option
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onUseCustomListChanged(false) }
            .padding(vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          RadioButton(
            selected = !isUsingCustomList,
            onClick = { onUseCustomListChanged(false) }
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "Default BDIX Directory",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = if (!isUsingCustomList) FontWeight.Bold else FontWeight.Normal
            )
            Text(
              text = "$defaultListCount preloaded ISP / FTP portals",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.outline
            )
          }
        }

        // Custom List Option
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onUseCustomListChanged(true) }
            .padding(vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          RadioButton(
            selected = isUsingCustomList,
            onClick = { onUseCustomListChanged(true) }
          )
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "Custom Website List",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = if (isUsingCustomList) FontWeight.Bold else FontWeight.Normal
            )
            Text(
              text = if (customListCount > 0) "$customListCount custom site(s) configured" else "No custom sites set yet (tap below to edit)",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.outline
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
          onClick = onEditCustomList,
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (isUsingCustomList) "Edit Custom Website List" else "Provide Custom Website List")
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Short Format Sample Card
        TextFileFormatSampleCard()

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        // App Theme Section
        Text(
          text = "App Theme",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(6.dp))

        ThemeOptionRow(
          label = "System Default",
          icon = Icons.Default.BrightnessAuto,
          selected = currentTheme == AppThemeMode.SYSTEM,
          onClick = { onThemeChanged(AppThemeMode.SYSTEM) }
        )
        ThemeOptionRow(
          label = "Light Mode",
          icon = Icons.Default.LightMode,
          selected = currentTheme == AppThemeMode.LIGHT,
          onClick = { onThemeChanged(AppThemeMode.LIGHT) }
        )
        ThemeOptionRow(
          label = "Dark Mode",
          icon = Icons.Default.DarkMode,
          selected = currentTheme == AppThemeMode.DARK,
          onClick = { onThemeChanged(AppThemeMode.DARK) }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        // Concurrency
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Speed,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Concurrent Workers",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold
            )
          }
          Text(
            text = "${concurrencyVal.roundToInt()} parallel",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
          )
        }
        Slider(
          value = concurrencyVal,
          onValueChange = {
            concurrencyVal = it
            onConcurrencyChanged(it.roundToInt())
          },
          valueRange = 4f..30f,
          steps = 25,
          modifier = Modifier.testTag("concurrency_slider")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Timeout
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Timer,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Connection Timeout",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold
            )
          }
          Text(
            text = "${timeoutVal.roundToInt()}s",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
          )
        }
        Slider(
          value = timeoutVal,
          onValueChange = {
            timeoutVal = it
            onTimeoutChanged(it.roundToInt().toLong())
          },
          valueRange = 2f..12f,
          steps = 9,
          modifier = Modifier.testTag("timeout_slider")
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        // Upgrades Callout Card
        Card(
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenUpgrades() }
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Lightbulb,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Explore Suggested Upgrades",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "FTP port 21 probes, WorkManager background monitor...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss) {
        Text("Done")
      }
    }
  )
}

@Composable
fun TextFileFormatSampleCard(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val sampleText = """# One URL or IP per line
http://server1.ftpbd.net
http://103.58.73.6
http://discoveryftp.net
http://172.16.50.4
https://cloudflare.com"""

  Card(
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ),
    shape = RoundedCornerShape(10.dp),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Text File Format Sample",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
          )
        }
        IconButton(
          onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Sample URLs", sampleText))
            Toast.makeText(context, "Sample copied to clipboard", Toast.LENGTH_SHORT).show()
          },
          modifier = Modifier.size(24.dp)
        ) {
          Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = "Copy Sample",
            modifier = Modifier.size(14.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "• Enter one website URL or IP per line\n• 'http://' or 'https://' prefix is optional\n• Lines starting with # are comments",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp
      )

      Spacer(modifier = Modifier.height(6.dp))
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = sampleText,
          style = MaterialTheme.typography.bodySmall.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            lineHeight = 15.sp
          ),
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(8.dp)
        )
      }
    }
  }
}

@Composable
private fun ThemeOptionRow(
  label: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  selected: Boolean,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    RadioButton(
      selected = selected,
      onClick = onClick
    )
    Spacer(modifier = Modifier.width(8.dp))
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(20.dp)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
    )
  }
}

@Composable
fun SaveListDialog(
  defaultTitle: String,
  onlineCount: Int,
  onSaveToRecord: (title: String) -> Unit,
  onSaveToFile: () -> Unit,
  onDismiss: () -> Unit
) {
  var title by remember { mutableStateOf(defaultTitle) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text("Save Online Sites List")
    },
    text = {
      Column {
        Text(
          text = "You are saving $onlineCount currently available website(s).",
          style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("List Name / Note") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("save_list_title_input"),
          singleLine = true
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = "Options:",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "• Save to in-app Record History for later re-testing and review.\n• Save as local .txt file to your device Downloads/Documents.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSaveToRecord(title)
        },
        modifier = Modifier.testTag("confirm_save_record_btn")
      ) {
        Text("Save Record")
      }
    },
    dismissButton = {
      Row {
        OutlinedButton(
          onClick = {
            onSaveToFile()
          },
          modifier = Modifier.testTag("save_to_local_file_btn")
        ) {
          Text("Save to .TXT")
        }
        Spacer(modifier = Modifier.width(8.dp))
        TextButton(onClick = onDismiss) {
          Text("Cancel")
        }
      }
    }
  )
}

@Composable
fun ManageUrlsDialog(
  currentUrls: List<String>,
  isUsingCustomList: Boolean,
  onSaveUrls: (List<String>) -> Unit,
  onResetDefault: () -> Unit,
  onDismiss: () -> Unit
) {
  var rawText by remember { mutableStateOf(currentUrls.joinToString("\n")) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(if (isUsingCustomList) "Custom Website List" else "Manage Target Websites")
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Total: ${rawText.lines().count { it.isNotBlank() && !it.startsWith("#") }} sites",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          TextButton(
            onClick = { rawText = SiteCheckerViewModel.SAMPLE_URL_TEXT },
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
          ) {
            Text("Insert Sample", fontSize = 12.sp, maxLines = 1)
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
          value = rawText,
          onValueChange = { rawText = it },
          label = { Text("Websites List") },
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp, max = 280.dp)
            .testTag("edit_urls_textarea")
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextButton(
            onClick = {
              rawText = DefaultWebsites.LIST.joinToString("\n")
            },
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
          ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Reset to Default", fontSize = 12.sp, maxLines = 1)
          }

          TextButton(
            onClick = { rawText = "" },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text("Clear", fontSize = 12.sp, maxLines = 1)
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val parsed = SiteCheckerViewModel.parseUrlsFromText(rawText)
          onSaveUrls(parsed)
          onDismiss()
        }
      ) {
        Text("Save & Apply")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun UpgradesDialog(
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Lightbulb,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text("Suggested Upgrades")
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
      ) {
        Text(
          text = "Here are recommended next-level upgrades tailored for ISP / BDIX FTP uptime auditing:",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        UpgradeCard(
          title = "1. Raw Socket FTP Port 21 Probing",
          detail = "Many local ISP and BDIX servers run dedicated FTP (port 21) or SMB (port 445) services without an HTTP web server. Adding TCP port sockets allows checking servers that don't respond on HTTP port 80."
        )

        UpgradeCard(
          title = "2. Scheduled Background WorkManager",
          detail = "Run recurring background health checks (e.g., every 30 minutes) and send Android notifications when high-priority media servers recover or go down."
        )

        UpgradeCard(
          title = "3. ISP & BDIX Route Verification",
          detail = "Detect whether your current Wi-Fi connection is actively on the Bangladesh Internet Exchange (BDIX) or routing through international bandwidth via traceroute."
        )

        UpgradeCard(
          title = "4. Streaming Playlist (.M3U) Export",
          detail = "Export online IPTV / movie streaming portals directly as an M3U playlist file for VLC or MX Player."
        )

        UpgradeCard(
          title = "5. Latency Jitter & Response Graphing",
          detail = "Track response time trends with interactive charts to identify ISP throttling during peak evening hours."
        )
      }
    },
    confirmButton = {
      Button(onClick = onDismiss) {
        Text("Got It")
      }
    }
  )
}

@Composable
private fun UpgradeCard(title: String, detail: String) {
  Card(
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ),
    shape = RoundedCornerShape(10.dp),
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )
      Spacer(modifier = Modifier.height(3.dp))
      Text(
        text = detail,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
