package com.timaimee.vpdemo.demo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Normaliza textos devolvidos pelo SDK para português europeu.
 * Algumas classes fechadas do SDK expõem `toString()` com etiquetas em chinês;
 * esta camada mantém os logs da demo legíveis sem alterar o protocolo BLE.
 */
public final class DemoTextTranslator {

    private static final Map<String, String> TRANSLATIONS = new LinkedHashMap<>();

    static {
        TRANSLATIONS.put("设备编号", "número do dispositivo");
        TRANSLATIONS.put("设备版本", "versão do dispositivo");
        TRANSLATIONS.put("设备测试版本", "versão de teste do dispositivo");
        TRANSLATIONS.put("是否有饮酒数据", "tem dados de ingestão de álcool");
        TRANSLATIONS.put("抬手亮屏", "ativar ecrã ao levantar o pulso");
        TRANSLATIONS.put("设置防丢", "configuração anti-perda");
        TRANSLATIONS.put("佩戴检测", "deteção de uso");
        TRANSLATIONS.put("功能第1包", "pacote de funcionalidades 1");
        TRANSLATIONS.put("功能第2包", "pacote de funcionalidades 2");
        TRANSLATIONS.put("功能第3包", "pacote de funcionalidades 3");
        TRANSLATIONS.put("功能第4包", "pacote de funcionalidades 4");
        TRANSLATIONS.put("功能第5包", "pacote de funcionalidades 5");
        TRANSLATIONS.put("消息开关第1包", "pacote de notificações 1");
        TRANSLATIONS.put("消息开关第2包", "pacote de notificações 2");
        TRANSLATIONS.put("开关设置", "configuração de interruptores");
        TRANSLATIONS.put("同步个人信息", "sincronizar informação pessoal");
        TRANSLATIONS.put("密码校验指令写入成功", "comando de validação da password escrito com sucesso");
        TRANSLATIONS.put("连接成功", "ligação estabelecida com sucesso");
        TRANSLATIONS.put("监听成功-可进行其他操作", "notificações ativas; pode executar outras operações");
        TRANSLATIONS.put("开始", "início");
        TRANSLATIONS.put("结束", "fim");
        TRANSLATIONS.put("读取", "ler");
        TRANSLATIONS.put("设置", "configurar");
        TRANSLATIONS.put("打开", "ativar");
        TRANSLATIONS.put("关闭", "desativar");
        TRANSLATIONS.put("成功", "sucesso");
        TRANSLATIONS.put("失败", "falha");
        TRANSLATIONS.put("状态", "estado");
        TRANSLATIONS.put("数据", "dados");
        TRANSLATIONS.put("心率", "frequência cardíaca");
        TRANSLATIONS.put("血压", "pressão arterial");
        TRANSLATIONS.put("血氧", "SpO2");
        TRANSLATIONS.put("温度", "temperatura");
        TRANSLATIONS.put("睡眠", "sono");
        TRANSLATIONS.put("步数", "passos");
        TRANSLATIONS.put("电池", "bateria");
        TRANSLATIONS.put("闹钟", "alarme");
        TRANSLATIONS.put("久坐", "sedentarismo");
        TRANSLATIONS.put("语言", "idioma");
        TRANSLATIONS.put("中文", "chinês");
        TRANSLATIONS.put("英文", "inglês");
        TRANSLATIONS.put("天气", "meteorologia");
        TRANSLATIONS.put("运动模式", "modo desportivo");
        TRANSLATIONS.put("身体成分", "composição corporal");
        TRANSLATIONS.put("血液成分", "composição sanguínea");
        TRANSLATIONS.put("血糖", "glicose");
        TRANSLATIONS.put("联系人", "contactos");
        TRANSLATIONS.put("固件升级", "atualização de firmware");
        TRANSLATIONS.put("手表", "relógio");
        TRANSLATIONS.put("手环", "pulseira");
        TRANSLATIONS.put("设备", "dispositivo");
        TRANSLATIONS.put("手机", "telefone");
    }

    private DemoTextTranslator() {
        // Classe utilitária; não deve ser instanciada.
    }

    /**
     * Traduz termos chineses conhecidos preservando os valores técnicos originais.
     *
     * @param text texto vindo da demo ou do SDK.
     * @return texto com etiquetas conhecidas traduzidas para português.
     */
    public static String translate(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String translated = text;
        for (Map.Entry<String, String> entry : TRANSLATIONS.entrySet()) {
            translated = translated.replace(entry.getKey(), entry.getValue());
        }
        translated = translated.replace('：', ':');
        translated = translated.replace('，', ',');
        translated = translated.replace('【', '[');
        translated = translated.replace('】', ']');
        return translated;
    }
}
