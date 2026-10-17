package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.agent.ActionApprovalProposal
import com.example.agent.GeminiOrchestrator
import com.example.agent.VoiceOrbState
import com.example.automation.VoiceSpeechController
import kotlin.math.sin

@Composable
fun HomeVoiceScreen(
    viewModel: ChhotuViewModel
) {
    val context = LocalContext.current
    val orbState by viewModel.orbState.collectAsStateWithLifecycle()
    val speechBanner by viewModel.assistantSpeechBanner.collectAsStateWithLifecycle()
    val pendingApproval by viewModel.pendingApproval.collectAsStateWithLifecycle()
    val isOfflineMode by viewModel.isOfflineMode.collectAsStateWithLifecycle()
    val simulateUntrustedSpeaker by viewModel.simulateUntrustedSpeaker.collectAsStateWithLifecycle()
    val wakeWordEnabled by viewModel.wakeWordEnabled.collectAsStateWithLifecycle()
    val configuredWakeWord by viewModel.configuredWakeWord.collectAsStateWithLifecycle()
    val selectedAiModel by viewModel.selectedAiModel.collectAsStateWithLifecycle()
    val chatHistory by viewModel.chatHistory.collectAsStateWithLifecycle()
    val voiceProfile by viewModel.voiceProfile.collectAsStateWithLifecycle()
    val voicePitch by viewModel.voicePitch.collectAsStateWithLifecycle()
    val voiceSpeed by viewModel.voiceSpeed.collectAsStateWithLifecycle()

    var showChatDrawer by remember { mutableStateOf(false) }
    var textCommandInput by remember { mutableStateOf("") }
    var pinConfirmationInput by remember { mutableStateOf("1234") }

    val voiceController = remember {
        VoiceSpeechController(
            context = context,
            onTranscriptReady = { transcript ->
                viewModel.setOrbListening(false)
                viewModel.processCommand(
                    rawCommand = transcript,
                    onSpeakHindi = { reply -> voiceControllerSpeakSafe(it = reply) }
                )
            },
            onSpeechError = { errMsg ->
                viewModel.setOrbListening(false)
                viewModel.processCommand(
                    rawCommand = "Chhotu, voice status batao: $errMsg"
                )
            }
        )
    }

    // Sync selected Male Dost Voice Pitch & Speed with TTS Engine
    LaunchedEffect(voiceProfile, voicePitch, voiceSpeed) {
        voiceController.updateMaleVoiceParameters(
            pitch = voicePitch,
            speed = voiceSpeed,
            profileLabel = voiceProfile
        )
    }

    val isMicActive by voiceController.isMicrophoneActive.collectAsStateWithLifecycle()
    val isSpeaking by voiceController.isSpeaking.collectAsStateWithLifecycle()
    val partialSpeech by voiceController.partialSpeechText.collectAsStateWithLifecycle()
    val activeVoiceDesc by voiceController.activeVoiceDescription.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            voiceController.release()
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setOrbListening(true)
            voiceController.startListening("en-IN")
        } else {
            viewModel.setOrbListening(false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Status Row: Wake-word badge, Offline toggle, Speaker identity toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = wakeWordEnabled,
                onClick = { viewModel.toggleWakeWordEnabled(!wakeWordEnabled) },
                label = { Text("Wake Word: \"$configuredWakeWord\"") },
                modifier = Modifier.testTag("wake_word_chip")
            )
            FilterChip(
                selected = isOfflineMode,
                onClick = { viewModel.toggleOfflineMode(!isOfflineMode) },
                leadingIcon = {
                    if (isOfflineMode) {
                        Icon(Icons.Outlined.WifiOff, contentDescription = "Offline Mode", modifier = Modifier.size(16.dp))
                    }
                },
                label = { Text(if (isOfflineMode) "Offline AI Mode" else "Online ($selectedAiModel)") },
                modifier = Modifier.testTag("offline_mode_toggle_chip")
            )
            FilterChip(
                selected = simulateUntrustedSpeaker,
                onClick = { viewModel.toggleUntrustedSpeakerSimulation(!simulateUntrustedSpeaker) },
                label = {
                    Text(
                        if (simulateUntrustedSpeaker) "Speaker: Unknown (Test E)"
                        else "Speaker: Verified Owner (Mere Dost)"
                    )
                },
                modifier = Modifier.testTag("speaker_trust_toggle_chip")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Gemini AI Model Selector Bar (Firebase AI Logic Models)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val models = listOf(
                GeminiOrchestrator.MODEL_FLASH_DEFAULT to "Gemini 3.8 Flash (Default)",
                GeminiOrchestrator.MODEL_FLASH_LITE to "Gemini 3.5 Flash-Lite (Fast)",
                GeminiOrchestrator.MODEL_ARGON_REASONING to "Gemini 4 Argon (Pro STEM)"
            )
            models.forEach { (modelId, label) ->
                FilterChip(
                    selected = selectedAiModel == modelId,
                    onClick = { viewModel.selectAiModel(modelId) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = label,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text(label) },
                    modifier = Modifier.testTag("model_chip_$modelId")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Human Male Friend ("Saccha Dost - Ladka") Voice Profile Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val voicePresets = listOf(
                Triple("WARM_DOST_MALE", "Warm Dost Voice (Ladka)", 0.88f to 0.95f),
                Triple("RESPECTFUL_ADVISOR_MALE", "Deep Respectful Dost", 0.82f to 0.92f),
                Triple("ENERGETIC_BHAI_MALE", "Energetic Young Dost", 0.94f to 1.02f)
            )
            voicePresets.forEach { (id, label, params) ->
                FilterChip(
                    selected = voiceProfile == id,
                    onClick = {
                        viewModel.updateVoiceProfile(id, params.first, params.second)
                        voiceController.updateMaleVoiceParameters(params.first, params.second, label)
                        voiceController.speakResponse(
                            "Namaste Mere Dost! Main Chhotu bol raha hoon. Aapki har baat ka main poori izzat aur sacche dost ki tarah paalan karunga."
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.RecordVoiceOver,
                            contentDescription = label,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text(label) },
                    modifier = Modifier.testTag("voice_profile_chip_$id")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Central Animated AI Voice Orb & Waveform
        ChhotuVoiceOrb(
            orbState = orbState,
            isMicActive = isMicActive || isSpeaking,
            onOrbTapped = {
                if (isMicActive || orbState == VoiceOrbState.LISTENING) {
                    voiceController.stopAllVoiceActivity()
                    viewModel.setOrbListening(false)
                } else {
                    val hasPerm = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasPerm) {
                        viewModel.setOrbListening(true)
                        voiceController.startListening("en-IN")
                    } else {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // State Pill Indicator
        Surface(
            color = when (orbState) {
                VoiceOrbState.LISTENING -> MaterialTheme.colorScheme.secondaryContainer
                VoiceOrbState.AWAITING_APPROVAL -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                VoiceOrbState.TASK_FAILED -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                VoiceOrbState.TASK_COMPLETED -> Color(0xFF10B981).copy(alpha = 0.2f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            shape = RoundedCornerShape(50),
            modifier = Modifier.testTag("voice_orb_state_pill")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isMicActive) Icons.Filled.Mic else Icons.Filled.MicOff,
                    contentDescription = "Microphone Status",
                    tint = if (isMicActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = orbState.labelHindi,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (partialSpeech.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "“$partialSpeech”",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Row: Test Chhotu's Realistic Male Voice + Prominent Emergency STOP Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    voiceController.speakResponse(speechBanner)
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("test_chhotu_voice_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Speak Chhotu Response")
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Chhotu Ki Awaaz Sunein",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = {
                    voiceController.stopAllVoiceActivity()
                    viewModel.triggerEmergencyStopAll()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("stop_all_tasks_button")
            ) {
                Icon(Icons.Filled.StopCircle, contentDescription = "Stop All Tasks")
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "STOP — Sabhi Rokein",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Task Approval Interface (Section 2 & Section 8)
        AnimatedVisibility(visible = pendingApproval != null) {
            pendingApproval?.let { proposal ->
                TaskApprovalGateCard(
                    proposal = proposal,
                    pinInput = pinConfirmationInput,
                    onPinChange = { pinConfirmationInput = it },
                    onBiometricApprove = {
                        val act = context as? Activity
                        if (act != null) {
                            viewModel.authManager.triggerPlatformBiometricPrompt(
                                activity = act,
                                onVerified = {
                                    viewModel.approvePendingProposal(
                                        pinOrBiometricVerified = true,
                                        onSpeakHindi = { voiceController.speakResponse(it) }
                                    )
                                },
                                onFallbackOrError = {
                                    val pinOk = viewModel.authManager.verifyFallbackPin(pinConfirmationInput)
                                    viewModel.approvePendingProposal(
                                        pinOrBiometricVerified = pinOk,
                                        onSpeakHindi = { voiceController.speakResponse(it) }
                                    )
                                }
                            )
                        } else {
                            val pinOk = viewModel.authManager.verifyFallbackPin(pinConfirmationInput)
                            viewModel.approvePendingProposal(
                                pinOrBiometricVerified = pinOk,
                                onSpeakHindi = { voiceController.speakResponse(it) }
                            )
                        }
                    },
                    onPinApprove = {
                        val pinOk = viewModel.authManager.verifyFallbackPin(pinConfirmationInput)
                        viewModel.approvePendingProposal(
                            pinOrBiometricVerified = pinOk,
                            onSpeakHindi = { voiceController.speakResponse(it) }
                        )
                    },
                    onReject = { viewModel.rejectPendingProposal() }
                )
            }
        }

        // Assistant Hinglish Response Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .testTag("assistant_response_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Chhotu (Mere Dost • $selectedAiModel)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isOfflineMode) "OFFLINE LOCAL" else activeVoiceDesc.take(26),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = speechBanner,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Voice Command Presets in Hinglish (Tap to Execute & Hear Chhotu Speak)
        Text(
            text = "Quick Voice Commands (Tap Karein — Chhotu Turant Kaam Karega Aur Bolega):",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        val quickCommands = listOf(
            "Chhotu, Chrome khol aur OpenAI ki website khol.",
            "Chhotu, Chrome khol.",
            "Chhotu, Google par mere business ke liye 5 naye marketing ideas khojo.",
            "Chhotu, mera naya video YouTube par upload aur publish karo.",
            "Chhotu, 'Q4_Tax_Audit.pdf' document delete karo.",
            "Chhotu, supplier ko ₹25,000 ka payment bhejo."
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickCommands.forEachIndexed { idx, cmd ->
                AssistChip(
                    onClick = {
                        viewModel.processCommand(
                            rawCommand = cmd,
                            onSpeakHindi = { voiceController.speakResponse(it) }
                        )
                    },
                    label = { Text(cmd, maxLines = 1) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.testTag("quick_voice_cmd_$idx")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Optional Expandable Text Chat Interface + Multi-Turn Gemini AI Chat (Section 2)
        OutlinedButton(
            onClick = { showChatDrawer = !showChatDrawer },
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp)
                .height(48.dp)
                .testTag("toggle_chat_interface_button"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = "Toggle Chat")
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (showChatDrawer) "Text Chat Chhupayein (Hide Chat)"
                else "Text Chat & Live Gemini Multi-Turn Dost Baatcheet Kholein"
            )
        }

        AnimatedVisibility(visible = showChatDrawer) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = textCommandInput,
                            onValueChange = { textCommandInput = it },
                            label = { Text("Chhotu ko Hinglish/English mein command dein ya sawaal poochein...") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_command_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (textCommandInput.isNotBlank()) {
                                    val toSend = textCommandInput
                                    textCommandInput = ""
                                    viewModel.processCommand(
                                        rawCommand = toSend,
                                        onSpeakHindi = { voiceController.speakResponse(it) }
                                    )
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("send_chat_command_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Execute Agent Command",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dedicated Multi-Turn Streaming Gemini Chat Button
                    Button(
                        onClick = {
                            val prompt = textCommandInput.ifBlank {
                                "Chhotu mere dost, aaj mere business productivity aur phone security ko behtar banane ke 3 badhiya tips batao."
                            }
                            textCommandInput = ""
                            viewModel.askGeminiMultiTurnChat(
                                userPrompt = prompt,
                                onSpeakHindi = { voiceController.speakResponse(it) }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("gemini_multiturn_chat_button")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = "Gemini Multi-Turn Chat")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Live Gemini Multi-Turn Dost Chat (Streaming)")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    chatHistory.takeLast(6).forEach { turn ->
                        val isOwner = turn.sender == "OWNER"
                        Surface(
                            color = if (isOwner) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (isOwner) "Aap (Owner - Mere Dost)" else "Chhotu AI (Aapka Dost)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    turn.verificationBadge?.let { badge ->
                                        Text(
                                            text = badge,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = turn.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun voiceControllerSpeakSafe(it: String) {
    // No-op callback helper
}

@Composable
private fun ChhotuVoiceOrb(
    orbState: VoiceOrbState,
    isMicActive: Boolean,
    onOrbTapped: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "chhotu_orb_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    val primaryColor = when (orbState) {
        VoiceOrbState.LISTENING -> Color(0xFF00F5D4)
        VoiceOrbState.PROCESSING, VoiceOrbState.EXECUTING_TASK -> Color(0xFF38BDF8)
        VoiceOrbState.AWAITING_APPROVAL -> Color(0xFFF59E0B)
        VoiceOrbState.TASK_FAILED -> Color(0xFFFF4D6D)
        VoiceOrbState.TASK_COMPLETED -> Color(0xFF10B981)
        else -> Color(0xFFFF9F1C)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(195.dp)
            .clip(CircleShape)
            .clickable(onClick = onOrbTapped)
            .testTag("central_voice_orb")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension * 0.28f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.38f),
                        primaryColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = baseRadius * 1.65f * pulseScale
                ),
                radius = baseRadius * 1.65f * pulseScale,
                center = center
            )

            drawCircle(
                color = primaryColor.copy(alpha = 0.45f),
                radius = baseRadius * 1.25f * pulseScale,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )

            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(primaryColor, Color(0xFF00F5D4))
                ),
                radius = baseRadius,
                center = center
            )

            val barCount = 7
            val spacing = (baseRadius * 1.1f) / barCount
            val startX = center.x - (spacing * (barCount - 1) / 2f)
            for (i in 0 until barCount) {
                val x = startX + i * spacing
                val dynamicAmp = if (isMicActive || orbState != VoiceOrbState.IDLE) {
                    (sin(wavePhase + i * 0.9f) * 0.5f + 0.6f).coerceIn(0.2f, 1.0f)
                } else {
                    0.25f
                }
                val halfHeight = baseRadius * 0.48f * dynamicAmp
                drawLine(
                    color = Color(0xFF080D1A),
                    start = Offset(x, center.y - halfHeight),
                    end = Offset(x, center.y + halfHeight),
                    strokeWidth = 4.dp.toPx()
                )
            }
        }
    }
}

@Composable
private fun TaskApprovalGateCard(
    proposal: ActionApprovalProposal,
    pinInput: String,
    onPinChange: (String) -> Unit,
    onBiometricApprove: () -> Unit,
    onPinApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .padding(bottom = 16.dp)
            .testTag("task_approval_gate_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Approval Required",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Owner Approval Gate (Maalik Ki Explicit Anumati)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            ApprovalDetailLine("1. Kya Hoga (What will happen):", proposal.whatWillHappenHindi)
            ApprovalDetailLine("2. Affected Account / Resource:", proposal.affectedAccountOrResource)
            ApprovalDetailLine("3. Data Sent or Changed:", proposal.dataSentOrChanged)
            ApprovalDetailLine(
                "4. Kya Wapas Badla Ja Sakta Hai (Reversible):",
                if (proposal.isReversible) "Haan (Reversible)" else "Nahi — Permanent Action (Irreversible)"
            )
            ApprovalDetailLine("5. Exact Action Being Approved:", proposal.exactActionApproved)

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = pinInput,
                onValueChange = onPinChange,
                label = { Text("Owner Security PIN (Default: 1234)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("approval_pin_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPinApprove,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("approve_pin_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "PIN Approve", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PIN Approve")
                }

                OutlinedButton(
                    onClick = onBiometricApprove,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("approve_biometric_button")
                ) {
                    Icon(Icons.Filled.Fingerprint, contentDescription = "Biometric Approve", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Biometric")
                }

                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("reject_proposal_button")
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancel Action", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun ApprovalDetailLine(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
