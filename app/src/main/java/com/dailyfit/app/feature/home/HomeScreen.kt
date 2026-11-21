package com.dailyfit.app.feature.home
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@Composable
fun HomeScreen(state: HomeUiState, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(state.title, style = MaterialTheme.typography.headlineMedium)
        Text(state.subtitle, style = MaterialTheme.typography.bodyLarge)
        FoundationCard("今日习惯", "下一阶段接入固定习惯和每日打卡")
        FoundationCard("今日任务", "后续接入仅属于创建日期的临时任务")
        FoundationCard("今日训练", "后续接入每周计划、训练执行和自动保存")
    }
}
@Composable
private fun FoundationCard(title: String, description: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
