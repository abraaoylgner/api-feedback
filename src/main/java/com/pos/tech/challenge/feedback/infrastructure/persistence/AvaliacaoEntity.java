package com.pos.tech.challenge.feedback.infrastructure.persistence;

import com.pos.tech.challenge.feedback.domain.enums.UrgenciaEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "avaliacoes")
public class AvaliacaoEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 1000)
    private String descricao;

    @Column(nullable = false)
    private int nota;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UrgenciaEnum urgencia;

    @Column(name = "data_envio", nullable = false)
    private LocalDateTime dataEnvio;

    public AvaliacaoEntity() {
    }

    public AvaliacaoEntity(UUID id, String descricao, int nota, UrgenciaEnum urgencia, LocalDateTime dataEnvio) {
        this.id = id;
        this.descricao = descricao;
        this.nota = nota;
        this.urgencia = urgencia;
        this.dataEnvio = dataEnvio;
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