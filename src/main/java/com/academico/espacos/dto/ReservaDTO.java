package com.academico.espacos.dto;

import com.academico.espacos.model.EspacoAcademico;
import com.academico.espacos.model.Professor;
import com.academico.espacos.model.enums.StatusReserva;
import com.academico.espacos.model.Reserva;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class ReservaDTO {
    private Long id;
    private EspacoAcademico espacoAcademico;
    private Long espacoAcademicoId;
    private Professor professor;
    private Long professorId;
    private LocalDate data;
    private String horaInicial; // String para receber formato HH:mm:ss
    private String horaFinal;   // String para receber formato HH:mm:ss
    private String finalidade;
    private StatusReserva status;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;
    private LocalDateTime dataUtilizacao;

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

    public Long getEspacoAcademicoId() {
        return espacoAcademicoId;
    }

    public void setEspacoAcademicoId(Long espacoAcademicoId) {
        this.espacoAcademicoId = espacoAcademicoId;
    }

    public Professor getProfessor() {
        return professor;
    }

    public void setProfessor(Professor professor) {
        this.professor = professor;
    }

    public Long getProfessorId() {
        return professorId;
    }

    public void setProfessorId(Long professorId) {
        this.professorId = professorId;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public String getHoraInicial() {
        return horaInicial;
    }

    public void setHoraInicial(String horaInicial) {
        this.horaInicial = horaInicial;
    }

    public String getHoraFinal() {
        return horaFinal;
    }

    public void setHoraFinal(String horaFinal) {
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

    // Método para converter DTO para entidade
    public Reserva toEntity() {
        Reserva reserva = new Reserva();
        reserva.setId(this.id);
        reserva.setEspacoAcademico(this.espacoAcademico);
        reserva.setProfessor(this.professor);
        reserva.setData(this.data);
        reserva.setFinalidade(this.finalidade);
        reserva.setStatus(this.status);
        reserva.setDataCriacao(this.dataCriacao);
        reserva.setDataAtualizacao(this.dataAtualizacao);
        reserva.setDataUtilizacao(this.dataUtilizacao);

        // Converter String para LocalTime
        if (horaInicial != null) {
            reserva.setHoraInicial(LocalTime.parse(horaInicial));
        }

        if (horaFinal != null) {
            reserva.setHoraFinal(LocalTime.parse(horaFinal));
        }

        return reserva;
    }

    // Método para converter entidade para DTO
    public static ReservaDTO fromEntity(Reserva reserva) {
        ReservaDTO dto = new ReservaDTO();
        dto.setId(reserva.getId());
        dto.setEspacoAcademico(reserva.getEspacoAcademico());
        dto.setProfessor(reserva.getProfessor());
        dto.setData(reserva.getData());
        dto.setFinalidade(reserva.getFinalidade());
        dto.setStatus(reserva.getStatus());
        dto.setDataCriacao(reserva.getDataCriacao());
        dto.setDataAtualizacao(reserva.getDataAtualizacao());
        dto.setDataUtilizacao(reserva.getDataUtilizacao());

        // Converter LocalTime para String
        if (reserva.getHoraInicial() != null) {
            dto.setHoraInicial(reserva.getHoraInicial().toString());
        }

        if (reserva.getHoraFinal() != null) {
            dto.setHoraFinal(reserva.getHoraFinal().toString());
        }

        return dto;
    }
}