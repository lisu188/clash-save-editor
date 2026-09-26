package com.lis.clash.desktop

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

internal val NavigationInk = Color(0xFF172B30)
internal val NavigationMuted = Color(0xFF9AB4B5)

internal fun WorkspacePage.icon(): ImageVector = when (this) {
    WorkspacePage.MAP -> Icons.Outlined.Map
    WorkspacePage.ARMIES -> Icons.Outlined.Flag
    WorkspacePage.BUILDINGS -> Icons.Outlined.Castle
    WorkspacePage.PLAYERS -> Icons.Outlined.People
    WorkspacePage.SCENARIO -> Icons.Outlined.Tune
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun StudioIconButton(label: String, icon: ImageVector, enabled: Boolean = true,
                              tint: Color = MaterialTheme.colorScheme.onSurfaceVariant, onClick: () -> Unit) {
    TooltipArea(tooltip = {
        Surface(shadowElevation = 4.dp, shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.inverseSurface) {
            Text(label, Modifier.padding(10.dp, 7.dp), color = MaterialTheme.colorScheme.inverseOnSurface,
                style = MaterialTheme.typography.labelMedium)
        }
    }) {
        IconButton(onClick, enabled = enabled, modifier = Modifier.size(36.dp).semantics { contentDescription = label }) {
            Icon(icon, null, Modifier.size(19.dp), tint = if (enabled) tint else tint.copy(alpha = .35f))
        }
    }
}

@Composable
internal fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun QuietBadge(text: String, color: Color = MaterialTheme.colorScheme.primary) {
    Surface(color = color.copy(alpha = .10f), shape = RoundedCornerShape(5.dp)) {
        Text(text, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = color,
            style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
internal fun PageHeading(title: String, subtitle: String, action: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, Modifier.padding(top = 5.dp), color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall)
        }
        action()
    }
}

@Composable
internal fun EmptyState(title: String, text: String, icon: ImageVector = Icons.Outlined.Explore,
                        action: (@Composable () -> Unit)? = null) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 360.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Icon(icon, null, Modifier.padding(18.dp).size(30.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            action?.invoke()
        }
    }
}

@Composable
internal fun PlayerDot(slot: Int, modifier: Modifier = Modifier) {
    Box(modifier.size(9.dp).background(playerColors.getOrElse(slot) { Color.Gray }, RoundedCornerShape(3.dp)))
}

internal fun playerName(state: EditorState, slot: Int): String =
    state.snapshot.players.firstOrNull { it.id.slot == slot }?.name?.ifBlank { null } ?: "Player $slot"
