package com.twinscalev4.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.twinscalev4.data.ChatMessage
import com.twinscalev4.data.GrowthMode
import com.twinscalev4.domain.SizeFormatter
import com.twinscalev4.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val state by viewModel.state.collectAsState()

    DisposableEffect(state.isJoined) {
        viewModel.setChatVisible(state.isJoined)
        onDispose { viewModel.setChatVisible(false) }
    }

    if (!state.isJoined) {
        JoinRoomBlock(
            isLoading = state.isLoading,
            error = state.error,
            onJoin = viewModel::joinRoom
        )
        return
    }

    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        TopChatBar(
            partnerName = state.partner?.name?.ifBlank { "Партнёр" } ?: "Ожидаем партнёра",
            isOnline = state.partner?.online == true
        )

        SizeInfoBlock(
            selfSize = SizeFormatter.formatMeters(state.selfSizeRaw),
            partnerSize = SizeFormatter.formatMeters(state.partner?.sizeMetersRaw ?: "1"),
            ratio = SizeFormatter.ratioText(state.selfSizeRaw, state.partner?.sizeMetersRaw ?: "1")
        )

        ControlPanel(
            selectedMode = state.selfMode,
            onModeSelected = viewModel::switchMode,
            onGrow = { viewModel.applyGrowth(true) },
            onShrink = { viewModel.applyGrowth(false) }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

        if (state.messages.isEmpty()) {
            EmptyChatState()
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.messages, key = { it.messageId }) { message ->
                    MessageBubble(
                        message = message,
                        isSelf = message.senderId == state.selfId
                    )
                }
            }
        }

        MessageComposer(
            value = state.draftMessage,
            onValueChange = viewModel::updateDraft,
            onSend = viewModel::sendMessage
        )
    }
}

@Composable
private fun TopChatBar(partnerName: String, isOnline: Boolean) {
    Surface(
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(partnerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    if (isOnline) "В сети" else "Не в сети",
                    color = if (isOnline) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isOnline) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline)
            )
        }
    }
}

@Composable
private fun SizeInfoBlock(selfSize: String, partnerSize: String, ratio: String) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Ваш размер: $selfSize", style = MaterialTheme.typography.titleSmall)
            Text("Размер партнёра: $partnerSize", style = MaterialTheme.typography.titleSmall)
            Text(ratio, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ControlPanel(
    selectedMode: GrowthMode,
    onModeSelected: (GrowthMode) -> Unit,
    onGrow: () -> Unit,
    onShrink: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GrowthMode.entries.forEach { mode ->
                    AssistChip(
                        onClick = { onModeSelected(mode) },
                        label = { Text(mode.displayName) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (mode == selectedMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            labelColor = if (mode == selectedMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onGrow,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Увеличить", tint = MaterialTheme.colorScheme.onPrimary)
                }
                IconButton(
                    onClick = onShrink,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Rounded.Remove, contentDescription = "Уменьшить", tint = MaterialTheme.colorScheme.onSecondary)
                }
            }
        }
    }
}

@Composable
private fun EmptyChatState() {
    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
        Text(
            "Пока нет сообщений. Начните диалог ✨",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MessageBubble(message: ChatMessage, isSelf: Boolean) {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isSelf) 18.dp else 4.dp,
                bottomEnd = if (isSelf) 4.dp else 18.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelf) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
            )
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                color = if (isSelf) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            formatter.format(Date(message.timestamp)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MessageComposer(value: String, onValueChange: (String) -> Unit, onSend: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Введите сообщение") },
            shape = RoundedCornerShape(18.dp),
            maxLines = 4
        )
        IconButton(
            onClick = onSend,
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        ) {
            Icon(
                Icons.Rounded.Send,
                contentDescription = "Отправить",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
