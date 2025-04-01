package com.academico.espacos.service;

import com.academico.espacos.model.Usuario;
import com.academico.espacos.model.Professor;
import com.academico.espacos.repository.UsuarioRepository;
import com.academico.espacos.repository.ProfessorRepository;
import com.academico.espacos.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private ProfessorRepository professorRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }
    
    @Transactional
    public Usuario criarUsuarioProfessor(String username, String password, Long professorId) {
        // Verifica se o username já existe
        if (usuarioRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username já existe");
        }
        
        // Busca o professor pelo ID
        Professor professor = professorRepository.findById(professorId)
            .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado"));
        
        // Verifica se o professor já tem um usuário
        if (usuarioRepository.findByProfessor(professor).isPresent()) {
            throw new IllegalArgumentException("Este professor já possui um usuário");
        }
        
        // Cria o usuário
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(password));
        usuario.setRole("ROLE_PROFESSOR");
        usuario.setProfessor(professor);
        
        return usuarioRepository.save(usuario);
    }
    
    @Transactional
    public void resetarSenha(Long usuarioId, String novaSenha) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        
        usuario.setPassword(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);
    }
    
    @Transactional
    public void excluirUsuario(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        
        // Com a exclusão em cascata configurada no banco, 
        // não precisamos excluir manualmente as reservas
        usuarioRepository.delete(usuario);
    }
}