package iudx.catalogue.server.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import iudx.catalogue.server.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.util.Constants.*;

@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class QueryDecoderTest {

  private static QueryDecoder queryDecoder;

  @BeforeAll
  @DisplayName("Deploying Verticle")
  static void startVertx(Vertx vertx, VertxTestContext testContext) {
    
    JsonObject elasticConfig = Configuration.getConfiguration("./configs/config-test.json", 0);
    
     vertx.deployVerticle(new DatabaseVerticle(), new
     DeploymentOptions().setConfig(elasticConfig), testContext.completing());
    queryDecoder = new QueryDecoder();
    testContext.completed();
  }

  @Test
  @Order(1)
  @DisplayName("GeoPoint request to DbQuery")
  public void searchGeoPointTest(VertxTestContext testContext) {

    JsonObject requests = new JsonObject()
        .put(GEOPROPERTY, LOCATION)
        .put(GEORELATION, GEOREL_WITHIN)
        .put(MAX_DISTANCE, 5000)
        .put(GEOMETRY, POINT)
        .put(COORDINATES, new JsonArray().add(73.927285).add(18.502712))
        .put(SEARCH_TYPE, SEARCH_TYPE_GEO)
        .put(SEARCH, true);

    JsonObject json = queryDecoder.searchQuery(requests);

    assertEquals(GEO_CIRCLE,
        json.getJsonObject(QUERY_KEY).getJsonObject("bool").getJsonArray(FILTER).getJsonObject(0)
            .getJsonObject(GEO_SHAPE_KEY).getJsonObject("location.geometry")
            .getJsonObject(SHAPE_KEY).getString(TYPE));
    testContext.completeNow();
  }
  
  @Test
  @Order(2)
  @DisplayName("GeoPolygon request to DbQuery")
  public void searchGeoPolygonTest(VertxTestContext testContext) {

    JsonObject requests = new JsonObject()
        .put(GEOPROPERTY, LOCATION)
        .put(GEORELATION, GEOREL_WITHIN)
        .put(MAX_DISTANCE, 5000)
        .put(GEOMETRY, POLYGON)
        .put(COORDINATES,
            new JsonArray().add(new JsonArray().add(new JsonArray().add(75.9).add(14.5))
                .add(new JsonArray().add(72).add(13)).add(new JsonArray().add(73).add(20))
                .add(new JsonArray().add(75.9).add(14.5))))
        .put(SEARCH_TYPE, SEARCH_TYPE_GEO)
        .put(SEARCH, true);

    JsonObject json = queryDecoder.searchQuery(requests);

    assertEquals(POLYGON,
        json.getJsonObject(QUERY_KEY).getJsonObject("bool").getJsonArray(FILTER).getJsonObject(0)
            .getJsonObject(GEO_SHAPE_KEY).getJsonObject("location.geometry")
            .getJsonObject(SHAPE_KEY).getString(TYPE));
    testContext.completeNow();
  }
  
  @Test
  @Order(3)
  @DisplayName("GeoBbox request to DbQuery")
  public void searchGeoBboxTest(VertxTestContext testContext) {

    JsonObject requests = new JsonObject()
        .put(GEOPROPERTY, LOCATION)
        .put(GEORELATION, GEOREL_WITHIN)
        .put(MAX_DISTANCE, 5000)
        .put(GEOMETRY, BBOX)
        .put(COORDINATES,
            new JsonArray().add(new JsonArray().add(73).add(20))
                .add(new JsonArray().add(75).add(14)))
        .put(SEARCH_TYPE, SEARCH_TYPE_GEO)
        .put(SEARCH, true);

    JsonObject json = queryDecoder.searchQuery(requests);

    assertEquals(GEO_BBOX,
        json.getJsonObject(QUERY_KEY).getJsonObject("bool").getJsonArray(FILTER).getJsonObject(0)
            .getJsonObject(GEO_SHAPE_KEY).getJsonObject("location.geometry")
            .getJsonObject(SHAPE_KEY).getString(TYPE));
    testContext.completeNow();
  }

  @Test
  @Order(4)
  @DisplayName("GeoLineString request to DbQuery")
  public void searchGeoLineStringTest(VertxTestContext testContext) {

    JsonObject requests = new JsonObject()
        .put(GEOPROPERTY, LOCATION)
        .put(GEORELATION, GEOREL_WITHIN)
        .put(MAX_DISTANCE, 5000)
        .put(GEOMETRY, LINESTRING)
        .put(COORDINATES,
            new JsonArray().add(new JsonArray().add(73.874537).add(18.528311))
                .add(new JsonArray().add(73.836808).add(18.572797))
                .add(new JsonArray().add(73.876484).add(18.525007)))
        .put(SEARCH_TYPE, SEARCH_TYPE_GEO)
        .put(SEARCH, true);

    JsonObject json = queryDecoder.searchQuery(requests);

    assertEquals(LINESTRING,
        json.getJsonObject(QUERY_KEY).getJsonObject("bool").getJsonArray(FILTER).getJsonObject(0)
            .getJsonObject(GEO_SHAPE_KEY).getJsonObject("location.geometry")
            .getJsonObject(SHAPE_KEY).getString(TYPE));
    testContext.completeNow();
  }
  
  @Test
  @Order(5)
  @DisplayName("Invalid QueryParameters")
  public void searchInvalidQueryParametersTest(VertxTestContext testContext) {

    JsonObject requests = new JsonObject()
        .put(GEOPROPERTY, LOCATION)
        .put(GEORELATION, GEOREL_WITHIN)
        .put(MAX_DISTANCE, 5000)
        .put(GEOMETRY, SHAPE_KEY)
        .put(COORDINATES,
            new JsonArray().add(new JsonArray().add(73.874537).add(18.528311))
                .add(new JsonArray().add(73.836808).add(18.572797))
                .add(new JsonArray().add(73.876484).add(18.525007)))
        .put(SEARCH_TYPE, SEARCH_TYPE_GEO)
        .put(SEARCH, true);

    JsonObject json = queryDecoder.searchQuery(requests);

    assertEquals(ERROR_INVALID_GEO_PARAMETER, json.getString(ERROR));
    testContext.completeNow();
  }
  
  
  @Test
  @Order(6)
  @DisplayName("Text search request to DbQuery")
  public void searchTextTest(VertxTestContext testContext) {

    JsonObject requests = new JsonObject()
        .put(Q_VALUE, "Golibar Square")
        .put(SEARCH_TYPE, SEARCH_TYPE_TEXT)
        .put(SEARCH, true);

    JsonObject json = queryDecoder.searchQuery(requests);

    assertEquals("Golibar Square",
        json.getJsonObject(QUERY_KEY).getJsonObject("bool").getJsonArray("must").getJsonObject(0)
            .getJsonObject("query_string").getString(QUERY_KEY));
    testContext.completeNow();
  }
  
  @Test
  @Order(7)
  @DisplayName("Attribute search request to DbQuery")
  public void searchAttributeTest(VertxTestContext testContext) {

    JsonObject requests = new JsonObject()
        .put(PROPERTY, new JsonArray().add(ID))
        .put(VALUE,
            new JsonArray().add(
                new JsonArray().add("datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs."
                    + "iudx.io/aqm-bosch-climo/Ambedkar society circle_29")))
        .put(SEARCH_TYPE, SEARCH_TYPE_ATTRIBUTE)
        .put(SEARCH, true);

    JsonObject json = queryDecoder.searchQuery(requests);

    assertEquals(
        "datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs."
            + "iudx.io/aqm-bosch-climo/Ambedkar society circle_29",
        json.getJsonObject(QUERY_KEY).getJsonObject("bool").getJsonArray("must").getJsonObject(0)
            .getJsonObject("bool").getJsonArray("should").getJsonObject(0).getJsonObject(MATCH_KEY)
            .getString(ID_KEYWORD));
    testContext.completeNow();
  }
  
  @Test
  @Order(8)
  @DisplayName("Tag search request to DbQuery")
  public void searchTagTest(VertxTestContext testContext) {

    JsonObject requests = new JsonObject().put(PROPERTY, new JsonArray().add(TAGS))
        .put(VALUE, new JsonArray().add(new JsonArray().add("pollution")))
        .put(SEARCH_TYPE, SEARCH_TYPE_ATTRIBUTE).put(SEARCH, true);

    JsonObject json = queryDecoder.searchQuery(requests);

    assertEquals("pollution",
        json.getJsonObject(QUERY_KEY).getJsonObject("bool").getJsonArray("must").getJsonObject(0)
            .getJsonObject("bool").getJsonArray("should").getJsonObject(0).getJsonObject(MATCH_KEY)
            .getString(TAGS));
    testContext.completeNow();
  }


  @Test
  @Order(9)
  @DisplayName("Relationship search request to DbQuery")
  public void searchRelationshipTest(VertxTestContext testContext) {

    JsonObject requests = new JsonObject().put(ID,
        "datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs.iudx.io"
            + "/aqm-bosch-climo/Sadhu_Wasvani_Square_24")
        .put(RELATIONSHIP, RESOURCE_GRP);

    JsonObject json = new JsonObject(queryDecoder.listRelationshipQuery(requests));

    assertEquals(ITEM_TYPE_RESOURCE_GROUP,
        json.getJsonObject(QUERY_KEY).getJsonObject("bool").getJsonArray("must").getJsonObject(1)
            .getJsonObject(TERM).getString("type.keyword"));
    testContext.completeNow();
  }


  @Test
  @Order(10)
  @DisplayName("Relationship search request to DbQuery")
  public void searchRelationshipTest2(VertxTestContext testContext) {

    JsonObject requests = new JsonObject()
        .put(ID, "datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs.iudx.io"
            + "/aqm-bosch-climo/Sadhu_Wasvani_Square_24")
        .put(RELATIONSHIP, RESOURCE_SVR);

    JsonObject json = new JsonObject(queryDecoder.listRelationshipQuery(requests));

    assertEquals(ITEM_TYPE_RESOURCE_SERVER, json.getJsonObject(QUERY_KEY).getJsonObject("bool")
        .getJsonArray("must").getJsonObject(2).getJsonObject(TERM).getString("type.keyword"));
    testContext.completeNow();
  }


  @Test
  @Order(11)
  @DisplayName("Relationship search request to DbQuery")
  public void listItemTagTest(VertxTestContext testContext) {

    JsonObject requests = new JsonObject()
        .put(ITEM_TYPE, TAGS)
        .put(TYPE, TAGS);

    JsonObject json = new JsonObject(queryDecoder.listItemQuery(requests));

    assertEquals(TAGS + KEYWORD_KEY, json.getJsonObject(AGGREGATION_KEY).getJsonObject(RESULTS)
        .getJsonObject(TERMS_KEY).getString("field"));
    testContext.completeNow();
  }


}
