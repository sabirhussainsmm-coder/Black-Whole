package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.ui.BlackHoleApp
import com.example.ui.BlackHoleViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  private val viewModel: BlackHoleViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme(darkTheme = true) {
        BlackHoleApp(
          viewModel = viewModel,
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }

  override fun onResume() {
    super.onResume()
    viewModel.checkClipboard()
  }
}

