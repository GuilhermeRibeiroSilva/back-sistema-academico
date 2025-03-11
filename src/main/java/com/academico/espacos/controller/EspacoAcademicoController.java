package com.academico.espacos.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.academico.espacos.model.EspacoAcademico;
import com.academico.espacos.service.EspacoAcademicoService;
import java.util.List;

@RestController
@RequestMapping("/api/espacos")
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

    @GetMapping("/{id}")
    public ResponseEntity<EspacoAcademico> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<EspacoAcademico> atualizar(@PathVariable Long id, @RequestBody EspacoAcademico espacoAcademico) {
        return ResponseEntity.ok(service.atualizar(id, espacoAcademico));
    }

    @PatchMapping("/{id}/indisponivel")
    public ResponseEntity<Void> tornarIndisponivel(@PathVariable Long id) {
        service.tornarIndisponivel(id);
        return ResponseEntity.ok().build();
    }
}