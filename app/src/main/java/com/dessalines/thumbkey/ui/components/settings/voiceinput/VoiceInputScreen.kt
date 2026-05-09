package com.dessalines.thumbkey.ui.components.settings.voiceinput

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.dessalines.thumbkey.R
import com.dessalines.thumbkey.db.AppSettingsViewModel
import com.dessalines.thumbkey.db.DEFAULT_KEY_MODIFICATIONS
import com.dessalines.thumbkey.db.KeyModificationsUpdate
import com.dessalines.thumbkey.utils.SimpleTopAppBar
import com.dessalines.thumbkey.utils.getPreferredVoiceImeId
import com.dessalines.thumbkey.utils.setPreferredVoiceImeId
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.ListPreferenceType
import me.zhanghai.compose.preference.Preference
import me.zhanghai.compose.preference.ProvidePreferenceTheme

private fun queryPickableImes(ctx: Context): List<InputMethodInfo> {
    val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    val ownPackage = ctx.packageName
    return imm.enabledInputMethodList
        .filter { it.packageName != ownPackage }
        .sortedBy { it.loadLabel(ctx.packageManager).toString().lowercase() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceInputScreen(
    navController: NavController,
    appSettingsViewModel: AppSettingsViewModel,
) {
    val ctx = LocalContext.current
    val settings by appSettingsViewModel.appSettings.observeAsState()
    val keyModifications = settings?.keyModifications ?: DEFAULT_KEY_MODIFICATIONS
    val currentImeId = getPreferredVoiceImeId(keyModifications) ?: ""

    var imes by remember { mutableStateOf(queryPickableImes(ctx)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    imes = queryPickableImes(ctx)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val pm = ctx.packageManager
    val noneId = ""
    val values = listOf(noneId) + imes.map { it.id }
    val labels =
        mapOf(noneId to stringResource(R.string.voice_input_none_label)) +
            imes.associate { it.id to it.loadLabel(pm).toString() }

    val openImeSettings = {
        ctx.startActivity(
            Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            SimpleTopAppBar(
                text = stringResource(R.string.voice_input),
                navController = navController,
            )
        },
        content = { padding ->
            Column(
                modifier =
                    Modifier
                        .padding(padding)
                        .verticalScroll(scrollState)
                        .background(color = MaterialTheme.colorScheme.surface)
                        .imePadding(),
            ) {
                ProvidePreferenceTheme {
                    if (imes.isEmpty()) {
                        Preference(
                            title = { Text(stringResource(R.string.preferred_voice_ime)) },
                            summary = { Text(stringResource(R.string.voice_input_no_voice_imes)) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Mic,
                                    contentDescription = null,
                                )
                            },
                            onClick = openImeSettings,
                        )
                    } else {
                        ListPreference(
                            type = ListPreferenceType.ALERT_DIALOG,
                            value = currentImeId,
                            values = values,
                            onValueChange = { newId ->
                                val updated = setPreferredVoiceImeId(keyModifications, newId)
                                appSettingsViewModel.updateKeyModifications(
                                    KeyModificationsUpdate(
                                        id = 1,
                                        keyModifications = updated,
                                    ),
                                )
                            },
                            title = { Text(stringResource(R.string.preferred_voice_ime)) },
                            summary = {
                                Text(labels[currentImeId] ?: stringResource(R.string.voice_input_none_label))
                            },
                            valueToText = { id -> AnnotatedString(labels[id] ?: id) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Outlined.Mic,
                                    contentDescription = null,
                                )
                            },
                        )
                        Preference(
                            title = { Text(stringResource(R.string.voice_input_open_system_settings)) },
                            onClick = openImeSettings,
                        )
                    }
                }
            }
        },
    )
}
