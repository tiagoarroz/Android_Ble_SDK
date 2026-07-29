import Capacitor
import CoreBluetooth
import Foundation
import VeepooBleSDK

/**
 * Bridge Capacitor para o SDK Veepoo/H Band no iOS.
 *
 * Os callbacks do SDK são convertidos em eventos homogéneos com o Android,
 * deixando a serialização dos comandos a cargo do serviço Angular.
 */
@objc(HBandPlugin)
public final class HBandPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "HBandPlugin"
    public let jsName = "HBand"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "getStatus", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "requestPermissions", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "startScan", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "stopScan", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "connect", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "disconnect", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "execute", returnType: CAPPluginReturnPromise)
    ]

    private let manager = VPBleCentralManage.sharedBleManager()!
    private var devices: [String: VPPeripheralModel] = [:]
    private var connectionState = "idle"
    private var connectedDevice: VPPeripheralModel?
    private var pendingConnectCall: CAPPluginCall?
    private var pendingPassword = "0000"
    private var lastRSSI: Int?
    private let isoFormatter = ISO8601DateFormatter()

    public override func load() {
        manager.isLogEnable = false
        manager.connectionTimeout = 30
        manager.vpBleCentralManageChangeBlock = { [weak self] _ in
            self?.emitStatus()
        }
    }

    @objc public func getStatus(_ call: CAPPluginCall) {
        call.resolve(statusPayload())
    }

    /**
     * No iOS a autorização Bluetooth é apresentada pelo sistema quando o
     * CBCentralManager é utilizado; esta chamada expõe apenas o estado atual.
     */
    @objc public override func requestPermissions(_ call: CAPPluginCall) {
        let state = manager.centralManager?.state
        call.resolve(["granted": state != .unauthorized && state != .unsupported])
    }

    @objc public func startScan(_ call: CAPPluginCall) {
        devices.removeAll()
        setConnectionState("scanning")
        manager.veepooSDKStartScanDeviceAndReceiveScanningDevice { [weak self] model in
            guard let self, let model, !model.deviceAddress.isEmpty else {
                return
            }
            self.devices[model.deviceAddress] = model
            self.lastRSSI = model.rssi?.intValue
            self.notifyListeners("deviceFound", data: self.devicePayload(model))
        }
        emitLog(level: "info", message: "BLE scan started", operation: "session.scan")
        call.resolve()
    }

    @objc public func stopScan(_ call: CAPPluginCall) {
        manager.veepooSDKStopScanDevice()
        setConnectionState(manager.isConnected ? "connected" : "idle")
        call.resolve()
    }

    /**
     * A Promise só termina depois de o SDK confirmar a password. Isto impede
     * que a camada web envie comandos antes de os serviços estarem prontos.
     */
    @objc public func connect(_ call: CAPPluginCall) {
        guard let deviceID = call.getString("deviceId"), !deviceID.isEmpty else {
            call.reject("DEVICE_ID_REQUIRED")
            return
        }
        guard let device = devices[deviceID] else {
            call.reject("DEVICE_NOT_FOUND")
            return
        }

        manager.veepooSDKStopScanDevice()
        pendingConnectCall = call
        pendingPassword = call.getString("password") ?? "0000"
        connectedDevice = device
        setConnectionState("connecting")

        manager.veepooSDKConnectDevice(device) { [weak self] state in
            self?.handleConnectionState(state)
        }
    }

    @objc public func disconnect(_ call: CAPPluginCall) {
        manager.veepooSDKDisconnectDevice()
        connectedDevice = nil
        setConnectionState("disconnected")
        call.resolve()
    }

    /**
     * Encaminha apenas comandos cujos contratos foram confirmados nos headers
     * e na aplicação de demonstração que acompanha o SDK iOS.
     */
    @objc public func execute(_ call: CAPPluginCall) {
        guard let operation = call.getString("operation") else {
            call.reject("OPERATION_REQUIRED")
            return
        }
        if !manager.isConnected && !operation.hasPrefix("session.") {
            call.reject("DEVICE_NOT_CONNECTED")
            return
        }

        switch operation {
        case "session.authenticate":
            pendingConnectCall = call
            pendingPassword = call.getString("password") ?? pendingPassword
            authenticateCurrentDevice()
        case "session.disconnect":
            disconnect(call)
        case "device.battery":
            readBattery(call, operation: operation)
        case "device.rssi":
            emitData(type: "rssi", values: ["dbm": lastRSSI as Any])
            accept(call, operation: operation)
        case "device.time":
            authenticateCurrentDevice(call: call, operation: operation)
        case "device.profile":
            syncProfile(call, operation: operation)
        case "measure.heartRate.start":
            testHeartRate(true, call: call, operation: operation)
        case "measure.heartRate.stop":
            testHeartRate(false, call: call, operation: operation)
        case "measure.bloodPressure.start":
            testBloodPressure(true, call: call, operation: operation)
        case "measure.bloodPressure.stop":
            testBloodPressure(false, call: call, operation: operation)
        case "measure.oxygen.start":
            testOxygen(true, call: call, operation: operation)
        case "measure.oxygen.stop":
            testOxygen(false, call: call, operation: operation)
        case "measure.breathing.start":
            testBreathing(true, call: call, operation: operation)
        case "measure.breathing.stop":
            testBreathing(false, call: call, operation: operation)
        case "measure.temperature.start":
            testTemperature(true, call: call, operation: operation)
        case "measure.temperature.stop":
            testTemperature(false, call: call, operation: operation)
        case "measure.fatigue.start":
            testFatigue(true, call: call, operation: operation)
        case "measure.fatigue.stop":
            testFatigue(false, call: call, operation: operation)
        case "measure.hrv.start":
            testHRV(true, call: call, operation: operation)
        case "measure.hrv.stop":
            testHRV(false, call: call, operation: operation)
        case "measure.stress.start":
            testStress(true, call: call, operation: operation)
        case "measure.stress.stop":
            testStress(false, call: call, operation: operation)
        case "measure.gsr.start":
            testGSR(true, call: call, operation: operation)
        case "measure.gsr.stop":
            testGSR(false, call: call, operation: operation)
        case "measure.bloodGlucose.start":
            testBloodGlucose(true, call: call, operation: operation)
        case "measure.bloodGlucose.stop":
            testBloodGlucose(false, call: call, operation: operation)
        case "measure.ecg.start":
            testECG(true, call: call, operation: operation)
        case "measure.ecg.stop":
            testECG(false, call: call, operation: operation)
        case "measure.bodyComposition.start":
            testBodyComposition(true, call: call, operation: operation)
        case "measure.bodyComposition.stop":
            testBodyComposition(false, call: call, operation: operation)
        case "measure.bloodComposition.start":
            testBloodComposition(true, call: call, operation: operation)
        case "measure.bloodComposition.stop":
            testBloodComposition(false, call: call, operation: operation)
        case "measure.miniCheckup.start":
            testHealthGlance(call, operation: operation)
        default:
            call.reject("OPERATION_NOT_IMPLEMENTED:\(operation)")
        }
    }

    private func handleConnectionState(_ state: DeviceConnectState) {
        switch state {
        case .BlePoweredOff:
            rejectPendingConnect("BLUETOOTH_DISABLED")
            setConnectionState("error")
        case .BleConnecting:
            setConnectionState("connecting")
        case .BleConnectSuccess:
            setConnectionState("authenticating")
            authenticateCurrentDevice()
        case .BleVerifyPasswordSuccess:
            completeConnection()
        case .BleConnectFailed:
            rejectPendingConnect("BLE_CONNECT_FAILED")
            setConnectionState("error")
        case .BleVerifyPasswordFailure:
            rejectPendingConnect("PASSWORD_VERIFICATION_FAILED")
            setConnectionState("error")
        case .BleConnectTimeout:
            rejectPendingConnect("BLE_CONNECT_TIMEOUT")
            setConnectionState("error")
        case .BleConfirmTimeout:
            rejectPendingConnect("DEVICE_CONFIRM_TIMEOUT")
            setConnectionState("error")
        @unknown default:
            emitLog(level: "warning", message: "Unknown BLE state: \(state.rawValue)")
        }
    }

    private func authenticateCurrentDevice(
        call: CAPPluginCall? = nil,
        operation: String? = nil
    ) {
        manager.veepooSDKSynchronousPassword(
            with: .VerifyPasswordType,
            password: pendingPassword
        ) { [weak self] result in
            guard let self else { return }
            if result.rawValue == 1 || result.rawValue == 6 {
                if let call, let operation {
                    self.accept(call, operation: operation)
                    self.emitData(type: "time", values: ["synchronised": true])
                } else {
                    self.completeConnection()
                }
            } else if let call {
                call.reject("PASSWORD_OR_TIME_SYNC_FAILED_\(result.rawValue)")
            } else {
                self.rejectPendingConnect("PASSWORD_VERIFICATION_FAILED_\(result.rawValue)")
                self.setConnectionState("error")
            }
        }
    }

    private func completeConnection() {
        guard pendingConnectCall != nil else {
            return
        }
        setConnectionState("connected")
        emitLog(level: "success", message: "Device authenticated", operation: "session.authenticate")
        pendingConnectCall?.resolve()
        pendingConnectCall = nil
    }

    private func rejectPendingConnect(_ message: String) {
        pendingConnectCall?.reject(message)
        pendingConnectCall = nil
    }

    private func readBattery(_ call: CAPPluginCall, operation: String) {
        manager.peripheralManage.veepooSDKReadDeviceBatteryAndChargeInfo {
            [weak self] isPercent, chargeState, lowBattery, battery in
            self?.emitData(
                type: "battery",
                values: [
                    "percent": isPercent ? battery : battery * 25,
                    "raw": battery,
                    "isPercent": isPercent,
                    "lowBattery": lowBattery,
                    "chargeState": chargeState.rawValue
                ]
            )
        }
        accept(call, operation: operation)
    }

    /**
     * A sincronização de perfil nunca usa defaults silenciosos: todos os
     * valores obrigatórios têm de chegar explicitamente da interface.
     */
    private func syncProfile(_ call: CAPPluginCall, operation: String) {
        guard
            let params = call.getObject("params"),
            let stature = params["heightCm"] as? Int,
            let weight = params["weightKg"] as? Int,
            let birth = params["birthYear"] as? Int,
            let sex = params["sex"] as? String,
            sex == "male" || sex == "female",
            let target = params["targetSteps"] as? Int
        else {
            call.reject("PROFILE_PARAMS_REQUIRED")
            return
        }
        manager.peripheralManage.veepooSDKSynchronousPersonalInformation(
            withStature: UInt(stature),
            weight: UInt(weight),
            birth: UInt(birth),
            sex: sex == "female" ? 0 : 1,
            targetStep: UInt(target)
        ) { [weak self] result in
            self?.emitData(type: "profile", values: ["success": result == 1])
        }
        accept(call, operation: operation)
    }

    private func testHeartRate(_ start: Bool, call: CAPPluginCall, operation: String) {
        manager.peripheralManage.veepooSDKTestHeartStart(start) { [weak self] state, value in
            self?.emitData(type: "heartRate", values: ["state": state.rawValue, "bpm": value])
        }
        accept(call, operation: operation)
    }

    private func testBloodPressure(_ start: Bool, call: CAPPluginCall, operation: String) {
        manager.peripheralManage.veepooSDKTestBloodStart(start, testMode: 0) {
            [weak self] state, progress, systolic, diastolic in
            self?.emitData(
                type: "bloodPressure",
                values: [
                    "state": state.rawValue,
                    "progress": progress,
                    "systolic": systolic,
                    "diastolic": diastolic
                ]
            )
        }
        accept(call, operation: operation)
    }

    private func testOxygen(_ start: Bool, call: CAPPluginCall, operation: String) {
        manager.peripheralManage.veepooSDKTestOxygenStart(start) { [weak self] state, value in
            self?.emitData(type: "oxygen", values: ["state": state.rawValue, "percent": value])
        }
        accept(call, operation: operation)
    }

    private func testBreathing(_ start: Bool, call: CAPPluginCall, operation: String) {
        manager.peripheralManage.veepooSDKTestBreathingRateStart(start) {
            [weak self] state, progress, value in
            self?.emitData(
                type: "breathing",
                values: ["state": state.rawValue, "progress": progress, "breathsPerMinute": value]
            )
        }
        accept(call, operation: operation)
    }

    private func testTemperature(_ start: Bool, call: CAPPluginCall, operation: String) {
        manager.peripheralManage.veepooSDK_temperatureTestStart(start) {
            [weak self] state, enabled, progress, value, surfaceValue in
            self?.emitData(
                type: "temperature",
                values: [
                    "state": state.rawValue,
                    "enabled": enabled,
                    "progress": progress,
                    "celsius": Double(value) / 10.0,
                    "surfaceCelsius": Double(surfaceValue) / 10.0
                ]
            )
        }
        accept(call, operation: operation)
    }

    private func testFatigue(_ start: Bool, call: CAPPluginCall, operation: String) {
        manager.peripheralManage.veepooSDKTestFatigueStart(start) {
            [weak self] state, progress, value in
            self?.emitData(
                type: "fatigue",
                values: ["state": state.rawValue, "progress": progress, "level": value]
            )
        }
        accept(call, operation: operation)
    }

    private func testHRV(_ start: Bool, call: CAPPluginCall, operation: String) {
        manager.peripheralManage.veepooSDK_HRVTest(start) { [weak self] progress, state, value in
            self?.emitData(
                type: "hrv",
                values: ["state": state.rawValue, "progress": progress, "milliseconds": value]
            )
        }
        accept(call, operation: operation)
    }

    private func testStress(_ start: Bool, call: CAPPluginCall, operation: String) {
        manager.peripheralManage.veepooSDK_stressTestStart(start) {
            [weak self] state, progress, value in
            self?.emitData(
                type: "stress",
                values: ["state": state.rawValue, "progress": progress, "value": value]
            )
        }
        accept(call, operation: operation)
    }

    private func testGSR(_ start: Bool, call: CAPPluginCall, operation: String) {
        manager.peripheralManage.veepooSDKTestGSRStart(start) { [weak self] progress in
            self?.emitData(
                type: "gsr",
                values: ["progress": Int((progress?.fractionCompleted ?? 0) * 100)]
            )
        } testResult: { [weak self] state, model in
            self?.emitData(
                type: "gsr",
                values: ["state": state.rawValue, "result": model?.description ?? ""]
            )
        }
        accept(call, operation: operation)
    }

    private func testBloodGlucose(_ start: Bool, call: CAPPluginCall, operation: String) {
        manager.peripheralManage.veepooSDKTestBloodGlucoseStart(
            start,
            isPersonalModel: false
        ) { [weak self] state, progress, value, level in
            self?.emitData(
                type: "bloodGlucose",
                values: [
                    "state": state.rawValue,
                    "progress": progress,
                    "mmolL": Double(value) / 100.0,
                    "riskLevel": level
                ]
            )
        }
        accept(call, operation: operation)
    }

    private func testECG(_ start: Bool, call: CAPPluginCall, operation: String) {
        manager.peripheralManage.veepooSDKTestECGStart(start) {
            [weak self] state, progress, model in
            self?.emitData(
                type: "ecg",
                values: [
                    "state": state.rawValue,
                    "progress": progress,
                    "result": model?.description ?? ""
                ]
            )
        }
        accept(call, operation: operation)
    }

    private func testBodyComposition(_ start: Bool, call: CAPPluginCall, operation: String) {
        manager.peripheralManage.veepooSDKTestBodyCompositionStart(start) {
            [weak self] lead, progress in
            self?.emitData(
                type: "bodyComposition",
                values: ["lead": lead, "progress": Int((progress?.fractionCompleted ?? 0) * 100)]
            )
        } testResult: { [weak self] state, model in
            self?.emitData(
                type: "bodyComposition",
                values: ["state": state.rawValue, "result": model?.description ?? ""]
            )
        }
        accept(call, operation: operation)
    }

    private func testBloodComposition(_ start: Bool, call: CAPPluginCall, operation: String) {
        manager.peripheralManage.veepooSDKTestBloodAnalysisStart(
            start,
            isPersonalModel: false
        ) { [weak self] progress in
            self?.emitData(
                type: "bloodComposition",
                values: ["progress": Int((progress?.fractionCompleted ?? 0) * 100)]
            )
        } testResult: { [weak self] state, model in
            self?.emitData(
                type: "bloodComposition",
                values: ["state": state.rawValue, "result": model?.description ?? ""]
            )
        }
        accept(call, operation: operation)
    }

    private func testHealthGlance(_ call: CAPPluginCall, operation: String) {
        manager.peripheralManage.veepooSDK_healthGlanceTestStart(true) {
            [weak self] progress in
            self?.emitData(type: "healthGlance", values: ["progress": progress])
        } andResult: { [weak self] state, model in
            self?.emitData(
                type: "healthGlance",
                values: ["state": state.rawValue, "result": model?.description ?? ""]
            )
        }
        accept(call, operation: operation)
    }

    private func capabilitiesPayload() -> [String: String] {
        guard let model = manager.peripheralModel else {
            return [:]
        }
        let support: (Bool) -> String = { $0 ? "supported" : "unsupported" }
        return [
            "heartRate": support(model.heartRateType > 0),
            "bloodPressure": support(model.bloodPressureType > 0),
            "bloodOxygen": support(model.bloodOxygenType > 0 || model.oxygenType > 0),
            "breathing": support(model.resRateType > 0),
            "temperature": support(model.temperatureType > 0),
            "hrv": support(model.hrvType > 0 || model.isSupportHRVTest),
            "ecg": support(model.ecgType > 0),
            "bloodGlucose": support(model.bloodGlucoseType > 0),
            "fatigue": "unknown",
            "stress": support(model.stressType > 1),
            "bodyComposition": support(model.bodyCompositionType > 0),
            "bloodComposition": support(model.bloodAnalysisType > 0),
            "gsr": support(model.gsrType > 0),
            "sleep": "supported",
            "sport": support(model.runningSaveTimes > 0),
            "autoMeasure": support(model.autoMonitSwitchType > 0),
            "alarms": "unknown",
            "textAlarms": "unknown",
            "lowPower": support(model.lowPowerType > 0),
            "femaleHealth": "unknown",
            "display": support(model.screenTypes > 0),
            "findDevice": support(model.searchDeviceFunction > 0),
            "camera": "unknown",
            "weather": support(model.weatherType == 1),
            "contacts": support(model.contactType > 0),
            "gps": support(model.agpsFunction > 0),
            "watchFaces": support(model.dialCount + model.marketDialCount + model.photoDialCount > 0)
        ]
    }

    private func statusPayload() -> [String: Any] {
        let bluetoothEnabled = manager.centralManager?.state == .poweredOn
        var payload: [String: Any] = [
            "available": manager.centralManager?.state != .unsupported,
            "bluetoothEnabled": bluetoothEnabled,
            "state": connectionState,
            "capabilities": capabilitiesPayload(),
            "sdkVersion": "2.2.XX.15",
            "platform": "ios"
        ]
        if let connectedDevice {
            payload["device"] = devicePayload(connectedDevice)
        }
        return payload
    }

    private func devicePayload(_ device: VPPeripheralModel) -> [String: Any] {
        var payload: [String: Any] = [
            "id": device.deviceAddress ?? "",
            "name": device.deviceName ?? "",
            "model": device.deviceName ?? ""
        ]
        if let rssi = device.rssi {
            payload["rssi"] = rssi.intValue
        }
        if !device.deviceVersion.isEmpty {
            payload["firmware"] = device.deviceVersion
        }
        return payload
    }

    private func setConnectionState(_ state: String) {
        connectionState = state
        emitStatus()
    }

    private func emitStatus() {
        notifyListeners("statusChanged", data: statusPayload(), retainUntilConsumed: true)
    }

    private func emitData(type: String, values: [String: Any]) {
        notifyListeners(
            "data",
            data: [
                "type": type,
                "timestamp": isoFormatter.string(from: Date()),
                "values": values
            ],
            retainUntilConsumed: true
        )
    }

    private func emitLog(level: String, message: String, operation: String? = nil) {
        var payload: [String: Any] = [
            "id": UUID().uuidString,
            "timestamp": isoFormatter.string(from: Date()),
            "level": level,
            "message": message
        ]
        if let operation {
            payload["operation"] = operation
        }
        notifyListeners("log", data: payload, retainUntilConsumed: true)
    }

    private func accept(_ call: CAPPluginCall, operation: String) {
        call.resolve(["operation": operation, "accepted": true])
    }
}
