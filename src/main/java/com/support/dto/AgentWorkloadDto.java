package com.support.dto;

public class AgentWorkloadDto {
    private Long agentId;
    private String agentName;
    private String agentEmail;
    private boolean isActive;
    private long totalAssigned;
    private long activeTickets;
    private long resolvedTickets;
    private long closedTickets;

    public AgentWorkloadDto(Long agentId, String agentName, String agentEmail, boolean isActive,
                            long totalAssigned, long activeTickets, long resolvedTickets, long closedTickets) {
        this.agentId = agentId;
        this.agentName = agentName;
        this.agentEmail = agentEmail;
        this.isActive = isActive;
        this.totalAssigned = totalAssigned;
        this.activeTickets = activeTickets;
        this.resolvedTickets = resolvedTickets;
        this.closedTickets = closedTickets;
    }

    public Long getAgentId() { return agentId; }
    public String getAgentName() { return agentName; }
    public String getAgentEmail() { return agentEmail; }
    public boolean isActive() { return isActive; }
    public long getTotalAssigned() { return totalAssigned; }
    public long getActiveTickets() { return activeTickets; }
    public long getResolvedTickets() { return resolvedTickets; }
    public long getClosedTickets() { return closedTickets; }
}
