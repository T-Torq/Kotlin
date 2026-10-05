package com.example.checklist.data

data class ChecklistApiModel(
    val id: String,
    val title: String,
    val borderColorHex: String = "#D3D3D3"
)

data class ChecklistItemApiModel(
    val id: String,
    val checklistId: String,
    val name: String,
    val isChecked: Boolean = false
)
