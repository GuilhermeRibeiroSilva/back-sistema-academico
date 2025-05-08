# Sistema de Controle de Espaços Acadêmicos (Back-end)

Este repositório contém a API REST que serve como back-end para o sistema de reserva de espaços acadêmicos TechBytes.

## O que este repositório faz?

Este back-end é responsável por:

- **Gerenciar dados**: Persistência e recuperação de informações sobre espaços, professores, reservas e usuários no banco de dados
- **Controlar autenticação**: Implementar sistema de login seguro com JWT
- **Implementar regras de negócio**: Validar disponibilidade de espaços, gerenciar conflitos de horário e controlar estados das reservas
- **Fornecer APIs**: Disponibilizar endpoints REST para todas as operações do sistema
- **Garantir segurança**: Controlar acesso às funcionalidades por perfis de usuário

## Tecnologias Principais

- Java 17
- Spring Boot
- Spring Security
- Spring Data JPA
- PostgreSQL

## Como configurar

1. Clone o repositório
2. Configure o banco de dados em `application.properties`
3. Execute a aplicação com `mvnw spring-boot:run`
