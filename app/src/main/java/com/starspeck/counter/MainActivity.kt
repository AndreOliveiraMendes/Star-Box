package com.starspeck.counter

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "counters")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val counters by context.dataStore.data.map { p ->
        (1..5).associate { "star_$it" to (p[intPreferencesKey("star_$it")] ?: 0) } +
        (1..5).associate { "shining_$it" to (p[intPreferencesKey("shining_$it")] ?: 0) }
    }.collectAsState(initial = emptyMap())

    var screen by remember { mutableStateOf("main") }

    MaterialTheme(colorScheme = darkColorScheme()) {
        when (screen) {
            "main" -> MainScreen(counters, context.dataStore, scope) { screen = "stats" }
            "stats" -> StatsScreen(counters, context.dataStore, scope) { screen = "main" }
        }
    }
}

@Composable
fun MainScreen(c: Map<String, Int>, ds: DataStore<Preferences>, scope: CoroutineScope, toStats: () -> Unit) {
    val star = c.filterKeys { it.startsWith("star_") }.values.sum()
    val shining = c.filterKeys { it.startsWith("shining_") }.values.sum()
    val total = star + shining
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Side("Star Speck", Color.Cyan, "star_", c, ds, scope)
            Side("Shining Star", Color.Yellow, "shining_", c, ds, scope)
        }
        Spacer(Modifier.height(40.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Total: $total", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Star: ${if(total>0) "%.1f%%".format(star*100f/total) else "0%"}", color = Color.Cyan)
                Text("Shining: ${if(total>0) "%.1f%%".format(shining*100f/total) else "0%"}", color = Color.Yellow)
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = toStats) {  // ← AQUI FALTAVA FECHAR 
                    Text("Ver estatísticas detalhadas →")
                }
            }
        }
    }
}

@Composable
fun Side(title: String, color: Color, prefix: String, c: Map<String, Int>, ds: DataStore<Preferences>, scope: CoroutineScope) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        (1..5).forEach { v ->
            Button(onClick = { scope.launch { ds.edit { it[intPreferencesKey(prefix+v)] = (it[intPreferencesKey(prefix+v)] ?: 0) + 1 } } },
                colors = ButtonDefaults.buttonColors(containerColor = color), modifier = Modifier.width(80.dp).padding(4.dp)) {
                Text("+$v", fontSize = 20.sp)
            }
        }
        Text("${c.filterKeys { it.startsWith(prefix) }.values.sum()}", color = color, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(c: Map<String, Int>, ds: DataStore<Preferences>, scope: CoroutineScope, back: () -> Unit) {
    val starTotal = c.filterKeys { it.startsWith("star_") }.values.sum()
    val shiningTotal = c.filterKeys { it.startsWith("shining_") }.values.sum()

    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = { Text("Estatísticas") },
            navigationIcon = {
                IconButton(onClick = back) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                }
            }
        )
    }) { p ->
        LazyColumn(
            Modifier
                .padding(p)
                .padding(16.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Total geral: ${starTotal + shiningTotal}",
                            fontSize = 22.sp, fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(24.dp))

                        Text("Star Speck", color = Color.Cyan, fontSize = 20.sp)
                        (1..5).forEach { v ->
                            val n = c["star_$v"] ?: 0
                            val pct = if (starTotal > 0) "%.1f%%".format(n * 100f / starTotal) else "0%"
                            Text("+$v → $n vezes ($pct)")
                        }

                        Spacer(Modifier.height(24.dp))

                        Text("Shining Star Speck", color = Color.Yellow, fontSize = 20.sp)
                        (1..5).forEach { v ->
                            val n = c["shining_$v"] ?: 0
                            val pct = if (shiningTotal > 0) "%.1f%%".format(n * 100f / shiningTotal) else "0%"
                            Text("+$v → $n vezes ($pct)")
                        }
                    }
                }
            }

            // --- BOTÃO DE RESET ---
            item {
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        scope.launch {
                            ds.edit { it.clear() }
                        }
                        back()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Zerar todas as estatísticas", color = Color.White)
                }
            }
        }
    }
}

