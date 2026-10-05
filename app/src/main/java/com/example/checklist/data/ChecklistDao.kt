package com.example.checklist.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface ChecklistDao {
    @Query("SELECT * FROM checklists ORDER BY createdAt DESC")
    suspend fun getAll(): List<ChecklistEntity>

    @Query("SELECT * FROM checklists WHERE apiId = :apiId")
    suspend fun getByApiId(apiId: String): ChecklistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(checklist: ChecklistEntity): Long

    @Update
    suspend fun update(checklist: ChecklistEntity)

    @Query("DELETE FROM checklists WHERE apiId = :apiId")
    suspend fun deleteByApiId(apiId: String)

    @Query("UPDATE checklists SET borderColorHex = :colorHex WHERE apiId = :apiId")
    suspend fun updateBorderColor(apiId: String, colorHex: String)
}

@Dao
interface ChecklistItemDao {
    @Query("SELECT * FROM checklist_items WHERE checklistId = :checklistId ORDER BY createdAt ASC")
    suspend fun getItemsForChecklist(checklistId: Long): List<ChecklistItemEntity>

    @Query("SELECT * FROM checklist_items WHERE apiId = :apiId")
    suspend fun getByApiId(apiId: String): ChecklistItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ChecklistItemEntity): Long

    @Update
    suspend fun update(item: ChecklistItemEntity)

    @Query("DELETE FROM checklist_items WHERE apiId = :apiId")
    suspend fun deleteByApiId(apiId: String)
}
