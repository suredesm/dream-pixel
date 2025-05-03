package lk.sure.dream.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import lk.sure.dream.data.DreamDatabase
import lk.sure.dream.data.dao.DreamDao
import lk.sure.dream.data.repos.DreamRepository
import lk.sure.dream.data.repos.PaletteRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    @Singleton
    fun providesDreamDao(
        @ApplicationContext context: Context
    ) = DreamDatabase.getInstance(context).dreamDao()

    @Provides
    @Singleton
    fun providesDreamRepo(
        @ApplicationContext context: Context,
        dreamDao: DreamDao
    ) = DreamRepository(context, dreamDao)

    @Provides
    @Singleton
    fun providesColorPaletteRepo(
        dreamDao: DreamDao,
    ) = PaletteRepository(dreamDao)
}