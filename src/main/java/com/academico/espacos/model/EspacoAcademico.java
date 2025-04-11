package com.academico.espacos.model;

import java.util.Objects;
import jakarta.persistence.*;

@Entity
@Table(name = "espacos_academicos")
public class EspacoAcademico {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String sigla;
    
    @Column(nullable = false)
    private String nome;
    
    private String descricao;
    
    @Column(nullable = false)
    private Integer capacidadeAlunos;
    
    @Column(nullable = false)
    private boolean disponivel = true;

    // Construtores
    public EspacoAcademico() {
        // Construtor padrão necessário para JPA
    }
    
    public EspacoAcademico(String sigla, String nome, Integer capacidadeAlunos) {
        this.sigla = sigla;
        this.nome = nome;
        this.capacidadeAlunos = capacidadeAlunos;
    }
    
    public EspacoAcademico(String sigla, String nome, String descricao, Integer capacidadeAlunos, boolean disponivel) {
        this.sigla = sigla;
        this.nome = nome;
        this.descricao = descricao;
        this.capacidadeAlunos = capacidadeAlunos;
        this.disponivel = disponivel;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSigla() {
        return sigla;
    }

    public void setSigla(String sigla) {
        if (sigla == null || sigla.trim().isEmpty()) {
            throw new IllegalArgumentException("A sigla não pode ser nula ou vazia");
        }
        this.sigla = sigla;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome não pode ser nulo ou vazio");
        }
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Integer getCapacidadeAlunos() {
        return capacidadeAlunos;
    }

    public void setCapacidadeAlunos(Integer capacidadeAlunos) {
        if (capacidadeAlunos == null || capacidadeAlunos < 0) {
            throw new IllegalArgumentException("A capacidade de alunos deve ser um valor não negativo");
        }
        this.capacidadeAlunos = capacidadeAlunos;
    }

    public boolean isDisponivel() {
        return disponivel;
    }

    public void setDisponivel(boolean disponivel) {
        this.disponivel = disponivel;
    }
    
    @Override
    public String toString() {
        return "EspacoAcademico{" +
                "id=" + id +
                ", sigla='" + sigla + '\'' +
                ", nome='" + nome + '\'' +
                ", capacidadeAlunos=" + capacidadeAlunos +
                ", disponivel=" + disponivel +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EspacoAcademico that = (EspacoAcademico) o;
        return Objects.equals(id, that.id) || Objects.equals(sigla, that.sigla);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, sigla);
    }
}