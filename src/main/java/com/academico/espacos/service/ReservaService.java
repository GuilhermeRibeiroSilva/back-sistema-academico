package com.academico.espacos.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academico.espacos.model.Reserva;
import com.academico.espacos.model.enums.StatusReserva;
import com.academico.espacos.model.EspacoAcademico;
import com.academico.espacos.model.Usuario;
import com.academico.espacos.model.Professor;
import com.academico.espacos.repository.ReservaRepository;
import com.academico.espacos.repository.EspacoAcademicoRepository;
import com.academico.espacos.repository.UsuarioRepository;
import com.academico.espacos.repository.ProfessorRepository;
import com.academico.espacos.exception.ResourceNotFoundException;
import com.academico.espacos.exception.ReservaConflitanteException;
import com.academico.espacos.security.JwtTokenProvider;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.TreeMap;
import java.time.format.DateTimeFormatter;

@Service
public class ReservaService {

	@Autowired
	private ReservaRepository repository;

	@Autowired
	private EspacoAcademicoRepository espacoRepository;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private ProfessorRepository professorRepository;

	@Autowired
	private JwtTokenProvider jwtTokenProvider;

	@Autowired
	private EntityManager entityManager;

	// Substitua a chamada ao findConflictingReservations por este método
	private List<Reserva> verificarReservasConflitantes(Long espacoId, LocalDate data, 
	                                                  LocalTime horaInicial, LocalTime horaFinal) {
	    // Buscar todas as reservas ativas para o espaço na data especificada
	    String jpql = "SELECT r FROM Reserva r " +
	                  "WHERE r.espacoAcademico.id = :espacoId " +
	                  "AND r.data = :data " +
	                  "AND r.status <> :statusCancelado";
	                  
	    List<Reserva> reservasNaData = entityManager.createQuery(jpql, Reserva.class)
	        .setParameter("espacoId", espacoId)
	        .setParameter("data", data)
	        .setParameter("statusCancelado", StatusReserva.CANCELADO)
	        .getResultList();
	    
	    // Filtrar manualmente as reservas que conflitam com o horário
	    return reservasNaData.stream()
	        .filter(r -> {
	            // Verifica se há sobreposição de horários
	            return !(r.getHoraFinal().isBefore(horaInicial) || r.getHoraInicial().isAfter(horaFinal));
	        })
	        .collect(Collectors.toList());
	}

	// Centralizar validações
	private void validarReserva(Reserva reserva, boolean isUpdate) {
	    // Validar se data está preenchida
	    if (reserva.getData() == null) {
	        throw new IllegalArgumentException("Data da reserva é obrigatória");
	    }
	    
	    if (reserva.getHoraInicial() == null || reserva.getHoraFinal() == null) {
	        throw new IllegalArgumentException("Horários de início e fim são obrigatórios");
	    }
	    
	    // Verificar data/hora no futuro
	    LocalDateTime agora = LocalDateTime.now();
	    LocalDateTime dataHoraReserva = LocalDateTime.of(reserva.getData(), reserva.getHoraInicial());
	    
	    if (dataHoraReserva.isBefore(agora)) {
	        throw new IllegalArgumentException("Não é possível fazer reservas para datas/horários passados");
	    }
	    
	    // Validar hora final maior que inicial
	    if (reserva.getHoraFinal().isBefore(reserva.getHoraInicial())) {
	        throw new IllegalArgumentException("Hora final não pode ser anterior à hora inicial");
	    }
	    
	    // Verificar espaço disponível
	    if (reserva.getEspacoAcademico() == null || reserva.getEspacoAcademico().getId() == null) {
	        throw new IllegalArgumentException("Espaço acadêmico é obrigatório");
	    }
	    
	    EspacoAcademico espaco = espacoRepository.findById(reserva.getEspacoAcademico().getId())
	        .orElseThrow(() -> new ResourceNotFoundException("Espaço Acadêmico não encontrado"));
	    
	    if (!espaco.isDisponivel()) {
	        throw new IllegalArgumentException("Este espaço não está disponível para reservas");
	    }
	    
	    // Verificar conflitos usando o novo método
	    List<Reserva> reservasConflitantes = verificarReservasConflitantes(
	        reserva.getEspacoAcademico().getId(),
	        reserva.getData(),
	        reserva.getHoraInicial(),
	        reserva.getHoraFinal()
	    );
	    
	    // Remover a própria reserva da lista de conflitos se for update
	    if (isUpdate && reserva.getId() != null) {
	        reservasConflitantes = reservasConflitantes.stream()
	            .filter(r -> !r.getId().equals(reserva.getId()))
	            .collect(Collectors.toList());
	    }
	    
	    if (!reservasConflitantes.isEmpty()) {
	        throw new ReservaConflitanteException("Já existe uma reserva para este horário");
	    }
	}

	// Usar nas funções existentes
	@Transactional
	public Reserva solicitar(Reserva reserva, Usuario usuarioAtual) {
		// Validações básicas
		validarReserva(reserva, false);
		
		// Se for professor, forçar que a reserva seja para ele mesmo
		if (usuarioAtual.isProfessor()) {
			if (!usuarioAtual.isOwner(reserva.getProfessor())) {
				throw new AccessDeniedException("Professores só podem fazer reservas para si mesmos");
			}
		}
		
		 // Adicionar ajuste para o problema de timezone
		// Se a data vem com um dia a menos, adicione 1 dia para compensar
		if (reserva.getData() != null) {
			reserva.setData(reserva.getData().plusDays(1));
		}
		
		// Verificar conflitos
		validarConflitos(reserva);
		
		// Buscar entidades completas
		EspacoAcademico espaco = espacoRepository.findById(reserva.getEspacoAcademico().getId())
			.orElseThrow(() -> new ResourceNotFoundException("Espaço Acadêmico não encontrado"));
			
		Professor professor = professorRepository.findById(reserva.getProfessor().getId())
			.orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado"));
		
		// Status inicial é PENDENTE para todos os usuários
		reserva.setStatus(StatusReserva.PENDENTE);
		reserva.setEspacoAcademico(espaco);
		reserva.setProfessor(professor);
		
		return repository.save(reserva);
	}

	public Reserva atualizar(Reserva reserva) {
		// Verificar se a reserva existe
		Reserva reservaExistente = repository.findById(reserva.getId())
			.orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada"));

		// Não permitir atualizar reservas já utilizadas
		if (reservaExistente.isUtilizado()) {
			throw new IllegalStateException("Não é possível alterar uma reserva já utilizada");
		}

		validarReserva(reserva, true);

		// Manter alguns dados originais que não devem ser alterados
		reserva.setUtilizado(reservaExistente.isUtilizado());

		return repository.save(reserva);
	}

	@Scheduled(fixedRate = 60000) // A cada minuto
	@Transactional
	public void atualizarStatusReservasAutomaticamente() {
	    try {
	        LocalDateTime agora = LocalDateTime.now();
	        
	        // Log para debug
	        System.out.println("Executando atualização automática de status: " + agora);
	        
	        // Buscar todas as reservas não canceladas
	        List<Reserva> reservasAtivas = repository.findByStatusNot(StatusReserva.CANCELADO);
	        System.out.println("Encontradas " + reservasAtivas.size() + " reservas ativas para verificação");
	        
	        for (Reserva reserva : reservasAtivas) {
	            LocalDateTime dataHoraInicio = LocalDateTime.of(reserva.getData(), reserva.getHoraInicial());
	            LocalDateTime dataHoraFim = LocalDateTime.of(reserva.getData(), reserva.getHoraFinal());
	            LocalDateTime limiteConfirmacao = dataHoraFim.plusMinutes(30); // 30 minutos após o fim
	            
	            StatusReserva statusAtual = reserva.getStatus();
	            StatusReserva novoStatus = null;
	            
	            // Verificar transição específica com base no status atual
	            if (statusAtual == StatusReserva.PENDENTE && agora.isAfter(dataHoraInicio) && agora.isBefore(dataHoraFim)) {
	                novoStatus = StatusReserva.EM_USO;
	            }
	            else if (statusAtual == StatusReserva.EM_USO && agora.isAfter(dataHoraFim) && agora.isBefore(limiteConfirmacao)) {
	                novoStatus = StatusReserva.AGUARDANDO_CONFIRMACAO;
	            }
	            else if (statusAtual == StatusReserva.AGUARDANDO_CONFIRMACAO && agora.isAfter(limiteConfirmacao)) {
	                novoStatus = StatusReserva.UTILIZADO;
	                reserva.setUtilizado(true);
	            }
	            
	            // Se um novo status foi determinado, atualizar
	            if (novoStatus != null && novoStatus != statusAtual) {
	                System.out.println("Reserva #" + reserva.getId() + " - Transição: " + statusAtual + " -> " + novoStatus);
	                reserva.setStatus(novoStatus);
	                repository.save(reserva);
	            }
	        }
	    } catch (Exception e) {
	        System.err.println("Erro ao atualizar status das reservas: " + e.getMessage());
	        e.printStackTrace();
	    }
	}

	@Transactional
	public void cancelarReserva(Long id, Usuario usuarioAtual) {
		Reserva reserva = repository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada"));
		
		// Verificar permissão
		if (!usuarioAtual.canAccess(reserva)) {
			throw new AccessDeniedException("Sem permissão para cancelar esta reserva");
		}
		
		// Verificar se o status permite cancelamento
		if (!reserva.getStatus().podeSerCancelada()) {
			throw new IllegalStateException("Não é possível cancelar a reserva com o status atual: " + reserva.getStatus());
		}
		
		// Se estiver em status PENDENTE, verificar se falta mais de 30 minutos
		if (reserva.getStatus() == StatusReserva.PENDENTE) {
			LocalDateTime agora = LocalDateTime.now();
			LocalDateTime horaInicio = LocalDateTime.of(reserva.getData(), reserva.getHoraInicial());
			
			long minutosAteInicio = ChronoUnit.MINUTES.between(agora, horaInicio);
			if (minutosAteInicio < 30 && minutosAteInicio > 0) {
				throw new IllegalStateException("Não é possível cancelar reservas com menos de 30 minutos de antecedência");
			}
		}
		
		reserva.setStatus(StatusReserva.CANCELADO);
		repository.save(reserva);
	}

	public boolean reservaPodeSerEditada(Long id) {
		Reserva reserva = repository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada"));
			
		// Não pode editar se já foi utilizada ou cancelada
		if (reserva.getStatus() == StatusReserva.UTILIZADO || 
			reserva.getStatus() == StatusReserva.CANCELADO) {
			return false;
		}
		
		// Verificar se está a mais de 30 minutos do início
		LocalDateTime agora = LocalDateTime.now();
		LocalDateTime horaInicio = LocalDateTime.of(reserva.getData(), reserva.getHoraInicial());
		
		return ChronoUnit.MINUTES.between(agora, horaInicio) >= 30;
	}

	@Transactional
	public void confirmarUtilizacao(Long id, Usuario usuarioAtual) {
		Reserva reserva = repository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada"));
			
		// Verificar permissão
		if (!usuarioAtual.canAccess(reserva)) {
			throw new AccessDeniedException("Sem permissão para confirmar esta reserva");
		}
		
		// Verificar se o status permite confirmação
		if (!reserva.getStatus().podeConfirmarUtilizacao()) {
			throw new IllegalStateException("Não é possível confirmar utilização neste momento. Status atual: " + reserva.getStatus());
		}
		
		reserva.setStatus(StatusReserva.UTILIZADO);
		reserva.setUtilizado(true);
		repository.save(reserva);
	}

	public List<Reserva> listarTodas() {
		// Buscar reservas já ordenadas
		List<Reserva> reservas = repository.findAllOrderByDataAndHoraInicial();

		// Atualiza status de reservas encerradas (2 horas após o término)
		LocalDateTime agora = LocalDateTime.now();

		for (Reserva reserva : reservas) {
			// Pular reservas canceladas
			if (reserva.getStatus() == StatusReserva.CANCELADO) {
				continue;
			}

			// Combina data e hora final
			LocalDateTime dataHoraFinal = LocalDateTime.of(reserva.getData(), reserva.getHoraFinal());

			// Se passou 2 horas do fim e não foi marcada como utilizada
			if (dataHoraFinal.plusHours(2).isBefore(agora) && !reserva.isUtilizado()) {
				reserva.setUtilizado(true);
				reserva.setStatus(StatusReserva.UTILIZADO);
				repository.save(reserva);
			}
		}

		return reservas;
	}

	public Optional<Reserva> buscarPorId(Long id) {
		return repository.findById(id);
	}

	public List<Reserva> buscarReservasPorProfessor(Long professorId) {
		return repository.findByProfessorId(professorId);
	}

	public List<Reserva> listarReservas(Usuario usuarioAtual) {
		List<Reserva> reservas;
		
		if (usuarioAtual.isAdmin()) {
			reservas = repository.findAll();
		} else if (usuarioAtual.isProfessor()) {
			reservas = repository.findByProfessorId(usuarioAtual.getProfessor().getId());
		} else {
			return new ArrayList<>();
		}
		
		// Filtrar reservas canceladas - importante!
		return reservas.stream()
			.filter(r -> r.getStatus() != StatusReserva.CANCELADO)
			.collect(Collectors.toList());
	}

	@Transactional
	public void excluir(Long reservaId, Usuario usuarioAtual) {
		Reserva reserva = repository.findById(reservaId)
				.orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada"));

		if (!usuarioAtual.canAccess(reserva)) {
			throw new AccessDeniedException("Sem permissão para acessar esta reserva");
		}

		repository.delete(reserva);
	}

	private void validarConflitos(Reserva reserva) {
		List<Reserva> conflitantes = repository.findReservasConflitantes(reserva.getEspacoAcademico().getId(),
				reserva.getData(), reserva.getHoraInicial(), reserva.getHoraFinal());

		// Remove a própria reserva da lista de conflitos (caso seja uma atualização)
		conflitantes = conflitantes.stream().filter(r -> !r.getId().equals(reserva.getId()))
				.filter(r -> r.getStatus() != StatusReserva.CANCELADO).collect(Collectors.toList());

		if (!conflitantes.isEmpty()) {
			throw new ReservaConflitanteException("Já existe uma reserva para este espaço neste horário");
		}
	}

	@Transactional
	public void atualizarStatusReserva(Long id, StatusReserva novoStatus) {
		Reserva reserva = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada"));

		// Validações de transição de status
		if (reserva.getStatus() == StatusReserva.CANCELADO) {
			throw new IllegalStateException("Não é possível alterar o status de uma reserva cancelada");
		}

		if (reserva.getStatus() == StatusReserva.UTILIZADO && novoStatus != StatusReserva.CANCELADO) {
			throw new IllegalStateException("Não é possível alterar o status de uma reserva já utilizada");
		}

		reserva.setStatus(novoStatus);
		repository.save(reserva);
	}

	/**
	 * Verifica os horários disponíveis para um determinado espaço em uma data específica
	 */
	public Map<String, Boolean> verificarHorariosDisponiveis(Long espacoId, LocalDate data, Long professorId) {
	    // Inicializar mapa de horários com intervalos de 10 minutos
	    Map<String, Boolean> horariosDisponiveis = new TreeMap<>();
	    
	    // Período típico de funcionamento: 7:00 às 23:00
	    LocalTime inicioExpediente = LocalTime.of(7, 0);
	    LocalTime fimExpediente = LocalTime.of(23, 0);
	    
	    // Criar slots de 10 minutos
	    LocalTime horaAtual = inicioExpediente;
	    while (horaAtual.isBefore(fimExpediente)) {
	        // Formatar para hora:minuto (HH:mm)
	        String horarioFormatado = horaAtual.format(DateTimeFormatter.ofPattern("HH:mm"));
	        
	        // Por padrão o horário está disponível
	        horariosDisponiveis.put(horarioFormatado, true);
	        
	        // Avançar 10 minutos
	        horaAtual = horaAtual.plusMinutes(10);
	    }
	    
	    // Buscar reservas existentes para esta data e espaço
	    String jpql = "SELECT r FROM Reserva r " +
	                  "WHERE r.espacoAcademico.id = :espacoId " +
	                  "AND r.data = :data " +
	                  "AND r.status <> :statusCancelado";
	                  
	    List<Reserva> reservasExistentes = entityManager.createQuery(jpql, Reserva.class)
	        .setParameter("espacoId", espacoId)
	        .setParameter("data", data)
	        .setParameter("statusCancelado", StatusReserva.CANCELADO)
	        .getResultList();
	    
	    // Marcar horários reservados como indisponíveis
	    for (Reserva reserva : reservasExistentes) {
	        LocalTime inicio = reserva.getHoraInicial();
	        LocalTime fim = reserva.getHoraFinal();
	        
	        // Marcar todos os slots que se sobrepõem com esta reserva como indisponíveis
	        horariosDisponiveis.forEach((hora, disponivel) -> {
	            LocalTime horaSlot = LocalTime.parse(hora);
	            
	            // Se o slot estiver dentro ou se sobrepõe à reserva existente
	            if (!horaSlot.isBefore(inicio) && horaSlot.isBefore(fim)) {
	                horariosDisponiveis.put(hora, false);
	            }
	        });
	    }
	    
	    return horariosDisponiveis;
	}

	/**
	 * Verifica se uma reserva pode ser alterada (apenas administradores e apenas até 30 minutos antes)
	 */
	public boolean reservaPodeSerAlterada(Long id, Usuario usuarioAtual) {
		Reserva reserva = repository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada"));
			
		// Apenas administradores podem alterar
		if (!usuarioAtual.isAdmin()) {
			return false;
		}
		
		// Não pode editar se não estiver no status pendente
		if (!reserva.getStatus().podeSerEditada()) {
			return false;
		}
		
		// Verificar se está a mais de 30 minutos do início
		LocalDateTime agora = LocalDateTime.now();
		LocalDateTime horaInicio = LocalDateTime.of(reserva.getData(), reserva.getHoraInicial());
		
		return ChronoUnit.MINUTES.between(agora, horaInicio) >= 30;
	}

	/**
	 * Verifica se uma reserva pode ser cancelada
	 */
	public boolean reservaPodeSerCancelada(Long id, Usuario usuarioAtual) {
		Reserva reserva = repository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada"));
			
		// Verificar permissão
		if (!usuarioAtual.canAccess(reserva)) {
			return false;
		}
		
		// Não pode cancelar baseado no status
		if (!reserva.getStatus().podeSerCancelada()) {
			return false;
		}
		
		// Se estiver em status PENDENTE, verificar se falta mais de 30 minutos
		if (reserva.getStatus() == StatusReserva.PENDENTE) {
			LocalDateTime agora = LocalDateTime.now();
			LocalDateTime horaInicio = LocalDateTime.of(reserva.getData(), reserva.getHoraInicial());
			
			return ChronoUnit.MINUTES.between(agora, horaInicio) >= 30;
		}
		
		return true;
	}
}