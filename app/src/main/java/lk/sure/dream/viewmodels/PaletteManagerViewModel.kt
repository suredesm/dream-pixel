package lk.sure.dream.viewmodels

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.sure.dream.data.entries.ColorPalette
import lk.sure.dream.data.repos.PaletteRepository
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PaletteManagerViewModel @Inject constructor(
    private val repo: PaletteRepository,
) : ViewModel() {
    private val _palettes = MutableStateFlow(emptyList<ColorPalette>())
    val palettes = _palettes.asStateFlow()

    init {
        fetchAllPalettes()
    }

    private fun fetchAllPalettes() {
        viewModelScope.launch {
            _palettes.update {
                repo.getAllPalettes()
            }
        }
    }

    fun createPalette(name: String, colors: List<Color>) {
        viewModelScope.launch {
            repo.insert(
                ColorPalette(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    colors = colors.map { it.toArgb() }
                )
            )
            fetchAllPalettes()
        }
    }

    fun duplicatePalette(palette: ColorPalette) {
        viewModelScope.launch {
            val paletteNames = palettes.value.map { it.name }
            var name = palette.name
            var count = 1

            while (paletteNames.contains("$name $count")) {
                count++
            }
            name = "$name $count"
            repo.insert(
                ColorPalette(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    colors = palette.colors
                )
            )
            fetchAllPalettes()
        }
    }

    fun updatePaletteColors(palette: ColorPalette, colors: List<Color>) {
        viewModelScope.launch {
            repo.update(palette.copy(colors = colors.map { it.toArgb() }))
            fetchAllPalettes()
        }
    }

    fun delete(palette: ColorPalette) {
        viewModelScope.launch {
            repo.delete(palette.id)
            fetchAllPalettes()
        }
    }
}