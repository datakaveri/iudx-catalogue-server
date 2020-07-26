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
    request.put(Constants.ITEM_TYPE, Constants.RESOURCE).put(Constants.ID,
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/pscdcl/xyz/testing123");
    dbService.createItem(request, testContext.succeeding(response -> testContext.verify(() -> {
      String status = response.getString(Constants.STATUS);
      System.out.println(response);
      assertEquals(Constants.SUCCESS, status);
      TimeUnit.SECONDS.sleep(5);
      testContext.completeNow();
    })));
  }

  @Test
  @Order(2)
  @DisplayName("Test updateItem")
  void updateItemTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request.put(Constants.ITEM_TYPE, Constants.RESOURCE)
        .put(Constants.ID,
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/pscdcl/xyz/testing123")
        .put("test", "test");
    dbService.updateItem(request, testContext.succeeding(response -> testContext.verify(() -> {
      String status = response.getString(Constants.STATUS);
      System.out.println(response);
      assertEquals(Constants.SUCCESS, status);
      TimeUnit.SECONDS.sleep(5);
      testContext.completeNow();
    })));
  }

  @Test
  @Order(3)
  @DisplayName("Test deleteItem")
  void deleteItemTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request.put(Constants.ITEM_TYPE, Constants.RESOURCE).put(Constants.ID,
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/pscdcl/xyz/testing123");
    dbService.deleteItem(request, testContext.succeeding(response -> testContext.verify(() -> {
      String status = response.getString(Constants.STATUS);
      System.out.println(response);
      assertEquals(Constants.SUCCESS, status);
      TimeUnit.SECONDS.sleep(5);
      testContext.completeNow();
    })));
  }

  @Test
  @Order(4)
  @DisplayName("Deleting Non Existant Item")
  void deleteNonExistantItemTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request.put(Constants.ITEM_TYPE, Constants.RESOURCE).put(Constants.ID,
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/pscdcl/xyz/testing123");
    dbService.deleteItem(request, testContext.failing(response -> testContext.verify(() -> {
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
    request.put(Constants.ITEM_TYPE, Constants.RESOURCE)
        .put(Constants.ID,
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/pscdcl/xyz/testing123")
        .put("test", "test");
    dbService.updateItem(request, testContext.succeeding(response -> testContext.verify(() -> {
      String status = response.getString(Constants.STATUS);
      System.out.println(response);
      assertEquals(Constants.SUCCESS, status);
      TimeUnit.SECONDS.sleep(5);
      testContext.completeNow();
    })));
  }

  @Test
  @Order(6)
  @DisplayName("Create existing item")
  void createExistingItemTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request.put(Constants.ITEM_TYPE, Constants.RESOURCE).put(Constants.ID,
        "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/pscdcl/xyz/testing123");
    dbService.createItem(request, testContext.failing(response -> testContext.verify(() -> {
      String status = response.getMessage();
      assertTrue(status.contains("\"status\":\"failed\""));
      TimeUnit.SECONDS.sleep(5);
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Geo-circle query")
  void searchGeoCircle(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put(Constants.SEARCH_TYPE, Constants.SEARCH_TYPE_GEO)
        .put(Constants.COORDINATES_KEY, new JsonArray().add(73.9).add(18.6))
        .put(Constants.MAX_DISTANCE, 5000).put(Constants.GEOMETRY, Constants.POINT)
        .put(Constants.GEORELATION, Constants.INTERSECTS)
        .put(Constants.GEOPROPERTY, Constants.GEO_KEY);

    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertEquals(73.9113707,
          response.getJsonArray(Constants.RESULT).getJsonObject(0).getJsonObject(Constants.LOCATION)
              .getJsonObject(Constants.GEOMETRY).getJsonArray(Constants.COORDINATES_KEY)
              .getDouble(0));
      testContext.completeNow();
    })));
  }

  // @Test
  // @DisplayName("Testing Basic Exceptions (No instanceId key)")
  // void searchWithNoResourceId(VertxTestContext testContext) {
  // JsonObject request = new JsonObject().put(Constants.SEARCH_TYPE,
  // Constants.SEARCH_TYPE_GEO);
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

    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
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
    JsonObject request = new JsonObject().put(Constants.GEOMETRY, Constants.POLYGON)
        .put(Constants.GEORELATION, Constants.WITHIN)
        .put(Constants.COORDINATES_KEY,
            new JsonArray().add(new JsonArray().add(new JsonArray().add(75.9).add(14.5))
                .add(new JsonArray().add(72).add(13)).add(new JsonArray().add(73).add(20))
                .add(new JsonArray().add(75.9).add(14.5))))
        .put(Constants.GEOPROPERTY, Constants.GEO_KEY)
        .put(Constants.SEARCH_TYPE, Constants.SEARCH_TYPE_GEO);

    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertEquals(73.783532,
          response.getJsonArray(Constants.RESULT).getJsonObject(0).getJsonObject(Constants.LOCATION)
              .getJsonObject(Constants.GEOMETRY).getJsonArray(Constants.COORDINATES_KEY)
              .getDouble(0));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Geo Exceptions (Missing necessary parameters [coordinates, geoproperty, georel)")
  void searchMissingGeoParamsGeometry(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put(Constants.SEARCH_TYPE, Constants.SEARCH_TYPE_GEO)
        .put(Constants.GEOMETRY, Constants.POLYGON);

    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      JsonObject res = new JsonObject(response.getMessage());
      assertEquals("Missing/Invalid geo parameters", res.getString(Constants.DESCRIPTION));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Geo Polygon Exceptions (First and Last coordinates don't match)")
  void searchPolygonFirstLastNoMatch(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put(Constants.GEOMETRY, Constants.POLYGON)
        .put(Constants.GEORELATION, Constants.WITHIN)
        .put(Constants.COORDINATES_KEY,
            new JsonArray().add(new JsonArray().add(new JsonArray().add(75.9).add(14.5))
                .add(new JsonArray().add(72).add(13)).add(new JsonArray().add(73).add(20))))
        .put(Constants.GEOPROPERTY, Constants.GEO_KEY)
        .put(Constants.SEARCH_TYPE, Constants.SEARCH_TYPE_GEO);

    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      JsonObject res = new JsonObject(response.getMessage());
      assertEquals("Coordinate mismatch (Polygon)", res.getString(Constants.DESCRIPTION));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Geo-LineString query")
  void searchGeoLineString(VertxTestContext testContext) {
    /** COORDINATES_KEY should look like this [[lo1,la1],[lo2,la2],[lo3,la3],[lo4,la4],[lo5,la5]] */
    JsonObject request = new JsonObject().put(Constants.GEOMETRY, Constants.LINESTRING)
        .put(Constants.GEORELATION, Constants.INTERSECTS)
        .put(Constants.COORDINATES_KEY,
            new JsonArray().add(new JsonArray().add(73.874537).add(18.528311))
                .add(new JsonArray().add(73.836808).add(18.572797))
                .add(new JsonArray().add(73.876484).add(18.525007)))
        .put(Constants.GEOPROPERTY, Constants.GEO_KEY)
        .put(Constants.SEARCH_TYPE, Constants.SEARCH_TYPE_GEO);

    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertEquals(73.874537,
          response.getJsonArray(Constants.RESULT).getJsonObject(0).getJsonObject(Constants.LOCATION)
              .getJsonObject(Constants.GEOMETRY).getJsonArray(Constants.COORDINATES_KEY)
              .getDouble(0));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Geo-BBOX query")
  void searchGeoBbox(VertxTestContext testContext) {
    /** coordinates should look like this [[lo1,la1],[lo3,la3]] */
    JsonObject request = new JsonObject().put(Constants.GEOMETRY, Constants.BBOX)
        .put(Constants.GEORELATION, Constants.WITHIN)
        .put(Constants.COORDINATES_KEY,
            new JsonArray().add(new JsonArray().add(73).add(20))
                .add(new JsonArray().add(75).add(14)))
        .put(Constants.GEOPROPERTY, Constants.GEO_KEY)
        .put(Constants.SEARCH_TYPE, Constants.SEARCH_TYPE_GEO);

    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertEquals(73.874537,
          response.getJsonArray(Constants.RESULT).getJsonObject(0).getJsonObject(Constants.LOCATION)
              .getJsonObject(Constants.GEOMETRY).getJsonArray(Constants.COORDINATES_KEY)
              .getDouble(0));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Geo-BBOX Exceptions [empty response]")
  void searchBboxEmptyResponse(VertxTestContext testContext) {
    /** COORDINATES_KEY should look like this [[lo1,la1],[lo3,la3]] */
    JsonObject request = new JsonObject().put(Constants.GEOMETRY, Constants.BBOX)
        .put(Constants.GEORELATION, Constants.WITHIN)
        .put(Constants.COORDINATES_KEY,
            new JsonArray().add(new JsonArray().add(82).add(25.33))
                .add(new JsonArray().add(82.01).add(25.317)))
        .put(Constants.GEOPROPERTY, Constants.GEO_KEY)
        .put(Constants.SEARCH_TYPE, Constants.SEARCH_TYPE_GEO);

    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      JsonObject res = new JsonObject(response.getMessage());
      assertEquals("Empty response", res.getString(Constants.DESCRIPTION));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Response Filter")
  void searchResponseFilter(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put(Constants.SEARCH_TYPE, Constants.RESPONSE_FILTER)
        .put("attrs", new JsonArray().add(Constants.ID).add(Constants.TAGS));
    Set<String> attrs = new HashSet<>();
    attrs.add(Constants.ID);
    attrs.add(Constants.TAGS);
    dbService.searchQuery(request, testContext.succeeding(response -> {
      Set<String> resAttrs = new HashSet<>();
      for (Object obj : response.getJsonArray(Constants.RESULT)) {
        JsonObject jsonObj = (JsonObject) obj;
        if (resAttrs != attrs) {
          resAttrs = jsonObj.fieldNames();
        }
      }
      Set<String> finalResAttrs = resAttrs;
      testContext.verify(() -> {
        assertEquals(attrs, finalResAttrs);
        testContext.completeNow();
      });
    }));
  }

  @Test
  @DisplayName("Testing Response Filter Exceptions (Missing parameters [attrs]")
  void searchMissingResponseFilterParams(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put(Constants.SEARCH_TYPE, Constants.RESPONSE_FILTER);

    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      JsonObject res = new JsonObject(response.getMessage());
      assertEquals(Constants.ERROR_INVALID_RESPONSE_FILTER, res.getString(Constants.DESCRIPTION));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Complex (Geo + Response Filter) Search")
  void searchComplexGeoResponse(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put(Constants.SEARCH_TYPE, Constants.RESPONSE_FILTER_GEO)
        .put("attrs", new JsonArray().add(Constants.ID).add(Constants.TAGS).add(Constants.LOCATION))
        .put(Constants.COORDINATES_KEY, new JsonArray().add(73.9).add(18.6))
        .put(Constants.MAX_DISTANCE, 5000).put(Constants.GEOMETRY, Constants.POINT)
        .put(Constants.GEORELATION, Constants.INTERSECTS)
        .put(Constants.GEOPROPERTY, Constants.GEO_KEY);
    Set<String> attrs = new HashSet<>();
    attrs.add(Constants.ID);
    attrs.add(Constants.TAGS);
    attrs.add(Constants.LOCATION);
    dbService.searchQuery(request, testContext.succeeding(response -> {
      Set<String> resAttrs = new HashSet<>();
      for (Object obj : response.getJsonArray(Constants.RESULT)) {
        JsonObject jsonObj = (JsonObject) obj;
        if (resAttrs != attrs) {
          resAttrs = jsonObj.fieldNames();
        }
      }
      Set<String> finalResAttrs = resAttrs;
      testContext.verify(() -> {
        assertEquals(73.9113707,
            response.getJsonArray(Constants.RESULT).getJsonObject(0)
                .getJsonObject(Constants.LOCATION).getJsonObject(Constants.GEOMETRY)
                .getJsonArray(Constants.COORDINATES_KEY).getDouble(0));
        assertEquals(attrs, finalResAttrs);
        testContext.completeNow();
      });
    }));
  }

  @Test
  @DisplayName("Testing Count Geo-circle query")
  void countGeoCircle(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put(Constants.SEARCH_TYPE, Constants.SEARCH_TYPE_GEO)
        .put(Constants.COORDINATES_KEY, new JsonArray().add(73.9).add(18.6))
        .put(Constants.MAX_DISTANCE, 5000).put(Constants.GEOMETRY, Constants.POINT)
        .put(Constants.GEORELATION, Constants.WITHIN).put(Constants.GEOPROPERTY, Constants.GEO_KEY);

    dbService.countQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertEquals(5, response.getInteger(Constants.COUNT));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing response filter with count")
  void countResponseFilter(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put(Constants.SEARCH_TYPE, Constants.RESPONSE_FILTER)
        .put("attrs", new JsonArray().add(Constants.ID).add(Constants.TAGS));

    dbService.countQuery(request, testContext.failing(response -> testContext.verify(() -> {
      JsonObject res = new JsonObject(response.getMessage());
      assertEquals(Constants.COUNT_UNSUPPORTED, res.getString(Constants.DESCRIPTION));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Complex (response filter + geo) with count")
  void countComplexI(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put(Constants.SEARCH_TYPE, Constants.RESPONSE_FILTER_GEO)
        .put("attrs", new JsonArray().add(Constants.ID).add(Constants.TAGS))
        .put(Constants.COORDINATES_KEY, new JsonArray().add(73.9).add(18.6))
        .put(Constants.MAX_DISTANCE, 5000).put(Constants.GEOMETRY, Constants.POINT)
        .put(Constants.GEORELATION, Constants.WITHIN)
        .put(Constants.GEOPROPERTY, Constants.GEO_KEY);;

    dbService.countQuery(request, testContext.failing(response -> testContext.verify(() -> {
      JsonObject res = new JsonObject(response.getMessage());
      assertEquals(Constants.COUNT_UNSUPPORTED, res.getString(Constants.DESCRIPTION));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Count Geo-LineString query")
  void countGeoLineString(VertxTestContext testContext) {
    /** COORDINATES_KEY should look like this [[lo1,la1],[lo2,la2],[lo3,la3],[lo4,la4],[lo5,la5]] */
    JsonObject request = new JsonObject().put(Constants.GEOMETRY, Constants.LINESTRING)
        .put(Constants.GEORELATION, Constants.INTERSECTS)
        .put(Constants.COORDINATES_KEY,
            new JsonArray().add(new JsonArray().add(73.874537).add(18.528311))
                .add(new JsonArray().add(73.836808).add(18.572797))
                .add(new JsonArray().add(73.876484).add(18.525007)))
        .put(Constants.GEOPROPERTY, Constants.GEO_KEY)
        .put(Constants.SEARCH_TYPE, Constants.SEARCH_TYPE_GEO);

    dbService.countQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertEquals(3, response.getInteger(Constants.COUNT));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Count Geo-BBOX query")
  void countGeoBbox(VertxTestContext testContext) {
    /** coordinates should look like this [[lo1,la1],[lo3,la3]] */
    JsonObject request = new JsonObject().put(Constants.GEOMETRY, Constants.BBOX)
        .put(Constants.GEORELATION, Constants.WITHIN)
        .put(Constants.COORDINATES_KEY,
            new JsonArray().add(new JsonArray().add(73).add(20))
                .add(new JsonArray().add(75).add(14)))
        .put(Constants.GEOPROPERTY, Constants.GEO_KEY)
        .put(Constants.SEARCH_TYPE, Constants.SEARCH_TYPE_GEO);

    dbService.countQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertEquals(50, response.getInteger(Constants.COUNT));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing invalid Search request")
  void searchInvalidType(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put(Constants.SEARCH_TYPE, "response!@$_geoS241")
        .put(Constants.GEOMETRY, Constants.BBOX).put(Constants.GEORELATION, Constants.WITHIN)
        .put(Constants.COORDINATES_KEY,
            new JsonArray().add(new JsonArray().add(73).add(20))
                .add(new JsonArray().add(75).add(14)))
        .put(Constants.GEOPROPERTY, Constants.GEO_KEY);

    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      JsonObject res = new JsonObject(response.getMessage());
      assertEquals(Constants.INVALID_SEARCH, res.getString(Constants.DESCRIPTION));
      testContext.completeNow();
    })));
  }

  /**
   * Simple Attribute Search test ( property=[id]&value=[valid-id] ).
   * 
   * @param testContext handles operations in Vert.x web
   */
  @Test
  @DisplayName("Testing Simple Attribute Search")
  void simpleAttributeSearch(VertxTestContext testContext) {

    /* Constructing request Json Body */
    JsonObject request = new JsonObject().put(Constants.SEARCH_TYPE, Constants.ATTRIBUTE_SEARCH)
        .put(Constants.PROPERTY, new JsonArray().add(Constants.ID)).put(Constants.VALUE,
            new JsonArray().add(
                new JsonArray().add("datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs."
                    + "iudx.org.in/aqm-bosch-climo/Ambedkar society circle_29")));

    /* requesting db service */
    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertEquals(73.879657,
          response.getJsonArray(Constants.RESULT).getJsonObject(0).getJsonObject(Constants.LOCATION)
              .getJsonObject(Constants.GEOMETRY).getJsonArray(Constants.COORDINATES_KEY)
              .getDouble(0));
      testContext.completeNow();
    })));
  }

  /**
   * Simple Attribute Search test with multiple attribute in "value" query parameter
   * (property=[id]&value=[valid-id1,valid-id2]).
   * 
   * @param testContext handles operations in Vert.x web
   */
  @Test
  @DisplayName("Testing Simple Attribute MultiValue Search")
  void simpleAttributeMultiValueSearch(VertxTestContext testContext) {

    /* Constructing request Json Body */
    JsonObject request = new JsonObject().put(Constants.SEARCH_TYPE, Constants.ATTRIBUTE_SEARCH)
        .put(Constants.PROPERTY, new JsonArray().add(Constants.ID)).put(Constants.VALUE,
            new JsonArray().add(new JsonArray()
                .add(
                    "datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs.iudx.org.in/aqm-bos"
                        + "ch-climo/Ambedkar society circle_29")
                .add("datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs.iudx.org.in"
                        + "/aqm-bosch-climo/Blue Diamond Square (Hotel Taj)_10")));

    /* requesting db service */
    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertEquals(73.88559,
          response.getJsonArray(Constants.RESULT).getJsonObject(0).getJsonObject(Constants.LOCATION)
              .getJsonObject(Constants.GEOMETRY).getJsonArray(Constants.COORDINATES_KEY)
              .getDouble(0));
      testContext.completeNow();
    })));
  }

  /**
   * Simple Attribute Search test with invalid property attribute name
   * (property=[invalid-attribute]&value=[valid-id1]).
   * 
   * @param testContext handles operations in Vert.x web
   */
  @Test
  @DisplayName("Testing Simple Attribute InvalidProperty Search")
  void simpleAttributeSearchInvalidProperty(VertxTestContext testContext) {

    /* Constructing request Json Body */
    JsonObject request = new JsonObject().put(Constants.SEARCH_TYPE, Constants.ATTRIBUTE_SEARCH)
        .put(Constants.PROPERTY, new JsonArray().add("invalidProperty"))
        .put(Constants.VALUE, new JsonArray().add(new JsonArray().add("rbccps.org/aa9d66a000d94a78"
            + "895de8d4c0b3a67f3450e531/pscdcl/aqm-bosch-climo/Ambedkar society circle_29")));

    /* requesting db service */
    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      JsonObject res = new JsonObject(response.getLocalizedMessage());
      assertEquals("failed", res.getString("status"));
      testContext.completeNow();
    })));
  }

  /**
   * Simple Attribute Search test with invalid attribute of "value"
   * (property=[valid-attribute]&value=[invalidValid-attribute]).
   * 
   * @param testContext handles operations in Vert.x web
   */
  @Test
  @DisplayName("Testing Simple Attribute NonExistingId Search")
  void simpleAttributeSearchInvalidId(VertxTestContext testContext) {

    /* Constructing request Json Body */
    JsonObject request = new JsonObject().put(Constants.SEARCH_TYPE, Constants.ATTRIBUTE_SEARCH)
        .put(Constants.PROPERTY, new JsonArray().add(Constants.ID))
        .put(Constants.VALUE, new JsonArray().add(new JsonArray().add("non-existing-id")));

    /* requesting db service */
    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      JsonObject res = new JsonObject(response.getLocalizedMessage());
      assertEquals("failed", res.getString("status"));
      testContext.completeNow();
    })));
  }

  /**
   * Multiple Attribute Search test, having multiple attribute in "property" and "value"
   * (property=[attribute1,attribute2]&value=[[valid-value1][valid-value2]]).
   * 
   * @param testContext handles operations in Vert.x web
   */
  @Test
  @DisplayName("Testing Multi Attribute Search")
  void multiAttributeSearch(VertxTestContext testContext) {

    /* Constructing request Json Body */
    JsonObject request = new JsonObject().put(Constants.SEARCH_TYPE, Constants.ATTRIBUTE_SEARCH)
        .put(Constants.PROPERTY,
            new JsonArray().add(Constants.TAGS).add(Constants.DEVICEID_KEY))
        .put(Constants.VALUE, new JsonArray().add(new JsonArray().add(Constants.TAG_AQM))
            .add(new JsonArray().add("8cff12b2-b8be-1230-c5f6-ca96b4e4e441").add("climo")));

    /* requesting db service */
    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertEquals(73.879657,
          response.getJsonArray(Constants.RESULT).getJsonObject(0).getJsonObject(Constants.LOCATION)
              .getJsonObject(Constants.GEOMETRY).getJsonArray(Constants.COORDINATES_KEY)
              .getDouble(0));
      testContext.completeNow();
    })));
  }


  /**
   * Multiple Attribute Search test, having invalid attribute in "property"
   * (property=[valid-attribute1,invalid-attribute2]&value=[[value1][value2]]).
   * 
   * @param testContext handles operations in Vert.x web
   */
  @Test
  @DisplayName("Testing Multi Attribute InvalidProperty Search")
  void multiAttributeSearchInvalidProperty(VertxTestContext testContext) {

    /* Constructing request Json Body */
    JsonObject request = new JsonObject().put(Constants.SEARCH_TYPE, Constants.ATTRIBUTE_SEARCH)
        .put(Constants.PROPERTY,
            new JsonArray().add(Constants.TAGS).add(Constants.DEVICEID_KEY.concat("invalid")))
        .put(Constants.VALUE, new JsonArray().add(new JsonArray().add(Constants.TAG_AQM))
            .add(new JsonArray().add("8cff12b2-b8be-1230-c5f6-ca96b4e4e441").add("climo")));

    /* requesting db service */
    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      JsonObject res = new JsonObject(response.getLocalizedMessage());
      assertEquals("failed", res.getString("status"));
      testContext.completeNow();
    })));
  }

  /**
   * Multiple Attribute Search test, having invalid attribute in "value"
   * (property=[valid-attribute1,valid-attribute2]&value=[[invalid-value1][valid-value2]]).
   * 
   * @param testContext handles operations in Vert.x web
   */
  @Test
  @DisplayName("Testing Simple Attribute NonExistingId Search")
  void multiAttributeSearchInvalidId(VertxTestContext testContext) {

    /* Constructing request Json Body */
    JsonObject request = new JsonObject().put(Constants.SEARCH_TYPE, Constants.ATTRIBUTE_SEARCH)
        .put(Constants.PROPERTY,
            new JsonArray().add(Constants.TAGS).add(Constants.DEVICEID_KEY))
        .put(Constants.VALUE,
            new JsonArray().add(new JsonArray().add(Constants.TAG_AQM.concat("invalidTag")))
                .add(new JsonArray().add("8cff12b2-b8be-1230-c5f6-ca96b4e4e441").add("climo")));

    /* requesting db service */
    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      JsonObject res = new JsonObject(response.getLocalizedMessage());
      assertEquals("failed", res.getString("status"));
      testContext.completeNow();
    })));
  }

  /**
   * Complex Attribute with ResponseFilter search test
   * (property=[attribute1,attribute2]&value=[value1,value2]&filter=[attribute1,attribute2,attribute3]).
   * 
   * @param testContext handles operations in Vert.x web
   */
  @Test
  @DisplayName("Testing Complex (Attribute + Response Filter) Search")
  void searchComplexAttribute(VertxTestContext testContext) {

    /* Constructing request Json Body */
    JsonObject request = new JsonObject()
        .put(Constants.SEARCH_TYPE, Constants.RESPONSE_FILTER.concat(Constants.ATTRIBUTE_SEARCH))
        .put(Constants.PROPERTY,
            new JsonArray().add(Constants.TAGS).add(Constants.DEVICEID_KEY))
        .put(Constants.VALUE,
            new JsonArray().add(new JsonArray().add(Constants.TAG_AQM))
                .add(new JsonArray().add("8cff12b2-b8be-1230-c5f6-ca96b4e4e441").add("climo")))
        .put(Constants.FILTER_KEY,
            new JsonArray().add(Constants.ID).add(Constants.TAGS).add(Constants.LOCATION));

    Set<String> attrs = new HashSet<>();
    attrs.add(Constants.ID);
    attrs.add(Constants.TAGS);
    attrs.add(Constants.LOCATION);

    dbService.searchQuery(request, testContext.succeeding(response -> {
      Set<String> resAttrs = new HashSet<>();
      for (Object obj : response.getJsonArray(Constants.RESULT)) {
        JsonObject jsonObj = (JsonObject) obj;
        if (resAttrs != attrs) {
          resAttrs = jsonObj.fieldNames();
        }
      }
      Set<String> finalResAttrs = resAttrs;
      testContext.verify(() -> {
        assertEquals(73.879657,
            response.getJsonArray(Constants.RESULT).getJsonObject(0)
                .getJsonObject(Constants.LOCATION).getJsonObject(Constants.GEOMETRY)
                .getJsonArray(Constants.COORDINATES_KEY).getDouble(0));
        assertEquals(attrs, finalResAttrs);
        testContext.completeNow();
      });
    }));
  }


  /**
   * Complex Attribute with Geo and ResponseFilter search test
   * (property=[attribute1,attribute2]&value=[value1,value2]&geoPointSearchAttributes
   * &filter=[attribute1,attribute2,attribute3]).
   * 
   * @param testContext handles operations in Vert.x web
   */
  @Test
  @DisplayName("Testing Complex (Attribute + Geo + Response Filter) Search")
  void searchComplexAttributeGeo(VertxTestContext testContext) {

    /* Constructing request Json Body */
    JsonObject request = new JsonObject()
        .put(Constants.SEARCH_TYPE,
            Constants.RESPONSE_FILTER.concat(Constants.ATTRIBUTE_SEARCH)
                .concat(Constants.GEO_SEARCH))
        .put(Constants.PROPERTY,
            new JsonArray().add(Constants.TAGS).add(Constants.DEVICEID_KEY))
        .put(Constants.VALUE,
            new JsonArray().add(new JsonArray().add(Constants.TAG_AQM))
                .add(new JsonArray().add("8cff12b2-b8be-1230-c5f6-ca96b4e4e441").add("climo")))
        .put(Constants.FILTER_KEY,
            new JsonArray().add(Constants.ID).add(Constants.TAGS).add(Constants.LOCATION))
        .put(Constants.COORDINATES_KEY, new JsonArray().add(73.87).add(18.55))
        .put(Constants.MAX_DISTANCE, 5000).put(Constants.GEOMETRY, Constants.POINT)
        .put(Constants.GEORELATION, Constants.INTERSECTS)
        .put(Constants.GEOPROPERTY, Constants.GEO_KEY);

    Set<String> attrs = new HashSet<>();
    attrs.add(Constants.ID);
    attrs.add(Constants.TAGS);
    attrs.add(Constants.LOCATION);

    dbService.searchQuery(request, testContext.succeeding(response -> {
      Set<String> resAttrs = new HashSet<>();
      for (Object obj : response.getJsonArray(Constants.RESULT)) {
        JsonObject jsonObj = (JsonObject) obj;
        if (resAttrs != attrs) {
          resAttrs = jsonObj.fieldNames();
        }
      }
      Set<String> finalResAttrs = resAttrs;
      testContext.verify(() -> {
        assertEquals(73.879657,
            response.getJsonArray(Constants.RESULT).getJsonObject(0)
                .getJsonObject(Constants.LOCATION).getJsonObject(Constants.GEOMETRY)
                .getJsonArray(Constants.COORDINATES_KEY).getDouble(0));
        assertEquals(attrs, finalResAttrs);
        testContext.completeNow();
      });
    }));
  }

  /**
   * Complex Attribute with Text and ResponseFilter search test
   * (property=[attribute1,attribute2]&value=[value1,value2]&q=string-value
   * &filter=[attribute1,attribute2,attribute3]).
   * 
   * @param testContext handles operations in Vert.x web
   */
  @Test
  @DisplayName("Testing Complex (Attribute + Text + Response Filter) Search")
  void searchComplexAttributeText(VertxTestContext testContext) {

    /* Constructing request Json Body */
    JsonObject request = new JsonObject()
        .put(Constants.SEARCH_TYPE,
            Constants.RESPONSE_FILTER.concat(Constants.ATTRIBUTE_SEARCH)
                .concat(Constants.TEXT_SEARCH))
        .put(Constants.PROPERTY,
            new JsonArray().add(Constants.TAGS).add(Constants.DEVICEID_KEY))
        .put(Constants.VALUE,
            new JsonArray().add(new JsonArray().add(Constants.TAG_AQM))
                .add(new JsonArray().add("05fbae93-d3f7-0bbe-dd5d-2c2b4180edc7")))
        .put(Constants.Q_KEY, "climo").put(Constants.FILTER_KEY,
            new JsonArray().add(Constants.ID).add(Constants.TAGS).add(Constants.LOCATION));


    Set<String> attrs = new HashSet<>();
    attrs.add(Constants.ID);
    attrs.add(Constants.TAGS);
    attrs.add(Constants.LOCATION);

    dbService.searchQuery(request, testContext.succeeding(response -> {
      Set<String> resAttrs = new HashSet<>();
      for (Object obj : response.getJsonArray(Constants.RESULT)) {
        JsonObject jsonObj = (JsonObject) obj;
        if (resAttrs != attrs) {
          resAttrs = jsonObj.fieldNames();
        }
      }
      Set<String> finalResAttrs = resAttrs;
      testContext.verify(() -> {
        assertEquals(73.8843632,
            response.getJsonArray(Constants.RESULT).getJsonObject(0)
                .getJsonObject(Constants.LOCATION).getJsonObject(Constants.GEOMETRY)
                .getJsonArray(Constants.COORDINATES_KEY).getDouble(0));
        assertEquals(attrs, finalResAttrs);
        testContext.completeNow();
      });
    }));
  }


  /**
   * Complex Text with Geo and ResponseFilter search test (q=string-value&geoPointSearchAttributes
   * &filter=[attribute1,attribute2,attribute3]).
   * 
   * @param testContext handles operations in Vert.x web
   */
  @Test
  @DisplayName("Testing Complex (Geo + Text + Response Filter) Search")
  void searchComplexGeoText(VertxTestContext testContext) {

    /* Constructing request Json Body */
    JsonObject request =
        new JsonObject()
            .put(Constants.SEARCH_TYPE,
                Constants.RESPONSE_FILTER.concat(Constants.TEXT_SEARCH)
                    .concat(Constants.GEO_SEARCH))
            .put(Constants.FILTER_KEY,
                new JsonArray().add(Constants.ID).add(Constants.TAGS).add(Constants.LOCATION))
            .put(Constants.COORDINATES_KEY, new JsonArray().add(73.878603).add(18.502865))
            .put(Constants.MAX_DISTANCE, 500).put(Constants.GEOMETRY, Constants.POINT)
            .put(Constants.GEORELATION, Constants.WITHIN)
            .put(Constants.GEOPROPERTY, Constants.GEO_KEY).put(Constants.Q_KEY, "Golibar Square");


    Set<String> attrs = new HashSet<>();
    attrs.add(Constants.ID);
    attrs.add(Constants.TAGS);
    attrs.add(Constants.LOCATION);

    dbService.searchQuery(request, testContext.succeeding(response -> {
      Set<String> resAttrs = new HashSet<>();
      for (Object obj : response.getJsonArray(Constants.RESULT)) {
        JsonObject jsonObj = (JsonObject) obj;
        if (resAttrs != attrs) {
          resAttrs = jsonObj.fieldNames();
        }
      }
      Set<String> finalResAttrs = resAttrs;
      testContext.verify(() -> {
        assertEquals(73.878603,
            response.getJsonArray(Constants.RESULT).getJsonObject(0)
                .getJsonObject(Constants.LOCATION).getJsonObject(Constants.GEOMETRY)
                .getJsonArray(Constants.COORDINATES_KEY).getDouble(0));
        assertEquals(attrs, finalResAttrs);
        testContext.completeNow();
      });
    }));
  }

  /**
   * Complex Text with ResponseFilter search test
   * (q=string-value&filter=[attribute1,attribute2,attribute3]).
   * 
   * @param testContext handles operations in Vert.x web
   */
  @Test
  @DisplayName("Testing Complex (Text + Response Filter) Search")
  void searchComplexText(VertxTestContext testContext) {

    /* Constructing request Json Body */
    JsonObject request = new JsonObject()
        .put(Constants.SEARCH_TYPE, Constants.RESPONSE_FILTER.concat(Constants.TEXT_SEARCH))
        .put(Constants.FILTER_KEY,
            new JsonArray().add(Constants.ID).add(Constants.TAGS).add(Constants.LOCATION))
        .put(Constants.Q_KEY, "Golibar Square");


    Set<String> attrs = new HashSet<>();
    attrs.add(Constants.ID);
    attrs.add(Constants.TAGS);
    attrs.add(Constants.LOCATION);

    dbService.searchQuery(request, testContext.succeeding(response -> {
      Set<String> resAttrs = new HashSet<>();
      for (Object obj : response.getJsonArray(Constants.RESULT)) {
        JsonObject jsonObj = (JsonObject) obj;
        if (resAttrs != attrs) {
          resAttrs = jsonObj.fieldNames();
        }
      }
      Set<String> finalResAttrs = resAttrs;
      testContext.verify(() -> {
        assertEquals(73.878603,
            response.getJsonArray(Constants.RESULT).getJsonObject(0)
                .getJsonObject(Constants.LOCATION).getJsonObject(Constants.GEOMETRY)
                .getJsonArray(Constants.COORDINATES_KEY).getDouble(0));
        assertEquals(attrs, finalResAttrs);
        testContext.completeNow();
      });
    }));
  }

  @Test
  @DisplayName("Testing Complex (Attribute + Geo + Text + Response Filter) Search")
  void searchComplexAttributeGeoText(VertxTestContext testContext) {

    /* Constructing request Json Body */
    JsonObject request = new JsonObject()
        .put(Constants.SEARCH_TYPE,
            Constants.RESPONSE_FILTER.concat(Constants.ATTRIBUTE_SEARCH)
                .concat(Constants.GEO_SEARCH).concat(Constants.TEXT_SEARCH))
        .put(Constants.PROPERTY,
            new JsonArray().add(Constants.TAGS).add(Constants.DEVICEID_KEY))
        .put(Constants.VALUE,
            new JsonArray().add(new JsonArray().add(Constants.TAG_AQM))
                .add(new JsonArray().add("8cff12b2-b8be-1230-c5f6-ca96b4e4e441").add("climo")))
        .put(Constants.FILTER_KEY,
            new JsonArray().add(Constants.ID).add(Constants.TAGS).add(Constants.LOCATION))
        .put(Constants.COORDINATES_KEY, new JsonArray().add(73.87).add(18.55))
        .put(Constants.MAX_DISTANCE, 5000).put(Constants.GEOMETRY, Constants.POINT)
        .put(Constants.GEORELATION, Constants.INTERSECTS)
        .put(Constants.GEOPROPERTY, Constants.GEO_KEY).put(Constants.Q_KEY, "society circle");

    Set<String> attrs = new HashSet<>();
    attrs.add(Constants.ID);
    attrs.add(Constants.TAGS);
    attrs.add(Constants.LOCATION);

    dbService.searchQuery(request, testContext.succeeding(response -> {
      Set<String> resAttrs = new HashSet<>();
      for (Object obj : response.getJsonArray(Constants.RESULT)) {
        JsonObject jsonObj = (JsonObject) obj;
        if (resAttrs != attrs) {
          resAttrs = jsonObj.fieldNames();
        }
      }
      Set<String> finalResAttrs = resAttrs;
      testContext.verify(() -> {
        assertEquals(73.879657,
            response.getJsonArray(Constants.RESULT).getJsonObject(0)
                .getJsonObject(Constants.LOCATION).getJsonObject(Constants.GEOMETRY)
                .getJsonArray(Constants.COORDINATES_KEY).getDouble(0));
        assertEquals(attrs, finalResAttrs);
        testContext.completeNow();
      });
    }));
  }
}
