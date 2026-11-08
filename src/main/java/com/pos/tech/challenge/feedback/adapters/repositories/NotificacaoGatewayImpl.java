package com.pos.tech.challenge.feedback.adapters.repositories;

import com.pos.tech.challenge.feedback.adapters.dtos.RelatorioSemanalDTO;
import com.pos.tech.challenge.feedback.application.ports.NotificacaoGateway;
import com.pos.tech.challenge.feedback.domain.enums.UrgenciaEnum;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class NotificacaoGatewayImpl implements NotificacaoGateway {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoGatewayImpl.class);

    private final ObjectProvider<SqsTemplate> sqsTemplateProvider;
    private final ObjectProvider<SesClient> sesClientProvider;
    private final String filaNotasCriticas;
    private final String emailAdmin;

    public NotificacaoGatewayImpl(
            ObjectProvider<SqsTemplate> sqsTemplateProvider,
            ObjectProvider<SesClient> sesClientProvider,
            @Value("${app.sqs.fila-criticas:}") String filaNotasCriticas,
            @Value("${app.mail.admin:admin@fiap.com}") String emailAdmin) {
        this.sqsTemplateProvider = sqsTemplateProvider;
        this.sesClientProvider = sesClientProvider;
        this.filaNotasCriticas = filaNotasCriticas;
        this.emailAdmin = emailAdmin;
    }

    @Override
    public void enviarAlertaCritico(String descricao, UrgenciaEnum urgencia, LocalDateTime dataEnvio) {
        log.warn("[ALERTA CRITICO] Urgencia: {} | Data de Envio: {} | Descricao: {}",
                urgencia, dataEnvio, descricao);

        // 1. Publica evento na fila SQS para processamento assíncrono
        SqsTemplate sqsTemplate = sqsTemplateProvider.getIfAvailable();
        if (sqsTemplate != null && filaNotasCriticas != null && !filaNotasCriticas.isBlank()) {
            Map<String, Object> payload = Map.of(
                    "descricao", descricao,
                    "urgencia", urgencia.name(),
                    "dataEnvio", dataEnvio.toString()
            );
            sqsTemplate.send(to -> to.queue(filaNotasCriticas).payload(payload));
        }

        // 2. Dispara e-mail de aviso de urgência aos administradores via Amazon SES
        String assunto = "[URGENTE] Feedback Crítico Recebido - Urgência: " + urgencia;
        String corpo = String.format(
                """
                Alerta de Avaliação Crítica detectada na plataforma:
                
                - Descrição: %s
                - Urgência: %s
                - Data de envio: %s
                """,
                descricao, urgencia, dataEnvio
        );

        enviarEmailSes(assunto, corpo);
    }

    @Override
    public void enviarRelatorioSemanal(RelatorioSemanalDTO relatorio) {
        log.info("[RELATORIO SEMANAL] Media: {} | Total Avaliacoes: {} | Por Dia: {} | Por Urgencia: {}",
                String.format("%.2f", relatorio.mediaAvaliacoes()),
                relatorio.avaliacoes().size(),
                relatorio.quantidadePorDia(),
                relatorio.quantidadePorUrgencia());

        StringBuilder sb = new StringBuilder();
        sb.append("Relatório Semanal de Feedbacks\n");
        sb.append("=========================================\n");
        sb.append(String.format("Média de Avaliações: %.2f\n\n", relatorio.mediaAvaliacoes()));

        sb.append("Quantidade de avaliações por dia:\n");
        relatorio.quantidadePorDia().forEach((dia, qtd) ->
                sb.append(String.format(" - %s: %d\n", dia, qtd)));

        sb.append("\nQuantidade de avaliações por urgência:\n");
        relatorio.quantidadePorUrgencia().forEach((urg, qtd) ->
                sb.append(String.format(" - %s: %d\n", urg, qtd)));

        sb.append("\nDetalhamento das Avaliações:\n");
        for (RelatorioSemanalDTO.ItemRelatorioDTO item : relatorio.avaliacoes()) {
            sb.append(String.format(" - Data: %s | Urgência: %s | Descrição: %s\n",
                    item.dataEnvio(), item.urgencia(), item.descricao()));
        }

        enviarEmailSes("Relatório Semanal de Feedbacks - Pós-Tech", sb.toString());
    }

    private void enviarEmailSes(String assunto, String corpoTexto) {
        SesClient sesClient = sesClientProvider.getIfAvailable();
        if (sesClient != null && emailAdmin != null && !emailAdmin.isBlank()) {
            SendEmailRequest request = SendEmailRequest.builder()
                    .source(emailAdmin)
                    .destination(Destination.builder().toAddresses(emailAdmin).build())
                    .message(Message.builder()
                            .subject(Content.builder().data(assunto).charset("UTF-8").build())
                            .body(Body.builder()
                                    .text(Content.builder().data(corpoTexto).charset("UTF-8").build())
                                    .build())
                            .build())
                    .build();
            sesClient.sendEmail(request);
        }
    }
}