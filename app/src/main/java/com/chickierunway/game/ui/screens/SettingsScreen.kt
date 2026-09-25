package com.chickierunway.game.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import com.chickierunway.game.Assets
import com.chickierunway.game.ui.components.FillScrollColumn
import com.chickierunway.game.ui.components.MascotParade
import com.chickierunway.game.ui.components.PanelWithMascot
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chickierunway.game.ui.components.GameBackground
import com.chickierunway.game.ui.components.GameTopBar
import com.chickierunway.game.ui.components.PanelText
import com.chickierunway.game.ui.components.PlateButton
import com.chickierunway.game.ui.theme.Cream
import com.chickierunway.game.ui.theme.LockedGray
import com.chickierunway.game.ui.theme.NightNavy
import com.chickierunway.game.ui.theme.SunYolk
import com.chickierunway.game.ui.theme.WrongRed
import com.chickierunway.game.ui.viewmodel.ProgressViewModel

/** External links. Replace PRIVACY_POLICY_URL with your published policy before release. */
object AppLinks {
    const val PRIVACY_POLICY_URL = "https://example.com/chickie-runway/privacy"
    const val PACKAGE_NAME = "com.chickierunway.game"
    const val MARKET_URI = "market://details?id=$PACKAGE_NAME"
    const val PLAY_WEB_URL = "https://play.google.com/store/apps/details?id=$PACKAGE_NAME"
}

@Composable
fun SettingsScreen(viewModel: ProgressViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showResetDialog by rememberSaveable { mutableStateOf(false) }

    GameBackground(dim = 0.05f) {
        FillScrollColumn {
            GameTopBar(title = "Settings", onBack = onBack)
            PanelWithMascot(
                mascot = Assets.mascotB,
                mascotDescription = null,
                title = "Control Panel",
                mascotSize = 110.dp,
                modifier = Modifier.padding(horizontal = 18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .toggleable(
                            value = state.soundEnabled,
                            role = Role.Switch,
                            onValueChange = { viewModel.setSoundEnabled(it) }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(Assets.item(7)),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(Modifier.size(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sound effects", color = Cream, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (state.soundEnabled) "On" else "Off",
                            color = if (state.soundEnabled) SunYolk else Cream.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Switch(
                        checked = state.soundEnabled,
                        onCheckedChange = null, // handled by the whole row (bigger touch target)
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NightNavy,
                            checkedTrackColor = SunYolk,
                            uncheckedThumbColor = Cream,
                            uncheckedTrackColor = LockedGray,
                            uncheckedBorderColor = Cream
                        )
                    )
                }

                PlateButton(
                    text = "Privacy Policy",
                    onClick = { openLink(context, AppLinks.PRIVACY_POLICY_URL, fallback = null) },
                    leadingIcon = Assets.item(15),
                    modifier = Modifier.fillMaxWidth()
                )
                PlateButton(
                    text = "Rate the Game",
                    onClick = { openLink(context, AppLinks.MARKET_URI, fallback = AppLinks.PLAY_WEB_URL) },
                    leadingIcon = Assets.item(0),
                    modifier = Modifier.fillMaxWidth()
                )
                PlateButton(
                    text = "Reset Progress",
                    onClick = { showResetDialog = true },
                    tint = WrongRed,
                    leadingIcon = Assets.item(3),
                    accessibilityLabel = "Reset progress. Asks for confirmation.",
                    modifier = Modifier.fillMaxWidth()
                )

                PanelText(
                    "Chickie Runway works fully offline. Scores and settings are stored only on this device and are never sent anywhere.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            MascotParade(
                mascots = listOf(Assets.mascotD, Assets.mascotA),
                modifier = Modifier.weight(1f).padding(top = 8.dp)
            )
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = NightNavy,
            titleContentColor = SunYolk,
            textContentColor = Cream,
            title = { Text("Reset all progress?") },
            text = {
                Text("This clears every quiz best score and locks Matching Pairs back to level 1. This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetAll()
                        showResetDialog = false
                        Toast.makeText(context, "Progress reset", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF8C8C))
                ) { Text("Reset", style = MaterialTheme.typography.titleMedium) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = SunYolk)
                ) { Text("Cancel", style = MaterialTheme.typography.titleMedium) }
            }
        )
    }
}

/** Opens a link in an external app; never crashes if nothing can handle it. */
private fun openLink(context: Context, uri: String, fallback: String?) {
    fun tryOpen(target: String): Boolean = try {
        val intent = Intent(Intent.ACTION_VIEW, target.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }

    if (tryOpen(uri)) return
    if (fallback != null && tryOpen(fallback)) return
    Toast.makeText(context, "No app available to open this link", Toast.LENGTH_SHORT).show()
}
