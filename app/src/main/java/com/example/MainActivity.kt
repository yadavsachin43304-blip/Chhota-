package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.agent.ChhotuTaskPlannerAndAgent
import com.example.agent.GeminiOrchestrator
import com.example.automation.DeviceAutomationEngine
import com.example.data.local.ChhotuLocalDatabase
import com.example.data.local.LocalOfflineRepository
import com.example.data.remote.ChhotuCloudRepository
import com.example.security.OwnerAuthManager
import com.example.security.configureAppCheck
import com.example.ui.AuthGateScreen
import com.example.ui.BusinessAndCodingScreen
import com.example.ui.ChhotuViewModel
import com.example.ui.FilesAndStorageScreen
import com.example.ui.HomeVoiceScreen
import com.example.ui.SecurityAndSettingsScreen
import com.example.ui.TasksAndPlannerScreen
import com.example.ui.theme.ChhotuAITheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1 (MANDATORY): Configure Firebase App Check with launch intent before any UI or Firebase calls
        configureAppCheck(this, intent)

        // STEP 2: Enable Edge-to-Edge layout
        enableEdgeToEdge()

        // STEP 3: Render Root Composable with dynamic Dark / Light / System Theme support
        setContent {
            val context = LocalContext.current
            val authManager = remember { OwnerAuthManager(context) }
            var appThemeMode by rememberSaveable { mutableStateOf(authManager.getThemeMode()) }
            val systemDark = isSystemInDarkTheme()
            val isDarkActive = when (appThemeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> systemDark
            }

            ChhotuAITheme(darkTheme = isDarkActive) {
                AppNavigation(
                    currentThemeMode = appThemeMode,
                    onThemeModeChanged = { newMode ->
                        authManager.setThemeMode(newMode)
                        appThemeMode = newMode
                    }
                )
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

enum class ChhotuNavTab(
    val route: String,
    val labelHindi: String,
    val icon: ImageVector,
    val testTagId: String
) {
    HOME("home", "Home", Icons.Filled.Mic, "nav_tab_home"),
    TASKS("tasks", "Tasks", Icons.Filled.Assignment, "nav_tab_tasks"),
    BUSINESS("business", "Business", Icons.Filled.BusinessCenter, "nav_tab_business"),
    FILES("files", "Vault Storage", Icons.Filled.FolderSpecial, "nav_tab_files"),
    ACTIVITY("activity", "Security", Icons.Filled.History, "nav_tab_activity"),
    SETTINGS("settings", "Settings", Icons.Filled.Settings, "nav_tab_settings")
}

@Composable
fun AppNavigation(
    auth: FirebaseAuth = Firebase.auth,
    currentThemeMode: String = "DARK",
    onThemeModeChanged: (String) -> Unit = {}
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthGateScreen(
            onAuthenticated = {},
            currentThemeMode = currentThemeMode,
            onToggleThemeMode = {
                val next = if (currentThemeMode == "LIGHT") "DARK" else "LIGHT"
                onThemeModeChanged(next)
            }
        )
    } else {
        ChhotuMainShell(
            currentUserId = user.uid,
            currentThemeMode = currentThemeMode,
            onThemeModeChanged = onThemeModeChanged
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChhotuMainShell(
    currentUserId: String,
    currentThemeMode: String = "DARK",
    onThemeModeChanged: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: ChhotuViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val firestore = FirebaseFirestore.getInstance(databaseId)
                val cloudRepo = ChhotuCloudRepository(firestore)
                val roomDb = ChhotuLocalDatabase.getInstance(app)
                val offlineRepo = LocalOfflineRepository(roomDb.offlineDao())
                val authManager = OwnerAuthManager(app)
                val automationEngine = DeviceAutomationEngine(app)
                val geminiOrchestrator = GeminiOrchestrator()
                val capabilities = ChhotuTaskPlannerAndAgent(authManager).evaluateDeviceOfflineCapabilities(app)

                ChhotuViewModel(
                    cloudRepository = cloudRepo,
                    offlineRepository = offlineRepo,
                    authManager = authManager,
                    automationEngine = automationEngine,
                    geminiOrchestrator = geminiOrchestrator,
                    currentUserId = currentUserId,
                    offlineCapabilities = capabilities
                )
            }
        }
    )

    var currentTab by rememberSaveable { mutableStateOf(ChhotuNavTab.HOME) }
    val pendingApproval by viewModel.pendingApproval.collectAsStateWithLifecycle()
    var globalPinInput by remember { mutableStateOf("1234") }

    // BackHandler returns to HOME from any secondary tab
    if (currentTab != ChhotuNavTab.HOME) {
        BackHandler {
            currentTab = ChhotuNavTab.HOME
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "CHHOTU AI • Mere Dost",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Respectful Voice-First Personal AI Agent",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    },
                    actions = {
                        val isLight = currentThemeMode == "LIGHT"
                        AssistChip(
                            onClick = {
                                val nextMode = if (isLight) "DARK" else "LIGHT"
                                viewModel.updateThemeMode(nextMode)
                                onThemeModeChanged(nextMode)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isLight) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                    contentDescription = "Toggle Dark or Light Mode",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            label = {
                                Text(if (isLight) "Light Mode" else "Dark Mode")
                            },
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .testTag("top_bar_theme_toggle_button")
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            bottomBar = {
                if (!isExpandedScreen) {
                    NavigationBar {
                        ChhotuNavTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = { Icon(tab.icon, contentDescription = tab.labelHindi) },
                                label = { Text(tab.labelHindi) },
                                modifier = Modifier.testTag(tab.testTagId)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    NavigationRail {
                        ChhotuNavTab.entries.forEach { tab ->
                            NavigationRailItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = { Icon(tab.icon, contentDescription = tab.labelHindi) },
                                label = { Text(tab.labelHindi) },
                                modifier = Modifier.testTag(tab.testTagId)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    // Global Approval Banner when triggered from non-Home tabs (Tasks, Business, Files)
                    AnimatedVisibility(visible = pendingApproval != null && currentTab != ChhotuNavTab.HOME) {
                        pendingApproval?.let { proposal ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .testTag("global_approval_banner_card"),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.Warning,
                                            contentDescription = "Approval Needed",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Owner Approval Required: ${proposal.exactActionApproved}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = proposal.whatWillHappenHindi,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = globalPinInput,
                                        onValueChange = { globalPinInput = it },
                                        label = { Text("Owner Security PIN (Default: 1234)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                val pinOk = viewModel.authManager.verifyFallbackPin(globalPinInput)
                                                viewModel.approvePendingProposal(pinOrBiometricVerified = pinOk)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .testTag("global_approve_pin_button")
                                        ) {
                                            Icon(Icons.Filled.CheckCircle, contentDescription = "Approve PIN", modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Approve (PIN)")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                val act = context as? Activity
                                                if (act != null) {
                                                    viewModel.authManager.triggerPlatformBiometricPrompt(
                                                        activity = act,
                                                        onVerified = { viewModel.approvePendingProposal(true) },
                                                        onFallbackOrError = {
                                                            val ok = viewModel.authManager.verifyFallbackPin(globalPinInput)
                                                            viewModel.approvePendingProposal(ok)
                                                        }
                                                    )
                                                } else {
                                                    val ok = viewModel.authManager.verifyFallbackPin(globalPinInput)
                                                    viewModel.approvePendingProposal(ok)
                                                }
                                            },
                                            modifier = Modifier.height(48.dp)
                                        ) {
                                            Icon(Icons.Filled.Fingerprint, contentDescription = "Biometric", modifier = Modifier.size(18.dp))
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.rejectPendingProposal() },
                                            modifier = Modifier
                                                .height(48.dp)
                                                .testTag("global_reject_button")
                                        ) {
                                            Icon(Icons.Filled.Close, contentDescription = "Cancel", modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Cancel")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        when (currentTab) {
                            ChhotuNavTab.HOME -> HomeVoiceScreen(viewModel = viewModel)
                            ChhotuNavTab.TASKS -> TasksAndPlannerScreen(viewModel = viewModel)
                            ChhotuNavTab.BUSINESS -> BusinessAndCodingScreen(viewModel = viewModel)
                            ChhotuNavTab.FILES -> FilesAndStorageScreen(viewModel = viewModel)
                            ChhotuNavTab.ACTIVITY -> SecurityAndSettingsScreen(
                                viewModel = viewModel,
                                initialTab = "ACTIVITY",
                                currentThemeMode = currentThemeMode,
                                onThemeModeChanged = onThemeModeChanged,
                                onSignedOut = {}
                            )
                            ChhotuNavTab.SETTINGS -> SecurityAndSettingsScreen(
                                viewModel = viewModel,
                                initialTab = "SETTINGS",
                                currentThemeMode = currentThemeMode,
                                onThemeModeChanged = onThemeModeChanged,
                                onSignedOut = {}
                            )
                        }
                    }
                }
            }
        }
    }
}
