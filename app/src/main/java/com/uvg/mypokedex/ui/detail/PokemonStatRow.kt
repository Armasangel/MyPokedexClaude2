package com.uvg.mypokedex.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.uvg.mypokedex.data.model.PokemonStat

@Composable
fun PokemonStatRow(stat: PokemonStat) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(stat.name)
        Spacer(modifier = Modifier.width(8.dp))
        LinearProgressIndicator(
            progress = { stat.value.toFloat() / stat.maxValue.toFloat() },
            modifier = Modifier.fillMaxWidth(0.7f).height(8.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(stat.value.toString())
    }
}
