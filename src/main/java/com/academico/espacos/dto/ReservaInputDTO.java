package com.academico.espacos.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.FutureOrPresent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservaInputDTO {
    
    @NotNull(message = "O ID do espaço acadêmico é obrigatório")
    private Long espacoAcademicoId;
    
    @NotNull(message = "O ID do professor é obrigatório")
    private Long professorId;
    
    @NotNull(message = "A data da reserva é obrigatória")
    @FutureOrPresent(message = "A data da reserva deve ser hoje ou uma data futura")
    private LocalDate data;
    
    @NotNull(message = "A hora inicial é obrigatória")
    private LocalTime horaInicial;
    
    @NotNull(message = "A hora final é obrigatória")
    private LocalTime horaFinal;
    
}