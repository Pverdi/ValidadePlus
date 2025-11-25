package com.example.validade

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
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
        // deixa o edge-to-edge desligado por enquanto
        // enableEdgeToEdge()
        setContent {
            ValidadeTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(WindowInsets.systemBars.asPaddingValues())
                ) {
                    ValidadeApp()
                }
            }
        }
    }
}



enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("Início", Icons.Default.Home),
    GRAFICO("Gráfico", Icons.Default.Favorite),
    SOBRE("Sobre", Icons.Default.AccountBox),
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
                Graficos(
                    modifier = Modifier
                        .fillMaxSize()
                )
            }

            AppDestinations.SOBRE -> {
                telaSobre()
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
        modifier = modifier
        .windowInsetsPadding(WindowInsets.systemBars),
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
                .padding(WindowInsets.systemBars.asPaddingValues())
        ) {
            Text("Resumo dos produtos", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            Text("Total cadastrados: $total")
            Text("Produtos vencidos: $contVencidos")
            Text("Produtos em risco (≤14 dias): $produtosRisco")
            Text("Produtos em alerta (15–30 dias): $produtosAlerta")
            Text("Dentro da validade (>30 dias): $naValidade")

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Gráfico de situação",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            GraficoBarrasHorizontais(
                contVencidos = contVencidos,
                produtosRisco = produtosRisco,
                produtosAlerta = produtosAlerta,
                naValidade = naValidade
            )
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
@Composable
fun GraficoBarrasHorizontais(
    contVencidos: Int,
    produtosRisco: Int,
    produtosAlerta: Int,
    naValidade: Int,
    modifier: Modifier = Modifier
) {
    val maxValor = listOf(contVencidos, produtosRisco, produtosAlerta, naValidade).maxOrNull() ?: 1

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Barra("Vencidos", contVencidos, maxValor, Color(0xFFFF5555))
        Barra("Risco (≤14 dias)", produtosRisco, maxValor, Color(0xFFFFAA00))
        Barra("Alerta (15–30 dias)", produtosAlerta, maxValor, Color(0xFFFFCC00))
        Barra("Dentro da validade", naValidade, maxValor, Color(0xFF66CC66))
    }
}

@Composable
fun Barra(
    titulo: String,
    valor: Int,
    maxValor: Int,
    cor: Color
) {
    Column {
        Text(titulo, style = MaterialTheme.typography.bodyMedium)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .background(Color(0xFFE0E0E0), shape = RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = if (maxValor == 0) 0f else valor / maxValor.toFloat())
                    .background(cor, shape = RoundedCornerShape(4.dp))
            )
        }

        Text("$valor itens", style = MaterialTheme.typography.labelSmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun telaSobre(){

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Sobre o aplicativo") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                text = "Objetivo do Aplicativo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Auxiliar no controle de validade de produtos, permitindo identificar itens vencidos, em risco, em atenção ou dentro da validade de forma simples, visual e organizada.",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Funcionalidades Principais",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "• Cadastro e listagem de produtos\n" +
                        "• Classificação automática por data de validade\n" +
                        "• Gráfico com visão geral dos itens\n" +
                        "• Interface intuitiva com navegação inferior",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Tecnologias Utilizadas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "• Android Studio\n" +
                        "• Kotlin\n" +
                        "• Jetpack Compose\n" +
                        "• Material Design 3",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Desenvolvido por",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Penélope Marques Verdi",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Projeto Acadêmico",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Aplicativo desenvolvido para a disciplina “Programação para Dispositivos Móveis em Android”.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}