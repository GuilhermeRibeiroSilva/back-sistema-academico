package com.academico.espacos.controller;

import com.academico.espacos.dto.DisponibilidadeDTO;
import com.academico.espacos.dto.ReservaDTO;
import com.academico.espacos.dto.ReservaInputDTO;
import com.academico.espacos.exception.BusinessException;
import com.academico.espacos.exception.ResourceNotFoundException;
import com.academico.espacos.model.Usuario;
import com.academico.espacos.model.enums.Perfil;
import com.academico.espacos.service.ReservaService;
import com.academico.espacos.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {
    
    @Autowired
    private ReservaService reservaService;
    
    @Autowired
    private UsuarioService usuarioService;
    
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
     * Obtém o usuário logado a partir do token de autenticação
     */
    private Usuario getUsuarioLogado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return usuarioService.buscarPorUsername(authentication.getName());
    }
}