package com.app.lockapp.data

import androidx.lifecycle.LiveData
import androidx.room.*
@Dao
interface AlarmPatternDao {
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

}