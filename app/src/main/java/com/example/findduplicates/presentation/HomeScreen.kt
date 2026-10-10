package com.example.findduplicates.presentation

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.findduplicates.R
import com.example.findduplicates.repository.FileRepository
import com.example.findduplicates.ui.theme.FindDuplicatesTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.PhotoSizeSelectActual
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.findduplicates.utils.PermissionUtils

@Composable
fun HomeScreen(viewModel: FileViewModel,navController: NavController) {
    val context = LocalContext.current
    val uiState by viewModel.files.collectAsState()
    val permissions = PermissionUtils.getMediaPermissions()
    var scanStarted by remember {
        mutableStateOf(false)
    }
    val duplicateGroups by
    viewModel.duplicateGroups.collectAsState()

    val isScanning by viewModel.isScanning.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val scanTotal by viewModel.scanTotal.collectAsState()
    val scanStage by viewModel.scanStage.collectAsState()
    val liveDuplicateCount by viewModel.liveDuplicateCount.collectAsState()

    LaunchedEffect(isScanning) {
        if (isScanning) {
            scanStarted = true
        }
        if (!isScanning && scanStarted) {

            scanStarted = false

            if (duplicateGroups.isNotEmpty()) {

                navController.navigate("duplicateResults")

            } else {

                Toast.makeText(
                    context,
                    "No Duplicates Found",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
   val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()){
       permissions->

       val granted = permissions.entries.all { it.value }
       if(granted)
       {
           viewModel.scanForDuplicates()

           println("Permission Granted:${uiState}")
       }
   }
    val Poppins = FontFamily(
        Font(R.font.poppins_bold)
    )
    val onScanAction: () -> Unit = {
        if (isScanning) {
            viewModel.stopScan()
        } else if (PermissionUtils.hasMediaPermissions(context)) {
            viewModel.scanForDuplicates()
        } else {
            permissionLauncher.launch(
                PermissionUtils.getMediaPermissions()
            )
        }
    }

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
                            .fillMaxSize()
//                            .heightIn(
//                                min = 180.dp,
//                                max = 230.dp
//                            )
                        ,
                        fontFamily = Poppins,
                        onScanClick =onScanAction
                        ,isScanning
                    )
                    if (isScanning) {
                        ScanProgressCard(
                            stage = scanStage,
                            progress = scanProgress,
                            total = scanTotal,
                            onStop = { viewModel.stopScan() },
                            duplicateCount = liveDuplicateCount,
                        )
                    }

//                    BodySection(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                    )
                }

            } else {

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {

                    TopSection(
                        modifier = Modifier
//                            .fillMaxWidth()
                            .fillMaxSize()
                            .weight(0.55f),
                        fontFamily = Poppins,
                        onScanClick =onScanAction,isScanning
                    )
                    if (isScanning) {
                        ScanProgressCard(
                            stage = scanStage,
                            progress = scanProgress,
                            total = scanTotal,
                            onStop = { viewModel.stopScan() },
                            duplicateCount = liveDuplicateCount,
                        )
                    }

//                    BodySection(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .weight(0.45f)
//                    )
                }
            }
        }
    }
}
@Composable
fun TopSection(
    modifier: Modifier = Modifier,
    fontFamily: FontFamily,
    onScanClick: () -> Unit,
    isScanning: Boolean
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
//                shape = RoundedCornerShape(
//                    bottomStart = 60.dp,
//                    bottomEnd = 60.dp
//                )
            )
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
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                ScanIcon(
                    isScanning = isScanning,
                    onClick = onScanClick
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = when {
                        isScanning -> "Tap to Stop Scan"
                        else -> "Tap me to Scan"
                    },
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
                    tint = Color.White,
                    contentDescription = title,
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
@Composable
fun ScanningLottie() {

    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(
            R.raw.scanning
        )
    )

    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = Modifier.size(220.dp)
    )
}
@Composable
fun ScanIcon(
    isScanning: Boolean,
    onClick: () -> Unit
) {

    val iconScale by animateFloatAsState(
        targetValue = if (isScanning) 0.75f else 1f,
        animationSpec = tween(
            durationMillis = 300
        ),
        label = "iconScale"
    )

    val infiniteTransition =
        rememberInfiniteTransition(
            label = "scanAnimation"
        )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = LinearEasing
            )
        ),
        label = "scanRotation"
    )

    Box(
        modifier = Modifier

            .clickable {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {

        if (isScanning) {

            CircularProgressIndicator(
                progress = { 0.75f },
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer {
                        rotationZ = rotation
                    },
                color = Color.White,
                trackColor = Color.Transparent,
                strokeWidth = 5.dp
            )
        }

        Image(
            painter = painterResource(R.drawable.search),
            contentDescription = "Scan files",
            modifier = Modifier
                .size(90.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
        )
    }
}
@Composable
fun ScanProgressCard(
    stage: String,
    progress: Int,
    total: Int,
    onStop: () -> Unit,
    duplicateCount: Int,
) {
    val fraction = if (total > 0) {
        (progress.toFloat() / total).coerceIn(0f, 1f)
    } else {
        0f
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Scanning your files",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = stage,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileCopy,
                        contentDescription = "Duplicate groups",
                        tint = Color(0xFFDC0D54)
                    )

                    Text(
                        text = "Duplicate groups found",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(
                    text = duplicateCount.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFDC0D54),
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "$progress / $total images analyzed",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedButton(
                onClick = onStop,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text("Stop Scan")
            }
        }
    }
}
@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    FindDuplicatesTheme {
        val viewModel = FileViewModel(FileRepository(LocalContext.current))
        HomeScreen(viewModel,navController = NavController(LocalContext.current))
    }
}
