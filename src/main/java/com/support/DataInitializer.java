package com.support;

import com.support.domain.*;
import com.support.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired private SLARepository slaRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AgentRepository agentRepository;
    @Autowired private TicketRepository ticketRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (slaRepository.count() > 0) {
            log.info("Database already seeded, skipping initialization.");
            return;
        }

        log.info("Seeding database with initial data...");

        // --- SLA ---
        SLA slaCritical = slaRepository.save(new SLA("Critical", 1, 4, "Critical issues — 1h response, 4h resolution"));
        SLA slaHigh     = slaRepository.save(new SLA("High",     4, 24, "High priority — 4h response, 24h resolution"));
        SLA slaMedium   = slaRepository.save(new SLA("Medium",   8, 48, "Medium priority — 8h response, 48h resolution"));
        SLA slaLow      = slaRepository.save(new SLA("Low",      24, 72, "Low priority — 24h response, 72h resolution"));

        // --- Categories ---
        Category catInfra    = new Category("Infrastructure",   "Server, network, and hardware issues");
        catInfra.setSla(slaCritical);
        catInfra = categoryRepository.save(catInfra);

        Category catSecurity = new Category("Security",         "Security incidents and vulnerabilities");
        catSecurity.setSla(slaHigh);
        catSecurity = categoryRepository.save(catSecurity);

        Category catSoftware = new Category("Software",         "Application bugs and feature requests");
        catSoftware.setSla(slaMedium);
        catSoftware = categoryRepository.save(catSoftware);

        Category catGeneral  = new Category("General Support",  "General inquiries and assistance");
        catGeneral.setSla(slaLow);
        catGeneral = categoryRepository.save(catGeneral);

        // --- Users ---
        User alice = userRepository.save(new User("Alice Johnson", "alice@example.com"));
        User bob   = userRepository.save(new User("Bob Smith",     "bob@example.com"));
        User carol = userRepository.save(new User("Carol White",   "carol@example.com"));

        // --- Agents ---
        Agent agentMax  = agentRepository.save(new Agent("Max Ivanov",    "max.ivanov@support.com"));
        Agent agentAnna = agentRepository.save(new Agent("Anna Petrova",  "anna.petrova@support.com"));
        Agent agentPete = agentRepository.save(new Agent("Pete Sidorov",  "pete.sidorov@support.com"));

        // --- Tickets ---
        Ticket t1 = new Ticket("Server is down", "Production server is not responding.", alice, catInfra);
        t1.setAssignedAgent(agentMax);
        t1.setStatus(TicketStatus.IN_PROGRESS);
        ticketRepository.save(t1);

        Ticket t2 = new Ticket("Suspected data breach", "Unusual login activity detected from unknown IP.", bob, catSecurity);
        t2.setAssignedAgent(agentAnna);
        t2.setStatus(TicketStatus.OPEN);
        ticketRepository.save(t2);

        Ticket t3 = new Ticket("Login page crash", "App crashes when entering special characters in password field.", carol, catSoftware);
        t3.setAssignedAgent(agentPete);
        t3.setStatus(TicketStatus.IN_PROGRESS);
        ticketRepository.save(t3);

        Ticket t4 = new Ticket("How to reset password?", "Cannot find the password reset option in the portal.", alice, catGeneral);
        t4.setStatus(TicketStatus.OPEN);
        ticketRepository.save(t4);

        Ticket t5 = new Ticket("SSL certificate expired", "HTTPS stopped working after certificate expiry.", bob, catInfra);
        t5.setAssignedAgent(agentMax);
        t5.setStatus(TicketStatus.RESOLVED);
        ticketRepository.save(t5);

        Ticket t6 = new Ticket("Slow database queries", "Reports take over 2 minutes to load.", carol, catSoftware);
        t6.setAssignedAgent(agentAnna);
        t6.setStatus(TicketStatus.ON_HOLD);
        ticketRepository.save(t6);

        Ticket t7 = new Ticket("Phishing email reported", "Employee received suspicious email with attachment.", alice, catSecurity);
        t7.setStatus(TicketStatus.OPEN);
        ticketRepository.save(t7);

        Ticket t8 = new Ticket("Cannot export to PDF", "Export button does nothing in Chrome.", bob, catSoftware);
        t8.setStatus(TicketStatus.CANCELLED);
        ticketRepository.save(t8);

        log.info("Database seeded: {} SLAs, {} categories, {} users, {} agents, {} tickets.",
                slaRepository.count(), categoryRepository.count(),
                userRepository.count(), agentRepository.count(), ticketRepository.count());
    }
}
