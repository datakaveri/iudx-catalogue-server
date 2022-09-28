package iudx.catalogue.server.auditing.util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static iudx.catalogue.server.auditing.util.Constants.*;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
public class ResponseBuilderTest {
    ResponseBuilder responseBuilder;
    @Test
    @DisplayName("test typeAndTitle method")
    void testTypeAndTitle(VertxTestContext testContext) {
        String status=SUCCESS;
        int statusCode=200;
        responseBuilder=new ResponseBuilder(status);
        assertNotNull( responseBuilder.setTypeAndTitle(statusCode));
        testContext.completeNow();

    }
    @Test
    @DisplayName("test setJsonArray method")
    void testSetJsonArray(VertxTestContext testContext) {
        String status="dummy";
        JsonArray jsonArray=new JsonArray();
        responseBuilder=new ResponseBuilder(status);
        assertNotNull(responseBuilder.setJsonArray(jsonArray));
        testContext.completeNow();

    }
    }
