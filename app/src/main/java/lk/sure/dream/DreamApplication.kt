package lk.sure.dream

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class DreamApplication: Application()

val Context.dataStore by preferencesDataStore(name = "preference")