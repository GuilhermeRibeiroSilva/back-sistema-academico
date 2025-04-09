package com.academico.espacos.model;

import com.academico.espacos.model.enums.StatusReserva;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "reservas")
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
    private StatusReserva status = StatusReserva.AGENDADO;
    
    @Column
    private LocalDateTime dataCriacao = LocalDateTime.now();
    
    @Column
    private LocalDateTime dataAtualizacao;
    
    @Column
    private LocalDateTime dataUtilizacao;
    
    @PreUpdate
    public void preUpdate() {
        this.dataAtualizacao = LocalDateTime.now();
    }
    
    // Método para confirmar utilização
    public void confirmarUtilizacao() {
        if (this.status == StatusReserva.EM_USO || this.status == StatusReserva.AGUARDANDO_CONFIRMACAO) {
            this.status = StatusReserva.UTILIZADO;
            this.dataUtilizacao = LocalDateTime.now();
        }
    }
    
    // Método para cancelar reserva
    public void cancelar() {
        if (this.status == StatusReserva.AGENDADO || this.status == StatusReserva.PENDENTE || this.status == StatusReserva.EM_USO) {
            this.status = StatusReserva.CANCELADO;
        }
    }
    
    // Método para verificar se a reserva pode ser editada
    public boolean podeSerEditada() {
        return this.status == StatusReserva.AGENDADO || this.status == StatusReserva.PENDENTE;
    }
    
    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EspacoAcademico getEspacoAcademico() {
        return espacoAcademico;
    }

    public void setEspacoAcademico(EspacoAcademico espacoAcademico) {
        this.espacoAcademico = espacoAcademico;
    }

    public Professor getProfessor() {
        return professor;
    }

    public void setProfessor(Professor professor) {
        this.professor = professor;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public LocalTime getHoraInicial() {
        return horaInicial;
    }

    public void setHoraInicial(LocalTime horaInicial) {
        this.horaInicial = horaInicial;
    }

    public LocalTime getHoraFinal() {
        return horaFinal;
    }

    public void setHoraFinal(LocalTime horaFinal) {
        this.horaFinal = horaFinal;
    }

    public String getFinalidade() {
        return finalidade;
    }

    public void setFinalidade(String finalidade) {
        this.finalidade = finalidade;
    }

    public StatusReserva getStatus() {
        return status;
    }

    public void setStatus(StatusReserva status) {
        this.status = status;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }

    public void setDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }

    public LocalDateTime getDataUtilizacao() {
        return dataUtilizacao;
    }

    public void setDataUtilizacao(LocalDateTime dataUtilizacao) {
        this.dataUtilizacao = dataUtilizacao;
    }
}