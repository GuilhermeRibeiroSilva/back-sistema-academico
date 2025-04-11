package com.academico.espacos.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import com.academico.espacos.model.EspacoAcademico;
import com.academico.espacos.service.EspacoAcademicoService;
import com.academico.espacos.repository.ReservaRepository;
import java.util.List;
import com.academico.espacos.exception.ErrorResponse;
import com.academico.espacos.exception.ResourceNotFoundException;

@RestController
@RequestMapping("/api/espacos")
@PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
public class EspacoAcademicoController {

    private final EspacoAcademicoService service;
    private final ReservaRepository reservaRepository;

    public EspacoAcademicoController(EspacoAcademicoService service, ReservaRepository reservaRepository) {
        this.service = service;
        this.reservaRepository = reservaRepository;
    }

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
            ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                e.getMessage()
            );
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PatchMapping("/{id}/indisponivel")
    public ResponseEntity<Void> tornarIndisponivel(@PathVariable Long id) {
        service.tornarIndisponivel(id);
        return ResponseEntity.ok().build();
    }
    
    @PatchMapping("/{id}/disponivel")
    public ResponseEntity<Void> tornarDisponivel(@PathVariable Long id) {
        service.tornarDisponivel(id);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluir(@PathVariable Long id) {
        try {
            service.excluir(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                e.getMessage()
            );
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{id}/tem-reservas-utilizadas")
    public ResponseEntity<Boolean> temReservasUtilizadas(@PathVariable Long id) {
        boolean temReservas = reservaRepository.existsByEspacoAcademicoIdAndStatusUtilizado(id);
        return ResponseEntity.ok(temReservas);
    }
}