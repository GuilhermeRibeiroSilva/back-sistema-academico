package com.academico.espacos.security;

import com.academico.espacos.model.Usuario;
import com.academico.espacos.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException(
                "Usuário com nome de usuário '" + username + "' não foi encontrado no sistema"));

        return createUserDetails(usuario);
    }
    
    private UserDetails createUserDetails(Usuario usuario) {
        return new User(
            usuario.getUsername(),
            usuario.getPassword(),
            Collections.singletonList(new SimpleGrantedAuthority(usuario.getRole()))
        );
    }
}