package com.example.findduplicates.presentation


import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.findduplicates.ui.theme.FindDuplicatesTheme
import com.example.findduplicates.utils.PermissionUtils
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

private val PrimaryBlue = Color(0xFF2879F0)
val HeaderBlue = Color(0xFF2670E8)
val HeaderLightBlue = Color(0xFF55A8F6)
private val NavyBlue = Color(0xFF102C60)
private val SecondaryText = Color(0xFF60789F)
val PageColor = Color(0xFFF8FBFF)
private val CardBorder = Color(0xFFD9E7FC)
private val LightBlue = Color(0xFFEDF5FF)
private val Green = Color(0xFF20AE7B)
private val Purple = Color(0xFF8061EE)

private enum class ScanMedia(
    val label: String,
    val subtitle: String
) {
    IMAGE("IMAGES", "Scan Photos"),
    VIDEO("Videos", "Scan Video files"),
    ALL_FILES("All files", "Coming soon")
}

@Composable
fun HomeScreenTemp(
    onStartScan: (mediaType: String, scanLimit: Int?) -> Unit,
    onOpenResults: () -> Unit,
    onOpenSettings: () -> Unit = {},
    viewModel: FileViewModel, navController: NavController
) {
    val context = LocalContext.current
    val mediaCounts by viewModel.mediaCounts.collectAsStateWithLifecycle()
    var selectedMedia by remember {
        mutableStateOf(ScanMedia.IMAGE)
    }
    val isScanning by viewModel.isScanning.collectAsState()
    // null means scan the entire gallery
    var selectedLimit by remember {
        mutableStateOf<Int?>(500)
    }

    val mediaCount = when (selectedMedia) {
        ScanMedia.IMAGE -> mediaCounts.totalImages
        ScanMedia.VIDEO -> mediaCounts.totalVideos
        ScanMedia.ALL_FILES -> mediaCounts.totalImages + mediaCounts.totalVideos
    }
    val scanProgress by viewModel.scanProgress.collectAsStateWithLifecycle()
    val scanTotal by viewModel.scanTotal.collectAsStateWithLifecycle()
    val scanStage by viewModel.scanStage.collectAsStateWithLifecycle()

    var progressFraction = if (scanTotal > 0) {
        (scanProgress.toFloat() / scanTotal).coerceIn(0f, 1f)
    } else {
        0f
    }
    val isComingSoon = selectedMedia == ScanMedia.ALL_FILES

    val effectiveLimit = selectedLimit?.let {
        minOf(it, mediaCount)
    } ?: mediaCount
    var scanStarted by remember { mutableStateOf(false) }

    LaunchedEffect(isScanning, scanStarted) {
        if (scanStarted && !isScanning) {
            scanStarted = false

            val duplicateGroups = viewModel.duplicateGroups.value
            if (duplicateGroups.isNotEmpty()) {
                onOpenResults()
            } else {
                // No duplicates found — stay on HomeScreen
                // Show a message to the user

                Toast.makeText(
                    context,
                    "No duplicates found",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsResult ->
        val granted = permissionsResult.entries.all { it.value }
        if (granted) {
            viewModel.scanForDuplicates(selectedMedia.name,selectedLimit)
        }
    }

    Scaffold(
        containerColor = PageColor,
//        bottomBar = {
//            NavigationBar(
//                containerColor = Color.White,
//                tonalElevation = 0.dp
//            ) {
//                NavigationBarItem(
//                    selected = true,
//                    onClick = {},
//                    icon = {
//                        Icon(
//                            Icons.Default.Home,
//                            contentDescription = "Home"
//                        )
//                    },
//                    label = { Text("Home") },
//                    colors = NavigationBarItemDefaults.colors(
//                        selectedIconColor = PrimaryBlue,
//                        selectedTextColor = PrimaryBlue,
//                        unselectedIconColor = Color(0xFF8495B0),
//                        unselectedTextColor = Color(0xFF8495B0),
//                        indicatorColor = Color.White
//                    )
//                )
//
//                NavigationBarItem(
//                    selected = false,
//                    onClick = onOpenResults,
//                    icon = {
//                        Icon(
//                            Icons.Default.PhotoLibrary,
//                            contentDescription = "Results"
//                        )
//                    },
//                    label = { Text("Results") },
//                    colors = NavigationBarItemDefaults.colors(
//                        unselectedIconColor = Color(0xFF8495B0),
//                        unselectedTextColor = Color(0xFF8495B0)
//                    )
//                )
//            }
//        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(HeaderBlue, HeaderLightBlue)
                    )
                )
                .padding(innerPadding)
                .statusBarsPadding()

                .verticalScroll(rememberScrollState())
        ) {
            // -------------------------------------------------
            // 1. Blue gradient header
            // -------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
//                    .background(
//                        Brush.horizontalGradient(
//                            listOf(HeaderBlue, HeaderLightBlue)
//                        )
//                    )
            )
            {
                Column(
                    modifier = Modifier.padding(
                        start = 20.dp,
                        end = 16.dp,
                        bottom = 12.dp
                    ), horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(17.dp),
                            color = Color.White,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(39.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(14.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Twinly",
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(Modifier.height(5.dp))

                            Text(
                                text = "Find duplicates. Free up space.",
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }

                        IconButton(
                            onClick = onOpenSettings
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White,
                                modifier = Modifier.size(27.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Decorative photo-search illustration
//                    PhotoSearchIllustration()
                }

            }

            // -------------------------------------------------
            // 2. Main content
            // -------------------------------------------------
            Surface(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)



                )
                {
                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "What would you like to scan?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyBlue
                    )

                    Spacer(Modifier.height(7.dp))

                    Text(
                        text = "Choose a media type and scan size.",
                        fontSize = 14.sp,
                        color = SecondaryText
                    )

                    Spacer(Modifier.height(22.dp))

                    // -------------------------------------------------
                    // 3. Media type cards
                    // -------------------------------------------------
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MediaOptionCard(
                            modifier = Modifier.weight(1f),
                            title = "Images",
                            subtitle = "Scan photos",
                            selected = selectedMedia == ScanMedia.IMAGE,
                            enabled = true,
                            icon = {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp)
                                )
                            },
                            onClick = {
                                selectedMedia = ScanMedia.IMAGE
                            }
                        )

                        MediaOptionCard(
                            modifier = Modifier.weight(1f),
                            title = "Videos",
                            subtitle = "Scan Video files",
                            selected = selectedMedia == ScanMedia.VIDEO,
                            enabled = true,
                            icon = {
                                Icon(
                                    Icons.Default.VideoLibrary,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp)
                                )
                            },
                            onClick = {
                                selectedMedia = ScanMedia.VIDEO
                            }
                        )

                        MediaOptionCard(
                            modifier = Modifier.weight(1f),
                            title = "All files",
                            subtitle = "Coming soon",
                            selected = false,
                            enabled = false,
                            icon = {
                                Icon(
                                    Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp)
                                )
                            },
                            onClick = {}
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // -------------------------------------------------
                    // 4. Available media + privacy
                    // -------------------------------------------------
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = LightBlue
                        ),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 15.dp,
                                vertical = 17.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFD5E7FF),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (selectedMedia == ScanMedia.VIDEO)
                                            Icons.Default.VideoLibrary
                                        else
                                            Icons.Default.Image,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(35.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.width(13.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Available ${
                                        selectedMedia.label.lowercase()
                                    }",
                                    fontSize = 12.sp,
                                    color = SecondaryText
                                )

                                Spacer(Modifier.height(3.dp))

                                Text(
                                    text = "%,d".format(mediaCount),
                                    fontSize = 25.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyBlue
                                )
                            }

                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = NavyBlue,
                                modifier = Modifier.size(25.dp)
                            )

                            Spacer(Modifier.width(9.dp))

                            HorizontalDivider(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(55.dp),
                                color = CardBorder
                            )

                            Spacer(Modifier.width(12.dp))

                            Column(
                                modifier = Modifier.widthIn(max = 125.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Security,
                                        contentDescription = null,
                                        tint = Green,
                                        modifier = Modifier.size(24.dp)
                                    )

                                    Spacer(Modifier.width(5.dp))

                                    Text(
                                        "On device",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Green
                                    )
                                }

                                Spacer(Modifier.height(4.dp))

                                Text(
                                    "Your photos stay private",
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp,
                                    color = SecondaryText
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(25.dp))

                    // -------------------------------------------------
                    // 5. Scan limit heading
                    // -------------------------------------------------
                    Text(
                        text = "Choose scan limit",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyBlue
                    )

                    Spacer(Modifier.height(5.dp))

                    Text(
                        text = "Start small or scan your entire gallery.",
                        fontSize = 13.sp,
                        color = SecondaryText
                    )

                    Spacer(Modifier.height(19.dp))

                    // -------------------------------------------------
                    // 6. Scan limit selection grid
                    // -------------------------------------------------
                    val limits = listOf(500, 1000, 5000, -1)

                    limits.chunked(2).forEach { rowLimits ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowLimits.forEach { limit ->
                                val isAll = limit == -1
                                val isSelected = if (isAll) {
                                    selectedLimit == null
                                } else {
                                    selectedLimit == limit
                                }

                                ScanLimitCard(
                                    modifier = Modifier.weight(1f),
                                    title = if (isAll) {
                                        "All"
                                    } else {
                                        "%,d".format(limit)
                                    },
                                    subtitle = if (isAll) {
                                        "Scan entire gallery"
                                    } else {
                                        "Up to ${"%,d".format(limit)} items"
                                    },
                                    selected = isSelected,
                                    onClick = {
                                        selectedLimit =
                                            if (isAll) null else limit
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                    }

                    Spacer(Modifier.height(4.dp))

                    // -------------------------------------------------
                    // 7. Information banner
                    // -------------------------------------------------
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(17.dp),
                        color = Color(0xFFEAF3FF)
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 14.dp,
                                vertical = 14.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = PrimaryBlue,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(25.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Text(
                                text = when {
                                    isComingSoon ->
                                        "Scanning all file types will be available soon."

                                    mediaCount == 0 ->
                                        "No ${selectedMedia.label.lowercase()} found in your gallery."

                                    else ->
                                        "Scanning ${
                                            "%,d".format(effectiveLimit)
                                        } ${
                                            selectedMedia.label.lowercase()
                                        } helps you find duplicates."
                                },
                                fontSize = 12.sp,
                                lineHeight = 19.sp,
                                color = PrimaryBlue
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // -------------------------------------------------
                    // 8. Start scan button
                    // -------------------------------------------------
                    ProgressiveScanButton(
                        isScanning = isScanning,
                        progress = progressFraction,
                        scanStage = scanStage,
                        enabled = !isComingSoon && mediaCount > 0, onClick = {
                            scanStarted = true
                                onStartScan(
                                    selectedMedia.name,
                                    selectedLimit
                                )

                        }
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Your photos and videos stay on your device.",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        color = SecondaryText
                    )

                    Spacer(Modifier.height(15.dp))
                }

            }
        }

    }
}

// =============================================================
// Media type card
// =============================================================

@Composable
private fun MediaOptionCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    selected: Boolean,
    enabled: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(182.dp)
            .clickable(
                enabled = enabled,
                onClick = onClick
            ),
        shape = RoundedCornerShape(19.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.5.dp,
            color = when {
                selected -> PrimaryBlue
                else -> CardBorder
            }
        ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                selected -> Color(0xFFEDF5FF)
                !enabled -> Color(0xFFF1F5FA)
                else -> Color.White
            }
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            if (selected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = PrimaryBlue,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(25.dp)
                )
            }

            if (!enabled) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd),
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFAFBED3)
                ) {
                    Text(
                        "Soon",
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 4.dp
                        ),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 5.dp, top = 8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = when {
                        !enabled -> Color(0xFFE1E8F1)
                        selected -> PrimaryBlue
                        title == "Videos" -> Color(0xFFEDE8FF)
                        else -> Color(0xFFE8F0FF)
                    },
                    modifier = Modifier.size(57.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        CompositionLocalProvider(
                            LocalContentColor provides when {
                                !enabled -> Color(0xFF8294AD)
                                selected -> Color.White
                                title == "Videos" -> Purple
                                else -> PrimaryBlue
                            }
                        ) {
                            icon()
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) NavyBlue else SecondaryText
                )

                Spacer(Modifier.height(3.dp))

                Text(
                    subtitle,
                    fontSize = 11.sp,
                    color = SecondaryText
                )
            }
        }
    }
}

// =============================================================
// Scan limit card
// =============================================================

@Composable
private fun ScanLimitCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(96.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.5.dp,
            color = if (selected) PrimaryBlue else CardBorder
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                Color(0xFFEDF5FF)
            else
                Color.White
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(
                    selectedColor = PrimaryBlue,
                    unselectedColor = Color(0xFFA3B4CE)
                )
            )

            Spacer(Modifier.width(6.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyBlue
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = SecondaryText
                )
            }
        }
    }
}
@Preview(showBackground = true)
@Composable
fun HomeScreenTempPreview() {
    FindDuplicatesTheme {
        HomeScreenTemp(
            onStartScan = { mediaType, scanLimit ->
                // UI callback only.
                // Connect your existing ViewModel here.
                //
                // mediaType: "IMAGES" or "VIDEO"
                // scanLimit: 500, 1000, 5000, or null for All
            },
            onOpenResults = {
                // Navigate to your duplicate results screen
            },
            onOpenSettings = {
                // Navigate to settings

            },
            viewModel = TODO(),
            navController = TODO()
        )
    }
}

@Composable
fun ProgressiveScanButton(
    isScanning: Boolean,
    progress: Float,
    scanStage: String,
    enabled: Boolean,
    onClick: () -> Unit
) {


    val animatedProgress by androidx.compose.animation.core
        .animateFloatAsState(
            targetValue = progress.coerceIn(0f, 1f),
            label = "scanProgress"
        )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(50.dp))
            .background(PrimaryBlue)
            .clickable(
                enabled = enabled && !isScanning,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isScanning) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(maxWidth * animatedProgress)
                    .background(Color(0xFF1749A8))
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.width(10.dp))

            Text(
                text = when {
                    isScanning ->
                        "$scanStage · ${(animatedProgress * 100).toInt()}%"
                    progress >= 1f ->
                        "Scan Complete"
                    else ->
                        "Start Scan"
                },
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
