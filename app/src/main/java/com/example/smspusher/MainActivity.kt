package com.example.smspusher

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.smspusher.ui.theme.SMSPusherTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.derivedStateOf

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.res.stringResource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

class MainActivity : ComponentActivity() {
    private val client = OkHttpClient()
    private val smsPermissions = listOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS  // Add this if your app needs to send SMS
    )

    private var showPermissionDialog by mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "SMS Permissions granted", Toast.LENGTH_SHORT).show()
            LogManager.addLog(this, "SMS Permissions granted")
        } else {
            val deniedPermissions = permissions.filterValues { !it }.keys.joinToString(", ")
            val message = "SMS Permissions denied: $deniedPermissions"
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            LogManager.addLog(this, message)
            // Optionally, explain why permissions are needed and prompt to grant again
            showPermissionExplanationDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SMSPusherTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainContent(modifier = Modifier.padding(innerPadding))
                }
            }
        }

        // Check and request permissions
        checkAndRequestPermissions()

        // Load logs
        LogManager.loadLogs(this)
    }

    companion object {
        const val PREFS_NAME = "SMSPusherPrefs"
        const val ENDPOINT_URL_KEY = "endpointUrl"
        const val AUTH_TOKEN_KEY = "authToken"
    }

    private fun checkAndRequestPermissions() {
        val permissionStatus = smsPermissions.associateWith { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }

        val deniedPermissions = permissionStatus.filterValues { !it }

        LogManager.addLog(this, "Permission status:")
        permissionStatus.forEach { (permission, isGranted) ->
            LogManager.addLog(this, "$permission: ${if (isGranted) "Granted" else "Denied"}")
        }

        if (deniedPermissions.isNotEmpty()) {
            LogManager.addLog(this, "Permissions not granted: ${deniedPermissions.keys.joinToString(", ")}")
            // Always request permissions first, regardless of whether we should show rationale
            LogManager.addLog(this, "Requesting permissions")
            requestPermissionLauncher.launch(deniedPermissions.keys.toTypedArray())
        } else {
            // All permissions are granted
            val message = "All SMS Permissions are granted"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            LogManager.addLog(this, message)
        }
    }

    @Composable
    fun PermissionDialog() {
        if (showPermissionDialog) {
            AlertDialog(
                onDismissRequest = { showPermissionDialog = false },
                title = { Text("Permission Required") },
                text = { Text("SMS read permission is required for this app to function. Please grant it in the app settings.") },
                confirmButton = {
                    TextButton(onClick = {
                        showPermissionDialog = false
                        openAppSettings()
                    }) {
                        Text("Open Settings")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showPermissionDialog = false
                        checkAndRequestPermissions() // Add this line to request permissions again
                    }) {
                        Text("Request Again")
                    }
                }
            )
        }
    }

    private fun openAppSettings() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            startActivity(this)
        }
    }

    private fun showPermissionExplanationDialog() {
        // Implement a dialog to explain why permissions are needed
        // and provide an option to request permissions again
    }

    suspend fun testConnection(endpointUrl: String, authToken: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = endpointUrl.removeSuffix("/")
                val testUrl = "$baseUrl/test-connection"
                val request = Request.Builder()
                    .url(testUrl)
                    .post("".toRequestBody("application/json".toMediaType()))
                    .header("Authorization", "Bearer $authToken")
                    .build()

                val response = client.newCall(request).execute()
                val isSuccessful = response.isSuccessful && response.code == 200
                
                if (isSuccessful) {
                    LogManager.addLog(this@MainActivity, "Test connection to $testUrl successful")
                } else {
                    LogManager.addLog(this@MainActivity, "Test connection to $testUrl failed. Response: ${response.code}")
                }
                
                isSuccessful
            } catch (e: IOException) {
                LogManager.addLog(this@MainActivity, "Test connection to $endpointUrl failed. Error: ${e.message}")
                false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE) }

    val (endpointUrl, setEndpointUrl) = remember { 
        mutableStateOf(sharedPreferences.getString(MainActivity.ENDPOINT_URL_KEY, "https://pusherworker.example.com") ?: "https://pusherworker.example.com")
    }
    val (authToken, setAuthToken) = remember { 
        mutableStateOf(sharedPreferences.getString(MainActivity.AUTH_TOKEN_KEY, "") ?: "")
    }
    val (isTestingConnection, setIsTestingConnection) = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val logs by LogManager.logs.collectAsState()
    val listState = rememberLazyListState()
    val hasLogs = remember { derivedStateOf { logs.isNotEmpty() } }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = stringResource(R.string.app_description),
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = endpointUrl,
            onValueChange = { 
                setEndpointUrl(it)
                sharedPreferences.edit()
                    .putString(MainActivity.ENDPOINT_URL_KEY, it)
                    .apply()
            },
            label = { Text(stringResource(R.string.endpoint_url)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        OutlinedTextField(
            value = authToken,
            onValueChange = { 
                setAuthToken(it)
                sharedPreferences.edit()
                    .putString(MainActivity.AUTH_TOKEN_KEY, it)
                    .apply()
            },
            label = { Text(stringResource(R.string.authorization_token)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        Button(
            onClick = {
                coroutineScope.launch {
                    setIsTestingConnection(true)
                    val isSuccessful = (context as MainActivity).testConnection(endpointUrl, authToken)
                    setIsTestingConnection(false)
                    val message = if (isSuccessful) {
                        context.getString(R.string.connection_successful)
                    } else {
                        context.getString(R.string.connection_failed)
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    LogManager.addLog(context, "Test connection result: $message")
                }
            },
            enabled = !isTestingConnection && endpointUrl.isNotBlank() && authToken.isNotBlank()
        ) {
            Text(stringResource(R.string.test_connection))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.logs),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            if (hasLogs.value) {
                IconButton(
                    onClick = { LogManager.clearLogs(context) },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.clear_logs)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 12.dp) // Add padding for the scrollbar
                    .drawVerticalScrollbar(listState)
            ) {
                items(logs) { log ->
                    Text(
                        text = log,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
    (LocalContext.current as? MainActivity)?.PermissionDialog()
}

fun Modifier.drawVerticalScrollbar(
    state: LazyListState,
    width: Dp = 8.dp,
    color: Color = Color.LightGray,
    alpha: Float = 0.5f
) = drawWithContent {
    drawContent()

    val firstVisibleElementIndex = state.firstVisibleItemIndex
    val needDrawScrollbar = state.isScrollInProgress || firstVisibleElementIndex > 0

    // Draw scrollbar if scrolling or if the first item is not visible
    if (needDrawScrollbar) {
        val elementHeight = this.size.height / state.layoutInfo.totalItemsCount
        val scrollbarOffsetY = firstVisibleElementIndex * elementHeight
        val scrollbarHeight = state.layoutInfo.visibleItemsInfo.size * elementHeight

        drawRect(
            color = color,
            topLeft = Offset(this.size.width - width.toPx(), scrollbarOffsetY),
            size = Size(width.toPx(), scrollbarHeight),
            alpha = alpha
        )
    }
}
