package com.starklosch.invernadero

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.starklosch.invernadero.ui.theme.InvernaderoTheme
import kotlin.math.roundToInt

class SettingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InvernaderoTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Main()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Main() {
    var min by remember { mutableStateOf("0") }
    var max by remember { mutableStateOf("100") }
    var desired by remember { mutableStateOf(.5f) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        TextField(label = { Text("Min") }, value = min, onValueChange = { min = it })
        Spacer(Modifier.height(16.dp))
        TextField(label = { Text("Max") }, value = max, onValueChange = { max = it })
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("0")
            Text("${(desired * 100).roundToInt()}")
            Text("100")
        }
        RangedSlider(desired, onValueChange = { desired = it })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangedSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    /*@IntRange(from = 0)*/
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished
    )
}