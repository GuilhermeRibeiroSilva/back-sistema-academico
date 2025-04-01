package com.academico.espacos.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academico.espacos.exception.ResourceNotFoundException;
import com.academico.espacos.model.Professor;
import com.academico.espacos.model.Reserva;
import com.academico.espacos.repository.ProfessorRepository;
import com.academico.espacos.repository.ReservaRepository;

@Service
public class ProfessorService {
    
    @Autowired
    private ProfessorRepository repository;
    
    @Autowired
    private ReservaRepository reservaRepository;
    
    public List<Professor> listarTodos() {
        return repository.findAll();
    }
    
    public Optional<Professor> buscarPorId(Long id) {
        return repository.findById(id);
    }
    
    // Adicionar validação de entrada no método salvar
    public Professor salvar(Professor professor) {
        // Validar campos obrigatórios
        if (professor.getNome() == null || professor.getNome().trim().isEmpty()) {
            throw new IllegalArgumentException("O nome do professor é obrigatório");
        }
        if (professor.getEscola() == null || professor.getEscola().trim().isEmpty()) {
            throw new IllegalArgumentException("A escola do professor é obrigatória");
        }
        return repository.save(professor);
    }
    
    public Professor atualizar(Long id, Professor professorAtualizado) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Professor não encontrado com id: " + id);
        }
        professorAtualizado.setId(id);
        return repository.save(professorAtualizado);
    }
    
    @Transactional
    public void excluir(Long id) {
        Professor professor = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado com id: " + id));
        
        // Verificar se há reservas para este professor
        List<Reserva> reservas = reservaRepository.findByProfessorId(id);
        if (!reservas.isEmpty()) {
            throw new IllegalStateException("Não é possível excluir este professor pois existem reservas associadas a ele");
        }
        
        repository.delete(professor);
    }
    
    // Adicionar opção de exclusão forçada
    @Transactional
    public void excluir(Long id, boolean forceDelete) {
        Professor professor = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado com id: " + id));
        
        // Verificar se há reservas para este professor
        List<Reserva> reservas = reservaRepository.findByProfessorId(id);
        if (!reservas.isEmpty() && !forceDelete) {
            throw new IllegalStateException("Não é possível excluir este professor pois existem reservas associadas a ele");
        }
        
        // Se força exclusão, excluir também as reservas
        if (forceDelete && !reservas.isEmpty()) {
            reservaRepository.deleteAll(reservas);
        }
        
        repository.delete(professor);
    }
}