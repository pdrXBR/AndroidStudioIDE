package com.androidide.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun CommandPalette(
    actions: List<Pair<String, () -> Unit>>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val filtered = actions.filter { it.first.contains(query, ignoreCase = true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .shadow(8.dp)
                .background(Color(0xFF3C3C3C), MaterialTheme.shapes.medium)
        ) {
            // Search input
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, null, tint = Color(0xFF858585),
                    modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("> Type a command...", fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color(0xFFD4D4D4),
                        unfocusedTextColor = Color(0xFFD4D4D4),
                        cursorColor = Color(0xFFAEAFAD),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                )
            }
            HorizontalDivider(color = Color(0xFF555555))

            // Results
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                itemsIndexed(filtered) { index, (label, action) ->
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        color = Color(0xFFD4D4D4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (index == 0) Color(0xFF04395E) else Color.Transparent)
                            .clickable { action(); onDismiss() }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}
