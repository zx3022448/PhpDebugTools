package com.zx3022448.phpdebugtools.execution

import com.zx3022448.phpdebugtools.diagnostics.CommandResult
import com.zx3022448.phpdebugtools.diagnostics.CommandRunner
import java.util.concurrent.CancellationException

interface RunningCommand {
    /**
     * 等待命令结束并返回结果。
     *
     * @return 命令标准输出、错误输出和退出码
     * @throws CancellationException 命令已被用户主动停止
     */
    fun await(): CommandResult

    /**
     * 请求停止当前命令；可取消实现应同时终止底层进程。
     */
    fun cancel()
}

interface CancellableCommandRunner : CommandRunner {
    /**
     * 启动命令并返回可等待、可取消的执行句柄。
     *
     * @param command 要执行的命令及参数
     * @param workingDirectory 可选工作目录
     * @return 当前命令的执行句柄
     */
    fun start(command: List<String>, workingDirectory: String?): RunningCommand

    override fun run(command: List<String>, workingDirectory: String?): CommandResult =
        start(command, workingDirectory).await()
}

class MethodInvokeExecution(
    private val runningCommand: RunningCommand,
    private val runtimeExecutor: RuntimeExecutor,
) {
    /**
     * 等待方法直调完成并解析运行时输出。
     *
     * @return 结构化调试结果
     * @throws CancellationException 请求已被用户主动停止
     */
    fun await(): DebugExecutionResult = runtimeExecutor.run(runningCommand)

    /**
     * 停止本次方法直调请求。
     */
    fun cancel() {
        runningCommand.cancel()
    }
}

