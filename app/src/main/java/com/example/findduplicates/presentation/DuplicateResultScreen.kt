package com.example.findduplicates.presentation

import android.app.Activity
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.findduplicates.data.DeviceFile
import com.example.findduplicates.data.DuplicateCategory
import com.example.findduplicates.data.DuplicateGroup
import com.example.findduplicates.presentation.components.MediaPreviewDialog
import com.example.findduplicates.ui.theme.ErrorRose
import com.example.findduplicates.ui.theme.SuccessGreen
import com.example.findduplicates.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateResultScreen(
    viewModel: FileViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Exact Photos", "Similar Photos", "Exact Videos", "Similar Videos")

    val exactPhotos by viewModel.exactPhotoGroups.collectAsState()
    val similarPhotos by viewModel.similarPhotoGroups.collectAsState()
    val exactVideos by viewModel.exactVideoGroups.collectAsState()
    val similarVideos by viewModel.similarVideoGroups.collectAsState()

    val selectedFileIds by viewModel.selectedFileIds.collectAsState()
    val selectedRecoverableBytes by viewModel.selectedRecoverableBytes.collectAsState()

    var previewFile by remember { mutableStateOf<DeviceFile?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Scoped storage deletion launcher
    val deleteIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.updateGroupsAfterDeletion(selectedFileIds)
            Toast.makeText(context, "Selected files deleted safely", Toast.LENGTH_SHORT).show()
        }
    }

    val currentCategory = when (selectedTabIndex) {
        0 -> DuplicateCategory.EXACT_PHOTO
        1 -> DuplicateCategory.SIMILAR_PHOTO
        2 -> DuplicateCategory.EXACT_VIDEO
        else -> DuplicateCategory.SIMILAR_VIDEO
    }

    val currentGroups = when (selectedTabIndex) {
        0 -> exactPhotos
        1 -> similarPhotos
        2 -> exactVideos
        else -> similarVideos
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Duplicate Results",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.selectAllRecommendedDuplicates(currentCategory) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.SelectAll,
                            contentDescription = "Select Recommended",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (selectedFileIds.isNotEmpty()) {
                        IconButton(onClick = { viewModel.deselectAll() }) {
                            Icon(
                                imageVector = Icons.Default.Deselect,
                                contentDescription = "Deselect All"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            AnimatedVisibility(visible = selectedFileIds.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 12.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${selectedFileIds.size} files selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Recover ${viewModel.formatFileSize(selectedRecoverableBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SuccessGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = { showDeleteConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRose),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Delete", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Category Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabTitles.forEachIndexed { index, title ->
                    val count = when (index) {
                        0 -> exactPhotos.size
                        1 -> similarPhotos.size
                        2 -> exactVideos.size
                        else -> similarVideos.size
                    }

                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (count > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Badge(
                                        containerColor = if (selectedTabIndex == index)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(text = count.toString())
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Results Content
            if (currentGroups.isEmpty()) {
                EmptyStateView(categoryTitle = tabTitles[selectedTabIndex])
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = currentGroups,
                        key = { it.id }
                    ) { group ->
                        DuplicateGroupCard(
                            group = group,
                            selectedIds = selectedFileIds,
                            onToggleSelection = { viewModel.toggleFileSelection(it) },
                            onSelectGroup = { viewModel.selectAllDuplicatesInGroup(group) },
                            onDeselectGroup = { viewModel.deselectGroup(group) },
                            onPreviewFile = { previewFile = it },
                            formatFileSize = { viewModel.formatFileSize(it) }
                        )
                    }
                }
            }
        }
    }

    // Media Preview Dialog
    previewFile?.let { file ->
        MediaPreviewDialog(
            file = file,
            formattedSize = viewModel.formatFileSize(file.size),
            onDismiss = { previewFile = null }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(text = "Confirm Deletion", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Are you sure you want to delete ${selectedFileIds.size} files?\n\n" +
                            "This will free up ${viewModel.formatFileSize(selectedRecoverableBytes)} of storage. " +
                            "Files will be deleted via Android MediaStore safely."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        val deleteIntent = viewModel.createDeleteIntent(context, selectedFileIds)
                        if (deleteIntent != null) {
                            deleteIntentLauncher.launch(
                                IntentSenderRequest.Builder(deleteIntent.intentSender).build()
                            )
                        } else {
                            // Pre-Android 11 fallback or direct deletion
                            viewModel.updateGroupsAfterDeletion(selectedFileIds)
                            Toast.makeText(context, "Files deleted", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRose)
                ) {
                    Text(text = "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(text = "Cancel")
                }
            }
        )
    }
}

@Composable
fun DuplicateGroupCard(
    group: DuplicateGroup,
    selectedIds: Set<Long>,
    onToggleSelection: (Long) -> Unit,
    onSelectGroup: () -> Unit,
    onDeselectGroup: () -> Unit,
    onPreviewFile: (DeviceFile) -> Unit,
    formatFileSize: (Long) -> String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Group Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (group.category == DuplicateCategory.SIMILAR_PHOTO ||
                            group.category == DuplicateCategory.SIMILAR_VIDEO
                        ) Icons.Default.AutoAwesome else Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (group.category == DuplicateCategory.SIMILAR_PHOTO)
                            WarningAmber else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${group.files.size} Items",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    if (group.similarityScore < 1.0f) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = WarningAmber.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${(group.similarityScore * 100).toInt()}% Match",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = WarningAmber
                            )
                        }
                    }
                }

                Text(
                    text = "Recover ${formatFileSize(group.recoverableSize)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = SuccessGreen
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Media Items Row / Grid
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(group.files, key = { it.id }) { file ->
                    val isRecommended = file.id == group.recommendedKeepId
                    val isSelected = selectedIds.contains(file.id)

                    MediaThumbnailItem(
                        file = file,
                        isRecommended = isRecommended,
                        isSelected = isSelected,
                        formattedSize = formatFileSize(file.size),
                        onClick = { onPreviewFile(file) },
                        onToggleSelection = { onToggleSelection(file.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Group Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDeselectGroup) {
                    Text(text = "Deselect Group", style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.width(4.dp))
                OutlinedButton(
                    onClick = onSelectGroup,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(text = "Select Duplicates", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun MediaThumbnailItem(
    file: DeviceFile,
    isRecommended: Boolean,
    isSelected: Boolean,
    formattedSize: String,
    onClick: () -> Unit,
    onToggleSelection: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(130.dp, 160.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = if (isSelected) 2.5.dp else if (isRecommended) 2.dp else 0.5.dp,
                color = if (isSelected) ErrorRose else if (isRecommended) SuccessGreen else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
    ) {
        // Thumbnail Image
        AsyncImage(
            model = file.uri,
            contentDescription = file.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient overlay at bottom for readable text
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(48.dp)
                .background(Color.Black.copy(alpha = 0.55f))
        )

        // File Details Overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(6.dp)
        ) {
            Text(
                text = formattedSize,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            if (file.resolution.isNotEmpty()) {
                Text(
                    text = file.resolution,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // Top Header: Checkbox and Recommended Badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            if (isRecommended) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SuccessGreen
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "KEEP",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() },
                modifier = Modifier.size(24.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = ErrorRose,
                    uncheckedColor = Color.White.copy(alpha = 0.85f),
                    checkmarkColor = Color.White
                )
            )
        }

        // Video Play Indicator
        if (file.isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(32.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Video",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyStateView(categoryTitle: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = SuccessGreen
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No $categoryTitle Found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Your media library is clean. No duplicate or similar items were detected in this category.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}