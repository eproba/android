package com.czaplicki.eproba.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface WorksheetDao {
    @Query("SELECT * FROM worksheets")
    fun getAll(): Flow<List<Worksheet>>

    @Query("SELECT * FROM worksheets")
    suspend fun getAllNow(): List<Worksheet>

    @Query("SELECT * FROM worksheets WHERE userId = :userId")
    fun getWorksheetsByUserId(userId: UUID): Flow<List<Worksheet>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg worksheet: Worksheet)

    @Update
    suspend fun update(vararg worksheet: Worksheet)

    @Delete
    suspend fun delete(vararg worksheet: Worksheet)

    @Query("DELETE FROM worksheets")
    suspend fun nukeTable()

    @Query("SELECT * FROM worksheets WHERE id = :id")
    suspend fun getNow(id: UUID): Worksheet?

}