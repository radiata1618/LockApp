import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.app.lockapp.AlarmBroadcastReceiver
import com.app.lockapp.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

fun restartApp(context: Context) {


    if (MainActivity.isInstantlyLockedCompanion||MainActivity.isScheduledLockedCompanion) {
        val context = context
        val intent = Intent(context, MainActivity().javaClass)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        ContextCompat.startActivity(context, intent, null)
    }
}

fun setRestartPlan(context: Context) {

    Log.d("■■■■■■■■■■■", "setRestartPlanが呼び出される")
    //明示的なBroadCast
    val intent = Intent(
        context,
        AlarmBroadcastReceiver::class.java
    )
    val pending: PendingIntent = PendingIntent.getBroadcast(
        context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // アラームをセットする
    val am: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    GlobalScope.launch(Dispatchers.IO) {
        val nextLockTime = MainActivity.lockTimeDao.getNextFromTime()
        if (nextLockTime != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Log.d("■■■■■■■■■■■", "★routeA★")
                am.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextLockTime.timeInMillis,
                    pending
                )
//                    am.setRepeating(AlarmManager.RTC_WAKEUP,calendar.timeInMillis+5000, 5000,pending)
            } else {
                Log.d("■■■■■■■■■■■", "routeB")
                am.setExact(AlarmManager.RTC_WAKEUP, nextLockTime.timeInMillis, pending)
//                    am.setRepeating(AlarmManager.RTC_WAKEUP,calendar.timeInMillis, 5000,pending)
            }

            Log.d(
                "■■■■■■■■■■■",
                "set alarmManager" + SimpleDateFormat("yyyy年MM月dd日 HH:mm").format(nextLockTime.getTime())
            )
        }
    }
}