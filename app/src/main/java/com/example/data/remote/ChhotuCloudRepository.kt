package com.example.data.remote

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Cloud Firestore Repository for CHHOTU AI.
 * Strictly initializes with `R.string.firestore_database_id` and scopes all queries to `userId`.
 */
class ChhotuCloudRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- 1. AGENT TASKS ---

    fun observeTasks(userId: String = requireUserId()): Flow<List<AgentTask>> = callbackFlow {
        val path = "tasks"
        val registration = db.collection(path)
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot
                        .toObjects(AgentTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                    trySend(items)
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun createTask(task: AgentTask): Result<String> = runCatching {
        val uid = requireUserId()
        val taskId = task.id.ifBlank { "task_${UUID.randomUUID().toString().replace("-", "").take(16)}" }
        val docRef = db.collection("tasks").document(taskId)
        val payload = mapOf(
            "userId" to uid,
            "title" to task.title.take(300),
            "userRequest" to task.userRequest.take(4000),
            "intentCategory" to task.intentCategory,
            "riskLevel" to task.riskLevel,
            "status" to task.status,
            "requiresApproval" to task.requiresApproval,
            "approvedByOwner" to task.approvedByOwner,
            "affectedResource" to task.affectedResource.take(1000),
            "reversible" to task.reversible,
            "verificationResult" to task.verificationResult.take(2000),
            "planSummary" to task.planSummary.take(4000),
            "executionTarget" to task.executionTarget,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            taskId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun getTaskById(taskId: String): Result<AgentTask> = runCatching {
        val docRef = db.collection("tasks").document(taskId)
        try {
            val snap = docRef.get().await()
            snap.toObject(AgentTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: throw NoSuchElementException("Task $taskId not found")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun getUserTasks(userId: String = requireUserId()): Result<List<AgentTask>> = runCatching {
        val path = "tasks"
        try {
            val snap = db.collection(path)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            snap.toObjects(AgentTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            throw e
        }
    }

    suspend fun updateTaskStatus(
        taskId: String,
        status: String,
        approvedByOwner: Boolean,
        verificationResult: String,
        planSummary: String? = null
    ): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("tasks").document(taskId)
        val updates = mutableMapOf<String, Any>(
            "status" to status,
            "approvedByOwner" to approvedByOwner,
            "verificationResult" to verificationResult.take(2000),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (planSummary != null) {
            updates["planSummary"] = planSummary.take(4000)
        }
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteTask(taskId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("tasks").document(taskId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- 2. BUSINESS PROJECTS ---

    fun observeBusinessProjects(userId: String = requireUserId()): Flow<List<BusinessProject>> = callbackFlow {
        val path = "business_projects"
        val registration = db.collection(path)
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot
                        .toObjects(BusinessProject::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                    trySend(items)
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun createBusinessProject(project: BusinessProject): Result<String> = runCatching {
        val uid = requireUserId()
        val projectId = project.id.ifBlank { "proj_${UUID.randomUUID().toString().replace("-", "").take(16)}" }
        val docRef = db.collection("business_projects").document(projectId)
        val payload = mapOf(
            "userId" to uid,
            "name" to project.name.take(200),
            "category" to project.category,
            "stage" to project.stage,
            "estimatedExpenseInr" to project.estimatedExpenseInr,
            "actualRevenueInr" to project.actualRevenueInr,
            "summary" to project.summary.take(4000),
            "nextAction" to project.nextAction.take(500),
            "deadlineText" to project.deadlineText.take(100),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            projectId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteBusinessProject(projectId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("business_projects").document(projectId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- 3. SECURE FILES ---

    fun observeSecureFiles(userId: String = requireUserId()): Flow<List<SecureFileRecord>> = callbackFlow {
        val path = "secure_files"
        val registration = db.collection(path)
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot
                        .toObjects(SecureFileRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                    trySend(items)
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun createSecureFileRecord(record: SecureFileRecord): Result<String> = runCatching {
        val uid = requireUserId()
        val fileId = record.id.ifBlank { "file_${UUID.randomUUID().toString().replace("-", "").take(16)}" }
        val docRef = db.collection("secure_files").document(fileId)
        val payload = mapOf(
            "userId" to uid,
            "fileName" to record.fileName.take(255),
            "mimeType" to record.mimeType.take(120),
            "folder" to record.folder.take(100),
            "tags" to record.tags.take(500),
            "sizeBytes" to record.sizeBytes,
            "encryptedWithKeystore" to record.encryptedWithKeystore,
            "storageLocation" to record.storageLocation,
            "aiSummary" to record.aiSummary.take(4000),
            "sha256Checksum" to record.sha256Checksum.take(128),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            fileId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteSecureFileRecord(fileId: String): Result<Unit> = runCatching {
        requireUserId()
        val docRef = db.collection("secure_files").document(fileId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- 4. SECURITY AUDIT LOGS (APPEND-ONLY) ---

    fun observeSecurityLogs(userId: String = requireUserId()): Flow<List<SecurityAuditLog>> = callbackFlow {
        val path = "security_logs"
        val registration = db.collection(path)
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, path)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot
                        .toObjects(SecurityAuditLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                    trySend(items)
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun appendSecurityLog(
        userId: String = "",
        eventType: String,
        severity: String,
        actorIdentity: String,
        description: String,
        taskRefId: String = "N/A"
    ): Result<String> = runCatching {
        val uid = userId.ifBlank { requireUserId() }
        val logId = "log_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("security_logs").document(logId)
        val payload = mapOf(
            "userId" to uid,
            "eventType" to eventType,
            "severity" to severity,
            "actorIdentity" to actorIdentity.take(120),
            "description" to description.take(2000),
            "taskRefId" to taskRefId.take(128),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            logId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }
}
