package ru.stockmann.replenishment.services.dwhexcelload.delete;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHDeletionSession;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHDeletionSessionRepository;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelColumnSpec;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadDefinition;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DWHUniversalDeletionService {

    private static final DateTimeFormatter ISO_DATE =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

    private final DataSource dataSource;
    private final DWHDatasetRegistry registry;
    private final DWHUniversalDeletionRepository repository;
    private final DWHDeletionSessionRepository sessionRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public DWHUniversalDeletionService(
            DataSource dataSource,
            DWHDatasetRegistry registry,
            DWHUniversalDeletionRepository repository,
            ObjectMapper objectMapper
    ) {
        this(dataSource, registry, repository,
                new DWHDeletionSessionRepository(dataSource), objectMapper);
    }

    DWHUniversalDeletionService(
            DataSource dataSource,
            DWHDatasetRegistry registry,
            DWHUniversalDeletionRepository repository,
            DWHDeletionSessionRepository sessionRepository,
            ObjectMapper objectMapper
    ) {
        this.dataSource = dataSource;
        this.registry = registry;
        this.repository = repository;
        this.sessionRepository = sessionRepository;
        this.objectMapper = objectMapper;
    }

    public DWHDatasetsResponse datasets() {
        return new DWHDatasetsResponse(registry.definitions().stream()
                .map(definition -> new DWHDatasetSummary(
                        definition.loadCode(), definition.serviceName()
                ))
                .toList());
    }

    public DWHDatasetFieldsResponse fields(String dataset) {
        DWHExcelLoadDefinition definition = registry.require(dataset);
        return new DWHDatasetFieldsResponse(
                definition.loadCode(),
                definition.columns().stream()
                        .map(column -> new DWHDatasetField(
                                column.targetColumnName(),
                                column.excelColumnName(),
                                column.valueKind()
                        ))
                        .toList()
        );
    }

    public DWHUniversalDeleteResult execute(
            DWHUniversalDeleteRequest request,
            String requestedBy
    ) {
        if (request == null) {
            throw DWHDeleteRequestException.emptyFilter();
        }
        DWHExcelLoadDefinition definition = registry.require(request.dataset());
        List<DWHDeleteFilter> filters = validateFilters(definition, request.filters());

        if (request.isDryRun()) {
            return dryRun(definition, filters);
        }

        String criteriaJson = serializeCriteria(request.filters());
        DWHDeletionSession session = DWHDeletionSession
                .universal(definition.loadType(), criteriaJson)
                .withRequestedBy(requestedBy);
        long sessionId = sessionRepository.create(session);
        return delete(definition, filters, sessionId);
    }

    private List<DWHDeleteFilter> validateFilters(
            DWHExcelLoadDefinition definition,
            Map<String, Object> requestedFilters
    ) {
        if (requestedFilters == null || requestedFilters.isEmpty()) {
            throw DWHDeleteRequestException.emptyFilter();
        }

        Map<String, DWHExcelColumnSpec> availableColumns = definition.columns().stream()
                .collect(Collectors.toMap(
                        DWHExcelColumnSpec::targetColumnName,
                        Function.identity(),
                        (first, second) -> {
                            throw new IllegalStateException(
                                    "Duplicate target column in definition: "
                                            + first.targetColumnName()
                            );
                        },
                        LinkedHashMap::new
                ));

        return requestedFilters.entrySet().stream()
                .map(entry -> {
                    DWHExcelColumnSpec column = availableColumns.get(entry.getKey());
                    if (column == null) {
                        throw DWHDeleteRequestException.invalidField(
                                entry.getKey(), definition.loadCode()
                        );
                    }
                    return new DWHDeleteFilter(
                            column,
                            convertValue(column, entry.getValue())
                    );
                })
                .toList();
    }

    private Object convertValue(DWHExcelColumnSpec column, Object value) {
        if (value == null) {
            return null;
        }
        String field = column.targetColumnName();
        return switch (column.valueKind()) {
            case TEXT -> {
                if (!(value instanceof String text)) {
                    throw invalidValue(field, column.valueKind().name(), "must be a JSON string");
                }
                yield text;
            }
            case INT -> toInteger(value, field);
            case BIGINT -> toLong(value, field);
            case DECIMAL -> toDecimal(value, field);
            case DATE -> toDate(value, field);
        };
    }

    private Integer toInteger(Object value, String field) {
        BigInteger integer = integralValue(value, field, "INT");
        try {
            return integer.intValueExact();
        } catch (ArithmeticException exception) {
            throw invalidValue(field, "INT", "value is outside the 32-bit integer range");
        }
    }

    private Long toLong(Object value, String field) {
        BigInteger integer = integralValue(value, field, "BIGINT");
        try {
            return integer.longValueExact();
        } catch (ArithmeticException exception) {
            throw invalidValue(field, "BIGINT", "value is outside the 64-bit integer range");
        }
    }

    private BigInteger integralValue(Object value, String field, String expectedType) {
        if (!(value instanceof Number number)) {
            throw invalidValue(field, expectedType, "must be a JSON integer");
        }
        try {
            return new BigDecimal(number.toString()).toBigIntegerExact();
        } catch (NumberFormatException | ArithmeticException exception) {
            throw invalidValue(field, expectedType, "must be a JSON integer");
        }
    }

    private BigDecimal toDecimal(Object value, String field) {
        if (!(value instanceof Number number)) {
            throw invalidValue(field, "DECIMAL", "must be a JSON number");
        }
        try {
            return new BigDecimal(number.toString());
        } catch (NumberFormatException exception) {
            throw invalidValue(field, "DECIMAL", "must be a finite JSON number");
        }
    }

    private LocalDate toDate(Object value, String field) {
        if (!(value instanceof String text)) {
            throw invalidValue(field, "DATE", "must be a string in yyyy-MM-dd format");
        }
        try {
            return LocalDate.parse(text, ISO_DATE);
        } catch (DateTimeParseException exception) {
            throw invalidValue(field, "DATE", "must be a valid date in yyyy-MM-dd format");
        }
    }

    private DWHDeleteRequestException invalidValue(
            String field,
            String expectedType,
            String detail
    ) {
        return DWHDeleteRequestException.invalidValue(field, expectedType, detail);
    }

    private DWHUniversalDeleteResult dryRun(
            DWHExcelLoadDefinition definition,
            List<DWHDeleteFilter> filters
    ) {
        try (Connection connection = dataSource.getConnection()) {
            long matchedRows = repository.count(connection, definition, filters);
            return DWHUniversalDeleteResult.dryRun(definition.loadCode(), matchedRows);
        } catch (SQLException exception) {
            throw new RuntimeException(
                    "Failed to count matching rows for " + definition.loadCode(), exception
            );
        }
    }

    private DWHUniversalDeleteResult delete(
            DWHExcelLoadDefinition definition,
            List<DWHDeleteFilter> filters,
            long sessionId
    ) {
        try (Connection connection = dataSource.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(false);
                long deletedRows = repository.delete(connection, definition, filters);
                sessionRepository.completeSuccess(connection, sessionId, deletedRows);
                connection.commit();
                return DWHUniversalDeleteResult.deleted(
                        sessionId, definition.loadCode(), deletedRows
                );
            } catch (RuntimeException | SQLException exception) {
                rollbackQuietly(connection);
                RuntimeException failure = exception instanceof RuntimeException runtime
                        ? runtime
                        : new RuntimeException(
                                "Failed to delete rows for " + definition.loadCode(), exception
                        );
                completeError(sessionId, failure);
                throw failure;
            } finally {
                restoreAutoCommitQuietly(connection, oldAutoCommit);
            }
        } catch (SQLException exception) {
            RuntimeException failure = new RuntimeException(
                    "Failed to open delete transaction for " + definition.loadCode(), exception
            );
            completeError(sessionId, failure);
            throw failure;
        }
    }

    private String serializeCriteria(Map<String, Object> filters) {
        try {
            return objectMapper.writeValueAsString(filters);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize delete criteria", exception);
        }
    }

    private void completeError(long sessionId, RuntimeException failure) {
        try {
            sessionRepository.completeError(sessionId, failure.getMessage());
        } catch (RuntimeException loggingFailure) {
            failure.addSuppressed(loggingFailure);
        }
    }

    private void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
        }
    }

    private void restoreAutoCommitQuietly(Connection connection, boolean oldAutoCommit) {
        try {
            connection.setAutoCommit(oldAutoCommit);
        } catch (SQLException ignored) {
        }
    }
}
