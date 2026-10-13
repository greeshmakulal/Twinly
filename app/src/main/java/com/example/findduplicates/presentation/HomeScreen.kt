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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.findduplicates.R
import com.example.findduplicates.ui.theme.FindDuplicatesTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.PhotoSizeSelectActual
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.findduplicates.utils.PermissionUtils

@Composable
fun HomeScreen(viewModel: FileViewModel, navController: NavController) {
    val context = LocalContext.current
    val uiState by viewModel.files.collectAsState()
    val permissions = PermissionUtils.getMediaPermissions()
    var scanStarted by remember { mutableStateOf(false) }

    val duplicateGroups by viewModel.duplicateGroups.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val scanTotal by viewModel.scanTotal.collectAsState()
    val scanStage by viewModel.scanStage.collectAsState()
    val liveDuplicateCount by viewModel.liveDuplicateCount.collectAsState()

    val exactCount by viewModel.exactDuplicateCount.collectAsState()
    val similarCount by viewModel.similarDuplicateCount.collectAsState()

    // Limit selection state (null means All)
    var selectedLimit by remember { mutableStateOf<Int?>(500) }
    val limitOptions = listOf(100, 500, 1000, 5000, null)

    LaunchedEffect(isScanning) {
        if (isScanning) {
            scanStarted = true
        }
        if (!isScanning && scanStarted) {
            scanStarted = false
            if (duplicateGroups.isNotEmpty()) {
                navController.navigate("duplicateResults")
            } else {
                Toast.makeText(context, "No Duplicates Found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsResult ->
        val granted = permissionsResult.entries.all { it.value }
        if (granted) {
            viewModel.scanForDuplicates(selectedLimit)
        }
    }

    val Poppins = FontFamily(Font(R.font.poppins_bold))

    val onScanAction: () -> Unit = {
        if (isScanning) {
            viewModel.stopScan()
        } else if (PermissionUtils.hasMediaPermissions(context)) {
            viewModel.scanForDuplicates(selectedLimit)
        } else {
            permissionLauncher.launch(PermissionUtils.getMediaPermissions())
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
                .padding(innerPadding)
        ) {
            val isLandscape = maxWidth > maxHeight

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Header Section with Scan Icon
                TopSection(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    fontFamily = Poppins,
                    onScanClick = onScanAction,
                    isScanning = isScanning
                )

                // Scan Limits & Live Metrics Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Scan Limit Configuration", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Limit Chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            limitOptions.forEach { limit ->
                                val label = limit?.let { "$it" } ?: "All"
                                FilterChip(
                                    selected = selectedLimit == limit,
                                    onClick = { if (!isScanning) selectedLimit = limit },
                                    label = { Text(label, fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFFEAEAEA))
                        Spacer(modifier = Modifier.height(12.dp))

                        // Live Counters Display
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Exact Matches", fontSize = 12.sp, color = Color.Gray)
                                Text("$exactCount groups", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1976D2))
                            }
                            Column {
                                Text("Similar Groups", fontSize = 12.sp, color = Color.Gray)
                                Text("$similarCount groups", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF388E3C))
                            }
                            Column {
                                Text("Total Live", fontSize = 12.sp, color = Color.Gray)
                                Text("$liveDuplicateCount sets", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black)
                            }
                        }

                        if (isScanning) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(scanStage, fontSize = 13.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            Spacer(modifier = Modifier.height(6.dp))
                            if (scanTotal > 0) {
                                LinearProgressIndicator(
                                    progress = { scanProgress.toFloat() / scanTotal.toFloat() },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("$scanProgress / $scanTotal images analyzed", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                // Feature Body Section
                BodySection(modifier = Modifier.fillMaxWidth())
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
        colors = listOf(Color(0xFF3F51B5), Color(0xFF5BA7E7))
    )

    Box(
        modifier = modifier.background(brush = headerGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isScanning) "Tap to Stop Scan" else "Tap me to Scan",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily
                )
            }
        }
    }
}

@Composable
fun BodySection(modifier: Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScanCard(
                modifier = Modifier.weight(1f),
                title = "Scan Photos",
                icon = Icons.Default.PhotoSizeSelectActual,
                gradientColors = listOf(Color(0xFF088610), Color(0xFFACE865))
            )
            ScanCard(
                modifier = Modifier.weight(1f),
                title = "Scan Audio",
                icon = Icons.Default.AudioFile,
                gradientColors = listOf(Color(0xFFDC0D54), Color(0xFFFC968E))
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ScanCard(
                modifier = Modifier.weight(1f),
                title = "Scan Document",
                icon = Icons.Default.FileCopy,
                gradientColors = listOf(Color(0xFF580965), Color(0xFFDC74EE))
            )
            ScanCard(
                modifier = Modifier.weight(1f),
                title = "Scan Video",
                icon = Icons.Default.Videocam,
                gradientColors = listOf(Color(0xFF043994), Color(0xFFAAB4EF))
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
        modifier = modifier.height(110.dp),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(cardShape)
                .background(brush = Brush.linearGradient(colors = gradientColors))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    tint = Color.White,
                    contentDescription = title,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ScanIcon(
    isScanning: Boolean,
    onClick: () -> Unit
) {
    val iconScale by animateFloatAsState(
        targetValue = if (isScanning) 0.75f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "iconScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "scanAnimation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing)
        ),
        label = "scanRotation"
    )

    Box(
        modifier = Modifier.clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isScanning) {
            CircularProgressIndicator(
                progress = { 0.75f },
                modifier = Modifier
                    .size(100.dp)
                    .graphicsLayer { rotationZ = rotation },
                color = Color.White,
                trackColor = Color.Transparent,
                strokeWidth = 4.dp
            )
        }

        Image(
            painter = painterResource(R.drawable.search),
            contentDescription = "Scan files",
            modifier = Modifier
                .size(75.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
        )
    }
}