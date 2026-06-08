package com.timaimee.vpdemo.demo;

import com.orhanobut.logger.Logger;

/**
 * Utilitário central de logging da aplicação de demonstração.
 * Padroniza os registos para facilitar a validação dos fluxos em runtime.
 */
public final class DemoStepLogger {

    private static final String TAG = "SDK_DEMO";

    private DemoStepLogger() {
        // Classe utilitária; não deve ser instanciada.
    }

    /**
     * Regista o início de um passo funcional.
     *
     * @param stepId identificador técnico curto do passo.
     * @param description descrição legível do que vai ser executado.
     */
    public static void stepStart(String stepId, String description) {
        Logger.t(TAG).i("[START][" + stepId + "] " + description);
    }

    /**
     * Regista sucesso de um passo funcional.
     *
     * @param stepId identificador técnico curto do passo.
     * @param details detalhe adicional do resultado.
     */
    public static void stepSuccess(String stepId, String details) {
        Logger.t(TAG).i("[SUCCESS][" + stepId + "] " + details);
    }

    /**
     * Regista falha de um passo funcional.
     *
     * @param stepId identificador técnico curto do passo.
     * @param details detalhe do erro/estado de falha.
     */
    public static void stepError(String stepId, String details) {
        Logger.t(TAG).e("[ERROR][" + stepId + "] " + details);
    }

    /**
     * Regista evento informativo de uma funcionalidade.
     *
     * @param feature nome curto da funcionalidade.
     * @param details detalhe do evento.
     */
    public static void featureEvent(String feature, String details) {
        Logger.t(TAG).d("[FEATURE][" + feature + "] " + details);
    }
}
