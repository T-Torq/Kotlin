package com.example.checklist

import androidx.activity.ComponentActivity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import com.example.checklist.data.ChecklistItemApiModel

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.checklist", appContext.packageName)
    }

    @Test
    fun itemEditFlow_opensDialogAndUpdatesName() {
        composeRule.setContent {
            var items by remember {
                mutableStateOf(
                    listOf(ChecklistItemApiModel(id = "0123456789abcdef01234567", checklistId = "89abcdef0123456701234567", name = "Mjolk"))
                )
            }
            var editedItemId by remember {
                mutableStateOf<String?>(null)
            }
            var dialogItem by remember {
                mutableStateOf<ChecklistItemApiModel?>(null)
            }
            var editText by remember {
                mutableStateOf("")
            }

            ChecklistDetailScreen(
                checklist = com.example.checklist.data.ChecklistApiModel(id = "89abcdef0123456701234567", title = "Helg"),
                items = items,
                onBack = {},
                onShowCreateItemDialog = {},
                onDeleteItem = {},
                onDeleteChecklist = {},
                onEditItem = {
                    editedItemId = it.id
                    dialogItem = it
                    editText = it.name
                },
                errorMessage = null
            )

            if (dialogItem != null) {
                AlertDialog(
                    onDismissRequest = {
                        dialogItem = null
                        editText = ""
                    },
                    title = { Text("Ändra namn på punkt") },
                    text = {
                        OutlinedTextField(
                            value = editText,
                            onValueChange = { editText = it },
                            modifier = Modifier.testTag("edit-name-field")
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val item = dialogItem
                                if (item != null) {
                                    items = items.map {
                                        if (it.id == item.id) it.copy(name = editText) else it
                                    }
                                }
                                dialogItem = null
                            }
                        ) {
                            Text("Spara")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { dialogItem = null }) {
                            Text("Avbryt")
                        }
                    }
                )
            }
        }

        composeRule.onNodeWithText("Mjolk").performTouchInput { longClick() }
        composeRule.onNodeWithText("Ändra namn på punkt").assertIsDisplayed()
        composeRule.onNodeWithTag("edit-name-field").performTextClearance()
        composeRule.onNodeWithTag("edit-name-field").performTextInput("Mjölk")
        composeRule.onNodeWithText("Spara").performClick()
        composeRule.onNodeWithText("Mjölk").assertIsDisplayed()
    }
}