package com.technova.campusdesk.ticket.entity;

// Lifecycle: ABIERTA -> ASIGNADA -> EN_PROCESO -> RESUELTA -> CERRADA.
public enum TicketStatus {
  ABIERTA,
  ASIGNADA,
  EN_PROCESO,
  RESUELTA,
  CERRADA
}
