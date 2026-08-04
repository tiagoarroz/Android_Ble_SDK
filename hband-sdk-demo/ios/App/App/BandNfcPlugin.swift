import Capacitor
import CoreNFC
import Foundation

/**
 * Bridge Core NFC dedicada às mensagens NDEF de texto usadas para identificar
 * uma pulseira. Cada chamada abre uma sessão explícita e termina após uma tag.
 */
@objc(BandNfcPlugin)
public final class BandNfcPlugin: CAPPlugin, CAPBridgedPlugin, NFCNDEFReaderSessionDelegate {
    public let identifier = "BandNfcPlugin"
    public let jsName = "BandNfc"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "getStatus", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "read", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "write", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "cancel", returnType: CAPPluginReturnPromise)
    ]

    private enum Mode { case read, write }

    private var session: NFCNDEFReaderSession?
    private var pendingCall: CAPPluginCall?
    private var mode: Mode?
    private var textToWrite: String?
    private var successMessage: String?

    @objc public func getStatus(_ call: CAPPluginCall) {
        let available = NFCNDEFReaderSession.readingAvailable
        call.resolve(["supported": available, "enabled": available])
    }

    @objc public func read(_ call: CAPPluginCall) {
        begin(call, mode: .read, text: nil)
    }

    @objc public func write(_ call: CAPPluginCall) {
        guard let text = call.getString("text"), !text.isEmpty else {
            call.reject("NFC_TEXT_REQUIRED")
            return
        }
        begin(call, mode: .write, text: text)
    }

    @objc public func cancel(_ call: CAPPluginCall) {
        finishWithError("NFC_CANCELLED")
        call.resolve()
    }

    private func begin(_ call: CAPPluginCall, mode requestedMode: Mode, text: String?) {
        guard NFCNDEFReaderSession.readingAvailable else {
            call.reject("NFC_NOT_SUPPORTED")
            return
        }
        guard pendingCall == nil else {
            call.reject("NFC_SESSION_ACTIVE")
            return
        }

        pendingCall = call
        mode = requestedMode
        textToWrite = text
        successMessage = call.getString("successMessage")
        let readerSession = NFCNDEFReaderSession(
            delegate: self,
            queue: nil,
            invalidateAfterFirstRead: false
        )
        readerSession.alertMessage = call.getString("alertMessage") ?? ""
        session = readerSession
        readerSession.begin()
    }

    /** Permite a leitura NDEF standard mesmo quando o iOS não expõe a tecnologia física da tag. */
    public func readerSession(
        _ session: NFCNDEFReaderSession,
        didDetectNDEFs messages: [NFCNDEFMessage]
    ) {
        guard mode == .read, let message = messages.first else {
            return
        }
        do {
            finishWithSuccess(["text": try text(from: message)])
        } catch let error as BandNfcError {
            finishWithError(error.code)
        } catch {
            finishWithError("NFC_IO_ERROR")
        }
    }

    public func readerSession(
        _ session: NFCNDEFReaderSession,
        didDetect tags: [NFCNDEFTag]
    ) {
        guard tags.count == 1, let tag = tags.first else {
            session.alertMessage = ""
            session.restartPolling()
            return
        }
        session.connect(to: tag) { [weak self] error in
            guard let self else { return }
            if error != nil {
                self.finishWithError("NFC_IO_ERROR")
                return
            }
            if self.mode == .write {
                self.write(to: tag)
            } else {
                self.read(from: tag)
            }
        }
    }

    public func readerSession(
        _ session: NFCNDEFReaderSession,
        didInvalidateWithError error: Error
    ) {
        guard pendingCall != nil else { return }
        let readerError = error as? NFCReaderError
        let code = readerError?.code == .readerSessionInvalidationErrorUserCanceled
            ? "NFC_CANCELLED"
            : "NFC_SESSION_INTERRUPTED"
        finishWithError(code, invalidate: false)
    }

    private func read(from tag: NFCNDEFTag) {
        tag.readNDEF { [weak self] message, error in
            guard let self else { return }
            guard error == nil, let message else {
                self.finishWithError("NFC_TAG_NOT_NDEF")
                return
            }
            do {
                self.finishWithSuccess(["text": try self.text(from: message)])
            } catch let parseError as BandNfcError {
                self.finishWithError(parseError.code)
            } catch {
                self.finishWithError("NFC_IO_ERROR")
            }
        }
    }

    private func write(to tag: NFCNDEFTag) {
        guard
            let textToWrite,
            let payload = NFCNDEFPayload.wellKnownTypeTextPayload(
                string: textToWrite,
                locale: Locale(identifier: "pt_PT")
            )
        else {
            finishWithError("NFC_TEXT_REQUIRED")
            return
        }
        let message = NFCNDEFMessage(records: [payload])
        tag.queryNDEFStatus { [weak self] status, capacity, error in
            guard let self else { return }
            guard error == nil else {
                self.finishWithError("NFC_TAG_NOT_NDEF")
                return
            }
            guard status == .readWrite else {
                self.finishWithError(status == .readOnly ? "NFC_TAG_READ_ONLY" : "NFC_TAG_NOT_NDEF")
                return
            }
            guard message.length <= capacity else {
                self.finishWithError("NFC_TAG_TOO_SMALL")
                return
            }
            tag.writeNDEF(message) { writeError in
                if writeError == nil {
                    self.finishWithSuccess([:])
                } else {
                    self.finishWithError("NFC_IO_ERROR")
                }
            }
        }
    }

    /** Extrai o primeiro registo RTD Text e respeita UTF-8 e UTF-16. */
    private func text(from message: NFCNDEFMessage) throws -> String {
        for record in message.records where record.typeNameFormat == .nfcWellKnown {
            guard record.type == Data([0x54]), let status = record.payload.first else {
                continue
            }
            let languageLength = Int(status & 0x3f)
            let offset = 1 + languageLength
            guard offset <= record.payload.count else { continue }
            let encoding: String.Encoding = (status & 0x80) == 0 ? .utf8 : .utf16
            if let value = String(data: Data(record.payload.dropFirst(offset)), encoding: encoding) {
                return value
            }
        }
        throw BandNfcError(code: "NFC_NO_TEXT_RECORD")
    }

    private func finishWithSuccess(_ result: JSObject) {
        DispatchQueue.main.async { [weak self] in
            guard let self, let call = self.pendingCall else { return }
            let activeSession = self.session
            let completedMessage = self.successMessage
            self.clearSession()
            if let completedMessage, !completedMessage.isEmpty {
                activeSession?.alertMessage = completedMessage
            }
            activeSession?.invalidate()
            call.resolve(result)
        }
    }

    private func finishWithError(_ code: String, invalidate: Bool = true) {
        DispatchQueue.main.async { [weak self] in
            guard let self, let call = self.pendingCall else { return }
            let activeSession = self.session
            self.clearSession()
            if invalidate {
                // A modal Angular apresenta o erro traduzido; o alerta nativo
                // deve apenas fechar, sem expor códigos técnicos à pessoa.
                activeSession?.invalidate()
            }
            call.reject(code)
        }
    }

    private func clearSession() {
        pendingCall = nil
        session = nil
        mode = nil
        textToWrite = nil
        successMessage = nil
    }

    private struct BandNfcError: Error {
        let code: String
    }
}
