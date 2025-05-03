package lk.sure.dream.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import lk.sure.dream.data.dao.DreamDao
import lk.sure.dream.data.entries.ColorPalette
import lk.sure.dream.data.entries.ProjectMeta
import lk.sure.dream.data.entries.SpriteMeta

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS palette (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                colors TEXT NOT NULL
            )
        """.trimIndent()
        )
    }
}

@Database(
    entities = [ProjectMeta::class, SpriteMeta::class, ColorPalette::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DreamDatabase: RoomDatabase() {
    abstract fun dreamDao(): DreamDao

    companion object {
        @Volatile
        private var _instance: DreamDatabase? = null

        fun getInstance(context: Context) = _instance ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context,
                DreamDatabase::class.java,
                "dream_database"
            ).addMigrations(MIGRATION_1_2).build()

            _instance = instance
            instance
        }
    }
}