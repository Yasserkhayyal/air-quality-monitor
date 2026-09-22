package com.yasserkhayal.androidprojects.airqualitymonitor.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSearchScreen(
    onBack: () -> Unit,
    onLocationSelected: (String) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_description_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.search_field_label)) },
                singleLine = true
            )
            if (query.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.search_progress))
                }
            } else {
                Text(stringResource(R.string.search_hint))
            }
            LocationResultItem(
                visible = query.isNotBlank(),
                onClick = { onLocationSelected(query) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationSearchScreenPreview() {
    MaterialTheme {
        LocationSearchScreen(
            onBack = {},
            onLocationSelected = {}
        )
    }
}

@Composable
private fun LocationResultItem(visible: Boolean, onClick: () -> Unit) {
    if (!visible) return
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.search_result_title))
            Text(stringResource(R.string.search_result_subtitle), style = MaterialTheme.typography.bodySmall)
        }
    }

}

@Preview(showBackground = true)
@Composable
private fun LocationResultItemPreview() {
    MaterialTheme {
        LocationResultItem(visible = true, onClick = {})
    }
}
