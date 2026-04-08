package ru.stockmann.replenishment.storeturnover.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.stockmann.replenishment.storeturnover.models.StoreTurnover;

@Repository
public interface StoreTurnoverRepository extends JpaRepository<StoreTurnover, Long> {
}