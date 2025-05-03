package lk.sure.dream.data.repos

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import lk.sure.dream.dataStore
import javax.inject.Inject

class PreferenceRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore = context.dataStore

    private val privacyPolicyAgreedKey = booleanPreferencesKey("privacy_policy_agreed")
    private val dreamBackupFolderUriKey = stringPreferencesKey("dream_backup_folder_uri")

    val isPrivacyPolicyAgreed = dataStore.data.map { it[privacyPolicyAgreedKey] ?: false }

    suspend fun agreeToPrivacyPolicy() {
        dataStore.edit { it[privacyPolicyAgreedKey] = true }
    }

    suspend fun loadDreamBackupFolderUri(): Uri? {
        val prefs = dataStore.data.first()
        return prefs[dreamBackupFolderUriKey]?.let { Uri.parse(it) }
    }

    suspend fun saveDreamBackupFolderUri(uri: Uri) {
        dataStore.edit { it[dreamBackupFolderUriKey] = uri.toString() }
    }
}