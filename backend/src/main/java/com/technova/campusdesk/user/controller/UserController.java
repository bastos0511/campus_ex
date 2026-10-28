package com.technova.campusdesk.user.controller;

import com.technova.campusdesk.user.dto.UserResponse;
import com.technova.campusdesk.user.service.UserService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// User lookups. The whole controller is ADMIN only.
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

  private final UserService userService;

  // Spring only injects constructor dependencies.
  public UserController(UserService userService) {
    this.userService = userService;
  }

  // Lists technicians (for assigning tickets).
  @GetMapping("/technicians")
  public List<UserResponse> listTechnicians() {
    return userService.listTechnicians();
  }

  // Lists all users.
  @GetMapping
  public List<UserResponse> listAll() {
    return userService.listAll();
  }
}
