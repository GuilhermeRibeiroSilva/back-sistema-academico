package com.academico.espacos.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.academico.espacos.model.Reserva;
import com.academico.espacos.service.ReservaService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.academico.espacos.exception.ResourceNotFoundException;
import com.academico.espacos.controller.AuthController.ErrorResponse;
import com.academico.espacos.exception.ReservaConflitanteException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.academico.espacos.security.JwtTokenProvider;
import com.academico.espacos.model.Usuario;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.format.annotation.DateTimeFormat;

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
    public ResponseEntity<?> criarReserva(@RequestBody Reserva reserva, @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            Usuario usuarioAtual = jwtTokenProvider.getUsuarioFromToken(token);
            
            // Adicionar validação de data atual para garantir
            LocalDateTime agora = LocalDateTime.now();
            if (reserva.getData() == null) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("A data da reserva é obrigatória"));
            }
            
            LocalDateTime dataHoraReserva = LocalDateTime.of(reserva.getData(), reserva.getHoraInicial());
            if (dataHoraReserva.isBefore(agora)) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Não é possível criar reservas para datas/horários passados"));
            }
            
            Reserva novaReserva = service.solicitar(reserva, usuarioAtual);
            return ResponseEntity.status(HttpStatus.CREATED).body(novaReserva);
        } catch (Exception e) {
            logger.error("Erro ao criar reserva: ", e);
            
            if (e instanceof ReservaConflitanteException) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse(e.getMessage()));
            } else if (e instanceof IllegalArgumentException || e instanceof AccessDeniedException) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage()));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Erro ao criar reserva: " + e.getMessage()));
            }
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> atualizarReserva(@PathVariable Long id, 
                                        @RequestBody Reserva reserva,
                                        @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            Usuario usuarioAtual = jwtTokenProvider.getUsuarioFromToken(token);
            
            // Verificar se o usuário é admin
            if (!usuarioAtual.isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse("Apenas administradores podem alterar reservas"));
            }
            
            // Garantir que o ID corresponda
            reserva.setId(id);
            
            // Verificar se a reserva pode ser alterada
            if (!service.reservaPodeSerAlterada(id, usuarioAtual)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Esta reserva não pode mais ser alterada"));
            }
            
            Reserva reservaAtualizada = service.atualizar(reserva);
            return ResponseEntity.ok(reservaAtualizada);
        } catch (Exception e) {
            logger.error("Erro ao atualizar reserva: ", e);
            
            if (e instanceof ReservaConflitanteException) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse(e.getMessage()));
            } else if (e instanceof ResourceNotFoundException) {
                return ResponseEntity.notFound().build();
            } else if (e instanceof IllegalArgumentException || e instanceof AccessDeniedException) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse(e.getMessage()));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Erro ao atualizar reserva: " + e.getMessage()));
            }
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

    @GetMapping("/horarios-disponiveis")
    public ResponseEntity<Map<String, Boolean>> getHorariosDisponiveis(
            @RequestParam("espacoId") Long espacoId,
            @RequestParam("data") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(value = "professorId", required = false) Long professorId) {
        
        try {
            Map<String, Boolean> horariosDisponiveis = service.verificarHorariosDisponiveis(
                espacoId, data, professorId);
            return ResponseEntity.ok(horariosDisponiveis);
        } catch (Exception e) {
            logger.error("Erro ao verificar horários disponíveis: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/pode-alterar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Boolean> verificarPodeAlterar(@PathVariable Long id, 
                                                     @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            Usuario usuarioAtual = jwtTokenProvider.getUsuarioFromToken(token);
            
            boolean podeAlterar = service.reservaPodeSerAlterada(id, usuarioAtual);
            return ResponseEntity.ok(podeAlterar);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Erro ao verificar se reserva pode ser alterada: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{id}/pode-cancelar")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    public ResponseEntity<Boolean> verificarPodeCancelar(@PathVariable Long id, 
                                                      @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.substring(7);
            Usuario usuarioAtual = jwtTokenProvider.getUsuarioFromToken(token);
            
            boolean podeCancelar = service.reservaPodeSerCancelada(id, usuarioAtual);
            return ResponseEntity.ok(podeCancelar);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Erro ao verificar se reserva pode ser cancelada: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{id}/pode-confirmar")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    public ResponseEntity<Boolean> verificarPodeConfirmar(@PathVariable Long id) {
        try {
            Reserva reserva = service.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada"));
                
            boolean podeConfirmar = reserva.getStatus().podeConfirmarUtilizacao();
            return ResponseEntity.ok(podeConfirmar);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Erro ao verificar se reserva pode ser confirmada: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}