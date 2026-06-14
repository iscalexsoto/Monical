package com.devsoto.monical.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.text.KeyboardOptions
import com.devsoto.monical.data.model.ReceiptItem
import com.devsoto.monical.ui.scan.ScanViewModel
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/**
 * Lets the user review and correct the extracted data before saving it to Firestore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    viewModel: ScanViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val draft = uiState.draft ?: return
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text("Revisa los datos", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = draft.merchant.orEmpty(),
            onValueChange = viewModel::updateMerchant,
            label = { Text("Comercio") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = draft.dateMillis?.let { formatDate(it) } ?: "",
            onValueChange = {},
            label = { Text("Fecha") },
            placeholder = { Text("dd/mm/aaaa") },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                TextButton(onClick = { showDatePicker = true }) { Text("Elegir") }
            },
        )
        Spacer(Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = draft.total?.let { formatAmount(it) } ?: "",
                onValueChange = { viewModel.updateTotal(it.replace(',', '.').toDoubleOrNull()) },
                label = { Text("Total") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(2f),
                singleLine = true,
            )
            Spacer(Modifier.width(12.dp))
            OutlinedTextField(
                value = draft.currency.orEmpty(),
                onValueChange = viewModel::updateCurrency,
                label = { Text("Moneda") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Productos", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = viewModel::addItem) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir producto")
            }
        }

        draft.items.forEachIndexed { index, item ->
            ItemRow(
                item = item,
                onChange = { viewModel.updateItem(index, it) },
                onRemove = { viewModel.removeItem(index) },
            )
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = viewModel::save,
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("Guardar recibo")
            }
        }

        uiState.error?.let { error ->
            Spacer(Modifier.height(12.dp))
            Text(error, color = MaterialTheme.colorScheme.error)
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = draft.dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateDate(pickerState.selectedDateMillis)
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun ItemRow(
    item: ReceiptItem,
    onChange: (ReceiptItem) -> Unit,
    onRemove: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = item.name,
            onValueChange = { onChange(item.copy(name = it)) },
            label = { Text("Producto") },
            modifier = Modifier.weight(2f),
            singleLine = true,
        )
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = item.lineTotal?.let { formatAmount(it) } ?: "",
            onValueChange = { onChange(item.copy(lineTotal = it.replace(',', '.').toDoubleOrNull())) },
            label = { Text("Precio") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Delete, contentDescription = "Eliminar producto")
        }
    }
}

private fun formatDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().format(dateFormatter)

private fun formatAmount(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
