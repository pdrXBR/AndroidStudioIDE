package com.androidide.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androidide.data.model.EditorTab

@Composable
fun StatusBar(
    activeTab: EditorTab?,
    isTermuxAvailable: Boolean,
    diagnosticCount: Int,
    onToggleFileTree: () -> Unit,
    onToggleTerminal: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp)
            .background(Color(0xFF007ACC)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side
        Row(modifier = Modifier.clickable(onClick = onToggleFileTree).padding(horizontal = 8.dp)) {
            Icon(Icons.Default.AccountTree, null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("Explorer", fontSize = 11.sp, color = Color.White, fontFamily = FontFamily.Default)
        }

        Spacer(Modifier.weight(1f))

        // Center: diagnostics
        if (diagnosticCount > 0) {
            Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                Icon(Icons.Default.Error, null, tint = Color(0xFFF48771), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("$diagnosticCount", fontSize = 11.sp, color = Color.White)
            }
        }

        // Right side
        if (activeTab != null) {
            Text(activeTab.language, fontSize = 11.sp, color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp))
        }

        Row(modifier = Modifier.clickable(onClick = onToggleTerminal).padding(horizontal = 8.dp)) {
            Icon(Icons.Default.Terminal, null, tint = Color.White, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("Terminal", fontSize = 11.sp, color = Color.White)
        }

        if (!isTermuxAvailable) {
            Text("⚠ Termux not installed", fontSize = 11.sp, color = Color(0xFFF48771),
                modifier = Modifier.padding(horizontal = 8.dp))
        }
    }
}
