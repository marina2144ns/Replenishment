package ru.stockmann.replenishment.services.dwhexcelload.delete;

import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelColumnSpec;

record DWHDeleteFilter(DWHExcelColumnSpec column, Object value) {
}
