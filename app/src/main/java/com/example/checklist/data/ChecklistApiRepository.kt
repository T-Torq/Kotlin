package com.example.checklist.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

private const val BASE_URL = "https://dummyjson.com/"

open class ChecklistApiRepository(
    context: Context? = null,
    private val client: OkHttpClient = OkHttpClient()
) {
    protected class ApiException(message: String) : IOException(message)

    private val database: ChecklistDatabase? = context?.let { ChecklistDatabase.getDatabase(it) }
    private val checklistDao = database?.checklistDao()
    private val itemDao = database?.checklistItemDao()

    private suspend fun loadLocalChecklists(): List<ChecklistApiModel> {
        val entities = checklistDao?.getAll() ?: return emptyList()
        return entities.map { it.toApiModel() }
    }

    private suspend fun loadLocalItemsForChecklist(checklistId: String): List<ChecklistItemApiModel> {
        val entity = checklistDao?.getByApiId(checklistId) ?: return emptyList()
        val items = itemDao?.getItemsForChecklist(entity.id) ?: return emptyList()
        return items.map { it.toApiModel() }
    }

    private suspend fun loadLocalItemsMap(): MutableMap<String, List<ChecklistItemApiModel>> {
        val checklists = checklistDao?.getAll() ?: return linkedMapOf()
        val result = linkedMapOf<String, List<ChecklistItemApiModel>>()
        
        for (checklist in checklists) {
            val items = itemDao?.getItemsForChecklist(checklist.id) ?: emptyList()
            result[checklist.apiId] = items.map { it.toApiModel() }
        }
        return result
    }

    private suspend fun saveLocalState(
        checklists: List<ChecklistApiModel>,
        itemsByChecklist: Map<String, List<ChecklistItemApiModel>>
    ) {
        checklists.forEach { checklist ->
            val entity = ChecklistEntity(
                apiId = checklist.id,
                title = checklist.title,
                borderColorHex = checklist.borderColorHex
            )
            val existingEntity = checklistDao?.getByApiId(checklist.id)
            if (existingEntity != null) {
                checklistDao.update(entity.copy(id = existingEntity.id, borderColorHex = existingEntity.borderColorHex))
            } else {
                checklistDao?.insert(entity)
            }
        }

        itemsByChecklist.forEach { (checklistId, items) ->
            val checklistEntity = checklistDao?.getByApiId(checklistId) ?: return@forEach
            items.forEach { item ->
                val itemEntity = ChecklistItemEntity(
                    checklistId = checklistEntity.id,
                    apiId = item.id,
                    name = item.name,
                    isChecked = item.isChecked
                )
                val existingItem = itemDao?.getByApiId(item.id)
                if (existingItem != null) {
                    itemDao.update(itemEntity.copy(id = existingItem.id))
                } else {
                    itemDao?.insert(itemEntity)
                }
            }
        }
    }

    private fun buildUrl(path: String, queryParams: Map<String, String> = emptyMap()): String {
        val builder = BASE_URL.toHttpUrl().newBuilder()
            .addPathSegments(path)
            .apply {
                queryParams.forEach { (key, value) -> addQueryParameter(key, value) }
            }
            .build()
        return builder.toString()
    }

    private fun parseChecklist(item: JSONObject): ChecklistApiModel = ChecklistApiModel(
        id = item.optString("id"),
        title = item.optString("todo").ifBlank { item.optString("title").ifBlank { "Untitled" } }
    )

    private fun parseChecklistItem(item: JSONObject, checklistId: String): ChecklistItemApiModel = ChecklistItemApiModel(
        id = item.optString("id"),
        checklistId = checklistId,
        name = item.optString("todo").ifBlank { item.optString("name").ifBlank { "Item" } },
        isChecked = item.optBoolean("completed", false)
    )
    open suspend fun getChecklists(sortBy: String = "title", order: String = "asc"): List<ChecklistApiModel> = withContext(Dispatchers.IO) {
        val local = loadLocalChecklists()
        if (local.isNotEmpty()) return@withContext sortChecklists(local, sortBy, order)

        val queryParams = mutableMapOf("limit" to "30")
        if (sortBy.isNotBlank()) queryParams["sortBy"] = sortBy
        if (order.isNotBlank()) queryParams["order"] = order

        val request = Request.Builder()
            .url(buildUrl("todos", queryParams))
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw ApiException("Inga checklistor ännu. Tryck + för att lägga till en checklista")
            }

            val body = response.body?.string() ?: "{\"todos\":[]}"
            val root = JSONObject(body)
            val items = root.optJSONArray("todos") ?: JSONArray()
            val remoteList = MutableList(items.length()) { index ->
                parseChecklist(items.getJSONObject(index))
            }

            val itemsByChecklist = mutableMapOf<String, List<ChecklistItemApiModel>>()
            remoteList.forEach { checklist ->
                itemsByChecklist[checklist.id] = emptyList()
            }
            saveLocalState(remoteList, itemsByChecklist)
            return@withContext sortChecklists(remoteList, sortBy, order)
        }
    }

    private fun sortChecklists(
        checklists: List<ChecklistApiModel>,
        sortBy: String,
        order: String
    ): List<ChecklistApiModel> {
        val sorted = when (sortBy) {
            "title" -> checklists.sortedBy { it.title.lowercase() }
            "borderColor" -> checklists.sortedBy { it.borderColorHex }
            "createdAt" -> checklists.sortedBy { it.id.hashCode() }
            else -> checklists.sortedBy { it.title.lowercase() }
        }
        return if (order == "desc") sorted.reversed() else sorted
    }

    open suspend fun createChecklist(title: String): ChecklistApiModel = withContext(Dispatchers.IO) {
        if (title.isBlank()) {
            throw ApiException("Checklistans namn får inte vara tomt")
        }

        val created = ChecklistApiModel(
            id = "local-${System.currentTimeMillis()}",
            title = title
        )
        val currentLists = loadLocalChecklists().ifEmpty {
            getChecklists()
        }
        val currentItems = mutableMapOf<String, List<ChecklistItemApiModel>>()
        val existingItems = loadLocalItemsMap()
        existingItems.forEach { (key, items) ->
            currentItems[key] = items
        }
        currentItems[created.id] = emptyList()
        saveLocalState(currentLists + created, currentItems)
        return@withContext created
    }

    open suspend fun getChecklistItems(checklistId: String): List<ChecklistItemApiModel> = withContext(Dispatchers.IO) {
        if (checklistId.isBlank()) {
            throw ApiException("Checklist ID saknas")
        }

        val local = loadLocalItemsForChecklist(checklistId)
        if (local.isNotEmpty()) return@withContext local

        val request = Request.Builder()
            .url(buildUrl("todos/$checklistId"))
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw ApiException("Inga punkter ännu. Tryck + för att lägga till en punkt")
            }

            val body = response.body.string()
            val item = JSONObject(body)
            val result = listOf(parseChecklistItem(item, checklistId))
            val currentLists = loadLocalChecklists().ifEmpty { getChecklists() }
            val currentItems = loadLocalItemsMap()
            currentItems[checklistId] = result
            saveLocalState(currentLists, currentItems)
            return@withContext result
        }
    }

    open suspend fun createChecklistItem(checklistId: String, name: String): ChecklistItemApiModel = withContext(Dispatchers.IO) {
        if (checklistId.isBlank()) {
            throw ApiException("Checklist ID saknas")
        }
        if (name.isBlank()) {
            throw ApiException("Punktens namn får inte vara tomt")
        }

        val newItem = ChecklistItemApiModel(
            id = "item-${System.currentTimeMillis()}",
            checklistId = checklistId,
            name = name,
            isChecked = false
        )
        val currentItems = loadLocalItemsMap()
        currentItems[checklistId] = (currentItems[checklistId] ?: emptyList()) + newItem
        val currentLists = loadLocalChecklists().ifEmpty { getChecklists() }
        saveLocalState(currentLists, currentItems)
        return@withContext newItem
    }

    open suspend fun deleteChecklist(checklistId: String) = withContext(Dispatchers.IO) {
        if (checklistId.isBlank()) {
            throw ApiException("Checklist ID saknas")
        }

        checklistDao?.deleteByApiId(checklistId)
        val currentLists = loadLocalChecklists().ifEmpty { getChecklists() }
        val currentItems = loadLocalItemsMap().filterKeys { it != checklistId }
        saveLocalState(currentLists, currentItems)
    }

    open suspend fun deleteChecklistItem(checklistId: String, itemId: String) = withContext(Dispatchers.IO) {
        if (checklistId.isBlank()) {
            throw ApiException("Checklist ID saknas")
        }
        if (itemId.isBlank()) {
            throw ApiException("Check item ID saknas")
        }

        itemDao?.deleteByApiId(itemId)
        val currentItems = loadLocalItemsMap()
        val existing = currentItems[checklistId] ?: emptyList()
        currentItems[checklistId] = existing.filterNot { it.id == itemId }
        val currentLists = loadLocalChecklists().ifEmpty { getChecklists() }
        saveLocalState(currentLists, currentItems)
    }

    open suspend fun updateChecklist(checklistId: String, title: String): ChecklistApiModel = withContext(Dispatchers.IO) {
        if (checklistId.isBlank()) {
            throw ApiException("Checklist ID saknas")
        }
        if (title.isBlank()) {
            throw ApiException("Checklistans namn får inte vara tomt")
        }

        val currentLists = loadLocalChecklists().ifEmpty { getChecklists() }
        val updated = currentLists.map { checklist ->
            if (checklist.id == checklistId) checklist.copy(title = title) else checklist
        }
        val currentItems = loadLocalItemsMap()
        saveLocalState(updated, currentItems)
        return@withContext updated.first { it.id == checklistId }
    }

    open suspend fun updateChecklistItem(
        checklistId: String,
        itemId: String,
        name: String,
        isChecked: Boolean = false
    ): ChecklistItemApiModel = withContext(Dispatchers.IO) {
        if (checklistId.isBlank()) {
            throw ApiException("Checklist ID saknas")
        }
        if (itemId.isBlank()) {
            throw ApiException("Check item ID saknas")
        }
        if (name.isBlank()) {
            throw ApiException("Punktens namn får inte vara tomt")
        }

        val currentItems = loadLocalItemsMap()
        val updatedItems = (currentItems[checklistId] ?: emptyList()).map {
            if (it.id == itemId) it.copy(name = name, isChecked = isChecked) else it
        }
        currentItems[checklistId] = updatedItems
        val currentLists = loadLocalChecklists().ifEmpty { getChecklists() }
        val updated = updatedItems.first { it.id == itemId }
        saveLocalState(currentLists, currentItems)
        return@withContext updated.copy(name = name, isChecked = isChecked)
    }

    private fun ChecklistEntity.toApiModel() = ChecklistApiModel(
        id = apiId,
        title = title,
        borderColorHex = borderColorHex
    )

    private fun ChecklistItemEntity.toApiModel() = ChecklistItemApiModel(
        id = apiId,
        checklistId = "", // Will be set by caller if needed
        name = name,
        isChecked = isChecked
    )
}
