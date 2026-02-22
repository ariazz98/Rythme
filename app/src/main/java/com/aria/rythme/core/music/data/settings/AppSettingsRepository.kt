package com.aria.rythme.core.music.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aria.rythme.core.utils.RythmeLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * DataStore 实例
 */
private val Context.scanSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings"
)

/**
 * 应用配置项仓库
 *
 * 使用 DataStore 持久化用户的配置。
 *
 * ## 使用示例
 * ```kotlin
 * val repository = ScanSettingsRepository(context)
 * 
 * // 获取当前配置
 * repository.settings.collect { settings ->
 *     // 使用配置
 * }
 * 
 * // 更新最小时长
 * repository.updateMinDuration(60_000L)  // 60秒
 * 
 * // 更新最小大小
 * repository.updateMinSize(200 * 1024L)  // 200KB
 * ```
 *
 * @param context 应用上下文
 */
class AppSettingsRepository(private val context: Context) {

    /**
     * 扫描设置流
     *
     * 当配置发生变化时会自动发射新值
     */
    val settings: Flow<ScanSettings> = context.scanSettingsDataStore.data
        .catch { exception ->
            RythmeLogger.e(TAG, "读取扫描设置失败", exception)
            emit(emptyPreferences())
        }
        .map { preferences ->
            ScanSettings(
                minDurationMs = preferences[KEY_MIN_DURATION_MS] 
                    ?: ScanSettings.DEFAULT_MIN_DURATION_MS,
                minSizeBytes = preferences[KEY_MIN_SIZE_BYTES] 
                    ?: ScanSettings.DEFAULT_MIN_SIZE_BYTES,
                excludeSystemDirs = preferences[KEY_EXCLUDE_SYSTEM_DIRS] ?: true
            )
        }

    /**
     * 更新最小时长
     *
     * @param durationMs 最小时长（毫秒）
     */
    suspend fun updateMinDuration(durationMs: Long) {
        RythmeLogger.d(TAG, "更新最小时长: ${durationMs}ms")
        context.scanSettingsDataStore.edit { preferences ->
            preferences[KEY_MIN_DURATION_MS] = durationMs
        }
    }

    /**
     * 更新最小大小
     *
     * @param sizeBytes 最小大小（字节）
     */
    suspend fun updateMinSize(sizeBytes: Long) {
        RythmeLogger.d(TAG, "更新最小大小: ${sizeBytes}bytes")
        context.scanSettingsDataStore.edit { preferences ->
            preferences[KEY_MIN_SIZE_BYTES] = sizeBytes
        }
    }

    /**
     * 更新是否排除系统目录
     *
     * @param exclude 是否排除
     */
    suspend fun updateExcludeSystemDirs(exclude: Boolean) {
        RythmeLogger.d(TAG, "更新排除系统目录: $exclude")
        context.scanSettingsDataStore.edit { preferences ->
            preferences[KEY_EXCLUDE_SYSTEM_DIRS] = exclude
        }
    }

    /**
     * 更新全部设置
     *
     * @param settings 新的扫描设置
     */
    suspend fun updateSettings(settings: ScanSettings) {
        RythmeLogger.d(TAG, "更新扫描设置: $settings")
        context.scanSettingsDataStore.edit { preferences ->
            preferences[KEY_MIN_DURATION_MS] = settings.minDurationMs
            preferences[KEY_MIN_SIZE_BYTES] = settings.minSizeBytes
            preferences[KEY_EXCLUDE_SYSTEM_DIRS] = settings.excludeSystemDirs
        }
    }

    /**
     * 重置为默认设置
     */
    suspend fun resetToDefaults() {
        RythmeLogger.d(TAG, "重置扫描设置为默认值")
        context.scanSettingsDataStore.edit { preferences ->
            preferences.clear()
        }
    }

    companion object {
        private const val TAG = "ScanSettingsRepo"
        
        private val KEY_MIN_DURATION_MS = longPreferencesKey("min_duration_ms")
        private val KEY_MIN_SIZE_BYTES = longPreferencesKey("min_size_bytes")
        private val KEY_EXCLUDE_SYSTEM_DIRS = booleanPreferencesKey("exclude_system_dirs")
    }
}
