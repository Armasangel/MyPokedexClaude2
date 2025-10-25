package com.uvg.mypokedex.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PokemonMeasurements(weight: Float, height: Float) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Column {
            Text(text = "Peso")
            Text(text = "$weight kg")
        }
        Column {
            Text(text = "Altura")
            Text(text = "$height m")
        }
    }
}
