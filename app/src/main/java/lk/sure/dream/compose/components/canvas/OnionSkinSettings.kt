package lk.sure.dream.compose.components.canvas

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Stable
class OnionSkinSettings {
    var enabled by mutableStateOf(false)

    /** Should skin wrapping around the frames */
    var isSkinWrapped by mutableStateOf(false)

    /** Should use frame colors as it is if true otherwise transform
     * backward skins to green shades and forward skins to green shades.
     * (with relevant alpha) */
    var useTrueColors by mutableStateOf(true)

    var backwardSkinCount by mutableIntStateOf(1)
        private set
    var forwardSkinCount by mutableIntStateOf(0)
        private set

    fun changeBackwardSkinCount(value: Int) {
        backwardSkinCount = value.coerceAtLeast(0)
    }

    fun changeForwardSkinCount(value: Int) {
        forwardSkinCount = value.coerceAtLeast(0)
    }
}