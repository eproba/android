package com.czaplicki.eproba.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams")
    fun getAll(): Flow<List<Exam>>

    @Query("SELECT * FROM exams")
    suspend fun getAllNow(): List<Exam>

    @Query("SELECT * FROM exams WHERE userId = :userId")
    fun getExamsByUserId(userId: Long): Flow<List<Exam>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg exam: Exam)

    @Update
    suspend fun update(vararg exam: Exam)

    @Delete
    suspend fun delete(vararg exam: Exam)

    @Query("DELETE FROM exams")
    suspend fun nukeTable()

    @Query("SELECT * FROM exams WHERE id = :id")
    suspend fun getNow(id: Long): Exam?

}