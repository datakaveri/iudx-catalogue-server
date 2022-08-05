package iudx.catalogue.server.apiserver.util;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;



import java.util.Map;

import static iudx.catalogue.server.util.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)

public class QueryMapperTest {


    @Test
    @DisplayName("Test map2Json method ")
    public void testMap2Json(VertxTestContext vertxTestContext) {

        MultiMap queryParameters=MultiMap.caseInsensitiveMultiMap();
        queryParameters.add("dummy key","");
        QueryMapper queryMapper=new QueryMapper();
        assertNull(  queryMapper.map2Json(queryParameters));
        vertxTestContext.completeNow();
    }
   @Test
    @DisplayName("Test map2Json method with paramKey value")
    public void testMap2JsonKey(VertxTestContext vertxTestContext) {
        MultiMap queryParameters=MultiMap.caseInsensitiveMultiMap();
        queryParameters.add("dummy key","/////");
        QueryMapper queryMapper=new QueryMapper();
        assertEquals(new JsonObject().put("dummy key","/////"), queryMapper.map2Json(queryParameters));
        vertxTestContext.completeNow();

    }
    @Test
    @DisplayName("Test map2Json method with coordinate value")
    public void testMap2JsonCoordinate(VertxTestContext vertxTestContext) {
        MultiMap queryParameters=MultiMap.caseInsensitiveMultiMap();
        queryParameters.add(COORDINATES,"13");
        QueryMapper queryMapper=new QueryMapper();
        Double value= Double.valueOf(queryParameters.get(COORDINATES));
        assertEquals(new JsonObject().put(COORDINATES,value),queryMapper.map2Json(queryParameters));
        vertxTestContext.completeNow();
    }
    @Test
    @DisplayName("Test map2Json method with  Q_Value key")
    public void testMap2JsonQValue(VertxTestContext vertxTestContext) {
        MultiMap queryParameters=MultiMap.caseInsensitiveMultiMap();
        queryParameters.add(Q_VALUE,"!");
        QueryMapper queryMapper=new QueryMapper();
        queryMapper.map2Json(queryParameters);
        vertxTestContext.completeNow();
    }
    @Test
    @DisplayName("Test map2Json method with [] key")
    public void testMap2JsonWithBracket(VertxTestContext vertxTestContext) {
        MultiMap queryParameters=MultiMap.caseInsensitiveMultiMap();
        queryParameters.add("dummy key","[abcd]");
        QueryMapper queryMapper=new QueryMapper();
        queryMapper.map2Json(queryParameters);
        vertxTestContext.completeNow();
    }
    @Test
    @DisplayName("Test map2Json method with exceptAttribute value")
    public void testMap2JsonExceptAttributeValue(VertxTestContext vertxTestContext) {
        MultiMap queryParameters=MultiMap.caseInsensitiveMultiMap();
        queryParameters.add(COORDINATES,"[hhv]");
        QueryMapper queryMapper=new QueryMapper();
        queryMapper.map2Json(queryParameters);
        vertxTestContext.completeNow();
    }
    @Test
    @DisplayName("Test map2Json method with  Q_Value key and q value")
    public void testMap2JsonQ_Value(VertxTestContext vertxTestContext) {
        MultiMap queryParameters=MultiMap.caseInsensitiveMultiMap();
        queryParameters.add(Q_VALUE,"q");
        QueryMapper queryMapper=new QueryMapper();
        queryMapper.map2Json(queryParameters);
        vertxTestContext.completeNow();
    }
    @Test
    @DisplayName("Test handle method with Q_VALUE GEOMETRY PROPERTY FILTER")
    public void testMap2JsonAttributes(VertxTestContext vertxTestContext) {
        MultiMap queryParameters=MultiMap.caseInsensitiveMultiMap();
        queryParameters.add(Q_VALUE,"q");
        queryParameters.add(GEOMETRY,"q");
        queryParameters.add(PROPERTY,"q");
        queryParameters.add(FILTER,"q");
        QueryMapper queryMapper=new QueryMapper();
        queryMapper.map2Json(queryParameters);
        vertxTestContext.completeNow();
    }

}
