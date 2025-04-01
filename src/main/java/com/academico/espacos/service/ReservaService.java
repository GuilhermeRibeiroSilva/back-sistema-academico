package com.academico.espacos.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.academico.espacos.model.Reserva;
import com.academico.espacos.model.Reserva.StatusReserva;
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

	@Scheduled(fixedRate = 60000) // Executa a cada minuto
	@Transactional
	public void atualizarStatusReservasAutomaticamente() {
		LocalDateTime agora = LocalDateTime.now();
		LocalDate hoje = agora.toLocalDate();
		LocalTime horaAtual = agora.toLocalTime();

		// Busca reservas pendentes do dia
		List<Reserva> reservasDoDia = repository.findByDataAndStatus(hoje, StatusReserva.PENDENTE);

		for (Reserva reserva : reservasDoDia) {
			// Atualiza para EM_USO se estiver no horário
			if (horaAtual.isAfter(reserva.getHoraInicial()) && horaAtual.isBefore(reserva.getHoraFinal())) {
				reserva.setStatus(StatusReserva.EM_USO);
				repository.save(reserva);
			}

			// Atualiza para UTILIZADO se passou do horário e não foi confirmado
			if (horaAtual.isAfter(reserva.getHoraFinal())) {
				reserva.setStatus(StatusReserva.UTILIZADO);
				repository.save(reserva);
			}
		}

		// Atualiza reservas EM_USO que já passaram do horário
		List<Reserva> reservasEmUso = repository.findByDataAndStatus(hoje, StatusReserva.EM_USO);
		for (Reserva reserva : reservasEmUso) {
			if (horaAtual.isAfter(reserva.getHoraFinal())) {
				reserva.setStatus(StatusReserva.UTILIZADO);
				repository.save(reserva);
			}
		}
	}

	public void confirmarUtilizacao(Long id) {
		Reserva reserva = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada"));

		// Add proper validation
		if (reserva.getStatus() == StatusReserva.UTILIZADO) {
			throw new IllegalStateException("Reserva já foi confirmada como utilizada");
		}

		// Update status
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
			if (reserva.getStatus() == StatusReserva.CANCELADA) {
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

	@Transactional
	public void cancelarReserva(Long id, Usuario usuarioAtual) {
		Reserva reserva = repository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada"));
		
		// Verificar permissão
		if (!usuarioAtual.canAccess(reserva)) {
			throw new AccessDeniedException("Sem permissão para acessar esta reserva");
		}
		
		// Não permitir cancelar reservas já concluídas ou já canceladas
		if (reserva.getStatus() == StatusReserva.UTILIZADO || 
			reserva.getStatus() == StatusReserva.CANCELADA) {
			throw new IllegalStateException(
				"Não é possível cancelar uma reserva que já foi " + 
				(reserva.getStatus() == StatusReserva.UTILIZADO ? "utilizada" : "cancelada")
			);
		}
		
		reserva.setStatus(StatusReserva.CANCELADA);
		repository.save(reserva);
	}

	private void validarConflitos(Reserva reserva) {
		List<Reserva> conflitantes = repository.findReservasConflitantes(reserva.getEspacoAcademico().getId(),
				reserva.getData(), reserva.getHoraInicial(), reserva.getHoraFinal());

		// Remove a própria reserva da lista de conflitos (caso seja uma atualização)
		conflitantes = conflitantes.stream().filter(r -> !r.getId().equals(reserva.getId()))
				.filter(r -> r.getStatus() != StatusReserva.CANCELADA).collect(Collectors.toList());

		if (!conflitantes.isEmpty()) {
			throw new ReservaConflitanteException("Já existe uma reserva para este espaço neste horário");
		}
	}

	@Transactional
	public void atualizarStatusReserva(Long id, StatusReserva novoStatus) {
		Reserva reserva = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada"));

		// Validações de transição de status
		if (reserva.getStatus() == StatusReserva.CANCELADA) {
			throw new IllegalStateException("Não é possível alterar o status de uma reserva cancelada");
		}

		if (reserva.getStatus() == StatusReserva.UTILIZADO && novoStatus != StatusReserva.CANCELADA) {
			throw new IllegalStateException("Não é possível alterar o status de uma reserva já utilizada");
		}

		reserva.setStatus(novoStatus);
		repository.save(reserva);
	}

	public List<Reserva> listarReservas(Usuario usuarioAtual) {
		if (usuarioAtual == null) {
			throw new IllegalArgumentException("Usuário não informado");
		}
		
		// Admin vê todas as reservas
		if (usuarioAtual.isAdmin()) {
			return repository.findAll();
		} 
		// Professor vê apenas suas próprias reservas
		else if (usuarioAtual.isProfessor() && usuarioAtual.getProfessor() != null) {
			return repository.findByProfessorId(usuarioAtual.getProfessor().getId());
		} 
		// Caso improvável, mas para garantir
		else {
			return new ArrayList<>();
		}
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
}