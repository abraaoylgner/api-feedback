package com.pos.tech.challenge.feedback.infrastructure.config;

import com.pos.tech.challenge.feedback.application.ports.AvaliacaoRepository;
import com.pos.tech.challenge.feedback.application.ports.NotificacaoGateway;
import com.pos.tech.challenge.feedback.application.usecases.GerarRelatorioSemanalUseCase;
import com.pos.tech.challenge.feedback.application.usecases.ProcessarAvaliacaoUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public ProcessarAvaliacaoUseCase processarAvaliacaoUseCase(
            AvaliacaoRepository repository,
            NotificacaoGateway notificacaoGateway) {
        return new ProcessarAvaliacaoUseCase(repository, notificacaoGateway);
    }

    @Bean
    public GerarRelatorioSemanalUseCase gerarRelatorioSemanalUseCase(
            AvaliacaoRepository repository,
            NotificacaoGateway notificacaoGateway) {
        return new GerarRelatorioSemanalUseCase(repository, notificacaoGateway);
    }
}