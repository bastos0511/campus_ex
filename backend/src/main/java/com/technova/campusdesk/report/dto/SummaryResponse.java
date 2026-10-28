package com.technova.campusdesk.report.dto;

// Dashboard counts: total + each status.
public record SummaryResponse(
    long total,
    long abiertas,
    long asignadas,
    long enProceso,
    long resueltas,
    long cerradas) {
}
