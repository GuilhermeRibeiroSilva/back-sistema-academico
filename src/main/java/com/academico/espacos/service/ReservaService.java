package com.academico.espacos.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.academico.espacos.model.Reserva;
import com.academico.espacos.model.Reserva.StatusReserva;
import com.academico.espacos.model.EspacoAcademico;
import com.academico.espacos.repository.ReservaRepository;
import com.academico.espacos.repository.EspacoAcademicoRepository;
import com.academico.espacos.exception.ResourceNotFoundException;
import com.academico.espacos.exception.ReservaConflitanteException;
import java.util.List;
import java.util.Optional;
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

	public Reserva solicitar(Reserva reserva) {
		EspacoAcademico espaco = espacoRepository.findById(reserva.getEspacoAcademico().getId())
				.orElseThrow(() -> new ResourceNotFoundException("Espaço Acadêmico não encontrado"));

		if (!espaco.isDisponivel()) {
			throw new IllegalStateException("Este espaço acadêmico não está disponível para reservas");
		}

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
		if (reserva.getData().isBefore(LocalDate.now())) {
			throw new IllegalArgumentException("Não é possível fazer reservas para datas passadas");
		}

		// Validar horários
		if (reserva.getHoraFinal().isBefore(reserva.getHoraInicial())) {
			throw new IllegalArgumentException("Hora final não pode ser anterior à hora inicial");
		}

		// Verificar conflitos
		List<Reserva> reservasConflitantes = repository.findReservasConflitantes(reserva.getEspacoAcademico().getId(),
				reserva.getData(), reserva.getHoraInicial(), reserva.getHoraFinal());

		if (!reservasConflitantes.isEmpty()) {
			throw new ReservaConflitanteException("Já existe uma reserva para este horário");
		}

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

		// Validar se a data não é passada
		if (reserva.getData().isBefore(LocalDate.now())) {
			throw new IllegalArgumentException("Não é possível atualizar para datas passadas");
		}

		// Validar horários
		if (reserva.getHoraFinal().isBefore(reserva.getHoraInicial())) {
			throw new IllegalArgumentException("Hora final não pode ser anterior à hora inicial");
		}

		// Verificar conflitos (excluindo a própria reserva)
		List<Reserva> reservasConflitantes = repository
				.findReservasConflitantes(reserva.getEspacoAcademico().getId(), reserva.getData(),
						reserva.getHoraInicial(), reserva.getHoraFinal())
				.stream().filter(r -> !r.getId().equals(reserva.getId())).toList();

		if (!reservasConflitantes.isEmpty()) {
			throw new ReservaConflitanteException("Já existe uma reserva para este horário");
		}

		// Validar se o espaço existe e está disponível
		EspacoAcademico espaco = espacoRepository.findById(reserva.getEspacoAcademico().getId())
				.orElseThrow(() -> new ResourceNotFoundException("Espaço Acadêmico não encontrado"));

		if (!espaco.isDisponivel()) {
			throw new IllegalStateException("Este espaço acadêmico não está disponível para reservas");
		}

		// Manter alguns dados originais que não devem ser alterados
		reserva.setUtilizado(reservaExistente.isUtilizado());

		return repository.save(reserva);
	}

	private void validarDadosReserva(Reserva reserva) {
		LocalDateTime agora = LocalDateTime.now();
		LocalDateTime dataHoraReserva = LocalDateTime.of(reserva.getData(), reserva.getHoraInicial());

		// Não permite reservas em datas passadas
		if (dataHoraReserva.isBefore(agora)) {
			throw new IllegalArgumentException("Não é possível fazer reservas em datas/horários passados");
		}

		// Validação do horário final
		if (reserva.getHoraFinal().isBefore(reserva.getHoraInicial())) {
			throw new IllegalArgumentException("Hora final deve ser posterior à hora inicial");
		}

		// Validação do mesmo dia
		LocalTime meiaNoite = LocalTime.MIDNIGHT;
		if (reserva.getHoraFinal().equals(meiaNoite)) {
			throw new IllegalArgumentException("Reservas devem terminar até 23:59 do mesmo dia");
		}
	}

	@Scheduled(fixedRate = 300000) // Executa a cada 5 minutos
	public void atualizarStatusReservas() {
		LocalDateTime agora = LocalDateTime.now();
		List<Reserva> reservas = repository.findAll();

		for (Reserva reserva : reservas) {
			LocalDateTime dataHoraInicial = LocalDateTime.of(reserva.getData(), reserva.getHoraInicial());
			LocalDateTime dataHoraFinal = LocalDateTime.of(reserva.getData(), reserva.getHoraFinal());

			// Marca como UTILIZADO após 2 horas do término
			if (dataHoraFinal.plusHours(2).isBefore(agora) && reserva.getStatus() == StatusReserva.PENDENTE) {
				reserva.setStatus(StatusReserva.UTILIZADO);
				reserva.setUtilizado(true);
				repository.save(reserva);
			}
		}
	}

	public void confirmarUtilizacao(Long id) {
		Reserva reserva = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada com id: " + id));
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

	public void cancelarReserva(Long id) {
		Reserva reserva = repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada com id: " + id));

		// Verificar se pode cancelar (até 1h antes)
		LocalDateTime agora = LocalDateTime.now();
		LocalDateTime dataHoraInicial = LocalDateTime.of(reserva.getData(), reserva.getHoraInicial());

		if (dataHoraInicial.minusHours(1).isBefore(agora)) {
			throw new IllegalStateException("Não é possível cancelar reservas com menos de 1 hora de antecedência");
		}

		// Marcar como cancelada em vez de excluir
		reserva.setStatus(StatusReserva.CANCELADO);
		repository.save(reserva);
	}

	private void validarConflitos(Reserva reserva) {
	    List<Reserva> conflitantes = repository.findReservasConflitantes(
	        reserva.getEspacoAcademico().getId(),
	        reserva.getData(),
	        reserva.getHoraInicial(),
	        reserva.getHoraFinal()
	    );
	    
	    // Remove a própria reserva da lista de conflitos (caso seja uma atualização)
	    conflitantes = conflitantes.stream()
	        .filter(r -> !r.getId().equals(reserva.getId()))
	        .filter(r -> r.getStatus() != StatusReserva.CANCELADO)
	        .collect(Collectors.toList());
	    
	    if (!conflitantes.isEmpty()) {
	        throw new ReservaConflitanteException("Já existe uma reserva para este espaço neste horário");
	    }
	}
}