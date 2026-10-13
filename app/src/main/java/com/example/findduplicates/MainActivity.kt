package com.example.findduplicates

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import com.example.findduplicates.data.local.AppDatabase
import com.example.findduplicates.presentation.AppNavGraph
import com.example.findduplicates.presentation.DuplicateResultScreen
import com.example.findduplicates.presentation.FileViewModel
import com.example.findduplicates.presentation.HomeScreen
import com.example.findduplicates.repository.FileRepository
import com.example.findduplicates.ui.theme.FindDuplicatesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel: FileViewModel by viewModels {
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    // 1. Get the Room database instance
                    val database = AppDatabase.getDatabase(applicationContext)

                    // 2. Pass the DAO to the Repository
                    val repository = FileRepository(applicationContext, database.embeddingDao())

                    // 3. Return the ViewModel
                    @Suppress("UNCHECKED_CAST")
                    return FileViewModel(repository) as T
                }
            }
        }
        enableEdgeToEdge()

        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = false
        setContent {
            FindDuplicatesTheme {
                val navController = rememberNavController()

                AppNavGraph(
                    navController = navController,
                    viewModel = viewModel
                )

            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    FindDuplicatesTheme {
        Greeting("Android")
    }
}