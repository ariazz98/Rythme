package com.aria.rythme.feature.pitch.data

import android.content.Context
import android.net.Uri
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ModelPhase { Checking, Missing, Paused, Downloading, Importing, Verifying, Ready, Failed }

data class ModelDownloadState(
    val phase: ModelPhase = ModelPhase.Checking,
    val bytes: Long = 0,
    val message: String? = null,
    val importing: Boolean = false
) {
    val transferring get() = phase in listOf(ModelPhase.Downloading, ModelPhase.Importing, ModelPhase.Verifying)
}

/** 服务和页面共享状态；离开页面不终止下载，重新进程启动仅检查文件，不自动联网。 */
class MelodyModelManager(private val context: Context, private val store: MelodyModelStore) {
    private val mutable = MutableStateFlow(ModelDownloadState())
    val state = mutable.asStateFlow()
    val totalBytes get() = store.spec.bytes

    suspend fun refresh() {
        if (mutable.value.transferring) return
        try {
            val status = store.status()
            mutable.update { old -> if (old.transferring) old else ModelDownloadState(
                phase = when { status.installed -> ModelPhase.Ready; status.downloadedBytes > 0 -> ModelPhase.Paused; else -> ModelPhase.Missing },
                bytes = status.downloadedBytes
            ) }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            failed(error.message ?: "无法读取模型文件")
        }
    }

    fun preparing() { mutable.update { it.copy(phase = ModelPhase.Downloading, message = null) } }
    fun failed(message: String) { mutable.update { it.copy(phase = ModelPhase.Failed, message = message) } }
    fun interrupt() = store.cancelTransfer()
    fun interrupted() { mutable.update { if (it.transferring) it.copy(phase = ModelPhase.Paused, message = null) else it } }

    suspend fun download(allowMetered: Boolean) {
        preparing()
        try {
            store.download(networkAllowed = {
                val network = context.getSystemService(ConnectivityManager::class.java)
                val caps = network.getNetworkCapabilities(network.activeNetwork)
                caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                    (allowMetered || !network.isActiveNetworkMetered)
            }) { bytes, verifying ->
                mutable.value = ModelDownloadState(if (verifying) ModelPhase.Verifying else ModelPhase.Downloading, bytes)
            }
            mutable.value = ModelDownloadState(ModelPhase.Ready, totalBytes)
        } catch (cancelled: CancellationException) { interrupted(); throw cancelled }
        catch (error: Exception) {
            val remaining = runCatching { store.status().downloadedBytes }.getOrDefault(mutable.value.bytes)
            mutable.value = ModelDownloadState(ModelPhase.Failed, remaining, downloadErrorMessage(error))
        }
    }

    suspend fun importFile(uri: Uri) {
        mutable.value = ModelDownloadState(ModelPhase.Importing, importing = true)
        try {
            store.importFile(open = { context.contentResolver.openInputStream(uri) ?: error("无法读取所选文件") }) { bytes, verifying ->
                mutable.value = ModelDownloadState(if (verifying) ModelPhase.Verifying else ModelPhase.Importing, bytes, importing = true)
            }
            mutable.value = ModelDownloadState(ModelPhase.Ready, totalBytes)
        } catch (error: Exception) {
            val cancelled = error is CancellationException || !currentCoroutineContext().isActive
            withContext(NonCancellable) {
                val remaining = store.status()
                mutable.value = ModelDownloadState(when {
                    remaining.installed -> ModelPhase.Ready
                    !cancelled -> ModelPhase.Failed
                    remaining.downloadedBytes > 0 -> ModelPhase.Paused
                    else -> ModelPhase.Missing
                }, remaining.downloadedBytes, if (cancelled) null else error.message ?: "导入失败，请重新选择文件")
            }
            if (cancelled) throw CancellationException("Import cancelled", error)
        }
    }

    suspend fun delete() {
        if (mutable.value.transferring) return
        mutable.update { it.copy(phase = ModelPhase.Checking) }
        try { store.deleteModel(); mutable.value = ModelDownloadState(ModelPhase.Missing) }
        catch (error: Exception) {
            if (error is CancellationException) throw error
            failed(error.message ?: "删除失败，请重试")
        }
    }
}

internal fun downloadErrorMessage(error: Exception): String = when (error) {
    is java.net.SocketTimeoutException -> "连接超时，已保留下载进度，请稍后重试"
    is java.net.UnknownHostException -> "无法解析模型下载地址，请检查网络后重试"
    is java.net.SocketException, is javax.net.ssl.SSLException -> "无法连接模型下载源，请检查网络或稍后重试"
    else -> error.message ?: "下载失败，请稍后重试"
}
