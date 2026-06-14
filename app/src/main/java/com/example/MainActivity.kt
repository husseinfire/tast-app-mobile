package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.data.TimeDatabase
import com.example.data.TimeRepository
import com.example.ui.AllocationsViewModel
import com.example.ui.AllocationsViewModelFactory
import com.example.ui.MainDashboardScreen
import com.example.ui.NotificationHelper
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    // Lazy initialization of Room database and Repository
    private val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            TimeDatabase::class.java,
            "chronos_time_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    private val repository by lazy {
        TimeRepository(database.timeDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createNotificationChannels(this)
        setContent {
            MyApplicationTheme {
                // Instantiating the ViewModel with the repository factory
                val viewModel: AllocationsViewModel = viewModel(
                    factory = AllocationsViewModelFactory(repository)
                )

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainDashboardScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

