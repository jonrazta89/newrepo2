package com.EternalTimes.GhostKey.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.EternalTimes.GhostKey.ui.theme.CvePalette
import com.EternalTimes.GhostKey.viewmodel.ExploitViewModel
import com.EternalTimes.GhostKey.viewmodel.ManagerOption

/**
 * 设置页：内核 Profile 选择 / 导入（IMG·JSON·HOCON）/ 导出（文件名含版本号）/
 * 已导入管理 / Root 管理器检测 / 运行日志入口。
 *
 * 颜色与风格完全沿用 CvePalette 与 MaterialTheme，与主页保持一致。
 */
@Composable
fun SettingsScreen(
    viewModel: ExploitViewModel,
    onBack: () -> Unit,
    onOpenLogs: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showProfilePicker by remember { mutableStateOf(false) }
    var showManagerPicker by remember { mutableStateOf(false) }
    var pendingExport by remember { mutableStateOf("") }

    val imgPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importImageFromUri(uri, queryDisplayName(context, uri))
    }
    val textPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importTextFromUri(uri, queryDisplayName(context, uri))
    }
    val createDoc = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(pendingExport.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                Toast.makeText(context, "Exported ${queryDisplayName(context, uri)}", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Export failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val stored = viewModel.storedProfiles()
    val selected = uiState.selectedProfileRelease

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CvePalette.Backdrop)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) {
                Text("Back", color = CvePalette.Accent, style = MaterialTheme.typography.labelLarge)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Settings", style = MaterialTheme.typography.titleLarge, color = CvePalette.TextPrimary)
                Text(
                    "Kernel Profile · Import / Export / Manager",
                    style = MaterialTheme.typography.labelSmall,
                    color = CvePalette.TextMuted,
                )
            }
        }
        HorizontalDivider(color = CvePalette.Outline, thickness = 1.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsCard(title = "Target Kernel Profile") {
                Text(
                    "Current: " + (selected ?: "Auto-adapt (current device)"),
                    color = CvePalette.TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Device kernel: ${uiState.kernelRelease}",
                    color = CvePalette.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Matched: ${uiState.matchedProfile}",
                    color = CvePalette.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showProfilePicker = true }) {
                        Text("Select Kernel")
                    }
                    OutlinedButton(onClick = {
                        pendingExport = viewModel.buildExportText()
                        createDoc.launch(viewModel.exportFileName())
                    }) {
                        Text("Export Current Profile")
                    }
                }
                Text(
                    "Exported filename automatically includes version (e.g. ...-v${appVersion(context)}.conf)",
                    color = CvePalette.TextMuted,
                    style = MaterialTheme.typography.labelSmall,
                )
            }

            SettingsCard(title = "Import Kernel File") {
                Text(
                    "Supports boot.img (auto-extract BTF struct offsets + kallsyms symbol addresses) and JSON / HOCON profiles." +
                        "GKI 6.6 kernels can auto-complete all fields; missing symbols will be listed for completion.",
                    color = CvePalette.TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(onClick = { imgPicker.launch(arrayOf("*/*")) }) {
                    Text("Import IMG (boot.img)")
                }
                OutlinedButton(onClick = {
                    textPicker.launch(
                        arrayOf("application/json", "text/plain", "application/octet-stream", "*/*"),
                    )
                }) {
                    Text("Import JSON / HOCON")
                }
                if (uiState.importMessage.isNotBlank()) {
                    val color = when {
                        uiState.importMessage.contains("失败") || uiState.importMessage.contains("Failed") -> CvePalette.Danger
                        uiState.importMessage.contains("缺少") || uiState.importMessage.contains("Missing") -> CvePalette.Warning
                        else -> CvePalette.Success
                    }
                    Text(
                        uiState.importMessage,
                        color = color,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            SettingsCard(title = "Imported Profiles (${stored.size})") {
                if (stored.isEmpty()) {
                    Text("None", color = CvePalette.TextMuted, style = MaterialTheme.typography.bodySmall)
                } else {
                    stored.forEach { sp ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    sp.release,
                                    color = CvePalette.TextPrimary,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    if (sp.complete) "Fields complete" else "Missing: ${sp.missing.joinToString()}",
                                    color = if (sp.complete) CvePalette.Success else CvePalette.Warning,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            TextButton(onClick = { viewModel.deleteStoredProfile(sp.fileName) }) {
                                Text("Delete", color = CvePalette.Danger, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        HorizontalDivider(color = CvePalette.Outline, thickness = 1.dp)
                    }
                }
            }

            SettingsCard(title = "Root Manager (Auto-adapt)") {
                val cands = uiState.managerCandidates
                /*
                 * 检测是异步的，首屏拿到的是「还没扫完」的中间态。
                 * 不区分「还在查」和「查了没有」，用户会把前者读成后者，
                 * 于是以为「检测不到管理器」。
                 */
                val scanning = uiState.managerScanning
                Text(
                    "Detected: " + when {
                        scanning -> "Scanning..."
                        uiState.managerScanError.isNotBlank() -> "Detection failed (${uiState.managerScanError})"
                        else -> uiState.detectedManager.ifBlank { "Not detected" }
                    },
                    color = if (uiState.managerScanError.isNotBlank() && !scanning) {
                        CvePalette.Warning
                    } else {
                        CvePalette.TextPrimary
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "KSU Source: " + when {
                        scanning -> "Scanning..."
                        else -> uiState.ksuSourceLabel.ifBlank { "Bundled ksud" }
                    },
                    color = CvePalette.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { viewModel.detectKsuSource(force = true) }) {
                        Text("Re-detect")
                    }
                    if (cands.size > 1) {
                        Button(onClick = { showManagerPicker = true }) {
                            Text("Select Manager")
                        }
                    }
                }
                if (cands.size > 1) {
                    Text(
                        "Current selection: " + (uiState.selectedManagerPackage ?: "Auto (highest priority)"),
                        color = CvePalette.TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "Detected ${cands.size} managers. Takeover will load the ksud bundled with the selected manager," +
                            "keeping the kernel module in sync with the installed manager (otherwise some managers won't recognize escalation or show auth prompts).",
                        color = CvePalette.TextMuted,
                        style = MaterialTheme.typography.labelSmall,
                    )
                } else {
                    Text(
                        "During escalation, ksud is preferentially extracted from the detected manager above (identified by content signature, works even with renamed/repackaged files)," +
                            "falling back to bundled APK ksud if not detected. Manager upgrades provide new kernel KMI support without updating this app.",
                        color = CvePalette.TextMuted,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            SettingsCard(title = "Logs") {
                /* 次数由 ViewModel 持有并随增删刷新；之前这里用
                   remember { viewModel.listRunLogs().size }，只算一次且在主线程做磁盘 IO，
                   跑完一轮 ROOT 回来看到的还是旧数字。 */
                val runCount = uiState.runLogCount
                OutlinedButton(onClick = onOpenLogs) {
                    Text("View Run History ($runCount runs)")
                }
                Text(
                    "Each ROOT run saves a separate record; tap to view the complete log for that run",
                    color = CvePalette.TextMuted,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }

    if (showProfilePicker) {
        ProfilePickerDialog(
            options = listOf<String?>(null) + viewModel.knownReleases(),
            selected = selected,
            onPick = { r ->
                viewModel.selectProfile(r)
                showProfilePicker = false
            },
            onDismiss = { showProfilePicker = false },
        )
    }

    if (showManagerPicker) {
        ManagerPickerDialog(
            options = uiState.managerCandidates,
            selected = uiState.selectedManagerPackage,
            onPick = { pkg ->
                viewModel.selectManager(pkg)
                showManagerPicker = false
            },
            onDismiss = { showManagerPicker = false },
        )
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CvePalette.Surface, RoundedCornerShape(12.dp))
            .border(1.dp, CvePalette.Outline, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = CvePalette.TextPrimary)
        content()
    }
}

@Composable
private fun ProfilePickerDialog(
    options: List<String?>,
    selected: String?,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = CvePalette.TextMuted)
            }
        },
        title = {
            Text("Select Target Kernel Profile", color = CvePalette.TextPrimary)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { opt ->
                    val isSelected = opt == selected
                    val label = opt ?: "Auto-adapt (current device)"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(opt) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (isSelected) "●" else "○",
                            color = if (isSelected) CvePalette.Accent else CvePalette.TextMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            label,
                            color = CvePalette.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    HorizontalDivider(color = CvePalette.Outline, thickness = 1.dp)
                }
            }
        },
        containerColor = CvePalette.Surface,
    )
}

/** 多管理器选择弹窗：null = 自动（优先级首个），否则按选中包名接管。 */
@Composable
private fun ManagerPickerDialog(
    options: List<ManagerOption>,
    selected: String?,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val rows = listOf<ManagerOption?>(null) + options
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = CvePalette.TextMuted)
            }
        },
        title = {
            Text("Select Root Manager for Takeover", color = CvePalette.TextPrimary)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                rows.forEach { opt ->
                    val pkg = opt?.packageName
                    val isSelected = pkg == selected
                    val label = opt?.label ?: "Auto (highest priority)"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(pkg) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (isSelected) "●" else "○",
                            color = if (isSelected) CvePalette.Accent else CvePalette.TextMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            label,
                            color = CvePalette.TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    HorizontalDivider(color = CvePalette.Outline, thickness = 1.dp)
                }
            }
        },
        containerColor = CvePalette.Surface,
    )
}

/** 查询 SAF Uri 的显示名（失败则回退到最后路径段）。 */
private fun queryDisplayName(context: Context, uri: Uri): String {
    val proj = arrayOf(android.provider.OpenableColumns.DISPLAY_NAME)
    val name = runCatching {
        context.contentResolver.query(uri, proj, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) c.getString(idx) else null
            } else null
        }
    }.getOrNull()
    return name ?: uri.lastPathSegment ?: "imported"
}

private fun appVersion(context: Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
}.getOrNull() ?: "unknown"
