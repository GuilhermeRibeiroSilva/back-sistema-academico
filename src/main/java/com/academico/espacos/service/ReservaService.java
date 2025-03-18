package com.academico.espacos.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.academico.espacos.model.Reserva;
import com.academico.espacos.model.Reserva.StatusReserva;
import com.academico.espacos.model.EspacoAcademico;
import com.academico.espacos.repository.ReservaRepository;
import com.academico.espacos.repository.EspacoAcademicoRepository;
import com.academico.espacos.exception.ResourceNotFoundException;
import com.academico.espacos.exception.ReservaConflitanteException;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class ReservaService {

    @Autowired
    private ReservaRepository repository;

    @Autowired
    private EspacoAcademicoRepository espacoRepository;

    public Reserva solicitar(Reserva reserva) {
        EspacoAcademico espaco = espacoRepository.findById(reserva.getEspacoAcademico().getId())
            .orElseThrow(() -> new ResourceNotFoundException("Espaço Acadêmico não encontrado"));

        if (!espaco.isDisponivel()) {
            throw new IllegalStateException("Este espaço acadêmico não está disponível para reservas");
        }
        
        if (reserva.getEspacoAcademico() == null || reserva.getEspacoAcademico().getId() == null) {
            throw new IllegalArgumentException("Espaço Acadêmico é obrigatório");
        }
        
        if (reserva.getProfessor() == null || reserva.getProfessor().getId() == null) {
            throw new IllegalArgumentException("Professor é obrigatório");
        }
        
        if (reserva.getData() == null) {
            throw new IllegalArgumentException("Data é obrigatória");
        }
        
        if (reserva.getHoraInicial() == null || reserva.getHoraFinal() == null) {
            throw new IllegalArgumentException("Horários são obrigatórios");
        }

        // Validar se a data não é passada
        if (reserva.getData().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Não é possível fazer reservas para datas passadas");
        }

        // Validar horários
        if (reserva.getHoraFinal().isBefore(reserva.getHoraInicial())) {
            throw new IllegalArgumentException("Hora final não pode ser anterior à hora inicial");
        }

        // Verificar conflitos
        List<Reserva> reservasConflitantes = repository.findReservasConflitantes(
            reserva.getEspacoAcademico().getId(),
            reserva.getData(),
            reserva.getHoraInicial(),
            reserva.getHoraFinal()
        );

        if (!reservasConflitantes.isEmpty()) {
            throw new ReservaConflitanteException("Já existe uma reserva para este horário");
        }

        return repository.save(reserva);
    }
    
    public Reserva atualizar(Reserva reserva) {
        // Verificar se a reserva existe
        Reserva reservaExistente = repository.findById(reserva.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada"));

        // Não permitir atualizar reservas já utilizadas
        if (reservaExistente.isUtilizado()) {
            throw new IllegalStateException("Não é possível alterar uma reserva já utilizada");
        }

        // Validar se a data não é passada
        if (reserva.getData().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Não é possível atualizar para datas passadas");
        }

        // Validar horários
        if (reserva.getHoraFinal().isBefore(reserva.getHoraInicial())) {
            throw new IllegalArgumentException("Hora final não pode ser anterior à hora inicial");
        }

        // Verificar conflitos (excluindo a própria reserva)
        List<Reserva> reservasConflitantes = repository.findReservasConflitantes(
            reserva.getEspacoAcademico().getId(),
            reserva.getData(),
            reserva.getHoraInicial(),
            reserva.getHoraFinal()
        ).stream()
        .filter(r -> !r.getId().equals(reserva.getId()))
        .toList();

        if (!reservasConflitantes.isEmpty()) {
            throw new ReservaConflitanteException("Já existe uma reserva para este horário");
        }

        // Validar se o espaço existe e está disponível
        EspacoAcademico espaco = espacoRepository.findById(reserva.getEspacoAcademico().getId())
            .orElseThrow(() -> new ResourceNotFoundException("Espaço Acadêmico não encontrado"));

        if (!espaco.isDisponivel()) {
            throw new IllegalStateException("Este espaço acadêmico não está disponível para reservas");
        }

        // Manter alguns dados originais que não devem ser alterados
        reserva.setUtilizado(reservaExistente.isUtilizado());

        return repository.save(reserva);
    }
    
    private void validarDadosReserva(Reserva reserva) {
        if (reserva.getEspacoAcademico() == null || reserva.getEspacoAcademico().getId() == null) {
            throw new IllegalArgumentException("Espaço Acadêmico é obrigatório");
        }
        
        if (reserva.getProfessor() == null || reserva.getProfessor().getId() == null) {
            throw new IllegalArgumentException("Professor é obrigatório");
        }
        
        if (reserva.getData() == null) {
            throw new IllegalArgumentException("Data é obrigatória");
        }
        
        if (reserva.getHoraInicial() == null || reserva.getHoraFinal() == null) {
            throw new IllegalArgumentException("Horários são obrigatórios");
        }
    }

    public void confirmarUtilizacao(Long id) {
        Reserva reserva = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada com id: " + id));
        reserva.setUtilizado(true);
        repository.save(reserva);
    }

    public List<Reserva> listarTodas() {
        // Buscar reservas já ordenadas
        List<Reserva> reservas = repository.findAllOrderByDataAndHoraInicial();
        
        // Atualiza status de reservas encerradas (2 horas após o término)
        LocalDateTime agora = LocalDateTime.now();
        
        for (Reserva reserva : reservas) {
            // Pular reservas canceladas
            if (reserva.getStatus() == StatusReserva.CANCELADO) {
                continue;
            }
            
            // Combina data e hora final
            LocalDateTime dataHoraFinal = LocalDateTime.of(
                reserva.getData(), 
                reserva.getHoraFinal()
            );
            
            // Se passou 2 horas do fim e não foi marcada como utilizada
            if (dataHoraFinal.plusHours(2).isBefore(agora) && !reserva.isUtilizado()) {
                reserva.setUtilizado(true);
                reserva.setStatus(StatusReserva.UTILIZADO);
                repository.save(reserva);
            }
        }
        
        return reservas;
    }

    public Optional<Reserva> buscarPorId(Long id) {
        return repository.findById(id);
    }
    
    public List<Reserva> buscarReservasPorProfessor(Long professorId) {
        return repository.findByProfessorId(professorId);
    }

    public void cancelarReserva(Long id) {
        Reserva reserva = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada com id: " + id));
        
        // Verificar se pode cancelar (até 1h antes)
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime dataHoraInicial = LocalDateTime.of(reserva.getData(), reserva.getHoraInicial());
        
        if (dataHoraInicial.minusHours(1).isBefore(agora)) {
            throw new IllegalStateException("Não é possível cancelar reservas com menos de 1 hora de antecedência");
        }
        
        // Marcar como cancelada em vez de excluir
        reserva.setStatus(StatusReserva.CANCELADO);
        repository.save(reserva);
    }
    
    private void validarConflitos(Reserva reserva) {
        // Verificar conflitos para o mesmo professor
        List<Reserva> reservasProfessor = repository.findByProfessorIdAndData(
            reserva.getProfessor().getId(), 
            reserva.getData()
        );
        
        for (Reserva r : reservasProfessor) {
            // Ignorar a própria reserva (no caso de edição)
            if (reserva.getId() != null && r.getId().equals(reserva.getId())) {
                continue;
            }
            
            // Ignorar reservas canceladas
            if (r.getStatus() == StatusReserva.CANCELADO) {
                continue;
            }
            
            // Verificar sobreposição de horários
            if ((r.getHoraInicial().isBefore(reserva.getHoraFinal()) || 
                 r.getHoraInicial().equals(reserva.getHoraFinal())) && 
                (r.getHoraFinal().isAfter(reserva.getHoraInicial()) || 
                 r.getHoraFinal().equals(reserva.getHoraInicial()))) {
                throw new ReservaConflitanteException(
                    "Professor já possui reserva neste horário"
                );
            }
        }
    }
}