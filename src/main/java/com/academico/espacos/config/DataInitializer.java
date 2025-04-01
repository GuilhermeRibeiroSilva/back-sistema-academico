package com.academico.espacos.config;

import com.academico.espacos.model.Usuario;
import com.academico.espacos.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.annotation.PostConstruct;

@Configuration
public class DataInitializer {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            try {
                // Verifica se já existe um usuário admin
                if (usuarioRepository.findByUsername("admin@admin.com").isEmpty()) {
                    // Cria o usuário admin
                    Usuario admin = new Usuario();
                    admin.setUsername("admin@admin.com");
                    admin.setPassword(passwordEncoder.encode("admin123"));
                    admin.setRole("ROLE_ADMIN");
                    
                    usuarioRepository.save(admin);
                    
                    System.out.println("Usuário admin criado com sucesso!");
                    System.out.println("Username: admin@admin.com");
                    System.out.println("Senha: admin123");
                } else {
                    System.out.println("Usuário admin já existe, pulando criação.");
                }
            } catch (Exception e) {
                System.err.println("Erro ao inicializar dados: " + e.getMessage());
                // Se quiser que a aplicação falhe ao iniciar em caso de erro:
                // throw e;
            }
        };
    }

    @PostConstruct
    public void verificarBancoDeDados() {
        // Verifica se a coluna status existe e está correta
        try {
            Query query = entityManager.createNativeQuery(
                "SELECT column_name, data_type FROM information_schema.columns " +
                "WHERE table_name = 'reservas' AND column_name = 'status'"
            );
            
            if (query.getResultList().isEmpty()) {
                throw new RuntimeException("A coluna 'status' não existe na tabela 'reservas'");
            }
            
            // Verifica se existem registros com status inválido
            Query invalidStatusQuery = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM reservas WHERE status NOT IN " +
                "('PENDENTE', 'EM_USO', 'UTILIZADO', 'CANCELADO')"
            );
            
            Long invalidCount = ((Number) invalidStatusQuery.getSingleResult()).longValue();
            if (invalidCount > 0) {
                // Corrige registros com status inválido
                entityManager.createNativeQuery(
                    "UPDATE reservas SET status = 'PENDENTE' WHERE status NOT IN " +
                    "('PENDENTE', 'EM_USO', 'UTILIZADO', 'CANCELADO')"
                ).executeUpdate();
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Erro ao verificar estrutura do banco de dados", e);
        }
    }
}