package com.example.axs_player.ui.screens.more

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.axs_player.data.library.ImportState
import com.example.axs_player.ui.components.PrimaryButton
import com.example.axs_player.ui.theme.PageBackground
import com.example.axs_player.ui.theme.StorageCache
import com.example.axs_player.ui.theme.StorageData
import com.example.axs_player.ui.theme.StorageFree
import com.example.axs_player.ui.theme.StorageOther
import com.example.axs_player.ui.theme.TextBlack
import java.util.Locale

private val BAR_HEIGHT = 8.dp
private val MARKER_SIZE = 8.dp
private val ROW_HEIGHT = 21.dp
private val LABEL_LETTER_SPACING = (-1).sp
private const val BYTES_IN_GIGABYTE = 1_000_000_000.0

@Composable
fun DataSavingScreen(
    modifier: Modifier = Modifier,
    viewModel: DataSavingViewModel = viewModel(factory = DataSavingViewModel.Factory)
) {
    val usage by viewModel.usage.collectAsStateWithLifecycle()
    val treeUri by viewModel.treeUri.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    var isRemoveConfirmationVisible by rememberSaveable { mutableStateOf(false) }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { tree ->
        tree?.let(viewModel::selectFolder)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
            .padding(start = 10.dp, top = 60.dp, end = 10.dp)
    ) {
        StorageLabel(text = "data saver")
        Spacer(modifier = Modifier.height(30.dp))
        StorageBar(usage = usage)
        Spacer(modifier = Modifier.height(23.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StorageRow(color = StorageOther, label = "other app", bytes = usage.otherBytes)
            StorageRow(color = StorageData, label = "data", bytes = usage.dataBytes)
            StorageRow(color = StorageCache, label = "cache", bytes = usage.cacheBytes)
            StorageRow(color = StorageFree, label = "free", bytes = usage.freeBytes)
        }
        Spacer(modifier = Modifier.height(30.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(text = "remove all data", onClick = { isRemoveConfirmationVisible = true })
            PrimaryButton(text = "clear cache", onClick = viewModel::clearCache)
            if (treeUri == null) {
                PrimaryButton(text = "restore library folder", onClick = { folderPicker.launch(null) })
            } else {
                val isScanning = importState is ImportState.Running
                PrimaryButton(
                    text = if (isScanning) "scanning..." else "rescan",
                    onClick = viewModel::rescan,
                    enabled = !isScanning
                )
                PrimaryButton(text = "change folder", onClick = { folderPicker.launch(null) })
            }
            importStatus(importState)?.let {
                Text(
                    text = it,
                    color = TextBlack,
                    fontSize = 14.sp
                )
            }
        }
    }

    if (isRemoveConfirmationVisible) {
        AlertDialog(
            onDismissRequest = { isRemoveConfirmationVisible = false },
            title = { Text("remove all data?") },
            text = { Text("All track files, covers and metadata will be deleted from the device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        isRemoveConfirmationVisible = false
                        viewModel.removeAllData()
                    }
                ) {
                    Text("remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { isRemoveConfirmationVisible = false }) {
                    Text("cancel")
                }
            }
        )
    }
}

@Composable
private fun StorageBar(usage: StorageUsage) {
    val otherFraction = usage.otherFraction
    val dataFraction = usage.dataFraction
    val cacheFraction = usage.cacheFraction
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(BAR_HEIGHT)
    ) {
        drawRect(color = StorageFree)
        drawRect(
            color = StorageCache,
            size = Size(size.width * (otherFraction + dataFraction + cacheFraction), size.height)
        )
        drawRect(
            color = StorageData,
            size = Size(size.width * (otherFraction + dataFraction), size.height)
        )
        drawRect(color = StorageOther, size = Size(size.width * otherFraction, size.height))
    }
}

@Composable
private fun StorageRow(color: Color, label: String, bytes: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(
                modifier = Modifier
                    .size(MARKER_SIZE)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(10.dp))
            StorageLabel(text = label)
        }
        StorageLabel(text = formatGigabytes(bytes), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StorageLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = TextBlack,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = LABEL_LETTER_SPACING,
        modifier = modifier
    )
}

private fun importStatus(state: ImportState): String? = when (state) {
    is ImportState.Finished ->
        "imported ${state.result.imported} • skipped ${state.result.skipped} • failed ${state.result.failed}"
    ImportState.Failed -> "scan failed"
    ImportState.Idle, is ImportState.Running -> null
}

private fun formatGigabytes(bytes: Long): String =
    String.format(Locale.forLanguageTag("ru"), "%.1f GB", bytes / BYTES_IN_GIGABYTE)
