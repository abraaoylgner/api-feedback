package com.pos.tech.challenge.feedback.adapters.dtos;

import com.pos.tech.challenge.feedback.domain.enums.UrgenciaEnum;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record RelatorioSemanalDTO(
        double mediaAvaliacoes,
        Map<LocalDate, Long> quantidadePorDia,
        Map<UrgenciaEnum, Long> quantidadePorUrgencia,
        List<ItemRelatorioDTO> avaliacoes
) {
    public record ItemRelatorioDTO(
            String descricao,
            UrgenciaEnum urgencia,
            LocalDateTime dataEnvio
    ) {}
}