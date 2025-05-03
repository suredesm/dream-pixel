package lk.sure.dream.data.repos

import lk.sure.dream.data.dao.DreamDao
import lk.sure.dream.data.entries.ColorPalette
import javax.inject.Inject

class PaletteRepository @Inject constructor(private val dreamDao: DreamDao) {
    suspend fun insert(colorPalette: ColorPalette) = dreamDao.insertColorPalette(colorPalette)

    suspend fun update(colorPalette: ColorPalette) = dreamDao.updateColorPalette(colorPalette)

    suspend fun delete(colorPaletteId: String) = dreamDao.deleteColorPalette(colorPaletteId)

    suspend fun getAllPalettes() = dreamDao.getAllColorPalettes()
}