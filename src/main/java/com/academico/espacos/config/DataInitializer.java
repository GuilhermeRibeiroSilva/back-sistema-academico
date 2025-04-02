package com.academico.espacos.config;

import com.academico.espacos.model.Usuario;
import com.academico.espacos.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
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
                e.printStackTrace();
            }
        };
    }

    @PostConstruct
    @Transactional // Adicionado para corrigir o erro
    public void verificarBancoDeDados() {
        try {
            // Primeiro verifica se a tabela existe
            boolean tabelaExiste = verificarSeTabelaExiste("reservas");
            if (!tabelaExiste) {
                System.out.println("Tabela 'reservas' ainda não foi criada. Ignorando verificações.");
                return;
            }
            
            // Verifica se a coluna status existe
            Query query = entityManager.createNativeQuery(
                "SELECT column_name, data_type FROM information_schema.columns " +
                "WHERE table_name = 'reservas' AND column_name = 'status'"
            );
            
            if (query.getResultList().isEmpty()) {
                System.out.println("AVISO: A coluna 'status' não existe na tabela 'reservas'. Ignorando verificações adicionais.");
                return;
            }
            
            // Verifica se existem registros com status inválido
            Query invalidStatusQuery = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM reservas WHERE status IS NOT NULL AND status NOT IN " +
                "('PENDENTE', 'EM_USO', 'UTILIZADO', 'CANCELADO')"
            );
            
            Long invalidCount = ((Number) invalidStatusQuery.getSingleResult()).longValue();
            if (invalidCount > 0) {
                System.out.println("Encontrados " + invalidCount + " registros com status inválido. Corrigindo para 'PENDENTE'...");
                // Corrige registros com status inválido
                int updated = entityManager.createNativeQuery(
                    "UPDATE reservas SET status = 'PENDENTE' WHERE status IS NOT NULL AND status NOT IN " +
                    "('PENDENTE', 'EM_USO', 'UTILIZADO', 'CANCELADO')"
                ).executeUpdate();
                
                System.out.println(updated + " registros atualizados com sucesso.");
            } else {
                System.out.println("Nenhum registro com status inválido encontrado.");
            }
            
        } catch (Exception e) {
            System.err.println("AVISO: Erro ao verificar estrutura do banco de dados: " + e.getMessage());
            e.printStackTrace();
            // Não lançamos a exceção para permitir que a aplicação continue iniciando
        }
    }
    
    // Método auxiliar para verificar se uma tabela existe
    private boolean verificarSeTabelaExiste(String tabela) {
        try {
            Query query = entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_name = :tabela"
            );
            query.setParameter("tabela", tabela);
            
            Number count = (Number) query.getSingleResult();
            return count.intValue() > 0;
        } catch (Exception e) {
            System.err.println("Erro ao verificar existência da tabela: " + e.getMessage());
            return false;
        }
    }
}