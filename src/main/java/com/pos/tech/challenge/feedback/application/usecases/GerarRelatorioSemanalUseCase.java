package com.pos.tech.challenge.feedback.application.usecases;

import com.pos.tech.challenge.feedback.adapters.dtos.RelatorioSemanalDTO;
import com.pos.tech.challenge.feedback.application.ports.AvaliacaoRepository;
import com.pos.tech.challenge.feedback.application.ports.NotificacaoGateway;
import com.pos.tech.challenge.feedback.domain.entities.Avaliacao;
import com.pos.tech.challenge.feedback.domain.enums.UrgenciaEnum;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GerarRelatorioSemanalUseCase {

    private final AvaliacaoRepository repository;
    private final NotificacaoGateway notificacaoGateway;

    public GerarRelatorioSemanalUseCase(AvaliacaoRepository repository, NotificacaoGateway notificacaoGateway) {
        this.repository = repository;
        this.notificacaoGateway = notificacaoGateway;
    }

    public RelatorioSemanalDTO executar() {
        List<Avaliacao> avaliacoes = repository.buscarTodas();

        double media = avaliacoes.stream()
                .mapToInt(Avaliacao::getNota)
                .average()
                .orElse(0.0);

        Map<LocalDate, Long> quantidadePorDia = avaliacoes.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getDataEnvio().toLocalDate(),
                        Collectors.counting()
                ));

        Map<UrgenciaEnum, Long> quantidadePorUrgencia = avaliacoes.stream()
                .collect(Collectors.groupingBy(
                        Avaliacao::getUrgencia,
                        Collectors.counting()
                ));

        List<RelatorioSemanalDTO.ItemRelatorioDTO> itens = avaliacoes.stream()
                .map(a -> new RelatorioSemanalDTO.ItemRelatorioDTO(
                        a.getDescricao(),
                        a.getUrgencia(),
                        a.getDataEnvio()
                ))
                .toList();

        RelatorioSemanalDTO relatorio = new RelatorioSemanalDTO(
                media,
                quantidadePorDia,
                quantidadePorUrgencia,
                itens
        );

        notificacaoGateway.enviarRelatorioSemanal(relatorio);
        return relatorio;
    }
}