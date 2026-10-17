package com.example.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class AgentTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val userRequest: String = "",
    val intentCategory: String = "INFO_RETRIEVAL",
    val riskLevel: String = "LOW",
    val status: String = "PENDING",
    val requiresApproval: Boolean = false,
    val approvedByOwner: Boolean = false,
    val affectedResource: String = "device",
    val reversible: Boolean = true,
    val verificationResult: String = "PENDING_VERIFICATION",
    val planSummary: String = "",
    val executionTarget: String = "LOCAL_DEVICE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BusinessProject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "MARKET_RESEARCH",
    val stage: String = "ACTIVE",
    val estimatedExpenseInr: Long = 0L,
    val actualRevenueInr: Long = 0L,
    val summary: String = "",
    val nextAction: String = "",
    val deadlineText: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SecureFileRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fileName: String = "",
    val mimeType: String = "text/plain",
    val folder: String = "General",
    val tags: String = "",
    val sizeBytes: Long = 0L,
    val encryptedWithKeystore: Boolean = true,
    val storageLocation: String = "DEVICE_ENCRYPTED",
    val aiSummary: String = "",
    val sha256Checksum: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SecurityAuditLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val eventType: String = "AUTH_VERIFIED",
    val severity: String = "INFO",
    val actorIdentity: String = "Owner",
    val description: String = "",
    val taskRefId: String = "N/A",
    @ServerTimestamp val createdAt: Timestamp? = null
)
