package com.academico.espacos.model.enums;

/**
 * Enum que representa os possíveis status de uma reserva
 */
public enum StatusReserva {
    /**
     * Reserva pendente de aprovação
     */
    PENDENTE,
    
    /**
     * Reserva em uso (horário atual está dentro do intervalo da reserva)
     */
    EM_USO,
    
    /**
     * Reserva já utilizada (concluída)
     */
    UTILIZADO,
    
    /**
     * Reserva cancelada pelo professor ou administrador
     */
    CANCELADO;
    
    /**
     * Verifica se o status representa uma reserva ativa (não cancelada)
     */
    public boolean isAtivo() {
        return this != CANCELADO;
    }
    
    /**
     * Verifica se o status representa uma reserva concluída
     */
    public boolean isConcluido() {
        return this == UTILIZADO;
    }
}