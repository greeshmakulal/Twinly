package com.example.findduplicates.presentation

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.findduplicates.Greeting
import com.example.findduplicates.R
import com.example.findduplicates.repository.FileRepository
import com.example.findduplicates.ui.theme.FindDuplicatesTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PhotoSizeSelectActual
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.findduplicates.ui.theme.themedBlue
import com.example.findduplicates.ui.theme.themedYellow
import com.example.findduplicates.utils.PermissionUtils

@Composable
fun HomeScreen(viewModel: FileViewModel) {
    val context = LocalContext.current
    val uiState by viewModel.files.collectAsState()
    val permissions = PermissionUtils.getMediaPermissions()
   val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()){
       permissions->

       val granted = permissions.entries.all { it.value }
       if(granted)
       {
           viewModel.getMediaFiles()

           println("Permission Granted:${uiState}")
       }
   }
    val Poppins = FontFamily(
        Font(R.font.poppins_bold)
    )


    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFDAE7F3),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()

        ) {
            val isLandscape = maxWidth > maxHeight

            if (isLandscape) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {

                    TopSection(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(
                                min = 180.dp,
                                max = 230.dp
                            ),
                        fontFamily = Poppins,
                        onScanClick = {
                            if (PermissionUtils.hasMediaPermissions(context)) {

                                viewModel.getMediaFiles()

                            } else {

                                permissionLauncher.launch(
                                    PermissionUtils.getMediaPermissions()
                                )
                            }

                        }
                    )

                    BodySection(
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                }

            } else {

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {

                    TopSection(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.55f),
                        fontFamily = Poppins,
                        onScanClick = {
                            if (PermissionUtils.hasMediaPermissions(context)) {

                                viewModel.getMediaFiles()

                            } else {

                                permissionLauncher.launch(
                                    PermissionUtils.getMediaPermissions()
                                )
                            }
                        }
                    )

                    BodySection(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.45f)
                    )
                }
            }
        }
    }
}
@Composable
fun TopSection(
    modifier: Modifier = Modifier,
    fontFamily: FontFamily,
    onScanClick: () -> Unit
) {

    val headerGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF3F51B5),
            Color(0xFF5BA7E7)
        )
    )

    Box(
        modifier = modifier
            .background(
                brush = headerGradient,
                shape = RoundedCornerShape(
                    bottomStart = 60.dp,
                    bottomEnd = 60.dp
                )
            ),


    ) {

        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 10.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Duplicate File Scanner",
                    modifier = Modifier.weight(1f),
                    color = Color.White,
                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily,
                    maxLines = 1
                )

                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable{
                        println("Clicked icon")
                    onScanClick()
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Image(
                    painter = painterResource(R.drawable.search_file),
                    contentDescription = "Search files",
                    modifier = Modifier.size(92.dp)
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = "Scan Files",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily
                )
            }
        }
    }
}
@Composable
fun BodySection(
   modifier: Modifier
) {

    Column(
        modifier = modifier
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            ScanCard(
                modifier = Modifier.weight(1f),
                title = "Scan Photos",
                icon = Icons.Default.PhotoSizeSelectActual,
                gradientColors = listOf(
                    Color(0xFF088610),
                    Color(0xFFACE865)
                )
            )

            ScanCard(
                modifier = Modifier.weight(1f),
                title = "Scan Audio",
                icon = Icons.Default.AudioFile,
                gradientColors = listOf(
                    Color(0xFFDC0D54),
                    Color(0xFFFC968E)
                )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            ScanCard(
                modifier = Modifier.weight(1f),
                title = "Scan Document",
                icon = Icons.Default.FileCopy,
                gradientColors = listOf(
                    Color(0xFF580965),
                    Color(0xFFDC74EE)
                )
            )

            ScanCard(
                modifier = Modifier.weight(1f),
                title = "Scan Video",
                icon = Icons.Default.Videocam,
                gradientColors = listOf(
                    Color(0xFF043994),
                    Color(0xFFAAB4EF)
                )
            )
        }
    }
}
@Composable
fun ScanCard(
    modifier: Modifier = Modifier,
    title: String,
    gradientColors: List<Color>,
    icon: ImageVector
) {

    val cardShape = RoundedCornerShape(16.dp)

    Card(
        modifier = modifier
            .height(120.dp),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(cardShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = gradientColors
                    )
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    FindDuplicatesTheme {
        val viewModel = FileViewModel(FileRepository(LocalContext.current))
        HomeScreen(viewModel)
    }
}
