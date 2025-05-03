package lk.sure.dream.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromColorList(colors: List<Int>): String {
        return colors.joinToString(",")
    }

    @TypeConverter
    fun toColorList(data: String): List<Int> {
        return if (data.isBlank()) emptyList()
        else data.split(",").map { it.toInt() }
    }
}