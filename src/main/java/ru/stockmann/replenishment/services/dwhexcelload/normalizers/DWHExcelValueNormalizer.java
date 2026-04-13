package ru.stockmann.replenishment.services.dwhexcelload.normalizers;

@FunctionalInterface
public interface DWHExcelValueNormalizer {
    String normalize(String rawValue);
}