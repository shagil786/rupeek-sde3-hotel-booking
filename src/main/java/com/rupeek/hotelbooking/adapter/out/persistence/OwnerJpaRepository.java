package com.rupeek.hotelbooking.adapter.out.persistence;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface OwnerJpaRepository extends JpaRepository<OwnerEntity, String> { Optional<OwnerEntity> findByUsername(String username); }
