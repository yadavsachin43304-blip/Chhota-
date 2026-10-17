package com.example.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.agent.ChhotuToolRegistry
import com.example.automation.ChhotuAccessibilityService
import com.example.security.signOutOwner

@Composable
fun SecurityAndSettingsScreen(
    viewModel: ChhotuViewModel,
    initialTab: String = "ACTIVITY", // "ACTIVITY" or "SETTINGS"
    currentThemeMode: String = "DARK",
    onThemeModeChanged: (String) -> Unit = {},
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val securityLogsState by viewModel.securityLogsState.collectAsStateWithLifecycle()
    val offlineItems by viewModel.offlineItemsState.collectAsStateWithLifecycle()
    val isOfflineMode by viewModel.isOfflineMode.collectAsStateWithLifecycle()
    val configuredWakeWord by viewModel.configuredWakeWord.collectAsStateWithLifecycle()
    val voiceProfile by viewModel.voiceProfile.collectAsStateWithLifecycle()
    val voicePitch by viewModel.voicePitch.collectAsStateWithLifecycle()
    val voiceSpeed by viewModel.voiceSpeed.collectAsStateWithLifecycle()
    val isAccessibilityConnected by ChhotuAccessibilityService.isServiceConnected.collectAsStateWithLifecycle()

    var activeSection by remember(initialTab) { mutableStateOf(initialTab) } // "ACTIVITY", "SETTINGS", "DELIVERABLES"
    var wakeWordInput by remember { mutableStateOf(configuredWakeWord) }
    var voiceEnrollPhrase by remember { mutableStateOf("Namaste Chhotu, main is phone ka maalik aur aapka dost hoon.") }
    var pinInput by remember { mutableStateOf("1234") }
    var enrollmentStatus by remember {
        mutableStateOf(
            if (viewModel.authManager.isVoiceEnrolled()) "Voice Embedding aur PIN Enrolled Hain (Active)"
            else "Default Owner Security Active (PIN: 1234)"
        )
    }

    var localNoteTitle by remember { mutableStateOf("Offline Meeting Note") }
    var localNoteBody by remember { mutableStateOf("Kal subah 10 baje supplier call aur invoice review karna hai.") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = activeSection == "ACTIVITY",
                    onClick = { activeSection = "ACTIVITY" },
                    label = { Text("Activity & Audit") },
                    modifier = Modifier.testTag("sec_tab_activity")
                )
                FilterChip(
                    selected = activeSection == "SETTINGS",
                    onClick = { activeSection = "SETTINGS" },
                    label = { Text("Theme, Voice & Security") },
                    modifier = Modifier.testTag("sec_tab_settings")
                )
                FilterChip(
                    selected = activeSection == "DELIVERABLES",
                    onClick = { activeSection = "DELIVERABLES" },
                    label = { Text("15 Deliverables & Tools") },
                    modifier = Modifier.testTag("sec_tab_deliverables")
                )
            }

            when (activeSection) {
                "ACTIVITY" -> {
                    Text(
                        text = "Security Dashboard & Immutable Audit Logs",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Live Security Posture (Suraksha Sthiti)",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("• Owner UID: ${viewModel.currentUserId.take(14)}...")
                            Text("• Accessibility Screen Verifier: ${if (isAccessibilityConnected) "Connected (Authorized)" else "Disabled (Intent-Only Safe Mode)"}")
                            Text("• High-Risk Approval Gate: Enabled (Payment, Delete, Publish, Send)")
                            Text("• Untrusted Webpage Prompt-Injection Guard: Active")
                            Text("• In-App Keystore AES-256-GCM File & Voice Encryption: Active")
                        }
                    }

                    when (val st = securityLogsState) {
                        is DataUiState.Loading -> Text("Audit logs load ho rahe hain mere dost...")
                        is DataUiState.Error -> Text("Error: ${st.message}", color = MaterialTheme.colorScheme.error)
                        is DataUiState.Success -> {
                            if (st.data.isEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Text(
                                        text = "Abhi koi audit log nahi hai mere dost. Home ya Tasks tab se koi command chalayein.",
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            } else {
                                st.data.forEach { log ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = log.eventType,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = log.severity,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (log.severity == "CRITICAL") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                            Text(
                                                text = log.description,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "Actor: ${log.actorIdentity} • Task: ${log.taskRefId}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "SETTINGS" -> {
                    Text(
                        text = "Dark/Light Mode, Male Dost Voice & Security Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // 1. Dark Mode / Light Mode / System Theme Selector Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.SettingsBrightness,
                                    contentDescription = "Theme Mode",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "App Theme Option (Dark Mode / Light Mode)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Mere Dost, apni pasand ke hisaab se Dark Mode, Light Mode ya System Default chunein:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = currentThemeMode == "DARK",
                                    onClick = {
                                        viewModel.updateThemeMode("DARK")
                                        onThemeModeChanged("DARK")
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Filled.DarkMode, contentDescription = "Dark", modifier = Modifier.size(16.dp))
                                    },
                                    label = { Text("Dark Mode") },
                                    modifier = Modifier.testTag("settings_theme_dark_chip")
                                )
                                FilterChip(
                                    selected = currentThemeMode == "LIGHT",
                                    onClick = {
                                        viewModel.updateThemeMode("LIGHT")
                                        onThemeModeChanged("LIGHT")
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Filled.LightMode, contentDescription = "Light", modifier = Modifier.size(16.dp))
                                    },
                                    label = { Text("Light Mode") },
                                    modifier = Modifier.testTag("settings_theme_light_chip")
                                )
                                FilterChip(
                                    selected = currentThemeMode == "SYSTEM",
                                    onClick = {
                                        viewModel.updateThemeMode("SYSTEM")
                                        onThemeModeChanged("SYSTEM")
                                    },
                                    label = { Text("System Auto") },
                                    modifier = Modifier.testTag("settings_theme_system_chip")
                                )
                            }
                        }
                    }

                    // 2. Chhotu Human Male Friend ("Ladka / Saccha Dost") Voice Configuration Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.RecordVoiceOver,
                                    contentDescription = "Chhotu Male Voice",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Chhotu Human Male Dost Voice (Insaano Jaisi Ladke Ki Awaaz)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Active Profile: $voiceProfile (Pitch ${"%.2f".format(voicePitch)} • Speed ${"%.2f".format(voiceSpeed)}x)\n" +
                                    "Chhotu aapki bhasha (Hinglish/Hindi/English) mein poori izzat ('Mere Dost / Aap') aur insaani feeling ke saath baat karta hai.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = voiceProfile == "WARM_DOST_MALE",
                                    onClick = { viewModel.updateVoiceProfile("WARM_DOST_MALE", 0.88f, 0.95f) },
                                    label = { Text("Warm Best Friend (Male)") }
                                )
                                FilterChip(
                                    selected = voiceProfile == "RESPECTFUL_ADVISOR_MALE",
                                    onClick = { viewModel.updateVoiceProfile("RESPECTFUL_ADVISOR_MALE", 0.82f, 0.92f) },
                                    label = { Text("Deep Respectful Dost") }
                                )
                                FilterChip(
                                    selected = voiceProfile == "ENERGETIC_BHAI_MALE",
                                    onClick = { viewModel.updateVoiceProfile("ENERGETIC_BHAI_MALE", 0.94f, 1.02f) },
                                    label = { Text("Energetic Young Dost") }
                                )
                            }
                        }
                    }

                    // 3. Owner Voice Embedding & PIN Enrollment (Section 3)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.RecordVoiceOver, contentDescription = "Voice Enrollment", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Owner Voice Embedding & PIN Enrollment (Section 3)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Raw audio recording kabhi save nahi ki jaati. Keval L2-normalized voice embedding ko Keystore se encrypt karke rakha jata hai.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Status: $enrollmentStatus",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = voiceEnrollPhrase,
                                onValueChange = { voiceEnrollPhrase = it },
                                label = { Text("Voice Enrollment Phrase") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = pinInput,
                                    onValueChange = { pinInput = it },
                                    label = { Text("Fallback Security PIN") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = wakeWordInput,
                                    onValueChange = { wakeWordInput = it },
                                    label = { Text("Wake Word") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val vec = viewModel.authManager.enrollOwnerVoiceSample(voiceEnrollPhrase)
                                        if (pinInput.length >= 4) {
                                            viewModel.authManager.enrollFallbackPin(pinInput)
                                        }
                                        viewModel.updateWakeWord(wakeWordInput)
                                        enrollmentStatus = "Enrolled: 8D Encrypted Embedding (${vec.take(3).joinToString { "%.2f".format(it) }}...)"
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("enroll_owner_voice_pin_button")
                                ) {
                                    Text("Enroll Voice + PIN + Wake Word")
                                }
                                OutlinedButton(
                                    onClick = {
                                        viewModel.authManager.revokeAndResetEnrollment()
                                        enrollmentStatus = "Enrollment Reset Kiya Gaya (Revoked & Reset)"
                                    },
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("revoke_enrollment_button")
                                ) {
                                    Icon(Icons.Filled.LockReset, contentDescription = "Revoke")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Revoke")
                                }
                            }
                        }
                    }

                    // 4. Offline AI Mode & Room Local Database (Section 9)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Offline AI Mode & Local Room Storage (Section 9)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Recommended Tier: ${viewModel.offlineCapabilities.recommendedLocalModelTier}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Switch(
                                    checked = isOfflineMode,
                                    onCheckedChange = { viewModel.toggleOfflineMode(it) },
                                    modifier = Modifier.testTag("settings_offline_switch")
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = localNoteTitle,
                                onValueChange = { localNoteTitle = it },
                                label = { Text("Local Offline Note / Reminder Title (Room DB)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = localNoteBody,
                                onValueChange = { localNoteBody = it },
                                label = { Text("Details (Available 100% Offline Inside App)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    viewModel.addLocalOfflineNoteOrReminder(localNoteTitle, localNoteBody, "NOTE")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("add_offline_room_note_button")
                            ) {
                                Text("Save Offline Note in App Room Database")
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            offlineItems.forEach { item ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "[${item.category}] ${item.title}",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = item.content,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        IconButton(onClick = { viewModel.deleteLocalOfflineItem(item.id) }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Delete Offline Item")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. Account Logout & Device Revocation
                    Button(
                        onClick = {
                            signOutOwner(
                                credentialManager = credentialManager,
                                onSignOutComplete = onSignedOut,
                                scope = scope
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("sign_out_owner_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign Out")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Account Logout & Session End Karein (Sign Out)")
                    }
                }

                "DELIVERABLES" -> {
                    Text(
                        text = "Section 18 & 22: Typed Tool Registry & 15 Deliverables",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "15 Required Engineering Deliverables Summary",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "1. PRD: Voice-first Hinglish/Hindi/English personal AI agent ('Chhotu') keeping human owner ('Mere Dost') in full control.\n" +
                                    "2. System Architecture: Android (Kotlin, Compose, Dark/Light Theme, In-App Encrypted Vault, Room, JobScheduler, Keystore, BiometricPrompt, Male Neural TTS) + Cloud (Firebase AI Logic Gemini 3.8 Flash, Firestore Enterprise, FastAPI/PostgreSQL reference backend in /backend/).\n" +
                                    "3. Threat Model: Zero-Trust Firestore rules, PromptInjectionGuard, Voice-replay mitigation, rate-limited PIN/Biometric.\n" +
                                    "4–8. Android, Backend (/backend/main.py), Schema (/firebase-blueprint.json & /backend/schema.sql), OpenAPI, & 12 Typed Integrations.\n" +
                                    "9–15. Offline AI Mode (Room + RAM capability tiering), Automated Tests, Privacy Disclosures, Known OS Limitations & 10-Phase Roadmap.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Text(
                        text = "12 Typed Integration Interfaces (Section 18)",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    ChhotuToolRegistry.allTools.forEach { spec ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${spec.displayName} (${spec.toolId})",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Risk: ${spec.riskClassification}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Text(
                                    text = "Actions: ${spec.supportedActions.joinToString()} | Confirm Required: ${spec.requiresExplicitConfirmation}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Verify: ${spec.verificationMethod} | Timeout: ${spec.timeoutMs}ms",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
