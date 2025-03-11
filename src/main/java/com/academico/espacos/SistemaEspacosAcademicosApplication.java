package com.academico.espacos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SistemaEspacosAcademicosApplication {
    public static void main(String[] args) {
        SpringApplication.run(SistemaEspacosAcademicosApplication.class, args);
    }
}