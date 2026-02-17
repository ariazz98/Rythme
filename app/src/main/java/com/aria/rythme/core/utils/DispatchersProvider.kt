package com.aria.rythme.core.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * 协程调度器提供者接口
 *
 * 提供统一的协程调度器访问接口，便于在测试时替换为测试调度器。
 * 这是依赖注入和单元测试的最佳实践。
 *
 * ## 为什么需要 DispatchersProvider？
 * 1. **可测试性**：在单元测试中可以替换为 TestDispatcher，实现确定性测试
 * 2. **依赖注入**：通过 DI 容器注入，便于管理和替换
 * 3. **统一管理**：集中管理所有协程调度器，避免硬编码
 * 4. **灵活性**：可以根据需要提供自定义调度器
 *
 * ## 调度器说明
 * - **Main**: 主线程调度器，用于 UI 更新和轻量级任务
 * - **IO**: IO 线程池，用于网络请求、文件读写、数据库操作
 * - **Default**: CPU 密集型线程池，用于复杂计算、数据处理
 * - **Unconfined**: 不限定线程，立即在当前线程执行（谨慎使用）
 *
 * ## 使用场景
 * ```kotlin
 * class MyViewModel(
 *     private val dispatchers: DispatchersProvider
 * ) : BaseViewModel<...>() {
 *
 *     override fun handleIntent(intent: MyIntent) {
 *         viewModelScope.launch(dispatchers.io) {
 *             // 在 IO 线程执行网络请求
 *             val data = apiService.getData()
 *
 *             withContext(dispatchers.default) {
 *                 // 在 Default 线程处理数据
 *                 val processed = processData(data)
 *             }
 *
 *             // 自动切换回 Main 线程更新 UI
 *             reduce(MyAction.DataLoaded(processed))
 *         }
 *     }
 * }
 * ```
 *
 * ## 单元测试示例
 * ```kotlin
 * class MyViewModelTest {
 *     // 测试调度器
 *     private val testDispatcher = StandardTestDispatcher()
 *     private val testDispatchers = object : DispatchersProvider {
 *         override val main = testDispatcher
 *         override val io = testDispatcher
 *         override val default = testDispatcher
 *         override val unconfined = testDispatcher
 *     }
 *
 *     @Test
 *     fun `test loading data`() = runTest(testDispatcher) {
 *         val viewModel = MyViewModel(testDispatchers)
 *         viewModel.sendIntent(MyIntent.LoadData)
 *         // 测试断言
 *     }
 * }
 * ```
 */
interface DispatchersProvider {
    /**
     * 主线程调度器
     *
     * 用于 UI 更新和轻量级任务。
     * 所有 UI 相关的操作都应该在此调度器上执行。
     *
     * 使用场景：
     * - 更新 UI 状态
     * - 显示 Toast/Dialog
     * - 触发动画
     * - 轻量级计算（< 16ms）
     */
    val main: CoroutineDispatcher

    /**
     * IO 线程池调度器
     *
     * 用于 IO 密集型操作，如网络请求、文件读写、数据库操作。
     * 此调度器针对 IO 操作进行了优化，线程池大小动态调整。
     *
     * 使用场景：
     * - 网络请求（HTTP、WebSocket）
     * - 文件读写操作
     * - 数据库查询和更新
     * - SharedPreferences 读写
     * - 序列化/反序列化
     */
    val io: CoroutineDispatcher

    /**
     * CPU 密集型线程池调度器
     *
     * 用于 CPU 密集型计算任务。
     * 线程池大小等于 CPU 核心数，适合并行计算。
     *
     * 使用场景：
     * - 复杂数学计算
     * - 图像处理
     * - 大数据排序/过滤
     * - 加密/解密
     * - 数据转换和映射
     */
    val default: CoroutineDispatcher

    /**
     * 不限定线程的调度器
     *
     * 立即在当前线程执行，不进行线程切换。
     * 谨慎使用，可能导致意外的线程行为。
     *
     * 使用场景：
     * - 性能关键的纯计算（无 IO 操作）
     * - 测试场景
     * - 明确知道当前线程安全的场景
     */
    val unconfined: CoroutineDispatcher
}

/**
 * 默认的协程调度器提供者实现
 *
 * 提供 Kotlin 协程的标准调度器。
 * 在生产环境中使用，通过 Koin 注入到 ViewModel 中。
 *
 * ## 注入配置
 * ```kotlin
 * // 在 Koin 模块中
 * single<DispatchersProvider> { DefaultDispatchersProvider() }
 * ```
 */
class DefaultDispatchersProvider : DispatchersProvider {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
    override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
}

/**
 * 测试用的协程调度器提供者
 *
 * 所有调度器都使用同一个测试调度器，便于控制协程执行和时间。
 * 仅用于单元测试。
 *
 * ## 使用示例
 * ```kotlin
 * @Test
 * fun myTest() = runTest {
 *     val testDispatchers = TestDispatchersProvider(this.testScheduler)
 *     val viewModel = MyViewModel(testDispatchers)
 *     // 测试代码
 * }
 * ```
 *
 * @param testDispatcher 测试调度器，通常是 StandardTestDispatcher 或 UnconfinedTestDispatcher
 */
class TestDispatchersProvider(
    private val testDispatcher: CoroutineDispatcher
) : DispatchersProvider {
    override val main: CoroutineDispatcher = testDispatcher
    override val io: CoroutineDispatcher = testDispatcher
    override val default: CoroutineDispatcher = testDispatcher
    override val unconfined: CoroutineDispatcher = testDispatcher
}
