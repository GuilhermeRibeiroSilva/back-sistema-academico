package com.academico.espacos.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/horarios-disponiveis")
public class HorarioDisponivelController {

    @GetMapping
    public ResponseEntity<List<String>> buscarHorariosDisponiveis(
            @RequestParam Long espacoId,
            @RequestParam String data,
            @RequestParam(required = false) Long reservaId) {
        
        try {
            // Converter data de String para LocalDate
            LocalDate dataLocalDate = LocalDate.parse(data);
            
            // Gerar horários de 7h às 23h com intervalos de 10 minutos
            List<String> horariosDisponiveis = gerarHorariosPadrao();          
            
            return ResponseEntity.ok(horariosDisponiveis);
            
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(List.of("Formato de data inválido. Use o formato yyyy-MM-dd"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(List.of("Erro ao processar a solicitação: " + e.getMessage()));
        }
    }
    
    private List<String> gerarHorariosPadrao() {
        List<String> horarios = new ArrayList<>();
        for (int hora = 7; hora <= 23; hora++) {
            for (int minuto = 0; minuto < 60; minuto += 10) {
                horarios.add(String.format("%02d:%02d:00", hora, minuto));
            }
        }
        return horarios;
    }
}