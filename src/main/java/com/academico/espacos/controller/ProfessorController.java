package com.academico.espacos.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.academico.espacos.dto.ReservaDTO;
import com.academico.espacos.exception.ErrorResponse;
import com.academico.espacos.exception.ResourceNotFoundException;
import com.academico.espacos.model.Professor;
import com.academico.espacos.service.ProfessorService;
import com.academico.espacos.service.ReservaService;

/**
 * Controller responsável por gerenciar operações relacionadas aos professores
 */
@RestController
@RequestMapping("/api/professores")
public class ProfessorController {

    private final ProfessorService professorService;
    private final ReservaService reservaService;

    @Autowired
    public ProfessorController(ProfessorService professorService, ReservaService reservaService) {
        this.professorService = professorService;
        this.reservaService = reservaService;
    }

    /**
     * Cria um novo professor
     * 
     * @param professor Dados do professor a ser criado
     * @return Professor criado com status 201
     */
    @PostMapping
    public ResponseEntity<Professor> criar(@RequestBody Professor professor) {
        Professor novoProfessor = professorService.salvar(professor);
        return ResponseEntity.status(HttpStatus.CREATED).body(novoProfessor);
    }

    /**
     * Lista todos os professores cadastrados
     * 
     * @return Lista de professores
     */
    @GetMapping
    public ResponseEntity<List<Professor>> listarTodos() {
        return ResponseEntity.ok(professorService.listarTodos());
    }

    /**
     * Busca professor por ID
     * 
     * @param id ID do professor
     * @return Professor encontrado ou 404 se não existir
     */
    @GetMapping("/{id}")
    public ResponseEntity<Professor> buscarPorId(@PathVariable Long id) {
        return professorService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Atualiza os dados de um professor
     * 
     * @param id ID do professor a ser atualizado
     * @param professor Dados atualizados do professor
     * @return Professor atualizado
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody Professor professor) {
        try {
            Professor professorAtualizado = professorService.atualizar(id, professor);
            return ResponseEntity.ok(professorAtualizado);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Erro ao atualizar professor: " + e.getMessage()));
        }
    }
    
    /**
     * Lista todas as reservas de um professor específico
     * 
     * @param id ID do professor
     * @return Lista de reservas do professor
     */
    @GetMapping("/{id}/reservas")
    public ResponseEntity<?> listarReservasDoProfessor(@PathVariable Long id) {
        try {
            professorService.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado com id: " + id));
            
            List<ReservaDTO> reservas = reservaService.buscarPorProfessor(id);
            return ResponseEntity.ok(reservas);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Erro ao listar reservas: " + e.getMessage()));
        }
    }
    
    /**
     * Exclui um professor
     * 
     * @param id ID do professor a ser excluído
     * @param force Flag para forçar exclusão mesmo com reservas vinculadas
     * @return 200 OK se excluído com sucesso
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluir(
            @PathVariable Long id,
            @RequestParam(name = "force", defaultValue = "false") boolean force) {
        try {
            professorService.excluir(id, force);
            return ResponseEntity.ok().build();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Erro ao excluir professor: " + e.getMessage()));
        }
    }
}