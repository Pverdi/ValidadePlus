package com.example.validade

import java.time.temporal.ChronoUnit
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.example.validade.ui.theme.ValidadeTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

// ---------------- DATASTORE --------------------

val Context.expiryDataStore by preferencesDataStore(name = "expiry_prefs")

object ExpiryRepository {

    private val ITEMS_KEY = stringSetPreferencesKey("expiry_items")

    private fun itemToString(item: ExpiryItem): String =
        "${item.name}||${item.expiryDate}"

    private fun stringToItem(value: String): ExpiryItem {
        val parts = value.split("||")
        return ExpiryItem(
            name = parts.getOrNull(0) ?: "",
            expiryDate = parts.getOrNull(1) ?: ""
        )
    }

    fun getItems(context: Context): Flow<List<ExpiryItem>> {
        return context.expiryDataStore.data.map { prefs: Preferences ->
            val set = prefs[ITEMS_KEY] ?: emptySet()
            set.map { stringToItem(it) }
        }
    }

    suspend fun saveItems(context: Context, items: List<ExpiryItem>) {
        context.expiryDataStore.edit { prefs ->
            prefs[ITEMS_KEY] = items.map { itemToString(it) }.toSet()
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ValidadeTheme {
                ValidadeApp()
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Home", Icons.Default.Home),
    GRAFICO("Gráfico", Icons.Default.Favorite),
    PROFILE("Profile", Icons.Default.AccountBox),
}

@PreviewScreenSizes
@Composable
fun ValidadeApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    )
    {
        when (currentDestination) {

            AppDestinations.HOME -> {
                ExpiryScreen(
                    modifier = Modifier
                        .fillMaxSize()
                )
            }

            AppDestinations.GRAFICO -> {
                PlaceholderScreen(
                    title = "Grafico",
                    modifier = Modifier
                        .fillMaxSize()
                )
            }

            AppDestinations.PROFILE -> {
                PlaceholderScreen(
                    title = "Profile",
                    modifier = Modifier
                        .fillMaxSize()
                )
            }
        }
    }
}

/* Função data */

data class ExpiryItem(
    val name: String,
    val expiryDate: String // dd/MM/yyyy
)

private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

fun parseDateOrMax(dateStr: String): LocalDate {
    return try {
        LocalDate.parse(dateStr, dateFormatter)
    } catch (e: DateTimeParseException) {
        LocalDate.MAX // se a data estiver ruim, joga pro final da lista
    }
}

/* Tela inicial */

@Composable
fun ExpiryScreen(modifier: Modifier = Modifier) {
    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // lê os itens salvos
    val itemsFromStore by ExpiryRepository
        .getItems(context)
        .collectAsState(initial = emptyList())

    // ordena
    val sortedItems = itemsFromStore.sortedBy { parseDateOrMax(it.expiryDate) }

    Surface(
        modifier = modifier,
        color = Color(0xFFF5F5F5)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            Text(
                text = "Controle de Validade",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Produto") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Data de validade (ex: 10/12/2025)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (name.isNotBlank() && date.isNotBlank()) {
                        val newList = itemsFromStore + ExpiryItem(name, date)
                        scope.launch {
                            ExpiryRepository.saveItems(context, newList)
                        }
                        name = ""
                        date = ""
                    }
                }
            ) {
                Text("Adicionar")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Itens cadastrados",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(sortedItems) { item ->
                    ExpiryCard(
                        item = item,
                        onDelete = {
                            val newList = itemsFromStore - item
                            scope.launch {
                                ExpiryRepository.saveItems(context, newList)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun Graficos(modifier: Modifier = Modifier) {

    val context = LocalContext.current

    // p/ ler os itens salvos
    val items by ExpiryRepository
        .getItems(context)
        .collectAsState(initial = emptyList())

    val diaAtual = LocalDate.now()

    // variáveis do gráfico
    var total = 0
    var contVencidos = 0    // produtos vencidos
    var produtosRisco = 0   // em risco de vencer 2 semanas
    var produtosAlerta = 0  // atenção em risco em 15 a 30 dias
    var naValidade = 0      // dentro da validade

    // total lógica
    total = items.size

    // classificar cada item
    items.forEach { item ->
        val expiry = parseDateOrMax(item.expiryDate)
        val daysUntil = ChronoUnit.DAYS.between(diaAtual, expiry)

        when {
            // Vencidos
            daysUntil < 0 -> contVencidos++

            // Em risco (até 14 dias)
            daysUntil in 0..14 -> produtosRisco++

            // Atenção (15 a 30 dias)
            daysUntil in 15..30 -> produtosAlerta++

            // Dentro da validade (>30 dias)
            else -> naValidade++
        }
    }

    // ⬆️ até aqui é só LÓGICA
    // ⬇️ daqui pra baixo é a UI (Surface + Column), FORA do forEach/when

    Surface(
        modifier = modifier,
        color = Color(0xFFF5F5F5)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Resumo dos produtos", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            Text("Total cadastrados: $total")
            Text("Produtos vencidos: $contVencidos")
            Text("Produtos em risco (≤14 dias): $produtosRisco")
            Text("Produtos em alerta (15–30 dias): $produtosAlerta")
            Text("Dentro da validade (>30 dias): $naValidade")
        }
    }
}

/* Produtos */

@Composable
fun ExpiryCard(
    item: ExpiryItem,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Validade: ${item.expiryDate}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(onClick = onDelete) {
                Text("Apagar")
            }
        }
    }
}

/*  outra tela  */

@Composable
fun PlaceholderScreen(title: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.Gray
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(text = "$title (em breve)")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExpiryScreenPreview() {
    ValidadeTheme {
        ExpiryScreen()
    }
}
