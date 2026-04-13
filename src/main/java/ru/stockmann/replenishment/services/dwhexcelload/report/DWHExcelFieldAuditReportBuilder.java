package ru.stockmann.replenishment.services.dwhexcelload.report;

import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelColumnSpec;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadDefinition;

import java.util.ArrayList;
import java.util.List;

public class DWHExcelFieldAuditReportBuilder {

    public List<DWHExcelFieldAuditRow> build(DWHExcelLoadDefinition definition) {
        List<DWHExcelFieldAuditRow> result = new ArrayList<>();

        for (DWHExcelColumnSpec c : definition.columns()) {
            result.add(new DWHExcelFieldAuditRow(
                    definition.loadCode(),
                    c.excelIndex(),
                    c.excelColumnName(),
                    c.rawColumnName(),
                    c.targetColumnName(),
                    c.valueKind().name(),
                    c.rawMaxLength(),
                    c.required(),
                    c.javaValidationNote(),
                    c.sqlValidationNote()
            ));
        }

        return result;
    }
}