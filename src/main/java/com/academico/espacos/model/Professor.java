package com.academico.espacos.model;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * Entidade que representa um professor no sistema acadêmico.
 */
@Entity
@Table(name = "professores")
public class Professor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String nome;
    
    @Column(nullable = false)
    private String escola;

    /**
     * Construtor padrão necessário para JPA.
     */
    public Professor() {
    }
    
    /**
     * Construtor com parâmetros.
     * 
     * @param nome Nome do professor
     * @param escola Escola à qual o professor está vinculado
     */
    public Professor(String nome, String escola) {
        this.nome = nome;
        this.escola = escola;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEscola() {
        return escola;
    }

    public void setEscola(String escola) {
        this.escola = escola;
    }
    
    /**
     * Retorna uma representação em string do professor.
     */
    @Override
    public String toString() {
        return "Professor{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", escola='" + escola + '\'' +
                '}';
    }
    
    /**
     * Verifica se este professor é igual a outro objeto.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Professor professor = (Professor) o;
        return Objects.equals(id, professor.id);
    }
    
    /**
     * Retorna o código hash deste professor.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}