package com.starklosch.invernadero

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.*
import com.juul.kable.Advertisement
import com.juul.kable.Bluetooth
import com.starklosch.invernadero.ui.theme.InvernaderoTheme
import kotlin.math.*

class SettingActivity : ComponentActivity() {
    var setting : String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        intent?.getStringExtra("setting")?.let { setting = it }

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

@Composable
private fun Main(){
    val activity = LocalContext.current as SettingActivity
    var pos by remember { mutableStateOf(0.5f) }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ){
        Text("Hola")
        Button(onClick = {
            val offset = 0.01f
            val min = convert(pos - offset, true)
            val max = convert(pos + offset, true)
            
            val intent = Intent()
            intent.putExtra("setting", activity.setting)
            intent.putExtra("min", min)
            intent.putExtra("max", max)
            activity.setResult(Activity.RESULT_OK, intent)
            activity.finish()
        }){
            Text("Test")
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
             text = "${convert(pos, true)}",
          //   modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Slider(
            value = pos,
         //   steps = 3,
            onValueChange = { pos = it }
        )
    }
}

fun convert(value : Float, exponential : Boolean) : Int {
    if (exponential)
        return 1000000f.pow(value).roundToInt()
    
    return (value * 100f).roundToInt()
}