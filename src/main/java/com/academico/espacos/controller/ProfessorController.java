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

import com.academico.espacos.exception.ErrorResponse;
import com.academico.espacos.exception.ResourceNotFoundException;
import com.academico.espacos.model.Professor;
import com.academico.espacos.model.Reserva;
import com.academico.espacos.service.ProfessorService;
import com.academico.espacos.service.ReservaService;

@RestController
@RequestMapping("/api/professores")
public class ProfessorController {

    @Autowired
    private ProfessorService service;
    
    @Autowired
    private ReservaService reservaService;

    @PostMapping
    public ResponseEntity<Professor> criar(@RequestBody Professor professor) {
        Professor novoProfessor = service.salvar(professor);
        return ResponseEntity.status(HttpStatus.CREATED).body(novoProfessor);
    }

    @GetMapping
    public ResponseEntity<List<Professor>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Professor> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Professor> atualizar(@PathVariable Long id, @RequestBody Professor professor) {
        return ResponseEntity.ok(service.atualizar(id, professor));
    }
    
    @GetMapping("/{id}/reservas")
    public ResponseEntity<?> listarReservasDoProfessor(@PathVariable Long id) {
        try {
            // Verifica se o professor existe
            Professor professor = service.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado com id: " + id));
            
            // Busca as reservas do professor
            List<Reserva> reservas = reservaService.buscarReservasPorProfessor(id);
            return ResponseEntity.ok(reservas);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Erro ao listar reservas: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluir(
            @PathVariable Long id,
            @RequestParam(name = "force", defaultValue = "false") boolean force) {
        try {
            // Usar novo método no service que aceita o parâmetro force
            service.excluir(id, force);
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