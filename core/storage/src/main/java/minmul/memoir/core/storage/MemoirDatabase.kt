package minmul.memoir.core.storage

import android.content.Context
import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

@Database(
    entities = [
        ItemEntity::class,
        AnalysisJobEntity::class,
        AnalysisResultEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@ColumnTypeConverters(MemoirColumnConverters::class)
abstract class MemoirDatabase : RoomDatabase() {
    abstract fun analysisWorkDao(): AnalysisWorkDao
    abstract fun itemDao(): ItemDao
    abstract fun analysisJobDao(): AnalysisJobDao
    abstract fun analysisResultDao(): AnalysisResultDao
    abstract fun contentWriteDao(): ContentWriteDao

    companion object {
        fun create(context: Context): MemoirDatabase {
            val dbFile = context.getDatabasePath(FILE_NAME)
            return Room.databaseBuilder<MemoirDatabase>(
                context = context,
                name = dbFile.absolutePath,
            )
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        }

        fun createInMemory(queryContext: CoroutineContext): MemoirDatabase {
            return Room.inMemoryDatabaseBuilder<MemoirDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(queryContext)
                .build()
        }

        private const val FILE_NAME = "memoir.db"
    }
}
