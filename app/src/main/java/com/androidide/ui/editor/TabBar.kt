package com.androidide.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
fun TabBar(
    tabs: List<EditorTab>,
    activeIndex: Int,
    onTabSelected: (Int) -> Unit,
    onTabClosed: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(35.dp)
            .background(Color(0xFF2D2D2D))
            .horizontalScroll(rememberScrollState())
    ) {
        tabs.forEachIndexed { index, tab ->
            TabItem(
                tab = tab,
                isActive = index == activeIndex,
                onClick = { onTabSelected(index) },
                onClose = { onTabClosed(tab.id) }
            )
        }
    }
}

@Composable
private fun TabItem(
    tab: EditorTab,
    isActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    val bg = if (isActive) Color(0xFF1E1E1E) else Color(0xFF2D2D2D)
    val textColor = if (isActive) Color(0xFFFFFFFF) else Color(0xFF969696)

    Row(
        modifier = Modifier
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .widthIn(min = 80.dp, max = 180.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (tab.isModified) {
            Icon(Icons.Default.Circle, null,
                tint = Color(0xFFCCCCCC),
                modifier = Modifier.size(8.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = tab.name,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = textColor,
            maxLines = 1,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onClose, modifier = Modifier.size(16.dp)) {
            Icon(Icons.Default.Close, "Close", tint = Color(0xFF969696), modifier = Modifier.size(12.dp))
        }
    }
    // Active tab bottom indicator
    if (isActive) {
        Box(modifier = Modifier.height(1.dp).width(1.dp).background(Color(0xFF007ACC)))
    }
}
