package com.academico.espacos.model;

import com.academico.espacos.model.enums.StatusReserva;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

@Entity
@Table(name = "reservas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reserva {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "espaco_id", nullable = false)
    private EspacoAcademico espacoAcademico;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "professor_id", nullable = false)
    private Professor professor;
    
    @Column(nullable = false)
    private LocalDate data;
    
    @Column(name = "hora_inicial")
    private LocalTime horaInicial;
    
    @Column(name = "hora_final")
    private LocalTime horaFinal;
    
    @Column(length = 500)
    private String finalidade;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusReserva status;
    
    @Column
    private LocalDateTime dataCriacao;
    
    @Column
    private LocalDateTime dataAtualizacao;
    
    @Column
    private LocalDateTime dataUtilizacao;
    
    @PrePersist
    public void prePersist() {
        this.dataCriacao = LocalDateTime.now();
        if (this.status == null) {
            this.status = StatusReserva.AGENDADO;
        }
    }
    
    @PreUpdate
    public void preUpdate() {
        this.dataAtualizacao = LocalDateTime.now();
    }
    
    /**
     * Confirma a utilização da reserva
     * @return true se a operação foi bem sucedida, false caso contrário
     */
    public boolean confirmarUtilizacao() {
        if (this.status == StatusReserva.EM_USO || this.status == StatusReserva.AGUARDANDO_CONFIRMACAO) {
            this.status = StatusReserva.UTILIZADO;
            this.dataUtilizacao = LocalDateTime.now();
            return true;
        }
        return false;
    }
    
    /**
     * Cancela a reserva
     * @return true se a operação foi bem sucedida, false caso contrário
     */
    public boolean cancelar() {
        if (this.status == StatusReserva.AGENDADO || this.status == StatusReserva.PENDENTE 
                || this.status == StatusReserva.EM_USO) {
            this.status = StatusReserva.CANCELADO;
            return true;
        }
        return false;
    }
    
    /**
     * Verifica se a reserva está em estado que permite edição
     * @return true se pode ser editada, false caso contrário
     */
    public boolean podeSerEditada() {
        return this.status == StatusReserva.AGENDADO || this.status == StatusReserva.PENDENTE;
    }
    
    /**
     * Verifica se o horário da reserva é válido
     * @return true se o horário final é após o inicial, false caso contrário
     */
    public boolean horarioValido() {
        return horaInicial != null && horaFinal != null && !horaFinal.isBefore(horaInicial);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reserva reserva = (Reserva) o;
        return Objects.equals(id, reserva.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Reserva{" +
                "id=" + id +
                ", espaco='" + (espacoAcademico != null ? espacoAcademico.getId() : null) + '\'' +
                ", professor='" + (professor != null ? professor.getId() : null) + '\'' +
                ", data=" + data +
                ", horário=" + horaInicial + " - " + horaFinal +
                ", status=" + status +
                '}';
    }
}