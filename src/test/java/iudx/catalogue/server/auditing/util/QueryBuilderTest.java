package iudx.catalogue.server.auditing.util;

import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import jdk.jfr.Description;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static iudx.catalogue.server.auditing.util.Constants.*;
import static iudx.catalogue.server.auditing.util.Constants.API;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class QueryBuilderTest {
    QueryBuilder queryBuilder;
    @Mock
    JsonObject request;

    @Test
    @Description("test buildReadQuery when request does not contain user id ")
    public void testBuildReadQueryNoUSER_ID(VertxTestContext vertxTestContext){
        request=new JsonObject();
        queryBuilder=new QueryBuilder();
        queryBuilder.buildReadQuery(request);
        vertxTestContext.completeNow();
    }
    @Test
    @Description("test buildReadQuery when request contains METHOD and ENDPOINT key")
    public void testBuildReadQuery(VertxTestContext vertxTestContext) {
        queryBuilder=new QueryBuilder();
        request=new JsonObject();
        request.put(METHOD,"httpMethod");
        request.put(ENDPOINT,"endPoint");
        request.put(USER_ID,"userID");
        request.put(API,"api");
        request.put(DATABASE_TABLE_NAME,"tableName");
        queryBuilder.buildReadQuery(request);
        vertxTestContext.completeNow();
    }


}
