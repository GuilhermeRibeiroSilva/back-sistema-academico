package com.academico.espacos.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.academico.espacos.model.Reserva;
import com.academico.espacos.service.ReservaService;
import java.util.List;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    @Autowired
    private ReservaService service;

    @PostMapping
    public ResponseEntity<Reserva> solicitar(@RequestBody Reserva reserva) {
        return ResponseEntity.ok(service.solicitar(reserva));
    }

    @GetMapping
    public ResponseEntity<List<Reserva>> listarTodas() {
        return ResponseEntity.ok(service.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Reserva> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/confirmar-utilizacao")
    public ResponseEntity<Void> confirmarUtilizacao(@PathVariable Long id) {
        service.confirmarUtilizacao(id);
        return ResponseEntity.ok().build();
    }
}