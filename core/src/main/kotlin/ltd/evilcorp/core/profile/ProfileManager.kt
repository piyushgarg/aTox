package ltd.evilcorp.core.profile

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import ltd.evilcorp.domain.features.auth.model.ProfileInfo
import androidx.core.content.edit

object ProfileManager {
    private const val PREFS_NAME = "atox_multi_profiles"
    private const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"
    private const val KEY_PROFILES_JSON = "profiles_json"
    private const val KEY_SHOW_PICKER = "show_picker_on_startup"
    private const val KEY_MIGRATION_DONE = "migration_done"

    const val DEFAULT_PROFILE_ID = "default"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Synchronized
    fun getActiveProfileId(context: Context): String {
        val prefs = getPrefs(context)
        var active = prefs.getString(KEY_ACTIVE_PROFILE_ID, DEFAULT_PROFILE_ID) ?: DEFAULT_PROFILE_ID
        
        // Migration logic for existing single-profile users
        if (active == DEFAULT_PROFILE_ID && !prefs.getBoolean(KEY_MIGRATION_DONE, false)) {
            val toxFiles = context.filesDir.listFiles()?.filter { it.extension == "tox" }?.map { it.nameWithoutExtension } ?: emptyList()
            if (toxFiles.isNotEmpty()) {
                active = toxFiles.first()
                prefs.edit(commit = true) {
                    putString(KEY_ACTIVE_PROFILE_ID, active)
                    putBoolean(KEY_MIGRATION_DONE, true)
                }
                
                // Rename Database
                val dbFile = context.getDatabasePath("core_db")
                if (dbFile.exists()) {
                    dbFile.renameTo(context.getDatabasePath("core_db_$active"))
                    val walFile = context.getDatabasePath("core_db-wal")
                    if (walFile.exists()) walFile.renameTo(context.getDatabasePath("core_db_${active}-wal"))
                    val shmFile = context.getDatabasePath("core_db-shm")
                    if (shmFile.exists()) shmFile.renameTo(context.getDatabasePath("core_db_${active}-shm"))
                }
                
                // Rename DataStore
                val dsFile = File(context.filesDir, "datastore/user_settings.preferences_pb")
                if (dsFile.exists()) {
                    dsFile.renameTo(File(context.filesDir, "datastore/user_settings_$active.preferences_pb"))
                }

                // Create the profile entry
                val list = getProfiles(context).toMutableList()
                list.removeAll { it.id == DEFAULT_PROFILE_ID }
                val defaultName = context.getString(ltd.evilcorp.core.R.string.profile_default_name)
                list.add(ProfileInfo(id = active, name = defaultName))
                saveProfiles(context, list)
            } else {
                // No tox files found on startup, mark migration as done so we don't trigger it during new profile creation
                prefs.edit(commit = true) {
                    putBoolean(KEY_MIGRATION_DONE, true)
                }
            }
        }
        return active
    }

    fun setActiveProfileId(context: Context, id: String) {
        getPrefs(context).edit(commit = true) { putString(KEY_ACTIVE_PROFILE_ID, id) }
    }

    fun getShowProfilePicker(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SHOW_PICKER, false)
    }

    fun setShowProfilePicker(context: Context, show: Boolean) {
        getPrefs(context).edit(commit = true) { putBoolean(KEY_SHOW_PICKER, show) }
    }

    fun getProfiles(context: Context): List<ProfileInfo> {
        val jsonStr = getPrefs(context).getString(KEY_PROFILES_JSON, "[]") ?: "[]"
        val array = JSONArray(jsonStr)
        val list = mutableListOf<ProfileInfo>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                ProfileInfo(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    avatarUri = if (obj.has("avatarUri")) obj.getString("avatarUri") else null
                )
            )
        }
        return list
    }

    fun saveProfiles(context: Context, profiles: List<ProfileInfo>) {
        val array = JSONArray()
        for (profile in profiles) {
            val obj = JSONObject()
            obj.put("id", profile.id)
            obj.put("name", profile.name)
            if (profile.avatarUri != null) {
                obj.put("avatarUri", profile.avatarUri)
            }
            array.put(obj)
        }
        getPrefs(context).edit(commit = true) { putString(KEY_PROFILES_JSON, array.toString()) }
    }

    fun addOrUpdateProfile(context: Context, profile: ProfileInfo) {
        val profiles = getProfiles(context).toMutableList()
        val index = profiles.indexOfFirst { it.id == profile.id }
        if (index >= 0) {
            profiles[index] = profile
        } else {
            profiles.add(profile)
        }
        saveProfiles(context, profiles)
    }

    fun removeProfile(context: Context, id: String) {
        val profiles = getProfiles(context).toMutableList()
        profiles.removeAll { it.id == id }
        saveProfiles(context, profiles)
    }

    private fun renameWithRetry(from: File, to: File, maxRetries: Int = 10, delayMs: Long = 50): Boolean {
        if (!from.exists()) return true
        for (i in 0 until maxRetries) {
            if (from.renameTo(to)) {
                android.util.Log.d("ProfileManager", "Successfully renamed ${from.name} to ${to.name} on attempt ${i + 1}")
                return true
            }
            android.util.Log.w("ProfileManager", "Failed to rename ${from.name} to ${to.name} on attempt ${i + 1}, retrying...")
            try {
                Thread.sleep(delayMs)
            } catch (ignored: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
        }
        android.util.Log.e("ProfileManager", "Failed to rename ${from.name} to ${to.name} after $maxRetries attempts")
        return false
    }

    fun renameProfileFiles(context: Context, oldId: String, newId: String) {
        android.util.Log.i("ProfileManager", "renameProfileFiles starting: oldId=$oldId, newId=$newId")
        if (oldId == newId) return
        
        val oldDbName = if (oldId == DEFAULT_PROFILE_ID) "core_db" else "core_db_$oldId"
        val newDbName = if (newId == DEFAULT_PROFILE_ID) "core_db" else "core_db_$newId"

        // Rename Database files
        val oldDbFile = context.getDatabasePath(oldDbName)
        val newDbFile = context.getDatabasePath(newDbName)
        android.util.Log.i("ProfileManager", "Renaming database file ${oldDbFile.absolutePath} (exists=${oldDbFile.exists()}) to ${newDbFile.absolutePath}")
        renameWithRetry(oldDbFile, newDbFile)
        
        val oldWalFile = context.getDatabasePath("$oldDbName-wal")
        val newWalFile = context.getDatabasePath("$newDbName-wal")
        android.util.Log.i("ProfileManager", "Renaming WAL file ${oldWalFile.absolutePath} (exists=${oldWalFile.exists()}) to ${newWalFile.absolutePath}")
        renameWithRetry(oldWalFile, newWalFile)
        
        val oldShmFile = context.getDatabasePath("$oldDbName-shm")
        val newShmFile = context.getDatabasePath("$newDbName-shm")
        android.util.Log.i("ProfileManager", "Renaming SHM file ${oldShmFile.absolutePath} (exists=${oldShmFile.exists()}) to ${newShmFile.absolutePath}")
        renameWithRetry(oldShmFile, newShmFile)
        
        // Rename DataStore files
        val oldDsName = if (oldId == DEFAULT_PROFILE_ID) "user_settings" else "user_settings_$oldId"
        val newDsName = if (newId == DEFAULT_PROFILE_ID) "user_settings" else "user_settings_$newId"
        val oldDsFile = File(context.filesDir, "datastore/$oldDsName.preferences_pb")
        val newDsFile = File(context.filesDir, "datastore/$newDsName.preferences_pb")
        android.util.Log.i("ProfileManager", "Renaming DataStore file ${oldDsFile.absolutePath} (exists=${oldDsFile.exists()}) to ${newDsFile.absolutePath}")
        renameWithRetry(oldDsFile, newDsFile)

        // Rename Avatar files
        val oldAvatarJpg = File(context.filesDir, "self_avatar_$oldId.jpg")
        val newAvatarJpg = File(context.filesDir, "self_avatar_$newId.jpg")
        renameWithRetry(oldAvatarJpg, newAvatarJpg)
        
        val oldAvatarPng = File(context.filesDir, "self_avatar_$oldId.png")
        val newAvatarPng = File(context.filesDir, "self_avatar_$newId.png")
        renameWithRetry(oldAvatarPng, newAvatarPng)
    }
}
