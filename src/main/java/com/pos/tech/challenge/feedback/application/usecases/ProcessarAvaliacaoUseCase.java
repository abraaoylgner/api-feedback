package com.pos.tech.challenge.feedback.application.usecases;

import com.pos.tech.challenge.feedback.domain.entities.Avaliacao;
import com.pos.tech.challenge.feedback.application.ports.AvaliacaoRepository;
import com.pos.tech.challenge.feedback.application.ports.NotificacaoGateway;

public class ProcessarAvaliacaoUseCase {

    private final AvaliacaoRepository repository;
    private final NotificacaoGateway notificacaoGateway;

    public ProcessarAvaliacaoUseCase(AvaliacaoRepository repository, NotificacaoGateway notificacaoGateway) {
        this.repository = repository;
        this.notificacaoGateway = notificacaoGateway;
    }

    public Avaliacao executar(String descricao, int nota) {
        Avaliacao avaliacao = new Avaliacao(descricao, nota);

        repository.salvar(avaliacao);

        if (avaliacao.isCritica()) {
            // Regra 2: Notificação Assíncrona. O Gateway apenas posta na Fila/Tópico.
            // O payload contém: Descrição, Urgência e Data de envio[cite: 4].
            notificacaoGateway.enviarAlertaCritico(
                    avaliacao.getDescricao(),
                    avaliacao.getUrgencia(),
                    avaliacao.getDataEnvio()
            );
        }

        return avaliacao;
    }
}