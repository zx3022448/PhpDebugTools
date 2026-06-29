package com.zx3022448.phpdebugtools.execution

import com.zx3022448.phpdebugtools.diagnostics.CommandResult
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class ProcessCommandRunner : CancellableCommandRunner {
    override fun start(command: List<String>, workingDirectory: String?): RunningCommand {
        val process = ProcessBuilder(command)
            .apply {
                if (workingDirectory != null) {
                    directory(Path.of(workingDirectory).toFile())
                }
            }
            .start()

        return ProcessRunningCommand(process)
    }
}

private class ProcessRunningCommand(
    private val process: Process,
) : RunningCommand {
    @Volatile
    private var cancelled = false

    override fun await(): CommandResult {
        val stdoutFuture = readAsync(process.inputStream)
        val stderrFuture = readAsync(process.errorStream)

        val exitCode = try {
            process.waitFor()
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            cancel()
            throw CancellationException("请求已停止")
        }

        val stdout = stdoutFuture.get()
        val stderr = stderrFuture.get()
        if (cancelled) {
            throw CancellationException("请求已停止")
        }
        return CommandResult(exitCode = exitCode, stdout = stdout, stderr = stderr)
    }

    override fun cancel() {
        cancelled = true
        if (!process.isAlive) {
            return
        }
        // 先给 PHP 进程一个正常退出机会，避免调试输出被强行截断。
        process.destroy()
        if (!process.waitFor(2, TimeUnit.SECONDS)) {
            process.destroyForcibly()
        }
    }

    private fun readAsync(stream: java.io.InputStream): CompletableFuture<String> =
        CompletableFuture.supplyAsync {
            stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        }
}
