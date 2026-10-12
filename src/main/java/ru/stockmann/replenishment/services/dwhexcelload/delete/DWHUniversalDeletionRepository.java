package ru.stockmann.replenishment.services.dwhexcelload.delete;

import org.springframework.stereotype.Repository;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadDefinition;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelValueKind;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class DWHUniversalDeletionRepository {

    public long delete(
            Connection connection,
            DWHExcelLoadDefinition definition,
            List<DWHDeleteFilter> filters
    ) throws SQLException {
        String sql = "DELETE FROM " + quoteQualifiedIdentifier(definition.targetTableName())
                + " WHERE " + whereClause(filters);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, filters);
            return statement.executeLargeUpdate();
        }
    }

    public long count(
            Connection connection,
            DWHExcelLoadDefinition definition,
            List<DWHDeleteFilter> filters
    ) throws SQLException {
        String sql = "SELECT COUNT_BIG(*) FROM "
                + quoteQualifiedIdentifier(definition.targetTableName())
                + " WHERE " + whereClause(filters);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, filters);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("COUNT_BIG returned no row");
                }
                return resultSet.getLong(1);
            }
        }
    }

    private String whereClause(List<DWHDeleteFilter> filters) {
        return filters.stream()
                .map(filter -> quoteIdentifier(filter.column().targetColumnName())
                        + (filter.value() == null ? " IS NULL" : " = ?"))
                .collect(Collectors.joining(" AND "));
    }

    private void bind(PreparedStatement statement, List<DWHDeleteFilter> filters)
            throws SQLException {
        int parameterIndex = 1;
        for (DWHDeleteFilter filter : filters) {
            if (filter.value() == null) {
                continue;
            }
            setValue(statement, parameterIndex++, filter.column().valueKind(), filter.value());
        }
    }

    private void setValue(
            PreparedStatement statement,
            int index,
            DWHExcelValueKind kind,
            Object value
    ) throws SQLException {
        switch (kind) {
            case TEXT -> statement.setString(index, (String) value);
            case INT -> statement.setInt(index, (Integer) value);
            case BIGINT -> statement.setLong(index, (Long) value);
            case DECIMAL -> statement.setBigDecimal(index, (BigDecimal) value);
            case DATE -> statement.setDate(index, Date.valueOf((LocalDate) value));
        }
    }

    static String quoteQualifiedIdentifier(String identifier) {
        return java.util.Arrays.stream(identifier.split("\\.", -1))
                .map(DWHUniversalDeletionRepository::quoteIdentifier)
                .collect(Collectors.joining("."));
    }

    static String quoteIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("SQL identifier is empty in DWH definition");
        }
        return "[" + identifier.replace("]", "]]") + "]";
    }
}
