package com.technova.campusdesk.ticket.entity;

import java.time.Duration;

// Ticket urgency level. Each one carries its SLA: max hours to resolve it.
public enum TicketPriority {
  BAJA(120),
  MEDIA(72),
  ALTA(24),
  CRITICA(4);

  private final int slaHours;

  TicketPriority(int slaHours) {
    this.slaHours = slaHours;
  }

  public int getSlaHours() { return slaHours; }

  // Time allowed from creation until the ticket must be resolved.
  public Duration sla() { return Duration.ofHours(slaHours); }
}
