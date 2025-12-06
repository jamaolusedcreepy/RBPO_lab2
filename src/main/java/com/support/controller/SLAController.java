package com.support.controller;

import com.support.domain.SLA;
import com.support.repository.SLARepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/slas")
public class SLAController {

    @Autowired
    private SLARepository slaRepository;

    // Создание SLA
    @PostMapping
    public ResponseEntity<?> createSLA(@Valid @RequestBody SLA sla) {
        if (slaRepository.existsByName(sla.getName())) {
            return ResponseEntity.badRequest()
                    .body("Error: SLA name already exists!");
        }
        SLA savedSLA = slaRepository.save(sla);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedSLA);
    }

    // Получение всех SLA
    @GetMapping
    public List<SLA> getAllSLAs() {
        return slaRepository.findAll();
    }

    // Получение SLA по ID
    @GetMapping("/{id}")
    public ResponseEntity<SLA> getSlaById(@PathVariable Long id) {
        Optional<SLA> sla = slaRepository.findById(id);
        return sla.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Обновление SLA
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSLA(@PathVariable Long id, @Valid @RequestBody SLA slaDetails) {
        return slaRepository.findById(id)
                .map(sla -> {
                    if (!sla.getName().equals(slaDetails.getName()) &&
                            slaRepository.existsByName(slaDetails.getName())) {
                        return ResponseEntity.badRequest()
                                .body("Error: SLA name already exists!");
                    }
                    sla.setName(slaDetails.getName());
                    sla.setResponseTimeHours(slaDetails.getResponseTimeHours());
                    sla.setResolutionTimeHours(slaDetails.getResolutionTimeHours());
                    sla.setDescription(slaDetails.getDescription());
                    SLA updatedSLA = slaRepository.save(sla);
                    return ResponseEntity.ok(updatedSLA);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Удаление SLA
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSLA(@PathVariable Long id) {
        return slaRepository.findById(id)
                .map(sla -> {
                    slaRepository.delete(sla);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}