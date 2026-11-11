package ru.stockmann.replenishment.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestParam;
import ru.stockmann.replenishment.services.dwhexcelload.core.DWHExcelLoadRequest;
import ru.stockmann.replenishment.services.dwhexcelload.delete.DWHUniversalDeleteRequest;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestedByEndpointContractTest {

    @Test
    void allSessionBasedBulkEndpointsDeclareOptionalRequestedByQueryParameter() throws Exception {
        assertOptionalLastParameter(CDDataController.class, "bulk",
                DWHExcelLoadRequest.class, String.class);
        assertOptionalLastParameter(CDEcomController.class, "bulk",
                DWHExcelLoadRequest.class, String.class);
        assertOptionalLastParameter(WeeklyDataController.class, "bulk",
                DWHExcelLoadRequest.class, String.class);
        assertOptionalLastParameter(SalesByChannelController.class, "bulk",
                DWHExcelLoadRequest.class, String.class);
        assertOptionalLastParameter(StoreTurnoverV2Controller.class, "bulk",
                DWHExcelLoadRequest.class, String.class);
        assertOptionalLastParameter(DWHDataController.class, "delete",
                DWHUniversalDeleteRequest.class, String.class);
    }

    @Test
    void typedLegacyDeleteEndpointsKeepOptionalRequestedByQueryParameter() throws Exception {
        assertOptionalLastParameter(CDDataController.class, "deleteByPeriod",
                Integer.class, Integer.class, String.class);
        assertOptionalLastParameter(CDDataController.class, "deleteByLoadSessionId",
                Long.class, String.class);
        assertOptionalLastParameter(CDEcomController.class, "deleteByPeriod",
                Integer.class, Integer.class, String.class);
        assertOptionalLastParameter(CDEcomController.class, "deleteByLoadSessionId",
                Long.class, String.class);
        assertOptionalLastParameter(WeeklyDataController.class, "deleteByPeriod",
                Short.class, Short.class, String.class);
        assertOptionalLastParameter(WeeklyDataController.class, "deleteByLoadSessionId",
                Long.class, String.class);
        assertOptionalLastParameter(SalesByChannelController.class, "deleteByLoadSessionId",
                Long.class, String.class);
        assertOptionalLastParameter(StoreTurnoverV2Controller.class, "deleteByLoadSessionId",
                Long.class, String.class);

        assertOptionalMapEndpoint(CDDataController.class, "deleteByNazvanieAndDen");
        assertOptionalMapEndpoint(CDEcomController.class, "deleteByNazvanieAndDen");
        assertOptionalMapEndpoint(SalesByChannelController.class, "deleteByYearAndMonth");
    }

    @Test
    void abcDataAndStoreTurnoverV1RemainOutsideRequestedByContract() throws Exception {
        assertNoRequestParam(ABCDataController.class.getDeclaredMethod(
                "bulk", ru.stockmann.replenishment.models.ABCDataLoadRequest.class
        ));
        assertNoRequestParam(StoreTurnoverController.class.getDeclaredMethod(
                "loadStoreTurnover", String.class
        ));
    }

    private void assertOptionalLastParameter(
            Class<?> controller, String methodName, Class<?>... parameterTypes
    ) throws Exception {
        Method method = controller.getDeclaredMethod(methodName, parameterTypes);
        RequestParam annotation = method.getParameters()[parameterTypes.length - 1]
                .getAnnotation(RequestParam.class);
        assertTrue(annotation != null, method.toString());
        assertFalse(annotation.required(), method.toString());
    }

    private void assertOptionalMapEndpoint(Class<?> controller, String methodName) throws Exception {
        Method method = controller.getDeclaredMethod(methodName, Map.class, String.class);
        Parameter[] parameters = method.getParameters();
        assertTrue(parameters[0].isAnnotationPresent(RequestParam.class));
        RequestParam requestedBy = parameters[1].getAnnotation(RequestParam.class);
        assertTrue(requestedBy != null);
        assertFalse(requestedBy.required());
    }

    private void assertNoRequestParam(Method method) {
        for (Parameter parameter : method.getParameters()) {
            assertNull(parameter.getAnnotation(RequestParam.class), method.toString());
        }
    }
}
