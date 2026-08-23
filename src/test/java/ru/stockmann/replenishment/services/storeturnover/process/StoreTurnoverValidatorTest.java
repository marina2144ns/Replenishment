package ru.stockmann.replenishment.services.storeturnover.process;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class StoreTurnoverValidatorTest {
    private final StoreTurnoverValidator validator=new StoreTurnoverValidator();

    @Test void validPeriodAndBlankMetricsMapToTypedValues(){
        StoreTurnoverRowValidationResult result=validator.validateAndMap(row("sku","08.2026","Store",null,""," ","NULL","0","-2","3","4"));
        assertTrue(result.valid());assertEquals(LocalDate.of(2026,8,1),result.stageRow().period());
        assertEquals(List.of(0,0,0,0,0,-2,3,4),List.of(result.stageRow().remainingSum(),result.stageRow().remainingDays(),result.stageRow().salesQuantity(),result.stageRow().sales(),result.stageRow().asp(),result.stageRow().revenue(),result.stageRow().gp(),result.stageRow().discountTotal()));
    }

    @Test void requiredSkuAndPeriodRejectMissingValuesWhileStoreRusRemainsOptional(){
        StoreTurnoverRowValidationResult result=validator.validateAndMap(row("\u00a0"," ","\u202f",null,null,null,null,null,null,null,null));
        assertFalse(result.valid());assertEquals(List.of("sku","period"),result.errors().stream().map(StoreTurnoverValidationError::fieldName).toList());
    }

    @Test void requiredFieldsRejectSupportedNullMarkers(){
        StoreTurnoverRowValidationResult result=validator.validateAndMap(row("N/A","NULL","—",null,null,null,null,null,null,null,null));
        assertFalse(result.valid());assertEquals(List.of("sku","period"),result.errors().stream().map(StoreTurnoverValidationError::fieldName).toList());
    }

    @Test void optionalStoreRusPreservesTextAndMapsMissingValuesToNull(){
        StoreTurnoverRowValidationResult text=validator.validateAndMap(row("sku","08.2026","Store 123",null,null,null,null,null,null,null,null));
        assertTrue(text.valid());assertEquals("Store 123",text.stageRow().storeRus());

        for(String missing:new String[]{null,"","   ","-","N/A"}){
            StoreTurnoverRowValidationResult result=validator.validateAndMap(row("sku","08.2026",missing,null,null,null,null,null,null,null,null));
            assertTrue(result.valid(),String.valueOf(missing));assertNull(result.stageRow().storeRus(),String.valueOf(missing));
        }
    }

    @Test void optionalStoreRusStillRejectsTextLongerThan255(){
        StoreTurnoverRowValidationResult result=validator.validateAndMap(row("sku","08.2026","x".repeat(256),null,null,null,null,null,null,null,null));
        assertFalse(result.valid());assertEquals("storeRus",result.errors().get(0).fieldName());assertEquals("TEXT_TOO_LONG",result.errors().get(0).errorCode());
    }

    @Test void periodUsesStrictMonthYearContract(){
        for(String invalid:List.of("13.2026","00.2026","2026-08","08/2026","abc"))assertFalse(validator.validateAndMap(row("s",invalid,"x","0","0","0","0","0","0","0","0")).valid(),invalid);
        assertEquals(LocalDate.of(2026,1,1),validator.validateAndMap(row("s","01.2026","x","0","0","0","0","0","0","0","0")).stageRow().period());
        assertEquals(LocalDate.of(2026,12,1),validator.validateAndMap(row("s","12.2026","x","0","0","0","0","0","0","0","0")).stageRow().period());
    }

    @Test void invalidAndOverflowIntegersAreErrorsNotZero(){
        for(String invalid:List.of("abc","1.5","2147483648")){
            StoreTurnoverRowValidationResult result=validator.validateAndMap(row("s","08.2026","x",invalid,"0","0","0","0","0","0","0"));
            assertFalse(result.valid());assertEquals("remainingSum",result.errors().get(0).fieldName());
        }
    }

    private StoreTurnoverRawRow row(String sku,String period,String store,String... metrics){
        return new StoreTurnoverRawRow(7L,9L,2L,sku,period,store,metrics[0],metrics[1],metrics[2],metrics[3],metrics[4],metrics[5],metrics[6],metrics[7]);
    }
}
