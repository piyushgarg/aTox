package ltd.evilcorp.core.tox.runtime

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ltd.evilcorp.domain.tox.bootstrap.BootstrapNodeRegistry
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.forEach as kForEach

private const val TAG = "ToxEngine"
private const val SLOW_ITERATION_LIMIT_MS = 10
private const val BOOTSTRAP_NODES_COUNT = 4
private const val RECOVERY_DELAY_MS = 1000L

@Singleton
class ToxEngine @Inject constructor(
    private val scope: CoroutineScope,
    private val nodeRegistry: BootstrapNodeRegistry,
) {
    private var running = false
    private var toxAvRunning = false
    private var iterateJob: Job? = null
    private var iterateAvJob: Job? = null

    var isBootstrapNeeded = true

    fun start(toxWrapper: ToxWrapper, onStopped: () -> Unit) {
        running = true
        isBootstrapNeeded = true

        iterateJob = scope.launch {
            while (running || toxAvRunning) {
                try {
                    if (isBootstrapNeeded) {
                        try {
                            bootstrap(toxWrapper)
                            isBootstrapNeeded = false
                        } catch (e: Exception) {
                            Log.e(TAG, e.toString())
                        }
                    }

                    val before = System.currentTimeMillis()
                    toxWrapper.iterate()
                    val timeTaken = System.currentTimeMillis() - before
                    val iterationInterval = toxWrapper.iterationInterval()
                    if (timeTaken > SLOW_ITERATION_LIMIT_MS && timeTaken > iterationInterval) {
                        Log.w(TAG, "Tox thread overran: $timeTaken/$iterationInterval.")
                    }
                    delay((iterationInterval - timeTaken).coerceAtLeast(0L))
                } catch (e: Exception) {
                    Log.e(TAG, "Error in Tox iteration loop: $e")
                    delay(RECOVERY_DELAY_MS)
                }
            }
            onStopped()
        }

        iterateAvJob = scope.launch {
            toxAvRunning = true
            while (running) {
                try {
                    toxWrapper.iterateAv()
                    delay(toxWrapper.iterationIntervalAv().coerceAtLeast(0L))
                } catch (e: Exception) {
                    Log.e(TAG, "Error in ToxAv iteration loop: $e")
                    delay(RECOVERY_DELAY_MS)
                }
            }
            toxAvRunning = false
        }
    }

    fun stop() {
        running = false
    }

    fun isRunning(): Boolean = running || toxAvRunning

    private fun bootstrap(toxWrapper: ToxWrapper) {
        nodeRegistry.get(BOOTSTRAP_NODES_COUNT).kForEach { node ->
            Log.i(TAG, "Bootstrapping from $node")
            toxWrapper.bootstrap(node.address, node.port, node.publicKey.bytes())
        }
    }
}
