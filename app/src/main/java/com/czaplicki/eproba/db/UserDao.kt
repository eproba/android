package com.czaplicki.eproba.db

import androidx.room.*

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    suspend fun getAll(): List<User>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg users: User)

    @Update
    suspend fun update(vararg users: User)

    @Delete
    suspend fun delete(vararg users: User)

    @Query("DELETE FROM users")
    suspend fun nukeTable()

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun loadUserById(id: Int): User

}