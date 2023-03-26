package com.starklosch.invernadero

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.starklosch.invernadero.extensions.parcelable
import com.starklosch.invernadero.ui.theme.InvernaderoTheme

class DebugActivity : ComponentActivity() {
    private var values: Values? = null
    private var info: Information? = null
    private var settings: Settings? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        with(intent) {
            settings = parcelable(EXTRA_SETTINGS)
            info = parcelable(EXTRA_INFO)
            values = parcelable(EXTRA_VALUES)
        }

        setContent {
            InvernaderoTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Main(info, settings, values)
                }
            }
        }
    }

    companion object {
        const val EXTRA_INFO = "INFO"
        const val EXTRA_SETTINGS = "SETTINGS"
        const val EXTRA_VALUES = "VALUES"
    }
}

fun String.replaces() = trimEnd(')').replace("(", ":\n").replace(", ", "\n").replace("=", " = ")

@Composable
private fun Main(info: Information?, settings: Settings?, values: Values?) {
    val padding = Modifier.padding(vertical = 16.dp)
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        info?.let { item { Text(it.toString().replaces()); Divider(modifier = padding) } }
        settings?.let { item { Text(it.toString().replaces()); Divider(modifier = padding) } }
        values?.let { item { Text(it.toString().replaces()) } }
    }
}
//
//fun testFlow() = flow {
//    for (i in 1..10){
//        emit(i)
//        delay(400)
//    }
//}
//
//suspend fun collect(){
//    withTimeoutOrNull(1000) {
//        var n = 0
//        testFlow().collect {
//            println(it)
//            if (it == 5){
//                n = it
//                cancel()
//            }
//        }
//        n
//    }
//}
//
//fun main() {
//    println("Hello, world!!!")
//    runBlocking {
//        collect()
//    }
//}