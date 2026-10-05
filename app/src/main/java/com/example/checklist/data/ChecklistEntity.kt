package com.example.checklist.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "checklists")
data class ChecklistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val apiId: String,
    val title: String,
    val borderColorHex: String = "#D3D3D3",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)
