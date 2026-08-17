package ru.stockmann.replenishment.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import ru.stockmann.replenishment.services.dwhexcelload.core.*;
import ru.stockmann.replenishment.services.dwhexcelload.definitions.StoreTurnoverExcelLoadDefinition;
import ru.stockmann.replenishment.services.storeturnover.StoreTurnoverBulkLoader;
import ru.stockmann.replenishment.services.storeturnover.process.StoreTurnoverProcessor;

import static org.junit.jupiter.api.Assertions.*;

class StoreTurnoverV2ControllerTest {
    @Test void acceptedCsvDelegatesToSharedAsyncLoader(){
        FakeLoader loader=new FakeLoader(DWHExcelLoadResult.ok(12L,"accepted"));FakeAsync async=new FakeAsync();
        StoreTurnoverV2Controller controller=new StoreTurnoverV2Controller(loader,async,null);DWHExcelLoadRequest request=new DWHExcelLoadRequest();request.setFilePath("/tmp/store.csv");
        ResponseEntity<?> response=controller.bulk(request);assertEquals(200,response.getStatusCode().value());assertEquals("/tmp/store.csv",loader.path);assertSame(loader,async.loader);assertEquals(12L,async.id);
    }
    @Test void blankPathIsBadRequestWithoutLoaderCall(){FakeLoader loader=new FakeLoader(DWHExcelLoadResult.ok(1L,"unused"));ResponseEntity<?> response=new StoreTurnoverV2Controller(loader,new FakeAsync(),null).bulk(new DWHExcelLoadRequest());assertEquals(400,response.getStatusCode().value());assertEquals(0,loader.calls);}
    private static class FakeLoader extends StoreTurnoverBulkLoader{DWHExcelLoadResult result;int calls;String path;FakeLoader(DWHExcelLoadResult r){super(null,new StoreTurnoverExcelLoadDefinition(),new StoreTurnoverProcessor(null,null,null,null,null,null,null));result=r;}@Override public DWHExcelLoadResult acceptFile(String p){calls++;path=p;return result;}}
    private static class FakeAsync extends DWHExcelAsyncLoadService{AbstractDWHExcelLoader loader;Long id;@Override public void startAsync(AbstractDWHExcelLoader l,Long i,String p){loader=l;id=i;}}
}
