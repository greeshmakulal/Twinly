
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage

import com.example.findduplicates.data.DeviceFile
import com.example.findduplicates.data.local.AppDatabase
import com.example.findduplicates.repository.FileRepository
import com.example.findduplicates.ui.theme.FindDuplicatesTheme

private enum class ResultTab {
    EXACT, SIMILAR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateResultScreenTemp(viewModel: FileViewModel, navController: NavController,
      onBack: () -> Unit,

) {
    var selectedTab by remember { mutableStateOf(ResultTab.EXACT) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val duplicateGroups by viewModel.duplicateGroups.collectAsState()
    var selectedGroup by remember { mutableStateOf<List<DeviceFile>>(emptyList()) }

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


    val selectedFiles = (duplicateGroups)
        .flatten()
        .filter { it.id in selectedIds }
    val selectedSize = selectedFiles.sumOf { it.size }
    val context = LocalContext.current
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val deletedIds = selectedFiles.map { it.id }.toSet()
            selectedFiles
//            onDeleted(deletedIds)
            viewModel.updateGroupsAfterDeletion(
                deletedIds = deletedIds
            )
            Toast.makeText(context, "Files deleted successfully", Toast.LENGTH_SHORT).show()
        }
    }
    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Duplicate Results",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                        , color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                            ,tint = Color.Black
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        selectedIds = emptySet()
                    }) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Clear selection",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },

    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .clip(RoundedCornerShape(20.dp))
                        ) {
            // Summary banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFEEF1F5)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF3478E5),
                        modifier = Modifier.size(30.dp)
                    )

                    Spacer(Modifier.width(12.dp))

                    Column {
                        Text(
                            "Scan results",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF17366D)
                        )
                        Text(
                            "Found ${duplicateGroups.size}  duplicate groups ",
                            fontSize = 12.sp,
                            color = Color(0xFF526783)
                        )
                    }
                }
            }


            if (duplicateGroups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(

                            "No  duplicates found",
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = 14.dp,
                        end = 14.dp,
                        top = 12.dp,
                        bottom = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(duplicateGroups) { index, group ->
                        DuplicateGroupCard(
                            groupNumber = index + 1,
                            groupIndex = index,
                            group = group,
                            isSimilar = selectedTab == ResultTab.SIMILAR,
                            selectedIds = selectedIds,
                            onToggle = { id ->
                                selectedIds = if (id in selectedIds) {
                                    selectedIds - id
                                } else {
                                    selectedIds + id
                                }
                            },navController = navController
                        )
                    }
                }
            }

            // Selection action bar
            if (selectedIds.isNotEmpty()) {
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${selectedIds.size} selected",
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.SemiBold
                        )

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
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD94747)
                            )
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DuplicateGroupCard(
    groupNumber: Int,
    group: List<DeviceFile>,
    isSimilar: Boolean,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    groupIndex:Int,
    navController: NavController) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF0F4F5)
        ),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isSimilar) Color(0xFFF0E9FF)
                    else Color(0xFFE6F0FF)
                ) {
                    Text(
                        text = "Group $groupNumber"
                           ,
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 6.dp
                        ),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSimilar) Color(0xFF7650C9)
                        else Color(0xFF2868C9)
                    )
                }

                Spacer(Modifier.weight(1f))

                Text(
                    "${group.size} images",
                    fontSize = 15.sp,
                    color = Color(0xFF1266D7),
                    modifier = Modifier.clickable {
                            navController.navigate("duplicateGroup/$groupIndex")
                        }

                )


            }

            Spacer(Modifier.height(12.dp))

            // Horizontal thumbnail strip
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                group
                    .forEach { file ->
                    val isSelected = file.id in selectedIds

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFEAF0F7))
                            .clickable { onToggle(file.id) }
                    ) {
                        AsyncImage(
                            model = file.uri,
                            contentDescription = file.displayName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(5.dp)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF3478E5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
                repeat((2 - group.take(2).size).coerceAtLeast(0)) {
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                    )
                }
            }
        }
    }
}
@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun DuplicatePreview() {
    FindDuplicatesTheme {



        val navController = rememberNavController()
        val database = AppDatabase.getDatabase(LocalContext.current)
        val repository = FileRepository(LocalContext.current, database.embeddingDao())
        val viewMOdel = FileViewModel(repository)
        DuplicateResultScreenTemp(

         viewMOdel, navController,
            onBack = {
                navController.popBackStack()
            },

        )
    }
}
