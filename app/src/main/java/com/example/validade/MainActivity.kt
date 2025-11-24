package com.example.validade

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
    FAVORITES("Favorites", Icons.Default.Favorite),
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
    ) {
        when (currentDestination) {

            AppDestinations.HOME -> {
                ExpiryScreen(
                    modifier = Modifier
                        .fillMaxSize()
                )
            }

            AppDestinations.FAVORITES -> {
                PlaceholderScreen(
                    title = "Favorites",
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

/* ------------ MODELO E FUNÇÕES DE DATA ------------ */

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

/* ------------ TELA HOME (CONTROLE DE VALIDADE) ------------ */

@Composable
fun ExpiryScreen(modifier: Modifier = Modifier) {
    var name by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var items by remember { mutableStateOf(listOf<ExpiryItem>()) }

    // ordena pelos que vencem antes
    val sortedItems = items.sortedBy { parseDateOrMax(it.expiryDate) }

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
                        items = items + ExpiryItem(name, date)
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
                            items = items - item
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/* ------------ CARD DE CADA PRODUTO ------------ */

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

/* ------------ TELAS DAS OUTRAS ABAS ------------ */

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
