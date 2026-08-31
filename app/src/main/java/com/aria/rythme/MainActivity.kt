package com.aria.rythme

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.aria.rythme.core.music.data.indexer.MusicIndexer
import com.aria.rythme.ui.theme.RythmeTheme
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/** Rythme 的 Compose 入口；功能架构约定见仓库根目录 AGENTS.md。 */
class MainActivity : ComponentActivity() {

    private val musicIndexer: MusicIndexer by inject()

    /**
     * 权限请求启动器
     */
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, R.string.permission_granted, Toast.LENGTH_SHORT).show()
            triggerMusicScan()
        } else {
            Toast.makeText(this, R.string.permission_required, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 检查并请求权限
        checkAndRequestPermission()

        setContent {
            RythmeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RythmeApp()
                }
            }
        }
    }

    /**
     * 检查并请求音频权限
     */
    private fun checkAndRequestPermission() {
        val permission = Manifest.permission.READ_MEDIA_AUDIO

        when {
            ContextCompat.checkSelfPermission(this, permission) ==
                    PackageManager.PERMISSION_GRANTED -> {
                // 权限已授予，触发扫描（应用重启时走这里）
                triggerMusicScan()
            }
            shouldShowRequestPermissionRationale(permission) -> {
                // 显示权限说明
                Toast.makeText(this, R.string.permission_required_storage, Toast.LENGTH_LONG).show()
                permissionLauncher.launch(permission)
            }
            else -> {
                // 直接请求权限
                permissionLauncher.launch(permission)
            }
        }
    }

    /**
     * 触发音乐扫描
     *
     * 权限确认后调用，MusicRepository 内部有防抖，重复调用安全。
     */
    private fun triggerMusicScan() {
        lifecycleScope.launch {
            musicIndexer.initialize()
        }
    }
}
