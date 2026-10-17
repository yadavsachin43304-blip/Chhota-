package com.example.automation

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Implements Section 2: Realistic Human Male Friend ("Ladka / Saccha Dost") Voice & Speech Controller.
 *
 * Features:
 * - Real Android `SpeechRecognizer` (`en-IN` & `hi-IN`) for Hinglish, Hindi, and English voice commands.
 * - Real Android `TextToSpeech` tuned specifically for a **warm, natural, respectful young Indian male friend ("Chhotu - Mere Dost")**:
 *   - Automatically selects the highest-quality **Indian Male Neural Voice** (`en-in-x-end`, `en-in-x-ene`, `hi-in-x-hid`, `hi-in-x-hie`, or male feature tags).
 *   - Automatically switches between Indian English (`en-IN`) for Roman Hinglish text and Hindi (`hi-IN`) for Devanagari text so Hinglish words are pronounced naturally like a real Indian human male friend.
 *   - Configures natural male vocal depth (`pitch = 0.88f`) and human conversational warmth (`speechRate = 0.95f`).
 *   - Adds natural conversational pauses and strips technical codes before speaking.
 */
class VoiceSpeechController(
    private val context: Context,
    private val onTranscriptReady: (String) -> Unit,
    private val onSpeechError: (String) -> Unit
) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "VoiceSpeechController"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private var currentPitch: Float = 0.88f
    private var currentSpeed: Float = 0.95f
    private var preferredHindiMaleVoice: Voice? = null
    private var preferredIndianEnglishMaleVoice: Voice? = null

    private val _isMicrophoneActive = MutableStateFlow(false)
    val isMicrophoneActive: StateFlow<Boolean> = _isMicrophoneActive.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _partialSpeechText = MutableStateFlow("")
    val partialSpeechText: StateFlow<String> = _partialSpeechText.asStateFlow()

    private val _activeVoiceDescription = MutableStateFlow("Warm Human Male Dost Voice (Pitch 0.88 • Speed 0.95x)")
    val activeVoiceDescription: StateFlow<String> = _activeVoiceDescription.asStateFlow()

    init {
        try {
            textToSpeech = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.w(TAG, "TextToSpeech initialization skipped in headless environment", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val tts = textToSpeech ?: return
            try {
                tts.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
            } catch (_: Exception) {
            }

            val indianEnglishLocale = Locale.forLanguageTag("en-IN")
            val hindiLocale = Locale.forLanguageTag("hi-IN")
            val langRes = tts.setLanguage(indianEnglishLocale)
            if (langRes == TextToSpeech.LANG_MISSING_DATA || langRes == TextToSpeech.LANG_NOT_SUPPORTED) {
                val hiRes = tts.setLanguage(hindiLocale)
                if (hiRes == TextToSpeech.LANG_MISSING_DATA || hiRes == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.language = Locale.US
                }
            }

            cacheBestMaleFriendVoices(tts)
            tts.setPitch(currentPitch)
            tts.setSpeechRate(currentSpeed)

            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })

            isTtsReady = true
        }
    }

    /**
     * Discovers and caches the highest-quality Male Indian (`en-IN` and `hi-IN`) neural voices on the device.
     */
    private fun cacheBestMaleFriendVoices(tts: TextToSpeech) {
        try {
            val allVoices: Set<Voice> = tts.voices ?: return

            fun scoreMaleVoice(v: Voice): Int {
                val lowerName = v.name.lowercase()
                val isKnownMaleId = lowerName.contains("male") ||
                    lowerName.contains("-hid") ||
                    lowerName.contains("-hie") ||
                    lowerName.contains("-end") ||
                    lowerName.contains("-ene") ||
                    lowerName.contains("en-us-x-iom") ||
                    lowerName.contains("en-us-x-tpd") ||
                    lowerName.contains("en-gb-x-rjs")
                val localBonus = if (!v.isNetworkConnectionRequired) 200 else 0
                return (if (isKnownMaleId) 1000 else 0) + localBonus + v.quality
            }

            preferredIndianEnglishMaleVoice = allVoices
                .filter { it.locale.language.equals("en", ignoreCase = true) && it.locale.country.equals("IN", ignoreCase = true) }
                .ifEmpty { allVoices.filter { it.locale.language.equals("en", ignoreCase = true) } }
                .maxByOrNull { scoreMaleVoice(it) }

            preferredHindiMaleVoice = allVoices
                .filter { it.locale.language.equals("hi", ignoreCase = true) }
                .maxByOrNull { scoreMaleVoice(it) }

            val chosen = preferredIndianEnglishMaleVoice ?: preferredHindiMaleVoice
            if (chosen != null) {
                tts.voice = chosen
                _activeVoiceDescription.value =
                    "Human Male Dost (${chosen.locale.displayLanguage} • Pitch ${"%.2f".format(currentPitch)})"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Voice selection fallback used", e)
        }
    }

    fun updateMaleVoiceParameters(pitch: Float, speed: Float, profileLabel: String = "Warm Human Male Dost") {
        currentPitch = pitch.coerceIn(0.65f, 1.25f)
        currentSpeed = speed.coerceIn(0.75f, 1.30f)
        try {
            textToSpeech?.setPitch(currentPitch)
            textToSpeech?.setSpeechRate(currentSpeed)
            _activeVoiceDescription.value =
                "$profileLabel (Pitch ${"%.2f".format(currentPitch)} • Speed ${"%.2f".format(currentSpeed)}x)"
        } catch (_: Exception) {
        }
    }

    fun startListening(languageTag: String = "en-IN") {
        stopSpeaking()
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onSpeechError("Mere Dost, is emulator par microphone hardware service active nahi hai — aap niche diye gaye Quick Command buttons ya Chat input ka use kar sakte hain.")
            return
        }

        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _isMicrophoneActive.value = true
                        }

                        override fun onBeginningOfSpeech() {
                            _isMicrophoneActive.value = true
                        }

                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            _isMicrophoneActive.value = false
                        }

                        override fun onError(error: Int) {
                            _isMicrophoneActive.value = false
                            val msg = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "Audio recording mein samasya aayi mere dost."
                                SpeechRecognizer.ERROR_NO_MATCH -> "Koi saaf awaaz sunai nahi di mere dost. Kripya dobara bolein ya Quick Command button dabayein."
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission (RECORD_AUDIO) zaroori hai mere dost."
                                else -> "Voice mic abhi uplabdh nahi hai (Code $error) — aap Quick Voice Commands ya Chat se turant kaam kar sakte hain mere dost."
                            }
                            onSpeechError(msg)
                        }

                        override fun onResults(results: Bundle?) {
                            _isMicrophoneActive.value = false
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val transcript = matches?.firstOrNull().orEmpty()
                            _partialSpeechText.value = transcript
                            if (transcript.isNotBlank()) {
                                onTranscriptReady(transcript)
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val partial = partialResults
                                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull()
                                .orEmpty()
                            if (partial.isNotBlank()) {
                                _partialSpeechText.value = partial
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            _partialSpeechText.value = ""
            _isMicrophoneActive.value = true
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _isMicrophoneActive.value = false
            Log.e(TAG, "SpeechRecognizer failed to start", e)
            onSpeechError("Microphone start nahi ho saka mere dost: ${e.localizedMessage}")
        }
    }

    /**
     * Speaks Chhotu's response in a realistic, emotional, respectful human male friend voice.
     */
    fun speakResponse(text: String) {
        if (!isTtsReady || text.isBlank()) return
        try {
            val tts = textToSpeech ?: return
            val hasDevanagari = text.any { it in '\u0900'..'\u097F' }
            val targetVoice = if (hasDevanagari) {
                preferredHindiMaleVoice ?: preferredIndianEnglishMaleVoice
            } else {
                preferredIndianEnglishMaleVoice ?: preferredHindiMaleVoice
            }
            if (targetVoice != null) {
                tts.voice = targetVoice
            }

            val cleanedForHumanSpeech = prepareHumanFriendSpeech(text)
            _isSpeaking.value = true
            tts.setPitch(currentPitch)
            tts.setSpeechRate(currentSpeed)
            tts.speak(
                cleanedForHumanSpeech.take(380),
                TextToSpeech.QUEUE_FLUSH,
                null,
                "chhotu_utt_${System.currentTimeMillis()}"
            )
        } catch (e: Exception) {
            Log.w(TAG, "TTS speak failed", e)
            _isSpeaking.value = false
        }
    }

    private fun prepareHumanFriendSpeech(rawText: String): String {
        return rawText
            // Strip parenthetical technical codes like (VERIFIED_APP_LAUNCHED) so Chhotu sounds like a real human friend
            .replace(Regex("\\([A-Z0-9_\\s.:/-]+\\)"), "")
            .replace("•", "... ")
            .replace("—", "... ")
            .replace("->", ", ")
            .replace("→", ", ")
            .replace("CHHOTU AI", "Chhotu")
            .trim()
    }

    fun stopSpeaking() {
        _isSpeaking.value = false
        try {
            textToSpeech?.stop()
        } catch (_: Exception) {
        }
    }

    fun stopAllVoiceActivity() {
        _isMicrophoneActive.value = false
        _isSpeaking.value = false
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            textToSpeech?.stop()
        } catch (_: Exception) {
        }
    }

    fun release() {
        stopAllVoiceActivity()
        try {
            speechRecognizer?.destroy()
            textToSpeech?.shutdown()
        } catch (_: Exception) {
        }
    }
}
