package com.technova.campusdesk.ticket.entity;

// Lifecycle: ABIERTA -> ASIGNADA -> EN_PROCESO -> RESUELTA -> CERRADA.
// CANCELADA: the requester gives up on an ABIERTA or ASIGNADA ticket.
// CERRADA and CANCELADA are final.
public enum TicketStatus {
  ABIERTA,
  ASIGNADA,
  EN_PROCESO,
  RESUELTA,
  CERRADA,
  CANCELADA;

  // True for the statuses that accept no further changes or comments.
  public boolean isFinal() {
    return this == CERRADA || this == CANCELADA;
  }
}
