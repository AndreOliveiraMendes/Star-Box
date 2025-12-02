package com.starspeck.counter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.core.content.FileProvider
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "counters")

class MainActivity : ComponentActivity() {

    private lateinit var csvPicker: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        csvPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                lifecycleScope.launch {
                    val imported = importCsv(this@MainActivity, uri)
                    writeImportedData(imported)
                    Toast.makeText(this@MainActivity, "Importação concluída", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this@MainActivity, "Nenhum arquivo selecionado", Toast.LENGTH_SHORT).show()
            }
        }

        setContent { App() }
    }

    fun pickCsvFile() {
        csvPicker.launch("text/*")
    }

    fun exportCsv() {
        lifecycleScope.launch {
            val prefs = dataStore.data.first()
            val counters = mutableMapOf<String, Int>()
            for (i in 1..5) {
                counters["star_$i"] = prefs[intPreferencesKey("star_$i")] ?: 0
                counters["shining_$i"] = prefs[intPreferencesKey("shining_$i")] ?: 0
            }

            val file = ExportImport.exportCsv(this@MainActivity, counters)
            shareFile(file)
        }
    }

    private fun shareFile(file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(intent, "Exportar CSV"))
    }

    private suspend fun importCsv(context: Context, uri: Uri): Map<String, Map<Int, Int>> {
        val result = mutableMapOf<String, MutableMap<Int, Int>>()

        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
            val first = reader.readLine()
            val lines = mutableListOf<String>()
            if (first != null && !first.contains("tipo;"))
                lines.add(first)

            reader.forEachLine { lines.add(it) }

            for (line in lines) {
                val parts = line.split(";")
                if (parts.size == 3) {
                    val tipo = parts[0]
                    val qtd = parts[1].toIntOrNull() ?: continue
                    val total = parts[2].toIntOrNull() ?: continue

                    val mapa = result.getOrPut(tipo) { mutableMapOf() }
                    mapa[qtd] = total
                }
            }
        }

        return result
    }

    private fun writeImportedData(map: Map<String, Map<Int, Int>>) {
        lifecycleScope.launch {
            dataStore.edit { prefs ->
                prefs.clear()
                map.forEach { (tipo, entries) ->
                    val prefix = if (tipo.trim().equals("Star Speck", ignoreCase = true))
                        "star_"
                    else
                        "shining_"

                    entries.forEach { (qtd, total) ->
                        prefs[intPreferencesKey(prefix + qtd)] = total
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val counters by context.dataStore.data
        .map { p ->
            buildMap<String, Int> {
                for (i in 1..5) put("star_$i", p[intPreferencesKey("star_$i")] ?: 0)
                for (i in 1..5) put("shining_$i", p[intPreferencesKey("shining_$i")] ?: 0)
            }
        }
        .collectAsState(initial = emptyMap())

    var screen by remember { mutableStateOf("main") }

    MaterialTheme(colorScheme = darkColorScheme()) {
        when (screen) {
            "main" -> MainScreen(counters, context.dataStore, scope) {
                screen = "stats"
            }

            "stats" -> StatsScreen(
                counters,
                context.dataStore,
                scope,
                back = { screen = "main" },
                toManualImport = { screen = "manual_import" }
            )

            "manual_import" -> ManualImportScreen(
                counters,
                context.dataStore,
                scope,
                onBack = { screen = "main" }
                )
            }

    }
}

@Composable
fun MainScreen(c: Map<String, Int>, ds: DataStore<Preferences>, scope: CoroutineScope, toStats: () -> Unit) {
    val star = c.filterKeys { it.startsWith("star_") }.values.sum()
    val shining = c.filterKeys { it.startsWith("shining_") }.values.sum()
    val total = star + shining

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                TextButton(onClick = toStats) { Text("Ver estatísticas detalhadas →") }
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
            Button(
                onClick = {
                    scope.launch {
                        ds.edit {
                            it[intPreferencesKey(prefix + v)] =
                                (it[intPreferencesKey(prefix + v)] ?: 0) + 1
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = color),
                modifier = Modifier
                    .width(80.dp)
                    .padding(4.dp)
            ) {
                Text("★ $v")
            }
        }
    }
}

@Composable
fun ManualImportScreen(
    current: Map<String, Int>,
    ds: DataStore<Preferences>,
    scope: CoroutineScope,
    onBack: () -> Unit
) {
    val fields = remember {
        mutableStateMapOf<String, String>().apply {
            for (i in 1..5) {
                this["star_$i"] = current["star_$i"]?.toString() ?: "0"
                this["shining_$i"] = current["shining_$i"]?.toString() ?: "0"
            }
        }
    }

    val scroll = rememberScrollState()

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scroll)   // 👈 rolagem ativada aqui
    ) {
        Text("Importar manualmente", fontSize = 22.sp)

        Spacer(Modifier.height(16.dp))

        // STAR
        Text("Star Speck", color = Color.Cyan, fontWeight = FontWeight.Bold)
        for (i in 1..5) {
            OutlinedTextField(
                value = fields["star_$i"] ?: "",
                onValueChange = { fields["star_$i"] = it.filter { ch -> ch.isDigit() } },
                label = { Text("star_$i") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        // SHINING
        Text("Shining Star Speck", color = Color.Yellow, fontWeight = FontWeight.Bold)
        for (i in 1..5) {
            OutlinedTextField(
                value = fields["shining_$i"] ?: "",
                onValueChange = { fields["shining_$i"] = it.filter { ch -> ch.isDigit() } },
                label = { Text("shining_$i") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = onBack) {
                Text("Cancelar")
            }

            Button(onClick = {
                scope.launch {
                    ds.edit { prefs ->
                        for ((k, v) in fields) {
                            prefs[intPreferencesKey(k)] = v.toIntOrNull() ?: 0
                        }
                    }
                }
                onBack()
            }) {
                Text("Importar")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    c: Map<String, Int>,
    ds: DataStore<Preferences>,
    scope: CoroutineScope,
    back: () -> Unit,
    toManualImport: () -> Unit
) {
    val context = LocalContext.current

    val starTotal = c.filterKeys { it.startsWith("star_") }.values.sum()
    val shiningTotal = c.filterKeys { it.startsWith("shining_") }.values.sum()
    val grandTotal = starTotal + shiningTotal

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Estatísticas") },
                navigationIcon = {
                    IconButton(onClick = back) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {

                        Text(
                            "Total geral: $grandTotal",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(24.dp))

                        StatsSection(
                            title = "Star Speck",
                            titleColor = Color.Cyan,
                            prefix = "star",
                            data = c,
                            categoryTotal = starTotal,
                            grandTotal = grandTotal
                        )

                        Spacer(Modifier.height(24.dp))

                        StatsSection(
                            title = "Shining Star Speck",
                            titleColor = Color.Yellow,
                            prefix = "shining",
                            data = c,
                            categoryTotal = shiningTotal,
                            grandTotal = grandTotal
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { (context as? MainActivity)?.exportCsv() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Exportar CSV")
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { (context as? MainActivity)?.pickCsvFile() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Importar CSV")
                }
            }

            item {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = toManualImport,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Importar manualmente valores")
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        scope.launch { ds.edit { it.clear() } }
                        back()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Zerar todas as estatísticas", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun StatsSection(
    title: String,
    titleColor: Color,
    prefix: String,
    data: Map<String, Int>,
    categoryTotal: Int,
    grandTotal: Int
) {
    Text(title, color = titleColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)

    for (i in 1..5) {
        val key = "${prefix}_$i"
        val value = data[key] ?: 0

        val pctCategory =
            if (categoryTotal > 0) "%.1f%%".format(value * 100f / categoryTotal)
            else "0%"

        val pctTotal =
            if (grandTotal > 0) "%.1f%%".format(value * 100f / grandTotal)
            else "0%"

        Text("+$i → $value vezes ($pctCategory) [$pctTotal]")
    }
}

