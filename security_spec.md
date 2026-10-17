# CHHOTU AI — Firestore Security Specification & Red Team Audit

## 1. Data Invariants
1. **Strict Multi-Tenant Ownership (`userId == request.auth.uid`)**: Every document in `/tasks/{taskId}`, `/business_projects/{projectId}`, `/secure_files/{fileId}`, and `/security_logs/{logId}` belongs exclusively to the authenticated owner whose UID is stored in `userId`.
2. **Path Variable Hardening (`isValidId`)**: All document IDs (`taskId`, `projectId`, `fileId`, `logId`) must match `^[a-zA-Z0-9_\-]+$` and be at most 128 characters.
3. **Immutable Creation Metadata**: `userId` and `createdAt` can never be mutated during an `update`.
4. **Temporal Integrity**: `createdAt` and `updatedAt` must be valid server timestamps (`is timestamp && <= request.time`).
5. **Append-Only Security Audit Logs**: Documents in `/security_logs/{logId}` are strictly immutable after creation (`allow update, delete: if false;`).
6. **Query Enforcement**: Every `allow list` rule explicitly enforces `resource.data.userId == request.auth.uid`.

## 2. The "Dirty Dozen" Adversarial Payloads
1. **Unauthenticated Task Read**: `unauthDb.collection("tasks").get()` -> Rejected (`isSignedIn()` fails).
2. **Cross-User Task Read**: Alice reads Bob's `/tasks/bob_task_1` -> Rejected (`existing().userId == request.auth.uid` fails).
3. **Unfiltered Collection Scan**: Alice runs `db.collection("tasks").get()` without `.where("userId", "==", aliceUid)` -> Rejected by Query Enforcer.
4. **Identity Spoofing on Create**: Alice creates a task with `userId: "bob_456"` -> Rejected (`incoming().userId == request.auth.uid` fails).
5. **Shadow Field Injection on Create**: Alice creates a task with an undeclared field `isAdminBypass: true` -> Rejected (`keys().hasOnly(...)` fails).
6. **Shadow Field Injection on Update**: Alice updates a task adding `secretOverride: 1` -> Rejected (`diff(existing()).affectedKeys().hasOnly(...)` fails).
7. **Owner Transfer Attack on Update**: Alice updates `userId` on her existing task to Bob -> Rejected (`incoming().userId == existing().userId` fails).
8. **Timestamp Backdating / Mutation**: Alice updates `createdAt` on an existing document -> Rejected (`incoming().createdAt == existing().createdAt` fails).
9. **Value Poisoning (Oversized String)**: Alice sends a 20,000-character `title` to `/tasks/{taskId}` -> Rejected (`isValidAgentTask` size check fails).
10. **Invalid Enum Injection**: Alice sets `riskLevel: "UNLIMITED"` on a task -> Rejected (`riskLevel in ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']` fails).
11. **Audit Log Tampering (Update)**: Alice attempts to modify `/security_logs/{logId}` -> Rejected (`allow update: if false`).
12. **Audit Log Deletion**: Alice attempts to delete `/security_logs/{logId}` -> Rejected (`allow delete: if false`).
