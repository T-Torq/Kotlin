package com.example.checklist

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.example.checklist.data.ChecklistApiModel
import com.example.checklist.data.ChecklistApiRepository
import com.example.checklist.data.ChecklistDatabase
import com.example.checklist.ui.theme.ChecklistTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        setContent {
            ChecklistTheme {
                ChecklistHomeScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistHomeScreen(
    modifier: Modifier = Modifier,
    repository: ChecklistApiRepository? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = repository ?: remember(context) { ChecklistApiRepository(context.applicationContext) }

    var checklists by remember { mutableStateOf<List<ChecklistApiModel>>(emptyList()) }
    var selectedChecklist by remember { mutableStateOf<ChecklistApiModel?>(null) }
    var checklistItems by remember { mutableStateOf<List<com.example.checklist.data.ChecklistItemApiModel>>(emptyList()) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showCreateItemDialog by remember { mutableStateOf(false) }
    var newChecklistName by remember { mutableStateOf("") }
    var newItemText by remember { mutableStateOf("") }
    var checklistSearchQuery by remember { mutableStateOf("") }
    var showChecklistSearchBar by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var detailErrorMessage by remember { mutableStateOf<String?>(null) }
    var checklistBeingEdited by remember { mutableStateOf<ChecklistApiModel?>(null) }
    var itemBeingEdited by remember { mutableStateOf<com.example.checklist.data.ChecklistItemApiModel?>(null) }
    var editText by remember { mutableStateOf("") }
    var selectedBorderColorHex by remember { mutableStateOf("#D3D3D3") }
    var newChecklistBorderColorHex by remember { mutableStateOf("#D3D3D3") }
    var showSortMenu by remember { mutableStateOf(false) }
    var sortBy by remember { mutableStateOf("title") }
    var sortOrder by remember { mutableStateOf("asc") }

    fun loadChecklists() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val checklistList = repo.getChecklists(sortBy, sortOrder)
                checklists = checklistList.map { checklist ->
                    checklist.copy(borderColorHex = loadChecklistBorderColor(context, checklist))
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Kunde inte hämta checklistor"
            } finally {
                isLoading = false
            }
        }
    }

    fun loadChecklistItems(checklistId: String) {
        scope.launch {
            try {
                detailErrorMessage = null
                checklistItems = repo.getChecklistItems(checklistId)
            } catch (e: Exception) {
                detailErrorMessage = e.message ?: "Kunde inte hämta punkter"
            }
        }
    }

    val filteredChecklists = remember(checklists, checklistSearchQuery) {
        if (checklistSearchQuery.isBlank()) checklists else checklists.filter {
            it.title.contains(checklistSearchQuery, ignoreCase = true)
        }
    }

    LaunchedEffect(Unit) {
        loadChecklists()
    }

    if (selectedChecklist != null) {
        ChecklistDetailScreen(
            checklist = selectedChecklist!!,
            items = checklistItems,
            onBack = {
                selectedChecklist = null
                showCreateItemDialog = false
                newItemText = ""
                checklistItems = emptyList()
                detailErrorMessage = null
                showChecklistSearchBar = false
                checklistSearchQuery = ""
            },
            onShowCreateItemDialog = { showCreateItemDialog = true },
            onEditItem = {
                itemBeingEdited = it
                editText = it.name
            },
            onToggleItem = { item ->
                scope.launch {
                    try {
                        detailErrorMessage = null
                        val updated = repo.updateChecklistItem(
                            checklistId = selectedChecklist!!.id,
                            itemId = item.id,
                            name = item.name,
                            isChecked = !item.isChecked
                        )
                        checklistItems = checklistItems.map {
                            if (it.id == item.id) updated else it
                        }
                    } catch (e: Exception) {
                        detailErrorMessage = e.message ?: "Kunde inte uppdatera punkt"
                    }
                }
            },
            errorMessage = detailErrorMessage
        )

        if (showCreateItemDialog) {
            val keyboardController = LocalSoftwareKeyboardController.current
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(showCreateItemDialog) {
                if (showCreateItemDialog) {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                }
            }

            AlertDialog(
                onDismissRequest = {
                    showCreateItemDialog = false
                    newItemText = ""
                },
                title = { Text("Ny punkt") },
                text = {
                    OutlinedTextField(
                        value = newItemText,
                        onValueChange = { newItemText = it },
                        label = { Text("Namn") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            disabledContainerColor = Color.White,
                            errorContainerColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val text = newItemText.trim()
                            if (text.isNotBlank()) {
                                scope.launch {
                                    try {
                                        detailErrorMessage = null
                                        val createdItem = repo.createChecklistItem(
                                            selectedChecklist!!.id,
                                            text
                                        )
                                        checklistItems = checklistItems + createdItem
                                        newItemText = ""
                                        showCreateItemDialog = false
                                    } catch (e: Exception) {
                                        detailErrorMessage = e.message ?: "Kunde inte skapa punkt"
                                    }
                                }
                            }
                        }
                    ) {
                        Text("Skapa")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showCreateItemDialog = false
                            newItemText = ""
                        }
                    ) {
                        Text("Avbryt")
                    }
                }
            )
        }
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("Checklistor") },
                    actions = {
                        IconButton(
                            onClick = { showSortMenu = !showSortMenu }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sortera checklistor")
                        }
                        if (showSortMenu) {
                            androidx.compose.material3.DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                modifier = Modifier
                                    .width(200.dp)
                                    .border(1.dp, Color.Gray)
                            ) {
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text("Namn") },
                                    onClick = {
                                        sortBy = "title"
                                        showSortMenu = false
                                        loadChecklists()
                                    }
                                )
                                HorizontalDivider(Modifier, thickness = 1.dp, color = Color.LightGray)
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text("Färg") },
                                    onClick = {
                                        sortBy = "borderColor"
                                        showSortMenu = false
                                        loadChecklists()
                                    }
                                )
                                HorizontalDivider(Modifier, thickness = 1.dp, color = Color.LightGray)
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text("Datum (Skapad )") },
                                    onClick = {
                                        sortBy = "createdAt"
                                        showSortMenu = false
                                        loadChecklists()
                                    }
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                sortOrder = if (sortOrder == "asc") "desc" else "asc"
                                loadChecklists()
                            }
                        ) {
                            Icon(
                                imageVector = if (sortOrder == "asc") Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = "Vända sorteringsriktning"
                            )
                        }
                        if (!showChecklistSearchBar) {
                            IconButton(
                                onClick = { showChecklistSearchBar = true }
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Sök checklistor")
                            }
                        }
                        IconButton(
                            onClick = { showCreateDialog = true }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Skapa ny checklista")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    if (showChecklistSearchBar) {
                        val keyboardController = LocalSoftwareKeyboardController.current
                        val focusRequester = remember { FocusRequester() }
                        LaunchedEffect(showChecklistSearchBar) {
                            if (showChecklistSearchBar) {
                                delay(150.milliseconds)
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            }
                        }

                        OutlinedTextField(
                            value = checklistSearchQuery,
                            onValueChange = { checklistSearchQuery = it },
                            placeholder = { Text("Sök checklistor") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Sök checklistor")
                            },
                            trailingIcon = {
                                if (checklistSearchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            checklistSearchQuery = ""
                                        }
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Rensa sökning")
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = Color.White,
                                focusedContainerColor = Color.White,
                                disabledContainerColor = Color.White,
                                errorContainerColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                        Spacer(modifier = Modifier.padding(top = 12.dp))
                    }

                    HorizontalDivider(Modifier, thickness = 1.dp, color = Color.LightGray)

                    when {
                        isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        errorMessage != null -> {
                            Text(
                                text = errorMessage ?: "Okänt fel",
                                modifier = Modifier.padding(top = 8.dp),
                                color = Color.Red
                            )
                        }

                        checklists.isEmpty() -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Det finns inga checklistor än",
                                    modifier = Modifier.padding(16.dp),
                                    color = Color.Gray
                                )
                            }
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp)
                            ) {
                                items(filteredChecklists) { checklist ->
                                    ChecklistRow(
                                        checklist = checklist,
                                        onClick = {
                                            selectedChecklist = checklist
                                            if (checklistSearchQuery.isBlank()) {
                                                showChecklistSearchBar = false
                                            }
                                            loadChecklistItems(checklist.id)
                                        },
                                        onLongClick = {
                                            checklistBeingEdited = checklist
                                            editText = checklist.title
                                            selectedBorderColorHex = checklist.borderColorHex
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(showCreateDialog) {
            if (showCreateDialog) {
                delay(150.milliseconds)
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Ny checklista") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newChecklistName,
                        onValueChange = { newChecklistName = it },
                        label = { Text("Namn") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            disabledContainerColor = Color.White,
                            errorContainerColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                    BorderColorPicker(
                        selectedColorHex = newChecklistBorderColorHex,
                        onColorSelected = { newChecklistBorderColorHex = it }
                    )
                }

            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newChecklistName.isNotBlank()) {
                            scope.launch {
                                try {
                                    val created = repo.createChecklist(
                                        newChecklistName.trim()
                                    )
                                    val checklistWithColor = created.copy(borderColorHex = newChecklistBorderColorHex)
                                    saveChecklistBorderColor(context, checklistWithColor)
                                    checklists = listOf(checklistWithColor) + checklists
                                    newChecklistName = ""
                                    newChecklistBorderColorHex = "#D3D3D3"
                                    showCreateDialog = false
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Kunde inte skapa checklista"
                                    showCreateDialog = false
                                }
                            }
                        }
                    }
                ) {
                    Text("Skapa")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    newChecklistName = ""
                    newChecklistBorderColorHex = "#D3D3D3"
                }) {
                    Text("Avbryt")
                }
            }
        )
    }

    if (checklistBeingEdited != null) {
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(checklistBeingEdited) {
            if (checklistBeingEdited != null) {
                delay(150.milliseconds)
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        }

        AlertDialog(
            onDismissRequest = {
                checklistBeingEdited = null
                editText = ""
                selectedBorderColorHex = "#D3D3D3"
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ändra namn på checklista",
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val checklist = checklistBeingEdited
                            if (checklist != null) {
                                scope.launch {
                                    try {
                                        errorMessage = null
                                        repo.deleteChecklist(checklist.id)
                                        checklists = checklists.filterNot { it.id == checklist.id }
                                        clearChecklistBorderColor(context, checklist.id)
                                        if (selectedChecklist?.id == checklist.id) {
                                            selectedChecklist = null
                                            checklistItems = emptyList()
                                            newItemText = ""
                                        }
                                        checklistBeingEdited = null
                                        editText = ""
                                        selectedBorderColorHex = "#D3D3D3"
                                    } catch (e: Exception) {
                                        errorMessage = e.message ?: "Kunde inte ta bort checklista"
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Ta bort checklista")
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        label = { Text("Namn") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White,
                            disabledContainerColor = Color.White,
                            errorContainerColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .testTag("edit-name-field")
                    )
                    BorderColorPicker(
                        selectedColorHex = selectedBorderColorHex,
                        onColorSelected = { selectedBorderColorHex = it }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val checklist = checklistBeingEdited
                        val newTitle = editText.trim()
                        if (checklist != null && newTitle.isNotBlank()) {
                            scope.launch {
                                try {
                                    errorMessage = null
                                    val updated = repo.updateChecklist(
                                        checklist.id,
                                        newTitle
                                    )
                                    checklists = checklists.map {
                                        if (it.id == checklist.id) {
                                            updated.copy(
                                                title = newTitle,
                                                borderColorHex = selectedBorderColorHex
                                            )
                                        } else it
                                    }
                                    saveChecklistBorderColor(
                                        context,
                                        updated.copy(
                                            title = newTitle,
                                            borderColorHex = selectedBorderColorHex
                                        )
                                    )
                                    if (selectedChecklist?.id == checklist.id) {
                                        selectedChecklist = updated.copy(
                                            title = newTitle,
                                            borderColorHex = selectedBorderColorHex
                                        )
                                    }
                                    checklistBeingEdited = null
                                    editText = ""
                                    selectedBorderColorHex = "#D3D3D3"
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Kunde inte ändra checklista"
                                }
                            }
                        }
                    }
                ) {
                    Text("Spara")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        checklistBeingEdited = null
                        editText = ""
                        selectedBorderColorHex = "#D3D3D3"
                    }
                ) {
                    Text("Avbryt")
                }
            }
        )
    }

    if (itemBeingEdited != null && selectedChecklist != null) {
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(itemBeingEdited) {
            if (itemBeingEdited != null) {
                delay(150.milliseconds)
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        }

        AlertDialog(
            onDismissRequest = {
                itemBeingEdited = null
                editText = ""
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ändra namn på punkt",
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val item = itemBeingEdited
                            val checklist = selectedChecklist
                            if (item != null && checklist != null) {
                                scope.launch {
                                    try {
                                        detailErrorMessage = null
                                        repo.deleteChecklistItem(checklist.id, item.id)
                                        checklistItems = checklistItems.filterNot { it.id == item.id }
                                        itemBeingEdited = null
                                        editText = ""
                                    } catch (e: Exception) {
                                        detailErrorMessage = e.message ?: "Kunde inte ta bort punkt"
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Ta bort punkt")
                    }
                }
            },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    label = { Text("Namn") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        errorContainerColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("edit-name-field")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val item = itemBeingEdited
                        val checklist = selectedChecklist
                        val newName = editText.trim()
                        if (item != null && checklist != null && newName.isNotBlank()) {
                            scope.launch {
                                try {
                                    detailErrorMessage = null
                                    val updated = repo.updateChecklistItem(
                                        checklistId = checklist.id,
                                        itemId = item.id,
                                        name = newName,
                                        isChecked = item.isChecked
                                    )
                                    checklistItems = checklistItems.map {
                                        if (it.id == item.id) updated.copy(name = newName) else it
                                    }
                                    itemBeingEdited = null
                                    editText = ""
                                } catch (e: Exception) {
                                    detailErrorMessage = e.message ?: "Kunde inte ändra punkt"
                                }
                            }
                        }
                    }
                ) {
                    Text("Spara")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        itemBeingEdited = null
                        editText = ""
                    }
                ) {
                    Text("Avbryt")
                }
            }
        )
    }
}

@Composable
fun BorderColorPicker(
    selectedColorHex: String,
    onColorSelected: (String) -> Unit
) {
    val colors = listOf("#D3D3D3", "#F44336", "#FF9800", "#FFEB3B", "#4CAF50", "#2196F3", "#9C27B0")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Färg på ram")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            colors.forEach { colorHex ->
                val color = Color(colorHex.toColorInt())
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .scale(if (selectedColorHex == colorHex) 1.05f else 1f)
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onColorSelected(colorHex) }
                        ),
                    border = BorderStroke(
                        width = if (selectedColorHex == colorHex) 4.dp else 2.dp,
                        color = color
                    ),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(modifier = Modifier.padding(vertical = 12.dp))
                }
            }

        }
    }
}

@Composable
fun EditableListRow(
    text: String,
    modifier: Modifier = Modifier,
    borderColor: Color = Color.LightGray,
    selected: Boolean = false,
    onSelect: (() -> Unit)? = null,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "rowScale")
    val containerColor by animateColorAsState(
        if (isPressed) Color(0xFFE0E0E0) else Color.White,
        label = "rowColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(3.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onSelect != null) {
                    RadioButton(
                        selected = selected,
                        onClick = onSelect
                    )
                }
                Text(
                    text = text,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    fontSize = 18.sp
                )
                trailingContent?.invoke()
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 1.dp,
            color = Color.LightGray
        )
    }
}

@Composable
fun ChecklistRow(
    checklist: ChecklistApiModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    EditableListRow(
        text = checklist.title,
        modifier = modifier,
        borderColor = Color(checklist.borderColorHex.toColorInt()),
        onClick = onClick,
        onLongClick = onLongClick
    )
}

@Composable
fun ChecklistItemRow(
    item: com.example.checklist.data.ChecklistItemApiModel,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
    onToggleChecked: (() -> Unit)? = null
) {
    EditableListRow(
        text = item.name,
        modifier = modifier,
        selected = item.isChecked,
        onSelect = onToggleChecked,
        onLongClick = onLongClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistDetailScreen(
    checklist: ChecklistApiModel,
    items: List<com.example.checklist.data.ChecklistItemApiModel>,
    onBack: () -> Unit,
    onShowCreateItemDialog: () -> Unit,
    onEditItem: (com.example.checklist.data.ChecklistItemApiModel) -> Unit,
    onToggleItem: (com.example.checklist.data.ChecklistItemApiModel) -> Unit,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    val checklistColor = Color(checklist.borderColorHex.toColorInt())
    var itemSearchQuery by remember { mutableStateOf("") }
    var showItemSearchBar by remember { mutableStateOf(false) }
    val filteredItems = remember(items, itemSearchQuery) {
        if (itemSearchQuery.isBlank()) items else items.filter {
            it.name.contains(itemSearchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(checklist.title) },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            showItemSearchBar = false
                            itemSearchQuery = ""
                            onBack()
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tillbaka")
                    }
                },
                actions = {
                    if (!showItemSearchBar) {
                        IconButton(onClick = { showItemSearchBar = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Sök i checklistan")
                        }
                    }
                    IconButton(onClick = onShowCreateItemDialog) {
                        Icon(Icons.Default.Add, contentDescription = "Skapa ny punkt")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = checklistColor
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showItemSearchBar) {
                val keyboardController = LocalSoftwareKeyboardController.current
                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(showItemSearchBar) {
                    if (showItemSearchBar) {
                        delay(150.milliseconds)
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }
                }

                OutlinedTextField(
                    value = itemSearchQuery,
                    onValueChange = { itemSearchQuery = it },
                    placeholder = { Text("Sök i checklistan") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Sök i checklistan")
                    },
                    trailingIcon = {
                        if (itemSearchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    itemSearchQuery = ""
                                }
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Rensa sökning")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = Color.Red
                )
            }

            HorizontalDivider(Modifier, thickness = 1.dp, color = Color.LightGray)

            if (filteredItems.isEmpty()) {
                Text(
                    text = if (itemSearchQuery.isBlank()) "" else "Inga matchande punkter",
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredItems) { item ->
                        ChecklistItemRow(
                            item = item,
                            onLongClick = { onEditItem(item) },
                            onToggleChecked = { onToggleItem(item) }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChecklistHomeScreenPreview() {
    ChecklistTheme {
        ChecklistHomeScreen()
    }
}

private suspend fun loadChecklistBorderColor(context: Context, checklist: ChecklistApiModel): String {
    val database = ChecklistDatabase.getDatabase(context)
    val entity = database.checklistDao().getByApiId(checklist.id)
    return entity?.borderColorHex ?: checklist.borderColorHex
}

private suspend fun saveChecklistBorderColor(context: Context, checklist: ChecklistApiModel) {
    val database = ChecklistDatabase.getDatabase(context)
    database.checklistDao().updateBorderColor(checklist.id, checklist.borderColorHex)
}

private suspend fun clearChecklistBorderColor(context: Context, checklistId: String) {
    val database = ChecklistDatabase.getDatabase(context)
    database.checklistDao().updateBorderColor(checklistId, "#D3D3D3")
}
