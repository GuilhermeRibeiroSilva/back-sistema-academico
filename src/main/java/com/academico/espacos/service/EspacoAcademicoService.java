package com.academico.espacos.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.academico.espacos.model.EspacoAcademico;
import com.academico.espacos.repository.EspacoAcademicoRepository;
import com.academico.espacos.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;

@Service
public class EspacoAcademicoService {

    @Autowired
    private EspacoAcademicoRepository repository;

    public EspacoAcademico salvar(EspacoAcademico espacoAcademico) {
        if (espacoAcademico.getSigla() == null || espacoAcademico.getSigla().trim().isEmpty()) {
            throw new IllegalArgumentException("A sigla é obrigatória");
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

    public void tornarIndisponivel(Long id) {
        EspacoAcademico espaco = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Espaço Acadêmico não encontrado com id: " + id));
        espaco.setDisponivel(false);
        repository.save(espaco);
    }

    public List<EspacoAcademico> listarDisponiveis() {
        return repository.findByDisponivelTrue();
    }
}