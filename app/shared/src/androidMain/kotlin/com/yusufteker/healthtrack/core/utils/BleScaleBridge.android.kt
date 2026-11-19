package com.yusufteker.healthtrack.core.utils

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.content.ContextCompat
import com.yusufteker.healthtrack.core.i18n.trEn
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

private val UUID_BODY_COMPOSITION = ParcelUuid.fromString("0000181b-0000-1000-8000-00805f9b34fb")
private val UUID_WEIGHT_SCALE = ParcelUuid.fromString("0000181d-0000-1000-8000-00805f9b34fb")

@Composable
actual fun rememberBleScaleBridge(): BleScaleBridge {
    if (LocalInspectionMode.current) {
        return remember {
            object : BleScaleBridge {
                override val isSupported: Boolean = true
                private val _state = MutableStateFlow(BleScaleLiveState())
                override val state: StateFlow<BleScaleLiveState> = _state.asStateFlow()
                override fun startScan(heightCm: Double, age: Int, isMale: Boolean) {}
                override fun stopScan() {}
            }
        }
    }

    val context = LocalContext.current
    val bleSupported = remember(context) {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    }

    val stateFlow = remember { MutableStateFlow(BleScaleLiveState()) }
    var activeProfile by remember { mutableStateOf(Triple(175.0, 26, true)) }
    var activeScanner by remember { mutableStateOf<BluetoothLeScanner?>(null) }
    var activeCallback by remember { mutableStateOf<ScanCallback?>(null) }
    var pendingStartAfterPermission by remember { mutableStateOf(false) }

    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun hasBlePermissions(): Boolean {
        return requiredPermissions.all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("MissingPermission")
    fun stopInternalScan() {
        val scanner = activeScanner
        val callback = activeCallback
        if (scanner != null && callback != null) {
            runCatching { scanner.stopScan(callback) }
                .onFailure { Napier.w("BLE stopScan warning: ${it.message}", tag = "BLE_SCALE") }
        }
        activeCallback = null
        activeScanner = null
    }

    @SuppressLint("MissingPermission")
    fun beginScanning() {
        if (!bleSupported) {
            stateFlow.value = BleScaleLiveState(
                scanState = BleScaleScanState.NOT_SUPPORTED,
                statusMessage = trEn(
                    "Cihazınız Bluetooth Low Energy (BLE) özelliğini desteklemiyor.",
                    "Your device does not support Bluetooth Low Energy (BLE)."
                )
            )
            return
        }

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter: BluetoothAdapter? = bluetoothManager?.adapter
        if (adapter == null) {
            stateFlow.value = BleScaleLiveState(
                scanState = BleScaleScanState.NOT_SUPPORTED,
                statusMessage = trEn("Bluetooth adaptörü bulunamadı.", "Bluetooth adapter not found.")
            )
            return
        }
        if (!adapter.isEnabled) {
            stateFlow.value = BleScaleLiveState(
                scanState = BleScaleScanState.BLUETOOTH_OFF,
                statusMessage = trEn(
                    "Lütfen Bluetooth'u açın ve tekrar deneyin.",
                    "Please turn on Bluetooth and try again."
                )
            )
            return
        }

        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            stateFlow.value = BleScaleLiveState(
                scanState = BleScaleScanState.ERROR,
                statusMessage = trEn(
                    "Bluetooth tarayıcı başlatılamadı.",
                    "Could not start Bluetooth scanner."
                )
            )
            return
        }

        stopInternalScan()
        stateFlow.value = BleScaleLiveState(
            scanState = BleScaleScanState.SCANNING,
            statusMessage = trEn(
                "Tartı aranıyor... Lütfen çıplak ayakla tartıya çıkın.",
                "Searching for scale... Step onto the scale barefoot."
            )
        )

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val serviceData = result.scanRecord?.serviceData ?: return
                val bytes181B = serviceData[UUID_BODY_COMPOSITION]
                val bytes181D = serviceData[UUID_WEIGHT_SCALE]

                val (h, a, male) = activeProfile
                val decoded = when {
                    bytes181B != null -> XiaomiScaleBleParser.parseServiceData(
                        XiaomiScaleBleParser.SERVICE_UUID_BODY_COMPOSITION,
                        bytes181B,
                        heightCm = h,
                        age = a,
                        isMale = male
                    )
                    bytes181D != null -> XiaomiScaleBleParser.parseServiceData(
                        XiaomiScaleBleParser.SERVICE_UUID_WEIGHT_SCALE,
                        bytes181D,
                        heightCm = h,
                        age = a,
                        isMale = male
                    )
                    else -> null
                } ?: return

                val prevState = stateFlow.value
                val prevReading = prevState.liveReading

                if (decoded.isWeightRemoved && prevState.scanState == BleScaleScanState.STABILIZED) {
                    return
                }
                if (prevReading != null && prevReading.isStabilized && !decoded.isStabilized &&
                    abs(prevReading.weightKg - decoded.weightKg) < 0.2
                ) {
                    return
                }

                val mergedReading = if (
                    prevReading != null &&
                    prevReading.bodyFatPercent != null &&
                    decoded.bodyFatPercent == null &&
                    abs(prevReading.weightKg - decoded.weightKg) < 0.2
                ) {
                    decoded.copy(
                        impedanceOhm = prevReading.impedanceOhm,
                        bodyFatPercent = prevReading.bodyFatPercent,
                        muscleMassKg = prevReading.muscleMassKg,
                        musclePercent = prevReading.musclePercent,
                        leanBodyMassKg = prevReading.leanBodyMassKg,
                        boneMassKg = prevReading.boneMassKg
                    )
                } else {
                    decoded
                }

                val nextScanState = if (mergedReading.isStabilized) {
                    BleScaleScanState.STABILIZED
                } else {
                    BleScaleScanState.MEASURING
                }

                val nextMessage = when {
                    nextScanState == BleScaleScanState.STABILIZED && mergedReading.bodyFatPercent != null -> trEn(
                        "Ölçüm tamamlandı! Yağ ve kas verisi alındı.",
                        "Measurement complete! Body fat and muscle data captured."
                    )
                    nextScanState == BleScaleScanState.STABILIZED -> trEn(
                        "Kilo sabitlendi! Empedans ölçümü bekleniyor (veya hemen kaydedebilirsiniz).",
                        "Weight stabilized! Waiting for impedance (or save now)."
                    )
                    else -> trEn(
                        "Tartılıyor... Sabit durun.",
                        "Measuring... Please stand still."
                    )
                }

                stateFlow.value = BleScaleLiveState(
                    scanState = nextScanState,
                    liveReading = mergedReading,
                    statusMessage = nextMessage
                )
            }

            override fun onScanFailed(errorCode: Int) {
                Napier.e("BLE scan failed with code=$errorCode", tag = "BLE_SCALE")
                stateFlow.value = BleScaleLiveState(
                    scanState = BleScaleScanState.ERROR,
                    liveReading = stateFlow.value.liveReading,
                    statusMessage = trEn(
                        "Bluetooth tarama hatası (kod: $errorCode).",
                        "Bluetooth scan error (code: $errorCode)."
                    )
                )
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        runCatching {
            activeScanner = scanner
            activeCallback = callback
            scanner.startScan(null, settings, callback)
        }.onFailure { err ->
            activeScanner = null
            activeCallback = null
            stateFlow.value = BleScaleLiveState(
                scanState = BleScaleScanState.ERROR,
                statusMessage = err.message ?: trEn("Tarama başlatılamadı.", "Failed to start scan.")
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val shouldStart = pendingStartAfterPermission
        pendingStartAfterPermission = false
        val allGranted = results.isNotEmpty() && results.values.all { it }
        if (allGranted || hasBlePermissions()) {
            if (shouldStart) beginScanning()
        } else {
            stateFlow.value = BleScaleLiveState(
                scanState = BleScaleScanState.PERMISSION_DENIED,
                statusMessage = trEn(
                    "Tartıyı okuyabilmek için Bluetooth tarama izni gerekiyor.",
                    "Bluetooth scan permission is required to read your smart scale."
                )
            )
        }
    }

    val bridge = remember(context, bleSupported, permissionLauncher) {
        object : BleScaleBridge {
            override val isSupported: Boolean = bleSupported
            override val state: StateFlow<BleScaleLiveState> = stateFlow.asStateFlow()

            override fun startScan(heightCm: Double, age: Int, isMale: Boolean) {
                activeProfile = Triple(heightCm, age, isMale)
                if (!bleSupported) {
                    stateFlow.value = BleScaleLiveState(
                        scanState = BleScaleScanState.NOT_SUPPORTED,
                        statusMessage = trEn(
                            "Cihazınız Bluetooth Low Energy (BLE) özelliğini desteklemiyor.",
                            "Your device does not support Bluetooth Low Energy (BLE)."
                        )
                    )
                    return
                }
                if (hasBlePermissions()) {
                    beginScanning()
                } else {
                    pendingStartAfterPermission = true
                    stateFlow.value = BleScaleLiveState(
                        scanState = BleScaleScanState.REQUESTING_PERMISSION,
                        statusMessage = trEn("Bluetooth izni isteniyor...", "Requesting Bluetooth permission...")
                    )
                    permissionLauncher.launch(requiredPermissions)
                }
            }

            override fun stopScan() {
                stopInternalScan()
                stateFlow.value = BleScaleLiveState()
            }
        }
    }

    DisposableEffect(bridge) {
        onDispose {
            stopInternalScan()
        }
    }

    return bridge
}
