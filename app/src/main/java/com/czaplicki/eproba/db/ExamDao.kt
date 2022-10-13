package com.czaplicki.eproba.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams")
    fun getAll(): Flow<List<Exam>>

    @Query("SELECT * FROM exams WHERE userId = :userId")
    fun getExamsByUserId(userId: Long): Flow<List<Exam>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExams(vararg exam: Exam)

    @Update
    suspend fun updateExams(vararg exam: Exam)

    @Delete
    suspend fun deleteExams(vararg exam: Exam)

    @Query("SELECT * FROM exams WHERE id = :id")
    suspend fun loadExamsById(id: Long): Exam

}