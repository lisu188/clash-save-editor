package com.lis.clash.desktop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
internal fun StartupScreen(state: EditorState, actions: EditorActions, dark: Boolean, toggleTheme: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        WelcomeHeader(dark, toggleTheme)
        Box(Modifier.fillMaxWidth().height(2.dp)) {
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxSize())
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val compact = maxWidth < 850.dp
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = if (compact) 24.dp else 40.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Column(Modifier.widthIn(max = 1120.dp).fillMaxWidth()) {
                    if (compact) {
                        WelcomeIntroduction(Modifier.fillMaxWidth(), compact = true)
                        Spacer(Modifier.height(24.dp))
                        WelcomeActions(state, actions, Modifier.fillMaxWidth())
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(44.dp), verticalAlignment = Alignment.CenterVertically) {
                            WelcomeIntroduction(Modifier.weight(1.02f), compact = false)
                            WelcomeActions(state, actions, Modifier.weight(1f))
                        }
                    }
                    Spacer(Modifier.height(30.dp))
                    RecentDocuments(state, actions)
                    if (state.busy) {
                        Text(state.notice, Modifier.padding(top = 16.dp),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(5.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp)))
            Text("Local workspace", Modifier.padding(start = 7.dp), style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text("Clash saves & scenarios", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WelcomeHeader(dark: Boolean, toggleTheme: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(8.dp)) {
            Icon(Icons.Outlined.Map, null, Modifier.padding(9.dp).size(23.dp), tint = MaterialTheme.colorScheme.onPrimary)
        }
        Column(Modifier.padding(start = 12.dp)) {
            Text("UNOFFICIAL CLASH SAVE EDITOR", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("Scenario & save editor", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.weight(1f))
        IconButton(toggleTheme) {
            Icon(if (dark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                if (dark) "Use light theme" else "Use dark theme", Modifier.size(19.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WelcomeIntroduction(modifier: Modifier, compact: Boolean) {
    Column(modifier) {
        Text("YOUR NEXT WORLD STARTS HERE", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text("Unofficial Clash Save Editor", style = MaterialTheme.typography.displaySmall)
        Text("Shape a new scenario or return to a world in progress.", Modifier.padding(top = 10.dp),
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!compact) {
            Spacer(Modifier.height(24.dp))
            CartographyIllustration(Modifier.fillMaxWidth().height(172.dp))
        }
    }
}

@Composable
private fun WelcomeActions(state: EditorState, actions: EditorActions, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StartupChoice(
            title = "Create a new scenario", description = "Begin with an empty map. Add players, terrain and starting forces.",
            button = "Create scenario from scratch", shortcut = "Ctrl + N", icon = Icons.Outlined.Add,
            primary = true, enabled = !state.busy, onClick = actions.newDocument
        )
        StartupChoice(
            title = "Open an existing world", description = "Continue from a Clash save or an editor project.",
            button = "Open existing save", shortcut = "Ctrl + O", icon = Icons.Outlined.FolderOpen,
            primary = false, enabled = !state.busy, onClick = actions.open
        )
    }
}

@Composable
private fun StartupChoice(
    title: String, description: String, button: String, shortcut: String,
    icon: ImageVector, primary: Boolean, enabled: Boolean, onClick: () -> Unit
) {
    OutlinedCard(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (primary) MaterialTheme.colorScheme.primary.copy(alpha = .42f) else MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(21.dp), tint = MaterialTheme.colorScheme.primary)
                Text(title, Modifier.weight(1f).padding(start = 10.dp), style = MaterialTheme.typography.titleMedium)
                Text(shortcut, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(description, Modifier.padding(top = 9.dp, bottom = 16.dp), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (primary) Button(onClick, enabled = enabled, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(6.dp), contentPadding = PaddingValues(14.dp, 11.dp)) {
                Text(button, Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(17.dp))
            } else OutlinedButton(onClick, enabled = enabled, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(6.dp), contentPadding = PaddingValues(14.dp, 11.dp)) {
                Text(button, Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(17.dp))
            }
        }
    }
}

@Composable
private fun RecentDocuments(state: EditorState, actions: EditorActions) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Recent files", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.weight(1f))
        Text("On this computer", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (state.recent.isEmpty()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.History, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.outline)
            Column(Modifier.padding(start = 12.dp)) {
                Text("A fresh start", style = MaterialTheme.typography.bodyMedium)
                Text("Files you open will appear here for next time.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            state.recent.take(4).forEach { path -> RecentDocument(path, !state.busy) { actions.openRecent(path) } }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentDocument(path: String, enabled: Boolean, onClick: () -> Unit) {
    val fileName = path.substringAfterLast('\\').substringAfterLast('/')
    val project = fileName.endsWith(".clashproj", ignoreCase = true)
    TooltipArea(tooltip = {
        Surface(color = MaterialTheme.colorScheme.inverseSurface, shape = RoundedCornerShape(6.dp), shadowElevation = 4.dp) {
            Text(path, Modifier.padding(10.dp, 7.dp).widthIn(max = 680.dp), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.inverseOnSurface)
        }
    }, modifier = Modifier.fillMaxWidth()) {
        Surface(onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().semantics {
            contentDescription = "Open recent ${if (project) "project" else "save"} $fileName, $path"
        }, color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(6.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (project) Icons.Outlined.Description else Icons.Outlined.FolderOpen, null,
                    Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Text(fileName, Modifier.widthIn(max = 230.dp).padding(start = 12.dp, end = 16.dp),
                    style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(path, Modifier.weight(1f).padding(end = 16.dp), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (project) "PROJECT" else "CLASH SAVE", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.padding(start = 16.dp).size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Decorative vector cartography; independent of game artwork and the current document. */
@Composable
private fun CartographyIllustration(modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF253F37))) {
        Canvas(Modifier.fillMaxSize().semantics { contentDescription = "Illustrated landscape with a river, forests and a castle" }) {
            val w = size.width
            val h = size.height
            val grid = Color(0xFF5C7C68).copy(alpha = .20f)
            for (x in 0..20) drawLine(grid, Offset(x * w / 20, 0f), Offset(x * w / 20, h), 1f)
            for (y in 0..10) drawLine(grid, Offset(0f, y * h / 10), Offset(w, y * h / 10), 1f)
            for (band in 0..5) {
                val contour = Path()
                for (i in 0..60) {
                    val x = i * w / 60
                    val y = h * (.10f + band * .17f) + sin(i * .13f + band * .9f) * h * .13f
                    if (i == 0) contour.moveTo(x, y) else contour.lineTo(x, y)
                }
                drawPath(contour, Color(0xFF95AF79).copy(alpha = .16f), style = Stroke(1.2f))
            }
            val river = Path().apply {
                moveTo(w * .34f, -h * .08f)
                cubicTo(w * .20f, h * .30f, w * .60f, h * .34f, w * .48f, h * .59f)
                cubicTo(w * .37f, h * .89f, w * .58f, h * .81f, w * .64f, h * 1.1f)
            }
            drawPath(river, Color(0xFF547D78), style = Stroke(h * .10f, cap = StrokeCap.Round))
            drawPath(river, Color(0xFF7BACA2).copy(alpha = .35f), style = Stroke(1.3f))
            val route = Path().apply {
                moveTo(w * .07f, h * .73f)
                cubicTo(w * .27f, h * .55f, w * .34f, h * .80f, w * .49f, h * .61f)
                cubicTo(w * .66f, h * .48f, w * .72f, h * .49f, w * .89f, h * .24f)
            }
            drawPath(route, Color(0xFFBEA476).copy(alpha = .75f), style = Stroke(2.5f, cap = StrokeCap.Round))
            for (i in 0 until 35) {
                val x = w * (.12f + ((i * 17) % 71) / 100f)
                val y = h * (.16f + ((i * 23) % 61) / 100f)
                if ((x < w * .31f || x > w * .60f) && (x < w * .71f || y > h * .44f)) {
                    val tree = Path().apply {
                        moveTo(x, y - 6f); lineTo(x - 4.5f, y + 3f); lineTo(x + 4.5f, y + 3f); close()
                    }
                    drawPath(tree, if (i % 3 == 0) Color(0xFF8DA879) else Color(0xFF668B6A))
                }
            }
            val castle = Offset(w * .73f, h * .42f)
            drawCircle(Color(0xFFDAC295).copy(alpha = .16f), 20f, castle)
            drawRect(Color(0xFFDAC295), castle - Offset(8f, 3f), Size(16f, 10f))
            drawRect(Color(0xFFDAC295), castle - Offset(10f, 9f), Size(5f, 17f))
            drawRect(Color(0xFFDAC295), castle + Offset(5f, -9f), Size(5f, 17f))
            drawRect(Color(0xFF253F37), castle + Offset(-2f, 1f), Size(4f, 7f))
            val compass = Offset(w - 25f, 25f)
            drawLine(Color(0xFFA7B99E), compass - Offset(0f, 9f), compass + Offset(0f, 9f), 1f)
            drawLine(Color(0xFFA7B99E), compass - Offset(6f, 0f), compass + Offset(6f, 0f), 1f)
            drawCircle(Color(0xFFA7B99E), 12f, compass, style = Stroke(.8f))
        }
        Text("A WORLD OF YOUR OWN", Modifier.align(Alignment.BottomStart).padding(14.dp, 10.dp),
            style = MaterialTheme.typography.labelSmall, color = Color(0xFFE3EAD8))
    }
}
