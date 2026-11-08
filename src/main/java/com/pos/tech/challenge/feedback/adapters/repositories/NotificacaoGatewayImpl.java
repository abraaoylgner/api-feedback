package com.pos.tech.challenge.feedback.adapters.repositories;

import com.pos.tech.challenge.feedback.application.ports.NotificacaoGateway;
import com.pos.tech.challenge.feedback.adapters.dtos.RelatorioSemanalDTO;
import com.pos.tech.challenge.feedback.domain.enums.UrgenciaEnum;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class NotificacaoGatewayImpl implements NotificacaoGateway {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoGatewayImpl.class);

    private final ObjectProvider<SqsTemplate> sqsTemplateProvider;
    private final String filaNotasCriticas;

    public NotificacaoGatewayImpl(
            ObjectProvider<SqsTemplate> sqsTemplateProvider,
            @Value("${app.sqs.fila-criticas:}") String filaNotasCriticas) {
        this.sqsTemplateProvider = sqsTemplateProvider;
        this.filaNotasCriticas = filaNotasCriticas;
    }

    @Override
    public void enviarAlertaCritico(String descricao, UrgenciaEnum urgencia, LocalDateTime dataEnvio) {
        log.warn("[ALERTA CRITICO] Urgencia: {} | Data de Envio: {} | Descricao: {}",
                urgencia, dataEnvio, descricao);

        SqsTemplate sqsTemplate = sqsTemplateProvider.getIfAvailable();
        if (sqsTemplate != null && filaNotasCriticas != null && !filaNotasCriticas.isBlank()) {
            Map<String, Object> payload = Map.of(
                    "descricao", descricao,
                    "urgencia", urgencia.name(),
                    "dataEnvio", dataEnvio.toString()
            );
            sqsTemplate.send(to -> to.queue(filaNotasCriticas).payload(payload));
        }
    }

    @Override
    public void enviarRelatorioSemanal(RelatorioSemanalDTO relatorio) {
        log.info("[RELATORIO SEMANAL] Media: {} | Total Avaliacoes: {} | Por Dia: {} | Por Urgencia: {}",
                String.format("%.2f", relatorio.mediaAvaliacoes()),
                relatorio.avaliacoes().size(),
                relatorio.quantidadePorDia(),
                relatorio.quantidadePorUrgencia());
    }
}