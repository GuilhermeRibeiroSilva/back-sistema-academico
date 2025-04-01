package com.academico.espacos.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import com.academico.espacos.model.EspacoAcademico;
import com.academico.espacos.service.EspacoAcademicoService;
import java.util.List;
import com.academico.espacos.exception.ErrorResponse;
import com.academico.espacos.exception.ResourceNotFoundException;

@RestController
@RequestMapping("/api/espacos")
@PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
public class EspacoAcademicoController {

    @Autowired
    private EspacoAcademicoService service;

    @PostMapping
    public ResponseEntity<EspacoAcademico> criar(@RequestBody EspacoAcademico espacoAcademico) {
        return ResponseEntity.ok(service.salvar(espacoAcademico));
    }

    @GetMapping
    public ResponseEntity<List<EspacoAcademico>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/disponiveis")
    public ResponseEntity<List<EspacoAcademico>> listarDisponiveis() {
        return ResponseEntity.ok(service.listarDisponiveis());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EspacoAcademico> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody EspacoAcademico espacoAcademico) {
        try {
            return ResponseEntity.ok(service.atualizar(id, espacoAcademico));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/indisponivel")
    public ResponseEntity<Void> tornarIndisponivel(@PathVariable Long id) {
        service.tornarIndisponivel(id);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluir(@PathVariable Long id) {
        try {
            service.excluir(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }
}