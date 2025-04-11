package com.academico.espacos.config;

import com.academico.espacos.model.Usuario;
import com.academico.espacos.repository.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Configuration
public class DataInitializer {

    private static final Logger logger = Logger.getLogger(DataInitializer.class.getName());
    private static final String ADMIN_USERNAME = "admin@admin.com";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String[] STATUS_VALIDOS = {"PENDENTE", "EM_USO", "UTILIZADO", "CANCELADO"};
    
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @Bean
    public CommandLineRunner initData() {
        return args -> criarUsuarioAdminSeNecessario();
    }

    private void criarUsuarioAdminSeNecessario() {
        try {
            if (usuarioRepository.findByUsername(ADMIN_USERNAME).isEmpty()) {
                Usuario admin = new Usuario();
                admin.setUsername(ADMIN_USERNAME);
                admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
                admin.setRole("ROLE_ADMIN");
                
                usuarioRepository.save(admin);
                
                logger.info("Usuário admin criado com sucesso!");
                logger.info("Username: " + ADMIN_USERNAME);
                logger.info("Senha: " + ADMIN_PASSWORD);
            } else {
                logger.info("Usuário admin já existe, pulando criação.");
            }
        } catch (Exception e) {
            logError("Erro ao inicializar dados", e);
        }
    }

    @PostConstruct
    @Transactional
    public void verificarBancoDeDados() {
        try {
            final String tabelaReservas = "reservas";
            final String colunaStatus = "status";
            
            if (!verificarSeTabelaExiste(tabelaReservas)) {
                logger.info("Tabela 'reservas' ainda não foi criada. Ignorando verificações.");
                return;
            }
            
            if (!verificarSeColunaExiste(tabelaReservas, colunaStatus)) {
                logger.info("AVISO: A coluna 'status' não existe na tabela 'reservas'. Ignorando verificações adicionais.");
                return;
            }
            
            corrigirStatusInvalidos(tabelaReservas);
            
        } catch (Exception e) {
            logError("AVISO: Erro ao verificar estrutura do banco de dados", e);
        }
    }
    
    private boolean verificarSeTabelaExiste(String tabela) {
        try {
            Query query = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = :tabela");
            query.setParameter("tabela", tabela);
            
            Number count = (Number) query.getSingleResult();
            return count.intValue() > 0;
        } catch (Exception e) {
            logError("Erro ao verificar existência da tabela", e);
            return false;
        }
    }
    
    private boolean verificarSeColunaExiste(String tabela, String coluna) {
        try {
            Query query = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM information_schema.columns " +
                "WHERE table_name = :tabela AND column_name = :coluna");
            query.setParameter("tabela", tabela);
            query.setParameter("coluna", coluna);
            
            Number count = (Number) query.getSingleResult();
            return count.intValue() > 0;
        } catch (Exception e) {
            logError("Erro ao verificar existência da coluna", e);
            return false;
        }
    }
    
    @Transactional
    private void corrigirStatusInvalidos(String tabela) {
        try {
            String statusValidos = String.join("', '", STATUS_VALIDOS);
            
            Query invalidStatusQuery = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM " + tabela + " WHERE status IS NOT NULL AND " +
                "status NOT IN ('" + statusValidos + "')");
            
            Long invalidCount = ((Number) invalidStatusQuery.getSingleResult()).longValue();
            
            if (invalidCount > 0) {
                logger.info("Encontrados " + invalidCount + " registros com status inválido. Corrigindo para 'PENDENTE'...");
                
                int updated = entityManager.createNativeQuery(
                    "UPDATE " + tabela + " SET status = 'PENDENTE' WHERE status IS NOT NULL AND " +
                    "status NOT IN ('" + statusValidos + "')").executeUpdate();
                
                logger.info(updated + " registros atualizados com sucesso.");
            } else {
                logger.info("Nenhum registro com status inválido encontrado.");
            }
        } catch (Exception e) {
            logError("Erro ao corrigir status inválidos", e);
        }
    }
    
    private void logError(String mensagem, Exception e) {
        logger.log(Level.SEVERE, mensagem + ": " + e.getMessage(), e);
    }
}