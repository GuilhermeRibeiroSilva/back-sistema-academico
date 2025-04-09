package com.academico.espacos.model.enums;

/**
 * Enum que representa os possíveis status de uma reserva
 */
public enum StatusReserva {
    /**
     * Reserva agendada - aguardando o início do horário
     * (anteriormente chamado de PENDENTE)
     */
    AGENDADO("PENDENTE"),
    
    /**
     * Status legado mantido para compatibilidade com banco de dados
     * Referencia o mesmo que AGENDADO
     * @deprecated Use AGENDADO em vez disso
     */
    @Deprecated
    PENDENTE("AGENDADO"),
    
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
    
    private final String aliasValue;
    
    StatusReserva() {
        this.aliasValue = null;
    }
    
    StatusReserva(String aliasValue) {
        this.aliasValue = aliasValue;
    }
    
    public static StatusReserva fromString(String text) {
        for (StatusReserva status : StatusReserva.values()) {
            if (status.name().equalsIgnoreCase(text) || 
                (status.aliasValue != null && status.aliasValue.equalsIgnoreCase(text))) {
                return status;
            }
        }
        return AGENDADO; // Default para manter comportamento existente
    }
    
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
        return this == AGENDADO || this == PENDENTE;
    }
    
    /**
     * Verifica se a reserva pode ser cancelada com base no status
     */
    public boolean podeSerCancelada() {
        return this == AGENDADO || this == PENDENTE || this == EM_USO;
    }
    
    /**
     * Verifica se a utilização pode ser confirmada
     */
    public boolean podeConfirmarUtilizacao() {
        return this == EM_USO || this == AGUARDANDO_CONFIRMACAO;
    }
}