package lk.sure.dream.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import lk.sure.dream.data.entries.ColorPalette
import lk.sure.dream.data.entries.ProjectMeta
import lk.sure.dream.data.entries.SpriteMeta

@Dao
interface DreamDao {
    @Insert
    suspend fun insertProject(projectMeta: ProjectMeta)

    @Update
    suspend fun updateProject(projectMeta: ProjectMeta)

    @Query("SELECT * FROM projects ORDER BY lastModified DESC")
    suspend fun getAllProjects(): List<ProjectMeta>

    @Insert
    suspend fun insertSprite(spriteMeta: SpriteMeta)

    @Update
    suspend fun updateSprite(spriteMeta: SpriteMeta)

    @Query("SELECT * FROM sprites WHERE (projectId IS NULL AND :projectId IS NULL) OR projectId = :projectId")
    suspend fun getAllSprites(projectId: String?): List<SpriteMeta>

    @Query("SELECT * FROM projects WHERE id = :projectId LIMIT 1")
    suspend fun getProject(projectId: String): ProjectMeta

    @Query("SELECT * FROM sprites WHERE id = :spriteId LIMIT 1")
    suspend fun getSprite(spriteId: String): SpriteMeta

    @Query("DELETE FROM sprites WHERE id = :spriteId")
    suspend fun deleteSprite(spriteId: String)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProject(projectId: String)

    @Query("SELECT * FROM sprites WHERE projectId = :projectId")
    suspend fun getAllSpritesFromProject(projectId: String): List<ProjectMeta>

    @Query("SELECT id FROM sprites WHERE projectId = :projectId ORDER BY  lastModified DESC LIMIT 1")
    suspend fun idOfFirstSprite(projectId: String): String?

    @Insert
    suspend fun insertColorPalette(palette: ColorPalette)

    @Update
    suspend fun updateColorPalette(palette: ColorPalette)

    @Upsert
    suspend fun upsertColorPalette(palette: ColorPalette)

    @Query("DELETE FROM palette WHERE id = :paletteId")
    suspend fun deleteColorPalette(paletteId: String)

    @Query("SELECT * FROM palette")
    suspend fun getAllColorPalettes(): List<ColorPalette>

    @Query("UPDATE sprites SET lastModified = :lastModifier WHERE id = :spriteId")
    suspend fun updateSpriteLastModified(spriteId: String, lastModifier: Long)

    @Query("SELECT * FROM sprites")
    suspend fun getAllSpritesRegardlessOfProject(): List<SpriteMeta>
}