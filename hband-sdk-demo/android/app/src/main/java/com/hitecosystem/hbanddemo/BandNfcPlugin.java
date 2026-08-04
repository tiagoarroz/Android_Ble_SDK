package com.hitecosystem.hbanddemo;

import android.app.Activity;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bridge NFC limitada a mensagens NDEF de texto da aplicação H Band.
 * Uma sessão mantém uma única chamada pendente até a etiqueta ser aproximada.
 */
@CapacitorPlugin(name = "BandNfc")
public class BandNfcPlugin extends Plugin {
    private enum Mode { READ, WRITE }

    private NfcAdapter adapter;
    private PluginCall pendingCall;
    private Mode mode;
    private String textToWrite;
    private final AtomicBoolean processing = new AtomicBoolean(false);

    @Override
    public void load() {
        adapter = NfcAdapter.getDefaultAdapter(getContext());
    }

    @PluginMethod
    public void getStatus(PluginCall call) {
        JSObject result = new JSObject();
        result.put("supported", adapter != null);
        result.put("enabled", adapter != null && adapter.isEnabled());
        call.resolve(result);
    }

    @PluginMethod
    public void read(PluginCall call) {
        beginSession(call, Mode.READ, null);
    }

    @PluginMethod
    public void write(PluginCall call) {
        String text = call.getString("text");
        if (text == null || text.isEmpty()) {
            call.reject("NFC_TEXT_REQUIRED");
            return;
        }
        beginSession(call, Mode.WRITE, text);
    }

    @PluginMethod
    public void cancel(PluginCall call) {
        rejectPending("NFC_CANCELLED");
        call.resolve();
    }

    @Override
    protected void handleOnPause() {
        super.handleOnPause();
        rejectPending("NFC_SESSION_INTERRUPTED");
    }

    private synchronized void beginSession(PluginCall call, Mode requestedMode, String text) {
        if (adapter == null) {
            call.reject("NFC_NOT_SUPPORTED");
            return;
        }
        if (!adapter.isEnabled()) {
            call.reject("NFC_NOT_ENABLED");
            return;
        }
        if (pendingCall != null) {
            call.reject("NFC_SESSION_ACTIVE");
            return;
        }
        Activity activity = getActivity();
        if (activity == null) {
            call.reject("NFC_ACTIVITY_UNAVAILABLE");
            return;
        }

        pendingCall = call;
        mode = requestedMode;
        textToWrite = text;
        processing.set(false);
        bridge.executeOnMainThread(() -> adapter.enableReaderMode(
            activity,
            this::handleTag,
            NfcAdapter.FLAG_READER_NFC_A
                | NfcAdapter.FLAG_READER_NFC_B
                | NfcAdapter.FLAG_READER_NFC_F
                | NfcAdapter.FLAG_READER_NFC_V,
            null
        ));
    }

    private void handleTag(Tag tag) {
        if (!processing.compareAndSet(false, true)) {
            return;
        }
        try {
            if (mode == Mode.WRITE) {
                writeText(tag, textToWrite);
                resolvePending(new JSObject());
                return;
            }
            JSObject result = new JSObject();
            result.put("text", readText(tag));
            resolvePending(result);
        } catch (NfcOperationException error) {
            rejectPending(error.getMessage());
        } catch (Exception error) {
            rejectPending("NFC_IO_ERROR");
        }
    }

    /** Lê o primeiro registo RTD Text e respeita o idioma e a codificação do payload. */
    private String readText(Tag tag) throws Exception {
        Ndef ndef = Ndef.get(tag);
        if (ndef == null) {
            throw new NfcOperationException("NFC_TAG_NOT_NDEF");
        }
        try {
            ndef.connect();
            NdefMessage message = ndef.getNdefMessage();
            if (message == null) {
                message = ndef.getCachedNdefMessage();
            }
            if (message == null) {
                throw new NfcOperationException("NFC_NO_TEXT_RECORD");
            }
            for (NdefRecord record : message.getRecords()) {
                if (
                    record.getTnf() == NdefRecord.TNF_WELL_KNOWN
                        && Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)
                ) {
                    byte[] payload = record.getPayload();
                    if (payload.length == 0) {
                        break;
                    }
                    int status = payload[0] & 0xff;
                    int languageLength = status & 0x3f;
                    int textOffset = 1 + languageLength;
                    if (textOffset > payload.length) {
                        break;
                    }
                    Charset charset = (status & 0x80) == 0
                        ? StandardCharsets.UTF_8
                        : StandardCharsets.UTF_16;
                    return new String(payload, textOffset, payload.length - textOffset, charset);
                }
            }
            throw new NfcOperationException("NFC_NO_TEXT_RECORD");
        } finally {
            if (ndef.isConnected()) {
                ndef.close();
            }
        }
    }

    /** Substitui a mensagem NDEF existente ou formata uma etiqueta ainda vazia. */
    private void writeText(Tag tag, String text) throws Exception {
        NdefMessage message = new NdefMessage(new NdefRecord[] {
            NdefRecord.createTextRecord("pt", text)
        });
        Ndef ndef = Ndef.get(tag);
        if (ndef != null) {
            try {
                ndef.connect();
                if (!ndef.isWritable()) {
                    throw new NfcOperationException("NFC_TAG_READ_ONLY");
                }
                if (ndef.getMaxSize() < message.toByteArray().length) {
                    throw new NfcOperationException("NFC_TAG_TOO_SMALL");
                }
                ndef.writeNdefMessage(message);
                return;
            } finally {
                if (ndef.isConnected()) {
                    ndef.close();
                }
            }
        }

        NdefFormatable formatable = NdefFormatable.get(tag);
        if (formatable == null) {
            throw new NfcOperationException("NFC_TAG_NOT_NDEF");
        }
        try {
            formatable.connect();
            formatable.format(message);
        } finally {
            if (formatable.isConnected()) {
                formatable.close();
            }
        }
    }

    private synchronized void resolvePending(JSObject result) {
        PluginCall call = pendingCall;
        clearSession();
        if (call != null) {
            call.resolve(result);
        }
    }

    private synchronized void rejectPending(String code) {
        PluginCall call = pendingCall;
        clearSession();
        if (call != null) {
            call.reject(code);
        }
    }

    private void clearSession() {
        Activity activity = getActivity();
        if (adapter != null && activity != null) {
            bridge.executeOnMainThread(() -> adapter.disableReaderMode(activity));
        }
        pendingCall = null;
        mode = null;
        textToWrite = null;
        processing.set(false);
    }

    private static final class NfcOperationException extends Exception {
        NfcOperationException(String code) {
            super(code);
        }
    }
}
