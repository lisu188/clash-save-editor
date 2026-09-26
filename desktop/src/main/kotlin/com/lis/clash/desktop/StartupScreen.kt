package com.lis.clash.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun StartupScreen(state: EditorState, actions: EditorActions, dark: Boolean, toggleTheme: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(28.dp, 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp)) {
                Text("C", Modifier.padding(16.dp, 10.dp), color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("CLASH STUDIO", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("SAVE & SCENARIO EDITOR", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.weight(1f))
            TextButton(toggleTheme, modifier = Modifier.semantics {
                contentDescription = if (dark) "Use light theme" else "Use dark theme"
            }) { Text(if (dark) "☀" else "☾") }
        }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(36.dp),
            contentAlignment = Alignment.Center) {
            Column(Modifier.widthIn(max = 920.dp).fillMaxWidth()) {
                Text("Welcome to Clash Studio", style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Text("Would you like to create a scenario from scratch or open an existing save?",
                    style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(32.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    StartupChoice(
                        title = "Start a new world",
                        description = "Start with an empty grass map, set up players, and build your world.",
                        button = "Create scenario from scratch",
                        icon = Icons.Outlined.Add,
                        enabled = !state.busy,
                        onClick = actions.newDocument,
                        modifier = Modifier.weight(1f)
                    )
                    StartupChoice(
                        title = "Continue your work",
                        description = "Continue from a Clash save or a saved editor project.",
                        button = "Open existing save",
                        icon = Icons.Outlined.FolderOpen,
                        enabled = !state.busy,
                        onClick = actions.open,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (state.busy) {
                    Spacer(Modifier.height(20.dp))
                    Text(state.notice, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Text("LOCAL FILES · OFFLINE", Modifier.align(Alignment.CenterHorizontally).padding(24.dp),
            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StartupChoice(
    title: String,
    description: String,
    button: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    OutlinedCard(modifier, shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().height(290.dp).padding(28.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(20.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Button(onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text(button) }
        }
    }
}
