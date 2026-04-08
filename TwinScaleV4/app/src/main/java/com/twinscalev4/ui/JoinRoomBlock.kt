package com.twinscalev4.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.twinscalev4.data.GrowthMode

@Composable
fun JoinRoomBlock(
    suggestedRoomId: String,
    isLoading: Boolean,
    error: String?,
    onJoin: (roomId: String, name: String, sizeMeters: String, mode: GrowthMode) -> Unit
) {
    var roomId by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("1.70") }
    var mode by remember { mutableStateOf(GrowthMode.BALANCED) }

    LaunchedEffect(suggestedRoomId) {
        if (suggestedRoomId.isNotBlank()) roomId = suggestedRoomId
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Добро пожаловать в TwinScale", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Введите данные для входа в приватную комнату", color = MaterialTheme.colorScheme.onSurfaceVariant)

                OutlinedTextField(value = roomId, onValueChange = { roomId = it }, label = { Text("ID комнаты") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Ваше имя") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = size, onValueChange = { size = it }, label = { Text("Ваш размер в метрах") }, modifier = Modifier.fillMaxWidth())

                ModeSelector(selected = mode, onSelected = { mode = it })

                if (!error.isNullOrBlank()) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = { onJoin(roomId, name, size, mode) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !isLoading
                ) {
                    Text(if (isLoading) "Подключение..." else "Войти в комнату")
                }
            }
        }
    }
}

@Composable
private fun ModeSelector(selected: GrowthMode, onSelected: (GrowthMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Режим изменения размера", style = MaterialTheme.typography.labelLarge)
        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GrowthMode.entries.forEach { mode ->
                Button(
                    onClick = { onSelected(mode) },
                    shape = RoundedCornerShape(14.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = if (selected == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selected == mode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(mode.displayName)
                }
            }
        }
    }
}
