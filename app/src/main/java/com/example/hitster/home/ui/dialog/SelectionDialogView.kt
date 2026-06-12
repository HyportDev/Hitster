package com.example.hitster.home.ui.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hitster.home.model.PlaylistSelectionItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionDialogView(
    title: String,
    items: List<PlaylistSelectionItem>,
    onCheckboxChange: (PlaylistSelectionItem, Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier.wrapContentWidth().wrapContentHeight().heightIn(50.dp, 500.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(modifier = Modifier.padding(
                top = 16.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = 8.dp
            )) {
                Text(text = title, fontWeight = FontWeight.SemiBold)

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items) { item ->
                        SelectionDialogItemView(item, onCheckboxChange)
                    }
                }

                DialogButtonRow(onDismiss = onDismiss, onConfirm = onConfirm)
            }
        }
    }
}

@Composable
private fun SelectionDialogItemView(
    item: PlaylistSelectionItem,
    onCheckboxChange: (PlaylistSelectionItem, Boolean) -> Unit
)  {
    item.playlist.name?.let { playlistName ->
        Row(
            modifier = Modifier.fillMaxWidth()
                .clickable(role = Role.Checkbox) { onCheckboxChange(item, !item.selected) },
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.selected,
                onCheckedChange = { onCheckboxChange(item, it) }
            )
            Text(text = playlistName)
        }
    }
}

