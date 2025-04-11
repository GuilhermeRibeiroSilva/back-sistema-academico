package com.academico.espacos.dto;

import com.academico.espacos.model.EspacoAcademico;
import com.academico.espacos.model.Professor;
import com.academico.espacos.model.Reserva;
import com.academico.espacos.model.enums.StatusReserva;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservaDTO {
    private Long id;
    private Long espacoAcademicoId;
    private Long professorId;
    private LocalDate data;
    private String horaInicial;
    private String horaFinal;
    private String finalidade;
    private StatusReserva status;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;
    private LocalDateTime dataUtilizacao;
    
    // Para facilitar a exibição nos clientes
    private String nomeEspaco;
    private String nomeProfessor;
    
    // Objetos completos - opcionais para uso interno
    private EspacoAcademico espacoAcademico;
    private Professor professor;

    // Método para converter entidade para DTO
    public static ReservaDTO fromEntity(Reserva reserva) {
        if (reserva == null) {
            return null;
        }
        
        ReservaDTO dto = new ReservaDTO();
        dto.setId(reserva.getId());
        
        if (reserva.getEspacoAcademico() != null) {
            dto.setEspacoAcademicoId(reserva.getEspacoAcademico().getId());
            dto.setEspacoAcademico(reserva.getEspacoAcademico());
            dto.setNomeEspaco(reserva.getEspacoAcademico().getNome());
        }
        
        if (reserva.getProfessor() != null) {
            dto.setProfessorId(reserva.getProfessor().getId());
            dto.setProfessor(reserva.getProfessor());
            dto.setNomeProfessor(reserva.getProfessor().getNome());
        }
        
        dto.setData(reserva.getData());
        dto.setFinalidade(reserva.getFinalidade());
        dto.setStatus(reserva.getStatus());
        dto.setDataCriacao(reserva.getDataCriacao());
        dto.setDataAtualizacao(reserva.getDataAtualizacao());
        dto.setDataUtilizacao(reserva.getDataUtilizacao());
        
        // Converter LocalTime para String
        if (reserva.getHoraInicial() != null) {
            dto.setHoraInicial(reserva.getHoraInicial().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        
        if (reserva.getHoraFinal() != null) {
            dto.setHoraFinal(reserva.getHoraFinal().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        
        return dto;
    }

    // Método para converter DTO para entidade
    public Reserva toEntity() {
        Reserva reserva = new Reserva();
        reserva.setId(this.id);
        reserva.setData(this.data);
        reserva.setFinalidade(this.finalidade);
        reserva.setStatus(this.status != null ? this.status : StatusReserva.PENDENTE);
        reserva.setDataCriacao(this.dataCriacao);
        reserva.setDataAtualizacao(this.dataAtualizacao);
        reserva.setDataUtilizacao(this.dataUtilizacao);

        // Converter String para LocalTime
        if (horaInicial != null && !horaInicial.isEmpty()) {
            try {
                reserva.setHoraInicial(LocalTime.parse(horaInicial));
            } catch (Exception e) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                reserva.setHoraInicial(LocalTime.parse(horaInicial, formatter));
            }
        }

        if (horaFinal != null && !horaFinal.isEmpty()) {
            try {
                reserva.setHoraFinal(LocalTime.parse(horaFinal));
            } catch (Exception e) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                reserva.setHoraFinal(LocalTime.parse(horaFinal, formatter));
            }
        }

        return reserva;
    }
}