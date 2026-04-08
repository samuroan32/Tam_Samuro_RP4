package com.twinscalev4

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.twinscalev4.notification.ChatPresenceManager
import com.twinscalev4.ui.ChatScreen
import com.twinscalev4.ui.theme.TwinScaleTheme
import com.twinscalev4.viewmodel.ChatViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ChatViewModel by viewModels()

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                ChatPresenceManager.isAppInForeground = true
            }

            override fun onStop(owner: LifecycleOwner) {
                ChatPresenceManager.isAppInForeground = false
            }
        })

        requestNotificationPermissionIfNeeded()
        handleRoomFromIntent(intent)

        setContent {
            TwinScaleTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ChatScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.syncLatestToken()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleRoomFromIntent(intent)
    }

    private fun handleRoomFromIntent(intent: Intent?) {
        val roomId = intent?.getStringExtra("roomId")
        viewModel.setSuggestedRoom(roomId)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
