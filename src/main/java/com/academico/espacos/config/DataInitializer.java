package com.academico.espacos.config;

import com.academico.espacos.model.Usuario;
import com.academico.espacos.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
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
            }
        };
    }
}