# CHHOTU AI (छोटू AI) — Complete Engineering Deliverables (Section 22)

## 1. Product Requirements Document (PRD)
- **Product Name**: CHHOTU AI (`छोटू AI`)
- **Category**: Secure, Voice-First Personal AI Agent, Mobile Automation Assistant, Personal Business Workspace, Sandboxed Coding Agent, and Encrypted File Vault for Android.
- **Default Language**: Hindi (`hi-IN`), with seamless English (`en-IN`) and Hinglish support.
- **Persona**: Warm, respectful, proactive, addressing the authenticated human owner as `"दोस्त"` while keeping the human owner strictly in control.
- **Core Principle**: Never claim an unverified action succeeded; never allow untrusted external content or unauthenticated speakers to execute privileged commands.

## 2. System Architecture
- **Android Client (`app/src/main/java/com/example/`)**:
  - **UI Layer**: Jetpack Compose (Material 3) with adaptive `NavigationBar` (phones) and `NavigationRail` (tablets/foldables), custom animated acoustic Voice Orb (`HomeVoiceScreen.kt`), and 5-point Human Confirmation Gate.
  - **Voice & Speech Layer**: `VoiceSpeechController.kt` wrapping Android `SpeechRecognizer` and `TextToSpeech` (`hi-IN` & `en-IN`) with configurable wake-word (`"छोटू"`).
  - **Agent & Planner Layer**: `ChhotuTaskPlannerAndAgent` + `ChhotuToolRegistry` (12 typed integrations) + `StopController` + `GeminiOrchestrator` (Firebase AI Logic `gemini-3.8-flash` with `Schema.obj` structured planning and streaming).
  - **Device Automation Layer**: `DeviceAutomationEngine.kt` (Android Intents & `PackageManager` verification), `ChhotuAccessibilityService.kt` (user-authorized foreground window state verification), and `ChhotuBackgroundJobService.kt` (`JobScheduler` persistent background jobs).
  - **Storage Layer**: Hybrid Cloud + Local persistence — **Cloud Firestore Enterprise** (`ChhotuCloudRepository.kt`) for cross-device synced tasks, business projects, file metadata, and append-only security logs + **Room SQLite** (`LocalOfflineDatabase.kt`) for Offline AI Mode notes, reminders, and local model metadata + **Android Keystore AES-256-GCM** (`KeystoreEncryptionManager.kt`).

## 3. Threat Model & Security Plan
1. **Voice Replay / Synthesis Defense**: Voice similarity score alone *never* authorizes `HIGH` or `CRITICAL` risk operations. Consequential actions require explicit in-app confirmation + `BiometricPrompt` / PIN verification (`OwnerAuthManager.kt`).
2. **Untrusted Speaker Rejection (Test E)**: Unknown speakers receive `"माफ़ करना, मैं केवल अपने मालिक के प्रमाणित निर्देशों पर काम कर सकता हूँ।"` and are logged in `/security_logs`.
3. **Prompt Injection Defense (Test H)**: `PromptInjectionGuard.kt` inspects external web pages, emails, and documents for override patterns before any tool planning.
4. **Zero-Trust Cloud Security**: `firestore.rules` enforces `request.auth.uid == userId`, strict field allowlists (`hasOnly`), size bounds, immutable `createdAt`, and append-only audit logs.

## 9. Offline-Mode Implementation Plan
- Evaluates device RAM via `ActivityManager.MemoryInfo` to recommend an appropriate local model tier.
- Supports offline calculations, local app launching, Room-backed local notes/reminders, and honest disclosure when live online resources are requested while offline (Test F).

## 11–13. Setup, Deployment, Privacy, Build & Release Instructions
1. **Firebase Provisioning**: Configured via `google-services.json`, `firebase_applet_config.xml`, and `configureAppCheck(this, intent)` in `MainActivity.onCreate()`.
2. **Firestore Security Rules**: Tested against local emulators via `node --test firestore.test.js` and Robolectric `ChhotuFirestoreRuleTest`, then deployed via `DeployRules`.
3. **Permissions & Privacy**: Uses least-privilege permissions (`RECORD_AUDIO` for voice commands, `USE_BIOMETRIC` for step-up confirmation, zero-permission Android Storage Access Framework `OpenDocument` for files). Never stores raw voice audio.

## 14. Known Operating-System & Hardware Limitations
- **Powered-Off Devices**: A powered-off phone cannot perform local computation (Test G); only previously authorized online tasks delegated to the remote job queue continue on the server.
- **Third-Party App Automation**: Android sandboxing prevents arbitrary cross-app UI manipulation unless the target app exposes official Intents/APIs or the owner explicitly enables `ChhotuAccessibilityService`. CHHOTU AI never bypasses CAPTCHAs, payment gateways, or OS security boundaries.
- **Physical Storage Limits**: Built-in mobile storage cannot allocate "1 trillion GB"; CHHOTU AI uses real `StatFs` device capacity telemetry paired with encrypted cloud storage.

## 15. Prioritized 10-Phase Roadmap
- **Phases 1–5 (Completed in MVP)**: Voice-first shell, owner authentication (`CredentialManager` + `BiometricPrompt` + encrypted voice embedding), Smart Task Planner, 12-tool registry, Intent/Browser automation, and Offline AI Mode.
- **Phases 6–10 (Completed & Verified)**: Cloud Firestore + FastAPI backend queue, Business & Coding workspaces, Keystore file vault, YouTube confirmation workflows, and full 10-scenario Acceptance Test verification (Tests A–J).
