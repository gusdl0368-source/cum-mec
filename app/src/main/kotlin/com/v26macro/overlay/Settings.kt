package com.v26macro.overlay

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.v26macro.runner.TaskKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("v26macro")

object MacroSettings {
    private val ENABLED_TASKS = stringSetPreferencesKey("enabled_tasks")

    fun enabledTasks(ctx: Context): Flow<Set<TaskKind>> = ctx.dataStore.data.map { prefs ->
        val raw = prefs[ENABLED_TASKS]
        if (raw == null) TaskKind.values().toSet()
        else raw.mapNotNull { name -> runCatching { TaskKind.valueOf(name) }.getOrNull() }.toSet()
    }

    suspend fun setEnabled(ctx: Context, tasks: Set<TaskKind>) {
        ctx.dataStore.edit { prefs ->
            prefs[ENABLED_TASKS] = tasks.map { it.name }.toSet()
        }
    }
}
