package com.app.lockapp

import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.Nullable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import androidx.room.Room
import com.app.lockapp.MainActivity.Companion.lockTimeDao
import com.app.lockapp.data.AppDatabase
import com.app.lockapp.data.LockTime
import com.app.lockapp.data.LockTimeDao
import com.app.lockapp.ui.theme.LockAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import restartApp
import setRestartPlan

class MainActivity : ComponentActivity() {

    companion object {
        lateinit var database: AppDatabase
        lateinit var lockTimeDao: LockTimeDao
        var isInstantlyLockedCompanion = false
        var isScheduledLockedCompanion = false
        lateinit var lockTimeLiveData: LiveData<List<LockTime>>
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

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "mainDatabase"
        ).build()

        lockTimeDao = database.lockTimeDao()
        lockTimeLiveData = lockTimeDao.getAllLiveData()

        setContent {
            LockAppTheme {
                // A surface container using the 'background' color from the theme
                var isLockedText by remember {
                    mutableStateOf(
                        if (isInstantlyLockedCompanion) {
                            "isLocked"
                        } else {
                            "isUnlocked"
                        }
                    )
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        BodyNextInfo(isLockedText)
                        LinesDayOfWeek()
                        Text(text = "Instant Lock")
                        Text(text = isLockedText)
                        Button(
                            onClick = {
                                isInstantlyLockedCompanion = !isInstantlyLockedCompanion
                                if (isInstantlyLockedCompanion) {
                                    isLockedText = "isLocked(instant)"
                                } else {
                                    isLockedText = "isUnlocked"
                                }
                            },
                        ) {
                            Text(text = "Lock")
                        }

                    }
                }
            }
        }

        requestPermission()
    }

    override fun onDestroy() {
        Log.d("■■■■■■■■■■■", "onDestroyが呼び出される")
        super.onDestroy()
        setRestartPlan(applicationContext)
        restartApp(applicationContext)
    }

    override fun onStop() {
        Log.d("■■■■■■■■■■■", "onStopが呼び出される")
        super.onStop()
        setRestartPlan(applicationContext)
        restartApp(applicationContext)
    }


    @Composable
    fun BodyNextInfo(ifLockedInstantly: String) {

        Log.d("■■■■■■■■■", "BodyNextInfo処理開始")
        var unLockedTimeByScheduling = remember { mutableStateOf("") }

        LaunchedEffect(unLockedTimeByScheduling) {
            GlobalScope.launch(Dispatchers.IO) {
                val unlockedTime = lockTimeDao.getNextToTimeIfScheduledLocking()

                if (unlockedTime != null) {

                    unLockedTimeByScheduling.value =
                        SimpleDateFormat("yyyy年MM月dd日 HH:mm").format(unlockedTime.getTime())
                    isScheduledLockedCompanion = true
                } else {
                    unLockedTimeByScheduling.value = ""
                    isScheduledLockedCompanion = false
                }

            }

        }
        if (unLockedTimeByScheduling.value != "") {
            Text(text = "isLocked(Schedule)", fontSize = 30.sp)
        } else {
            Text(text = ifLockedInstantly, fontSize = 30.sp)
        }

        Text(text = "解除日時：${unLockedTimeByScheduling.value}")
    }
}


@Composable
fun LinesDayOfWeek() {

    var lockTimeList = remember { mutableStateListOf<LockTime>() }
    val lifecycleOwner = LocalLifecycleOwner.current

    if (!MainActivity.lockTimeLiveData.hasObservers()) {
        MainActivity.lockTimeLiveData.observe(lifecycleOwner) {
            if (it.isEmpty()) {
                GlobalScope.launch(Dispatchers.IO) {
                    lockTimeDao.insertAllDefaultData()
                }
            } else {
                lockTimeList.clear()
                lockTimeList.addAll(it)
            }
        }

    }

    if (lockTimeList.isEmpty()) {
        Text(text = "Preparing")
    } else {
        LazyColumn {
            items(lockTimeList) { lockTimeData ->
                LineDayOfWeekUnit(lockTimeData)
            }
        }

    }
}

