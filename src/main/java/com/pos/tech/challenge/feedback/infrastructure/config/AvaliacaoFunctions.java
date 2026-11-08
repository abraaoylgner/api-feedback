package com.pos.tech.challenge.feedback.infrastructure.config;

import com.pos.tech.challenge.feedback.adapters.dtos.AvaliacaoRequestDTO;
import com.pos.tech.challenge.feedback.adapters.dtos.AvaliacaoResponseDTO;
import com.pos.tech.challenge.feedback.application.usecases.GerarRelatorioSemanalUseCase;
import com.pos.tech.challenge.feedback.application.usecases.ProcessarAvaliacaoUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;
import java.util.function.Function;

@Configuration
public class AvaliacaoFunctions {

    // Regra 1: Recepção. API Gateway faz POST para esta função.
    @Bean
    public Function<AvaliacaoRequestDTO, AvaliacaoResponseDTO> processarAvaliacao(
            ProcessarAvaliacaoUseCase useCase) {

        return request -> {
            var avaliacao = useCase.executar(request.descricao(), request.nota());
            return new AvaliacaoResponseDTO(avaliacao.getId(), "Avaliação registrada com sucesso.");
        };
    }

    // Regra 3: Relatórios. EventBridge aciona esta função semanalmente.
    @Bean
    public Consumer<Void> gerarRelatorioSemanal(
            GerarRelatorioSemanalUseCase useCase) {

        return input -> {
            // Aciona o caso de uso que agrupa e envia o relatório
            // Contendo: Descrição, Urgência, Data, Qtd/dia, Qtd/urgência[cite: 4].
            useCase.executar();
        };
    }
}

