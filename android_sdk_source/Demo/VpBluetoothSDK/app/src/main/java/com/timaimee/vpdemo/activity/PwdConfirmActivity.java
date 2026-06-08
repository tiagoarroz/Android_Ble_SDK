package com.timaimee.vpdemo.activity;

import static com.veepoo.protocol.model.enums.EFunctionStatus.SUPPORT;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.inuker.bluetooth.library.Code;
import com.orhanobut.logger.Logger;
import com.timaimee.vpdemo.R;
import com.timaimee.vpdemo.demo.DeviceCapabilityStore;
import com.timaimee.vpdemo.demo.DemoStepLogger;
import com.veepoo.protocol.VPOperateManager;
import com.veepoo.protocol.listener.base.IBleWriteResponse;
import com.veepoo.protocol.listener.data.ICustomSettingDataListener;
import com.veepoo.protocol.listener.data.IDeviceFuctionDataListener;
import com.veepoo.protocol.listener.data.ISocialMsgDataListener;
import com.veepoo.protocol.model.datas.DeviceFunctionPackage1;
import com.veepoo.protocol.model.datas.DeviceFunctionPackage2;
import com.veepoo.protocol.model.datas.DeviceFunctionPackage3;
import com.veepoo.protocol.model.datas.DeviceFunctionPackage4;
import com.veepoo.protocol.model.datas.DeviceFunctionPackage5;
import com.veepoo.protocol.model.datas.FunctionDeviceSupportData;
import com.veepoo.protocol.model.datas.FunctionSocailMsgData;
import com.veepoo.protocol.model.datas.PwdData;
import com.veepoo.protocol.listener.data.IPwdDataListener;
import com.veepoo.protocol.model.enums.EFunctionStatus;
import com.veepoo.protocol.model.settings.CustomSettingData;

public class PwdConfirmActivity extends AppCompatActivity {

    private static final String TAG = "-Validação de palavra-passe-";

    private EditText etPassword;
    private ScrollView svInfo;
    private Button btnConfirm, btn2Function;
    private RadioGroup rgConnectConfirm;
    private TextView tvPwdInfo, tvDeviceInfo;
    private int deviceNumber = 0;
    private String deviceVersion = "";
    private String deviceTestVersion = "";

    private StringBuilder sb = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_pwd_confirm);
        DemoStepLogger.stepStart("PWD_SCREEN_INIT", "Arranque do ecrã de validação de password");
        isOadModel = getIntent().getBooleanExtra("isoadmodel", false);
        deviceaddress = getIntent().getStringExtra("deviceaddress");

        VPOperateManager.getInstance().init(this);
        etPassword = findViewById(R.id.et_password);
        btnConfirm = findViewById(R.id.btn_confirm);
        btn2Function = findViewById(R.id.btn2Function);
        tvPwdInfo = findViewById(R.id.tv_pwd_info);
        svInfo = findViewById(R.id.svInfo);
        tvDeviceInfo = findViewById(R.id.tvDeviceInfo);
        rgConnectConfirm = findViewById(R.id.rgConnectConfirm);
        VPOperateManager.getInstance().setDeviceShowConfirm(true);//
        rgConnectConfirm.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if (i == R.id.rbNeedCC) {
                    VPOperateManager.getInstance().setDeviceShowConfirm(true);
                } else if (i == R.id.rbUnneedCC) {
                    VPOperateManager.getInstance().setDeviceShowConfirm(false);
                }
            }
        });

        btnConfirm.setEnabled(true);
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnConfirm.setEnabled(s.length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmPassword();
            }
        });
        btn2Function.setEnabled(false);
        btn2Function.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toFunctionTestPager();
            }
        });
        DemoStepLogger.stepSuccess("PWD_SCREEN_INIT", "Ecrã de validação preparado; pronto para autenticar");
    }

    /**
     * Executa o handshake obrigatório de password com o dispositivo.
     * Este passo também devolve as capacidades suportadas para orientar os testes seguintes.
     */
    private void confirmPassword() {
        String password = etPassword.getText().toString().trim();
        if (password.isEmpty()) {
            Toast.makeText(this, "Introduzir palavra-passe", Toast.LENGTH_SHORT).show();
            DemoStepLogger.stepError("PWD_VALIDATE", "Tentativa sem password");
            return;
        }

        DemoStepLogger.stepStart("PWD_VALIDATE", "Envio de password e recolha de capacidades do dispositivo");
        btnConfirm.setEnabled(false);
        btn2Function.setEnabled(false);
        DeviceCapabilityStore.reset();
        sb.setLength(0);
        tvDeviceInfo.setText("");
        tvPwdInfo.setText("A validar palavra-passe...");
        VPOperateManager.getInstance().confirmDevicePwd(new IBleWriteResponse() {
            @Override
            public void onResponse(int code) {
                if (code == Code.REQUEST_SUCCESS) {
                    tvPwdInfo.setText("Validação de palavra-passe");
                    DemoStepLogger.featureEvent("PWD_VALIDATE", "Comando de validação escrito com sucesso");
                } else {
                    tvPwdInfo.setText("Validação de palavra-passe");
                    btn2Function.setEnabled(false);
                    DemoStepLogger.stepError("PWD_VALIDATE", "Falha ao escrever comando de validação. code=" + code);
                }
            }
        }, new IPwdDataListener() {
            @Override
            public void onPwdDataChange(PwdData pwdData) {
                String message = "PwdData:\n" + pwdData.toString();
                Logger.t(TAG).i(message);
                deviceNumber = pwdData.getDeviceNumber();
                deviceVersion = pwdData.getDeviceVersion();
                deviceTestVersion = pwdData.getDeviceTestVersion();
                sb.append("dispositivo：").append(deviceNumber).append(",：").append(deviceVersion).append(", ：").append(deviceTestVersion);
                DemoStepLogger.stepSuccess("PWD_VALIDATE", "Password validada e metadata do dispositivo recebida");
            }

            @Override
            public void onConnectionConfirmTimeout() {
                tvPwdInfo.setText("Erro: tempo de confirmação de ligação esgotado");
                btn2Function.setEnabled(false);
                DemoStepLogger.stepError("PWD_VALIDATE", "Timeout de confirmação no dispositivo");
            }
        }, new IDeviceFuctionDataListener() {
            @Override
            public void onFunctionSupportDataChange(FunctionDeviceSupportData functionSupport) {
                String message = "FunctionDeviceSupportData:\n" + functionSupport.toString();
                Logger.t(TAG).i(message);
                EFunctionStatus newCalcSport = functionSupport.getNewCalcSport();
                if (newCalcSport != null && newCalcSport.equals(SUPPORT)) {
                    isNewSportCalc = true;
                } else {
                    isNewSportCalc = false;
                }
                watchDataDay = functionSupport.getWathcDay();
                weatherStyle = functionSupport.getWeatherStyle();
                contactMsgLength = functionSupport.getContactMsgLength();
                allMsgLenght = functionSupport.getAllMsgLength();
                isSleepPrecision = functionSupport.getPrecisionSleep() == SUPPORT;
                DeviceCapabilityStore.setFunctionSupport(functionSupport);
                DemoStepLogger.featureEvent("PWD_CAPABILITIES", "Pacote principal de capacidades recebido");
            }

            @Override
            public void onDeviceFunctionPackage1Report(DeviceFunctionPackage1 functionPackage1) {
                String message = "funcionalidade1:\n" + functionPackage1.toString();
                DeviceCapabilityStore.setPackage1(functionPackage1);
                appendDeviceInfo(message);
                Logger.t(TAG).i(message);
            }

            @Override
            public void onDeviceFunctionPackage2Report(DeviceFunctionPackage2 functionPackage2) {
                String message = "funcionalidade2:\n" + functionPackage2.toString();
                DeviceCapabilityStore.setPackage2(functionPackage2);
                appendDeviceInfo(message);
                Logger.t(TAG).i(message);
            }

            @Override
            public void onDeviceFunctionPackage3Report(DeviceFunctionPackage3 functionPackage3) {
                String message = "funcionalidade3:\n" + functionPackage3.toString();
                DeviceCapabilityStore.setPackage3(functionPackage3);
                appendDeviceInfo(message);
                Logger.t(TAG).i(message);
            }

            @Override
            public void onDeviceFunctionPackage4Report(DeviceFunctionPackage4 functionPackage4) {
                String message = "funcionalidade4:\n" + functionPackage4.toString();
                DeviceCapabilityStore.setPackage4(functionPackage4);
                appendDeviceInfo(message);
                Logger.t(TAG).i(message);
            }

            @Override
            public void onDeviceFunctionPackage5Report(DeviceFunctionPackage5 functionPackage5) {
                String message = "funcionalidade5:\n" + functionPackage5.toString();
                DeviceCapabilityStore.setPackage5(functionPackage5);
                appendDeviceInfo(message);
                Logger.t(TAG).i(message);
            }
        }, new ISocialMsgDataListener() {
            @Override
            public void onSocialMsgSupportDataChange(FunctionSocailMsgData socailMsgData) {
                String message = "1:\n" + socailMsgData.toString();
                DeviceCapabilityStore.setSocialMessageSupport(socailMsgData);
                appendDeviceInfo(message);
                Logger.t(TAG).i(message);
            }

            @Override
            public void onSocialMsgSupportDataChange2(FunctionSocailMsgData socailMsgData) {
                String message = "2:\n" + socailMsgData.toString();
                DeviceCapabilityStore.setSocialMessageSupport(socailMsgData);
                appendDeviceInfo(message);
                Logger.t(TAG).i(message);
            }
        }, new ICustomSettingDataListener() {
            @Override
            public void OnSettingDataChange(CustomSettingData customSettingData) {
                btnConfirm.setEnabled(true);
                btn2Function.setEnabled(true);
                String message = "Configurar:\n" + customSettingData.toString();
                appendDeviceInfo(message);
                Logger.t(TAG).i(message);
                DemoStepLogger.stepSuccess("PWD_VALIDATE", "Handshake concluído; botões de teste desbloqueados");
            }
        }, password, true);
    }

    private void appendDeviceInfo1(String msg) {
        runOnUiThread(() -> {
            sb.append("\n").append("#############################");
            sb.append("\n").append(msg);
            tvDeviceInfo.setText(sb.toString());
            svInfo.fullScroll(View.FOCUS_DOWN);
        });
    }

    private void appendDeviceInfo(String msg) {
        runOnUiThread(() -> {
            String line = "########################";
            String coloredLine = "<font color='#0000FF' size='16'>" + line + "</font>";
            String styledMsg = msg.replaceAll("([^]+)", "<font color='#FF0000'><b>$1</b></font>");
            sb.append("\n").append(coloredLine);
            sb.append("\n").append(styledMsg).append("\n");
            tvDeviceInfo.setText(android.text.Html.fromHtml(sb.toString()));
            svInfo.fullScroll(View.FOCUS_DOWN);
        });
    }

    private int watchDataDay = 0;
    private int weatherStyle = 0;
    private int contactMsgLength = 0;
    private int allMsgLenght = 0;
    private boolean isSleepPrecision = false;
    private boolean isNewSportCalc = false;
    private boolean isOadModel = false;
    private String deviceaddress = "";

    private void toFunctionTestPager() {
        DemoStepLogger.stepStart("OPERATE_SCREEN_OPEN", "Pedido de entrada no ecrã de funcionalidades");
        new AlertDialog.Builder(this)
                .setTitle("Ir para teste de funcionalidades")
                .setMessage("Nota: só pode testar funcionalidades suportadas pelo dispositivo atual. Confirme o suporte nos callbacks de IDeviceFuctionDataListener.")
                .setPositiveButton("Confirmar", (dialog, which) -> {
                    // Confirmar
                    Intent intent = new Intent(this, OperaterActivity.class);
                    intent.putExtra("password_confirmed", true);
                    intent.putExtra("deviceNumber", deviceNumber);
                    intent.putExtra("deviceVersion", deviceVersion);
                    intent.putExtra("deviceTestVersion", deviceTestVersion);
                    intent.putExtra("watchDataDay", watchDataDay);
                    intent.putExtra("weatherStyle", weatherStyle);
                    intent.putExtra("contactMsgLength", contactMsgLength);
                    intent.putExtra("allMsgLenght", allMsgLenght);
                    intent.putExtra("isSleepPrecision", isSleepPrecision);
                    intent.putExtra("isNewSportCalc", isNewSportCalc);
                    intent.putExtra("isOadModel", isOadModel);
                    intent.putExtra("deviceaddress", deviceaddress);
                    intent.putExtra("hasDeviceCapabilities", DeviceCapabilityStore.hasDeviceCapabilities());
                    startActivity(intent);
                    DemoStepLogger.stepSuccess("OPERATE_SCREEN_OPEN", "Transição para OperaterActivity concluída");
                    dialog.dismiss();
                    finish();
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {
                    // Cancelar
                    dialog.dismiss();
                })
                .setCancelable(true) // Desativar
                .show();
    }
}
