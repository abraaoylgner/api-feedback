package com.pos.tech.challenge.feedback.adapters.dtos;

import java.time.LocalDateTime;

public record ErroResponseDTO(
        LocalDateTime timestamp,
        int status,
        String erro,
        String mensagem,
        String path
) {}