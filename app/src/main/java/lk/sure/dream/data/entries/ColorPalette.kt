package lk.sure.dream.data.entries

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "palette")
data class ColorPalette(
    @PrimaryKey val id: String,
    val name: String,
    val colors: List<Int>,
)