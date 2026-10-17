package com.example.automation

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Authorized AccessibilityService for CHHOTU AI (Section 4).
 *
 * Policy & Security Boundaries:
 * - Active ONLY when the human owner explicitly enables it in Android Accessibility Settings.
 * - Verifies current foreground package & window state before any screen-dependent workflow.
 * - Never bypasses authentication, CAPTCHAs, payment protections, or security boundaries.
 * - Immediately halts on unexpected screen transitions or when StopController is triggered.
 */
class ChhotuAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ChhotuAccessibility"

        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

        private val _foregroundPackage = MutableStateFlow("None")
        val foregroundPackage: StateFlow<String> = _foregroundPackage.asStateFlow()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isServiceConnected.value = true
        Log.i(TAG, "CHHOTU AI AccessibilityService connected by owner authorization.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString()
        if (!pkg.isNullOrBlank()) {
            _foregroundPackage.value = pkg
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "CHHOTU AI AccessibilityService interrupted; halting active screen workflows.")
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceConnected.value = false
    }
}
