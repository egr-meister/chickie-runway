package com.chickierunway.game.integration

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** sub13: one accelerometer reading at launch and one a second later. */
object AccelerometerProbe {

    private const val TAG = "AccelerometerProbe"
    private const val SAMPLE_WINDOW_MS = 1_000L
    private const val AWAIT_TIMEOUT_SECONDS = 3L

    private val started = AtomicBoolean(false)
    private val ready = CountDownLatch(1)

    @Volatile private var first: FloatArray? = null
    @Volatile private var last: FloatArray? = null
    @Volatile private var sensorManager: SensorManager? = null

    /**
     * Events and the cut-off run on their own looper, not the main one: anything blocking
     * the main thread while waiting for sub13 would otherwise block the very events it
     * waits for and always end up with "na_na" after the timeout.
     */
    private val samplingHandler: Handler by lazy {
        Handler(HandlerThread("accel-probe").apply { start() }.looper)
    }

    /**
     * Called from Application.onCreate, not from the URL builder: the window is a full
     * second and the reading must be ready by the time routing asks for it.
     */
    fun start(context: Context) {
        // A second start() would register a second listener that never gets removed.
        if (!started.compareAndSet(false, true)) return
        runCatching { beginSampling(context) }.onFailure {
            Log.w(TAG, "Could not start the accelerometer", it)
            // Inside Application.onCreate: never crash, and never leave await() hanging.
            ready.countDown()
        }
    }

    private fun beginSampling(context: Context) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (manager == null || sensor == null) {
            Log.w(TAG, "No accelerometer available")
            // Nothing is coming - open the latch so await() does not wait for it.
            ready.countDown()
            return
        }

        val registered =
            manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL, samplingHandler)
        if (!registered) {
            Log.w(TAG, "registerListener refused the accelerometer")
            ready.countDown()
            return
        }

        sensorManager = manager
        samplingHandler.postDelayed(::stop, SAMPLE_WINDOW_MS)
    }

    /**
     * Blocks until the window closes, capped at three seconds in case the URL is built
     * earlier. Background threads only.
     */
    fun await(): String {
        if (!started.get()) {
            Log.w(TAG, "await() before start() - AccelerometerProbe.start is missing from Application.onCreate")
            return MotionSample.describe(null, null)
        }
        runCatching { ready.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS) }
            .onFailure {
                Log.w(TAG, "Interrupted while waiting for the accelerometer", it)
                Thread.currentThread().interrupt()
            }
        return MotionSample.describe(first, last)
    }

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            // SensorEvent.values is a reused buffer - copy, or both fields share one array.
            val values = event?.values?.copyOf() ?: return
            // First event is the launch reading; each later one replaces the end reading.
            if (first == null) first = values else last = values
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private fun stop() {
        runCatching { sensorManager?.unregisterListener(listener) }
            .onFailure { Log.w(TAG, "unregisterListener failed", it) }
        // The sensor must not stay live for the life of the app.
        sensorManager = null
        ready.countDown()
    }
}
