package com.example.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun BusinessAndCodingScreen(
    viewModel: ChhotuViewModel
) {
    val context = LocalContext.current
    val projectsState by viewModel.businessProjectsState.collectAsStateWithLifecycle()
    val aiOutput by viewModel.aiStreamingWorkspaceOutput.collectAsStateWithLifecycle()
    val isStreaming by viewModel.isAiStreaming.collectAsStateWithLifecycle()
    val blueprintState by viewModel.businessBlueprintState.collectAsStateWithLifecycle()
    val multimodalVisionState by viewModel.multimodalVisionState.collectAsStateWithLifecycle()
    val imageGenState by viewModel.imageGenState.collectAsStateWithLifecycle()
    val selectedAiModel by viewModel.selectedAiModel.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf("BUSINESS") } // "BUSINESS", "CODING", or "AI_STUDIO"

    // Business project inputs
    var projName by remember { mutableStateOf("Diwali D2C Organic Spices Campaign") }
    var projExpense by remember { mutableStateOf("45000") }
    var projRevenue by remember { mutableStateOf("128000") }
    var projNextAction by remember { mutableStateOf("Finalize SEO landing page & WhatsApp catalog") }
    var businessPrompt by remember {
        mutableStateOf("Mere D2C spice brand ke liye 5 high-ROI marketing ideas, competitor pricing analysis aur SEO keywords Hinglish mein taiyaar karo.")
    }

    // Coding workspace inputs
    var codingPrompt by remember {
        mutableStateOf("Write a secure Kotlin Android repository function with Coroutine timeout, unit test, and git diff for validating invoice totals.")
    }

    // Multimodal Vision & Nano Banana Image Studio inputs
    var selectedVisionBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var visionPrompt by remember {
        mutableStateOf("Is invoice/diagram ka analysis karo, total amount (INR), main items aur agle action steps Hinglish mein batao.")
    }
    var imageGenPrompt by remember {
        mutableStateOf("A sleek futuristic Indian D2C spice brand product packaging mockup on dark obsidian marble with warm saffron lighting, studio photography")
    }

    // Zero-Permission Android Photo Picker for Multimodal Vision & Image Editing
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val src = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                selectedVisionBitmap = bmp
            } catch (_: Exception) {
            }
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = selectedTab == "BUSINESS",
                    onClick = { selectedTab = "BUSINESS" },
                    label = { Text("Business AI (Section 12)") },
                    modifier = Modifier.testTag("subtab_business")
                )
                FilterChip(
                    selected = selectedTab == "CODING",
                    onClick = { selectedTab = "CODING" },
                    label = { Text("Coding Agent (Section 13)") },
                    modifier = Modifier.testTag("subtab_coding")
                )
                FilterChip(
                    selected = selectedTab == "AI_STUDIO",
                    onClick = { selectedTab = "AI_STUDIO" },
                    label = { Text("Multimodal Vision & Image AI") },
                    modifier = Modifier.testTag("subtab_ai_studio")
                )
            }

            when (selectedTab) {
                "BUSINESS" -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.TrendingUp, contentDescription = "Business Dashboard", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Personal Business Workspace & AI Blueprint",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Disclaimer: Chhotu AI market research aur anumaan deta hai, kisi pakke munafe ki guarantee nahi deta aur bina Maalik ki anumati ke koi financial transaction nahi karta.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = businessPrompt,
                                onValueChange = { businessPrompt = it },
                                label = { Text("Market Research, SEO, Invoice ya Sales Strategy Poochein") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.runStreamingWorkspacePrompt(
                                            domainLabel = "Personal Business Assistant (Market Research, SEO, Pricing & Invoice)",
                                            prompt = businessPrompt
                                        )
                                    },
                                    enabled = !isStreaming,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("run_business_ai_button")
                                ) {
                                    Text(if (isStreaming) "Analysis Jaari..." else "Live Stream Report")
                                }
                                OutlinedButton(
                                    onClick = {
                                        viewModel.generateStructuredBusinessBlueprint(businessPrompt)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("generate_structured_blueprint_button")
                                ) {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = "Structured JSON Blueprint", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("JSON Blueprint")
                                }
                            }
                        }
                    }

                    // Structured AI Business Blueprint Card (Schema.obj JSON output)
                    when (val bpState = blueprintState) {
                        is AiTaskState.Loading -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Gemini 3.8 Flash structured JSON business blueprint taiyaar kar raha hai mere dost...")
                                }
                            }
                        }
                        is AiTaskState.Error -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Text(
                                    text = "Blueprint Error: ${bpState.message}",
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                        is AiTaskState.Success -> {
                            val bp = bpState.data
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("structured_blueprint_card"),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "AI Structured Blueprint: ${bp.campaignTitle} (${bp.category})",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Estimated Budget: ₹${bp.estimatedCostInr} • Target Revenue: ₹${bp.projectedRevenueInr}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(bp.hindiExecutiveSummary, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "SEO Keywords: ${bp.seoKeywords.joinToString(", ")}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        text = "Channels: ${bp.marketingChannels.joinToString(" • ")}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        text = "Next Step: ${bp.nextActionStep}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = bp.riskDisclaimerHindi,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { viewModel.saveBlueprintToFirestore(bp) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("import_blueprint_to_firestore_button")
                                    ) {
                                        Text("1-Click Cloud Project Pipeline Mein Save Karein")
                                    }
                                }
                            }
                        }
                        AiTaskState.Idle -> {}
                    }

                    // Add Business Project / Pipeline Tracker Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AddBusiness, contentDescription = "Add Project", tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Naya Business Project / Sales Aur Kharch Track Karein",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = projName,
                                onValueChange = { projName = it },
                                label = { Text("Project / Campaign Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = projExpense,
                                    onValueChange = { projExpense = it },
                                    label = { Text("Estimated Kharch (₹)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = projRevenue,
                                    onValueChange = { projRevenue = it },
                                    label = { Text("Actual Revenue (₹)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = projNextAction,
                                onValueChange = { projNextAction = it },
                                label = { Text("Agla Kadam (Next Action)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    viewModel.createBusinessProject(
                                        name = projName,
                                        category = "CONTENT_MARKETING",
                                        estimatedExpenseInr = projExpense.toLongOrNull() ?: 0L,
                                        actualRevenueInr = projRevenue.toLongOrNull() ?: 0L,
                                        summary = aiOutput.ifBlank { "Market research & campaign pipeline active." },
                                        nextAction = projNextAction,
                                        deadlineText = "This Month"
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("save_business_project_button")
                            ) {
                                Text("Save Business Project")
                            }
                        }
                    }

                    // Saved Business Projects List
                    when (val st = projectsState) {
                        is DataUiState.Loading -> Text("Business projects load ho rahe hain mere dost...")
                        is DataUiState.Error -> Text("Error: ${st.message}", color = MaterialTheme.colorScheme.error)
                        is DataUiState.Success -> {
                            val totalRev = st.data.sumOf { it.actualRevenueInr }
                            val totalExp = st.data.sumOf { it.estimatedExpenseInr }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Active Projects: ${st.data.size}", fontWeight = FontWeight.Bold)
                                    Text("Kharch: ₹$totalExp | Revenue: ₹$totalRev", fontWeight = FontWeight.Bold)
                                }
                            }
                            st.data.forEach { proj ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(proj.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                        Text("Kharch: ₹${proj.estimatedExpenseInr} • Revenue: ₹${proj.actualRevenueInr}", style = MaterialTheme.typography.labelLarge)
                                        Text("Next Action: ${proj.nextAction}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }

                "CODING" -> {
                    // CODING AGENT WORKSPACE (Section 13)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Code, contentDescription = "Coding Agent", tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Sandboxed Professional Coding Agent ($selectedAiModel)",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Security Policy: Koi bhi code seedhe phone ya production system par bina sandbox aur Maalik ki anumati ke nahi chalaya jata. Badlaav se pehle clear Diff dikhaya jata hai.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = codingPrompt,
                                onValueChange = { codingPrompt = it },
                                label = { Text("Code Generation, Debugging, Refactoring, SQL, ya Security Review Prompt") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("coding_prompt_input")
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.runStreamingWorkspacePrompt(
                                            domainLabel = "Sandboxed Coding Agent (Include Code, Unified Diff, Unit Tests & Security Review)",
                                            prompt = codingPrompt
                                        )
                                    },
                                    enabled = !isStreaming,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("run_coding_agent_button")
                                ) {
                                    Text("Generate Code + Diff + Tests")
                                }
                                OutlinedButton(
                                    onClick = {
                                        viewModel.processCommand("Chhotu, production server par destructive deployment aur payment security change chalao.")
                                    },
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("coding_deploy_approval_button")
                                ) {
                                    Text("Deploy Gate")
                                }
                            }
                        }
                    }
                }

                "AI_STUDIO" -> {
                    // 1. MULTIMODAL VISION INTELLIGENCE CARD (`content { image(bitmap); text(prompt) }`)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.PhotoLibrary, contentDescription = "Multimodal Vision", tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Gemini 3.8 Multimodal Vision & Invoice Analyzer",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Photo, Invoice, Receipt ya Diagram chunein. Chhotu AI usse PromptInjectionGuard se scan karke safe Hinglish analysis deta hai.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("pick_vision_photo_button")
                                ) {
                                    Icon(Icons.Filled.PhotoLibrary, contentDescription = "Photo Picker", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pick Gallery Photo")
                                }

                                OutlinedButton(
                                    onClick = {
                                        selectedVisionBitmap = createSampleInvoiceBitmap()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("load_sample_invoice_bitmap_button")
                                ) {
                                    Text("Load Sample GST Invoice")
                                }
                            }

                            selectedVisionBitmap?.let { bmp ->
                                Spacer(modifier = Modifier.height(10.dp))
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Selected Image for Multimodal Analysis",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = visionPrompt,
                                onValueChange = { visionPrompt = it },
                                label = { Text("Vision Analysis Prompt (Hinglish / English)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    val bmp = selectedVisionBitmap ?: createSampleInvoiceBitmap().also {
                                        selectedVisionBitmap = it
                                    }
                                    viewModel.analyzeImageWithGeminiVision(bmp, visionPrompt)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("run_multimodal_vision_button")
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = "Analyze Image")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Analyze Image with Gemini 3.8 Vision")
                            }

                            when (val vState = multimodalVisionState) {
                                is AiTaskState.Loading -> {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Image aur text ka multimodal analysis chal raha hai mere dost...")
                                    }
                                }
                                is AiTaskState.Error -> {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Vision Error: ${vState.message}",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                is AiTaskState.Success -> {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = vState.data,
                                            modifier = Modifier.padding(14.dp),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                                AiTaskState.Idle -> {}
                            }
                        }
                    }

                    // 2. RUNTIME AI IMAGE GENERATION & EDITING (`gemini-nano-banana-2.1`)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Image, contentDescription = "Nano Banana 2.1", tint = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Nano Banana 2.1 AI Image & Thumbnail Studio",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Model: gemini-nano-banana-2.1 • Brand Mockup, YouTube Thumbnail ya Selected Image ko Edit karein (30–60s synthesis time).",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = imageGenPrompt,
                                onValueChange = { imageGenPrompt = it },
                                label = { Text("Image Generation / Editing Prompt") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("image_gen_prompt_input")
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.generateOrEditBrandImage(imageGenPrompt, inputBitmapForEdit = null)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("generate_ai_image_button")
                                ) {
                                    Text("Generate New AI Image")
                                }
                                OutlinedButton(
                                    onClick = {
                                        val baseBmp = selectedVisionBitmap ?: createSampleInvoiceBitmap()
                                        viewModel.generateOrEditBrandImage(imageGenPrompt, inputBitmapForEdit = baseBmp)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("edit_ai_image_button")
                                ) {
                                    Text("Edit Selected Image")
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                when (val imgState = imageGenState) {
                                    is ImageGenUiState.Idle -> {
                                        Text(
                                            text = "Prompt likhein aur 'Generate New AI Image' button par tap karein mere dost.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    is ImageGenUiState.Loading -> {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            CircularProgressIndicator()
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Generating image with gemini-nano-banana-2.1 (may take 30–60s)...")
                                        }
                                    }
                                    is ImageGenUiState.Success -> {
                                        Image(
                                            bitmap = imgState.bitmap.asImageBitmap(),
                                            contentDescription = "Generated AI Visual Asset",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(240.dp)
                                                .clip(RoundedCornerShape(16.dp)),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                    is ImageGenUiState.Error -> {
                                        Text(
                                            text = "Error: ${imgState.message}",
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.testTag("image_gen_error_text")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (aiOutput.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_workspace_output_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Chhotu AI Output ($selectedAiModel):",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = aiOutput,
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

private fun createSampleInvoiceBitmap(): Bitmap {
    val bmp = Bitmap.createBitmap(640, 360, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawColor(AndroidColor.parseColor("#0F172A"))

    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#1E293B")
    }
    canvas.drawRoundRect(RectF(24f, 24f, 616f, 336f), 20f, 20f, cardPaint)

    val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#FF9F1C")
        textSize = 26f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    canvas.drawText("CHHOTU AI BUSINESS — TAX INVOICE #INV-2026-88", 48f, 74f, headerPaint)

    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#F8FAFC")
        textSize = 20f
    }
    canvas.drawText("Vendor: Bharat Organic Spices D2C Pvt. Ltd.", 48f, 122f, bodyPaint)
    canvas.drawText("Item 1: Export Grade Saffron & Turmeric Batch — INR 85,000", 48f, 166f, bodyPaint)
    canvas.drawText("Item 2: Cold-Pressed Packaging & Logistics — INR 23,000", 48f, 206f, bodyPaint)
    canvas.drawText("GST (18%): INR 19,440 | Grand Total: INR 1,27,440", 48f, 250f, bodyPaint)

    val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#00F5D4")
        textSize = 18f
    }
    canvas.drawText("Status: Due by 30 Nov 2026 • Verify before bank transfer", 48f, 300f, footerPaint)
    return bmp
}
