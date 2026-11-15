
package com.example.findduplicates.presentation

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.findduplicates.data.DeviceFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ScreenBackground = Color(0xFFF3F7FC)
private val PrimaryBlue = Color(0xFF3F51B5)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DuplicateGroupDetailScreen(
    viewModel: FileViewModel,
    groupIndex: Int,
    navController: NavController
) {
    val duplicateGroups by viewModel.duplicateGroups.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val group = duplicateGroups.getOrNull(groupIndex)

    var selectedIds by remember(groupIndex) {
        mutableStateOf<Set<Long>>(emptySet())
    }

    var showDeleteConfirmation by remember {
        mutableStateOf(false)
    }

    var isDeleting by remember {
        mutableStateOf(false)
    }

    // IDs and group identity are saved while the system confirmation
    // screen is open, so we don't rely on a potentially stale index.
    var pendingDeleteIds by remember {
        mutableStateOf<Set<Long>>(emptySet())
    }

    var originalGroupIds by remember {
        mutableStateOf<Set<Long>>(emptySet())
    }

    val pagerState = rememberPagerState(
        pageCount = { group?.size ?: 0 }
    )

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        isDeleting = false

        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.removeDeletedFiles(pendingDeleteIds)
            selectedIds = emptySet()

            // Read the latest groups after updating the ViewModel.
            val updatedGroups = viewModel.duplicateGroups.value

            val currentGroupRemains = updatedGroups.any { updatedGroup ->
                updatedGroup.any { file ->
                    file.id in originalGroupIds
                }
            }

            when {
                updatedGroups.isEmpty() -> {
                    Toast.makeText(
                        context,
                        "No duplicates found",
                        Toast.LENGTH_SHORT
                    ).show()

                    navController.navigate("home") {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                }

                !currentGroupRemains -> {
                    navController.popBackStack()
                }

                else -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "Images deleted successfully"
                        )
                    }
                }
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(
                    "Deletion cancelled"
                )
            }
        }

        pendingDeleteIds = emptySet()
        originalGroupIds = emptySet()
    }

    LaunchedEffect(group?.size) {
        val size = group?.size ?: 0

        if (size > 0 && pagerState.currentPage >= size) {
            pagerState.scrollToPage(size - 1)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .statusBarsPadding(),
        containerColor = ScreenBackground,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->

        if (group.isNullOrEmpty()) {
            LaunchedEffect(Unit) {
                navController.popBackStack()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        enabled = !isDeleting,
                        onClick = { navController.popBackStack() }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Duplicate Group ${groupIndex + 1}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF172B4D)
                        )

                        Text(
                            text = "${group.size} images remaining",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }

                    TextButton(
                        enabled = !isDeleting,
                        onClick = {
                            selectedIds =
                                if (group.all { it.id in selectedIds }) {
                                    emptySet()
                                } else {
                                    group.map { it.id }.toSet()
                                }
                        }
                    ) {
                        Text(
                            text = if (
                                group.all { it.id in selectedIds }
                            ) "Clear" else "Select all",
                            color = PrimaryBlue
                        )
                    }
                }

                Text(
                    text = "${pagerState.currentPage + 1} of ${group.size}",
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 8.dp),
                    color = PrimaryBlue,
                    fontWeight = FontWeight.SemiBold
                )

                // Swipe horizontally through the group's images.
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    pageSpacing = 12.dp,
                    key = { page -> group[page].id }
                ) { page ->
                    val file = group[page]
                    val isSelected = file.id in selectedIds

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = file.uri,
                                contentDescription = file.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            if (isDeleting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(40.dp),
                                    color = PrimaryBlue
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = file.displayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = formatFileSize(file.size),
                            fontSize = 13.sp,
                            color = Color.Gray
                        )

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                Color(0xFFE2E8FF)
                            } else {
                                Color.White
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    enabled = !isDeleting,
                                    onCheckedChange = { checked ->
                                        selectedIds =
                                            if (checked) {
                                                selectedIds + file.id
                                            } else {
                                                selectedIds - file.id
                                            }
                                    },
                                    colors = androidx.compose.material3.CheckboxDefaults.colors(
                                        checkedColor = HeaderBlue,
                                        uncheckedColor = Color.Gray,
                                        checkmarkColor = Color.White
                                    )
                                )

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Select for deletion",
                                        fontWeight = FontWeight.Medium
                                    )

                                    Text(
                                        text = if (isSelected) {
                                            "This image is selected"
                                        } else {
                                            "Keep this image"
                                        },
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                    }
                }

                // Page indicators
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(group.size.coerceAtMost(12)) { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(
                                    if (pagerState.currentPage == index) {
                                        8.dp
                                    } else {
                                        6.dp
                                    }
                                )
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == index) {
                                        PrimaryBlue
                                    } else {
                                        Color.LightGray
                                    }
                                )
                        )
                    }
                }

                Button(
                    onClick = { showDeleteConfirmation = true },
                    enabled = selectedIds.isNotEmpty() && !isDeleting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp)
                            ,
                    shape = RoundedCornerShape(14.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = HeaderBlue,
                        contentColor = Color.White
                    )
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp).padding(2.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(text ="Deleting...",modifier = Modifier.padding(2.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,modifier = Modifier.padding(2.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text("Delete Selected (${selectedIds.size})",modifier = Modifier.padding(2.dp))
                    }
                }
            }
        }
    }

    // Confirmation dialog
    if (showDeleteConfirmation && !group.isNullOrEmpty()) {
        val filesToDelete = group.filter {
            it.id in selectedIds
        }

        if (filesToDelete.isEmpty()) {
            showDeleteConfirmation = false
            return
        }

        showDeleteConfirmation = false
        isDeleting = true

        scope.launch {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Android 11+: system confirmation.
                    pendingDeleteIds = filesToDelete
                        .map { it.id }
                        .toSet()

                    originalGroupIds = group
                        .map { it.id }
                        .toSet()

                    val request = withContext(Dispatchers.IO) {
                        MediaStore.createDeleteRequest(
                            context.contentResolver,
                            filesToDelete.map { it.uri }
                        )
                    }

                    deleteLauncher.launch(
                        IntentSenderRequest.Builder(
                            request.intentSender
                        ).build()
                    )
                    // Keep isDeleting true until the launcher
                    // receives the result.
                } else {
                    // Older Android versions: direct deletion.
                    val result = deleteFilesDirect(
                        context = context,
                        files = filesToDelete
                    )

                    if (result.deletedIds.isNotEmpty()) {
                        val originalIds = group
                            .map { it.id }
                            .toSet()

                        viewModel.removeDeletedFiles(
                            result.deletedIds
                        )
                        selectedIds = emptySet()

                        val updatedGroups =
                            viewModel.duplicateGroups.value

                        val currentGroupRemains =
                            updatedGroups.any { updatedGroup ->
                                updatedGroup.any {
                                    it.id in originalIds
                                }
                            }

                        when {
                            updatedGroups.isEmpty() -> {
                                Toast.makeText(
                                    context,
                                    "No duplicates found",
                                    Toast.LENGTH_SHORT
                                ).show()

                                navController.navigate("home") {
                                    popUpTo(
                                        navController.graph
                                            .startDestinationId
                                    ) {
                                        inclusive = false
                                    }
                                    launchSingleTop = true
                                }
                            }

                            !currentGroupRemains -> {
                                navController.popBackStack()
                            }

                            else -> {
                                snackbarHostState.showSnackbar(
                                    "${result.deletedIds.size} image(s) deleted"
                                )
                            }
                        }
                    } else {
                        snackbarHostState.showSnackbar(
                            result.errorMessage
                                ?: "No images were deleted."
                        )
                    }

                    isDeleting = false
                }
            } catch (e: Exception) {
                isDeleting = false
                snackbarHostState.showSnackbar(
                    "Could not delete images. Please try again."
                )
            }
        }
//        AlertDialog(
//            onDismissRequest = {
//                if (!isDeleting) {
//                    showDeleteConfirmation = false
//                }
//            },
//            title = {
//                Text("Delete selected images?")
//            },
//            text = {
//                Text(
//                    "You selected ${selectedIds.size} image(s). " +
//                            "This action may not be recoverable."
//                )
//            },
//            confirmButton = {
//                TextButton(
//                    enabled = !isDeleting,
//                    onClick = {
//                        val filesToDelete = group.filter {
//                            it.id in selectedIds
//                        }
//
//                        if (filesToDelete.isEmpty()) {
//                            showDeleteConfirmation = false
//                            return@TextButton
//                        }
//
//                        showDeleteConfirmation = false
//                        isDeleting = true
//
//                        scope.launch {
//                            try {
//                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                                    // Android 11+: system confirmation.
//                                    pendingDeleteIds = filesToDelete
//                                        .map { it.id }
//                                        .toSet()
//
//                                    originalGroupIds = group
//                                        .map { it.id }
//                                        .toSet()
//
//                                    val request = withContext(Dispatchers.IO) {
//                                        MediaStore.createDeleteRequest(
//                                            context.contentResolver,
//                                            filesToDelete.map { it.uri }
//                                        )
//                                    }
//
//                                    deleteLauncher.launch(
//                                        IntentSenderRequest.Builder(
//                                            request.intentSender
//                                        ).build()
//                                    )
//                                    // Keep isDeleting true until the launcher
//                                    // receives the result.
//                                } else {
//                                    // Older Android versions: direct deletion.
//                                    val result = deleteFilesDirect(
//                                        context = context,
//                                        files = filesToDelete
//                                    )
//
//                                    if (result.deletedIds.isNotEmpty()) {
//                                        val originalIds = group
//                                            .map { it.id }
//                                            .toSet()
//
//                                        viewModel.removeDeletedFiles(
//                                            result.deletedIds
//                                        )
//                                        selectedIds = emptySet()
//
//                                        val updatedGroups =
//                                            viewModel.duplicateGroups.value
//
//                                        val currentGroupRemains =
//                                            updatedGroups.any { updatedGroup ->
//                                                updatedGroup.any {
//                                                    it.id in originalIds
//                                                }
//                                            }
//
//                                        when {
//                                            updatedGroups.isEmpty() -> {
//                                                Toast.makeText(
//                                                    context,
//                                                    "No duplicates found",
//                                                    Toast.LENGTH_SHORT
//                                                ).show()
//
//                                                navController.navigate("home") {
//                                                    popUpTo(
//                                                        navController.graph
//                                                            .startDestinationId
//                                                    ) {
//                                                        inclusive = false
//                                                    }
//                                                    launchSingleTop = true
//                                                }
//                                            }
//
//                                            !currentGroupRemains -> {
//                                                navController.popBackStack()
//                                            }
//
//                                            else -> {
//                                                snackbarHostState.showSnackbar(
//                                                    "${result.deletedIds.size} image(s) deleted"
//                                                )
//                                            }
//                                        }
//                                    } else {
//                                        snackbarHostState.showSnackbar(
//                                            result.errorMessage
//                                                ?: "No images were deleted."
//                                        )
//                                    }
//
//                                    isDeleting = false
//                                }
//                            } catch (e: Exception) {
//                                isDeleting = false
//                                snackbarHostState.showSnackbar(
//                                    "Could not delete images. Please try again."
//                                )
//                            }
//                        }
//                    }
//                ) {
//                    Text("Delete")
//                }
//            },
//            dismissButton = {
//                TextButton(
//                    enabled = !isDeleting,
//                    onClick = { showDeleteConfirmation = false }
//                ) {
//                    Text("Cancel")
//                }
//            }
//        )
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024L * 1024 * 1024 ->
            "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))

        bytes >= 1024L * 1024 ->
            "%.2f MB".format(bytes / (1024.0 * 1024))

        bytes >= 1024 ->
            "%.1f KB".format(bytes / 1024.0)

        else -> "$bytes B"
    }
}

private data class DirectDeleteResult(
    val deletedIds: Set<Long>,
    val errorMessage: String? = null
)

/**
 * Direct deletion helper for Android versions below API 30.
 * Android may still reject deletion if the app doesn't own the media.
 */
private suspend fun deleteFilesDirect(
    context: Context,
    files: List<DeviceFile>
): DirectDeleteResult = withContext(Dispatchers.IO) {
    val resolver: ContentResolver = context.contentResolver
    val deletedIds = mutableSetOf<Long>()
    var encounteredSecurityException = false

    for (file in files) {
        try {
            val rowsDeleted = resolver.delete(file.uri, null, null)

            if (rowsDeleted > 0) {
                deletedIds.add(file.id)
            }
        } catch (e: SecurityException) {
            encounteredSecurityException = true
        } catch (_: Exception) {
            // Continue attempting the remaining files.
        }
    }

    DirectDeleteResult(
        deletedIds = deletedIds,
        errorMessage = if (
            encounteredSecurityException && deletedIds.isEmpty()
        ) {
            "Android requires permission to delete these files."
        } else if (deletedIds.isEmpty()) {
            "No images could be deleted."
        } else {
            null
        }
    )
}
