package com.yusufteker.healthtrack.core.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.yusufteker.healthtrack.core.i18n.trEn
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.CoreBluetooth.CBAdvertisementDataLocalNameKey
import platform.CoreBluetooth.CBAdvertisementDataManufacturerDataKey
import platform.CoreBluetooth.CBAdvertisementDataServiceDataKey
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBCentralManagerScanOptionAllowDuplicatesKey
import platform.CoreBluetooth.CBManagerStatePoweredOff
import platform.CoreBluetooth.CBManagerStatePoweredOn
import platform.CoreBluetooth.CBManagerStateResetting
import platform.CoreBluetooth.CBManagerStateUnauthorized
import platform.CoreBluetooth.CBManagerStateUnsupported
import platform.CoreBluetooth.CBPeripheral
import platform.CoreBluetooth.CBUUID
import platform.Foundation.NSData
import platform.Foundation.NSNumber
import platform.darwin.NSObject
import platform.posix.memcpy
import kotlin.math.abs

private class IosBleScaleController : BleScaleBridge {
    private val _state = MutableStateFlow(BleScaleLiveState())
    override val isSupported: Boolean = true
    override val state: StateFlow<BleScaleLiveState> = _state.asStateFlow()

    private var heightCm: Double = 175.0
    private var age: Int = 26
    private var isMale: Boolean = true
    private var wantsToScan: Boolean = false
    private var centralManager: CBCentralManager? = null

    private val delegate = object : NSObject(), CBCentralManagerDelegateProtocol {
        override fun centralManagerDidUpdateState(central: CBCentralManager) {
            when (central.state) {
                CBManagerStatePoweredOn -> {
                    if (wantsToScan) {
                        startCoreBluetoothScan(central)
                    }
                }
                CBManagerStatePoweredOff -> {
                    _state.value = BleScaleLiveState(
                        scanState = BleScaleScanState.BLUETOOTH_OFF,
                        liveReading = _state.value.liveReading,
                        statusMessage = trEn(
                            "Lütfen iPhone Bluetooth özelliğini açın.",
                            "Please turn on Bluetooth in iPhone Settings."
                        )
                    )
                }
                CBManagerStateUnauthorized -> {
                    _state.value = BleScaleLiveState(
                        scanState = BleScaleScanState.PERMISSION_DENIED,
                        statusMessage = trEn(
                            "Tartıyı okuyabilmek için Ayarlar'dan HealthTrack Bluetooth izni verin.",
                            "Allow Bluetooth permission for HealthTrack in iOS Settings to read your scale."
                        )
                    )
                }
                CBManagerStateUnsupported -> {
                    _state.value = BleScaleLiveState(
                        scanState = BleScaleScanState.NOT_SUPPORTED,
                        statusMessage = trEn(
                            "Bu cihazda Bluetooth Low Energy desteklenmiyor.",
                            "Bluetooth Low Energy is not supported on this device."
                        )
                    )
                }
                CBManagerStateResetting -> {
                    _state.value = _state.value.copy(
                        scanState = BleScaleScanState.SCANNING,
                        statusMessage = trEn("Bluetooth hazırlanıyor...", "Preparing Bluetooth...")
                    )
                }
                else -> Unit
            }
        }

        override fun centralManager(
            central: CBCentralManager,
            didDiscoverPeripheral: CBPeripheral,
            advertisementData: Map<Any?, *>,
            RSSI: NSNumber
        ) {
            val serviceData = advertisementData[CBAdvertisementDataServiceDataKey] as? Map<*, *>
            var bytes181B: ByteArray? = null
            var bytes181D: ByteArray? = null

            if (serviceData != null) {
                for ((key, value) in serviceData) {
                    val uuidStr = (key as? CBUUID)?.UUIDString ?: continue
                    val data = value as? NSData ?: continue
                    when {
                        uuidStr.equals("181B", ignoreCase = true) ||
                            uuidStr.startsWith("0000181B", ignoreCase = true) -> {
                            bytes181B = data.toByteArray()
                        }
                        uuidStr.equals("181D", ignoreCase = true) ||
                            uuidStr.startsWith("0000181D", ignoreCase = true) -> {
                            bytes181D = data.toByteArray()
                        }
                    }
                }
            }

            val mfgData = (advertisementData[CBAdvertisementDataManufacturerDataKey] as? NSData)?.toByteArray()
            val manufacturerFrames = if (mfgData != null && mfgData.isNotEmpty()) listOf(mfgData) else emptyList()

            if (bytes181B == null && bytes181D == null && manufacturerFrames.isEmpty()) return

            val deviceName = (advertisementData[CBAdvertisementDataLocalNameKey] as? String)
                ?: didDiscoverPeripheral.name

            val decoded = SmartScaleBleParser.parseAdvertisement(
                serviceData181B = bytes181B,
                serviceData181D = bytes181D,
                manufacturerDataList = manufacturerFrames,
                deviceName = deviceName,
                heightCm = heightCm,
                age = age,
                isMale = isMale
            ) ?: return

            val prevState = _state.value
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

            _state.value = BleScaleLiveState(
                scanState = nextScanState,
                liveReading = mergedReading,
                statusMessage = nextMessage
            )
        }
    }

    override fun startScan(heightCm: Double, age: Int, isMale: Boolean) {
        this.heightCm = heightCm
        this.age = age
        this.isMale = isMale
        this.wantsToScan = true

        _state.value = BleScaleLiveState(
            scanState = BleScaleScanState.SCANNING,
            statusMessage = trEn(
                "Tartı aranıyor... Lütfen çıplak ayakla tartıya çıkın.",
                "Searching for scale... Step onto the scale barefoot."
            )
        )

        val existing = centralManager
        if (existing == null) {
            centralManager = CBCentralManager(delegate = delegate, queue = null)
        } else {
            when (existing.state) {
                CBManagerStatePoweredOn -> startCoreBluetoothScan(existing)
                CBManagerStatePoweredOff -> _state.value = BleScaleLiveState(
                    scanState = BleScaleScanState.BLUETOOTH_OFF,
                    statusMessage = trEn(
                        "Lütfen iPhone Bluetooth özelliğini açın.",
                        "Please turn on Bluetooth in iPhone Settings."
                    )
                )
                CBManagerStateUnauthorized -> _state.value = BleScaleLiveState(
                    scanState = BleScaleScanState.PERMISSION_DENIED,
                    statusMessage = trEn(
                        "Tartıyı okuyabilmek için Ayarlar'dan HealthTrack Bluetooth izni verin.",
                        "Allow Bluetooth permission for HealthTrack in iOS Settings to read your scale."
                    )
                )
                CBManagerStateUnsupported -> _state.value = BleScaleLiveState(
                    scanState = BleScaleScanState.NOT_SUPPORTED,
                    statusMessage = trEn(
                        "Bu cihazda Bluetooth Low Energy desteklenmiyor.",
                        "Bluetooth Low Energy is not supported on this device."
                    )
                )
                else -> Unit // Will trigger via centralManagerDidUpdateState
            }
        }
    }

    private fun startCoreBluetoothScan(manager: CBCentralManager) {
        manager.stopScan()
        val options: Map<Any?, *> = mapOf(
            CBCentralManagerScanOptionAllowDuplicatesKey to true
        )
        manager.scanForPeripheralsWithServices(serviceUUIDs = null, options = options)
    }

    override fun stopScan() {
        wantsToScan = false
        centralManager?.stopScan()
        _state.value = BleScaleLiveState()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun NSData.toByteArray(): ByteArray {
        val len = length
        val size = len.toInt()
        if (size <= 0) return ByteArray(0)
        val result = ByteArray(size)
        result.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this.bytes, len)
        }
        return result
    }
}

@Composable
actual fun rememberBleScaleBridge(): BleScaleBridge {
    val controller = remember { IosBleScaleController() }
    DisposableEffect(controller) {
        onDispose {
            controller.stopScan()
        }
    }
    return controller
}
