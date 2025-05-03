package lk.sure.dream.data.repos

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import lk.sure.dream.compose.components.SpriteItemUiState
import lk.sure.dream.compose.components.canvas.CanvasStateSaver
import lk.sure.dream.compose.components.canvas.DREAM_EXTENSION_NAME
import lk.sure.dream.compose.components.canvas.addExtensionIfNot
import lk.sure.dream.compose.components.canvas.dreamFileDir
import lk.sure.dream.compose.home.ProjectItemUiState
import lk.sure.dream.data.dao.DreamDao
import lk.sure.dream.data.entries.ColorPalette
import lk.sure.dream.data.entries.ProjectMeta
import lk.sure.dream.data.entries.SpriteMeta
import lk.sure.dream.viewmodels.BackupRestoreConflict
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

const val DREAM_BACKUP_EXTENSION = "drmb"
val String.withBackupExtension get() = "$this.$DREAM_BACKUP_EXTENSION"

class DreamRepository @Inject constructor(
    private val context: Context,
    private val dreamDao: DreamDao,
) {
    private val dreamFileDir = context.dreamFileDir

    /** @return random generated id of the project */
    suspend fun createProject(
        projectName: String,
        description: String,
        thumbnail: Bitmap? = null,
    ) = withContext(Dispatchers.IO) {
        val projectId = UUID.randomUUID().toString()

        thumbnail?.let { thumbnailNonNull ->
            val thumbnailStream = ByteArrayOutputStream()
            thumbnailNonNull.compress(Bitmap.CompressFormat.PNG, 100, thumbnailStream)

            val thumbnailFile = File(dreamFileDir, "$projectId.png")
            thumbnailFile.writeBytes(thumbnailStream.toByteArray())
            thumbnailStream.close()
        }

        dreamDao.insertProject(
            ProjectMeta(
                id = projectId,
                name = projectName,
                description = description,
                lastModified = System.nanoTime(),
            )
        )

        projectId
    }

    /** @return random generated id of the sprite */
    suspend fun createSprite(
        spriteName: String,
        projectId: String?,
        description: String,
        width: Int,
        height: Int,
        thumbnail: Bitmap?,
        content: ByteArray? = null,
        lastModified: Long? = null,
    ) = withContext(Dispatchers.IO) {
        val spriteId = UUID.randomUUID().toString()

        thumbnail?.let { thumbnailNonNull ->
            val thumbnailStream = ByteArrayOutputStream()
            thumbnailNonNull.compress(Bitmap.CompressFormat.PNG, 100, thumbnailStream)

            val thumbnailFile = File(dreamFileDir, "$spriteId.png")

            thumbnailFile.writeBytes(thumbnailStream.toByteArray())
            thumbnailStream.close()
        }

        dreamDao.insertSprite(
            SpriteMeta(
                id = spriteId,
                name = spriteName,
                projectId = projectId,
                description = description,
                width = width,
                height = height,
                lastModified = lastModified ?: System.currentTimeMillis(),
            )
        )

        val spriteFile = File(dreamFileDir, spriteId.addExtensionIfNot())
        spriteFile.createNewFile()
        if (content != null) {
            spriteFile.writeBytes(content)
        }

        spriteId
    }

    suspend fun getAllProjects(fetchThumbnails: Boolean = true): List<ProjectItemUiState> {
        return dreamDao.getAllProjects().map { meta ->
            ProjectItemUiState(
                meta.id,
                meta.name,
                meta.description,
                if (fetchThumbnails) {
                    try {
                        dreamDao.idOfFirstSprite(meta.id)?.let {
                            val path = File(dreamFileDir, "$it.png")
                                .absolutePath ?: return@let null
                            BitmapFactory.decodeFile(path).asImageBitmap()
                        }
                    } catch (_: Exception) {
                        null
                    }
                } else null
            )
        }
    }

    suspend fun getAllSprites(projectId: String?): List<SpriteItemUiState> =
        withContext(Dispatchers.IO) {

            val fileMap = buildMap {
                dreamFileDir.listFiles { file: File ->
                    file.extension == "png"
                }?.forEach {
                    put(it.nameWithoutExtension, it!!)
                }
            }

            dreamDao.getAllSprites(projectId).map {
                val file = fileMap[it.id]

                val image = if (file == null) {
                    null
                } else {
                    BitmapFactory.decodeFile(file.absolutePath).asImageBitmap()
                }

                SpriteItemUiState(
                    id = it.id,
                    spriteName = it.name,
                    spriteDescription = it.description,
                    width = it.width,
                    height = it.height,
                    lastModified = it.lastModified,
                    thumbnail = image,
                )
            }
        }

    suspend fun fetchSpriteContent(
        spriteId: String,
    ) = withContext(Dispatchers.IO) {
        val spriteFile = File(dreamFileDir, spriteId + DREAM_EXTENSION_NAME)
        spriteFile.readBytes()
    }

    suspend fun saveSprite(
        spriteId: String,
        content: ByteArray,
    ) = withContext(Dispatchers.IO) {
        val spriteFile = File(dreamFileDir, spriteId + DREAM_EXTENSION_NAME)
        spriteFile.writeBytes(content)

        dreamDao.updateSpriteLastModified(spriteId, System.currentTimeMillis())
    }

    suspend fun getProject(projectId: String): ProjectItemUiState {
        return dreamDao.getProject(projectId).run {
            ProjectItemUiState(
                id,
                name,
                description,
                null
            )
        }
    }

    suspend fun updateThumbnail(spriteId: String, thumbnail: Bitmap) = withContext(Dispatchers.IO) {
        val thumbnailFile = File(dreamFileDir, "$spriteId.png")
        val byteStream = ByteArrayOutputStream()
        thumbnail.compress(Bitmap.CompressFormat.PNG, 100, byteStream)
        thumbnailFile.createNewFile()
        thumbnailFile.writeBytes(byteStream.toByteArray())
        byteStream.close()
    }

    suspend fun getSprite(spriteId: String) = dreamDao.getSprite(spriteId)

    suspend fun deleteSprite(spriteId: String) = withContext(Dispatchers.IO) {
        val thumbnailFile = File(dreamFileDir, "$spriteId.png")
        thumbnailFile.delete()
        dreamDao.deleteSprite(spriteId)
    }

    suspend fun deleteProjectWithAllContent(projectId: String) = withContext(Dispatchers.IO) {
        val projectSprites = dreamDao.getAllSpritesFromProject(projectId)
        for (projectSprite in projectSprites) {
            deleteSprite(projectSprite.id)
        }
        dreamDao.deleteProject(projectId)
    }

    suspend fun updateProjectMeta(
        projectId: String,
        name: String? = null,
        description: String? = null,
    ) {
        val real = dreamDao.getProject(projectId)
        dreamDao.updateProject(
            ProjectMeta(
                projectId,
                name ?: real.name,
                description ?: real.description,
                real.lastModified
            )
        )
    }

    suspend fun updateSpriteMeta(
        id: String,
        spriteName: String? = null,
        description: String? = null,
        lastModified: Long? = null,
    ) {
        val real = dreamDao.getSprite(id)

        dreamDao.updateSprite(
            SpriteMeta(
                id = id,
                name = spriteName ?: real.name,
                projectId = real.projectId,
                description = description ?: real.description,
                width = real.width,
                height = real.height,
                lastModified = lastModified ?: real.lastModified,
            )
        )
    }

    suspend fun saveOnlyRecentColors(id: String, recentColors: List<Color>) {
        val content = fetchSpriteContent(spriteId = id)
        val data = CanvasStateSaver.fromByteArray(content)
        val modified =
            CanvasStateSaver.toByteArray(data.copy(recentColors = recentColors.map { it.toArgb() }))

        saveSprite(id, modified)
    }

    suspend fun duplicateSprite(originalId: String, name: String, description: String) {
        val sprite = getSprite(originalId)
        val content = fetchSpriteContent(originalId)

        val id = createSprite(
            spriteName = name,
            projectId = sprite.projectId,
            description = description,
            width = sprite.width,
            height = sprite.height,
            thumbnail = null,
            content = content
        )

        val originalPng = File(dreamFileDir, "$originalId.png")
        val duplicatePng = File(dreamFileDir, "$id.png")
        withContext(Dispatchers.IO) {
            Files.copy(
                originalPng.toPath(),
                duplicatePng.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    suspend fun changeSpriteProject(spriteId: String, projectId: String?) {
        val real = dreamDao.getSprite(spriteId)

        if (projectId == real.id) return

        dreamDao.updateSprite(
            SpriteMeta(
                id = spriteId,
                name = real.name,
                projectId = projectId,
                description = real.description,
                width = real.width,
                height = real.height,
                lastModified = real.lastModified,
            )
        )
    }

    suspend fun retrieveBackups(uri: Uri?): List<DocumentFile> = withContext(Dispatchers.IO) {
        if (uri == null) return@withContext emptyList()

        val rootDoc = DocumentFile.fromTreeUri(context, uri)
        rootDoc?.listFiles()?.filter { it.name?.substringAfter(".") == DREAM_BACKUP_EXTENSION }
            ?.toList() ?: emptyList()
    }

    // Uses [BackupVersion.V1] as Format
    suspend fun createBackup(
        uri: Uri?,
        name: String,
        spriteIds: List<String>,
        paletteIds: List<String>,
    ): Boolean {
        if (uri == null) return false

        val sprites =
            dreamDao.getAllSpritesRegardlessOfProject().filter { spriteIds.contains(it.id) }
        val projects = sprites.mapNotNull { it.projectId }.toSet().map { dreamDao.getProject(it) }
        val palettes = dreamDao.getAllColorPalettes().filter { paletteIds.contains(it.id) }

        val mainDir = DocumentFile.fromTreeUri(context, uri)
        val backupFile =
            mainDir?.findFile(name.withBackupExtension) ?: mainDir?.createFile(
                "dream/backup",
                name.withBackupExtension
            )

        if (backupFile == null) return false

        val backupUri = backupFile.uri
        val outputStream = context.contentResolver.openOutputStream(backupUri)
        val zipOutputStream = ZipOutputStream(BufferedOutputStream(outputStream))

        zipOutputStream.use { zos ->
            sprites.forEach { sprite ->
                val file = File(dreamFileDir, "${sprite.id}.drm")
                val projectDirName = sprite.projectId ?: "Home"

                FileInputStream(file).use {
                    zos.putNextEntry(ZipEntry("$projectDirName/${sprite.id}"))
                    it.copyTo(zos)
                    zos.closeEntry()
                }

            }

            zos.putNextEntry(ZipEntry("meta.json"))
            zos.write(Json.encodeToString(projects).toByteArray())
            zos.closeEntry()

            sprites.groupBy { it.projectId }.forEach { (projectId, spriteGroup) ->
                zos.putNextEntry(ZipEntry("${projectId ?: "Home"}/meta.json"))
                zos.write(Json.encodeToString(spriteGroup).toByteArray())
                zos.closeEntry()
            }

            spriteIds.forEach { id ->
                val thumbnail = File(dreamFileDir, "$id.png")
                if (thumbnail.exists()) {
                    FileInputStream(thumbnail).use { fis ->
                        zos.putNextEntry(ZipEntry("Thumbnails/$id.png"))
                        fis.copyTo(zos)
                        zos.closeEntry()
                    }
                }
            }

            zos.putNextEntry(ZipEntry("palettes.json"))
            zos.write(Json.encodeToString(palettes).toByteArray())
            zos.closeEntry()
        }

        return true
    }

    suspend fun retrieveBackup(
        file: DocumentFile,
    ): BackupMetaList? = withContext(Dispatchers.IO) {
        val sprites = mutableListOf<SpriteMeta>()
        val projects = mutableListOf<ProjectMeta>()
        val palettes = mutableListOf<ColorPalette>()
        val thumbnails = mutableMapOf<String, ImageBitmap>()

        context.contentResolver.openInputStream(file.uri)?.use { fis ->
            ZipInputStream(BufferedInputStream(fis)).use { zos ->
                var zipEntry = zos.nextEntry
                while (zipEntry != null) {
                    val entryName = zipEntry.name
                    if (zipEntry.isDirectory) return@withContext null

                    val nameSplit = entryName.split("/")
                        .also { if (it.size > 2) return@withContext null }

                    if (nameSplit.size == 1) {
                        when (entryName) {
                            "meta.json" -> {
                                val pros =
                                    Json.decodeFromString<List<ProjectMeta>>(String(zos.readBytes()))
                                projects.addAll(pros)
                            }

                            "palettes.json" -> {
                                val pals =
                                    Json.decodeFromString<List<ColorPalette>>(String(zos.readBytes()))
                                palettes.addAll(pals)
                            }

                            else -> return@withContext null
                        }
                    } else {
                        if (nameSplit.first() == "Thumbnails") {
                            val image = zos.readBytes().let {
                                BitmapFactory.decodeByteArray(it, 0, it.size).asImageBitmap()
                            }

                            thumbnails[nameSplit.last().substringBefore(".")] = image
                        } else if (nameSplit.last() == "meta.json") {
                            val mSprites =
                                Json.decodeFromString<List<SpriteMeta>>(String(zos.readBytes()))
                            sprites.addAll(mSprites)
                        }
                    }

                    zos.closeEntry()
                    zipEntry = zos.nextEntry
                }
            }
        }

        return@withContext BackupMetaList(sprites, projects, palettes, thumbnails)
    }

    suspend fun restoreSprites(
        backup: DocumentFile,
        spriteIds: List<String>,
        conflict: BackupRestoreConflict,
        paletteIds: List<String>,
    ): Boolean = withContext(Dispatchers.IO) {
        val unzipDest = File(dreamFileDir, "unzip").also { unzipFile ->
            if (unzipFile.isFile) unzipFile.delete()
            unzipFile.mkdir()

            unzipFile.listFiles()?.forEach {
                if (it.isDirectory) {
                    it.deleteRecursively()
                } else {
                    it.delete()
                }
            }
        }

        val idsOfProjectsToUpdates = mutableSetOf<String?>()
        val spriteProjectMap = mutableMapOf<String, String?>()

        context.contentResolver.openInputStream(backup.uri).use { fis ->
            ZipInputStream(fis).use { zis ->
                var zipEntry = zis.nextEntry
                while (zipEntry != null) {
                    val entryName = zipEntry.name
                    val nameSplit = entryName
                        .split("/")
                        .also { if (it.size > 2) return@withContext false }

                    if (nameSplit.size == 1) {
                        val file = File(unzipDest, nameSplit.first())
                        file.createNewFile()
                        FileOutputStream(file).use { fos ->
                            zis.copyTo(fos)
                        }
                    } else {
                        val fileName = nameSplit.last()
                        val spriteId = fileName.takeIf { it in spriteIds }
                        val projectId = nameSplit.first()

                        if (fileName == "meta.json") {
                            val parent = File(unzipDest, projectId).also { it.mkdir() }
                            val file = File(parent, fileName).also { it.createNewFile() }
                            FileOutputStream(file).use { fos ->
                                zis.copyTo(fos)
                            }
                        } else if (spriteId != null) {
                            val parent = File(unzipDest, projectId).also { it.mkdir() }
                            val file = File(parent, spriteId).also { it.createNewFile() }
                            FileOutputStream(file).use { fos ->
                                zis.copyTo(fos)
                            }

                            val proId = projectId.takeUnless { projectId == "Home" }
                            spriteProjectMap[spriteId] = proId
                            idsOfProjectsToUpdates.add(proId)
                        } else if (projectId == "Thumbnails") {
                            val parent = File(unzipDest, projectId).also { it.mkdir() }
                            val file = File(parent, fileName).also { it.createNewFile() }
                            FileOutputStream(file).use { fos ->
                                zis.copyTo(fos)
                            }
                        }
                    }

                    zis.closeEntry()
                    zipEntry = zis.nextEntry
                }
            }
        }

        File(unzipDest, "palettes.json").takeIf { it.exists() }?.let { file ->
            FileInputStream(file).use { fis ->
                val palettes = Json
                    .decodeFromString<List<ColorPalette>>(String(fis.readBytes()))
                    .filter { it.id in paletteIds }

                when (conflict) {
                    BackupRestoreConflict.OverwriteAll,
                    BackupRestoreConflict.OverwriteWithoutProjectMeta,
                        -> {
                        for (palette in palettes) {
                            dreamDao.upsertColorPalette(palette)
                        }
                    }

                    BackupRestoreConflict.TakeCopy -> {
                        val currentPaletteIds = dreamDao.getAllColorPalettes().map { it.id }

                        for (palette in palettes) {
                            if (palette.id in currentPaletteIds) {
                                dreamDao
                                    .insertColorPalette(
                                        palette.copy(
                                            id = UUID.randomUUID().toString(),
                                            name = palette.name + " - copy"
                                        )
                                    )
                            } else {
                                dreamDao.insertColorPalette(palette)
                            }
                        }
                    }
                }
            }
        }

        File(unzipDest, "meta.json").takeIf { it.exists() }?.let { file ->
            FileInputStream(file).use { fis ->
                val projects = Json.decodeFromString<List<ProjectMeta>>(
                    String(fis.readBytes())
                )
                val currentProjectIds = dreamDao.getAllProjects().map { it.id }

                when (conflict) {
                    BackupRestoreConflict.OverwriteAll -> {
                        for (project in projects) {
                            if (project.id in currentProjectIds) {
                                dreamDao.updateProject(project)
                            } else {
                                dreamDao.insertProject(project)
                            }
                        }
                    }

                    else -> {
                        for (project in projects) {
                            if (project.id !in currentProjectIds) {
                                dreamDao.insertProject(project)
                            }
                        }
                    }
                }
            }
        }

        idsOfProjectsToUpdates.forEach { projectId ->
            val projectFile = File(unzipDest, projectId ?: "Home")
            val spriteMetaFile = File(projectFile, "meta.json")
            val spriteMeta: List<SpriteMeta>

            FileInputStream(spriteMetaFile).use {
                val content = String(it.readBytes())
                spriteMeta = Json.decodeFromString(content)
            }

            val existingSpriteIds = dreamDao.getAllSprites(projectId).map { it.id }
            val thumbnailsFile = File(unzipDest, "Thumbnails")

            spriteMeta.filter { it.id in spriteIds }.forEach { meta ->
                if (meta.id in existingSpriteIds) {
                    when (conflict) {
                        BackupRestoreConflict.OverwriteAll, BackupRestoreConflict.OverwriteWithoutProjectMeta -> {
                            Files.move(
                                File(projectFile, meta.id).toPath(),
                                File(dreamFileDir, "${meta.id}.drm").toPath(),
                                StandardCopyOption.REPLACE_EXISTING,
                            )
                            dreamDao.updateSprite(meta)
                            File(thumbnailsFile, "${meta.id}.png").takeIf { it.exists() }
                                ?.let { thumbnail ->
                                    Files.move(
                                        thumbnail.toPath(),
                                        File(dreamFileDir, thumbnail.name).toPath(),
                                        StandardCopyOption.REPLACE_EXISTING,
                                    )
                                }
                        }

                        BackupRestoreConflict.TakeCopy -> {
                            val newId = UUID.randomUUID().toString()
                            Files.move(
                                File(projectFile, meta.id).toPath(),
                                File(dreamFileDir, "$newId.drm").toPath(),
                                StandardCopyOption.REPLACE_EXISTING,
                            )
                            dreamDao.insertSprite(
                                meta.copy(
                                    id = newId,
                                    name = meta.name + " - copy"
                                )
                            )
                            File(thumbnailsFile, "${meta.id}.png").takeIf { it.exists() }
                                ?.let { thumbnail ->
                                    Files.move(
                                        thumbnail.toPath(),
                                        File(dreamFileDir, "$newId.png").toPath(),
                                        StandardCopyOption.REPLACE_EXISTING,
                                    )
                                }
                        }
                    }
                } else {
                    Files.move(
                        File(projectFile, meta.id).toPath(),
                        File(dreamFileDir, "${meta.id}.drm").toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                    )
                    dreamDao.insertSprite(meta)
                    File(thumbnailsFile, "${meta.id}.png").takeIf { it.exists() }
                        ?.let { thumbnail ->
                            Files.move(
                                thumbnail.toPath(),
                                File(dreamFileDir, thumbnail.name).toPath(),
                                StandardCopyOption.REPLACE_EXISTING,
                            )
                        }
                }
            }
        }

        unzipDest.deleteRecursively()

        return@withContext true
    }

    suspend fun getAllSpritesRegardlessProject() = dreamDao.getAllSpritesRegardlessOfProject()
}

/** Every backup is in zip format. And it contains backup.txt
 * containing version ( V([BackupVersion]) ) information of the backup. According
 * to this the backup should be decoded.
 */
private enum class BackupVersion {
    /** All Projects are directories with their ids as name
     * relevant dream file is under it's project with that sprites
     * ids as it's name (without any file extension). Project-less
     * dream files (Home sprites) are in Home (name of the directory)
     * directory. All the projects meta ([ProjectMeta]) is in meta.json
     * in Main directory. And all project contains such file which
     * contains sprites meta ([SpriteMeta]). All thumbnails are
     * in Thumbnails directory with relevant sprite is as name (
     * .png file name extension inclusive). Palettes are in palettes.json */
    V1;
}

data class BackupMetaList(
    val sprites: List<SpriteMeta>,
    val projects: List<ProjectMeta>,
    val palettes: List<ColorPalette>,
    val thumbnail: Map<String, ImageBitmap>,
)