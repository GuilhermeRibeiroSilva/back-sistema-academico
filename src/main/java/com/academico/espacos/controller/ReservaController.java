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
import com.academico.espacos.security.JwtTokenProvider;
import com.academico.espacos.model.Usuario;
import org.springframework.security.access.AccessDeniedException;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private static final Logger logger = LoggerFactory.getLogger(ReservaController.class);

    @Autowired
    private ReservaService service;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    public ResponseEntity<?> solicitar(@RequestBody Reserva reserva, @RequestHeader("Authorization") String authHeader) {
        try {
            // Extrair usuário do token
            String token = authHeader.substring(7);
            Usuario usuarioAtual = jwtTokenProvider.getUsuarioFromToken(token);
            
            // Passar o usuário atual para o método solicitar
            Reserva novaReserva = service.solicitar(reserva, usuarioAtual);
            return ResponseEntity.ok(novaReserva);
        } catch (IllegalArgumentException | AccessDeniedException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse(e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(e.getMessage()));
        } catch (ReservaConflitanteException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Erro ao solicitar reserva: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Erro interno do servidor"));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
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
            logger.error("Erro ao atualizar reserva: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Erro interno do servidor"));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    public ResponseEntity<List<Reserva>> listarTodas(@RequestHeader("Authorization") String authHeader) {
        // Extrair usuário do token
        String token = authHeader.substring(7);
        Usuario usuarioAtual = jwtTokenProvider.getUsuarioFromToken(token);
        
        // Filtrar reservas conforme o tipo de usuário
        return ResponseEntity.ok(service.listarReservas(usuarioAtual));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    public ResponseEntity<Reserva> buscarPorId(@PathVariable Long id) {
        return service.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    public ResponseEntity<?> cancelar(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        try {
            // Extrair o token e obter o usuário atual
            String token = authHeader.substring(7);
            Usuario usuarioAtual = jwtTokenProvider.getUsuarioFromToken(token);
            
            // Passar o usuário para o método cancelarReserva
            service.cancelarReserva(id, usuarioAtual);
            return ResponseEntity.ok().build();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(e.getMessage()));
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
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    public ResponseEntity<?> confirmarUtilizacao(@PathVariable Long id, 
                                               @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            Usuario usuarioAtual = jwtTokenProvider.getUsuarioFromToken(token);
            
            service.confirmarUtilizacao(id, usuarioAtual);
            return ResponseEntity.ok().build();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Erro ao confirmar utilização: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Erro interno do servidor"));
        }
    }

    @GetMapping("/{id}/pode-editar")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    public ResponseEntity<Boolean> verificarPodeEditar(@PathVariable Long id) {
        try {
            boolean podeEditar = service.reservaPodeSerEditada(id);
            return ResponseEntity.ok(podeEditar);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Erro ao verificar se reserva pode ser editada: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}