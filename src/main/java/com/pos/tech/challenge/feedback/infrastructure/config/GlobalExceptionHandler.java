package com.pos.tech.challenge.feedback.infrastructure.config;

import com.pos.tech.challenge.feedback.adapters.dtos.ErroResponseDTO;
import com.pos.tech.challenge.feedback.domain.exceptions.DominioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({DominioException.class, IllegalArgumentException.class})
    public ResponseEntity<ErroResponseDTO> handleDominioException(
            RuntimeException ex,
            ServerWebExchange exchange) {

        String path = exchange.getRequest().getPath().value();
        log.warn("[ERRO DE VALIDACAO] Path: {} | Mensagem: {}", path, ex.getMessage());

        ErroResponseDTO erro = new ErroResponseDTO(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                path
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ErroResponseDTO> handleJsonInvalidoException(
            ServerWebInputException ex,
            ServerWebExchange exchange) {

        String path = exchange.getRequest().getPath().value();
        log.warn("[PAYLOAD INVALIDO] Path: {} | Detalhe: {}", path, ex.getMessage());

        ErroResponseDTO erro = new ErroResponseDTO(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Payload JSON inválido ou campos com formato incorreto.",
                path
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponseDTO> handleGenericException(
            Exception ex,
            ServerWebExchange exchange) {

        String path = exchange.getRequest().getPath().value();
        log.error("[ERRO INTERNO] Path: {} | Erro inesperado: ", path, ex);

        ErroResponseDTO erro = new ErroResponseDTO(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Ocorreu um erro interno inesperado ao processar a requisição.",
                path
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
    }
}