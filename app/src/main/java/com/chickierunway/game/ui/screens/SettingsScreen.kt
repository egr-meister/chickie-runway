package com.chickierunway.game.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chickierunway.game.Assets
import com.chickierunway.game.R
import com.chickierunway.game.ui.AppLanguage
import com.chickierunway.game.ui.components.FillScrollColumn
import com.chickierunway.game.ui.components.GameBackground
import com.chickierunway.game.ui.components.GameTopBar
import com.chickierunway.game.ui.components.MascotParade
import com.chickierunway.game.ui.components.PanelText
import com.chickierunway.game.ui.components.PanelWithMascot
import com.chickierunway.game.ui.components.PlateButton
import com.chickierunway.game.ui.theme.AccentColor
import com.chickierunway.game.ui.theme.Cream
import com.chickierunway.game.ui.theme.LockedGray
import com.chickierunway.game.ui.theme.NightNavy
import com.chickierunway.game.ui.theme.SunYolk
import com.chickierunway.game.ui.theme.ThemeMode
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
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
    val resetDone = stringResource(R.string.settings_reset_done)
    val noApp = stringResource(R.string.settings_no_app)

    GameBackground(dim = 0.05f) {
        FillScrollColumn {
            GameTopBar(title = stringResource(R.string.settings_title), onBack = onBack)
            PanelWithMascot(
                mascot = Assets.mascotB,
                mascotDescription = null,
                title = stringResource(R.string.settings_panel),
                mascotSize = 110.dp,
                modifier = Modifier.padding(horizontal = 18.dp)
            ) {
                SoundRow(enabled = state.soundEnabled, onChange = viewModel::setSoundEnabled)

                SectionLabel(stringResource(R.string.settings_theme))
                ThemeSelector(selected = state.themeMode, onSelect = viewModel::setThemeMode)

                SectionLabel(stringResource(R.string.settings_accent))
                AccentSelector(selected = state.accent, onSelect = viewModel::setAccent)

                SectionLabel(stringResource(R.string.settings_language))
                PlateButton(
                    text = stringResource(AppLanguage.current().label),
                    onClick = { showLanguageDialog = true },
                    leadingIcon = Assets.item(1),
                    accessibilityLabel = stringResource(R.string.settings_language_choose),
                    modifier = Modifier.fillMaxWidth()
                )

                PlateButton(
                    text = stringResource(R.string.settings_privacy),
                    onClick = { openLink(context, AppLinks.PRIVACY_POLICY_URL, fallback = null, noApp) },
                    leadingIcon = Assets.item(15),
                    modifier = Modifier.fillMaxWidth()
                )
                PlateButton(
                    text = stringResource(R.string.settings_rate),
                    onClick = { openLink(context, AppLinks.MARKET_URI, fallback = AppLinks.PLAY_WEB_URL, noApp) },
                    leadingIcon = Assets.item(0),
                    modifier = Modifier.fillMaxWidth()
                )
                PlateButton(
                    text = stringResource(R.string.settings_reset),
                    onClick = { showResetDialog = true },
                    tint = WrongRed,
                    leadingIcon = Assets.item(3),
                    accessibilityLabel = stringResource(R.string.settings_reset_cd),
                    modifier = Modifier.fillMaxWidth()
                )

                PanelText(
                    stringResource(R.string.settings_storage_note),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            MascotParade(
                mascots = listOf(Assets.mascotD, Assets.mascotA),
                modifier = Modifier.weight(1f).padding(top = 8.dp)
            )
        }
    }

    if (showLanguageDialog) {
        LanguageDialog(
            onPick = { tag ->
                showLanguageDialog = false
                // Recreates the Activity with the new locale; persisted by AppCompat itself.
                AppLanguage.apply(tag)
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = NightNavy,
            titleContentColor = SunYolk,
            textContentColor = Cream,
            title = { Text(stringResource(R.string.settings_reset_title)) },
            text = { Text(stringResource(R.string.settings_reset_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetAll()
                        showResetDialog = false
                        Toast.makeText(context, resetDone, Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF8C8C))
                ) { Text(stringResource(R.string.action_reset), style = MaterialTheme.typography.titleMedium) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = SunYolk)
                ) { Text(stringResource(R.string.action_cancel), style = MaterialTheme.typography.titleMedium) }
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = SunYolk,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
    )
}

@Composable
private fun SoundRow(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = enabled, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(Assets.item(7)),
            contentDescription = null,
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.size(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.settings_sound), color = Cream, style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(if (enabled) R.string.state_on else R.string.state_off),
                color = if (enabled) SunYolk else Cream.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Switch(
            checked = enabled,
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
}

/** Three segments: System / Light / Dark. Each is a radio button for accessibility. */
@Composable
private fun ThemeSelector(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val labels = mapOf(
        ThemeMode.SYSTEM to stringResource(R.string.theme_system),
        ThemeMode.LIGHT to stringResource(R.string.theme_light),
        ThemeMode.DARK to stringResource(R.string.theme_dark)
    )
    Row(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ThemeMode.entries.forEach { mode ->
            val isSelected = mode == selected
            val shape = RoundedCornerShape(50)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(shape)
                    .background(if (isSelected) SunYolk else NightNavy.copy(alpha = 0.6f))
                    .border(BorderStroke(2.dp, SunYolk), shape)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(mode) })
                    .padding(horizontal = 6.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = labels.getValue(mode),
                    color = if (isSelected) NightNavy else Cream,
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

/** Colour swatches with their names; the chosen one gets a ring and a check. */
@Composable
private fun AccentSelector(selected: AccentColor, onSelect: (AccentColor) -> Unit) {
    val names = mapOf(
        AccentColor.GOLD to stringResource(R.string.accent_gold),
        AccentColor.SKY to stringResource(R.string.accent_sky),
        AccentColor.MEADOW to stringResource(R.string.accent_meadow),
        AccentColor.BERRY to stringResource(R.string.accent_berry)
    )
    Row(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        AccentColor.entries.forEach { accent ->
            val isSelected = accent == selected
            val name = names.getValue(accent)
            val description = if (isSelected) stringResource(R.string.state_selected_cd, name) else name
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(accent) })
                    .clearAndSetSemantics { contentDescription = description }
                    .padding(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(accent.main)
                        .border(BorderStroke(if (isSelected) 3.dp else 1.dp, if (isSelected) Cream else NightNavy), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) Text("✓", color = NightNavy, style = MaterialTheme.typography.titleMedium)
                }
                Text(name, color = Cream, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        }
    }
}

/** First "System default", then the seven languages, each named in its own language. */
@Composable
private fun LanguageDialog(onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    val currentTag = AppLanguage.currentTag()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NightNavy,
        titleContentColor = SunYolk,
        textContentColor = Cream,
        title = { Text(stringResource(R.string.settings_language_choose)) },
        text = {
            Column(modifier = Modifier.selectableGroup().verticalScroll(rememberScrollState())) {
                AppLanguage.options.forEach { option ->
                    val isSelected = option.tag == currentTag
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onPick(option.tag) })
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = SunYolk, unselectedColor = Cream)
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(stringResource(option.label), color = Cream, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = SunYolk)
            ) { Text(stringResource(R.string.action_cancel), style = MaterialTheme.typography.titleMedium) }
        }
    )
}

/** Opens a link in an external app; never crashes if nothing can handle it. */
private fun openLink(context: Context, uri: String, fallback: String?, noAppMessage: String) {
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
    Toast.makeText(context, noAppMessage, Toast.LENGTH_SHORT).show()
}
