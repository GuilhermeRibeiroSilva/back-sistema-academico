package com.academico.espacos.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisponibilidadeDTO {
    
    @NotNull(message = "A data é obrigatória")
    private LocalDate data;
    
    @NotNull(message = "A hora inicial é obrigatória")
    private LocalTime horaInicial;
    
    @NotNull(message = "A hora final é obrigatória")
    private LocalTime horaFinal;
    
    @NotNull(message = "O ID do espaço é obrigatório")
    private Long espacoId;
    
    /**
     * ID da reserva a ser ignorada na verificação de disponibilidade.
     * Utilizado durante edições para que a própria reserva não seja 
     * considerada um conflito.
     */
    private Long reservaId;
}