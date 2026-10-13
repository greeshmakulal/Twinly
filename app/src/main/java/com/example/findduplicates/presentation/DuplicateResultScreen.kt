package com.example.findduplicates.presentation

import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.example.findduplicates.Greeting
import com.example.findduplicates.data.DeviceFile
import com.example.findduplicates.data.DuplicateFile
import com.example.findduplicates.data.local.AppDatabase
import com.example.findduplicates.repository.FileRepository
import com.example.findduplicates.ui.theme.FindDuplicatesTheme
import kotlinx.coroutines.flow.distinctUntilChanged

private const val GROUPS_PER_PAGE = 20

@Composable
fun DuplicateResultScreen(viewModel: FileViewModel, navController: NavController) {
    val duplicateGroups by viewModel.duplicateGroups.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanStage by viewModel.scanStage.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val scanTotal by viewModel.scanTotal.collectAsState()

    val exactCount by viewModel.exactDuplicateCount.collectAsState()
    val similarCount by viewModel.similarDuplicateCount.collectAsState()

    // Limit selection state (null means All)
    var selectedLimit by remember { mutableStateOf<Int?>(500) }
    val selectedFiles = remember { mutableStateListOf<DeviceFile>() }
    val selectedSize = selectedFiles.sumOf { it.size }
    val context = LocalContext.current
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val deletedIds = selectedFiles.map { it.id }.toSet()
            selectedFiles.clear()
//            onDeleted(deletedIds)
            Toast.makeText(context, "Files deleted successfully", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),

        topBar = {
            TopBar(onBackClick = { navController.popBackStack() })
        }
        , bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = Color.Blue,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "${selectedFiles.size} Selected", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(text = formatFileSize(selectedSize), color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val mediaUris = selectedFiles.map { it.uri }
                            if (mediaUris.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                val pendingIntent = MediaStore.createDeleteRequest(
                                    context.contentResolver,
                                    mediaUris
                                )
                                deleteLauncher.launch(
                                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                                )
                            }

                        },
                        enabled = selectedFiles.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red,
                            disabledContainerColor = Color.Gray
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Selected", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        MainContent(
            modifier = Modifier.padding(innerPadding),
            selectedFiles = selectedFiles,
            groups = duplicateGroups,
            isScanning = isScanning,
            scanStage = scanStage,
            scanProgress = scanProgress,
            scanTotal = scanTotal,
            exactCount = exactCount,
            similarCount = similarCount,
            selectedLimit = selectedLimit,
            onLimitChanged = { selectedLimit = it },
            onStartScan = { viewModel.scanForDuplicates(selectedLimit) },
            onStopScan = { viewModel.stopScan() },
            onDeleted = { deletedIds ->
                viewModel.removeDeletedFiles(deletedIds)
            }
        )
    }
}

@Composable
fun TopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Blue)
            .statusBarsPadding()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick, modifier = Modifier.weight(0.15f)) {
            Icon(
                imageVector = Icons.Default.ArrowBackIosNew,
                contentDescription = "Back",
                tint = Color.White
            )
        }
        Text(
            text = "Duplicate file scanner",
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(0.85f),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun MainContent(
    modifier: Modifier,
    groups: List<List<DeviceFile>> = emptyList(),
    isScanning: Boolean,
    selectedFiles: MutableList<DeviceFile>,
    scanStage: String,
    scanProgress: Int,
    scanTotal: Int,
    exactCount: Int,
    similarCount: Int,
    selectedLimit: Int?,
    onLimitChanged: (Int?) -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onDeleted: (Set<Long>) -> Unit
) {

println("duplicate groups: ${groups.take(50)}")


    val sampleDuplicateGroups: List<List<DeviceFile>> = listOf(
        listOf(
            DeviceFile(
                id = 1000025139L,
                displayName = "IMG_1084.jpg",
                size = 9387024L,
                uri = Uri.parse(
                    "content://media/external/images/media/1000025139"
                ),
                mimeType = "image/jpeg",
                dateModified = 1717351012L
            ),
            DeviceFile(
                id = 1000025140L,
                displayName = "IMG_1086.jpg",
                size = 9715343L,
                uri = Uri.parse(
                    "content://media/external/images/media/1000025140"
                ),
                mimeType = "image/jpeg",
                dateModified = 1717351017L
            ),
            DeviceFile(
                id = 1000025141L,
                displayName = "IMG_1085.jpg",
                size = 9718730L,
                uri = Uri.parse(
                    "content://media/external/images/media/1000025141"
                ),
                mimeType = "image/jpeg",
                dateModified = 1717351019L
            )
        ),
        listOf(
            DeviceFile(
                id = 1000025144L,
                displayName = "IMG_1171.jpg",
                size = 9204660L,
                uri = Uri.parse(
                    "content://media/external/images/media/1000025144"
                ),
                mimeType = "image/jpeg",
                dateModified = 1717351055L
            )
        )
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Blue)
    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(12.dp),
//            verticalArrangement = Arrangement.spacedBy(12.dp)
//        ) {
//            // Control & Live Metrics Card
//            Card(
//                modifier = Modifier.fillMaxWidth(),
//                shape = RoundedCornerShape(16.dp),
//                colors = CardDefaults.cardColors(containerColor = Color.White),
//                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
//            ) {
//                Column(modifier = Modifier.padding(16.dp)) {
//                    Text("Scan Limit", fontWeight = FontWeight.Bold, fontSize = 14.sp)
//                    Spacer(modifier = Modifier.height(8.dp))
//
//                    Row(
//                        horizontalArrangement = Arrangement.spacedBy(6.dp),
//                        modifier = Modifier.fillMaxWidth()
//                    ) {
//                        limitOptions.forEach { limit ->
//                            val label = limit?.let { "$it" } ?: "All"
//                            FilterChip(
//                                selected = selectedLimit == limit,
//                                onClick = { if (!isScanning) onLimitChanged(limit) },
//                                label = { Text(label, fontSize = 12.sp) },
//                                modifier = Modifier.weight(1f)
//                            )
//                        }
//                    }
//
//                    Spacer(modifier = Modifier.height(12.dp))
//                    HorizontalDivider(color = Color(0xFFEAEAEA))
//                    Spacer(modifier = Modifier.height(12.dp))
//
//                    // Live Counters Row
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.SpaceBetween
//                    ) {
//                        Column {
//                            Text("Exact Matches", fontSize = 12.sp, color = Color.Gray)
//                            Text("$exactCount groups", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1976D2))
//                        }
//                        Column {
//                            Text("Similar Groups", fontSize = 12.sp, color = Color.Gray)
//                            Text("$similarCount groups", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF388E3C))
//                        }
//                        Column {
//                            Text("Total Found", fontSize = 12.sp, color = Color.Gray)
//                            Text("${groups.size} sets", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
//                        }
//                    }
//
//                    if (isScanning) {
//                        Spacer(modifier = Modifier.height(12.dp))
//                        Text(scanStage, fontSize = 13.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
//                        Spacer(modifier = Modifier.height(6.dp))
//                        if (scanTotal > 0) {
//                            LinearProgressIndicator(
//                                progress = { scanProgress.toFloat() / scanTotal.toFloat() },
//                                modifier = Modifier.fillMaxWidth()
//                            )
//                            Spacer(modifier = Modifier.height(4.dp))
//                            Text("$scanProgress / $scanTotal processed", fontSize = 11.sp, color = Color.Gray)
//                        }
//                    }
//
//                    Spacer(modifier = Modifier.height(12.dp))
//
//                    // Action Button (Start / Stop)
//                    if (!isScanning) {
//                        Button(
//                            onClick = onStartScan,
//                            modifier = Modifier.fillMaxWidth(),
//                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
//                        ) {
//                            Text("Start Smart Scan", fontWeight = FontWeight.Bold)
//                        }
//                    } else {
//                        Button(
//                            onClick = onStopScan,
//                            modifier = Modifier.fillMaxWidth(),
//                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
//                        ) {
//                            Text("Stop Scan", fontWeight = FontWeight.Bold)
//                        }
//                    }
//                }
//            }
        Surface(modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),

            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            // Results List Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    


            ) {
                if (groups.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isScanning) "Scanning gallery..." else "No duplicate groups found yet.\nTap 'Start Smart Scan' above.",
                            textAlign = TextAlign.Center,
                            color = Color.DarkGray
                        )
                    }
                } else {
                    DuplicateItemsList(
                        groups = groups,
                        selectedFiles = selectedFiles,
                        onSelectionChange = { file, checked ->
                            if (checked) {
                                if (!selectedFiles.contains(file)) selectedFiles.add(file)
                            } else {
                                selectedFiles.remove(file)
                            }
                        }
                    )
                }
            }
        }

            // Bottom Deletion Bar
//            Surface(
//                modifier = Modifier.fillMaxWidth(),
//                shape = RoundedCornerShape(20.dp),
//                color = Color.Blue,
//                shadowElevation = 6.dp
//            ) {
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(horizontal = 16.dp, vertical = 12.dp),
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.SpaceBetween
//                ) {
//                    Column {
//                        Text(text = "${selectedFiles.size} Selected", color = Color.White, fontWeight = FontWeight.Bold)
//                        Text(text = formatFileSize(selectedSize), color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
//                    }
//
//                    Button(
//                        onClick = {
//                            val mediaUris = selectedFiles.map { it.uri }
//                            if (mediaUris.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                                val pendingIntent = MediaStore.createDeleteRequest(
//                                    context.contentResolver,
//                                    mediaUris
//                                )
//                                deleteLauncher.launch(
//                                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
//                                )
//                            }
//                        },
//                        enabled = selectedFiles.isNotEmpty(),
//                        colors = ButtonDefaults.buttonColors(
//                            containerColor = Color.Red,
//                            disabledContainerColor = Color.Gray
//                        )
//                    ) {
//                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Color.White)
//                        Spacer(modifier = Modifier.width(6.dp))
//                        Text("Delete Selected", color = Color.White, fontWeight = FontWeight.Bold)
//                    }
//                }
//            }
        }
    }
//}

@Composable
fun DuplicateItemsList(
    groups: List<List<DeviceFile>>,
    selectedFiles: List<DeviceFile>,
    onSelectionChange: (DeviceFile, Boolean) -> Unit
) {
    val listState = rememberLazyListState()
    var visibleGroupCount by remember(groups) {
        mutableStateOf(minOf(GROUPS_PER_PAGE, groups.size))
    }

    val approachingBottom by remember(listState, visibleGroupCount, groups.size) {
        derivedStateOf {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisibleIndex >= visibleGroupCount - 4 && visibleGroupCount < groups.size
        }
    }

    LaunchedEffect(approachingBottom) {
        if (approachingBottom) {
            visibleGroupCount = minOf(visibleGroupCount + GROUPS_PER_PAGE, groups.size)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(
            count = visibleGroupCount,
            key = { index -> groups[index].firstOrNull()?.id ?: index.toLong() }
        ) { index ->
            DuplicateCard(
                group = DuplicateFile(id = index + 1, files = groups[index]),
                selectedFiles = selectedFiles,
                onSelectionChange = onSelectionChange
            )
        }
    }
}

@Composable
fun DuplicateCard(
    group: DuplicateFile,
    selectedFiles: List<DeviceFile>,
    onSelectionChange: (DeviceFile, Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF2F7FC))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Group ${group.id}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "${group.files.size} identical/similar files",

                        fontSize = 12.sp
                    )
                }
            }
            LazyRow() {

                items(
                    count = group.files.size,
                    key = { index -> group.files[index].id }
                ) { index ->

                    DuplicateFileRow(
                        file = group.files[index],
                        isOriginal = index == 0,
                        selected = selectedFiles.contains(group.files[index]),
                        onCheckedChange = { checked -> onSelectionChange(group.files[index], checked) }
                    )
                }


            }
        }
    }
}

@Composable
fun DuplicateFileRow(
    file: DeviceFile,
    isOriginal: Boolean,
    selected: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(checkedColor = Color.Red)
        )

//        Spacer(modifier = Modifier.width(6.dp))

        AsyncImage(
            model = file.uri,
            contentDescription = file.displayName,
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable {

                },
            contentScale = ContentScale.Fit,

        )

//        Spacer(modifier = Modifier.width(10.dp))

//        Column(modifier = Modifier.weight(1f)) {
//            Text(
//                text = file.displayName,
//                maxLines = 1,
//                overflow = TextOverflow.Ellipsis,
//                fontWeight = FontWeight.Medium,
//                fontSize = 14.sp
//            )
//            Spacer(modifier = Modifier.height(2.dp))
//            Text(
//                text = formatFileSize(file.size),
//                color = Color.Gray,
//                fontSize = 12.sp
//            )
//            Spacer(modifier = Modifier.height(4.dp))
//            Box(
//                modifier = Modifier
//                    .background(
//                        if (isOriginal) Color(0xFFDDF5E4) else Color(0xFFFFE1E5),
//                        RoundedCornerShape(6.dp)
//                    )
//                    .padding(horizontal = 6.dp, vertical = 2.dp)
//            ) {
//                Text(
//                    text = if (isOriginal) "Original" else "Duplicate",
//                    color = if (isOriginal) Color(0xFF15803D) else Color(0xFFC62828),
//                    fontSize = 10.sp,
//                    fontWeight = FontWeight.Bold
//                )
//            }
//        }
    }
}
@Composable
fun DuplicateFileCard(
    file: DeviceFile,
    isOriginal: Boolean,
    selected: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {


        Spacer(modifier = Modifier.width(6.dp))

        AsyncImage(
            model = file.uri,
            contentDescription = file.displayName,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.displayName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatFileSize(file.size),
                color = Color.Gray,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .background(
                        if (isOriginal) Color(0xFFDDF5E4) else Color(0xFFFFE1E5),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isOriginal) "Original" else "Duplicate",
                    color = if (isOriginal) Color(0xFF15803D) else Color(0xFFC62828),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@SuppressLint("DefaultLocale")
fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024L * 1024L * 1024L -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024L * 1024L -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024L -> String.format("%.2f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FindDuplicatesTheme {
        val navController = rememberNavController()
        val database = AppDatabase.getDatabase(LocalContext.current)
        val repository = FileRepository(LocalContext.current, database.embeddingDao())

        DuplicateResultScreen(FileViewModel(repository), navController = navController)
    }
}