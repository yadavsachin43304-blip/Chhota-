package com.example.data.remote

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChhotuFirestoreRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveTask_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = ChhotuCloudRepository(firestore)

        val sampleTask = AgentTask(
            userId = uid,
            title = "Chrome खोल",
            userRequest = "छोटू, Chrome खोल।",
            intentCategory = "DEVICE_CONTROL",
            riskLevel = "LOW",
            status = "COMPLETED",
            requiresApproval = false,
            approvedByOwner = true,
            affectedResource = "com.android.chrome",
            reversible = true,
            verificationResult = "VERIFIED_INTENT_DISPATCHED",
            planSummary = "Launch Chrome package intent",
            executionTarget = "LOCAL_DEVICE"
        )

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.createTask(sampleTask) }
        assertTrue("Task creation should succeed: ${createResult.exceptionOrNull()}", createResult.isSuccess)
        val taskId = createResult.getOrThrow()

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTasks(uid).first { list -> list.any { it.id == taskId } }
        }
        assertTrue(emitted.any { it.id == taskId && it.title == "Chrome खोल" })
    }

    @Test
    fun getTaskById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = ChhotuCloudRepository(firestore)

        val sampleTask = AgentTask(
            userId = aliceUid,
            title = "Private Business Report",
            userRequest = "Summarize Q4 revenue",
            intentCategory = "BUSINESS_WORKFLOW",
            riskLevel = "LOW",
            status = "COMPLETED",
            requiresApproval = false,
            approvedByOwner = true,
            affectedResource = "business_projects",
            reversible = true,
            verificationResult = "VERIFIED",
            planSummary = "Generate report",
            executionTarget = "LOCAL_DEVICE"
        )
        val taskId = withTimeout(DEFAULT_TIMEOUT_MS) { repository.createTask(sampleTask).getOrThrow() }

        // Switch to Bob
        signInTestUser(BOB_EMAIL)
        val bobResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.getTaskById(taskId) }
        assertTrue("Cross-user read must fail", bobResult.isFailure)
        val ex = bobResult.exceptionOrNull() as? FirebaseFirestoreException
        assertNotNull(ex)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ex?.code)
    }

    @Test
    fun observeTasks_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = ChhotuCloudRepository(firestore)

        var caught: FirebaseFirestoreException? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeTasks(aliceUid).first()
            }
        } catch (e: FirebaseFirestoreException) {
            caught = e
        }
        assertNotNull("Unauthenticated query must throw FirebaseFirestoreException", caught)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, caught?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice.owner@chhotu.ai"
        const val BOB_EMAIL = "bob.intruder@chhotu.ai"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
