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

	// Centralizar validações
	private void validarReserva(Reserva reserva, boolean isUpdate) {
		// Verificar campos obrigatórios
		if (reserva.getEspacoAcademico() == null || reserva.getEspacoAcademico().getId() == null) {
			throw new IllegalArgumentException("Espaço Acadêmico é obrigatório");
		}

		if (reserva.getProfessor() == null || reserva.getProfessor().getId() == null) {
			throw new IllegalArgumentException("Professor é obrigatório");
		}

		if (reserva.getData() == null) {
			throw new IllegalArgumentException("Data é obrigatória");
		}

		if (reserva.getHoraInicial() == null || reserva.getHoraFinal() == null) {
			throw new IllegalArgumentException("Horários são obrigatórios");
		}

		// Validar se a data não é passada
		LocalDateTime agora = LocalDateTime.now();
		LocalDate hoje = agora.toLocalDate();

		if (reserva.getData().isBefore(hoje)) {
			throw new IllegalArgumentException("Não é possível fazer reservas para datas passadas");
		}

		// Validar horários
		if (reserva.getHoraFinal().isBefore(reserva.getHoraInicial())) {
			throw new IllegalArgumentException("Hora final não pode ser anterior à hora inicial");
		}

		 // Validar espaço disponível
		EspacoAcademico espaco = espacoRepository.findById(reserva.getEspacoAcademico().getId())
			.orElseThrow(() -> new ResourceNotFoundException("Espaço Acadêmico não encontrado"));

		if (!espaco.isDisponivel()) {
			throw new IllegalStateException("Este espaço acadêmico não está disponível para reservas");
		}

		 // Verificar conflitos (opcionalmente excluindo a própria reserva se for atualização)
		List<Reserva> reservasConflitantes = repository.findReservasConflitantes(
			reserva.getEspacoAcademico().getId(),
			reserva.getData(), 
			reserva.getHoraInicial(), 
			reserva.getHoraFinal());

		if (isUpdate) {
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
		LocalDateTime agora = LocalDateTime.now();
		LocalDate hoje = agora.toLocalDate();
		LocalTime horaAtual = agora.toLocalTime();
		
		// Buscar todas as reservas não canceladas
		List<Reserva> reservasAtivas = repository.findByStatusNot(StatusReserva.CANCELADO);
		
		for (Reserva reserva : reservasAtivas) {
			// Pular reservas já utilizadas (concluídas)
			if (reserva.getStatus() == StatusReserva.UTILIZADO) {
				continue;
			}
			
			// Melhorar a verificação para marcar como EM_USO
			if (reserva.getData().equals(hoje) && 
				horaAtual.isAfter(reserva.getHoraInicial()) && 
				horaAtual.isBefore(reserva.getHoraFinal())) {
				
				// Log para depuração
				System.out.println("Atualizando reserva #" + reserva.getId() + " para EM_USO");
				
				reserva.setStatus(StatusReserva.EM_USO);
				repository.save(reserva);
				continue;
			}
			
			// Verificar se deve ser marcada como UTILIZADO (passou do horário final)
			LocalDateTime dataHoraFinal = LocalDateTime.of(reserva.getData(), reserva.getHoraFinal());
			if (agora.isAfter(dataHoraFinal)) {
				// Log para depuração
				System.out.println("Atualizando reserva #" + reserva.getId() + " para UTILIZADO");
				
				reserva.setStatus(StatusReserva.UTILIZADO);
				repository.save(reserva);
			}
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
		
		// Verificar se não pode mais ser cancelada (já utilizada ou em andamento)
		if (reserva.getStatus() == StatusReserva.UTILIZADO) {
			throw new IllegalStateException("Não é possível cancelar uma reserva já utilizada");
		}
		
		// Verificar se está a menos de 30 minutos do início
		LocalDateTime agora = LocalDateTime.now();
		LocalDateTime horaInicio = LocalDateTime.of(reserva.getData(), reserva.getHoraInicial());
		
		long minutosAteInicio = ChronoUnit.MINUTES.between(agora, horaInicio);
		if (minutosAteInicio < 30 && minutosAteInicio > 0) {
			throw new IllegalStateException("Não é possível cancelar reservas com menos de 30 minutos de antecedência");
		}
		
		// IMPORTANTE: Mudamos para CANCELADO em vez de excluir
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
		
		// Verificar se está em uso
		LocalDateTime agora = LocalDateTime.now();
		LocalDateTime horaInicio = LocalDateTime.of(reserva.getData(), reserva.getHoraInicial());
		LocalDateTime horaFim = LocalDateTime.of(reserva.getData(), reserva.getHoraFinal());
		
		if (!(agora.isAfter(horaInicio) && agora.isBefore(horaFim))) {
			throw new IllegalStateException("Só é possível confirmar a utilização durante o período reservado");
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
		// Inicializar mapa de horários com intervalos de 30 minutos entre 7:00 e 23:00
		Map<String, Boolean> horariosDisponiveis = new TreeMap<>();
		
		LocalTime horaInicial = LocalTime.of(7, 0);
		LocalTime horaFinal = LocalTime.of(23, 0);
		
		// Criar slots de 30 minutos
		while (horaInicial.isBefore(horaFinal)) {
			String slot = horaInicial.toString();
			horariosDisponiveis.put(slot, true); // inicialmente todos os slots estão disponíveis
			horaInicial = horaInicial.plusMinutes(30);
		}
		
		// Buscar reservas existentes para o espaço na data específica
		List<Reserva> reservasExistentes = repository.findByEspacoAcademicoIdAndData(espacoId, data);
		
		// Marcar horários ocupados
		for (Reserva reserva : reservasExistentes) {
			if (reserva.getStatus() == StatusReserva.CANCELADO) {
				continue; // ignorar reservas canceladas
			}
			
			LocalTime inicio = reserva.getHoraInicial();
			LocalTime fim = reserva.getHoraFinal();
			
			// Marcar todos os slots entre o início e o fim da reserva como indisponíveis
			LocalTime atual = inicio;
			while (atual.isBefore(fim)) {
				String slot = atual.toString();
				if (horariosDisponiveis.containsKey(slot)) {
					horariosDisponiveis.put(slot, false);
				}
				atual = atual.plusMinutes(30);
			}
		}
		
		// Se um professor foi especificado, verificar também suas reservas em outros espaços
		if (professorId != null) {
			List<Reserva> reservasProfessor = repository.findByProfessorIdAndData(professorId, data);
			
			for (Reserva reserva : reservasProfessor) {
				if (reserva.getStatus() == StatusReserva.CANCELADO) {
					continue;
				}
				
				LocalTime inicio = reserva.getHoraInicial();
				LocalTime fim = reserva.getHoraFinal();
				
				// Marcar horários em que o professor já está ocupado
				LocalTime atual = inicio;
				while (atual.isBefore(fim)) {
					String slot = atual.toString();
					if (horariosDisponiveis.containsKey(slot)) {
						horariosDisponiveis.put(slot, false);
					}
					atual = atual.plusMinutes(30);
				}
			}
		}
		
		return horariosDisponiveis;
	}
}