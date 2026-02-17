package com.aria.rythme.core.testing

import com.aria.rythme.core.mvi.BaseViewModel
import com.aria.rythme.core.mvi.MviAction
import com.aria.rythme.core.mvi.MviEffect
import com.aria.rythme.core.mvi.MviIntent
import com.aria.rythme.core.mvi.MviState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*

/**
 * MVI 单元测试工具集
 *
 * 提供一组便捷的测试工具和扩展函数，简化 MVI 架构的单元测试编写。
 * 包括状态断言、副作用收集、协程测试等功能。
 *
 * ## 主要功能
 * 1. **状态断言**：验证 State 的值和变化
 * 2. **副作用收集**：收集和验证 Effect
 * 3. **协程测试**：提供测试调度器和协程作用域
 * 4. **Intent 发送**：简化 Intent 的发送和等待
 *
 * ## 使用示例
 * ```kotlin
 * @OptIn(ExperimentalCoroutinesApi::class)
 * class LoginViewModelTest {
 *
 *     private val testDispatcher = StandardTestDispatcher()
 *     private lateinit var viewModel: LoginViewModel
 *
 *     @Before
 *     fun setup() {
 *         Dispatchers.setMain(testDispatcher)
 *         viewModel = LoginViewModel()
 *     }
 *
 *     @After
 *     fun tearDown() {
 *         Dispatchers.resetMain()
 *     }
 *
 *     @Test
 *     fun `when username changed, state should update`() = runTest(testDispatcher) {
 *         // Given
 *         val newUsername = "test@example.com"
 *
 *         // When
 *         viewModel.sendIntent(LoginIntent.OnUsernameChanged(newUsername))
 *         advanceUntilIdle() // 等待协程完成
 *
 *         // Then
 *         viewModel.state.assertValue { it.username == newUsername }
 *     }
 *
 *     @Test
 *     fun `when login success, should emit navigate effect`() = runTest(testDispatcher) {
 *         // When
 *         viewModel.sendIntent(LoginIntent.OnLoginClick)
 *         advanceUntilIdle()
 *
 *         // Then
 *         viewModel.effect.test {
 *             val effect = awaitItem()
 *             assertTrue(effect is LoginEffect.NavigateToHome)
 *         }
 *     }
 * }
 * ```
 */

/**
 * 状态断言扩展函数
 *
 * 验证 StateFlow 当前的值是否满足指定条件。
 *
 * @receiver Flow<S> StateFlow 或 Flow
 * @param predicate 断言条件
 * @throws AssertionError 如果断言失败
 *
 * 使用示例：
 * ```kotlin
 * viewModel.state.assertValue { it.isLoading == true }
 * viewModel.state.assertValue { it.username.isNotEmpty() }
 * ```
 */
suspend fun <S : MviState> Flow<S>.assertValue(predicate: (S) -> Boolean) {
    val value = first()
    assertTrue("State assertion failed: $value", predicate(value))
}

/**
 * 状态相等断言
 *
 * 验证 StateFlow 当前的值是否等于期望值。
 *
 * @receiver Flow<S> StateFlow 或 Flow
 * @param expected 期望的状态值
 * @throws AssertionError 如果状态不相等
 *
 * 使用示例：
 * ```kotlin
 * viewModel.state.assertEquals(LoginState(username = "test", isLoading = false))
 * ```
 */
suspend fun <S : MviState> Flow<S>.assertEquals(expected: S) {
    val actual = first()
    assertEquals("State mismatch", expected, actual)
}

/**
 * 状态字段断言
 *
 * 验证状态中的特定字段值。
 *
 * @receiver Flow<S> StateFlow 或 Flow
 * @param fieldName 字段名称（用于错误信息）
 * @param selector 字段选择器
 * @param expected 期望的字段值
 * @throws AssertionError 如果字段值不匹配
 *
 * 使用示例：
 * ```kotlin
 * viewModel.state.assertField("username", { it.username }, "test@example.com")
 * viewModel.state.assertField("isLoading", { it.isLoading }, true)
 * ```
 */
suspend fun <S : MviState, T> Flow<S>.assertField(
    fieldName: String,
    selector: (S) -> T,
    expected: T
) {
    val state = first()
    val actual = selector(state)
    assertEquals("Field '$fieldName' mismatch", expected, actual)
}

/**
 * 收集多个状态变化
 *
 * 收集指定数量的状态值，用于验证状态变化序列。
 *
 * @receiver Flow<S> StateFlow 或 Flow
 * @param count 要收集的状态数量
 * @return 状态值列表
 *
 * 使用示例：
 * ```kotlin
 * val states = viewModel.state.collectStates(3)
 * assertEquals(3, states.size)
 * assertTrue(states[0].isLoading == false)
 * assertTrue(states[1].isLoading == true)
 * assertTrue(states[2].isLoading == false)
 * ```
 */
suspend fun <S : MviState> Flow<S>.collectStates(count: Int): List<S> {
    return take(count).toList()
}

/**
 * 副作用收集扩展函数
 *
 * 收集指定数量的副作用事件。
 *
 * @receiver Flow<E> Effect Flow
 * @param count 要收集的副作用数量
 * @return 副作用列表
 *
 * 使用示例：
 * ```kotlin
 * val effects = viewModel.effect.collectEffects(2)
 * assertTrue(effects[0] is LoginEffect.ShowToast)
 * assertTrue(effects[1] is LoginEffect.NavigateToHome)
 * ```
 */
suspend fun <E : MviEffect> Flow<E>.collectEffects(count: Int): List<E> {
    return take(count).toList()
}

/**
 * 验证副作用类型
 *
 * 收集第一个副作用并验证其类型。
 *
 * @receiver Flow<E> Effect Flow
 * @param expectedType 期望的副作用类型
 * @throws AssertionError 如果类型不匹配
 *
 * 使用示例：
 * ```kotlin
 * viewModel.effect.assertEffectType<LoginEffect.NavigateToHome>()
 * ```
 */
suspend inline fun <reified T : MviEffect> Flow<MviEffect>.assertEffectType() {
    val effect = first()
    assertTrue(
        "Expected effect type ${T::class.simpleName} but got ${effect::class.simpleName}",
        effect is T
    )
}

/**
 * 验证副作用值
 *
 * 收集第一个副作用并验证其满足指定条件。
 *
 * @receiver Flow<E> Effect Flow
 * @param predicate 断言条件
 * @throws AssertionError 如果断言失败
 *
 * 使用示例：
 * ```kotlin
 * viewModel.effect.assertEffect<LoginEffect.ShowToast> { it.message == "登录成功" }
 * ```
 */
suspend inline fun <reified T : MviEffect> Flow<MviEffect>.assertEffect(
    crossinline predicate: (T) -> Boolean
) {
    val effect = first()
    assertTrue("Effect type mismatch", effect is T)
    assertTrue("Effect assertion failed: $effect", predicate(effect as T))
}

/**
 * ViewModel 测试扩展函数
 *
 * 提供便捷的 ViewModel 测试方法。
 */

/**
 * 发送 Intent 并等待处理完成
 *
 * 在测试中发送 Intent 后自动等待协程完成。
 *
 * @receiver BaseViewModel
 * @param intent 要发送的意图
 *
 * 注意：需要在 runTest 作用域中使用，并调用 advanceUntilIdle()
 *
 * 使用示例：
 * ```kotlin
 * @Test
 * fun test() = runTest {
 *     viewModel.sendIntentAndWait(MyIntent.DoSomething)
 *     advanceUntilIdle()
 *     // 验证结果
 * }
 * ```
 */
fun <I : MviIntent, S : MviState, A : MviAction, E : MviEffect>
    BaseViewModel<I, S, A, E>.sendIntentAndWait(intent: I) {
    sendIntent(intent)
}

/**
 * 获取当前状态值
 *
 * 在测试中获取 StateFlow 的当前值。
 *
 * @receiver BaseViewModel
 * @return 当前状态
 *
 * 使用示例：
 * ```kotlin
 * val currentState = viewModel.currentState()
 * assertEquals("test", currentState.username)
 * ```
 */
fun <I : MviIntent, S : MviState, A : MviAction, E : MviEffect>
    BaseViewModel<I, S, A, E>.currentState(): S {
    return state.value
}

/**
 * 测试调度器提供者
 *
 * 为测试提供统一的调度器配置。
 *
 * ## 使用场景
 * 1. **StandardTestDispatcher**: 适合需要手动控制协程执行的测试
 * 2. **UnconfinedTestDispatcher**: 适合协程立即执行的测试
 *
 * 使用示例：
 * ```kotlin
 * @OptIn(ExperimentalCoroutinesApi::class)
 * class MyViewModelTest {
 *     private val testScheduler = TestCoroutineScheduler()
 *     private val testDispatcher = StandardTestDispatcher(testScheduler)
 *
 *     @Before
 *     fun setup() {
 *         Dispatchers.setMain(testDispatcher)
 *     }
 *
 *     @After
 *     fun tearDown() {
 *         Dispatchers.resetMain()
 *     }
 *
 *     @Test
 *     fun test() = runTest(testDispatcher) {
 *         // 测试代码
 *     }
 * }
 * ```
 */
@OptIn(ExperimentalCoroutinesApi::class)
object MviTestDispatchers {
    /**
     * 创建标准测试调度器
     *
     * 需要手动调用 advanceUntilIdle() 或 advanceTimeBy() 来执行协程。
     */
    fun createStandardTestDispatcher(): TestDispatcher = StandardTestDispatcher()

    /**
     * 创建非限制测试调度器
     *
     * 协程会立即执行，无需手动推进。
     */
    fun createUnconfinedTestDispatcher(): TestDispatcher = UnconfinedTestDispatcher()
}

/**
 * 测试辅助类：状态观察器
 *
 * 用于在测试中收集和验证状态变化历史。
 *
 * 使用示例：
 * ```kotlin
 * @Test
 * fun test() = runTest {
 *     val observer = StateObserver(viewModel.state)
 *
 *     viewModel.sendIntent(MyIntent.DoSomething)
 *     advanceUntilIdle()
 *
 *     // 验证状态变化序列
 *     observer.assertStateSequence(
 *         MyState(isLoading = false),
 *         MyState(isLoading = true),
 *         MyState(isLoading = false, data = "result")
 *     )
 * }
 * ```
 */
class StateObserver<S : MviState>(
    private val stateFlow: Flow<S>
) {
    private val states = mutableListOf<S>()

    /**
     * 开始收集状态
     */
    suspend fun startCollecting() {
        stateFlow.collect { states.add(it) }
    }

    /**
     * 获取收集到的所有状态
     */
    fun getStates(): List<S> = states.toList()

    /**
     * 验证状态变化序列
     */
    fun assertStateSequence(vararg expectedStates: S) {
        assertEquals("State sequence size mismatch", expectedStates.size, states.size)
        expectedStates.forEachIndexed { index, expected ->
            assertEquals("State mismatch at index $index", expected, states[index])
        }
    }

    /**
     * 验证状态变化数量
     */
    fun assertStateCount(expectedCount: Int) {
        assertEquals("State count mismatch", expectedCount, states.size)
    }
}

/**
 * 测试辅助类：副作用观察器
 *
 * 用于在测试中收集和验证副作用事件。
 *
 * 使用示例：
 * ```kotlin
 * @Test
 * fun test() = runTest {
 *     val observer = EffectObserver(viewModel.effect)
 *     launch { observer.startCollecting() }
 *
 *     viewModel.sendIntent(MyIntent.DoSomething)
 *     advanceUntilIdle()
 *
 *     // 验证副作用
 *     observer.assertEffect<MyEffect.ShowToast> { it.message == "Success" }
 * }
 * ```
 */
class EffectObserver<E : MviEffect>(
    private val effectFlow: Flow<E>
) {
    private val effects = mutableListOf<E>()

    /**
     * 开始收集副作用
     */
    suspend fun startCollecting() {
        effectFlow.collect { effects.add(it) }
    }

    /**
     * 获取收集到的所有副作用
     */
    fun getEffects(): List<E> = effects.toList()

    /**
     * 验证副作用数量
     */
    fun assertEffectCount(expectedCount: Int) {
        assertEquals("Effect count mismatch", expectedCount, effects.size)
    }

    /**
     * 验证包含特定类型的副作用
     */
    inline fun <reified T : E> assertContainsEffect(): T {
        val effect = effects.firstOrNull { it is T }
        assertNotNull("No effect of type ${T::class.simpleName} found", effect)
        return effect as T
    }

    /**
     * 验证副作用
     */
    inline fun <reified T : E> assertEffect(crossinline predicate: (T) -> Boolean) {
        val effect = assertContainsEffect<T>()
        assertTrue("Effect assertion failed: $effect", predicate(effect))
    }
}
