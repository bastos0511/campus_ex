package com.technova.campusdesk.user.service;

import com.technova.campusdesk.user.dto.UserResponse;
import com.technova.campusdesk.user.entity.Role;
import com.technova.campusdesk.user.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// User lookups (the ADMIN permission is enforced in the controller).
@Service
public class UserService {

  private final UserRepository users;

  // Spring only injects constructor dependencies.
  public UserService(UserRepository users) {
    this.users = users;
  }

  // Lists technicians by name (for assigning tickets).
  @Transactional(readOnly = true)
  public List<UserResponse> listTechnicians() {
    return users.findByRoleOrderByFullNameAsc(Role.TECHNICIAN)
        .stream().map(UserResponse::from).toList();
  }

  // Lists all users by name.
  @Transactional(readOnly = true)
  public List<UserResponse> listAll() {
    return users.findAllByOrderByFullNameAsc().stream().map(UserResponse::from).toList();
  }
}
