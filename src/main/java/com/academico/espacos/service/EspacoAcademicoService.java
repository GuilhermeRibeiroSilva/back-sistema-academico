package com.academico.espacos.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academico.espacos.exception.BusinessException;
import com.academico.espacos.exception.ResourceNotFoundException;
import com.academico.espacos.model.EspacoAcademico;
import com.academico.espacos.model.Reserva;
import com.academico.espacos.repository.EspacoAcademicoRepository;
import com.academico.espacos.repository.ReservaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class EspacoAcademicoService {

    private final EspacoAcademicoRepository repository;
    private final ReservaRepository reservaRepository;

    // Construtor para injeção de dependências (melhor prática do que @Autowired em campos)
    public EspacoAcademicoService(EspacoAcademicoRepository repository, ReservaRepository reservaRepository) {
        this.repository = repository;
        this.reservaRepository = reservaRepository;
    }

    @Transactional
    public EspacoAcademico salvar(EspacoAcademico espacoAcademico) {
        validarEspaco(espacoAcademico);
        return repository.save(espacoAcademico);
    }

    public List<EspacoAcademico> listarTodos() {
        return repository.findAll();
    }

    public Optional<EspacoAcademico> buscarPorId(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public EspacoAcademico atualizar(Long id, EspacoAcademico espacoAcademico) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Espaço Acadêmico não encontrado com id: " + id);
        }
        validarEspaco(espacoAcademico);
        espacoAcademico.setId(id);
        return repository.save(espacoAcademico);
    }
    
    @Transactional
    public void excluir(Long id) {
        excluir(id, false);
    }

    @Transactional
    public void excluir(Long id, boolean force) {
        EspacoAcademico espaco = buscarEspacoOuFalhar(id);
        
        // Verificar se existem reservas associadas
        if (!force) {
            List<Reserva> reservas = reservaRepository.findByEspacoAcademicoId(id);
            if (!reservas.isEmpty()) {
                throw new BusinessException("Não é possível excluir este espaço pois existem reservas associadas a ele");
            }
            
            // Verificar se existem reservas utilizadas para este espaço
            if (reservaRepository.existsByEspacoAcademicoIdAndStatusUtilizado(id)) {
                throw new BusinessException("Não é possível excluir um espaço com reservas utilizadas. Use force=true para forçar exclusão.");
            }
        }
        
        repository.delete(espaco);
    }

    @Transactional
    public void tornarDisponivel(Long id) {
        EspacoAcademico espaco = buscarEspacoOuFalhar(id);
        espaco.setDisponivel(true);
        repository.save(espaco);
    }

    @Transactional
    public void tornarIndisponivel(Long id) {
        EspacoAcademico espaco = buscarEspacoOuFalhar(id);
        espaco.setDisponivel(false);
        repository.save(espaco);
    }

    public List<EspacoAcademico> listarDisponiveis() {
        return repository.findByDisponivelTrue();
    }

    public List<EspacoAcademico> listarDisponiveisParaReserva(LocalDate data, String horaInicial, String horaFinal) {
        return repository.findByDisponivelTrue().stream()
            .filter(espaco -> {
                List<Reserva> reservasConflitantes = reservaRepository
                    .findConflictingReservations(espaco.getId(), data, horaInicial, horaFinal);
                return reservasConflitantes.isEmpty();
            })
            .toList();
    }

    public boolean espacoTemReservaUtilizada(Long id) {
        return reservaRepository.existsByIdAndStatusUtilizado(id);
    }
    
    // Métodos auxiliares privados
    
    private void validarEspaco(EspacoAcademico espacoAcademico) {
        if (espacoAcademico.getSigla() == null || espacoAcademico.getSigla().trim().isEmpty()) {
            throw new IllegalArgumentException("A sigla é obrigatória");
        }
        if (espacoAcademico.getNome() == null || espacoAcademico.getNome().trim().isEmpty()) {
            throw new IllegalArgumentException("O nome é obrigatório");
        }
        if (espacoAcademico.getCapacidadeAlunos() == null || espacoAcademico.getCapacidadeAlunos() <= 0) {
            throw new IllegalArgumentException("A capacidade de alunos deve ser um número positivo");
        }
    }
    
    private EspacoAcademico buscarEspacoOuFalhar(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Espaço Acadêmico não encontrado com id: " + id));
    }
}