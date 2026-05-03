package com.yosyoo.shopperassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.yosyoo.shopperassistant.ui.screens.ShopperAssistantApp
import com.yosyoo.shopperassistant.ui.theme.ShopperAssistantTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShopperAssistantTheme {
                ShopperAssistantApp()
            }
        }
    }
}
