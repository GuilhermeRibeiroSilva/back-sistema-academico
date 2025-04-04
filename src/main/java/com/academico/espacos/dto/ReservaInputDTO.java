package com.academico.espacos.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public class ReservaInputDTO {
    private Long espacoAcademicoId;
    private Long professorId;
    private LocalDate data;
    private LocalTime horaInicial;
    private LocalTime horaFinal;
    private String finalidade;

    // Getters e Setters
    public Long getEspacoAcademicoId() {
        return espacoAcademicoId;
    }

    public void setEspacoAcademicoId(Long espacoAcademicoId) {
        this.espacoAcademicoId = espacoAcademicoId;
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
}