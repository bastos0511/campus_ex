package com.technova.campusdesk.user.repository;

import com.technova.campusdesk.user.entity.Role;
import com.technova.campusdesk.user.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// Access to the users table. Spring writes the code.
public interface UserRepository extends JpaRepository<User, Long> {

  // Finds a user by email (for login).
  Optional<User> findByEmail(String email);

  // Tells whether the email is already registered.
  boolean existsByEmail(String email);

  // Technicians ordered by name (for assigning tickets).
  List<User> findByRoleOrderByFullNameAsc(Role role);

  // All users ordered by name.
  List<User> findAllByOrderByFullNameAsc();
}
