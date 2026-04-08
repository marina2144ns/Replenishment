package ru.stockmann.replenishment.storeturnover.controllers;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.stockmann.replenishment.storeturnover.models.StoreTurnover;
import ru.stockmann.replenishment.storeturnover.services.StoreTurnoverService;


import java.io.FileReader;
import java.io.IOException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import java.util.List;

@RestController
@RequestMapping("/v1.0/")
public class StoreTurnoverController {
    @Autowired
    private StoreTurnoverService storeTurnoverService;

    @PostMapping("")
    public String loadStoreTurnover(@RequestBody String filePath)
    {
        List<String> errors = new ArrayList<>();

        CSVParser parser = new CSVParserBuilder()
                .withSeparator(';')  // Указываем разделитель
                .build();
        try (CSVReader reader = new CSVReaderBuilder(new FileReader(filePath)) .withCSVParser(parser).build()) {
            String[] line;
            int rowNum = 0;

            // Пропустить заголовок
            reader.readNext();
            while ((line = reader.readNext()) != null) {
                rowNum++;
                try {
                    StoreTurnover turnover = parseRow(line);
                    storeTurnoverService.save(turnover);
                } catch (Exception e) {
                    errors.add("Ошибка в строке " + rowNum + ": " + e.getMessage());
                    break;
                }
            }
            if(!errors.isEmpty()){
                return  errors.toString();
            }
        } catch (IOException | CsvValidationException e) {
            return  "Ошибка чтения файла: " + e.getMessage();
        }
        return "OK";
    }

    private StoreTurnover parseRow(String[] row) throws Exception {
        String fieldName="";
        try {
            StoreTurnover turnover = new StoreTurnover();
            fieldName="Sku";
            turnover.setSku(row[0]);
            fieldName="Period";
            turnover.setPeriod(convertToDate(row[1]));
            fieldName="StoreRus";
            turnover.setStoreRus(row[2]);
            fieldName="RemainingSum";
            turnover.setRemainingSum(Integer.parseInt(row[3]));
            fieldName="RemainingDays";
            turnover.setRemainingDays(Integer.parseInt(row[4]));
            fieldName="SalesQuantity";
            turnover.setSalesQuantity(Integer.parseInt(row[5]));
            fieldName="Sales";
            turnover.setSales(Integer.parseInt(row[6]));
            fieldName="Asp";
            turnover.setAsp(Integer.parseInt(row[7]));
            fieldName="Revenue";
            turnover.setRevenue(Integer.parseInt(row[8]));
            fieldName="Gp";
            turnover.setGp(Integer.parseInt(row[9]));
            fieldName="DiscountTotal";
            turnover.setDiscountTotal(Integer.parseInt(row[10]));
            return turnover;
        } catch (Exception e) {
            throw new Exception("Неверный формат данных в поле : " + fieldName+" " + e.getMessage());
        }
    }
    private LocalDate convertToDate(String period)  {
        // Определяем формат без дня
       // DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM.yyyy");

        // Разбираем строку, добавляя первый день месяца
        return LocalDate.parse("01." + period, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

}
