package com.devsoto.monical

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.devsoto.monical.ui.navigation.MonicalNavHost
import com.devsoto.monical.ui.theme.MonicalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MonicalTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MonicalNavHost(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
