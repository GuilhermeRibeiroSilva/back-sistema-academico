package com.academico.espacos.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.academico.espacos.model.EspacoAcademico;
import com.academico.espacos.repository.EspacoAcademicoRepository;
import com.academico.espacos.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;
import com.academico.espacos.model.Reserva;
import com.academico.espacos.repository.ReservaRepository;
import java.time.LocalDate;
import java.util.stream.Collectors;

@Service
public class EspacoAcademicoService {

    @Autowired
    private EspacoAcademicoRepository repository;
    
    @Autowired
    private ReservaRepository reservaRepository;

    // Adicionar validação para outros campos obrigatórios
    public EspacoAcademico salvar(EspacoAcademico espacoAcademico) {
        if (espacoAcademico.getSigla() == null || espacoAcademico.getSigla().trim().isEmpty()) {
            throw new IllegalArgumentException("A sigla é obrigatória");
        }
        if (espacoAcademico.getNome() == null || espacoAcademico.getNome().trim().isEmpty()) {
            throw new IllegalArgumentException("O nome é obrigatório");
        }
        if (espacoAcademico.getCapacidadeAlunos() == null || espacoAcademico.getCapacidadeAlunos() <= 0) {
            throw new IllegalArgumentException("A capacidade de alunos deve ser um número positivo");
        }
        return repository.save(espacoAcademico);
    }

    public List<EspacoAcademico> listarTodos() {
        return repository.findAll();
    }

    public Optional<EspacoAcademico> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public EspacoAcademico atualizar(Long id, EspacoAcademico espacoAcademico) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Espaço Acadêmico não encontrado com id: " + id);
        }
        espacoAcademico.setId(id);
        return repository.save(espacoAcademico);
    }
    
    @Transactional
    public void excluir(Long id) {
        EspacoAcademico espaco = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Espaço Acadêmico não encontrado com id: " + id));
        
        List<Reserva> reservas = reservaRepository.findByEspacoAcademicoId(id);
        if (!reservas.isEmpty()) {
            throw new IllegalStateException("Não é possível excluir este espaço pois existem reservas associadas a ele");
        }
        
        repository.delete(espaco);
    }

    public void tornarIndisponivel(Long id) {
        EspacoAcademico espaco = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Espaço Acadêmico não encontrado com id: " + id));
        espaco.setDisponivel(false);
        repository.save(espaco);
    }

    public List<EspacoAcademico> listarDisponiveis() {
        return repository.findByDisponivelTrue();
    }

    // Método para verificar disponibilidade real considerando reservas
    public List<EspacoAcademico> listarDisponiveisParaReserva(LocalDate data, String horaInicial, String horaFinal) {
        // Implementação para verificar espaços realmente disponíveis no horário específico
        List<EspacoAcademico> espacosDisponiveis = repository.findByDisponivelTrue();
        
        // Filtrar espaços com reservas no mesmo horário
        return espacosDisponiveis.stream()
            .filter(espaco -> {
                List<Reserva> reservasConflitantes = reservaRepository
                    .findConflictingReservations(espaco.getId(), data, horaInicial, horaFinal);
                return reservasConflitantes.isEmpty();
            })
            .collect(Collectors.toList());
    }
}