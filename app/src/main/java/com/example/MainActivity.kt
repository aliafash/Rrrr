package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.DaliliViewModel
import com.example.ui.screens.DaliliAppContainer
import com.example.ui.screens.parseHexColor
import com.example.ui.theme.DaliliTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: DaliliViewModel = viewModel()
            val parsedPrimary = parseHexColor(viewModel.primaryColorStr, Color(0xFF000000))
            val parsedSecondary = parseHexColor(viewModel.secondaryColorStr, Color(0xFFFFD700))

            DaliliTheme(
                primaryColor = parsedPrimary,
                secondaryColor = parsedSecondary
            ) {
                DaliliAppContainer(viewModel = viewModel)
            }
        }
    }
}
