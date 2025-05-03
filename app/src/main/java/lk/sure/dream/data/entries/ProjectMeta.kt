package lk.sure.dream.data.entries

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "projects")
data class ProjectMeta(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val lastModified: Long
)