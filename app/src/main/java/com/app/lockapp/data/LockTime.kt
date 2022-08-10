package com.app.lockapp.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "lockTime")
data class LockTime (
    @PrimaryKey val dayId: Int,
    val dayName: String,
    val fromTimeHour:Int,
    val fromTimeMinute:Int,
    val fromBeforeDay:Boolean,
    val toTimeHour:Int,
    val toTimeMinute:Int,
    val enableLock:Boolean
): Parcelable


