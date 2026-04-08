package ru.stockmann.replenishment.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.stockmann.replenishment.models.StoreTurnover;

@Repository
public interface StoreTurnoverRepository extends JpaRepository<StoreTurnover, Long> {
}