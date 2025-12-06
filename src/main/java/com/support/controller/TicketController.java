package com.support.controller;

import com.support.domain.*;
import com.support.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    // Создание тикета
    @PostMapping
    public ResponseEntity<?> createTicket(@Valid @RequestBody Ticket ticket) {
        // Проверяем существование пользователя
        if (!userRepository.existsById(ticket.getUser().getId())) {
            return ResponseEntity.badRequest()
                    .body("Error: User not found!");
        }

        // Проверяем существование категории
        if (!categoryRepository.existsById(ticket.getCategory().getId())) {
            return ResponseEntity.badRequest()
                    .body("Error: Category not found!");
        }

        // Загружаем полные объекты
        User user = userRepository.findById(ticket.getUser().getId()).get();
        Category category = categoryRepository.findById(ticket.getCategory().getId()).get();

        ticket.setUser(user);
        ticket.setCategory(category);

        Ticket savedTicket = ticketRepository.save(ticket);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedTicket);
    }

    // Получение всех тикетов
    @GetMapping
    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    // Получение тикета по ID
    @GetMapping("/{id}")
    public ResponseEntity<Ticket> getTicketById(@PathVariable Long id) {
        Optional<Ticket> ticket = ticketRepository.findById(id);
        return ticket.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Получение тикетов пользователя
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Ticket>> getTicketsByUser(@PathVariable Long userId) {
        if (!userRepository.existsById(userId)) {
            return ResponseEntity.notFound().build();
        }
        List<Ticket> tickets = ticketRepository.findByUserId(userId);
        return ResponseEntity.ok(tickets);
    }

    // Получение тикетов агента
    @GetMapping("/agent/{agentId}")
    public ResponseEntity<List<Ticket>> getTicketsByAgent(@PathVariable Long agentId) {
        if (!agentRepository.existsById(agentId)) {
            return ResponseEntity.notFound().build();
        }
        List<Ticket> tickets = ticketRepository.findByAssignedAgentId(agentId);
        return ResponseEntity.ok(tickets);
    }

    // Получение просроченных тикетов
    @GetMapping("/overdue")
    public List<Ticket> getOverdueTickets() {
        return ticketRepository.findOverdueTickets(LocalDateTime.now());
    }

    // Обновление тикета
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTicket(@PathVariable Long id, @Valid @RequestBody Ticket ticketDetails) {
        return ticketRepository.findById(id)
                .map(ticket -> {
                    ticket.setTitle(ticketDetails.getTitle());
                    ticket.setDescription(ticketDetails.getDescription());

                    // Обновление категории если изменилась
                    if (!ticket.getCategory().getId().equals(ticketDetails.getCategory().getId())) {
                        Category newCategory = categoryRepository.findById(ticketDetails.getCategory().getId())
                                .orElse(ticket.getCategory());
                        ticket.setCategory(newCategory);
                    }

                    Ticket updatedTicket = ticketRepository.save(ticket);
                    return ResponseEntity.ok(updatedTicket);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Назначение агента тикету
    @PutMapping("/{id}/assign")
    public ResponseEntity<?> assignAgent(@PathVariable Long id, @RequestParam Long agentId) {
        return ticketRepository.findById(id)
                .map(ticket -> {
                    Optional<Agent> agent = agentRepository.findById(agentId);
                    if (agent.isEmpty()) {
                        return ResponseEntity.badRequest().body("Error: Agent not found!");
                    }
                    ticket.setAssignedAgent(agent.get());
                    Ticket updatedTicket = ticketRepository.save(ticket);
                    return ResponseEntity.ok(updatedTicket);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Изменение статуса тикета
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam TicketStatus newStatus) {
        return ticketRepository.findById(id)
                .map(ticket -> {
                    if (!ticket.canTransitionTo(newStatus)) {
                        return ResponseEntity.badRequest()
                                .body("Error: Invalid status transition from " + ticket.getStatus() + " to " + newStatus);
                    }
                    ticket.setStatus(newStatus);
                    Ticket updatedTicket = ticketRepository.save(ticket);
                    return ResponseEntity.ok(updatedTicket);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Закрытие тикета с решением
    @PutMapping("/{id}/close")
    public ResponseEntity<?> closeTicket(@PathVariable Long id, @RequestParam String solution) {
        return ticketRepository.findById(id)
                .map(ticket -> {
                    if (ticket.close(solution)) {
                        Ticket updatedTicket = ticketRepository.save(ticket);
                        return ResponseEntity.ok(updatedTicket);
                    } else {
                        return ResponseEntity.badRequest()
                                .body("Error: Cannot close ticket. Ticket must be in RESOLVED status and solution must be provided.");
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Удаление тикета
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTicket(@PathVariable Long id) {
        return ticketRepository.findById(id)
                .map(ticket -> {
                    ticketRepository.delete(ticket);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}