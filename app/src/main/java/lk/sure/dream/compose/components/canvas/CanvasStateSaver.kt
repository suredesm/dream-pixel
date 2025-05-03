package lk.sure.dream.compose.components.canvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File

const val DREAM_DIR_NAME = "dream"

/** ".drm" */
const val DREAM_EXTENSION_NAME = ".drm"

private var mDreamDir: File? = null

val Context.dreamFileDir get() = mDreamDir ?: File(filesDir, DREAM_DIR_NAME).also { it.mkdir() }

fun String.addExtensionIfNot() = removeSuffix(DREAM_EXTENSION_NAME) + DREAM_EXTENSION_NAME

/**
 * The helper class for convert a [EditorCanvasState] to
 * savable byte array and vice versa.
 *
 * @see [CanvasStateSaver.Version] */
class CanvasStateSaver private constructor() {

    /**
     * Version enum for canvas state saving method
     * @param versionInt is version representation in the file as [Int]
     */
    enum class Version(internal val versionInt: Int) {
        /**
         * File format v0.1.
         *
         * First 10 bytes represent text Dream.
         * And next 4 bytes is width of the Canvas in pixels
         * And next 4 bytes is height of the Canvas in pixels
         *
         * And next 4 bytes is frame count
         * And next 4 bytes is layer count
         *   For repeated pattern for all layers (Arrangement bottom to top)
         **    First 1 byte is layer visibility either 0 or 1
         **    Next 4 bytes are layer name length
         **    Next each two bytes are for single [Char] until the layer name ends
         *
         * For repeated pattern for all frames
         **  For repeated pattern for all layers (Arrangement bottom to top)
         ***   Next each four bytes are color represented as ARGB colors until [width * height]
         */
        V0_1(0),

        /**
         * Successor of [V0_1]. Maps to Integer version of 1.
         * #### Improvements:
         * - Wrapper around png
         * - Recent Colors
         *
         *
         * **Caution:** All 2 Bytes are [Short] unless mentioned
         * #### Format:
         * - 12 Bytes - Represents text vDream (v for versioned)
         * - 2 Bytes - Represents 1 ([versionInt])
         * - 2 Bytes - Width
         * - 2 Bytes - Height
         * - 2 Bytes - Frame count
         * - 2 Bytes - Layer count
         * - 2 Bytes - Recent color count
         * - Layer Details Meta (For Layer count repeated pattern as following)
         *     - 1 Byte - Layer visibility as 0 or 1 in [Boolean]
         *     - 4 Bytes - Layer name length in [Int]
         *     - Layer name represented as [String] with length of previously mentioned
         * - Recent Colors (For Recent color count repeated pattern as following)
         *     - 4 Bytes - [Int] value from [Color.toArgb()]
         * - Rest is compressed png from [Bitmap]. The png has arrangement as strip
         * where layers are in horizontal order for all frames.
         * */
        V0_2(1),
    }

    /** Holds [Layer] data for storing and retrieving purpose */
    data class LayerData(
        val name: String,
        val isVisible: Boolean,
    )

    /** Holds [EditorCanvasState]'s data for storing and retrieving purpose */
    data class CanvasData(
        val width: Int,
        val height: Int,
        val frameCount: Int,
        val layers: List<LayerData>,
        val bitmaps: List<Bitmap>,
        val recentColors: List<Int>? = null,
    )

    companion object {

        private val defaultPackVersion = Version.V0_2

        fun getDefaultCanvasData(width: Int, height: Int) = CanvasData(
            width = width,
            height = height,
            frameCount = 1,
            layers = listOf(LayerData("Layer 1", true)),
            bitmaps = listOf(
                Bitmap.createBitmap(
                    width,
                    height,
                    Bitmap.Config.ARGB_8888
                )
            ),
        )

        /** Pack [CanvasData] to File writable [ByteArray]
         *
         * Resulting byte array can be unpacked using [fromByteArray]
         * @see [fromByteArray] */
        fun toByteArray(canvasData: CanvasData, version: Version = defaultPackVersion): ByteArray {
            return when (version) {
                Version.V0_1 -> configureV0p1(
                    canvasData.width,
                    canvasData.height,
                    canvasData.frameCount,
                    canvasData.layers,
                    canvasData.bitmaps,
                )

                Version.V0_2 -> configureV0p2(
                    canvasData.width,
                    canvasData.height,
                    canvasData.frameCount,
                    canvasData.recentColors,
                    canvasData.layers,
                    canvasData.bitmaps,
                )
            }
        }

        /** v0.1 packing method for [Version.V0_1] */
        private fun configureV0p1(
            width: Int,
            height: Int,
            frameCount: Int,
            layers: List<LayerData>,
            bitmaps: List<Bitmap>,
        ): ByteArray {
            val outputStream = ByteArrayOutputStream()
            val dataOutputStream = DataOutputStream(outputStream)

            dataOutputStream.writeChars("Dream")
            dataOutputStream.writeInt(width)
            dataOutputStream.writeInt(height)
            dataOutputStream.writeInt(frameCount)
            dataOutputStream.writeInt(layers.size)

            for (layer in layers) {
                dataOutputStream.writeBoolean(layer.isVisible)
                dataOutputStream.writeInt(layer.name.length)
                dataOutputStream.writeChars(layer.name)
            }

            for (frameIdx in 0..<frameCount) {
                for (layerIdx in layers.indices) {
                    val bitmap = bitmaps[frameIdx * layers.size + layerIdx]
                    for (y in 0..<height) {
                        for (x in 0..<width) {
                            val color = bitmap.getPixel(x, y)
                            dataOutputStream.writeInt(color)
                        }
                    }
                }
            }

            dataOutputStream.flush()
            return outputStream.toByteArray()
        }

        /** v0.1 packing method for [Version.V0_2] */
        private fun configureV0p2(
            width: Int,
            height: Int,
            frameCount: Int,
            recentColors: List<Int>?,
            layers: List<LayerData>,
            bitmaps: List<Bitmap>,
        ): ByteArray {
            val outputStream = ByteArrayOutputStream()
            DataOutputStream(outputStream).use { dataOutputStream ->
                dataOutputStream.writeChars("vDream")
                dataOutputStream.writeShort(Version.V0_2.versionInt)
                dataOutputStream.writeShort(width)
                dataOutputStream.writeShort(height)
                dataOutputStream.writeShort(frameCount)
                dataOutputStream.writeShort(layers.size)
                dataOutputStream.writeShort(recentColors?.size ?: 0)

                for (layer in layers) {
                    dataOutputStream.writeBoolean(layer.isVisible)
                    dataOutputStream.writeInt(layer.name.length)
                    dataOutputStream.writeChars(layer.name)
                }

                recentColors?.forEach { color ->
                    dataOutputStream.writeInt(color)
                }

                if (bitmaps.isNotEmpty()) {
                    val resultWidth = width * bitmaps.size
                    val resultBitmap =
                        Bitmap.createBitmap(resultWidth, height, Bitmap.Config.ARGB_8888)

                    Canvas(resultBitmap).apply {
                        var xOffset = 0
                        for (bitmap in bitmaps) {
                            drawBitmap(bitmap, xOffset.toFloat(), 0F, null)
                            xOffset += width
                        }
                    }

                    resultBitmap.compress(Bitmap.CompressFormat.PNG, 100, dataOutputStream)
                }
            }

            return outputStream.toByteArray()
        }

        /** Unpacks the byte array packed using [CanvasStateSaver.toByteArray] */
        fun fromByteArray(array: ByteArray): CanvasData {
            val stream = ByteArrayInputStream(array)
            return DataInputStream(stream).use { dataStream ->
                // Check if start with v (meaning versioned)
                val isVersioned = dataStream.readChar() == 'v'

                // Read chars "Dream"
                repeat(
                    if (isVersioned) 5 else 4
                ) { dataStream.readChar() }

                val versionInt =
                    if (isVersioned) dataStream.readShort().toInt() else Version.V0_1.versionInt

                val version = Version.entries.first { it.versionInt == versionInt }

                when (version) {
                    Version.V0_1 -> dataStream.fromByteArrayV0p1()
                    Version.V0_2 -> dataStream.fromByteArrayV0p2()
                }
            }
        }

        private fun DataInputStream.fromByteArrayV0p1(): CanvasData {
            val width = readInt()
            val height = readInt()
            val frameCount = readInt()
            val layerCount = readInt()

            val layers = buildList {
                repeat(layerCount) {
                    val isVisible = readBoolean()
                    val length = readInt()

                    val name = buildString {
                        repeat(length) {
                            append(readChar())
                        }
                    }

                    add(
                        LayerData(
                            name,
                            isVisible
                        )
                    )
                }
            }

            val bitmaps = mutableListOf<Bitmap>()

            for (frameIdx in 0..<frameCount) {
                for (layerIdx in 0..<layerCount) {
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmaps.add(bitmap)
                    for (y in 0..<height) {
                        for (x in 0..<width) {
                            val color = readInt()
                            bitmap.setPixel(x, y, color)
                        }
                    }
                }
            }

            return CanvasData(
                width, height, frameCount, layers, bitmaps
            )
        }

        private fun DataInputStream.fromByteArrayV0p2(): CanvasData {
            val width = readShort().toInt()
            val height = readShort().toInt()
            val frameCount = readShort().toInt()
            val layerCount = readShort().toInt()
            val recentColorCount = readShort().toInt()

            val layers = buildList {
                repeat(layerCount) {
                    val isVisible = readBoolean()
                    val length = readInt()

                    val name = buildString {
                        repeat(length) {
                            append(readChar())
                        }
                    }

                    add(
                        LayerData(
                            name,
                            isVisible
                        )
                    )
                }
            }

            val recentColors = buildList {
                repeat(recentColorCount) { add(readInt()) }
            }

            val bitmaps = BitmapFactory.decodeStream(this).let { bitmap ->
                buildList {
                    for (i in 0..<layerCount * frameCount) {
                        add(Bitmap.createBitmap(bitmap, i * width, 0, width, height))
                    }
                }
            }

            return CanvasData(
                width, height, frameCount, layers, bitmaps, recentColors
            )
        }
    }
}