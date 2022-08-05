package iudx.catalogue.server.apiserver.util;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class RespBuilderTest {
    @Test
    @DisplayName("Test withDetail method")
    public void testWithDetail(VertxTestContext vertxTestContext) {
        RespBuilder respBuilder = new RespBuilder();
        String detail = "dummy";
        respBuilder.withDetail(detail);
        vertxTestContext.completeNow();

    }

    @Test
    @DisplayName("Test withResult method")
    public void testWithResult(VertxTestContext vertxTestContext) {
        RespBuilder respBuilder = new RespBuilder();
        String id="dummy id";
        String method="dummy method";
        String status="dummy status";
        respBuilder.withResult(id,method,status);
        vertxTestContext.completeNow();

    }
    @Test
    @DisplayName("Test withResult method with detail parameter")
    public void testWithResultDetail(VertxTestContext vertxTestContext) {
        RespBuilder respBuilder = new RespBuilder();
        String id="dummy id";
        String method="dummy method";
        String status="dummy status";
        String detail="dummy detail";
        respBuilder.withResult(id,method,status,detail);
        vertxTestContext.completeNow();

    }
    @Test
    @DisplayName("Test getJsonResponse method")
    public void testGetJsonResponse(VertxTestContext vertxTestContext) {
        RespBuilder respBuilder = new RespBuilder();

        respBuilder.getJsonResponse();
        vertxTestContext.completeNow();

    }
}
