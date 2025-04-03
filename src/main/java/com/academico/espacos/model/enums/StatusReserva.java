package com.academico.espacos.model.enums;

/**
 * Enum que representa os possíveis status de uma reserva
 */
public enum StatusReserva {
    /**
     * Reserva pendente - aguardando o início do horário
     */
    PENDENTE,
    
    /**
     * Reserva em uso (horário atual está dentro do intervalo da reserva)
     */
    EM_USO,
    
    /**
     * Reserva aguardando confirmação de utilização (após o término)
     */
    AGUARDANDO_CONFIRMACAO,
    
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
    
    /**
     * Verifica se a reserva pode ser editada com base no status
     */
    public boolean podeSerEditada() {
        return this == PENDENTE;
    }
    
    /**
     * Verifica se a reserva pode ser cancelada com base no status
     */
    public boolean podeSerCancelada() {
        return this == PENDENTE || this == EM_USO;
    }
    
    /**
     * Verifica se a utilização pode ser confirmada
     */
    public boolean podeConfirmarUtilizacao() {
        return this == EM_USO || this == AGUARDANDO_CONFIRMACAO;
    }
}