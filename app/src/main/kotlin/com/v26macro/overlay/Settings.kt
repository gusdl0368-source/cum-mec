package com.v26macro.overlay

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("v26macro")

/** 매크로 동작 설정. UI 토글/스테퍼와 1:1로 매핑된다. */
data class MacroConfig(
    val sponsorEnabled: Boolean = true,
    val pointShopEnabled: Boolean = true,
    val homerunCount: Int = 8,        // 0~8, 0이면 스킵
    val specialMatchCount: Int = 3,   // 0~5, 0이면 스킵
    val rankingEnabled: Boolean = false,
    val leagueEnabled: Boolean = false,
)

object MacroSettings {
    private val SPONSOR_ENABLED = booleanPreferencesKey("sponsor_enabled")
    private val POINTSHOP_ENABLED = booleanPreferencesKey("pointshop_enabled")
    private val HOMERUN_COUNT = intPreferencesKey("homerun_count")
    private val SPECIALMATCH_COUNT = intPreferencesKey("specialmatch_count")
    private val RANKING_ENABLED = booleanPreferencesKey("ranking_enabled")
    private val LEAGUE_ENABLED = booleanPreferencesKey("league_enabled")

    const val HOMERUN_MAX = 8
    const val SPECIALMATCH_MAX = 5

    fun config(ctx: Context): Flow<MacroConfig> = ctx.dataStore.data.map { p ->
        MacroConfig(
            sponsorEnabled = p[SPONSOR_ENABLED] ?: true,
            pointShopEnabled = p[POINTSHOP_ENABLED] ?: true,
            homerunCount = (p[HOMERUN_COUNT] ?: HOMERUN_MAX).coerceIn(0, HOMERUN_MAX),
            specialMatchCount = (p[SPECIALMATCH_COUNT] ?: 3).coerceIn(0, SPECIALMATCH_MAX),
            rankingEnabled = p[RANKING_ENABLED] ?: false,
            leagueEnabled = p[LEAGUE_ENABLED] ?: false,
        )
    }

    suspend fun snapshot(ctx: Context): MacroConfig = config(ctx).first()

    suspend fun update(ctx: Context, transform: (MacroConfig) -> MacroConfig) {
        val current = snapshot(ctx)
        val next = transform(current)
        ctx.dataStore.edit { p ->
            p[SPONSOR_ENABLED] = next.sponsorEnabled
            p[POINTSHOP_ENABLED] = next.pointShopEnabled
            p[HOMERUN_COUNT] = next.homerunCount.coerceIn(0, HOMERUN_MAX)
            p[SPECIALMATCH_COUNT] = next.specialMatchCount.coerceIn(0, SPECIALMATCH_MAX)
            p[RANKING_ENABLED] = next.rankingEnabled
            p[LEAGUE_ENABLED] = next.leagueEnabled
        }
    }
}
