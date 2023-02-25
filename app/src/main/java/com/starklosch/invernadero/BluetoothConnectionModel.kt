//@file:OptIn(ExperimentalTime::class)
//
package com.starklosch.invernadero
//
//import android.app.Application
//import android.util.Log
//import androidx.lifecycle.AndroidViewModel
//import androidx.lifecycle.viewModelScope
//import com.juul.kable.ConnectionLostException
//import com.juul.kable.NotReadyException
//import com.juul.kable.Peripheral
//import com.juul.kable.State
//import com.juul.kable.peripheral
//import com.juul.sensortag.Sample
//import com.juul.sensortag.Vector3f
//import com.juul.sensortag.features.sensor.ViewState.Connected.GyroState
//import com.juul.sensortag.features.sensor.ViewState.Connected.GyroState.AxisState
//import com.starklosch.invernadero.SensorTag
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.GlobalScope
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.flow.Flow
//import kotlinx.coroutines.flow.catch
//import kotlinx.coroutines.flow.combine
//import kotlinx.coroutines.flow.filter
//import kotlinx.coroutines.flow.flatMapLatest
//import kotlinx.coroutines.flow.flow
//import kotlinx.coroutines.flow.flowOf
//import kotlinx.coroutines.flow.launchIn
//import kotlinx.coroutines.flow.onEach
//import kotlinx.coroutines.flow.onStart
//import kotlinx.coroutines.flow.scan
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withTimeoutOrNull
//import java.util.concurrent.TimeUnit
//import java.util.concurrent.atomic.AtomicInteger
//import kotlin.math.absoluteValue
//import kotlin.math.pow
//import kotlin.time.ExperimentalTime
//import kotlin.time.TimeMark
//import kotlin.time.TimeSource
//
//private val DISCONNECT_TIMEOUT = TimeUnit.SECONDS.toMillis(5)
//
//sealed class ViewState {
//
//    object Connecting : ViewState()
//
//    data class Connected(
//        val rssi: Int,
//        val gyro: GyroState
//    ) : ViewState() {
//
//        data class GyroState(
//            val x: AxisState,
//            val y: AxisState,
//            val z: AxisState
//        ) {
//
//            data class AxisState(
//                val degreesPerSecond: Float,
//                val progress: Float,
//            )
//        }
//    }
//
//    object Disconnecting : ViewState()
//
//    object Disconnected : ViewState()
//}
//
//val ViewState.label: CharSequence
//    get() = when (this) {
//        ViewState.Connecting -> "Connecting"
//        is ViewState.Connected -> "Connected"
//        ViewState.Disconnecting -> "Disconnecting"
//        ViewState.Disconnected -> "Disconnected"
//    }
//
//class BluetoothConnectionModel(
//    application: Application,
//    macAddress: String
//) : AndroidViewModel(application) {
//
//    private val peripheral = viewModelScope.peripheral(macAddress)
//    private val sensorTag = SensorTag(peripheral)
//    private val connectionAttempt = AtomicInteger()
//
//    private val periodProgress = AtomicInteger()
//
//    private var startTime: TimeMark? = null
//
//    val data = sensorTag.gyro
//        .onStart { startTime = TimeSource.Monotonic.markNow() }
//        .scan(emptyList<Sample>()) { accumulator, value ->
//            val t = startTime!!.elapsedNow().inWholeMilliseconds / 1000f
//            accumulator.takeLast(50) + Sample(t, value.x, value.y, value.z)
//        }
//        .filter { it.size > 3 }
//
//    init {
//        viewModelScope.enableAutoReconnect()
//        viewModelScope.connect()
//    }
//
//    private fun CoroutineScope.enableAutoReconnect() {
//        peripheral.state
//            .filter { it is State.Disconnected }
//            .onEach {
//                val timeMillis =
//                    backoff(base = 500L, multiplier = 2f, retry = connectionAttempt.getAndIncrement())
//                Log.info { "Waiting $timeMillis ms to reconnect..." }
//                delay(timeMillis)
//                connect()
//            }
//            .launchIn(this)
//    }
//
//    private fun CoroutineScope.connect() {
//        connectionAttempt.incrementAndGet()
//        launch {
//            Log.debug { "connect" }
//            try {
//                peripheral.connect()
//                sensorTag.enableGyro()
//                sensorTag.writeGyroPeriodProgress(periodProgress.get())
//                connectionAttempt.set(0)
//            } catch (e: ConnectionLostException) {
//                Log.warn(e) { "Connection attempt failed" }
//            }
//        }
//    }
//
//    val viewState: Flow<ViewState> = peripheral.state.flatMapLatest { state ->
//        when (state) {
//            is State.Connecting -> flowOf(ViewState.Connecting)
//            State.Connected -> combine(peripheral.remoteRssi(), sensorTag.gyro) { rssi, gyro ->
//                ViewState.Connected(rssi, gyroState(gyro))
//            }
//            State.Disconnecting -> flowOf(ViewState.Disconnecting)
//            is State.Disconnected -> flowOf(ViewState.Disconnected)
//        }
//    }
//
//    private val max = Max()
//    private fun gyroState(gyro: Vector3f): GyroState {
//        val progress = gyro.progress(max.maxOf(gyro))
//        return GyroState(
//            x = AxisState(degreesPerSecond = gyro.x, progress = progress.x),
//            y = AxisState(degreesPerSecond = gyro.y, progress = progress.y),
//            z = AxisState(degreesPerSecond = gyro.z, progress = progress.z),
//        )
//    }
//
//    fun setPeriod(progress: Int) {
//        periodProgress.set(progress)
//        viewModelScope.launch {
//            sensorTag.writeGyroPeriodProgress(progress)
//        }
//    }
//
//    override fun onCleared() {
//        GlobalScope.launch {
//            withTimeoutOrNull(DISCONNECT_TIMEOUT) {
//                peripheral.disconnect()
//            }
//        }
//    }
//}
//
//private fun Peripheral.remoteRssi() = flow {
//    while (true) {
//        val rssi = rssi()
//        Log.debug { "RSSI: $rssi" }
//        emit(rssi)
//        delay(1_000L)
//    }
//}.catch { cause ->
//    // todo: Investigate better way of handling this failure case.
//    // When disconnecting, we may attempt to read `rssi` causing a `NotReadyException` but the hope is that `remoteRssi`
//    // Flow would already be cancelled by the time the `Peripheral` is "not ready" (doesn't seem to be the case).
//    if (cause !is NotReadyException) throw cause
//}
//
//private suspend fun SensorTag.writeGyroPeriodProgress(progress: Int) {
//    val period = progress / 100f * (2550 - 100) + 100
//    Log.verbose { "period = $period" }
//    writeGyroPeriod(period.toLong())
//}
//
//private fun Vector3f.progress(max: Max) = Vector3f(
//    if (max.x != 0f) x.absoluteValue / max.x else 0f,
//    if (max.y != 0f) y.absoluteValue / max.y else 0f,
//    if (max.z != 0f) z.absoluteValue / max.z else 0f,
//)
//
//private data class Max(
//    var x: Float = 0f,
//    var y: Float = 0f,
//    var z: Float = 0f
//) {
//    fun maxOf(vector: Vector3f) = apply {
//        x = maxOf(x, vector.x.absoluteValue)
//        y = maxOf(y, vector.y.absoluteValue)
//        z = maxOf(z, vector.z.absoluteValue)
//    }
//}
//
///**
// * Exponential backoff using the following formula:
// *
// * ```
// * delay = base * multiplier ^ retry
// * ```
// *
// * For example (using `base = 100` and `multiplier = 2`):
// *
// * | retry | delay |
// * |-------|-------|
// * |   1   |   100 |
// * |   2   |   200 |
// * |   3   |   400 |
// * |   4   |   800 |
// * |   5   |  1600 |
// * |  ...  |   ... |
// *
// * Inspired by:
// * [Exponential Backoff And Jitter](https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/)
// *
// * @return Backoff delay (in units matching [base] units, e.g. if [base] units are milliseconds then returned delay will be milliseconds).
// */
//private fun backoff(
//    base: Long,
//    multiplier: Float,
//    retry: Int,
//): Long = (base * multiplier.pow(retry - 1)).toLong()