package com.app.lockapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.Nullable
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.room.Room
import com.app.lockapp.MainActivity.Companion.lockTimeDao
import com.app.lockapp.common.commonTranslateTimeIntToString
import com.app.lockapp.data.AppDatabase
import com.app.lockapp.data.LockTime
import com.app.lockapp.data.LockTimeDao
import com.app.lockapp.ui.theme.CommonColorSecondary
import com.app.lockapp.ui.theme.CommonColorTertiary
import com.app.lockapp.ui.theme.LockAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : ComponentActivity() {

    companion object {
        lateinit var database: AppDatabase
        lateinit var lockTimeDao: LockTimeDao
        var isLockedCompanion = false
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
                        if (isLockedCompanion) {
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

                    Column() {
                        Log.d("■■■■■■■■■■■■■■", "column")
                        LinesDayOfWeek()
                        Text(text = "Instant Lock")
                        Text(text = isLockedText)
                        Button(
                            onClick = {
                                isLockedCompanion = !isLockedCompanion
                                if (isLockedCompanion) {
                                    isLockedText = "isLocked"
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
        super.onDestroy()
        setRestartPlan()
        restartApp()
    }

    override fun onStop() {
        super.onStop()
        setRestartPlan()
        restartApp()
    }

    private fun restartApp() {
        //スケジュールドロックタイムの期間かどうか
        if (isLockedCompanion) {
            val context = applicationContext
            val intent = Intent(context, MainActivity().javaClass)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            ContextCompat.startActivity(context, intent, null)
        }
    }

    private fun setRestartPlan() {
        //明示的なBroadCast
        val intent = Intent(
            applicationContext,
            AlarmBroadcastReceiver::class.java
        )
        val pending: PendingIntent = PendingIntent.getBroadcast(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // アラームをセットする
        val am: AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        GlobalScope.launch(Dispatchers.IO) {
            val nextLockTime = lockTimeDao.getNextFromTime()
            if (nextLockTime != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextLockTime.timeInMillis,
                        pending
                    )
                } else {
                    am.setExact(AlarmManager.RTC_WAKEUP, nextLockTime.timeInMillis, pending)
                }

                Log.d("■■■■■■■■■■■","set alarmManager" + SimpleDateFormat("yyyy年MM月dd日 HH:mm").format(nextLockTime.getTime()))
            }
        }
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

@Composable
fun LineDayOfWeekUnit(
    lockTimeData: LockTime
) {
    // Value for storing time as a string
    val mTime = remember { mutableStateOf("") }
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .border(
                width = 0.5.dp,
                color = Color.DarkGray,
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .padding(all = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                modifier = Modifier.width(90.dp), text = lockTimeData.dayName
            )
            Text(
                modifier = Modifier.clickable {
                    // Creating a TimePicker dialod
                    val fromTimePickerDialog = TimePickerDialog(
                        context,
                        { _, mHour: Int, mMinute: Int ->
                            GlobalScope.launch(Dispatchers.IO) {
                                lockTimeDao.updateFromTime(lockTimeData.dayId, mHour, mMinute)
                            }
                        },
                        lockTimeData.fromTimeHour, lockTimeData.fromTimeMinute, false
                    )
                    fromTimePickerDialog.show()
                },
                text = commonTranslateTimeIntToString(
                    if (lockTimeData.fromTimeHour < 12) {
                        lockTimeData.fromTimeHour + 24
                    } else {
                        lockTimeData.fromTimeHour
                    },
                    lockTimeData.fromTimeMinute
                )
            )
            Text(text = "～")
            Text(text = "翌日")
            Text(
                modifier = Modifier.clickable {
                    // Creating a TimePicker dialod
                    val toTimePickerDialog = TimePickerDialog(
                        context,
                        { _, mHour: Int, mMinute: Int ->
                            GlobalScope.launch(Dispatchers.IO) {
                                lockTimeDao.updateToTime(lockTimeData.dayId, mHour, mMinute)
                            }
                        },
                        lockTimeData.toTimeHour, lockTimeData.toTimeMinute, false
                    )
                    toTimePickerDialog.show()
                },
                text = commonTranslateTimeIntToString(
                    lockTimeData.toTimeHour,
                    lockTimeData.toTimeMinute
                )
            )
            DayOfWeekButtonUnit(lockTimeData)
        }

    }
}


@Composable
fun DayOfWeekButtonUnit(lockTime: LockTime) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .size(30.dp, 30.dp)
            .toggleable(
                value = lockTime.enableLock,
                onValueChange = {
                    GlobalScope.launch(Dispatchers.IO) {
                        lockTimeDao.updateEnable(lockTime)
                    }
                }
            ),
        shape = CircleShape,
        color = if (lockTime.enableLock) CommonColorSecondary else CommonColorTertiary,
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (lockTime.enableLock) {
                    "ON"
                } else {
                    "OFF"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (lockTime.enableLock) Color.White else Color.Black,
            )
        }
    }
}
