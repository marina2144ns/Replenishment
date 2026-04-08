package ru.stockmann.replenishment.storeturnover.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.stockmann.replenishment.storeturnover.models.StoreTurnover;
import ru.stockmann.replenishment.storeturnover.repositories.StoreTurnoverRepository;

@Service
public class StoreTurnoverService {

    @Autowired
    private StoreTurnoverRepository storeTurnoverRepository;

    @Transactional
    public void save(StoreTurnover storeTurnover){
        storeTurnoverRepository.save(storeTurnover);
    }
}
