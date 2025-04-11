package com.academico.espacos.service;

import com.academico.espacos.dto.ReservaDTO;
import com.academico.espacos.dto.ReservaInputDTO;
import com.academico.espacos.exception.BusinessException;
import com.academico.espacos.exception.ResourceNotFoundException;
import com.academico.espacos.model.EspacoAcademico;
import com.academico.espacos.model.Professor;
import com.academico.espacos.model.Reserva;
import com.academico.espacos.model.enums.StatusReserva;
import com.academico.espacos.repository.EspacoAcademicoRepository;
import com.academico.espacos.repository.ProfessorRepository;
import com.academico.espacos.repository.ReservaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReservaService {

    private static final Logger logger = LoggerFactory.getLogger(ReservaService.class);
    private static final int TEMPO_ESPERA_CONFIRMACAO_MINUTOS = 30;
    
    private final ReservaRepository reservaRepository;
    private final EspacoAcademicoRepository espacoRepository;
    private final ProfessorRepository professorRepository;
    
    public ReservaService(
            ReservaRepository reservaRepository,
            EspacoAcademicoRepository espacoRepository,
            ProfessorRepository professorRepository) {
        this.reservaRepository = reservaRepository;
        this.espacoRepository = espacoRepository;
        this.professorRepository = professorRepository;
    }
    
    /**
     * Busca todas as reservas ativas (não canceladas)
     */
    public List<ReservaDTO> buscarTodasAtivas() {
        return reservaRepository.findByStatusNot(StatusReserva.CANCELADO)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Busca reservas de um professor específico
     */
    public List<ReservaDTO> buscarPorProfessor(Long professorId) {
        Professor professor = encontrarProfessor(professorId);
                
        return reservaRepository.findByProfessorAndStatusNot(professor, StatusReserva.CANCELADO)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Busca uma reserva por ID
     */
    public ReservaDTO buscarPorId(Long id) {
        return toDTO(getReservaById(id));
    }
    
    /**
     * Busca uma entidade Reserva por ID (auxiliar)
     */
    private Reserva getReservaById(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada"));
    }
    
    /**
     * Cria uma nova reserva
     */
    @Transactional
    public ReservaDTO criar(ReservaInputDTO input) {
        validarDados(input, null);
        
        Reserva reserva = new Reserva();
        preencherDadosReserva(reserva, input);
        reserva.setStatus(StatusReserva.PENDENTE);
        
        return toDTO(reservaRepository.save(reserva));
    }
    
    /**
     * Atualiza uma reserva existente
     */
    @Transactional
    public ReservaDTO atualizar(Long id, ReservaInputDTO input) {
        Reserva reserva = getReservaById(id);
        
        if (!reserva.podeSerEditada()) {
            throw new BusinessException("Esta reserva não pode mais ser alterada");
        }
        
        validarDados(input, id);
        preencherDadosReserva(reserva, input);
        
        return toDTO(reservaRepository.save(reserva));
    }
    
    /**
     * Cancela uma reserva
     */
    @Transactional
    public void cancelar(Long id) {
        Reserva reserva = getReservaById(id);
        
        if (!reserva.getStatus().podeSerCancelada()) {
            throw new BusinessException("Esta reserva não pode ser cancelada");
        }
        
        reserva.cancelar();
        reservaRepository.save(reserva);
    }
    
    /**
     * Confirma a utilização de uma reserva
     */
    @Transactional
    public void confirmarUtilizacao(Long id) {
        Reserva reserva = getReservaById(id);
        
        if (!reserva.getStatus().podeConfirmarUtilizacao()) {
            throw new BusinessException("Não é possível confirmar a utilização desta reserva");
        }
        
        reserva.confirmarUtilizacao();
        reservaRepository.save(reserva);
    }
    
    /**
     * Verifica disponibilidade para uma nova reserva
     */
    public boolean verificarDisponibilidade(LocalDate data, LocalTime horaInicial, LocalTime horaFinal, Long espacoId, Long reservaIdExcluir) {
        return reservaRepository.countConflitos(data, horaInicial, horaFinal, espacoId, reservaIdExcluir) == 0;
    }
    
    /**
     * Método para atualizar automaticamente o status das reservas
     */
    @Scheduled(fixedRate = 60000) // Verifica a cada minuto
    @Transactional
    public void atualizarStatusReservas() {
        try {
            LocalDate hoje = LocalDate.now();
            LocalTime agora = LocalTime.now();
            
            logger.info("Atualizando status das reservas em: {}", LocalDateTime.now());
            
            atualizarReservasPendentes(hoje, agora);
            atualizarReservasEmUso(hoje, agora);
            atualizarReservasAguardandoConfirmacao(hoje, agora);
            atualizarReservasAnterioresAguardandoConfirmacao(hoje);
            
        } catch (Exception e) {
            logger.error("Erro ao atualizar status das reservas: {}", e.getMessage(), e);
        }
    }
    
    private void atualizarReservasPendentes(LocalDate hoje, LocalTime agora) {
        List<Reserva> reservasPendentes = reservaRepository.findByDataAndStatus(hoje, StatusReserva.PENDENTE);
        
        for (Reserva reserva : reservasPendentes) {
            corrigirInversaoHoras(reserva);
            
            // Se chegou na hora inicial, mudar para EM_USO
            if (agora.isAfter(reserva.getHoraInicial()) || agora.equals(reserva.getHoraInicial())) {
                atualizarStatus(reserva, StatusReserva.EM_USO);
            }
        }
    }
    
    private void atualizarReservasEmUso(LocalDate hoje, LocalTime agora) {
        List<Reserva> reservasEmUso = reservaRepository.findByDataAndStatus(hoje, StatusReserva.EM_USO);
        
        for (Reserva reserva : reservasEmUso) {
            // Se passou da hora final, mudar para AGUARDANDO_CONFIRMACAO
            if (agora.isAfter(reserva.getHoraFinal()) || agora.equals(reserva.getHoraFinal())) {
                if (verificarReservaCompleta(reserva)) {
                    corrigirInversaoHoras(reserva);
                    atualizarStatus(reserva, StatusReserva.AGUARDANDO_CONFIRMACAO);
                }
            }
        }
    }
    
    private void atualizarReservasAguardandoConfirmacao(LocalDate hoje, LocalTime agora) {
        List<Reserva> reservasAguardando = reservaRepository.findByDataAndStatus(hoje, StatusReserva.AGUARDANDO_CONFIRMACAO);
        
        for (Reserva reserva : reservasAguardando) {
            LocalTime horaLimiteConfirmacao = reserva.getHoraFinal().plusMinutes(TEMPO_ESPERA_CONFIRMACAO_MINUTOS);
            
            // Se já passou 30 minutos após horário final, atualizar para UTILIZADO automaticamente
            if (agora.isAfter(horaLimiteConfirmacao)) {
                confirmarUtilizacaoAutomatica(reserva);
            }
        }
    }
    
    private void atualizarReservasAnterioresAguardandoConfirmacao(LocalDate hoje) {
        List<Reserva> reservasAnterioresAguardando = reservaRepository.findByDataBeforeAndStatus(hoje, StatusReserva.AGUARDANDO_CONFIRMACAO);
        
        for (Reserva reserva : reservasAnterioresAguardando) {
            confirmarUtilizacaoAutomatica(reserva);
        }
    }
    
    private void confirmarUtilizacaoAutomatica(Reserva reserva) {
        reserva.setStatus(StatusReserva.UTILIZADO);
        reserva.setDataAtualizacao(LocalDateTime.now());
        reserva.setDataUtilizacao(LocalDateTime.now());
        logger.info("Reserva #{} atualizada para UTILIZADO automaticamente", reserva.getId());
        reservaRepository.save(reserva);
    }
    
    private void atualizarStatus(Reserva reserva, StatusReserva novoStatus) {
        reserva.setStatus(novoStatus);
        reserva.setDataAtualizacao(LocalDateTime.now());
        logger.info("Reserva #{} atualizada para {}", reserva.getId(), novoStatus);
        reservaRepository.save(reserva);
    }
    
    private boolean verificarReservaCompleta(Reserva reserva) {
        return reserva.getHoraInicial() != null && 
               reserva.getHoraFinal() != null &&
               reserva.getEspacoAcademico() != null && 
               reserva.getProfessor() != null;
    }
    
    private void corrigirInversaoHoras(Reserva reserva) {
        LocalTime horaInicial = reserva.getHoraInicial();
        LocalTime horaFinal = reserva.getHoraFinal();
        
        if (horaInicial != null && horaFinal != null && horaInicial.isAfter(horaFinal)) {
            reserva.setHoraInicial(horaFinal);
            reserva.setHoraFinal(horaInicial);
        }
    }
    
    /**
     * Busca reservas por espaço acadêmico e data
     */
    public List<Reserva> buscarPorEspacoEData(Long espacoId, LocalDate data) {
        // Verificar se o espaço existe
        encontrarEspaco(espacoId);
        
        // Buscar reservas
        return reservaRepository.findByEspacoAcademicoIdAndData(espacoId, data);
    }
    
    /**
     * Preenche os dados de uma reserva a partir de um DTO
     */
    private void preencherDadosReserva(Reserva reserva, ReservaInputDTO input) {
        EspacoAcademico espaco = encontrarEspaco(input.getEspacoAcademicoId());
        Professor professor = encontrarProfessor(input.getProfessorId());
                
        reserva.setEspacoAcademico(espaco);
        reserva.setProfessor(professor);
        reserva.setData(input.getData());
        reserva.setHoraInicial(input.getHoraInicial());
        reserva.setHoraFinal(input.getHoraFinal());
        reserva.setFinalidade(input.getFinalidade());
    }
    
    private EspacoAcademico encontrarEspaco(Long espacoId) {
        return espacoRepository.findById(espacoId)
                .orElseThrow(() -> new ResourceNotFoundException("Espaço Acadêmico não encontrado"));
    }
    
    private Professor encontrarProfessor(Long professorId) {
        return professorRepository.findById(professorId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado"));
    }
    
    /**
     * Valida os dados de uma reserva
     */
    private void validarDados(ReservaInputDTO input, Long idReservaAtual) {
        // Verificar se o espaço existe e está disponível
        EspacoAcademico espaco = encontrarEspaco(input.getEspacoAcademicoId());
        
        // Verificar se o espaço está marcado como disponível no sistema
        if (!espaco.isDisponivel()) {
            throw new BusinessException("Este espaço está indisponível (em manutenção/reforma)");
        }
        
        // Verificar se o professor existe
        encontrarProfessor(input.getProfessorId());
        
        // Obter dados de data e hora
        validarDataHora(input.getData(), input.getHoraInicial(), input.getHoraFinal());
        
        // Verificar conflito com outras reservas
        if (!verificarDisponibilidade(
                input.getData(), 
                input.getHoraInicial(), 
                input.getHoraFinal(), 
                input.getEspacoAcademicoId(), 
                idReservaAtual)) {
            throw new BusinessException("Já existe uma reserva para este espaço no horário solicitado");
        }
    }
    
    private void validarDataHora(LocalDate data, LocalTime horaInicial, LocalTime horaFinal) {
        // Validação de data passada
        if (data.isBefore(LocalDate.now())) {
            throw new BusinessException("Não é possível fazer reservas para datas passadas");
        }
        
        // Validação de hora (hora final deve ser posterior à inicial)
        if (horaInicial.isAfter(horaFinal) || horaInicial.equals(horaFinal)) {
            throw new BusinessException("Hora final deve ser posterior à hora inicial");
        }
    }
    
    /**
     * Converte entidade para DTO
     */
    private ReservaDTO toDTO(Reserva reserva) {
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
    
    /**
     * Converte entidade para DTO
     */
    public ReservaDTO toReservaDTO(Reserva reserva) {
        return ReservaDTO.fromEntity(reserva);
    }
    
    /**
     * Cria uma nova reserva a partir de um DTO
     */
    @Transactional
    public Reserva criarReserva(ReservaDTO reservaDTO) {
        // Validar e converter dados do DTO
        Reserva reserva = new Reserva();
        
        // Buscar entidades relacionadas
        EspacoAcademico espaco = encontrarEspaco(reservaDTO.getEspacoAcademicoId());
        Professor professor = encontrarProfessor(reservaDTO.getProfessorId());
        
        // Preencher a reserva
        reserva.setEspacoAcademico(espaco);
        reserva.setProfessor(professor);
        reserva.setData(reservaDTO.getData());
        
        // Converter String para LocalTime
        if (reservaDTO.getHoraInicial() != null) {
            reserva.setHoraInicial(LocalTime.parse(reservaDTO.getHoraInicial()));
        }
        
        if (reservaDTO.getHoraFinal() != null) {
            reserva.setHoraFinal(LocalTime.parse(reservaDTO.getHoraFinal()));
        }
        
        reserva.setFinalidade(reservaDTO.getFinalidade());
        reserva.setStatus(StatusReserva.PENDENTE); // Nova reserva sempre começa como pendente
        reserva.setDataCriacao(LocalDateTime.now());
        reserva.setDataAtualizacao(LocalDateTime.now());
        
        // Verificar disponibilidade
        if (!verificarDisponibilidade(
            reserva.getData(),
            reserva.getHoraInicial(),
            reserva.getHoraFinal(),
            reserva.getEspacoAcademico().getId(),
            reserva.getId()
        )) {
            throw new BusinessException("Já existe uma reserva para este espaço no horário solicitado");
        }
        
        // Salvar e retornar
        return reservaRepository.save(reserva);
    }
}