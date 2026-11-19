package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.BacGateApp
import com.example.ui.BacGateViewModel
import com.example.ui.BacGateViewModelFactory
import com.example.ui.theme.BacGateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.BLACK),
            navigationBarStyle = SystemBarStyle.dark(Color.BLACK)
        )
        setContent {
            BacGateTheme {
                val vm: BacGateViewModel = viewModel(
                    factory = BacGateViewModelFactory(applicationContext)
                )
                BacGateApp(viewModel = vm)
            }
        }
    }
}
