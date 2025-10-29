package com.capstone.tele_ticketing_backend_1.repo;

import com.capstone.tele_ticketing_backend_1.entities.ERole;
import com.capstone.tele_ticketing_backend_1.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface RoleRepo extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(ERole name);
}
