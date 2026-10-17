package com.example.ui

import android.net.Uri
import android.os.Environment
import android.os.StatFs
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun FilesAndStorageScreen(
    viewModel: ChhotuViewModel
) {
    val context = LocalContext.current
    val inAppVaultFiles by viewModel.inAppVaultFiles.collectAsStateWithLifecycle()
    val decryptedPreview by viewModel.decryptedPreviewText.collectAsStateWithLifecycle()
    val filesState by viewModel.secureFilesState.collectAsStateWithLifecycle()
    val documentAiState by viewModel.documentAiState.collectAsStateWithLifecycle()

    var fileName by remember { mutableStateOf("Q4_Tax_Audit.pdf") }
    var folderName by remember { mutableStateOf("Finance_Vault") }
    var fileTags by remember { mutableStateOf("tax,audit,in-app-vault") }
    var fileBodyText by remember {
        mutableStateOf("FY2026 Q4 GST & Income Tax Summary: Total Taxable Revenue INR 12,80,000. All filings verified inside Chhotu In-App Vault.")
    }
    var aiGeneratedSummary by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Real device storage capacity inspection via StatFs
    val (freeGb, totalGb) = remember {
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val total = (stat.totalBytes / (1024.0 * 1024.0 * 1024.0)).toInt().coerceAtLeast(1)
            val free = (stat.availableBytes / (1024.0 * 1024.0 * 1024.0)).toInt()
            free to total
        } catch (_: Exception) {
            24 to 64
        }
    }

    // Zero-Permission Android Storage Access Framework (SAF) Document Picker
    val safDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val lastSeg = uri.lastPathSegment?.substringAfterLast('/') ?: "Imported_Document.txt"
            val extractedText = try {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText().take(4000) }
                    ?: "Imported via Android Storage Access Framework ($uri)"
            } catch (_: Exception) {
                "Imported via Android Storage Access Framework ($uri)"
            }
            fileName = lastSeg
            fileBodyText = extractedText
            viewModel.encryptAndStoreDocument(
                fileName = lastSeg,
                mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream",
                folder = folderName,
                tags = fileTags,
                rawTextContent = extractedText,
                storageLocation = "IN_APP_ENCRYPTED_VAULT",
                customAiSummary = aiGeneratedSummary
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "In-App Secure Storage Vault & AI Document Manager",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            // 1. In-App Storage Vault + Device + Cloud Architecture Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.EnhancedEncryption,
                            contentDescription = "Keystore Encryption",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "App Mein Hi Private Encrypted Storage (AES-256-GCM)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val inAppTotalBytes = inAppVaultFiles.sumOf { it.sizeBytes }
                    Text(
                        text = "Mere Dost, aapki files seedhe is App ke private encrypted vault (filesDir/chhotu_in_app_vault) mein Android Keystore AES-256-GCM ke saath surakshit rehti hain:\n" +
                            "• In-App Vault Files Saved: ${inAppVaultFiles.size} files (${inAppTotalBytes} bytes encrypted in-app)\n" +
                            "• Phone Storage Available: $freeGb GB Free / $totalGb GB Total\n" +
                            "• Cloud Encrypted Backup: Active (Sirf Maalik Ki Anumati Se)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    val usedFraction = ((totalGb - freeGb).toFloat() / totalGb.toFloat()).coerceIn(0.05f, 0.95f)
                    LinearProgressIndicator(
                        progress = { usedFraction },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 2. Decrypted In-App File Viewer Modal Card (when user taps "View / Decrypt In-App")
            AnimatedVisibility(visible = decryptedPreview != null) {
                decryptedPreview?.let { previewText ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("in_app_decrypted_preview_card"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "In-App Decrypted File Viewer (Keystore Verified)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = { viewModel.clearDecryptedPreview() }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Close Preview")
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = previewText,
                                fontFamily = JetBrainsMonoFontFamily,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 3. Create / Encrypt & Save File to In-App Storage Card + Gemini AI Smart Auto-Tagger
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Nayi File App Storage Mein Encrypt Aur Save Karein",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = fileName,
                            onValueChange = { fileName = it },
                            label = { Text("File Name (e.g. Tax_Report.pdf)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("secure_file_name_input"),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = folderName,
                            onValueChange = { folderName = it },
                            label = { Text("In-App Folder") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = fileTags,
                        onValueChange = { fileTags = it },
                        label = { Text("Tags (comma separated)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = fileBodyText,
                        onValueChange = { fileBodyText = it },
                        label = { Text("Document Content (App ke andar AES-256-GCM se encrypt hoga)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Gemini 3.8 Flash Structured Document Intelligence Button
                    OutlinedButton(
                        onClick = {
                            viewModel.analyzeDocumentWithGemini(
                                fileName = fileName,
                                rawTextContent = fileBodyText,
                                onAutoFillMetadata = { suggestedFolder, suggestedTags, summary ->
                                    folderName = suggestedFolder
                                    fileTags = suggestedTags
                                    aiGeneratedSummary = summary
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("ai_analyze_document_button")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = "Gemini Document AI")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gemini AI Se Smart Folder, Tags & Summary Nikalein")
                    }

                    when (val docAi = documentAiState) {
                        is AiTaskState.Loading -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Gemini 3.8 Flash aapke document ka vishleshan kar raha hai mere dost...")
                            }
                        }
                        is AiTaskState.Error -> {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "AI Document Error: ${docAi.message}",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        is AiTaskState.Success -> {
                            val d = docAi.data
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "AI Classification • Sensitivity: ${d.sensitivityLevel}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Summary: ${d.hindiSummary}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Insights: ${d.actionableInsights}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                        AiTaskState.Idle -> {}
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.encryptAndStoreDocument(
                                    fileName = fileName,
                                    mimeType = "application/pdf",
                                    folder = folderName,
                                    tags = fileTags,
                                    rawTextContent = fileBodyText,
                                    customAiSummary = aiGeneratedSummary
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("encrypt_save_file_button")
                        ) {
                            Icon(Icons.Filled.EnhancedEncryption, contentDescription = "Encrypt & Save In-App")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save in App Storage")
                        }
                        OutlinedButton(
                            onClick = {
                                safDocumentLauncher.launch(arrayOf("*/*"))
                            },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("saf_pick_document_button")
                        ) {
                            Icon(Icons.Filled.UploadFile, contentDescription = "SAF Picker")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import Phone File")
                        }
                    }
                }
            }

            // 4. Search & Filter In-App Vault Files
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("In-App Vault Mein File, Folder Ya Tag Khojein...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_files_input"),
                singleLine = true
            )

            // 5. Primary In-App Encrypted Vault Files List (Always Available Inside App)
            Text(
                text = "App Ke Andar Surakshit Files (In-App Encrypted Storage Vault):",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            val filteredInApp = inAppVaultFiles.filter {
                searchQuery.isBlank() ||
                    it.fileName.contains(searchQuery, ignoreCase = true) ||
                    it.folder.contains(searchQuery, ignoreCase = true) ||
                    it.tags.contains(searchQuery, ignoreCase = true)
            }

            if (filteredInApp.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        text = "Koi matching file nahi mili mere dost. Upar 'Save in App Storage' button dabayein.",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                filteredInApp.forEach { file ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Folder,
                                        contentDescription = "Folder",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "${file.folder} / ${file.fileName}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Tags: ${file.tags} • ${file.sizeBytes} bytes • IN-APP VAULT",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                                Row {
                                    IconButton(
                                        onClick = { viewModel.previewInAppVaultFile(file) },
                                        modifier = Modifier.testTag("view_in_app_file_${file.fileName}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Visibility,
                                            contentDescription = "Decrypt & View File In-App",
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.requestInAppVaultFileDeletion(file) },
                                        modifier = Modifier.testTag("delete_file_${file.fileName}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Request Deletion with Confirmation",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "SHA-256: ${file.sha256Checksum.take(24)}... • AES-256-GCM Keystore",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = file.aiSummary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 6. Cloud Synced File Records (Firestore)
            when (val st = filesState) {
                is DataUiState.Loading -> {}
                is DataUiState.Error -> {}
                is DataUiState.Success -> {
                    val cloudOnly = st.data.filter { cloudItem ->
                        inAppVaultFiles.none { it.fileName.equals(cloudItem.fileName, ignoreCase = true) }
                    }
                    if (cloudOnly.isNotEmpty()) {
                        Text(
                            text = "Cloud Backup Synced Records (${cloudOnly.size}):",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        cloudOnly.forEach { file ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${file.folder} / ${file.fileName}", fontWeight = FontWeight.Bold)
                                        Text(file.aiSummary, style = MaterialTheme.typography.bodySmall)
                                    }
                                    IconButton(onClick = { viewModel.requestSecureFileDeletion(file) }) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Delete Cloud File",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
