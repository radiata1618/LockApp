package com.app.lockapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.Nullable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.app.lockapp.ui.theme.LockAppTheme

class MainActivity : ComponentActivity() {

    companion object {
        var isLockedCompanion =false
    }

    private val REQUEST_PERMISSION_CODE = 1

    // SYSTEM_ALERT_WINDOWが許可されているかのチェック
    fun isGranted(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    // SYSTEM_ALERT_WINDOWの許可をリクエストする
    private fun requestPermission() {
        if (Settings.canDrawOverlays(this)) {
            // 許可されたときの処理
        } else {
            val uri: Uri = Uri.parse("package:$packageName")
            val intent: Intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri)
            startActivityForResult(intent, REQUEST_PERMISSION_CODE)
        }
    }

    // 許可されたかの確認は、onActivityResultでチェックする
    override fun onActivityResult(requestCode: Int, resultCode: Int, @Nullable data: Intent?) {
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (Settings.canDrawOverlays(this)) {
                // 許可されたときの処理
            } else {
                // 拒否されたときの処理
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LockAppTheme {
                // A surface container using the 'background' color from the theme
                var isLockedText by remember { mutableStateOf(if(isLockedCompanion){"isLocked"}else{"isUnlocked"}) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(){
                        Text(text = isLockedText)
                        Button(
                            onClick = {
                                isLockedCompanion = !isLockedCompanion
                                if(isLockedCompanion){
                                    isLockedText="isLocked"
                                }else{
                                    isLockedText="isUnlocked"
                                }
                            },
                        ){
                            Text(text = "Lock")
                        }

                    }
                }
            }
        }

        requestPermission()
    }

    override fun onDestroy(){
        super.onDestroy()

        if(isLockedCompanion){
            restartApp()
        }
    }

    override fun onStop() {
        super.onStop()

        if(isLockedCompanion){
            restartApp()
        }
    }


    private fun restartApp(){
        val context = applicationContext
        val intent = Intent(context, MainActivity().javaClass)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        ContextCompat.startActivity(context, intent, null)
    }
}

@Composable
fun LinesDayOfWeek() {
    LazyColumn {
        items(mainAlarmPatternList) { alarmPatternData ->
            LineDayOfWeekUnit()
        }
    }
}
@Composable
fun LineDayOfWeekUnit() {
    LazyColumn {
        items(mainAlarmPatternList) { alarmPatternData ->
            AlarmPatternListUnit(
                alarmPatternData,
                navController
            )
        }
    }
}