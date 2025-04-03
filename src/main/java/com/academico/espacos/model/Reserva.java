package com.academico.espacos.model;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.academico.espacos.model.enums.StatusReserva;

@Entity
@Table(name = "reservas")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "espaco_id", nullable = false)
    private EspacoAcademico espacoAcademico;

    @ManyToOne
    @JoinColumn(name = "professor_id", nullable = false)
    private Professor professor;

    @Column(nullable = false)
    private LocalDate data;

    @Column(name = "hora_inicial", nullable = false)
    private LocalTime horaInicial;

    @Column(name = "hora_final", nullable = false)
    private LocalTime horaFinal;

    @Column(length = 255)
    private String finalidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusReserva status = StatusReserva.PENDENTE;
    
    @Column(name = "utilizado")
    private boolean utilizado = false;

    public boolean isUtilizado() {
        return this.status == StatusReserva.UTILIZADO || this.utilizado;
    }

    public void setUtilizado(boolean utilizado) {
        this.utilizado = utilizado;
        if (utilizado) {
            this.status = StatusReserva.UTILIZADO;
        }
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

    @Override
    public String toString() {
        return "Reserva [id=" + id + ", espacoAcademico=" + espacoAcademico + ", professor=" + professor + ", data="
                + data + ", horaInicial=" + horaInicial + ", horaFinal=" + horaFinal + ", finalidade=" + finalidade
                + ", status=" + status + ", utilizado=" + utilizado + "]";
    }
}