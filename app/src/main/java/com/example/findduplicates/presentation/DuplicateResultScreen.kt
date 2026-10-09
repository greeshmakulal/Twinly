package com.example.findduplicates.presentation

import android.annotation.SuppressLint
import android.app.Activity
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.format.Formatter.formatFileSize
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.toString
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.findduplicates.repository.FileRepository
import com.example.findduplicates.ui.theme.FindDuplicatesTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.example.findduplicates.data.DeviceFile
import com.example.findduplicates.data.DuplicateFile

@Composable
fun DuplicateResultScreen(viewModel: FileViewModel,navController: NavController){
    val duplicateGroups by
    viewModel.duplicateGroups.collectAsState()
    val groups = duplicateGroups.mapIndexed { index, files ->
        DuplicateFile(
            id = index + 1,
            files = files
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),

                topBar = {
            TopBar()
        }
    ) {innerPadding->
        MainContent(modifier =
            Modifier.padding(innerPadding),
            groups,
            onDeleted = { deletedIds ->
                viewModel.removeDeletedFiles(deletedIds)
                    viewModel.scanForDuplicates()
                navController.popBackStack()

            })

    }
    
}
@Composable
fun TopBar()
{
    Row(modifier = Modifier
        .fillMaxWidth()
        .background(Color.Blue)
        .statusBarsPadding(), horizontalArrangement = Arrangement.Center)
    {
        Icon(
            imageVector = Icons.Default.ArrowBackIosNew,
            contentDescription = "Back",
            tint = Color.White,
            modifier = Modifier
                .size(30.dp)
                .padding(2.dp)
                .weight(0.1f)
        )
        Text(text = "Duplicate Groups", textAlign = TextAlign.Center, modifier = Modifier
            .weight(0.9f)
            .padding(2.dp)
            ,color = Color.White, fontWeight = FontWeight.Bold)
    }
}
@SuppressLint("DefaultLocale")
@Composable
fun MainContent(
    modifier: Modifier,
    groups: List<DuplicateFile>,
    onDeleted: (Set<Long>) -> Unit
) {

    val selectedFiles = remember {
        mutableStateListOf<DeviceFile>()
    }

    val selectedSize = selectedFiles.sumOf { it.size }
    val cardShape = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 24.dp
    )
    val context = LocalContext.current

    val deleteLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->

            if (result.resultCode == Activity.RESULT_OK) {

                val deletedIds = selectedFiles.map { it.id }.toSet()
                selectedFiles.clear()
                onDeleted(deletedIds)
                Toast.makeText(
                    context,
                    "Files deleted",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFDAE7F3))
    ) {

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(Color.Blue)
                )

                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 10.dp),
                    shape = cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    DuplicateItems(
                        groups = groups,
                        selectedFiles = selectedFiles,
                        onSelectionChange = { file, checked ->

                            if (checked) {

                                if (!selectedFiles.contains(file)) {
                                    selectedFiles.add(file)
                                }

                            } else {

                                selectedFiles.remove(file)
                            }
                        })
                }
            }


            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(0.15f)
                    .padding(horizontal = 3.dp)
                    .offset(y = (-20).dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.Blue,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 20.dp,
                            vertical = 16.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Absolute.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(0.4f)) {
                        Text(text = "${selectedFiles.size} Selected", color = Color.White)
                        Text(text =  formatFileSize(selectedSize), color = Color.White)
                    }
                    val mediaUris = selectedFiles
                        .filter { file ->
                            file.mimeType?.startsWith("image/") == true ||
                                    file.mimeType?.startsWith("video/") == true ||
                                    file.mimeType?.startsWith("audio/") == true
                        }
                        .map { it.uri }
                    Button(
                        onClick = {

                        if (mediaUris.isNotEmpty()) {

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {


                                val pendingIntent =
                                    MediaStore.createDeleteRequest(
                                        context.contentResolver,
                                        mediaUris
                                    )

                                val intentSenderRequest =
                                    IntentSenderRequest.Builder(
                                        pendingIntent.intentSender
                                    ).build()

                                deleteLauncher.launch(
                                    intentSenderRequest
                                )
                            }
                        }
                    },
                        enabled = selectedFiles.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red,
                            contentColor = Color.White,
                            disabledContainerColor = Color.LightGray
                        ),


                        modifier = Modifier.weight(0.6f),
                    ) {
                        Row(modifier = Modifier
                            .padding(2.dp)
                            .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Absolute.Center) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            tint = Color.White,
                            contentDescription = "Delete"
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "Delete",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        }
                    }
                }
            }
        }


    }
}
@Composable
fun DuplicateItems(
    groups:List<DuplicateFile>,
    selectedFiles: List<DeviceFile>,
    onSelectionChange: (DeviceFile, Boolean) -> Unit,
)
{
    LazyColumn(){

        items(
            items = groups,
            key = { it.id}
        )
        {
            DuplicateCard(
                DuplicateFile(
                    id = 1,
                    files = it.files
                ),
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {

        Column {

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF2F7FC))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint = Color(0xFF1976D2)
                )

                Spacer(
                    modifier = Modifier.width(10.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Duplicate Group ${group.id}",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Text(
                        text = "${group.files.size} files",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }

            group.files.forEachIndexed { index, file ->

                if (index != 0) {
                    HorizontalDivider(
                        color = Color(0xFFEAEAEA)
                    )
                }

                DuplicateFileRow(
                    file = file,
                    isOriginal = index == 0,
                    selected = selectedFiles.contains(file),
                    onCheckedChange = { checked ->

                        onSelectionChange(
                            file,
                            checked
                        )
                    }
                )
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
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Checkbox(
            checked = selected,
            enabled = true,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = Color.Red,
                uncheckedColor = Color.Gray,
                checkmarkColor = Color.White
            )
        )

        Spacer(
            modifier = Modifier.width(8.dp)
        )

        AsyncImage(
            model = file.uri,
            contentDescription = file.displayName,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(
            modifier = Modifier.width(12.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = file.displayName,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = formatFileSize(LocalContext.current,file.size),
                color = Color.Black,
                fontSize = 13.sp
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Box(
                modifier = Modifier
                    .background(
                        if (isOriginal)
                            Color(0xFFDDF5E4)
                        else
                            Color(0xFFFFE1E5),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(
                        horizontal = 8.dp,
                        vertical = 4.dp
                    )
            ) {

                Text(
                    text = if (isOriginal) {
                        "Original"
                    } else {
                        "Duplicate"
                    },
                    color = if (isOriginal) {
                        Color(0xFF15803D)
                    } else {
                        Color(0xFFC62828)
                    },
                    fontSize = 11.sp
                )
            }
        }
    }
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 ->
            String.format(
                "%.2f GB",
                bytes / (1024.0 * 1024.0 * 1024.0)
            )

        bytes >= 1024 * 1024 ->
            String.format(
                "%.2f MB",
                bytes / (1024.0 * 1024.0)
            )

        bytes >= 1024 ->
            String.format(
                "%.2f KB",
                bytes / 1024.0
            )

        else ->
            "$bytes B"
    }
}
@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun DuplicateResultScreenPreview() {
    FindDuplicatesTheme {
        DuplicateResultScreen(
            FileViewModel(FileRepository(LocalContext.current)),
            navController =  NavController(LocalContext.current)
        )
    }
}
