package com.academico.espacos.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "reservas")
public class Reserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(nullable = false)
    private EspacoAcademico espacoAcademico;
    
    @ManyToOne
    @JoinColumn(nullable = false)
    private Professor professor;
    
    @Column(nullable = false)
    private LocalDate data;
    
    @Column(nullable = false)
    private LocalTime horaInicial;
    
    @Column(nullable = false)
    private LocalTime horaFinal;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusReserva status = StatusReserva.PENDENTE;
    
    private boolean utilizado = false;

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

    public boolean isUtilizado() {
        return utilizado;
    }

    public void setUtilizado(boolean utilizado) {
        this.utilizado = utilizado;
    }
    
    public StatusReserva getStatus() {
        return status;
    }
    
    public void setStatus(StatusReserva status) {
        this.status = status;
    }
    
    public enum StatusReserva {
        PENDENTE,
        CONFIRMADA,
        EM_USO,    
        UTILIZADO,
        CANCELADA
    }
}