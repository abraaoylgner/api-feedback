package com.pos.tech.challenge.feedback.application.ports;

import com.pos.tech.challenge.feedback.domain.entities.Avaliacao;
import java.util.List;

public interface AvaliacaoRepository {
    void salvar(Avaliacao avaliacao);
    List<Avaliacao> buscarTodas();
}
