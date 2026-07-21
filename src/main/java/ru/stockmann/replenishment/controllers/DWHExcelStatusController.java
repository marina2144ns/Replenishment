package ru.stockmann.replenishment.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.stockmann.replenishment.services.DWHExcelStatusService;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadSessionNotFoundException;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadStatusResult;

@RestController
@RequestMapping("/dwhexcel/v1.0")
public class DWHExcelStatusController {

    private final DWHExcelStatusService statusService;

    public DWHExcelStatusController(DWHExcelStatusService statusService) {
        this.statusService = statusService;
    }

    @GetMapping("/status/{id}")
    public ResponseEntity<DWHExcelLoadStatusResult> getStatus(@PathVariable Long id) {

        try {
            DWHExcelLoadStatusResult result = statusService.getStatus(id);
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (DWHExcelLoadSessionNotFoundException e) {
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }
}
