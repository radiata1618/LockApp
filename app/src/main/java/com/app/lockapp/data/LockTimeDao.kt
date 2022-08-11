package com.app.lockapp.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.room.*
import java.util.*

@Dao
interface LockTimeDao {
    @Insert
    fun insert(lockTime: LockTime): Long

    @Update
    fun update(lockTime: LockTime)

    @Query("delete from lockTime where dayId = :id")
    fun delete(id: Int)

    @Query("delete from lockTime")
    fun deleteAll()

    @Query("select * from lockTime")
    fun getAll(): List<LockTime>

    @Query("SELECT * FROM lockTime ORDER BY dayId ASC")
    fun getAllLiveData(): LiveData<List<LockTime>>

    @Query("select * from lockTime where dayId = :id")
    fun getLockTime(id: Int): LockTime

    @Query("select * from lockTime where dayId = :id")
    fun getLockTimeLiveData(id: Int): LiveData<LockTime>


    fun getNextToTimeIfScheduledLocking(): Calendar? {

        val cal = Calendar.getInstance()

        //昨日の曜日をまずはチェック

        var dayOfWeekToday = cal.get(Calendar.DAY_OF_WEEK)
        var dayOfWeekYesterday = cal.get(Calendar.DAY_OF_WEEK) - 1
        if (dayOfWeekYesterday == 0) {
            dayOfWeekYesterday = 7
        }

        var lockTimeDataYesterday = getLockTime(dayOfWeekYesterday)

        var tmpBoolean=false
        //前日チェック
        if (lockTimeDataYesterday.enableLock) {//有効なスケジュール

            tmpBoolean =
                        lockTimeDataYesterday.fromTimeHour > 12
                        || lockTimeDataYesterday.fromTimeHour < cal.get(Calendar.HOUR_OF_DAY)
                        ||( (lockTimeDataYesterday.fromTimeHour == cal.get(Calendar.HOUR_OF_DAY)) && (lockTimeDataYesterday.fromTimeMinute < cal.get(Calendar.MINUTE)))

            if (tmpBoolean) {//Fromを過ぎているかどうか

                tmpBoolean = cal.get(Calendar.HOUR_OF_DAY) < lockTimeDataYesterday.toTimeHour
                            ||( (cal.get(Calendar.HOUR_OF_DAY) == lockTimeDataYesterday.toTimeHour ) && (cal.get(Calendar.MINUTE) < lockTimeDataYesterday.toTimeMinute))

                Log.d("■■■■■■■■■","今の時"+cal.get(Calendar.HOUR_OF_DAY).toString())
                Log.d("■■■■■■■■■","lockTimeDataYesterday.toTimeHour:"+lockTimeDataYesterday.toTimeHour)
                Log.d("■■■■■■■■■",tmpBoolean.toString()+"前日チェックToの値")
                if (tmpBoolean) {//TO以前かどうか

                    Log.d("■■■■■■■■■","前日チェックTOの前")
                    cal.set(Calendar.HOUR_OF_DAY,lockTimeDataYesterday.toTimeHour)
                    cal.set(Calendar.MINUTE,lockTimeDataYesterday.toTimeMinute)
                    return cal

                }
            }
        }

        var lockTimeDataToday = getLockTime(dayOfWeekToday)
        //当日チェック
        if (lockTimeDataToday.enableLock) {//有効なスケジュール

            val tmpBoolean =
                        12 <= lockTimeDataToday.fromTimeHour
                        && (lockTimeDataToday.fromTimeHour < cal.get(Calendar.HOUR_OF_DAY)
                        ||( (lockTimeDataToday.fromTimeHour == cal.get(Calendar.HOUR_OF_DAY)) && (lockTimeDataToday.fromTimeMinute < cal.get(Calendar.MINUTE))))

            if (tmpBoolean) {//Fromを過ぎているかどうか

                Log.d("■■■■■■■■■","当日チェックFromを過ぎている")
                cal.set(Calendar.HOUR_OF_DAY,lockTimeDataToday.toTimeHour)
                cal.set(Calendar.MINUTE,lockTimeDataToday.toTimeMinute)
                cal.add(Calendar.DATE,1)
                return cal
            }
        }

        return null
    }

    fun getNextFromTime(): Calendar? {
        val now = Calendar.getInstance()

        //昨日の曜日をまずはチェック
        var dayOfWeek = now.get(Calendar.DAY_OF_WEEK) - 1
        if (dayOfWeek == 0) {
            dayOfWeek = 7
        }

        Log.d("■■■■■■■■■■■■WeekId", dayOfWeek.toString())
        var lockTimeData = getLockTime(dayOfWeek)

        if (lockTimeData.enableLock && lockTimeData.fromTimeHour < 12) {
            val calendarTmp =
                getCalendarTodayByHourMinute(lockTimeData.fromTimeHour, lockTimeData.fromTimeMinute)
            if (Calendar.getInstance() < getCalendarTodayByHourMinute(
                    lockTimeData.fromTimeHour,
                    lockTimeData.fromTimeMinute
                )
            ) {
                return calendarTmp
            }
        }

        //先1週間を順にチェック
        for (i in 1..7) {
            dayOfWeek += 1
            if (dayOfWeek > 7) {
                dayOfWeek -= 7
            }
            Log.d("■■■■■■■■■■■■", dayOfWeek.toString()+"曜日")
            lockTimeData = getLockTime(dayOfWeek)
            if (lockTimeData.enableLock) {
                val calendarTmp = getCalendarTodayByHourMinute(
                    lockTimeData.fromTimeHour,
                    lockTimeData.fromTimeMinute
                )
                calendarTmp.add(Calendar.DATE, i - 1)
                //翌日かどうかで場合分け
                if (lockTimeData.fromTimeHour < 12) {
                    calendarTmp.add(Calendar.DATE, 1)
                }
                //過ぎた日付じゃなければ終わり
                if(now.before(calendarTmp)){
                    return calendarTmp
                }
            }
        }
        return null
    }

    fun getCalendarTodayByHourMinute(hour: Int, minute: Int): Calendar {

        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        return cal
    }

    fun insertAllDefaultData() {
        insertDefaultDataUnit(1, "Sunday")
        insertDefaultDataUnit(2, "Monday")
        insertDefaultDataUnit(3, "Tuesday")
        insertDefaultDataUnit(4, "Wednesday")
        insertDefaultDataUnit(5, "Thursday")
        insertDefaultDataUnit(6, "Friday")
        insertDefaultDataUnit(7, "Saturday")
    }

    fun insertDefaultDataUnit(dayId: Int, dayName: String) {
        insert(
            LockTime(
                dayId,
                dayName,
                23,
                0,
                false,
                6,
                0,
                false
            )
        )
    }

    fun updateEnable(lockTime: LockTime) {
        Log.d("■■■■■■■■■■■■■■", "updateEnable")
        update(
            LockTime(
                lockTime.dayId,
                lockTime.dayName,
                lockTime.fromTimeHour,
                lockTime.fromTimeMinute,
                lockTime.fromBeforeDay,
                lockTime.toTimeHour,
                lockTime.toTimeMinute,
                !lockTime.enableLock,
            )
        )

    }

    fun updateFromTime(dayId: Int, fromTimeHour: Int, fromTimeMinute: Int) {

        var lockTime = getLockTime(dayId)

        update(
            LockTime(
                lockTime.dayId,
                lockTime.dayName,
                fromTimeHour,
                fromTimeMinute,
                calcFromBeforeDay(
                    fromTimeHour,
                    fromTimeMinute,
                    lockTime.toTimeHour,
                    lockTime.toTimeMinute
                ),
                lockTime.toTimeHour,
                lockTime.toTimeMinute,
                lockTime.enableLock,
            )
        )
    }

    fun updateToTime(dayId: Int, toTimeHour: Int, toTimeMinute: Int) {

        var lockTime = getLockTime(dayId)

        update(
            LockTime(
                lockTime.dayId,
                lockTime.dayName,
                lockTime.fromTimeHour,
                lockTime.fromTimeMinute,
                calcFromBeforeDay(
                    lockTime.fromTimeHour,
                    lockTime.fromTimeMinute,
                    toTimeHour,
                    toTimeMinute
                ),
                toTimeHour,
                toTimeMinute,
                lockTime.enableLock,
            )
        )
    }

    fun calcFromBeforeDay(
        fromTimeHour: Int,
        fromTimeMinute: Int,
        toTimeHour: Int,
        toTimeMinute: Int
    ): Boolean {

        return if (fromTimeHour < toTimeHour) {
            true;
        } else if (fromTimeHour == toTimeHour) {

            if (fromTimeMinute < toTimeMinute) {
                true;
            } else if (fromTimeMinute == toTimeMinute) {
                false;

            } else {
                false;
            }
        } else {
            false;
        }

    }

}