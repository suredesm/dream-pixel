package lk.sure.dream.data.entries

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "sprites")
data class SpriteMeta(
    @PrimaryKey val id: String,
    val name: String,
    val projectId: String?,
    val description: String,
    val width: Int,
    val height: Int,
    val lastModified: Long
)