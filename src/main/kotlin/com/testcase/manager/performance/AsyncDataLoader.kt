package com.testcase.manager.performance

import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.CoroutineContext

/**
 * 异步数据加载器
 * 用于高效加载大量数据
 */
class AsyncDataLoader : CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    // 加载队列
    private val loadQueue = ConcurrentLinkedQueue<LoadRequest>()

    // 正在进行的加载任务
    private val activeLoads = mutableMapOf<Int, Job>()

    // 加载完成回调
    var onDataLoaded: ((requestId: Int, data: List<Array<Any?>>) -> Unit)? = null

    // 加载进度回调
    var onProgressUpdate: ((loaded: Int, total: Int) -> Unit)? = null

    // 错误回调
    var onError: ((error: Throwable) -> Unit)? = null

    // 是否正在加载
    val isLoading: Boolean
        get() = activeLoads.isNotEmpty()

    // 加载配置
    var batchSize = 100
    var maxConcurrentLoads = 3
    var loadDelayMs = 50L

    /**
     * 加载请求
     */
    data class LoadRequest(
        val id: Int,
        val startRow: Int,
        val endRow: Int,
        val dataProvider: suspend (start: Int, end: Int) -> List<Array<Any?>>
    )

    /**
     * 请求加载数据
     */
    fun requestLoad(
        startRow: Int,
        endRow: Int,
        dataProvider: suspend (start: Int, end: Int) -> List<Array<Any?>>
    ): Int {
        val requestId = generateRequestId()
        val request = LoadRequest(requestId, startRow, endRow, dataProvider)

        loadQueue.offer(request)
        processQueue()

        return requestId
    }

    /**
     * 取消加载请求
     */
    fun cancelLoad(requestId: Int) {
        activeLoads[requestId]?.cancel()
        activeLoads.remove(requestId)

        // 从队列中移除
        loadQueue.removeIf { it.id == requestId }
    }

    /**
     * 取消所有加载
     */
    fun cancelAll() {
        activeLoads.values.forEach { it.cancel() }
        activeLoads.clear()
        loadQueue.clear()
    }

    /**
     * 处理加载队列
     */
    private fun processQueue() {
        if (activeLoads.size >= maxConcurrentLoads) return

        val request = loadQueue.poll() ?: return

        val job = launch {
            try {
                delay(loadDelayMs)

                val data = withContext(Dispatchers.IO) {
                    request.dataProvider(request.startRow, request.endRow)
                }

                withContext(Dispatchers.Main) {
                    onDataLoaded?.invoke(request.id, data)
                }
            } catch (e: CancellationException) {
                // 正常取消，忽略
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError?.invoke(e)
                }
            } finally {
                activeLoads.remove(request.id)
                processQueue()
            }
        }

        activeLoads[request.id] = job
    }

    /**
     * 批量加载数据
     */
    fun loadBatch(
        totalRows: Int,
        dataProvider: suspend (start: Int, end: Int) -> List<Array<Any?>>
    ): Int {
        val requestId = generateRequestId()
        var loadedCount = 0

        val job = launch {
            try {
                val batches = (0 until totalRows step batchSize).map { start ->
                    val end = (start + batchSize).coerceAtMost(totalRows)
                    start to end
                }

                for ((start, end) in batches) {
                    val data = withContext(Dispatchers.IO) {
                        dataProvider(start, end)
                    }

                    loadedCount += data.size

                    withContext(Dispatchers.Main) {
                        onDataLoaded?.invoke(requestId, data)
                        onProgressUpdate?.invoke(loadedCount, totalRows)
                    }

                    delay(loadDelayMs)
                }
            } catch (e: CancellationException) {
                // 正常取消
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError?.invoke(e)
                }
            } finally {
                activeLoads.remove(requestId)
            }
        }

        activeLoads[requestId] = job
        return requestId
    }

    /**
     * 预加载数据
     */
    fun preload(
        centerRow: Int,
        range: Int,
        totalRows: Int,
        dataProvider: suspend (start: Int, end: Int) -> List<Array<Any?>>
    ): Int {
        val startRow = (centerRow - range).coerceAtLeast(0)
        val endRow = (centerRow + range).coerceAtMost(totalRows - 1)
        return requestLoad(startRow, endRow, dataProvider)
    }

    /**
     * 释放资源
     */
    fun dispose() {
        cancelAll()
        job.cancel()
    }

    /**
     * 生成请求 ID
     */
    private fun generateRequestId(): Int {
        return (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
    }

    companion object {
        /**
         * 创建数据加载器
         */
        fun create(configure: (AsyncDataLoader.() -> Unit)? = null): AsyncDataLoader {
            return AsyncDataLoader().apply {
                configure?.invoke(this)
            }
        }
    }
}

/**
 * 加载状态
 */
sealed class LoadState {
    object Idle : LoadState()
    object Loading : LoadState()
    data class Progress(val loaded: Int, val total: Int) : LoadState()
    data class Success(val data: List<Array<Any?>>) : LoadState()
    data class Error(val exception: Throwable) : LoadState()
}
