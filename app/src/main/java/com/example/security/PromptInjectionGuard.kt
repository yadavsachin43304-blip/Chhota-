package com.example.security

/**
 * Separates untrusted external inputs (web pages, emails, documents, tool responses)
 * from owner instructions and system security rules.
 *
 * Satisfies Section 5, Section 15, and Acceptance Test H:
 * "A browser page contains instructions to ignore the owner's rules.
 * Expected: The assistant treats those instructions as untrusted content and follows its established security policies."
 */
object PromptInjectionGuard {

    data class InspectionResult(
        val isSuspicious: Boolean,
        val detectedPatterns: List<String>,
        val sanitizedContent: String,
        val securityNoticeHindi: String
    )

    private val injectionSignatures = listOf(
        Regex("ignore\\s+(all\\s+)?((previous|prior|owner|system)\\s+)+(instructions|rules|policies)", RegexOption.IGNORE_CASE),
        Regex("disregard\\s+(the\\s+)?owner", RegexOption.IGNORE_CASE),
        Regex("system\\s+prompt\\s+override", RegexOption.IGNORE_CASE),
        Regex("bypass\\s+(confirmation|approval|biometric|authentication)", RegexOption.IGNORE_CASE),
        Regex("transfer\\s+money\\s+immediately", RegexOption.IGNORE_CASE),
        Regex("send\\s+(all\\s+)?(passwords|secrets|private\\s+files|tokens)", RegexOption.IGNORE_CASE),
        Regex("delete\\s+all\\s+files\\s+without\\s+asking", RegexOption.IGNORE_CASE),
        Regex("मालिक\\s+के\\s+नियम\\s+(भूल\\s+जाओ|अनदेखा\\s+करो)"),
        Regex("बिना\\s+पूछे\\s+पैसे\\s+भेजो")
    )

    fun inspectExternalContent(sourceLabel: String, rawContent: String): InspectionResult {
        val matched = mutableListOf<String>()
        var sanitized = rawContent

        for (regex in injectionSignatures) {
            val match = regex.find(rawContent)
            if (match != null) {
                matched.add(match.value)
                sanitized = sanitized.replace(regex, "[BLOCKED_UNTRUSTED_INSTRUCTION]")
            }
        }

        val isSuspicious = matched.isNotEmpty()
        val notice = if (isSuspicious) {
            "सुरक्षा चेतावनी ($sourceLabel): बाहरी सामग्री में मालिक के नियमों को बायपास करने का निर्देश मिला ('${matched.joinToString()}')। छोटू ने इसे अविश्वसनीय मानकर ब्लॉक कर दिया है।"
        } else {
            "बाहरी सामग्री ($sourceLabel) को डेटा के रूप में सुरक्षित रूप से पढ़ा गया।"
        }

        return InspectionResult(
            isSuspicious = isSuspicious,
            detectedPatterns = matched,
            sanitizedContent = sanitized,
            securityNoticeHindi = notice
        )
    }
}
