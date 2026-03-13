package com.support.controller;

import com.support.domain.Agent;
import com.support.repository.AgentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    @Autowired
    private AgentRepository agentRepository;

    // Создание агента
    @PostMapping
    public ResponseEntity<?> createAgent(@Valid @RequestBody Agent agent) {
        if (agentRepository.existsByEmail(agent.getEmail())) {
            return ResponseEntity.badRequest()
                    .body("Error: Email is already in use!");
        }
        Agent savedAgent = agentRepository.save(agent);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedAgent);
    }

    // Получение всех агентов
    @GetMapping
    public List<Agent> getAllAgents() {
        return agentRepository.findAll();
    }

    // Получение активных агентов
    @GetMapping("/active")
    public List<Agent> getActiveAgents() {
        return agentRepository.findByIsActiveTrue();
    }

    // Получение агента по ID
    @GetMapping("/{id}")
    public ResponseEntity<Agent> getAgentById(@PathVariable Long id) {
        Optional<Agent> agent = agentRepository.findById(id);
        return agent.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Обновление агента
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAgent(@PathVariable Long id, @Valid @RequestBody Agent agentDetails) {
        return agentRepository.findById(id)
                .map(agent -> {
                    if (!agent.getEmail().equals(agentDetails.getEmail()) &&
                            agentRepository.existsByEmail(agentDetails.getEmail())) {
                        return ResponseEntity.badRequest()
                                .body("Error: Email is already in use!");
                    }
                    agent.setName(agentDetails.getName());
                    agent.setEmail(agentDetails.getEmail());
                    agent.setIsActive(agentDetails.getIsActive());
                    Agent updatedAgent = agentRepository.save(agent);
                    return ResponseEntity.ok(updatedAgent);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Удаление агента
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAgent(@PathVariable Long id) {
        return agentRepository.findById(id)
                .map(agent -> {
                    agentRepository.delete(agent);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}