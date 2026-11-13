package io.github.koppor.koppter.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.Dialog

/**
 * A dialog that asks before it closes with changes that are not saved: on Esc, on a click beside it and on the back button.
 * The buttons of the dialog close it themselves, as they say what they do.
 */
// [impl->dsn~unsaved-changes~1]
@Composable
fun GuardedDialog(changed: Boolean, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    var asking by remember { mutableStateOf(false) }
    val dismiss = { if (changed) asking = true else onDismiss() }

    Dialog(onDismissRequest = dismiss) {
        Box(
            Modifier.onPreviewKeyEvent {
                // On the release: the question that the key opens would get the release of the key otherwise and close again.
                if (it.key == Key.Escape && it.type == KeyEventType.KeyUp) dismiss()
                it.key == Key.Escape
            },
        ) {
            content()
        }
    }
    if (asking) {
        AlertDialog(
            onDismissRequest = { asking = false },
            title = { Text("Discard the changes?") },
            text = { Text("The changes in this dialog are not saved.") },
            confirmButton = { TextButton(onClick = onDismiss, modifier = Modifier.testTag("discard-confirm")) { Text("Discard") } },
            dismissButton = { TextButton(onClick = { asking = false }, modifier = Modifier.testTag("discard-keep")) { Text("Keep editing") } },
            modifier = Modifier.testTag("discard-dialog"),
        )
    }
}
