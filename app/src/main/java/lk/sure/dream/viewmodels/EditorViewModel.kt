package lk.sure.dream.viewmodels

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.get
import androidx.core.graphics.set
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import lk.sure.dream.compose.components.canvas.CanvasStateSaver
import lk.sure.dream.compose.components.canvas.EditorCanvasState
import lk.sure.dream.data.entries.ColorPalette
import lk.sure.dream.data.repos.DreamRepository
import lk.sure.dream.data.repos.PaletteRepository
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    private val repository: DreamRepository,
    private val paletteRepository: PaletteRepository,
) : ViewModel() {

    private var _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private var _frameBitmaps = MutableStateFlow<List<Bitmap>?>(null)
    val frameBitmaps = _frameBitmaps.asStateFlow()

    private var _spriteName = MutableStateFlow<String?>(null)
    val spriteName = _spriteName.asStateFlow()

    lateinit var canvasState: EditorCanvasState

    private val _palettes = MutableStateFlow<List<ColorPalette>?>(null)
    val palettes = _palettes.asStateFlow()

    private val _mainPalette = MutableStateFlow<List<Color>>(emptyList())
    val mainPalette = _mainPalette.asStateFlow()

    private val _shouldUpdatedPalette = MutableStateFlow<ColorPalette?>(null)
    val shouldUpdatedPalette = _shouldUpdatedPalette.asStateFlow()

    fun restoreState(spriteId: String) {
        viewModelScope.launch {
            _spriteName.update { repository.getSprite(spriteId).name }
            val content = repository.fetchSpriteContent(spriteId)
            val data = CanvasStateSaver.fromByteArray(content)
            canvasState = EditorCanvasState.createState(data)

            _mainPalette.update { canvasState.recentColors.asReversed() }
            canvasState.saveAll()
            delay(300)
            _isLoading.update { false }
        }
    }

    fun saveState(spriteId: String) {
        viewModelScope.launch {
            val content = canvasState.toByteArray()
            repository.saveSprite(spriteId, content)
            val frame = canvasState.frames.firstOrNull() ?: return@launch
            val thumbnail = Bitmap.createScaledBitmap(frame.image, 128, 128, false)
            repository.updateThumbnail(spriteId, thumbnail)
            canvasState.saveAll()
        }
    }

    fun saveOnlyRecentColors(spriteId: String) {
        viewModelScope.launch {
            repository.saveOnlyRecentColors(spriteId, canvasState.recentColors)
        }
    }

    fun saveAsSprite(
        currentSpriteId: String,
        spriteName: String,
        description: String,
        onCompletion: () -> Unit
    ) {
        viewModelScope.launch {
            val currentMeta = repository.getSprite(currentSpriteId)
            val content = canvasState.toByteArray()
            val spriteId = repository.createSprite(
                spriteName = spriteName,
                projectId = currentMeta.projectId,
                description = description,
                width = currentMeta.width,
                height = currentMeta.height,
                thumbnail = null,
                content = content,
            )
            val frame = canvasState.frames.firstOrNull() ?: return@launch
            val thumbnail = Bitmap.createScaledBitmap(frame.image, 128, 128, false)
            repository.updateThumbnail(spriteId, thumbnail)
        }.invokeOnCompletion { onCompletion() }
    }

    fun loadBitmapFrames() {
        viewModelScope.launch {
            val list = canvasState.frames.map {
                it.image
            }
            _frameBitmaps.update { list }
        }
    }

    fun freeBitmapFrames() {
        viewModelScope.launch {
            _frameBitmaps.update { null }
        }
    }

    fun export(
        exportAsStrip: Boolean,
        selectedFrames: List<Int>,
        fileName: String,
        uri: Uri,
    ) {
        viewModelScope.launch {
            if (selectedFrames.isEmpty()) return@launch

            val name = fileName.ifEmpty { spriteName.value ?: "File" }

            if (exportAsStrip) {
                exportInStripMode(context, selectedFrames, uri, name)
            } else {
                exportInSeparateMode(context, selectedFrames, uri, name)
            }
        }
    }

    private fun exportInStripMode(context: Context, selectedFrames: List<Int>, destUri: Uri, fileName: String) {
        val (width, height) = canvasState.canvasSize
        val exportImage =
            Bitmap.createBitmap(width * selectedFrames.size, height, Bitmap.Config.ARGB_8888)
        var offset = 0

        for (idx in selectedFrames.toSet()) {
            val frame = canvasState.frames[idx]
            val bitmap = frame.image

            for (y in 0..<height) {
                for (x in 0..<width) {
                    val color = bitmap[x, y]
                    exportImage[offset + x, y] = color
                }
            }

            offset += width
        }

        val resolver = context.contentResolver

        val folderDocumentUri = DocumentsContract.buildDocumentUriUsingTree(
            destUri, DocumentsContract.getTreeDocumentId(destUri)
        )

        val docUri = DocumentsContract.createDocument(
            resolver, folderDocumentUri, "image/png", fileName
        )

        docUri?.let { uri ->
            resolver.openOutputStream(uri)?.use {
                exportImage.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    private fun exportInSeparateMode(
        context: Context,
        selectedFrames: List<Int>,
        destUri: Uri,
        fileName: String,
    ) {
        val contentResolver = context.contentResolver

        val mainFolderDocumentUri = DocumentsContract.buildDocumentUriUsingTree(
            destUri, DocumentsContract.getTreeDocumentId(destUri)
        )

        val contentUri = DocumentsContract.createDocument(
            contentResolver, mainFolderDocumentUri, "vnd.android.document/directory", fileName
        )
        var count = 1

        for (frameIndex in selectedFrames) {
            val docUri = contentUri?.let {
                DocumentsContract.createDocument(
                    contentResolver, it, "image/png", "${count++}"
                )
            }

            docUri?.let { uri ->
                contentResolver.openOutputStream(uri)?.use {
                    canvasState.frames[frameIndex].image.compress(Bitmap.CompressFormat.PNG, 100, it)
//                    val final = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
//                    final.eraseColor(if (frameIndex < 20) Color.White.toArgb() else Color(0xFF3DDC84).toArgb())
//                    val canvas = Canvas(final)
//                    canvas.drawBitmap(
//                        canvasState.frames[frameIndex].image,
//                        null,
//                        Rect(0, 420, 1080, 420 + 1080),
//                        null
//                    )
//                    final.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
        }
    }

    fun fetchPalettes() {
        viewModelScope.launch {
            _palettes.update { paletteRepository.getAllPalettes() }
            val pal = palettes.value?.firstOrNull { it.id == shouldUpdatedPalette.value?.id }
                ?: return@launch
            _shouldUpdatedPalette.update { pal }
            _mainPalette.update { pal.colors.map { Color(it) } }
        }
    }

    fun changeMainPalette(palette: ColorPalette) {
        _mainPalette.update { palette.colors.map { Color(it) } }
    }

    fun changeShouldUpdatePalette(palette: ColorPalette?) {
        _shouldUpdatedPalette.update { palette }
    }

    fun addToRecentPalette(color: Color) {
        canvasState.addToRecent(color)
        addToCurrentPalette(color)
    }

    private fun addToCurrentPalette(color: Color) {
        viewModelScope.launch {
            val palette = shouldUpdatedPalette.value
            if (palette != null && !palette.colors.contains(color.toArgb())) {
                paletteRepository.update(
                    palette.copy(colors = palette.colors + color.toArgb())
                )
            }
            fetchPalettes()
        }
    }

    fun createPalette(name: String, colors: List<Color>) {
        viewModelScope.launch {
            paletteRepository.insert(
                ColorPalette(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    colors = colors.map { it.toArgb() }
                )
            )
            fetchPalettes()
        }
    }

    fun duplicatePalette(palette: ColorPalette) {
        viewModelScope.launch {
            val paletteNames = palettes.value?.map { it.name }
            var name = palette.name
            var count = 1

            if (paletteNames != null) {
                while (paletteNames.contains("$name $count")) {
                    count++
                }
            }

            name = "$name $count"
            paletteRepository.insert(
                ColorPalette(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    colors = palette.colors
                )
            )
            fetchPalettes()
        }
    }

    fun updatePaletteColors(palette: ColorPalette, colors: List<Color>) {
        viewModelScope.launch {
            paletteRepository.update(palette.copy(colors = colors.map { it.toArgb() }))
            fetchPalettes()
        }
    }

    fun delete(palette: ColorPalette) {
        viewModelScope.launch {
            paletteRepository.delete(palette.id)
            fetchPalettes()
        }
    }

    fun changeMainPaletteToRecent() {
        _mainPalette.update { canvasState.recentColors.asReversed() }
    }
}