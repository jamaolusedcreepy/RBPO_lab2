package com.support.dto;

import com.support.domain.TicketStatus;
import java.util.Map;

public class CategoryStatsDto {
    private Long categoryId;
    private String categoryName;
    private String slaName;
    private Integer slaResponseHours;
    private Integer slaResolutionHours;
    private long totalTickets;
    private Map<TicketStatus, Long> ticketsByStatus;

    public CategoryStatsDto(Long categoryId, String categoryName,
                            String slaName, Integer slaResponseHours, Integer slaResolutionHours,
                            long totalTickets, Map<TicketStatus, Long> ticketsByStatus) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.slaName = slaName;
        this.slaResponseHours = slaResponseHours;
        this.slaResolutionHours = slaResolutionHours;
        this.totalTickets = totalTickets;
        this.ticketsByStatus = ticketsByStatus;
    }

    public Long getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public String getSlaName() { return slaName; }
    public Integer getSlaResponseHours() { return slaResponseHours; }
    public Integer getSlaResolutionHours() { return slaResolutionHours; }
    public long getTotalTickets() { return totalTickets; }
    public Map<TicketStatus, Long> getTicketsByStatus() { return ticketsByStatus; }
}
