package com.technova.campusdesk.attachment.entity;

import com.technova.campusdesk.ticket.entity.Ticket;
import com.technova.campusdesk.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "ticket_attachments")
public class TicketAttachment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "nombre_archivo", nullable = false, length = 255)
  private String nombreArchivo;

  @Column(name = "url_o_referencia", nullable = false, length = 2000)
  private String urlOReferencia;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_contenido", nullable = false, length = 20)
  private TipoContenido tipoContenido;

  @Column(name = "fecha_creacion", nullable = false, updatable = false)
  private Instant fechaCreacion;

  @ManyToOne(optional = false)
  @JoinColumn(name = "ticket_id", nullable = false, updatable = false)
  private Ticket ticket;

  @ManyToOne(optional = false)
  @JoinColumn(name = "subido_por_id", nullable = false, updatable = false)
  private User subidoPor;

  protected TicketAttachment() {}

  public TicketAttachment(Ticket ticket, User subidoPor, String nombreArchivo,
      String urlOReferencia, TipoContenido tipoContenido) {
    this.ticket = ticket;
    this.subidoPor = subidoPor;
    this.nombreArchivo = nombreArchivo;
    this.urlOReferencia = urlOReferencia;
    this.tipoContenido = tipoContenido;
  }

  @PrePersist
  void onCreate() {
    fechaCreacion = Instant.now();
  }

  public Long getId() { return id; }
  public String getNombreArchivo() { return nombreArchivo; }
  public String getUrlOReferencia() { return urlOReferencia; }
  public TipoContenido getTipoContenido() { return tipoContenido; }
  public Instant getFechaCreacion() { return fechaCreacion; }
  public Ticket getTicket() { return ticket; }
  public User getSubidoPor() { return subidoPor; }
}
