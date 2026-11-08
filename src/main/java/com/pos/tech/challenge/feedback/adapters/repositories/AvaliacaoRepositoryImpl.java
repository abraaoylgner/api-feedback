package com.pos.tech.challenge.feedback.adapters.repositories;


import com.pos.tech.challenge.feedback.application.ports.AvaliacaoRepository;
import com.pos.tech.challenge.feedback.domain.entities.Avaliacao;
import com.pos.tech.challenge.feedback.infrastructure.persistence.AvaliacaoEntity;
import com.pos.tech.challenge.feedback.infrastructure.persistence.SpringDataAvaliacaoRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AvaliacaoRepositoryImpl implements AvaliacaoRepository {

    private final SpringDataAvaliacaoRepository jpaRepository;

    public AvaliacaoRepositoryImpl(SpringDataAvaliacaoRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void salvar(Avaliacao avaliacao) {
        AvaliacaoEntity entity = new AvaliacaoEntity(
                avaliacao.getId(),
                avaliacao.getDescricao(),
                avaliacao.getNota(),
                avaliacao.getUrgencia(),
                avaliacao.getDataEnvio()
        );
        jpaRepository.save(entity);
    }

    @Override
    public List<Avaliacao> buscarTodas() {
        return jpaRepository.findAll().stream()
                .map(entity -> new Avaliacao(
                        entity.getId(),
                        entity.getDescricao(),
                        entity.getNota(),
                        entity.getUrgencia(),
                        entity.getDataEnvio()
                ))
                .toList();
    }
}