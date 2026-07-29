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
        case "history.activity.current":
            readCurrentSteps(call, operation: operation)
        case "history.metric":
            readMetricHistory(call, operation: operation)
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

    private func readCurrentSteps(_ call: CAPPluginCall, operation: String) {
        manager.peripheralManage.veepooSDK_readStepData(withDayNumber: 0) {
            [weak self] values in
            self?.emitData(type: "steps", values: self?.stepValues(values ?? [:]) ?? [:])
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
                    "bpm": Int(model?.aveHeart ?? "0") ?? 0,
                    "hrvMilliseconds": Int(model?.aveHrv ?? "0") ?? 0,
                    "qtMilliseconds": Int(model?.aveQT ?? "0") ?? 0
                ],
                samples: model?.filterSignals.compactMap { ($0 as? NSNumber)?.doubleValue }
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
            guard let model else { return }
            self?.emitData(
                type: "bodyComposition",
                values: self?.bodyCompositionValues(model, state: state.rawValue) ?? [:]
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

    /**
     * Sincroniza primeiro a área de dados apropriada do dispositivo e só
     * consulta a base local do SDK quando este assinala a leitura como completa.
     */
    private func readMetricHistory(_ call: CAPPluginCall, operation: String) {
        guard
            let params = call.getObject("params"),
            let metric = params["metric"] as? String,
            let date = params["date"] as? String,
            isValidDate(date)
        else {
            call.reject("HISTORY_METRIC_AND_DATE_REQUIRED")
            return
        }

        if metric == "steps" {
            guard readStepHistory(call, metric: metric, date: date) else {
                return
            }
            accept(call, operation: operation)
            return
        }

        let completion: (VPReadDeviceBaseDataState, UInt, UInt, UInt) -> Void = {
            [weak self] state, _, _, _ in
            guard state == .complete else { return }
            self?.queryHistory(metric: metric, date: date)
        }

        switch metric {
        case "oxygen":
            manager.peripheralManage.veepooSdkStartReadDeviceOxygenData(completion)
        case "temperature" where manager.peripheralModel?.temperatureType != 5:
            manager.peripheralManage.veepooSdkStartReadDeviceTemperatureData(completion)
        case "heartRate", "bloodPressure", "temperature", "bloodGlucose",
             "ecg", "bodyComposition", "stress":
            manager.peripheralManage.veepooSdkStartReadDeviceAllData(readStateChange: completion)
        default:
            call.reject("HISTORY_METRIC_UNSUPPORTED:\(metric)")
            return
        }
        accept(call, operation: operation)
    }

    /**
     * Converte a data civil num deslocamento suportado pelo SDK e devolve o
     * total diário de passos, distância e calorias num único registo.
     */
    private func readStepHistory(_ call: CAPPluginCall, metric: String, date: String) -> Bool {
        guard
            let target = dateValue(date),
            let today = dateValue(dateString(Date())),
            let dayOffset = Calendar.current.dateComponents([.day], from: target, to: today).day,
            dayOffset >= 0,
            dayOffset <= Int(manager.peripheralModel?.saveDays ?? 0)
        else {
            call.reject("HISTORY_DATE_OUTSIDE_DEVICE_RETENTION")
            return false
        }

        manager.peripheralManage.veepooSDK_readStepData(withDayNumber: dayOffset) {
            [weak self] source in
            guard let self else { return }
            let values = self.stepValues(source ?? [:])
            self.emitHistory(
                metric: metric,
                date: date,
                records: [[
                    "timestamp": "\(date)T23:59:59",
                    "values": values
                ]]
            )
        }
        return true
    }

    private func stepValues(_ source: [AnyHashable: Any]) -> [String: Any] {
        [
            "steps": Int(number(source["Step"]) ?? 0),
            "distanceKm": number(source["Dis"]) ?? 0,
            "caloriesKcal": number(source["Cal"]) ?? 0
        ]
    }

    /**
     * Converte os formatos heterogéneos da base Veepoo num contrato único de
     * registos temporais consumido pelas visualizações Angular.
     */
    private func queryHistory(metric: String, date: String) {
        guard let tableID = manager.peripheralModel?.deviceAddress, !tableID.isEmpty else {
            emitLog(level: "error", message: "DEVICE_ADDRESS_UNAVAILABLE", operation: "history.metric")
            return
        }

        let records: [[String: Any]]
        switch metric {
        case "heartRate", "stress":
            let original = VPDataBaseOperation.veepooSDKGetOriginalData(
                withDate: date,
                andTableID: tableID
            ) as? [String: [String: Any]] ?? [:]
            records = original.keys.sorted().compactMap { time in
                guard let source = original[time] else { return nil }
                let key = metric == "heartRate" ? "heartValue" : "stress"
                guard let value = number(source[key]), value > 0 else { return nil }
                let field = metric == "heartRate" ? "bpm" : "score"
                let samples = metric == "heartRate"
                    ? numericArray(source["ecgs"] ?? source["ppgs"])
                    : []
                return historyRecord(
                    date: date,
                    time: time,
                    values: [field: value],
                    samples: samples
                )
            }
        case "bloodPressure":
            let source = VPDataBaseOperation.veepooSDKGetBloodData(
                withDate: date,
                andTableID: tableID
            ) as? [[String: Any]] ?? []
            records = source.map {
                historyRecord(
                    date: date,
                    time: string($0["Time"]),
                    values: [
                        "systolic": number($0["systolic"]) ?? 0,
                        "diastolic": number($0["diastolic"]) ?? 0
                    ]
                )
            }
        case "oxygen":
            let source = VPDataBaseOperation.veepooSDKGetDeviceOxygenData(
                withDate: date,
                andTableID: tableID
            ) as? [[String: Any]] ?? []
            records = source.map {
                historyRecord(
                    date: date,
                    time: string($0["Time"]),
                    values: [
                        "percent": number($0["OxygenValue"]) ?? 0,
                        "pulseBpm": number($0["HeartValue"]) ?? 0
                    ]
                )
            }
        case "temperature":
            let source = VPDataBaseOperation.veepooSDKGetDeviceTemperatureData(
                withDate: date,
                andTableID: tableID
            ) as? [[String: Any]] ?? []
            records = source.map {
                let time = String(
                    format: "%02d:%02d",
                    Int(number($0["hour"]) ?? 0),
                    Int(number($0["minute"]) ?? 0)
                )
                return historyRecord(
                    date: date,
                    time: time,
                    values: [
                        "celsius": number($0["value"]) ?? 0,
                        "surfaceCelsius": number($0["riginalValue"]) ?? 0
                    ]
                )
            }
        case "bloodGlucose":
            let source = VPDataBaseOperation.veepooSDKGetDeviceBloodGlucoseData(
                withDate: date,
                andTableID: tableID
            ) as? [[String: Any]] ?? []
            records = source.flatMap { item -> [[String: Any]] in
                let values = numericArray(item["bloodGlucoses"])
                return values.enumerated().map { index, value in
                    historyRecord(
                        date: date,
                        time: string(item["time"]),
                        values: ["mmolL": value, "sample": index + 1]
                    )
                }
            }
        case "ecg":
            let source: [VPECGTestDataModel] = VPDataBaseOperation.veepooSDKGetDeviceOffStoreECG(
                withDate: date,
                andTableID: tableID
            ) ?? []
            records = source.map { ecgHistoryRecord($0, date: date) }
        case "bodyComposition":
            let source: [VPBodyCompositionValueModel] = VPDataBaseOperation.veepooSDKGetDeviceOffStoreBodyComposition(
                withDate: date,
                andTableID: tableID
            ) ?? []
            records = source.map {
                historyRecord(
                    date: date,
                    time: $0.testTime,
                    values: bodyCompositionValues($0)
                )
            }
        default:
            records = []
        }
        emitHistory(metric: metric, date: date, records: records)
    }

    private func ecgHistoryRecord(_ model: VPECGTestDataModel, date: String) -> [String: Any] {
        let values: [String: Any] = [
            "bpm": number(model.aveHeart) ?? 0,
            "hrvMilliseconds": number(model.aveHrv) ?? 0,
            "qtMilliseconds": number(model.aveQT) ?? 0,
            "durationSeconds": number(model.duration) ?? 0
        ]
        return historyRecord(
            date: date,
            time: model.testTime,
            values: values,
            samples: numericArray(model.filterSignals)
        )
    }

    private func bodyCompositionValues(
        _ model: VPBodyCompositionValueModel,
        state: Any? = nil
    ) -> [String: Any] {
        var values: [String: Any] = [
            "bmi": Double(model.bmi) ?? 0,
            "bodyFatPercent": Double(model.bodyFatPercentage) ?? 0,
            "waterPercent": Double(model.bodyMoisture) ?? 0,
            "muscleMassKg": Double(model.muscleMass) ?? 0,
            "boneMassKg": Double(model.boneMass) ?? 0,
            "basalMetabolismKcal": Double(model.basalMetabolicRate) ?? 0
        ]
        if let state {
            values["state"] = state
        }
        return values
    }

    private func historyRecord(
        date: String,
        time: String,
        values: [String: Any],
        samples: [Double] = []
    ) -> [String: Any] {
        var record: [String: Any] = [
            "timestamp": "\(date)T\(normalisedTime(time))",
            "values": values
        ]
        if !samples.isEmpty {
            record["samples"] = samples
        }
        return record
    }

    private func emitHistory(metric: String, date: String, records: [[String: Any]]) {
        let samples = records.flatMap { ($0["samples"] as? [Double]) ?? [] }
        var payload: [String: Any] = [
            "type": "history",
            "metric": metric,
            "date": date,
            "timestamp": isoFormatter.string(from: Date()),
            "values": ["records": records.count],
            "records": records
        ]
        if !samples.isEmpty {
            payload["samples"] = samples
        }
        notifyListeners("data", data: payload, retainUntilConsumed: true)
    }

    private func number(_ value: Any?) -> Double? {
        if let number = value as? NSNumber {
            return number.doubleValue
        }
        if let text = value as? String {
            return Double(text)
        }
        return nil
    }

    private func numericArray(_ value: Any?) -> [Double] {
        (value as? [Any] ?? []).compactMap(number)
    }

    private func string(_ value: Any?) -> String {
        value as? String ?? "00:00:00"
    }

    private func normalisedTime(_ time: String) -> String {
        let parts = time.split(separator: ":")
        if parts.count == 2 {
            return "\(time):00"
        }
        return parts.count == 3 ? time : "00:00:00"
    }

    private func isValidDate(_ value: String) -> Bool {
        dateValue(value) != nil
    }

    private func dateValue(_ value: String) -> Date? {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd"
        formatter.isLenient = false
        return formatter.date(from: value)
    }

    private func dateString(_ value: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: value)
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
            "ecg": support(model.ecgType > 0),
            "bloodGlucose": support(model.bloodGlucoseType > 0),
            "fatigue": "unknown",
            "stress": support(model.stressType > 1),
            "bodyComposition": support(model.bodyCompositionType > 0),
            "bloodComposition": support(model.bloodAnalysisType > 0),
            "gsr": support(model.gsrType > 0),
            "sleep": "supported",
            "steps": support(model.saveDays > 0),
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

    private func emitData(type: String, values: [String: Any], samples: [Double]? = nil) {
        var payload: [String: Any] = [
            "type": type,
            "timestamp": isoFormatter.string(from: Date()),
            "values": values
        ]
        if let samples {
            payload["samples"] = samples
        }
        notifyListeners(
            "data",
            data: payload,
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
