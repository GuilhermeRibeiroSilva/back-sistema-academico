package com.academico.espacos.controller;

import com.academico.espacos.dto.DisponibilidadeDTO;
import com.academico.espacos.dto.ReservaDTO;
import com.academico.espacos.dto.ReservaInputDTO;
import com.academico.espacos.exception.ErrorResponse;
import com.academico.espacos.model.EspacoAcademico;
import com.academico.espacos.model.Reserva;
import com.academico.espacos.model.Usuario;
import com.academico.espacos.model.enums.Perfil;
import com.academico.espacos.model.enums.StatusReserva;
import com.academico.espacos.repository.EspacoAcademicoRepository;
import com.academico.espacos.service.ReservaService;
import com.academico.espacos.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {
    
    private static final Logger logger = LoggerFactory.getLogger(ReservaController.class);
    
    @Autowired
    private ReservaService reservaService;
    
    @Autowired
    private UsuarioService usuarioService;
    
    @Autowired
    private EspacoAcademicoRepository espacoRepository;
    
    /**
     * Lista todas as reservas baseado no perfil do usuário logado
     */
    @GetMapping
    public ResponseEntity<List<ReservaDTO>> listar() {
        Usuario usuarioLogado = getUsuarioLogado();
        
        if (usuarioLogado.getPerfil() == Perfil.ADMIN) {
            return ResponseEntity.ok(reservaService.buscarTodasAtivas());
        } else {
            return ResponseEntity.ok(reservaService.buscarPorProfessor(usuarioLogado.getProfessor().getId()));
        }
    }
    
    /**
     * Busca uma reserva específica por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReservaDTO> buscarPorId(@PathVariable Long id) {
        Usuario usuarioLogado = getUsuarioLogado();
        ReservaDTO reserva = reservaService.buscarPorId(id);
        
        if (!temPermissaoParaAcessarReserva(usuarioLogado, reserva)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(reserva);
    }
    
    /**
     * Cria uma nova reserva
     */
    @PostMapping
    public ResponseEntity<?> criar(@Valid @RequestBody ReservaInputDTO input) {
        try {
            logger.info("Criando nova reserva: {}", input);
            Usuario usuarioLogado = getUsuarioLogado();
            
            // Se não for admin, só pode fazer reserva para si mesmo
            if (usuarioLogado.getPerfil() != Perfil.ADMIN) {
                input.setProfessorId(usuarioLogado.getProfessor().getId());
            }
            
            // Validar horários
            if (input.getHoraInicial() != null && input.getHoraFinal() != null) {
                LocalTime horaInicial = LocalTime.parse(input.getHoraInicial());
                LocalTime horaFinal = LocalTime.parse(input.getHoraFinal());
                
                if (horaInicial.isAfter(horaFinal)) {
                    logger.error("Horários inválidos: {} > {}", input.getHoraInicial(), input.getHoraFinal());
                    return ResponseEntity.badRequest()
                            .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), 
                                 "A hora inicial não pode ser maior que a hora final"));
                }
            }
            
            ReservaDTO reservaCriada = reservaService.criar(input);
            logger.info("Reserva criada com sucesso: ID = {}", reservaCriada.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(reservaCriada);
        } catch (Exception e) {
            logger.error("Erro ao criar reserva: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), 
                         "Erro ao criar reserva: " + e.getMessage()));
        }
    }
    
    /**
     * Atualiza uma reserva existente (apenas admin)
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody ReservaInputDTO input) {
        try {
            Usuario usuarioLogado = getUsuarioLogado();
            
            // Apenas admins podem editar reservas
            if (usuarioLogado.getPerfil() != Perfil.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            ReservaDTO reservaAtualizada = reservaService.atualizar(id, input);
            return ResponseEntity.ok(reservaAtualizada);
        } catch (Exception e) {
            logger.error("Erro ao atualizar reserva {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), 
                         "Erro ao atualizar reserva: " + e.getMessage()));
        }
    }
    
    /**
     * Cancela uma reserva
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelar(@PathVariable Long id) {
        try {
            Usuario usuarioLogado = getUsuarioLogado();
            
            // Apenas admin pode cancelar reservas
            if (usuarioLogado.getPerfil() != Perfil.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            reservaService.cancelar(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.error("Erro ao cancelar reserva {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), 
                         "Erro ao cancelar reserva: " + e.getMessage()));
        }
    }
    
    /**
     * Confirma a utilização de uma reserva
     */
    @PostMapping("/{id}/confirmar")
    public ResponseEntity<?> confirmarUtilizacao(@PathVariable Long id) {
        try {
            Usuario usuarioLogado = getUsuarioLogado();
            ReservaDTO reserva = reservaService.buscarPorId(id);
            
            if (!temPermissaoParaAcessarReserva(usuarioLogado, reserva)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            reservaService.confirmarUtilizacao(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Erro ao confirmar utilização da reserva {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), 
                         "Erro ao confirmar utilização: " + e.getMessage()));
        }
    }
    
    /**
     * Verifica disponibilidade para reserva, considerando também o status do espaço
     */
    @PostMapping("/verificar-disponibilidade")
    public ResponseEntity<?> verificarDisponibilidade(@RequestBody DisponibilidadeDTO dto) {
        try {
            // Primeiro verificar se o espaço está marcado como disponível
            Optional<EspacoAcademico> espacoOpt = espacoRepository.findById(dto.getEspacoId());
            
            if (espacoOpt.isEmpty() || !espacoOpt.get().isDisponivel()) {
                return ResponseEntity.ok(false); // Espaço não existe ou está indisponível
            }
            
            boolean disponivel = reservaService.verificarDisponibilidade(
                dto.getData(), 
                dto.getHoraInicial(), 
                dto.getHoraFinal(), 
                dto.getEspacoId(),
                dto.getReservaId()
            );
            
            return ResponseEntity.ok(disponivel);
        } catch (Exception e) {
            logger.error("Erro ao verificar disponibilidade: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), 
                         "Erro ao verificar disponibilidade: " + e.getMessage()));
        }
    }
    
    /**
     * Busca reservas por espaço e data - incluindo apenas reservas não canceladas
     */
    @GetMapping("/buscar-por-espaco-data")
    public ResponseEntity<?> buscarPorEspacoEData(
            @RequestParam Long espacoId,
            @RequestParam String data) {
        try {
            LocalDate localDate = LocalDate.parse(data);
            List<Reserva> reservas = reservaService.buscarPorEspacoEData(espacoId, localDate);
            
            // Filtrar apenas reservas não canceladas
            List<Reserva> reservasAtivas = reservas.stream()
                .filter(r -> r.getStatus() != StatusReserva.CANCELADO)
                .collect(Collectors.toList());
                
            return ResponseEntity.ok(reservasAtivas.stream()
                .map(reservaService::toReservaDTO)
                .collect(Collectors.toList()));
        } catch (Exception e) {
            logger.error("Erro ao buscar reservas por espaço e data: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), 
                         "Erro ao buscar reservas: " + e.getMessage()));
        }
    }
    
    /**
     * Obtém o usuário logado a partir do token de autenticação
     */
    private Usuario getUsuarioLogado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return usuarioService.buscarPorUsername(authentication.getName());
    }
    
    /**
     * Verifica se o usuário tem permissão para acessar uma reserva específica
     */
    private boolean temPermissaoParaAcessarReserva(Usuario usuario, ReservaDTO reserva) {
        return usuario.getPerfil() == Perfil.ADMIN || 
               reserva.getProfessor().getId().equals(usuario.getProfessor().getId());
    }
}