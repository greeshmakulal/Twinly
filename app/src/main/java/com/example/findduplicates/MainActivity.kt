
package com.example.findduplicates

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import com.example.findduplicates.data.local.AppDatabase
import com.example.findduplicates.presentation.AppNavGraph
import com.example.findduplicates.presentation.FileViewModel
import com.example.findduplicates.repository.FileRepository
import com.example.findduplicates.ui.theme.FindDuplicatesTheme
import com.example.findduplicates.utils.PermissionUtils

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: FileViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the ViewModel only once.
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(
                modelClass: Class<T>
            ): T {
                val database =
                    AppDatabase.getDatabase(applicationContext)

                val repository = FileRepository(
                    applicationContext,
                    database.embeddingDao()
                )

                @Suppress("UNCHECKED_CAST")
                return FileViewModel(repository) as T
            }
        }

        viewModel = ViewModelProvider(this, factory)
            .get(FileViewModel::class.java)

        enableEdgeToEdge()

        WindowCompat.getInsetsController(
            window,
            window.decorView
        ).isAppearanceLightStatusBars = false

        setContent {
            val context = LocalContext.current

            val permissions = remember {
                PermissionUtils.getMediaPermissions()
            }

            var permissionAttempts by rememberSaveable {
                mutableIntStateOf(0)
            }

            // Handle permission already granted on a previous launch.
            var permissionGranted by remember {
                mutableStateOf(
                    PermissionUtils.hasMediaPermissions(context)
                )
            }

            val permissionLauncher =
                rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { result ->

                    // Require every requested permission for full access.
                    val allGranted = permissions.all { permission ->
                        result[permission] == true ||
                                ContextCompat.checkSelfPermission(
                                    context,
                                    permission
                                ) == PackageManager.PERMISSION_GRANTED
                    }

                    if (allGranted) {
                        permissionGranted = true
                    } else {
                        permissionAttempts++
                    }
                }

            // Request permissions when the app opens.
            LaunchedEffect(permissionAttempts, permissionGranted) {
                when {
                    permissionGranted -> {
                        // Scanning is triggered in the effect below.
                    }

                    permissionAttempts < 2 -> {
                        permissionLauncher.launch(permissions)
                    }

                    else -> {
                        Toast.makeText(
                            context,
                            "Media permission is required to scan your files.",
                            Toast.LENGTH_LONG
                        ).show()

                        finish()
                    }
                }
            }

            // Start scanning only after permission is granted.
            LaunchedEffect(permissionGranted) {
                if (permissionGranted) {
                    // Use your existing ViewModel scan method here.
                    viewModel.loadMediaCounts()
                }
            }

            val preferencesManager = remember { com.example.findduplicates.data.local.PreferencesManager(context) }
            val themeMode by preferencesManager.themeModeFlow.collectAsState(initial = com.example.findduplicates.data.local.AppThemeMode.SYSTEM)
            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val isDarkTheme = when (themeMode) {
                com.example.findduplicates.data.local.AppThemeMode.LIGHT -> false
                com.example.findduplicates.data.local.AppThemeMode.DARK -> true
                else -> isSystemDark
            }

            FindDuplicatesTheme(darkTheme = isDarkTheme) {
                val navController = rememberNavController()

                AppNavGraph(
                    navController = navController,
                    viewModel = viewModel
                )
            }
        }
    }
}
