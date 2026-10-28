package com.technova.campusdesk.common.dto;

import java.util.List;
import org.springframework.data.domain.Page;

// One page of results plus the data the frontend needs to paginate.
public record PageResponse<T>(
    List<T> items,
    int page,
    int size,
    long totalItems,
    int totalPages) {

  // Converts a Spring Data page into a stable JSON shape.
  public static <T> PageResponse<T> from(Page<T> page) {
    return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
        page.getTotalElements(), page.getTotalPages());
  }
}
