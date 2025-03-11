package com.academico.espacos.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.academico.espacos.model.Reserva;
import com.academico.espacos.model.EspacoAcademico;
import com.academico.espacos.repository.ReservaRepository;
import com.academico.espacos.repository.EspacoAcademicoRepository;
import com.academico.espacos.exception.ResourceNotFoundException;
import com.academico.espacos.exception.ReservaConflitanteException;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Service
public class ReservaService {

    @Autowired
    private ReservaRepository repository;

    @Autowired
    private EspacoAcademicoRepository espacoRepository;

    public Reserva solicitar(Reserva reserva) {
        // Validar se o espaço existe e está disponível
        EspacoAcademico espaco = espacoRepository.findById(reserva.getEspacoAcademico().getId())
            .orElseThrow(() -> new ResourceNotFoundException("Espaço Acadêmico não encontrado"));

        if (!espaco.isDisponivel()) {
            throw new IllegalStateException("Este espaço acadêmico não está disponível para reservas");
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

    public void confirmarUtilizacao(Long id) {
        Reserva reserva = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada com id: " + id));
        reserva.setUtilizado(true);
        repository.save(reserva);
    }

    public List<Reserva> listarTodas() {
        return repository.findAll();
    }

    public Optional<Reserva> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public void cancelarReserva(Long id) {
        Reserva reserva = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada com id: " + id));
        repository.delete(reserva);
    }
}