package app.vinilogs.core.data.di

import android.content.Context
import androidx.room.Room
import app.vinilogs.core.data.local.RecordDao
import app.vinilogs.core.data.local.VinilogsDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides the app's single Room database (T-10, ADR-2) and its DAO. */
@Module
@InstallIn(SingletonComponent::class)
object RoomModule {
    private const val DATABASE_NAME = "vinilogs.db"

    @Provides
    @Singleton
    fun provideVinilogsDatabase(
        @ApplicationContext context: Context,
    ): VinilogsDatabase =
        Room.databaseBuilder(context, VinilogsDatabase::class.java, DATABASE_NAME).build()

    @Provides
    @Singleton
    fun provideRecordDao(database: VinilogsDatabase): RecordDao = database.recordDao()
}
