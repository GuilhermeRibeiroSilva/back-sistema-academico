package com.academico.espacos.controller;

import com.academico.espacos.dto.DisponibilidadeDTO;
import com.academico.espacos.dto.ReservaDTO;
import com.academico.espacos.dto.ReservaInputDTO;
import com.academico.espacos.exception.BusinessException;
import com.academico.espacos.exception.ResourceNotFoundException;
import com.academico.espacos.exception.ErrorResponse;
import com.academico.espacos.model.Reserva;
import com.academico.espacos.model.Usuario;
import com.academico.espacos.model.enums.Perfil;
import com.academico.espacos.model.enums.StatusReserva;
import com.academico.espacos.service.ReservaService;
import com.academico.espacos.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {
    
    @Autowired
    private ReservaService reservaService;
    
    @Autowired
    private UsuarioService usuarioService;
    
    private static final Logger logger = LoggerFactory.getLogger(ReservaController.class);
    
    /**
     * Lista todas as reservas
     * Para admin: todas as reservas
     * Para professor: apenas suas reservas
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
        
        // Professor só pode ver suas próprias reservas
        if (usuarioLogado.getPerfil() != Perfil.ADMIN && 
            !reserva.getProfessor().getId().equals(usuarioLogado.getProfessor().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(reserva);
    }
    
    /**
     * Cria uma nova reserva
     */
    @PostMapping
    public ResponseEntity<ReservaDTO> criar(@RequestBody ReservaInputDTO input) {
        Usuario usuarioLogado = getUsuarioLogado();
        
        // Se não for admin, só pode fazer reserva para si mesmo
        if (usuarioLogado.getPerfil() != Perfil.ADMIN) {
            input.setProfessorId(usuarioLogado.getProfessor().getId());
        }
        
        ReservaDTO reservaCriada = reservaService.criar(input);
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaCriada);
    }
    
    /**
     * Atualiza uma reserva existente (apenas admin)
     */
    @PutMapping("/{id}")
    public ResponseEntity<ReservaDTO> atualizar(@PathVariable Long id, @RequestBody ReservaInputDTO input) {
        Usuario usuarioLogado = getUsuarioLogado();
        
        // Apenas admins podem editar reservas
        if (usuarioLogado.getPerfil() != Perfil.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(reservaService.atualizar(id, input));
    }
    
    /**
     * Cancela uma reserva
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        Usuario usuarioLogado = getUsuarioLogado();
        ReservaDTO reserva = reservaService.buscarPorId(id);
        
        // Apenas admin pode cancelar reservas
        if (usuarioLogado.getPerfil() != Perfil.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        reservaService.cancelar(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Confirma a utilização de uma reserva
     */
    @PostMapping("/{id}/confirmar")
    public ResponseEntity<Void> confirmarUtilizacao(@PathVariable Long id) {
        Usuario usuarioLogado = getUsuarioLogado();
        ReservaDTO reserva = reservaService.buscarPorId(id);
        
        // Professor só pode confirmar suas próprias reservas
        if (usuarioLogado.getPerfil() != Perfil.ADMIN && 
            !reserva.getProfessor().getId().equals(usuarioLogado.getProfessor().getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        reservaService.confirmarUtilizacao(id);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Verifica disponibilidade para reserva
     */
    @PostMapping("/verificar-disponibilidade")
    public ResponseEntity<Boolean> verificarDisponibilidade(@RequestBody DisponibilidadeDTO dto) {
        boolean disponivel = reservaService.verificarDisponibilidade(
            dto.getData(), 
            dto.getHoraInicial(), 
            dto.getHoraFinal(), 
            dto.getEspacoId(),
            dto.getReservaId()
        );
        
        return ResponseEntity.ok(disponivel);
    }
    
    /**
     * Busca reservas por espaço e data - modificado para incluir apenas reservas não canceladas
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
                
            return ResponseEntity.ok(reservasAtivas.stream().map(this::toReservaDTO).collect(Collectors.toList()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), 
                         "Erro ao buscar reservas: " + e.getMessage()));
        }
    }
    
    /**
     * Cria uma nova reserva com validação de horários
     */
    @PostMapping
    public ResponseEntity<?> criarReserva(@Valid @RequestBody ReservaDTO reservaDTO) {
        try {
            logger.info("Tentando criar reserva: {}", reservaDTO);
            
            // Validar formato e ordem dos horários
            if (reservaDTO.getHoraInicial() != null && reservaDTO.getHoraFinal() != null) {
                LocalTime horaInicial = LocalTime.parse(reservaDTO.getHoraInicial());
                LocalTime horaFinal = LocalTime.parse(reservaDTO.getHoraFinal());
                
                if (horaInicial.isAfter(horaFinal)) {
                    logger.error("Tentativa de criar reserva com horários invertidos: {} > {}", 
                        reservaDTO.getHoraInicial(), reservaDTO.getHoraFinal());
                    return ResponseEntity.badRequest().body(
                        new ErrorResponse(HttpStatus.BAD_REQUEST.value(), 
                        "A hora inicial não pode ser maior que a hora final"));
                }
            }
            
            // Chamar o serviço para criar a reserva
            Reserva reserva = reservaService.criarReserva(reservaDTO);
            logger.info("Reserva criada com sucesso: ID = {}", reserva.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(toReservaDTO(reserva));
        } catch (Exception e) {
            logger.error("Erro ao criar reserva: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), 
                         "Erro ao criar reserva: " + e.getMessage()));
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
     * Método auxiliar para converter Reserva para ReservaDTO
     */
    private ReservaDTO toReservaDTO(Reserva reserva) {
        ReservaDTO dto = new ReservaDTO();
        dto.setId(reserva.getId());
        
        if (reserva.getEspacoAcademico() != null) {
            dto.setEspacoAcademico(reserva.getEspacoAcademico());
            dto.setEspacoAcademicoId(reserva.getEspacoAcademico().getId());
        }
        
        if (reserva.getProfessor() != null) {
            dto.setProfessor(reserva.getProfessor());
            dto.setProfessorId(reserva.getProfessor().getId());
        }
        
        dto.setData(reserva.getData());
        
        // Converter LocalTime para String
        if (reserva.getHoraInicial() != null) {
            dto.setHoraInicial(reserva.getHoraInicial().toString());
        }
        
        if (reserva.getHoraFinal() != null) {
            dto.setHoraFinal(reserva.getHoraFinal().toString());
        }
        
        dto.setFinalidade(reserva.getFinalidade());
        dto.setStatus(reserva.getStatus());
        dto.setDataCriacao(reserva.getDataCriacao());
        dto.setDataAtualizacao(reserva.getDataAtualizacao());
        dto.setDataUtilizacao(reserva.getDataUtilizacao());
        
        return dto;
    }
}