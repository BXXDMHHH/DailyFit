package com.dailyfit.app.feature.history
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("历史", style = MaterialTheme.typography.headlineMedium)
        Text("历史页面只提供查询。正式训练历史保存后永久只读，禁止修改和删除。", style = MaterialTheme.typography.bodyLarge)
    }
}
