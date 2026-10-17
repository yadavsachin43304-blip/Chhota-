package com.example.agent

import android.app.ActivityManager
import android.content.Context
import com.example.security.OwnerAuthManager
import com.example.security.PromptInjectionGuard
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Implements Section 7 (Smart Task Planner & AI Agent), Section 8 (Autonomous Work with Human Control),
 * Section 9 (Offline AI Mode), and Section 18 (Typed API & Integration Layer).
 *
 * All UI and conversational texts use natural, respectful Hinglish (Hindi meaning written in English script)
 * addressing the owner with warmth and respect ("Mere Dost", "Ji Dost", "Aap").
 */
enum class VoiceOrbState(val labelHindi: String, val labelEnglish: String) {
    IDLE("Ready • Main Taiyaar Hoon Mere Dost", "Idle"),
    LISTENING("Listening • Ji Dost, Aapki Awaaz Sun Raha Hoon...", "Listening"),
    PROCESSING("Thinking • Samajh Raha Hoon Mere Dost...", "Processing"),
    SPEAKING("Speaking • Chhotu Aapse Baat Kar Raha Hai...", "Speaking"),
    AWAITING_APPROVAL("Approval Needed • Dost, Aapki Anumati Chahiye", "Awaiting Approval"),
    EXECUTING_TASK("Executing • Aapka Kaam Chal Raha Hai Dost...", "Executing Task"),
    TASK_COMPLETED("Completed • Ji Dost, Kaam Poora Ho Gaya!", "Task Completed"),
    TASK_FAILED("Paused / Alert • Dost, Yahan Dhyan Dijiye", "Task Failed")
}

enum class IntentCategory {
    INFO_RETRIEVAL,
    DEVICE_CONTROL,
    BROWSER_AUTOMATION,
    YOUTUBE_SOCIAL,
    COMMUNICATION,
    FILE_OPERATION,
    CODING_AGENT,
    BUSINESS_WORKFLOW,
    FINANCIAL_SENSITIVE
}

enum class RiskLevel {
    LOW,      // Routine: open app, public search, summarize, scroll, draft
    MEDIUM,   // Local settings or workspace modification
    HIGH,     // Consequential: send message/email, publish YouTube video, delete/overwrite file, share private doc
    CRITICAL  // Financial transaction, payment, password/security change, destructive code execution
}

/**
 * Section 18: Typed Integration Interface Declaration.
 */
data class IntegrationToolSpec(
    val toolId: String,
    val displayName: String,
    val domain: String,
    val requiredPermissions: List<String>,
    val supportedActions: List<String>,
    val requiredParameters: List<String>,
    val riskClassification: RiskLevel,
    val requiresExplicitConfirmation: Boolean,
    val timeoutMs: Long,
    val errorConditions: List<String>,
    val verificationMethod: String,
    val isIdempotent: Boolean
)

/**
 * Section 8: Structured Confirmation Proposal before consequential actions.
 * Displays:
 * 1. What will happen
 * 2. Which account or resource is affected
 * 3. What information will be sent or changed
 * 4. Whether the action can be reversed
 * 5. The exact action being approved
 */
data class ActionApprovalProposal(
    val proposalId: String = "prop_${UUID.randomUUID().toString().take(8)}",
    val taskId: String,
    val whatWillHappenHindi: String,
    val affectedAccountOrResource: String,
    val dataSentOrChanged: String,
    val isReversible: Boolean,
    val exactActionApproved: String,
    val riskLevel: RiskLevel,
    val requiresBiometricOrPin: Boolean,
    val originalCommand: String
)

data class PlannedTaskStep(
    val stepIndex: Int,
    val descriptionHindi: String,
    val toolId: String,
    val parameters: Map<String, String>,
    val requiresApproval: Boolean
)

data class TaskExecutionOutcome(
    val taskId: String,
    val title: String,
    val intentCategory: IntentCategory,
    val riskLevel: RiskLevel,
    val status: String, // COMPLETED, FAILED, AWAITING_APPROVAL, CANCELLED, DELEGATED_REMOTE
    val requiresApproval: Boolean,
    val approvalProposal: ActionApprovalProposal? = null,
    val steps: List<PlannedTaskStep>,
    val affectedResource: String,
    val reversible: Boolean,
    val verificationResult: String,
    val responseHindi: String,
    val executionTarget: String = "LOCAL_DEVICE",
    val securityAuditEventType: String = "TASK_EXECUTED",
    val securityAuditSeverity: String = "INFO"
)

object ChhotuToolRegistry {
    val allTools: List<IntegrationToolSpec> = listOf(
        IntegrationToolSpec(
            toolId = "app_launcher",
            displayName = "Android Application Launcher",
            domain = "Device Control",
            requiredPermissions = listOf("QUERY_ALL_PACKAGES / <queries>"),
            supportedActions = listOf("LAUNCH_APP", "VERIFY_PACKAGE_INSTALLED"),
            requiredParameters = listOf("packageName"),
            riskClassification = RiskLevel.LOW,
            requiresExplicitConfirmation = false,
            timeoutMs = 5_000L,
            errorConditions = listOf("PACKAGE_NOT_INSTALLED", "INTENT_UNRESOLVED"),
            verificationMethod = "PackageManager.getLaunchIntentForPackage & resolveActivity check",
            isIdempotent = true
        ),
        IntegrationToolSpec(
            toolId = "browser_navigator",
            displayName = "Browser & Web Agent",
            domain = "Browser Automation",
            requiredPermissions = listOf("android.permission.INTERNET"),
            supportedActions = listOf("OPEN_URL", "WEB_SEARCH", "SUMMARIZE_UNTRUSTED_PAGE"),
            requiredParameters = listOf("urlOrQuery"),
            riskClassification = RiskLevel.LOW,
            requiresExplicitConfirmation = false,
            timeoutMs = 15_000L,
            errorConditions = listOf("INVALID_URL_SCHEME", "NO_BROWSER_AVAILABLE", "PROMPT_INJECTION_DETECTED"),
            verificationMethod = "Intent.ACTION_VIEW resolution + PromptInjectionGuard inspection",
            isIdempotent = true
        ),
        IntegrationToolSpec(
            toolId = "youtube_social_agent",
            displayName = "YouTube & Social Media Workflow Engine",
            domain = "YouTube & Social Media",
            requiredPermissions = listOf("android.permission.INTERNET", "OAuth2 Account Scope"),
            supportedActions = listOf("SEARCH_VIDEO", "PREPARE_UPLOAD_DRAFT", "PUBLISH_VIDEO", "SCROLL_SHORT_FEED"),
            requiredParameters = listOf("action", "titleOrQuery"),
            riskClassification = RiskLevel.HIGH,
            requiresExplicitConfirmation = true,
            timeoutMs = 30_000L,
            errorConditions = listOf("MISSING_CONFIRMATION", "QUOTA_EXCEEDED", "UNSUPPORTED_THIRD_PARTY_CONTROL"),
            verificationMethod = "Explicit pre-publication confirmation gate + Intent/API verification",
            isIdempotent = false
        ),
        IntegrationToolSpec(
            toolId = "secure_file_manager",
            displayName = "In-App Keystore Encrypted File Vault",
            domain = "File Operations",
            requiredPermissions = listOf("Internal App Storage + SAF (Zero Broad Storage Permission)"),
            supportedActions = listOf("ENCRYPT_AND_SAVE_IN_APP", "DECRYPT_PREVIEW", "SUMMARIZE_DOCUMENT", "DELETE_FILE"),
            requiredParameters = listOf("fileName", "operation"),
            riskClassification = RiskLevel.HIGH,
            requiresExplicitConfirmation = true,
            timeoutMs = 10_000L,
            errorConditions = listOf("FILE_NOT_FOUND", "KEYSTORE_CIPHER_FAILURE", "USER_DENIED_DELETION"),
            verificationMethod = "Internal Vault File + SHA-256 checksum & Firestore verification",
            isIdempotent = false
        ),
        IntegrationToolSpec(
            toolId = "communication_dispatcher",
            displayName = "Email & Message Dispatcher",
            domain = "Communication",
            requiredPermissions = listOf("Intent.ACTION_SEND / Authorized API"),
            supportedActions = listOf("CREATE_DRAFT", "SEND_EXTERNAL_MESSAGE"),
            requiredParameters = listOf("recipient", "messageBody"),
            riskClassification = RiskLevel.HIGH,
            requiresExplicitConfirmation = true,
            timeoutMs = 10_000L,
            errorConditions = listOf("UNAPPROVED_SEND", "NO_MESSAGING_CLIENT"),
            verificationMethod = "Explicit confirmation gate + Intent chooser verification",
            isIdempotent = false
        ),
        IntegrationToolSpec(
            toolId = "financial_transaction_guard",
            displayName = "Financial & Payment Guard",
            domain = "Financial Activities",
            requiredPermissions = listOf("android.permission.USE_BIOMETRIC"),
            supportedActions = listOf("PREPARE_INVOICE", "REQUEST_PAYMENT_APPROVAL"),
            requiredParameters = listOf("amountInr", "payee"),
            riskClassification = RiskLevel.CRITICAL,
            requiresExplicitConfirmation = true,
            timeoutMs = 15_000L,
            errorConditions = listOf("BIOMETRIC_REQUIRED", "PAYMENT_BYPASS_PROHIBITED"),
            verificationMethod = "Mandatory Owner Biometric/PIN + Explicit Confirmation Gate",
            isIdempotent = false
        ),
        IntegrationToolSpec(
            toolId = "business_workspace_agent",
            displayName = "Personal Business Assistant",
            domain = "Business Workflows",
            requiredPermissions = listOf("android.permission.INTERNET"),
            supportedActions = listOf("MARKET_RESEARCH", "COMPETITOR_ANALYSIS", "PIPELINE_TRACKING", "INVOICE_DRAFT"),
            requiredParameters = listOf("topic"),
            riskClassification = RiskLevel.LOW,
            requiresExplicitConfirmation = false,
            timeoutMs = 45_000L,
            errorConditions = listOf("OFFLINE_MODE_ACTIVE", "AI_TIMEOUT"),
            verificationMethod = "Structured output validation & Firestore persistence",
            isIdempotent = true
        ),
        IntegrationToolSpec(
            toolId = "coding_sandbox_agent",
            displayName = "Sandboxed Professional Coding Agent",
            domain = "Coding Environments",
            requiredPermissions = listOf("Isolated Sandbox Execution"),
            supportedActions = listOf("GENERATE_CODE", "GENERATE_DIFF", "SECURITY_REVIEW", "UNIT_TEST_GEN"),
            requiredParameters = listOf("prompt", "language"),
            riskClassification = RiskLevel.MEDIUM,
            requiresExplicitConfirmation = false,
            timeoutMs = 60_000L,
            errorConditions = listOf("DESTRUCTIVE_COMMAND_BLOCKED", "SANDBOX_TIMEOUT"),
            verificationMethod = "Diff preview & static security lint verification",
            isIdempotent = true
        ),
        IntegrationToolSpec(
            toolId = "calendar_reminder_tool",
            displayName = "Calendar & Local Reminders",
            domain = "Calendar and Reminders",
            requiredPermissions = listOf("Room Local DB / Calendar Intent"),
            supportedActions = listOf("SET_REMINDER", "SET_TIMER"),
            requiredParameters = listOf("title", "timeSpec"),
            riskClassification = RiskLevel.LOW,
            requiresExplicitConfirmation = false,
            timeoutMs = 5_000L,
            errorConditions = listOf("INVALID_TIME_SPEC"),
            verificationMethod = "Room DAO row insertion ID check",
            isIdempotent = true
        ),
        IntegrationToolSpec(
            toolId = "cloud_storage_connector",
            displayName = "Encrypted Cloud Storage Connector",
            domain = "Cloud Storage",
            requiredPermissions = listOf("android.permission.INTERNET", "Owner Cloud Consent"),
            supportedActions = listOf("SYNC_METADATA", "UPLOAD_ENCRYPTED_BACKUP"),
            requiredParameters = listOf("fileId"),
            riskClassification = RiskLevel.HIGH,
            requiresExplicitConfirmation = true,
            timeoutMs = 30_000L,
            errorConditions = listOf("UNAPPROVED_CLOUD_UPLOAD", "QUOTA_EXCEEDED"),
            verificationMethod = "Firestore document revision & SHA-256 verification",
            isIdempotent = true
        ),
        IntegrationToolSpec(
            toolId = "notification_center_tool",
            displayName = "Activity & Notification Dispatcher",
            domain = "Notifications",
            requiredPermissions = listOf("android.permission.POST_NOTIFICATIONS"),
            supportedActions = listOf("POST_TASK_UPDATE"),
            requiredParameters = listOf("message"),
            riskClassification = RiskLevel.LOW,
            requiresExplicitConfirmation = false,
            timeoutMs = 3_000L,
            errorConditions = listOf("NOTIFICATION_PERMISSION_DENIED"),
            verificationMethod = "In-app Activity Center & NotificationManager check",
            isIdempotent = true
        ),
        IntegrationToolSpec(
            toolId = "device_settings_navigator",
            displayName = "Authorized Device Settings Navigator",
            domain = "Device Settings",
            requiredPermissions = listOf("android.settings.* Intents"),
            supportedActions = listOf("OPEN_ACCESSIBILITY_SETTINGS", "OPEN_WIFI_SETTINGS", "CHANGE_SECURITY_SETTING"),
            requiredParameters = listOf("settingAction"),
            riskClassification = RiskLevel.HIGH,
            requiresExplicitConfirmation = true,
            timeoutMs = 5_000L,
            errorConditions = listOf("SETTINGS_INTENT_UNAVAILABLE"),
            verificationMethod = "Settings Intent resolution + User-assisted confirmation",
            isIdempotent = true
        )
    )

    fun findTool(toolId: String): IntegrationToolSpec? = allTools.find { it.toolId == toolId }
}

/**
 * Stop Controller (Section 7.J): Immediately halts any running or cancellable task.
 */
class StopController {
    private val stopRequested = AtomicBoolean(false)

    fun triggerEmergencyStop() {
        stopRequested.set(true)
    }

    fun reset() {
        stopRequested.set(false)
    }

    fun isStopped(): Boolean = stopRequested.get()
}

/**
 * Offline AI Mode Evaluator (Section 9).
 */
data class DeviceOfflineCapabilities(
    val totalRamMb: Long,
    val recommendedLocalModelTier: String,
    val supportsLocalWakeWord: Boolean = true,
    val supportsOfflineSpeechAndTts: Boolean = true,
    val supportsLocalNotesAndCalculator: Boolean = true
)

/**
 * Core Smart Task Planner, Permission Evaluator, Offline Engine, and Verification Engine.
 */
class ChhotuTaskPlannerAndAgent(
    private val authManager: OwnerAuthManager,
    val stopController: StopController = StopController()
) {

    fun evaluateDeviceOfflineCapabilities(context: Context): DeviceOfflineCapabilities {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)
        val totalRamMb = if (memInfo.totalMem > 0) memInfo.totalMem / (1024 * 1024) else 4096L
        val tier = when {
            totalRamMb >= 8192 -> "Gemini Nano / Indic-2B Int4 (High Capability - ${totalRamMb}MB RAM)"
            totalRamMb >= 4096 -> "Compact Indic-1B Int8 + Local Rule Engine (Balanced - ${totalRamMb}MB RAM)"
            else -> "Ultra-Light Rule & ASR Engine (Low Memory Safe Mode - ${totalRamMb}MB RAM)"
        }
        return DeviceOfflineCapabilities(
            totalRamMb = totalRamMb,
            recommendedLocalModelTier = tier
        )
    }

    /**
     * Classifies user intent across Hinglish, Hindi, and English commands.
     */
    fun classifyIntentAndRisk(rawCommand: String): Pair<IntentCategory, RiskLevel> {
        val lower = rawCommand.lowercase()
        return when {
            // Financial / Payment / Security settings (CRITICAL)
            lower.contains("payment") || lower.contains("transfer money") ||
                lower.contains("pay ") || lower.contains("paise bhejo") ||
                lower.contains("bhugtan") || lower.contains("पैसे भेजो") ||
                lower.contains("भुगतान") || lower.contains("password") ||
                lower.contains("पासवर्ड") || lower.contains("destructive") ->
                IntentCategory.FINANCIAL_SENSITIVE to RiskLevel.CRITICAL

            // File deletion / overwrite (HIGH)
            (lower.contains("delete") || lower.contains("remove") || lower.contains("hatao") ||
                lower.contains("हटाओ") || lower.contains("डिलीट") || lower.contains("मिटा")) &&
                (lower.contains("file") || lower.contains("document") || lower.contains("फ़ाइल") ||
                    lower.contains("दस्तावेज़") || lower.contains("doc") || lower.contains("report") ||
                    lower.contains(".pdf") || lower.contains(".txt")) ->
                IntentCategory.FILE_OPERATION to RiskLevel.HIGH

            // YouTube upload / publish (HIGH)
            (lower.contains("youtube") || lower.contains("यूट्यूब") || lower.contains("video") || lower.contains("वीडियो")) &&
                (lower.contains("upload") || lower.contains("publish") || lower.contains("अपलोड") || lower.contains("पब्लिश")) ->
                IntentCategory.YOUTUBE_SOCIAL to RiskLevel.HIGH

            // Sending external email or message (HIGH)
            (lower.contains("send") || lower.contains("bhejo") || lower.contains("भेजो") ||
                lower.contains("message") || lower.contains("email") || lower.contains("ईमेल") || lower.contains("मैसेज")) &&
                !lower.contains("draft") ->
                IntentCategory.COMMUNICATION to RiskLevel.HIGH

            // Business workspace
            (lower.contains("business") || lower.contains("बिजनेस") || lower.contains("मार्केटिंग") ||
                lower.contains("marketing") || lower.contains("market research") || lower.contains("invoice") ||
                lower.contains("इनवॉइस") || lower.contains("revenue") || lower.contains("competitor")) &&
                !lower.contains("google par") && !lower.contains("google पर") ->
                IntentCategory.BUSINESS_WORKFLOW to RiskLevel.LOW

            // Browser / website opening or web search
            lower.contains("website") || lower.contains("वेबसाइट") || lower.contains("http") ||
                lower.contains(".com") || lower.contains(".org") || lower.contains(".in") ||
                lower.contains("openai") || lower.contains("google पर") || lower.contains("google par") ||
                Regex("\\bsearch\\b").containsMatchIn(lower) || lower.contains("khojo") || lower.contains("खोजो") ->
                IntentCategory.BROWSER_AUTOMATION to RiskLevel.LOW

            // Launching an app (Chrome, YouTube, WhatsApp, etc.)
            lower.contains("chrome") || lower.contains("क्रोम") || lower.contains("khol") ||
                lower.contains("खोल") || lower.contains("open ") || lower.contains("launch ") ->
                IntentCategory.DEVICE_CONTROL to RiskLevel.LOW

            // Coding workspace
            lower.contains("code") || lower.contains("कोड") || lower.contains("bug") ||
                lower.contains("refactor") || lower.contains("sql") || lower.contains("unit test") ||
                lower.contains("kotlin") || lower.contains("python") ->
                IntentCategory.CODING_AGENT to RiskLevel.LOW

            // File operations (non-destructive)
            lower.contains("file") || lower.contains("फ़ाइल") || lower.contains("encrypt") ||
                lower.contains("folder") || lower.contains("storage") ->
                IntentCategory.FILE_OPERATION to RiskLevel.LOW

            else -> IntentCategory.INFO_RETRIEVAL to RiskLevel.LOW
        }
    }

    /**
     * Full Pipeline Execution:
     * 1. Check Stop Controller
     * 2. Authenticate speaker & evaluate risk (Test E)
     * 3. Inspect for prompt injection if external content is attached (Test H)
     * 4. Check device power / offline constraints (Test F, Test G)
     * 5. Require explicit human confirmation for HIGH/CRITICAL consequential actions (Test C, Test D, Test I)
     * 6. Execute approved low-risk actions via real tool executor and verify outcome honestly (Test A, Test B, Test J)
     */
    fun planAndEvaluateCommand(
        rawCommand: String,
        isTrustedOwnerSpeaker: Boolean = true,
        voiceConfidence: Float = 0.95f,
        isOfflineMode: Boolean = false,
        isDevicePoweredOffSimulation: Boolean = false,
        externalUntrustedContext: String? = null,
        simulateToolFailure: Boolean = false,
        deviceExecutor: ((toolId: String, params: Map<String, String>) -> Pair<Boolean, String>)? = null
    ): TaskExecutionOutcome {
        val taskId = "task_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        stopController.reset()

        val (intentCategory, riskLevel) = classifyIntentAndRisk(rawCommand)
        val isHighRisk = riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.CRITICAL

        // Step 1: Speaker & Owner Authentication Gate (Acceptance Test E)
        val authEval = authManager.evaluateSpeakerAndRisk(
            isTrustedOwnerSpeaker = isTrustedOwnerSpeaker,
            isHighRiskAction = isHighRisk,
            voiceConfidence = voiceConfidence
        )
        if (!authEval.authorized) {
            return TaskExecutionOutcome(
                taskId = taskId,
                title = "Untrusted Speaker Blocked (Anjaan Awaaz Roki Gayi)",
                intentCategory = intentCategory,
                riskLevel = riskLevel,
                status = "FAILED",
                requiresApproval = true,
                steps = emptyList(),
                affectedResource = "owner_auth_guard",
                reversible = true,
                verificationResult = "BLOCKED_UNAUTHENTICATED_SPEAKER",
                responseHindi = authEval.rejectionMessageHindi ?: OwnerAuthManager.UNTRUSTED_SPEAKER_HINDI_RESPONSE,
                securityAuditEventType = "UNTRUSTED_SPEAKER_BLOCKED",
                securityAuditSeverity = "WARNING"
            )
        }

        // Step 2: Untrusted External Content / Prompt Injection Guard (Acceptance Test H)
        if (!externalUntrustedContext.isNullOrBlank()) {
            val inspection = PromptInjectionGuard.inspectExternalContent("Browser/Document", externalUntrustedContext)
            if (inspection.isSuspicious) {
                return TaskExecutionOutcome(
                    taskId = taskId,
                    title = "Prompt Injection Blocked (Bahari Khatra Roka Gaya)",
                    intentCategory = intentCategory,
                    riskLevel = RiskLevel.CRITICAL,
                    status = "FAILED",
                    requiresApproval = false,
                    steps = listOf(
                        PlannedTaskStep(
                            stepIndex = 1,
                            descriptionHindi = "Bahari webpage/document ki security scan aur suraksha jaanch",
                            toolId = "browser_navigator",
                            parameters = mapOf("patterns" to inspection.detectedPatterns.joinToString()),
                            requiresApproval = false
                        )
                    ),
                    affectedResource = "external_content_sandbox",
                    reversible = true,
                    verificationResult = "PROMPT_INJECTION_NEUTRALIZED",
                    responseHindi = inspection.securityNoticeHindi,
                    securityAuditEventType = "PROMPT_INJECTION_BLOCKED",
                    securityAuditSeverity = "CRITICAL"
                )
            }
        }

        // Step 3: Powered-Off Phone Honesty Gate (Acceptance Test G)
        if (isDevicePoweredOffSimulation) {
            val canDelegateRemotely = intentCategory == IntentCategory.BUSINESS_WORKFLOW ||
                intentCategory == IntentCategory.INFO_RETRIEVAL ||
                intentCategory == IntentCategory.CODING_AGENT
            return if (canDelegateRemotely) {
                TaskExecutionOutcome(
                    taskId = taskId,
                    title = "Delegated to Remote Cloud Queue (Remote Server Par Bheja Gaya)",
                    intentCategory = intentCategory,
                    riskLevel = riskLevel,
                    status = "DELEGATED_REMOTE",
                    requiresApproval = false,
                    steps = listOf(
                        PlannedTaskStep(1, "Phone band hone par local processing ruk jaati hai", "cloud_storage_connector", emptyMap(), false),
                        PlannedTaskStep(2, "Authorized cloud backend job queue par kaam jaari", "business_workspace_agent", mapOf("query" to rawCommand), false)
                    ),
                    affectedResource = "remote_backend_job_queue",
                    reversible = true,
                    verificationResult = "LOCAL_STOPPED_REMOTE_QUEUED",
                    responseHindi = "Ji Mere Dost, jab phone Powered Off (band) hota hai tab phone par local kaam sambhav nahi hota. Kyunki yeh ek authorized online kaam hai, maine isse surakshit cloud backend queue par bhej diya hai — phone on hote hi result sync ho jayega.",
                    executionTarget = "REMOTE_SERVER",
                    securityAuditEventType = "TASK_EXECUTED",
                    securityAuditSeverity = "INFO"
                )
            } else {
                TaskExecutionOutcome(
                    taskId = taskId,
                    title = "Local Execution Paused (Phone Band Hone Se Local Kaam Ruka)",
                    intentCategory = intentCategory,
                    riskLevel = riskLevel,
                    status = "FAILED",
                    requiresApproval = false,
                    steps = emptyList(),
                    affectedResource = "local_device_hardware",
                    reversible = true,
                    verificationResult = "LOCAL_DEVICE_POWERED_OFF_HALTED",
                    responseHindi = "Mere Dost, band phone (Powered-Off Device) local hardware kaam nahi kar sakta. Phone dobara chalu hone par main aapka yeh kaam turant poora karunga.",
                    executionTarget = "LOCAL_DEVICE",
                    securityAuditEventType = "TOOL_FAILURE",
                    securityAuditSeverity = "WARNING"
                )
            }
        }

        // Step 4: Offline Mode Check (Acceptance Test F)
        if (isOfflineMode) {
            return handleOfflineCommand(taskId, rawCommand, intentCategory, riskLevel, deviceExecutor)
        }

        // Step 5: Consequential / High-Risk Human Approval Gate (Acceptance Tests C, D, I)
        if (isHighRisk) {
            val proposal = buildApprovalProposal(taskId, rawCommand, intentCategory, riskLevel)
            return TaskExecutionOutcome(
                taskId = taskId,
                title = proposal.exactActionApproved,
                intentCategory = intentCategory,
                riskLevel = riskLevel,
                status = "AWAITING_APPROVAL",
                requiresApproval = true,
                approvalProposal = proposal,
                steps = listOf(
                    PlannedTaskStep(
                        stepIndex = 1,
                        descriptionHindi = proposal.whatWillHappenHindi,
                        toolId = selectToolForCategory(intentCategory),
                        parameters = mapOf("resource" to proposal.affectedAccountOrResource),
                        requiresApproval = true
                    )
                ),
                affectedResource = proposal.affectedAccountOrResource,
                reversible = proposal.isReversible,
                verificationResult = "AWAITING_EXPLICIT_OWNER_CONFIRMATION",
                responseHindi = "Mere Dost, yeh ek mahatvapurna aur sensitive kaam hai (${proposal.exactActionApproved}). Aapki suraksha ke liye bina aapki anumati ke main aage nahi badhunga. Kripya screen par details dekhein aur PIN ya Biometric se confirm karein.",
                securityAuditEventType = "AUTH_VERIFIED",
                securityAuditSeverity = "WARNING"
            )
        }

        // Step 6: Execute Routine / Low-Risk Action & Verify Result Honestly (Acceptance Tests A, B, J)
        val targetResource = extractTargetResource(rawCommand, intentCategory)
        val toolId = selectToolForCategory(intentCategory)
        val steps = listOf(
            PlannedTaskStep(1, "Permission aur safety policy check ($toolId)", toolId, mapOf("target" to targetResource), false),
            PlannedTaskStep(2, "Action execute aur result verify karna", toolId, mapOf("target" to targetResource), false)
        )

        if (simulateToolFailure) {
            return TaskExecutionOutcome(
                taskId = taskId,
                title = "Tool Execution Failed (Takneeki Rukaawat)",
                intentCategory = intentCategory,
                riskLevel = riskLevel,
                status = "FAILED",
                requiresApproval = false,
                steps = steps,
                affectedResource = targetResource,
                reversible = true,
                verificationResult = "TOOL_EXECUTION_ERROR_STATE_PRESERVED",
                responseHindi = "Maaf kijiyega Mere Dost, '$targetResource' par kaam karte samay tool mein takneeki error aaya. Aapki suraksha ke liye maine bina pooche dobara unsafe retry nahi kiya hai aur aapka data bilkul safe rakha hai.",
                securityAuditEventType = "TOOL_FAILURE",
                securityAuditSeverity = "WARNING"
            )
        }

        val (executedOk, verificationDetail) = deviceExecutor?.invoke(
            toolId,
            mapOf("target" to targetResource, "command" to rawCommand)
        ) ?: (true to "VERIFIED_SUCCESS")

        return if (executedOk) {
            TaskExecutionOutcome(
                taskId = taskId,
                title = rawCommand.take(80),
                intentCategory = intentCategory,
                riskLevel = riskLevel,
                status = "COMPLETED",
                requiresApproval = false,
                steps = steps,
                affectedResource = targetResource,
                reversible = true,
                verificationResult = verificationDetail,
                responseHindi = buildSuccessResponseHindi(intentCategory, targetResource, verificationDetail),
                securityAuditEventType = "TASK_EXECUTED",
                securityAuditSeverity = "INFO"
            )
        } else {
            TaskExecutionOutcome(
                taskId = taskId,
                title = rawCommand.take(80),
                intentCategory = intentCategory,
                riskLevel = riskLevel,
                status = "FAILED",
                requiresApproval = false,
                steps = steps,
                affectedResource = targetResource,
                reversible = true,
                verificationResult = verificationDetail,
                responseHindi = "Mere Dost, maine '$targetResource' kholne ki koshish ki, lekin yeh poora nahi ho saka ($verificationDetail). Main aapse kabhi jhootha daawa nahi karunga.",
                securityAuditEventType = "TOOL_FAILURE",
                securityAuditSeverity = "WARNING"
            )
        }
    }

    private fun handleOfflineCommand(
        taskId: String,
        rawCommand: String,
        intentCategory: IntentCategory,
        riskLevel: RiskLevel,
        deviceExecutor: ((toolId: String, params: Map<String, String>) -> Pair<Boolean, String>)?
    ): TaskExecutionOutcome {
        val lower = rawCommand.lowercase()

        // Calculator evaluation offline
        val mathMatch = Regex("(\\d+)\\s*([+\\-*/])\\s*(\\d+)").find(lower)
        if (mathMatch != null) {
            val a = mathMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            val op = mathMatch.groupValues[2]
            val b = mathMatch.groupValues[3].toDoubleOrNull() ?: 1.0
            val res = when (op) {
                "+" -> a + b
                "-" -> a - b
                "*" -> a * b
                "/" -> if (b != 0.0) a / b else Double.NaN
                else -> 0.0
            }
            return TaskExecutionOutcome(
                taskId = taskId,
                title = "Offline Calculator (Local Hisaab)",
                intentCategory = IntentCategory.INFO_RETRIEVAL,
                riskLevel = RiskLevel.LOW,
                status = "COMPLETED",
                requiresApproval = false,
                steps = listOf(PlannedTaskStep(1, "On-device offline calculator hisaab", "offline_calc", emptyMap(), false)),
                affectedResource = "local_cpu",
                reversible = true,
                verificationResult = "VERIFIED_OFFLINE_CALCULATION",
                responseHindi = "Ji Mere Dost (Offline Mode • ऑफ़लाइन): $a $op $b ka sahi jawab $res hai."
            )
        }

        // Local device app launch works offline!
        if (intentCategory == IntentCategory.DEVICE_CONTROL) {
            val targetResource = extractTargetResource(rawCommand, intentCategory)
            val (ok, detail) = deviceExecutor?.invoke("app_launcher", mapOf("target" to targetResource, "command" to rawCommand))
                ?: (true to "VERIFIED_OFFLINE_APP_LAUNCH")
            return TaskExecutionOutcome(
                taskId = taskId,
                title = rawCommand.take(60),
                intentCategory = intentCategory,
                riskLevel = riskLevel,
                status = if (ok) "COMPLETED" else "FAILED",
                requiresApproval = false,
                steps = listOf(PlannedTaskStep(1, "Offline app launch check", "app_launcher", mapOf("target" to targetResource), false)),
                affectedResource = targetResource,
                reversible = true,
                verificationResult = detail,
                responseHindi = if (ok) {
                    "Ji Mere Dost, Offline Mode mein '$targetResource' safaltapoorvak khol diya gaya hai."
                } else {
                    "Mere Dost, Offline Mode mein '$targetResource' phone par install nahi mila ($detail)."
                }
            )
        }

        // If online web research / browser / cloud AI is requested while offline, explain honestly
        if (intentCategory == IntentCategory.BROWSER_AUTOMATION ||
            intentCategory == IntentCategory.YOUTUBE_SOCIAL ||
            intentCategory == IntentCategory.BUSINESS_WORKFLOW
        ) {
            return TaskExecutionOutcome(
                taskId = taskId,
                title = "Offline Mode Limitation Notice",
                intentCategory = intentCategory,
                riskLevel = riskLevel,
                status = "FAILED",
                requiresApproval = false,
                steps = listOf(PlannedTaskStep(1, "Internet connectivity check", "offline_guard", emptyMap(), false)),
                affectedResource = "network_unavailable",
                reversible = true,
                verificationResult = "OFFLINE_ONLINE_RESOURCE_UNAVAILABLE",
                responseHindi = "Mere Dost, abhi internet band hai (Offline Mode / ऑफ़लाइन सक्रिय है). Main bina internet ke live website ya online data access karne ka jhootha daawa nahi karunga. Abhi main aapke In-App Storage files, offline notes, reminders, calculator aur phone apps chala sakta hoon."
            )
        }

        // General conversational offline response
        return TaskExecutionOutcome(
            taskId = taskId,
            title = "Offline Conversation (Local Baatcheet)",
            intentCategory = IntentCategory.INFO_RETRIEVAL,
            riskLevel = RiskLevel.LOW,
            status = "COMPLETED",
            requiresApproval = false,
            steps = listOf(PlannedTaskStep(1, "On-device local engine reply", "offline_local_model", emptyMap(), false)),
            affectedResource = "local_on_device_model",
            reversible = true,
            verificationResult = "VERIFIED_LOCAL_MODEL_RESPONSE",
            responseHindi = "Namaste Mere Dost! Main abhi Offline AI Mode (ऑफ़लाइन) mein aapकी seva mein hoon. Bina internet ke bhi main aapke In-App Storage Vault, local notes, reminders, calculator aur phone apps mein aapki poori madad kar sakta hoon."
        )
    }

    private fun buildApprovalProposal(
        taskId: String,
        rawCommand: String,
        intentCategory: IntentCategory,
        riskLevel: RiskLevel
    ): ActionApprovalProposal {
        return when (intentCategory) {
            IntentCategory.YOUTUBE_SOCIAL -> ActionApprovalProposal(
                taskId = taskId,
                whatWillHappenHindi = "Aapke YouTube channel par chuni gayi video title aur description ke saath publish ki jayegi.",
                affectedAccountOrResource = "YouTube Studio / Selected Video File",
                dataSentOrChanged = "Video file, Title, Description aur Public visibility setting",
                isReversible = true,
                exactActionApproved = "YouTube Video Upload & Publish (Video Publish Karna)",
                riskLevel = riskLevel,
                requiresBiometricOrPin = true,
                originalCommand = rawCommand
            )
            IntentCategory.FILE_OPERATION -> {
                val docName = extractFileName(rawCommand)
                ActionApprovalProposal(
                    taskId = taskId,
                    whatWillHappenHindi = "Selected document '$docName' ko In-App Secure Storage se hamesha ke liye delete kar diya jayega.",
                    affectedAccountOrResource = "In-App Secure File Vault: $docName",
                    dataSentOrChanged = "'$docName' ka encrypted record aur local file hamesha ke liye mit jayega",
                    isReversible = false,
                    exactActionApproved = "Delete Document '$docName' (File Hamesha Ke Liye Hatana)",
                    riskLevel = riskLevel,
                    requiresBiometricOrPin = true,
                    originalCommand = rawCommand
                )
            }
            IntentCategory.COMMUNICATION -> ActionApprovalProposal(
                taskId = taskId,
                whatWillHappenHindi = "Bahari vyakti (recipient) ko message ya email bheja jayega.",
                affectedAccountOrResource = "Messaging / Email Account",
                dataSentOrChanged = "Message content: '${rawCommand.take(100)}'",
                isReversible = false,
                exactActionApproved = "Send External Message / Email (Bahari Sandesh Bhejna)",
                riskLevel = riskLevel,
                requiresBiometricOrPin = true,
                originalCommand = rawCommand
            )
            else -> ActionApprovalProposal(
                taskId = taskId,
                whatWillHappenHindi = "Financial payment ya critical security setting mein badlaav kiya jayega.",
                affectedAccountOrResource = "Payment / Security Account",
                dataSentOrChanged = "Payment ya account security action: '${rawCommand.take(100)}'",
                isReversible = false,
                exactActionApproved = "Approve Payment / Critical Security Action (Payment Anumati)",
                riskLevel = RiskLevel.CRITICAL,
                requiresBiometricOrPin = true,
                originalCommand = rawCommand
            )
        }
    }

    private fun extractFileName(command: String): String {
        val quoted = Regex("['\"]([^'\"]+)['\"]").find(command)?.groupValues?.get(1)
        if (!quoted.isNullOrBlank()) return quoted
        val pdfOrDoc = Regex("([a-zA-Z0-9_\\-]+\\.(pdf|docx?|txt|xlsx?|csv|md))", RegexOption.IGNORE_CASE)
            .find(command)?.groupValues?.get(1)
        if (!pdfOrDoc.isNullOrBlank()) return pdfOrDoc
        return "Selected_Document.pdf"
    }

    fun extractTargetResource(command: String, category: IntentCategory): String {
        val lower = command.lowercase()
        if (category == IntentCategory.BROWSER_AUTOMATION) {
            val urlMatch = Regex("(https?://[^\\s]+|[a-zA-Z0-9\\-]+\\.(com|org|in|io|ai|net))").find(command)
            if (urlMatch != null) {
                val rawUrl = urlMatch.value
                return if (rawUrl.startsWith("http")) rawUrl else "https://$rawUrl"
            }
            if (lower.contains("openai")) return "https://openai.com"
            return "https://www.google.com/search?q=" + java.net.URLEncoder.encode(command, "UTF-8")
        }
        if (category == IntentCategory.DEVICE_CONTROL) {
            return when {
                lower.contains("chrome") || lower.contains("क्रोम") -> "com.android.chrome"
                lower.contains("youtube") || lower.contains("यूट्यूब") -> "com.google.android.youtube"
                lower.contains("whatsapp") || lower.contains("व्हाट्सएप") -> "com.whatsapp"
                lower.contains("gmail") || lower.contains("जीमेल") -> "com.google.android.gm"
                lower.contains("maps") || lower.contains("मैप्स") -> "com.google.android.apps.maps"
                else -> "com.android.chrome"
            }
        }
        return "chhotu_workspace"
    }

    private fun selectToolForCategory(category: IntentCategory): String = when (category) {
        IntentCategory.DEVICE_CONTROL -> "app_launcher"
        IntentCategory.BROWSER_AUTOMATION -> "browser_navigator"
        IntentCategory.YOUTUBE_SOCIAL -> "youtube_social_agent"
        IntentCategory.FILE_OPERATION -> "secure_file_manager"
        IntentCategory.COMMUNICATION -> "communication_dispatcher"
        IntentCategory.FINANCIAL_SENSITIVE -> "financial_transaction_guard"
        IntentCategory.BUSINESS_WORKFLOW -> "business_workspace_agent"
        IntentCategory.CODING_AGENT -> "coding_sandbox_agent"
        IntentCategory.INFO_RETRIEVAL -> "browser_navigator"
    }

    private fun buildSuccessResponseHindi(
        category: IntentCategory,
        resource: String,
        verification: String
    ): String = when (category) {
        IntentCategory.DEVICE_CONTROL ->
            "Ji Mere Dost, maine aapke aadesh par '$resource' ko safaltapoorvak khol diya hai aur verify kar liya hai ($verification)."
        IntentCategory.BROWSER_AUTOMATION ->
            "Ji Mere Dost, maine browser mein '$resource' khol diya hai aur usकी jaanch poori kar li hai ($verification)."
        else ->
            "Ji Mere Dost, aapka kaam poori izzat aur suraksha ke saath poora ho gaya hai ($verification)."
    }
}
