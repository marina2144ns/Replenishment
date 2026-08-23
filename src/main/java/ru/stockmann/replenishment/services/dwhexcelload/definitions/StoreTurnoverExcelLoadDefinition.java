package ru.stockmann.replenishment.services.dwhexcelload.definitions;

import org.springframework.stereotype.Component;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelColumnSpec;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelColumns;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadDefinition;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadType;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelNullHandling;

import java.util.List;

@Component
public class StoreTurnoverExcelLoadDefinition implements DWHExcelLoadDefinition {
    private static final int RAW_LENGTH = 4000;

    @Override public DWHExcelLoadType loadType() { return DWHExcelLoadType.STORE_TURNOVER; }
    @Override public String rawTableName() { return "dbo.StoreTurnover_raw"; }
    @Override public String targetTableName() { return "dbo.StoreTurnover"; }
    @Override public String processProcedureName() { throw new UnsupportedOperationException("Java processing only"); }
    @Override public int expectedColumnCount() { return 11; }
    @Override public int batchSize() { return 10_000; }

    @Override
    public List<DWHExcelColumnSpec> columns() {
        return List.of(
                text(0, "SKUItem", "sku", true),
                date(1, "MonthYear", "period"),
                text(2, "StoreRus_BPO", "storeRus", false),
                metric(3, "СуммаОстатковНаКаждуюДатуВыбранногоПериода", "remainingSum"),
                metric(4, "Кол_воДнейСОстатками_0", "remainingDays"),
                metric(5, "SalesQuantity", "salesQuantity"),
                metric(6, "Sales", "sales"),
                metric(7, "ASP", "asp"),
                metric(8, "Revenue", "revenue"),
                metric(9, "GP", "gp"),
                metric(10, "DiscountTotal", "discountTotal")
        );
    }

    private static DWHExcelColumnSpec text(int index, String header, String name, boolean required) {
        return DWHExcelColumns.text(index, header, name, name, RAW_LENGTH, required,
                DWHExcelNullHandling.KEEP_NULL);
    }

    private static DWHExcelColumnSpec date(int index, String header, String name) {
        return DWHExcelColumns.date(index, header, name, name, RAW_LENGTH, true,
                DWHExcelNullHandling.KEEP_NULL);
    }

    private static DWHExcelColumnSpec metric(int index, String header, String name) {
        return DWHExcelColumns.intNumber(index, header, name, name, RAW_LENGTH, false,
                DWHExcelNullHandling.ZERO);
    }
}
