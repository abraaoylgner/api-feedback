package com.pos.tech.challenge.feedback.domain.entities;

import com.pos.tech.challenge.feedback.domain.enums.UrgenciaEnum;
import com.pos.tech.challenge.feedback.domain.exceptions.DominioException;

import java.time.LocalDateTime;
import java.util.UUID;

public class Avaliacao {
    private UUID id;
    private String descricao;
    private int nota;
    private UrgenciaEnum urgencia;
    private LocalDateTime dataEnvio;

    public Avaliacao(String descricao, int nota) {
        validar(descricao, nota);
        this.id = UUID.randomUUID();
        this.descricao = descricao.trim();
        this.nota = nota;
        this.dataEnvio = LocalDateTime.now();
        this.urgencia = classificarUrgencia(nota);
    }

    public Avaliacao(UUID id, String descricao, int nota, UrgenciaEnum urgencia, LocalDateTime dataEnvio) {
        this.id = id;
        this.descricao = descricao;
        this.nota = nota;
        this.urgencia = urgencia;
        this.dataEnvio = dataEnvio;
    }

    private void validar(String descricao, int nota) {
        if (nota < 0 || nota > 10) {
            throw new DominioException("Nota deve ser entre 0 e 10");
        }
        if (descricao == null || descricao.isBlank()) {
            throw new DominioException("A descrição da avaliação é obrigatória");
        }
    }

    private UrgenciaEnum classificarUrgencia(int nota) {
        if (nota <= 3) return UrgenciaEnum.ALTA;
        if (nota <= 5) return UrgenciaEnum.MEDIA;
        if (nota <= 7) return UrgenciaEnum.BAIXA;
        return UrgenciaEnum.NENHUMA;
    }

    public boolean isCritica() {
        return this.urgencia == UrgenciaEnum.ALTA || this.urgencia == UrgenciaEnum.MEDIA;
    }

    public UUID getId() {
        return id;
    }

    public String getDescricao() {
        return descricao;
    }

    public int getNota() {
        return nota;
    }

    public UrgenciaEnum getUrgencia() {
        return urgencia;
    }

    public LocalDateTime getDataEnvio() {
        return dataEnvio;
    }
}