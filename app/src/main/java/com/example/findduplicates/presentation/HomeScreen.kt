package com.example.findduplicates.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PhotoSizeSelectActual
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import com.example.findduplicates.ui.theme.themedBlue
import com.example.findduplicates.ui.theme.themedYellow

@Composable
fun HomeScreen(viewModel: FileViewModel) {
    val context = LocalContext.current

   val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()){permissions->

       val granted = permissions.entries.all { it.value }
       if(granted)
       {
           viewModel.getMediaFiles()
       }
   }
    Scaffold(   containerColor = Color(0xFF0C0A0B),
        topBar = {TopSection() },
        bottomBar = { },
        floatingActionButton = {

        }
    ) { innerPadding ->
        Spacer(Modifier.size(10.dp))
        BodySection(innerPadding)

    }

}
@Composable
fun TopSection()
{

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.48f),
            shape = RoundedCornerShape(
                    bottomStart = 20.dp,
                    bottomEnd = 20.dp),
                    color = themedBlue
    )
    {
        Box(
            modifier = Modifier
                 .background(
                    brush = Brush.linearGradient(
                        colors =listOf(
                            Color(0xFF3F51B5),
                            Color(0xFFFC968E)
                        )
                    )
                   )
                .fillMaxWidth()
                .fillMaxHeight(),
            contentAlignment = Alignment.Center

        )
        {
            Column(modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                Image(
                    painter = painterResource(R.drawable.search_file),
                    contentDescription = "Search",
                    modifier = Modifier.size(92.dp)
                )
                Spacer(Modifier.size(4.dp))
                Text(modifier = Modifier.fillMaxWidth(), text = "Scan Files", textAlign = TextAlign.Center, color = Color.White)

            }
        }
    }

}
@Composable
fun BodySection(
    padding: PaddingValues
) {
    Column(modifier = Modifier.padding(padding)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            ScanCard(
                modifier = Modifier.weight(1f),
                title = "Files Scanned",
                icon = Icons.Default.PhotoSizeSelectActual,
                gradientColors = listOf(
                    Color(0xFFFED953),
                    Color(0xFFF1EAAA)
                )
            )

            ScanCard(
                modifier = Modifier.weight(1f),
                title = "Duplicates",
                icon = Icons.Default.AudioFile,
                gradientColors = listOf(
                    Color(0xFFF44336),
                    Color(0xFFFC968E)
                )
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            ScanCard(
                modifier = Modifier.weight(1f),
                title = "Files Scanned",
                icon = Icons.Default.PhotoSizeSelectActual,
                gradientColors = listOf(
                    Color(0xFF4AE850),
                    Color(0xFFF1EAAA)
                )
            )

            ScanCard(
                modifier = Modifier.weight(1f),
                title = "Duplicates",
                icon = Icons.Default.AudioFile,
                gradientColors = listOf(
                    Color(0xFF367CF4),
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
    Card(
        modifier = modifier
            .height(120.dp),
        shape = RoundedCornerShape(16.dp),
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
                .background(
                    brush = Brush.linearGradient(
                        colors = gradientColors
                    )
                )
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
                    contentDescription = null
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
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
