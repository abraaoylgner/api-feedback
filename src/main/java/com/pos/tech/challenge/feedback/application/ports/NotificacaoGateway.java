package com.pos.tech.challenge.feedback.application.ports;

import com.pos.tech.challenge.feedback.adapters.dtos.RelatorioSemanalDTO;
import com.pos.tech.challenge.feedback.domain.enums.UrgenciaEnum;

import java.time.LocalDateTime;

public interface NotificacaoGateway {
    void enviarAlertaCritico(String descricao, UrgenciaEnum urgencia, LocalDateTime dataEnvio);
    void enviarRelatorioSemanal(RelatorioSemanalDTO relatorio);
}