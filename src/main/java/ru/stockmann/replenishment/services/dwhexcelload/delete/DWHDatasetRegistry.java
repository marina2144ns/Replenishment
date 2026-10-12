package ru.stockmann.replenishment.services.dwhexcelload.delete;

import org.springframework.stereotype.Component;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadDefinition;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadType;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class DWHDatasetRegistry {

    private final Map<DWHExcelLoadType, DWHExcelLoadDefinition> definitions;

    public DWHDatasetRegistry(List<DWHExcelLoadDefinition> registeredDefinitions) {
        EnumMap<DWHExcelLoadType, DWHExcelLoadDefinition> indexed =
                new EnumMap<>(DWHExcelLoadType.class);
        for (DWHExcelLoadDefinition definition : registeredDefinitions) {
            DWHExcelLoadDefinition duplicate = indexed.put(definition.loadType(), definition);
            if (duplicate != null) {
                throw new IllegalStateException(
                        "Duplicate DWH definition for " + definition.loadType().code()
                );
            }
        }
        this.definitions = Map.copyOf(indexed);
    }

    public List<DWHExcelLoadDefinition> definitions() {
        return definitions.values().stream()
                .sorted(Comparator.comparingInt(definition -> definition.loadType().ordinal()))
                .toList();
    }

    public DWHExcelLoadDefinition require(String dataset) {
        final DWHExcelLoadType type;
        try {
            type = DWHExcelLoadType.fromCode(dataset);
        } catch (IllegalArgumentException exception) {
            throw DWHDeleteRequestException.invalidDataset(dataset);
        }

        DWHExcelLoadDefinition definition = definitions.get(type);
        if (definition == null) {
            throw DWHDeleteRequestException.invalidDataset(dataset);
        }
        return definition;
    }
}
