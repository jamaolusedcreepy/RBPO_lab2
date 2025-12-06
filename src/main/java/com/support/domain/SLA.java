package com.support.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "slas")
public class SLA {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Column(unique = true, nullable = false)
    private String name;

    @NotNull(message = "Response time is required")
    @Column(name = "response_time_hours")
    private Integer responseTimeHours;

    @NotNull(message = "Resolution time is required")
    @Column(name = "resolution_time_hours")
    private Integer resolutionTimeHours;

    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "sla")
    private Category category;

    // Конструкторы
    public SLA() {
        this.createdAt = LocalDateTime.now();
    }

    public SLA(String name, Integer responseTimeHours, Integer resolutionTimeHours, String description) {
        this();
        this.name = name;
        this.responseTimeHours = responseTimeHours;
        this.resolutionTimeHours = resolutionTimeHours;
        this.description = description;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getResponseTimeHours() { return responseTimeHours; }
    public void setResponseTimeHours(Integer responseTimeHours) { this.responseTimeHours = responseTimeHours; }

    public Integer getResolutionTimeHours() { return resolutionTimeHours; }
    public void setResolutionTimeHours(Integer resolutionTimeHours) { this.resolutionTimeHours = resolutionTimeHours; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    // Методы для расчета дедлайнов
    public LocalDateTime calculateResponseDeadline(LocalDateTime from) {
        return from.plus(responseTimeHours, ChronoUnit.HOURS);
    }

    public LocalDateTime calculateResolutionDeadline(LocalDateTime from) {
        return from.plus(resolutionTimeHours, ChronoUnit.HOURS);
    }
}