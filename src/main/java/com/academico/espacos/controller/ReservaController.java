package com.academico.espacos.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.academico.espacos.model.Reserva;
import com.academico.espacos.service.ReservaService;

import java.time.LocalDate;
import java.util.List;
import com.academico.espacos.exception.ResourceNotFoundException;
import com.academico.espacos.controller.AuthController.ErrorResponse;
import com.academico.espacos.exception.ReservaConflitanteException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/reservas")
@PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
public class ReservaController {

    private static final Logger logger = LoggerFactory.getLogger(ReservaController.class);

    @Autowired
    private ReservaService service;

    @PostMapping
    public ResponseEntity<?> solicitar(@RequestBody Reserva reserva) {
        try {
            // Validações básicas
            if (reserva.getData() == null || reserva.getHoraInicial() == null || 
                reserva.getHoraFinal() == null || reserva.getEspacoAcademico() == null) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Todos os campos são obrigatórios"));
            }

            // Validação de data passada
            if (reserva.getData().isBefore(LocalDate.now())) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Não é possível fazer reservas para datas passadas"));
            }

            Reserva novaReserva = service.solicitar(reserva);
            return ResponseEntity.ok(novaReserva);
        } catch (Exception e) {
            // Prática não recomendada para produção
            e.printStackTrace();  // Substituir por logger
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Erro interno do servidor"));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody Reserva reserva) {
        try {
            reserva.setId(id); // Garante que o ID está correto
            Reserva reservaAtualizada = service.atualizar(reserva);
            return ResponseEntity.ok(reservaAtualizada);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (ReservaConflitanteException e) {
            return ResponseEntity.status(409)
                .body(new ErrorResponse(e.getMessage()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            // Usar logger em vez de printStackTrace
            logger.error("Erro ao atualizar reserva: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Erro interno do servidor"));
        }
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
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelarReserva(@PathVariable Long id) {
        try {
            service.cancelarReserva(id);
            return ResponseEntity.ok().build();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Erro ao cancelar reserva: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Erro interno do servidor"));
        }
    }

    @PatchMapping("/{id}/confirmar")
    public ResponseEntity<?> confirmarUtilizacao(@PathVariable Long id) {
        try {
            service.confirmarUtilizacao(id);
            return ResponseEntity.ok().build();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            // Prática não recomendada para produção
            e.printStackTrace();  // Substituir por logger
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Erro interno do servidor"));
        }
    }
}