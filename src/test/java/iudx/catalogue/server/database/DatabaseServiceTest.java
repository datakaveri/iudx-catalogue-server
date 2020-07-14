package iudx.catalogue.server.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DatabaseServiceTest {
  private static Logger logger = LoggerFactory.getLogger(DatabaseServiceTest.class);
  private static DatabaseService dbService;
  private static Vertx vertxObj;
  private static RestClient client;
  private static Properties properties;
  private static InputStream inputstream;
  private static String databaseIP;
  private static int databasePort;

  @BeforeAll
  @DisplayName("Deploying Verticle")
  static void startVertx(Vertx vertx, VertxTestContext testContext) {
    vertxObj = vertx;

    /* Read the configuration and set the rabbitMQ server properties. */
    properties = new Properties();
    inputstream = null;

    try {

      inputstream = new FileInputStream(Constants.CONFIG_FILE);
      properties.load(inputstream);

      databaseIP = properties.getProperty(Constants.DATABASE_IP);
      databasePort = Integer.parseInt(properties.getProperty(Constants.DATABASE_PORT));

    } catch (Exception ex) {

      logger.info(ex.toString());
    }

    // TODO : Need to enable TLS using xpack security
    client = RestClient.builder(new HttpHost(databaseIP, databasePort, Constants.HTTP)).build();
    dbService = new DatabaseServiceImpl(client);
    testContext.completeNow();
  }

  @AfterEach
  public void finish(VertxTestContext testContext) {
    logger.info("Finishing....");
    vertxObj.close(testContext.succeeding(response -> testContext.completeNow()));
  }

  @Test
  @Order(1)
  @DisplayName("Test CreateItem")
  void createItemTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request
        .put("itemType", "Resource")
        .put("id", "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/pscdcl/xyz/testing123");
    dbService.createItem(
        request,
        testContext.succeeding(
            response ->
                testContext.verify(
                    () -> {
                      String success = "success";
                      String status = response.getString("status");
                      System.out.println(response);
                      assertEquals(success, status);
                      TimeUnit.SECONDS.sleep(5);
                      testContext.completeNow();
                    })));
  }

  @Test
  @Order(2)
  @DisplayName("Test updateItem")
  void updateItemTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request
        .put("itemType", "Resource")
        .put("id", "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/pscdcl/xyz/testing123")
        .put("test", "test");
    dbService.updateItem(
        request,
        testContext.succeeding(
            response ->
                testContext.verify(
                    () -> {
                      String success = "success";
                      String status = response.getString("status");
                      System.out.println(response);
                      assertEquals(success, status);
                      TimeUnit.SECONDS.sleep(5);
                      testContext.completeNow();
                    })));
  }

  @Test
  @Order(3)
  @DisplayName("Test deleteItem")
  void deleteItemTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request
        .put("itemType", "Resource")
        .put("id", "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/pscdcl/xyz/testing123");
    dbService.deleteItem(
        request,
        testContext.succeeding(
            response ->
                testContext.verify(
                    () -> {
                      String success = "success";
                      String status = response.getString("status");
                      System.out.println(response);
                      assertEquals(success, status);
                      TimeUnit.SECONDS.sleep(5);
                      testContext.completeNow();
                    })));
  }

  @Test
  @Order(4)
  @DisplayName("Deleting Non Existant Item")
  void deleteNonExistantItemTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request
        .put("itemType", "Resource")
        .put("id", "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/pscdcl/xyz/testing123");
    dbService.deleteItem(
        request,
        testContext.failing(
            response ->
                testContext.verify(
                    () -> {
                      String failed = "failed";
                      String status = response.getMessage();
                      System.out.println(status);
                      assertTrue(status.contains("\"status\":\"failed\""));
                      TimeUnit.SECONDS.sleep(5);
                      testContext.completeNow();
                    })));
  }

  @Test
  @Order(5)
  @DisplayName("Update non existant Item")
  void updateNonExistantItemTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request
        .put("itemType", "Resource")
        .put("id", "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/pscdcl/xyz/testing123")
        .put("test", "test");
    dbService.updateItem(
        request,
        testContext.succeeding(
            response ->
                testContext.verify(
                    () -> {
                      String success = "success";
                      String status = response.getString("status");
                      System.out.println(response);
                      assertEquals(success, status);
                      TimeUnit.SECONDS.sleep(5);
                      testContext.completeNow();
                    })));
  }

  @Test
  @Order(6)
  @DisplayName("Create existing item")
  void createExistingItemTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request
        .put("itemType", "Resource")
        .put("id", "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/pscdcl/xyz/testing123");
    dbService.createItem(
        request,
        testContext.failing(
            response ->
                testContext.verify(
                    () -> {
                      String success = "success";
                      String status = response.getMessage();
                      assertTrue(status.contains("\"status\":\"failed\""));
                      TimeUnit.SECONDS.sleep(5);
                      testContext.completeNow();
                    })));
  }

  @Test
  @DisplayName("Testing Geo-circle query")
  void searchGeoCircle(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject()
            .put(Constants.SEARCH_TYPE, Constants.SEARCH_TYPE_GEO)
            .put(Constants.COORDINATES_KEY, new JsonArray().add(73.9).add(18.6))
            .put(Constants.MAX_DISTANCE, 5000)
            .put(Constants.GEOMETRY, Constants.POINT)
            .put(Constants.GEORELATION, Constants.INTERSECTS)
            .put(Constants.GEOPROPERTY, Constants.GEO_KEY);

    dbService.searchQuery(
        request,
        testContext.succeeding(
            response ->
                testContext.verify(
                    () -> {
                      assertEquals(
                          73.9113707,
                          response
                              .getJsonArray(Constants.RESULT)
                              .getJsonObject(0)
                              .getJsonObject(Constants.LOCATION)
                              .getJsonObject(Constants.GEOMETRY)
                              .getJsonArray(Constants.COORDINATES_KEY)
                              .getDouble(0));
                      testContext.completeNow();
                    })));
  }

  // @Test
  // @DisplayName("Testing Basic Exceptions (No instanceId key)")
  // void searchWithNoResourceId(VertxTestContext testContext) {
  // JsonObject request = new JsonObject().put(Constants.SEARCH_TYPE,
  //     Constants.SEARCH_TYPE_GEO);
  //
  // dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
  // JsonObject res = new JsonObject(response.getMessage());
  // assertEquals("No instanceId found", res.getString(Constants.DESCRIPTION));
  // testContext.completeNow();
  // })));
  // }

  @Test
  @DisplayName("Testing Basic Exceptions (No searchType key)")
  void searchWithSearchType(VertxTestContext testContext) {
    JsonObject request = new JsonObject();

    dbService.searchQuery(
        request,
        testContext.failing(
            response ->
                testContext.verify(
                    () -> {
                      JsonObject res = new JsonObject(response.getMessage());
                      assertEquals("No searchType found", res.getString(Constants.DESCRIPTION));
                      testContext.completeNow();
                    })));
  }

  @Test
  @DisplayName("Testing Geo-Polygon query")
  void searchGeoPolygon(VertxTestContext testContext) {
    /**
     * coordinates should look like this
     * [[[lo1,la1],[lo2,la2],[lo3,la3],[lo4,la4],[lo5,la5],[lo1,la1]]]
     */
    JsonObject request =
        new JsonObject()
            .put(Constants.GEOMETRY, Constants.POLYGON)
            .put(Constants.GEORELATION, Constants.WITHIN)
            .put(
                Constants.COORDINATES_KEY,
                new JsonArray()
                    .add(
                        new JsonArray()
                            .add(new JsonArray().add(75.9).add(14.5))
                            .add(new JsonArray().add(72).add(13))
                            .add(new JsonArray().add(73).add(20))
                            .add(new JsonArray().add(75.9).add(14.5))))
            .put(Constants.GEOPROPERTY, Constants.GEO_KEY)
            .put(Constants.SEARCH_TYPE, Constants.SEARCH_TYPE_GEO);

    dbService.searchQuery(
        request,
        testContext.succeeding(
            response ->
                testContext.verify(
                    () -> {
                      assertEquals(
                          73.783532,
                          response
                              .getJsonArray(Constants.RESULT)
                              .getJsonObject(0)
                              .getJsonObject(Constants.LOCATION)
                              .getJsonObject(Constants.GEOMETRY)
                              .getJsonArray(Constants.COORDINATES_KEY)
                              .getDouble(0));
                      testContext.completeNow();
                    })));
  }

  @Test
  @DisplayName(
      "Testing Geo Exceptions (Missing necessary parameters [coordinates, geoproperty, georel)")
  void searchMissingGeoParamsGeometry(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject()
            .put(Constants.SEARCH_TYPE, Constants.SEARCH_TYPE_GEO)
            .put(Constants.GEOMETRY, Constants.POLYGON);

    dbService.searchQuery(
        request,
        testContext.failing(
            response ->
                testContext.verify(
                    () -> {
                      JsonObject res = new JsonObject(response.getMessage());
                      assertEquals(
                          "Missing/Invalid geo parameters", res.getString(Constants.DESCRIPTION));
                      testContext.completeNow();
                    })));
  }

  @Test
  @DisplayName("Testing Geo Polygon Exceptions (First and Last coordinates don't match)")
  void searchPolygonFirstLastNoMatch(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject()
            .put(Constants.GEOMETRY, Constants.POLYGON)
            .put(Constants.GEORELATION, Constants.WITHIN)
            .put(
                Constants.COORDINATES_KEY,
                new JsonArray()
                    .add(
                        new JsonArray()
                            .add(new JsonArray().add(75.9).add(14.5))
                            .add(new JsonArray().add(72).add(13))
                            .add(new JsonArray().add(73).add(20))))
            .put(Constants.GEOPROPERTY, Constants.GEO_KEY)
            .put(Constants.SEARCH_TYPE, Constants.SEARCH_TYPE_GEO);

    dbService.searchQuery(
        request,
        testContext.failing(
            response ->
                testContext.verify(
                    () -> {
                      JsonObject res = new JsonObject(response.getMessage());
                      assertEquals(
                          "Coordinate mismatch (Polygon)", res.getString(Constants.DESCRIPTION));
                      testContext.completeNow();
                    })));
  }

  @Test
  @DisplayName("Testing Geo-LineString query")
  void searchGeoLineString(VertxTestContext testContext) {
    /** COORDINATES_KEY should look like this [[lo1,la1],[lo2,la2],[lo3,la3],[lo4,la4],[lo5,la5]] */
    JsonObject request =
        new JsonObject()
            .put(Constants.GEOMETRY, Constants.LINESTRING)
            .put(Constants.GEORELATION, Constants.INTERSECTS)
            .put(
                Constants.COORDINATES_KEY,
                new JsonArray()
                    .add(new JsonArray().add(73.874537).add(18.528311))
                    .add(new JsonArray().add(73.836808).add(18.572797))
                    .add(new JsonArray().add(73.876484).add(18.525007)))
            .put(Constants.GEOPROPERTY, Constants.GEO_KEY)
            .put(Constants.SEARCH_TYPE, Constants.SEARCH_TYPE_GEO);

    dbService.searchQuery(
        request,
        testContext.succeeding(
            response ->
                testContext.verify(
                    () -> {
                      assertEquals(
                          73.874537,
                          response
                              .getJsonArray(Constants.RESULT)
                              .getJsonObject(0)
                              .getJsonObject(Constants.LOCATION)
                              .getJsonObject(Constants.GEOMETRY)
                              .getJsonArray(Constants.COORDINATES_KEY)
                              .getDouble(0));
                      testContext.completeNow();
                    })));
  }

  @Test
  @DisplayName("Testing Geo-BBOX query")
  void searchGeoBbox(VertxTestContext testContext) {
    /** coordinates should look like this [[lo1,la1],[lo3,la3]] */
    JsonObject request =
        new JsonObject()
            .put(Constants.GEOMETRY, Constants.BBOX)
            .put(Constants.GEORELATION, Constants.WITHIN)
            .put(
                Constants.COORDINATES_KEY,
                new JsonArray()
                    .add(new JsonArray().add(73).add(20))
                    .add(new JsonArray().add(75).add(14)))
            .put(Constants.GEOPROPERTY, Constants.GEO_KEY)
            .put(Constants.SEARCH_TYPE, Constants.SEARCH_TYPE_GEO);

    dbService.searchQuery(
        request,
        testContext.succeeding(
            response ->
                testContext.verify(
                    () -> {
                      assertEquals(
                          73.874537,
                          response
                              .getJsonArray(Constants.RESULT)
                              .getJsonObject(0)
                              .getJsonObject(Constants.LOCATION)
                              .getJsonObject(Constants.GEOMETRY)
                              .getJsonArray(Constants.COORDINATES_KEY)
                              .getDouble(0));
                      testContext.completeNow();
                    })));
  }

  @Test
  @DisplayName("Testing Geo-BBOX Exceptions [empty response]")
  void searchBboxEmptyResponse(VertxTestContext testContext) {
    /** COORDINATES_KEY should look like this [[lo1,la1],[lo3,la3]] */
    JsonObject request =
        new JsonObject()
            .put(Constants.GEOMETRY, Constants.BBOX)
            .put(Constants.GEORELATION, Constants.WITHIN)
            .put(
                Constants.COORDINATES_KEY,
                new JsonArray()
                    .add(new JsonArray().add(82).add(25.33))
                    .add(new JsonArray().add(82.01).add(25.317)))
            .put(Constants.GEOPROPERTY, Constants.GEO_KEY)
            .put(Constants.SEARCH_TYPE, Constants.SEARCH_TYPE_GEO);

    dbService.searchQuery(
        request,
        testContext.failing(
            response ->
                testContext.verify(
                    () -> {
                      JsonObject res = new JsonObject(response.getMessage());
                      assertEquals("Empty response", res.getString(Constants.DESCRIPTION));
                      testContext.completeNow();
                    })));
  }

  @Test
  @DisplayName("Testing Response Filter")
  void searchResponseFilter(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject()
            .put(Constants.SEARCH_TYPE, Constants.RESPONSE_FILTER)
            .put("attrs", new JsonArray().add(Constants.ID).add(Constants.TAGS));
    Set<String> attrs = new HashSet<>();
    attrs.add(Constants.ID);
    attrs.add(Constants.TAGS);
    dbService.searchQuery(
        request,
        testContext.succeeding(
            response -> {
              Set<String> resAttrs = new HashSet<>();
              for (Object obj : response.getJsonArray(Constants.RESULT)) {
                JsonObject jsonObj = (JsonObject) obj;
                if (resAttrs != attrs) {
                  resAttrs = jsonObj.fieldNames();
                }
              }
              Set<String> finalResAttrs = resAttrs;
              testContext.verify(
                  () -> {
                    assertEquals(attrs, finalResAttrs);
                    testContext.completeNow();
                  });
            }));
  }

  @Test
  @DisplayName("Testing Response Filter Exceptions (Missing parameters [attrs]")
  void searchMissingResponseFilterParams(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put(Constants.SEARCH_TYPE, Constants.RESPONSE_FILTER);

    dbService.searchQuery(
        request,
        testContext.failing(
            response ->
                testContext.verify(
                    () -> {
                      JsonObject res = new JsonObject(response.getMessage());
                      assertEquals(
                          Constants.ERROR_INVALID_RESPONSE_FILTER,
                          res.getString(Constants.DESCRIPTION));
                      testContext.completeNow();
                    })));
  }

  @Test
  @DisplayName("Testing Complex (Geo + Response Filter) Search")
  void searchComplexGeoResponse(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject()
            .put(Constants.SEARCH_TYPE, Constants.RESPONSE_FILTER_GEO)
            .put(
                "attrs",
                new JsonArray().add(Constants.ID).add(Constants.TAGS).add(Constants.LOCATION))
            .put(Constants.COORDINATES_KEY, new JsonArray().add(73.9).add(18.6))
            .put(Constants.MAX_DISTANCE, 5000)
            .put(Constants.GEOMETRY, Constants.POINT)
            .put(Constants.GEORELATION, Constants.INTERSECTS)
            .put(Constants.GEOPROPERTY, Constants.GEO_KEY);
    Set<String> attrs = new HashSet<>();
    attrs.add(Constants.ID);
    attrs.add(Constants.TAGS);
    attrs.add(Constants.LOCATION);
    dbService.searchQuery(
        request,
        testContext.succeeding(
            response -> {
              Set<String> resAttrs = new HashSet<>();
              for (Object obj : response.getJsonArray(Constants.RESULT)) {
                JsonObject jsonObj = (JsonObject) obj;
                if (resAttrs != attrs) {
                  resAttrs = jsonObj.fieldNames();
                }
              }
              Set<String> finalResAttrs = resAttrs;
              testContext.verify(
                  () -> {
                    assertEquals(
                        73.9113707,
                        response
                            .getJsonArray(Constants.RESULT)
                            .getJsonObject(0)
                            .getJsonObject(Constants.LOCATION)
                            .getJsonObject(Constants.GEOMETRY)
                            .getJsonArray(Constants.COORDINATES_KEY)
                            .getDouble(0));
                    assertEquals(attrs, finalResAttrs);
                    testContext.completeNow();
                  });
            }));
  }

  @Test
  @DisplayName("Testing Count Geo-circle query")
  void countGeoCircle(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject()
            .put(Constants.SEARCH_TYPE, Constants.SEARCH_TYPE_GEO)
            .put(Constants.COORDINATES_KEY, new JsonArray().add(73.9).add(18.6))
            .put(Constants.MAX_DISTANCE, 5000)
            .put(Constants.GEOMETRY, Constants.POINT)
            .put(Constants.GEORELATION, Constants.WITHIN)
            .put(Constants.GEOPROPERTY, Constants.GEO_KEY);

    dbService.countQuery(
        request,
        testContext.succeeding(
            response ->
                testContext.verify(
                    () -> {
                      assertEquals(5, response.getInteger(Constants.COUNT));
                      testContext.completeNow();
                    })));
  }

  @Test
  @DisplayName("Testing response filter with count")
  void countResponseFilter(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject()
            .put(Constants.SEARCH_TYPE, Constants.RESPONSE_FILTER)
            .put("attrs", new JsonArray().add(Constants.ID).add(Constants.TAGS));

    dbService.countQuery(
        request,
        testContext.failing(
            response ->
                testContext.verify(
                    () -> {
                      JsonObject res = new JsonObject(response.getMessage());
                      assertEquals(
                          Constants.COUNT_UNSUPPORTED, res.getString(Constants.DESCRIPTION));
                      testContext.completeNow();
                    })));
  }

  @Test
  @DisplayName("Testing Complex (response filter + geo) with count")
  void countComplexI(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject()
            .put(Constants.SEARCH_TYPE, Constants.RESPONSE_FILTER_GEO)
            .put("attrs", new JsonArray().add(Constants.ID).add(Constants.TAGS))
            .put(Constants.COORDINATES_KEY, new JsonArray().add(73.9).add(18.6))
            .put(Constants.MAX_DISTANCE, 5000)
            .put(Constants.GEOMETRY, Constants.POINT)
            .put(Constants.GEORELATION, Constants.WITHIN)
            .put(Constants.GEOPROPERTY, Constants.GEO_KEY);
    ;

    dbService.countQuery(
        request,
        testContext.failing(
            response ->
                testContext.verify(
                    () -> {
                      JsonObject res = new JsonObject(response.getMessage());
                      assertEquals(
                          Constants.COUNT_UNSUPPORTED, res.getString(Constants.DESCRIPTION));
                      testContext.completeNow();
                    })));
  }

  @Test
  @DisplayName("Testing Count Geo-LineString query")
  void countGeoLineString(VertxTestContext testContext) {
    /** COORDINATES_KEY should look like this [[lo1,la1],[lo2,la2],[lo3,la3],[lo4,la4],[lo5,la5]] */
    JsonObject request =
        new JsonObject()
            .put(Constants.GEOMETRY, Constants.LINESTRING)
            .put(Constants.GEORELATION, Constants.INTERSECTS)
            .put(
                Constants.COORDINATES_KEY,
                new JsonArray()
                    .add(new JsonArray().add(73.874537).add(18.528311))
                    .add(new JsonArray().add(73.836808).add(18.572797))
                    .add(new JsonArray().add(73.876484).add(18.525007)))
            .put(Constants.GEOPROPERTY, Constants.GEO_KEY)
            .put(Constants.SEARCH_TYPE, Constants.SEARCH_TYPE_GEO);

    dbService.countQuery(
        request,
        testContext.succeeding(
            response ->
                testContext.verify(
                    () -> {
                      assertEquals(3, response.getInteger(Constants.COUNT));
                      testContext.completeNow();
                    })));
  }

  @Test
  @DisplayName("Testing Count Geo-BBOX query")
  void countGeoBbox(VertxTestContext testContext) {
    /** coordinates should look like this [[lo1,la1],[lo3,la3]] */
    JsonObject request =
        new JsonObject()
            .put(Constants.GEOMETRY, Constants.BBOX)
            .put(Constants.GEORELATION, Constants.WITHIN)
            .put(
                Constants.COORDINATES_KEY,
                new JsonArray()
                    .add(new JsonArray().add(73).add(20))
                    .add(new JsonArray().add(75).add(14)))
            .put(Constants.GEOPROPERTY, Constants.GEO_KEY)
            .put(Constants.SEARCH_TYPE, Constants.SEARCH_TYPE_GEO);

    dbService.countQuery(
        request,
        testContext.succeeding(
            response ->
                testContext.verify(
                    () -> {
                      assertEquals(50, response.getInteger(Constants.COUNT));
                      testContext.completeNow();
                    })));
  }

  @Test
  @DisplayName("Testing invalid Search request")
  void searchInvalidType(VertxTestContext testContext) {
    JsonObject request =
        new JsonObject()
            .put(Constants.SEARCH_TYPE, "response!@$_geoS241")
            .put(Constants.GEOMETRY, Constants.BBOX)
            .put(Constants.GEORELATION, Constants.WITHIN)
            .put(
                Constants.COORDINATES_KEY,
                new JsonArray()
                    .add(new JsonArray().add(73).add(20))
                    .add(new JsonArray().add(75).add(14)))
            .put(Constants.GEOPROPERTY, Constants.GEO_KEY);

    dbService.searchQuery(
        request,
        testContext.failing(
            response ->
                testContext.verify(
                    () -> {
                      JsonObject res = new JsonObject(response.getMessage());
                      assertEquals(Constants.INVALID_SEARCH, res.getString(Constants.DESCRIPTION));
                      testContext.completeNow();
                    })));
  }
}
