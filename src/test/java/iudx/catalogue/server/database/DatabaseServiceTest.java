package iudx.catalogue.server.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import iudx.catalogue.server.Configuration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.util.Constants.*;
import static org.mockito.Mockito.*;

import iudx.catalogue.server.nlpsearch.NLPSearchService;
import iudx.catalogue.server.geocoding.GeocodingService;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;


@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DatabaseServiceTest {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseServiceTest.class);
  private static DatabaseService dbService;
  private static Vertx vertxObj;
  private static ElasticClient client;
  private static String docIndex;
  private static String ratingIndex;
  private static String mlayerInstanceIndex;
  private static String mlayerDomainIndex;
  private static String databaseIP;
  private static int databasePort;
  private static String databaseUser;
  private static String databasePassword;
  private String ratingID;
  private static Configuration config;
  private static WebClient webClient;
  private static NLPSearchService nlpService;
  private static GeocodingService geocodingService;
  private static JsonArray optionalModules;
  @Mock
  AsyncResult<JsonObject> asyncResult;
  @BeforeAll
  @DisplayName("Deploying Verticle")
  static void startVertx(Vertx vertx, VertxTestContext testContext) {

    vertxObj = vertx;
    JsonObject dbConfig = Configuration.getConfiguration("./configs/config-test.json", 0);

    /* Configuration setup */
    databaseIP = dbConfig.getString(DATABASE_IP);
    databasePort = dbConfig.getInteger(DATABASE_PORT);
    databaseUser = dbConfig.getString(DATABASE_UNAME);
    databasePassword = dbConfig.getString(DATABASE_PASSWD);
    docIndex = dbConfig.getString(DOC_INDEX);
    ratingIndex = dbConfig.getString(RATING_INDEX);
    optionalModules = dbConfig.getJsonArray(OPTIONAL_MODULES);

    client = new ElasticClient(databaseIP, databasePort, docIndex, databaseUser, databasePassword);

    if (optionalModules.contains(NLPSEARCH_PACKAGE_NAME)
            && optionalModules.contains(GEOCODING_PACKAGE_NAME)) {
      NLPSearchService nlpService = NLPSearchService.createProxy(vertx, NLP_SERVICE_ADDRESS);
      GeocodingService geoService = GeocodingService.createProxy(vertx, GEOCODING_SERVICE_ADDRESS);
      dbService = new DatabaseServiceImpl(client, docIndex, ratingIndex,mlayerInstanceIndex,mlayerDomainIndex,nlpService, geoService);
    } else {
      dbService = new DatabaseServiceImpl(client, docIndex, ratingIndex,mlayerInstanceIndex,mlayerDomainIndex);
    }

    testContext.completeNow();
  }

  @AfterEach
  public void finish(VertxTestContext testContext) {
    LOGGER.info("Finishing....");
    vertxObj.close(testContext.succeeding(response -> testContext.completeNow()));
  }

  @Test
  @Order(1)
  @DisplayName("Test CreateItem")
  void createItemTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    JsonArray jsonArray = new JsonArray().add("iudx:Resource");
    request.put(ID, "f2e69f85-52df-47a4-9e35-76c7362f21d3").put("type",jsonArray);
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    JsonObject json = new JsonObject().put(TOTAL_HITS,0);
    when(asyncResult.result()).thenReturn(json);
    when(asyncResult.succeeded()).thenReturn(true);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
            .when(DatabaseServiceImpl.client)
            .searchAsync(any(), any(), any());
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
            .when(DatabaseServiceImpl.client)
            .docPostAsync(any(), any(), any());
    dbService.createItem(
            request,
            handler -> {
              if (handler.succeeded()) {
                verify(DatabaseServiceImpl.client, times(1)).searchAsync(anyString(), any(), any());
                testContext.completeNow();
              } else {
                testContext.failNow("Fail");
              }
            });
  }

  @Test
  @Order(2)
  @DisplayName("Test updateItem")
  void updateItemTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    JsonArray jsonArray = new JsonArray().add("iudx:Resource");
    JsonArray result = new JsonArray().add("dummy");
    request.put(ID, "dummyid").put("type",jsonArray);
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    JsonObject json = new JsonObject().put(TOTAL_HITS,1).put(RESULTS,result);
    when(asyncResult.result()).thenReturn(json);
    when(asyncResult.succeeded()).thenReturn(true);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
            .when(DatabaseServiceImpl.client)
            .searchGetId(any(), any(), any());
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(3)).handle(asyncResult);
                return null;
              }
            })
            .when(DatabaseServiceImpl.client)
            .docPutAsync(any(), any(), any(),any());
    dbService.updateItem(
            request,
            handler -> {
              if (handler.succeeded()) {
                verify(DatabaseServiceImpl.client, times(1)).searchGetId(anyString(), any(), any());
                verify(DatabaseServiceImpl.client, times(1)).docPutAsync(anyString(), any(), any(),any());
                testContext.completeNow();
              } else {
                testContext.failNow("Fail");
              }
            });
  }
  @Test
  @Order(3)
  @DisplayName("Test deleteItem")
  void deleteItemTest(VertxTestContext testContext) {
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    JsonObject request = new JsonObject().put("id","dummy id");
    when(asyncResult.succeeded()).thenReturn(true);
    JsonArray jsonArray = new JsonArray().add("dummy id");
    JsonObject json = new JsonObject().put(TOTAL_HITS,1).put(RESULTS,jsonArray);
    when(asyncResult.result()).thenReturn(json);

    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
            .when(DatabaseServiceImpl.client)
            .searchGetId(any(), any(), any());
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
            .when(DatabaseServiceImpl.client)
            .docDelAsync(any(), any(), any());
    dbService.deleteItem(
            request,
            handler -> {
              if (handler.succeeded()) {
                verify(DatabaseServiceImpl.client, times(1)).searchGetId(any(), any(), any());
                verify(DatabaseServiceImpl.client, times(1)).docDelAsync(any(), any(), any());
                testContext.completeNow();
              } else {
                testContext.failNow("fail");
              }
            });
  }

 /*@Test
  @Order(4)
  @DisplayName("Deleting Non Existant Item")
  void deleteNonExistantItemTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request.put(ITEM_TYPE, RESOURCE).put(ID,
        "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/aqm-bosch-climo/Noble Hospital junction_3512345");
    dbService.deleteItem(request, testContext.succeeding(response -> testContext.verify(() -> {
      String status = response.getString(TYPE);
   // assertEquals(TYPE_ITEM_NOT_FOUND, status);
      TimeUnit.SECONDS.sleep(5);
      testContext.completeNow();
    })));
  }*/

/*  @Test
  @Order(5)
  @DisplayName("Update non existant Item")
  void updateNonExistantItemTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request.put(ITEM_TYPE, RESOURCE).put(ID,
                    "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/aqm-bosch-climo/Noble Hospital junction_354567")
            .put("test", "test");
    dbService.updateItem(request, testContext.failing(response -> testContext.verify(() -> {
      String status = new JsonObject(response.getMessage()).getString(TYPE);
      assertEquals(TYPE_ITEM_NOT_FOUND, status);
      TimeUnit.SECONDS.sleep(5);
      testContext.completeNow();
    })));
  }*/

  /*@Test
  @Order(6)
  @DisplayName("Create existing item")
  void createExistingItemTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request.put(ITEM_TYPE, RESOURCE).put(ID,
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/aqm-bosch-climo/aqm_test_3");
    dbService.createItem(request, testContext.failing(response -> testContext.verify(() -> {
      String status = new JsonObject(response.getMessage()).getString(TYPE);
      assertEquals(TYPE_ALREADY_EXISTS, status);
      TimeUnit.SECONDS.sleep(5);
      testContext.completeNow();
    })));
  }*/

/*  @Test
  @Order(9)
  @DisplayName("Create Rating Test")
  void createRatingTest(VertxTestContext testContext) {

    JsonObject request = new JsonObject()
            .put("rating",4.5)
            .put("comment","some comment")
            .put("id", "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood")
            .put("userID", "some-user")
            .put("status", "approved")
            .put("ratingID", "rating-id");

    LOGGER.debug(this.ratingID);
    dbService.createRating(request, testContext.succeeding(response -> testContext.verify(() -> {
      String status = response.getString(TYPE);
      assertEquals(TYPE_SUCCESS, status);
      TimeUnit.SECONDS.sleep(5);
      testContext.completeNow();
    })));
  }

  @Test
  @Order(10)
  @DisplayName("Create existing rating")
  void createExistingRatingTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
            .put("rating",4.5)
            .put("comment","some comment")
            .put("id", "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood")
            .put("userID", "some-user")
            .put("status", "approved")
            .put("ratingID", "rating-id");

    dbService.createRating(request, testContext.failing(response -> testContext.verify(() -> {
      String status = new JsonObject(response.getMessage()).getString(TYPE);
      assertEquals(TYPE_ALREADY_EXISTS, status);
      testContext.completeNow();
    })));
  }

  @Test
  @Order(11)
  @DisplayName("Update rating test")
  void updateRatingTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
            .put("rating",4.5)
            .put("comment","some comment")
            .put("id", "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood")
            .put("userID", "some-user")
            .put("status", "approved")
            .put("ratingID", "rating-id");

    dbService.updateRating(request, testContext.succeeding(response -> testContext.verify(() -> {
      String status = response.getString(TYPE);
      assertEquals(TYPE_SUCCESS, status);
      TimeUnit.SECONDS.sleep(5);
      testContext.completeNow();
    })));
  }

  @Test
  @Order(12)
  @DisplayName("Update non-existing rating test")
  void updateNonExistingRatingTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
            .put("rating",4.5)
            .put("comment","some comment")
            .put("id", "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood")
            .put("userID", "some-user")
            .put("status", "approved")
            .put("ratingID", "rating-id-abc");

    dbService.updateRating(request, testContext.failing(response -> testContext.verify(() -> {
      String status = new JsonObject(response.getMessage()).getString(TYPE);
      assertEquals(TYPE_ITEM_NOT_FOUND, status);
      testContext.completeNow();
    })));
  }

  @Test
  @Order(15)
  @DisplayName("Delete Rating test")
  void deleteRatingTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put("ratingID", "rating-id");

    dbService.deleteRating(request, testContext.succeeding(response -> testContext.verify(() -> {
      String status = response.getString(TYPE);
      assertEquals(TYPE_SUCCESS, status);
      TimeUnit.SECONDS.sleep(5);
      testContext.completeNow();
    })));
  }

  @Test
  @Order(16)
  @DisplayName("Delete non-existing rating test")
  void deleteNonExistingRatingTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put("ratingID", "rating-id-abc");

    dbService.deleteRating(request, testContext.failing(response -> testContext.verify(() -> {
      String status = new JsonObject(response.getMessage()).getString(TYPE);
      assertEquals(TYPE_ITEM_NOT_FOUND, status);
      testContext.completeNow();
    })));
  }

  @Test
  @Order(13)
  @DisplayName("Get rating of a resource for a user")
  void getRatingForUserTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put("ratingID", "rating-id");

    dbService.getRatings(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertEquals(4.5,response.getJsonArray(RESULT).getJsonObject(0).getDouble("rating"));
      testContext.completeNow();
    })));
  }

  @Test
  @Order(14)
  @DisplayName("Get all ratings of a resource")
  void getAllRatingsofResourceTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put("id","iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood");

    dbService.getRatings(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertTrue(response.getInteger("totalHits") >= 1);
      testContext.completeNow();
    })));
  }

  /*  @Test
    @DisplayName("Fail: Get rating for non-existing rating id")
    void failureGetRatingForNonExistingIDTest(VertxTestContext testContext) {
      JsonObject request = new JsonObject().put("ratingID","18c2a0bcafc188ce8cac0c20857a70e88259f60778c6aafb3d22dd9f03531c2b");

      dbService.getRatings(request, testContext.succeeding(response -> testContext.verify(() -> {
        assertEquals(0,response.getInteger("totalHits"));
        testContext.completeNow();
      })));
    }*/
 /* @Test
  @DisplayName("Testing Geo-circle query")
  void searchGeoCircle(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put(SEARCH_TYPE, SEARCH_TYPE_GEO)
        .put(COORDINATES_KEY, new JsonArray().add(73.927).add(18.508)).put(MAX_DISTANCE, 5000)
        .put(GEOMETRY, POINT).put(GEORELATION, GEOREL_WITHIN).put(GEOPROPERTY, LOCATION);

    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertEquals(73.927,
          response.getJsonArray(RESULT).getJsonObject(0).getJsonObject(LOCATION)
              .getJsonObject(GEOMETRY).getJsonArray(COORDINATES_KEY)
              .getDouble(0));
      testContext.completeNow();
    })));
  }
*/
 /* @Test
  @DisplayName("Testing Basic Exceptions (No searchType key)")
  void searchWithSearchType(VertxTestContext testContext) {
    JsonObject request = new JsonObject();

    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      JsonObject res = new JsonObject(response.getMessage());
      assertEquals("No searchType found", res.getString(DETAIL));
      testContext.completeNow();
    })));
  }

/*  @Test
  @DisplayName("Testing Geo-Polygon query")
  void searchGeoPolygon(VertxTestContext testContext) {
    /**
     * coordinates should look like this
     * [[[lo1,la1],[lo2,la2],[lo3,la3],[lo4,la4],[lo5,la5],[lo1,la1]]]
     */
   /* JsonObject request =
        new JsonObject().put(GEOMETRY, POLYGON).put(GEORELATION, GEOREL_WITHIN).put(COORDINATES_KEY,
            new JsonArray().add(new JsonArray().add(new JsonArray().add(76.9).add(14.5))
                .add(new JsonArray().add(72).add(13)).add(new JsonArray().add(73).add(20))
                .add(new JsonArray().add(76.9).add(14.5))))
            .put(GEOPROPERTY, LOCATION).put(SEARCH_TYPE, SEARCH_TYPE_GEO);

    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertEquals(73.927,
          response.getJsonArray(RESULT).getJsonObject(0).getJsonObject(LOCATION)
              .getJsonObject(GEOMETRY).getJsonArray(COORDINATES_KEY)
              .getDouble(0));
      testContext.completeNow();
    })));
  }*/

  // @Test
  // @DisplayName("Testing Geo Exceptions (Missing necessary parameters [coordinates, geoproperty,
  // georel)")
  // void searchMissingGeoParamsGeometry(VertxTestContext testContext) {
  // JsonObject request = new JsonObject().put(SEARCH_TYPE, SEARCH_TYPE_GEO).put(GEOMETRY, POLYGON);
  //
  // dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
  // JsonObject res = new JsonObject(response.getMessage());
  // // assertEquals("Missing/Invalid geo parameters", res.getString(DETAIL));
  // assertTrue(response.getCause() instanceof NullPointerException);
  // testContext.completeNow();
  // })));
  // }

 // @Test
 // @DisplayName("Testing Geo Polygon Exceptions (First and Last coordinates don't match)")
 /* void searchPolygonFirstLastNoMatch(VertxTestContext testContext) {
    JsonObject request =
            new JsonObject().put(GEOMETRY, POLYGON).put(GEORELATION, GEOREL_WITHIN).put(COORDINATES_KEY,
                            new JsonArray().add(new JsonArray().add(new JsonArray().add(75.9).add(14.5))
                                    .add(new JsonArray().add(72).add(13)).add(new JsonArray().add(73).add(20))))
                    .put(GEOPROPERTY, GEO_KEY).put(SEARCH_TYPE, SEARCH_TYPE_GEO);

    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      JsonObject res = new JsonObject(response.getMessage());
      assertEquals("Coordinate mismatch (Polygon)", res.getString(DETAIL));
      testContext.completeNow();
    })));
  }

 /* @Test
  @DisplayName("Testing Geo-LineString query")
  void searchGeoLineString(VertxTestContext testContext) {
    /** COORDINATES_KEY should look like this [[lo1,la1],[lo2,la2],[lo3,la3],[lo4,la4],[lo5,la5]] */
  /*  JsonObject request =
        new JsonObject().put(GEOMETRY, LINESTRING).put(GEORELATION, INTERSECTS).put(COORDINATES_KEY,
            new JsonArray().add(new JsonArray().add(73.927).add(18.528))
                .add(new JsonArray().add(73.836).add(18.572))
                .add(new JsonArray().add(73.927).add(13.091)))
            .put(GEOPROPERTY, LOCATION).put(SEARCH_TYPE, SEARCH_TYPE_GEO);

    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertEquals(73.927,
          response.getJsonArray(RESULT).getJsonObject(0).getJsonObject(LOCATION)
              .getJsonObject(GEOMETRY).getJsonArray(COORDINATES_KEY)
              .getDouble(0));
      testContext.completeNow();
    })));
  }
*/
 /* @Test
  @DisplayName("Testing Geo-BBOX query")
  void searchGeoBbox(VertxTestContext testContext) {
    /** coordinates should look like this [[lo1,la1],[lo3,la3]] */
   /* JsonObject request =
        new JsonObject().put(GEOMETRY, BBOX).put(GEORELATION, GEOREL_WITHIN).put(COORDINATES_KEY,
            new JsonArray().add(new JsonArray().add(73).add(20))
                .add(new JsonArray().add(75).add(14)))
            .put(GEOPROPERTY, LOCATION).put(SEARCH_TYPE, SEARCH_TYPE_GEO);

    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertEquals(73.927,
          response.getJsonArray(RESULT).getJsonObject(0).getJsonObject(LOCATION)
              .getJsonObject(GEOMETRY).getJsonArray(COORDINATES_KEY)
              .getDouble(0));
      testContext.completeNow();
    })));
  }*/

 /* @Test
  @DisplayName("Testing Geo-BBOX Exceptions [empty response]")
  void searchBboxEmptyResponse(VertxTestContext testContext) {
    /** COORDINATES_KEY should look like this [[lo1,la1],[lo3,la3]] */
  /*  JsonObject request =
        new JsonObject().put(GEOMETRY, BBOX).put(GEORELATION, GEOREL_WITHIN).put(COORDINATES_KEY,
            new JsonArray().add(new JsonArray().add(82).add(25.33))
                .add(new JsonArray().add(82.01).add(25.317)))
            .put(GEOPROPERTY, LOCATION).put(SEARCH_TYPE, SEARCH_TYPE_GEO);

    dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      Integer res = response.getInteger(TOTAL_HITS);
      assertEquals(0, res);
      testContext.completeNow();
    })));
  }
*/
 /* @Test
  @DisplayName("Testing Response Filter")
  void searchResponseFilter(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put(SEARCH_TYPE, RESPONSE_FILTER).put("attrs",
        new JsonArray().add(ID).add(TAGS)).put(LIMIT, 1);
    Set<String> attrs = new HashSet<>();
    attrs.add(ID);
    attrs.add(TAGS);
    dbService.searchQuery(request, testContext.succeeding(response -> {
      Set<String> resAttrs = new HashSet<>();
      resAttrs = response.getJsonArray(RESULTS).getJsonObject(0).fieldNames();

      Set<String> finalResAttrs = resAttrs;
      testContext.verify(() -> {
        assertEquals(attrs, finalResAttrs);
        testContext.completeNow();
      });
    }));
  }*/

/*  @Test
  @DisplayName("Testing Response Filter Exceptions (Missing parameters [attrs]")
  void searchMissingResponseFilterParams(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put(SEARCH_TYPE, RESPONSE_FILTER);

    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      LOGGER.debug(response);
      JsonObject res = new JsonObject(response.getMessage());
      assertEquals(TYPE_BAD_FILTER, res.getString(TYPE));
      testContext.completeNow();
    })));
  }

 /* @Test
  @DisplayName("Testing Complex (Geo + Response Filter) Search")
  void searchComplexGeoResponse(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put(SEARCH_TYPE, RESPONSE_FILTER_GEO)
        .put("attrs", new JsonArray().add(ID).add(TAGS).add(LOCATION))
        .put(COORDINATES_KEY, new JsonArray().add(73.927285).add(18.502712)).put(MAX_DISTANCE, 5000)
        .put(GEOMETRY, POINT).put(GEORELATION, INTERSECTS).put(GEOPROPERTY, LOCATION);
    Set<String> attrs = new HashSet<>();
    attrs.add(ID);
    attrs.add(TAGS);
    attrs.add(LOCATION);
    dbService.searchQuery(request, testContext.succeeding(response -> {
      Set<String> resAttrs = new HashSet<>();
      for (Object obj : response.getJsonArray(RESULT)) {
        JsonObject jsonObj = (JsonObject) obj;
        if (resAttrs != attrs) {
          resAttrs = jsonObj.fieldNames();
        }
      }
      Set<String> finalResAttrs = resAttrs;
      testContext.verify(() -> {
        assertEquals(73.927,
            response.getJsonArray(RESULT).getJsonObject(0).getJsonObject(LOCATION)
                .getJsonObject(GEOMETRY).getJsonArray(COORDINATES_KEY).getDouble(0));
        assertEquals(attrs, finalResAttrs);
        testContext.completeNow();
      });
    }));
  }
*/
 /* @Test
  @DisplayName("Testing Count Geo-circle query")
  void countGeoCircle(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put(SEARCH_TYPE, SEARCH_TYPE_GEO)
        .put(COORDINATES_KEY, new JsonArray().add(73.927285).add(18.502712)).put(MAX_DISTANCE, 5000)
        .put(GEOMETRY, POINT).put(GEORELATION, GEOREL_WITHIN).put(GEOPROPERTY, LOCATION);

    dbService.countQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertNotEquals(0, response.getInteger(TOTAL_HITS));
      testContext.completeNow();
    })));
  }*/

 /* @Test
  @DisplayName("Testing response filter with count")
  void countResponseFilter(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put(SEARCH_TYPE, RESPONSE_FILTER).put("attrs",
            new JsonArray().add(ID).add(TAGS));

    dbService.countQuery(request, testContext.failing(response -> testContext.verify(() -> {
      JsonObject res = new JsonObject(response.getMessage());
      assertEquals(TYPE_OPERATION_NOT_ALLOWED, res.getString(TYPE));
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Testing Complex (response filter + geo) with count")
  void countComplexI(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put(SEARCH_TYPE, RESPONSE_FILTER_GEO)
            .put("attrs", new JsonArray().add(ID).add(TAGS))
            .put(COORDINATES_KEY, new JsonArray().add(73.9).add(18.6)).put(MAX_DISTANCE, 5000)
            .put(GEOMETRY, POINT).put(GEORELATION, GEOREL_WITHIN).put(GEOPROPERTY, GEO_KEY);;

    dbService.countQuery(request, testContext.failing(response -> testContext.verify(() -> {
      JsonObject res = new JsonObject(response.getMessage());
      assertEquals(TYPE_OPERATION_NOT_ALLOWED, res.getString(TYPE));
      testContext.completeNow();
    })));
  }

/*  @Test
  @DisplayName("Testing Count Geo-LineString query")
  void countGeoLineString(VertxTestContext testContext) {
    /** COORDINATES_KEY should look like this [[lo1,la1],[lo2,la2],[lo3,la3],[lo4,la4],[lo5,la5]] */
  /*  JsonObject request =
        new JsonObject().put(GEOMETRY, LINESTRING).put(GEORELATION, INTERSECTS).put(COORDINATES_KEY,
            new JsonArray().add(new JsonArray().add(73.927).add(13.691))
                .add(new JsonArray().add(73.836).add(18.57))
                .add(new JsonArray().add(73.927).add(13.691)))
            .put(GEOPROPERTY, LOCATION).put(SEARCH_TYPE, SEARCH_TYPE_GEO);

    dbService.countQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertNotEquals(0, response.getInteger(TOTAL_HITS));
      testContext.completeNow();
    })));
  }*/

/*  @Test
  @DisplayName("Testing Count Geo-BBOX query")
  void countGeoBbox(VertxTestContext testContext) {
    /** coordinates should look like this [[lo1,la1],[lo3,la3]] */
  /*  JsonObject request =
        new JsonObject().put(GEOMETRY, BBOX).put(GEORELATION, GEOREL_WITHIN).put(COORDINATES_KEY,
            new JsonArray().add(new JsonArray().add(73).add(20))
                .add(new JsonArray().add(75).add(18)))
            .put(GEOPROPERTY, LOCATION).put(SEARCH_TYPE, SEARCH_TYPE_GEO);

    dbService.countQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertEquals(TYPE_SUCCESS, response.getString(TYPE));
      testContext.completeNow();
    })));
  }*/

 /* @Test
  @DisplayName("Testing invalid Search request")
  void searchInvalidType(VertxTestContext testContext) {
    JsonObject request = new JsonObject().put(SEARCH_TYPE, "response!@$_geoS241")
            .put(GEOMETRY, BBOX).put(GEORELATION, GEOREL_WITHIN).put(COORDINATES_KEY,
                    new JsonArray().add(new JsonArray().add(73).add(20))
                            .add(new JsonArray().add(75).add(14)))
            .put(GEOPROPERTY, GEO_KEY);

    dbService.searchQuery(request, testContext.failing(response -> testContext.verify(() -> {
      JsonObject res = new JsonObject(response.getMessage());
      assertEquals(TYPE_INVALID_SYNTAX, res.getString(TYPE));
      testContext.completeNow();
    })));
  }

  /**
   * Simple Attribute Search test ( property=[id]&value=[valid-id] ).
   *
   * @param testContext handles operations in Vert.x web
   */
 /* @Test
  @DisplayName("Testing Simple Attribute Search")
  void simpleAttributeSearch(VertxTestContext testContext) {

    /* Constructing request Json Body */
 /*   JsonObject request = new JsonObject().put(SEARCH_TYPE, SEARCH_TYPE_ATTRIBUTE)
        .put(PROPERTY, new JsonArray().add(ID)).put(VALUE,
            new JsonArray().add(
                new JsonArray().add("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs."
                    + "iudx.io/aqm-bosch-climo/aqm_test_4")));

    /* requesting db service */
  /*  dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertEquals(73.927,
          response.getJsonArray(RESULT).getJsonObject(0).getJsonObject(LOCATION)
              .getJsonObject(GEOMETRY).getJsonArray(COORDINATES_KEY)
              .getDouble(0));
      testContext.completeNow();
    })));
  }
*/
  /**
   * Simple Attribute Search test with multiple attribute in "value" query parameter
   * (property=[id]&value=[valid-id1,valid-id2]).
   *
   * @param testContext handles operations in Vert.x web
   */
 /* @Test
  @DisplayName("Testing Simple Attribute MultiValue Search")
  void simpleAttributeMultiValueSearch(VertxTestContext testContext) {

    /* Constructing request Json Body */
  /*  JsonObject request = new JsonObject().put(SEARCH_TYPE, SEARCH_TYPE_ATTRIBUTE)
        .put(PROPERTY, new JsonArray().add(ID)).put(VALUE,
            new JsonArray().add(new JsonArray()
                .add(
                    "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/aqm-bos"
                        + "ch-climo/aqm_test_4")
                .add("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io"
                    + "/aqm-bosch-climo/aqm_test_5")));

    /* requesting db service */
    /*dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertEquals(73.927,
          response.getJsonArray(RESULT).getJsonObject(0).getJsonObject(LOCATION)
              .getJsonObject(GEOMETRY).getJsonArray(COORDINATES_KEY)
              .getDouble(0));
      testContext.completeNow();
    })));
  }
*/
  /**
   * Simple Attribute Search test with invalid property attribute name
   * (property=[invalid-attribute]&value=[valid-id1]).
   *
   * @param testContext handles operations in Vert.x web
   */
/*  @Test
  @DisplayName("Testing Simple Attribute InvalidProperty Search")
  void simpleAttributeSearchInvalidProperty(VertxTestContext testContext) {

    /* Constructing request Json Body */
 //   JsonObject request = new JsonObject().put(SEARCH_TYPE, SEARCH_TYPE_ATTRIBUTE)
  //          .put(PROPERTY, new JsonArray().add("invalidProperty"))
  //          .put(VALUE, new JsonArray().add(new JsonArray().add("rbccps.org/aa9d66a000d94a78"
           //         + "895de8d4c0b3a67f3450e531/pscdcl/aqm-bosch-climo/Ambedkar society circle_29")));

    /* requesting db service */
 /*   dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      Integer res = response.getInteger(TOTAL_HITS);
      assertEquals(0, res);
      testContext.completeNow();
    })));
  }

  /**
   * Simple Attribute Search test with invalid attribute of "value"
   * (property=[valid-attribute]&value=[invalidValid-attribute]).
   *
   * @param testContext handles operations in Vert.x web
   */
 // @Test
 // @DisplayName("Testing Simple Attribute NonExistingId Search")
 /* void simpleAttributeSearchInvalidId(VertxTestContext testContext) {

    /* Constructing request Json Body */
  /*  JsonObject request =
            new JsonObject().put(SEARCH_TYPE, SEARCH_TYPE_ATTRIBUTE)
                    .put(PROPERTY, new JsonArray().add(ID))
                    .put(VALUE, new JsonArray().add(new JsonArray().add("non-existing-id")));

    /* requesting db service */
  /*  dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      Integer res = response.getInteger(TOTAL_HITS);
      assertEquals(0, res);
      testContext.completeNow();
    })));
  }

  /**
   * Multiple Attribute Search test, having multiple attribute in "property" and "value"
   * (property=[attribute1,attribute2]&value=[[valid-value1][valid-value2]]).
   *
   * @param testContext handles operations in Vert.x web
   */
  /*@Test
  @DisplayName("Testing Multi Attribute Search")
  void multiAttributeSearch(VertxTestContext testContext) {

    /* Constructing request Json Body */
   /* JsonObject request = new JsonObject().put(SEARCH_TYPE, SEARCH_TYPE_ATTRIBUTE)
        .put(PROPERTY, new JsonArray().add(TAGS).add(TYPE))
        .put(VALUE, new JsonArray().add(new JsonArray().add(TAG_AQM))
            .add(new JsonArray().add("iudx:Resource").add("climo")));

    /* requesting db service */
 /*   dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      assertEquals(73.927,
          response.getJsonArray(RESULT).getJsonObject(0).getJsonObject(LOCATION)
              .getJsonObject(GEOMETRY).getJsonArray(COORDINATES_KEY)
              .getDouble(0));
      testContext.completeNow();
    })));
  }*/


  /**
   * Multiple Attribute Search test, having invalid attribute in "property"
   * (property=[valid-attribute1,invalid-attribute2]&value=[[value1][value2]]).
   *
   * @param testContext handles operations in Vert.x web
   */
//  @Test
 // @DisplayName("Testing Multi Attribute InvalidProperty Search")
 /* void multiAttributeSearchInvalidProperty(VertxTestContext testContext) {

    /* Constructing request Json Body */
 /*   JsonObject request = new JsonObject().put(SEARCH_TYPE, SEARCH_TYPE_ATTRIBUTE)
            .put(PROPERTY, new JsonArray().add(TAGS).add(DEVICEID_KEY.concat("invalid")))
            .put(VALUE, new JsonArray().add(new JsonArray().add(TAG_AQM))
                    .add(new JsonArray().add("8cff12b2-b8be-1230-c5f6-ca96b4e4e441").add("climo")));

    /* requesting db service */
  /*  dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      Integer res = response.getInteger(TOTAL_HITS);
      assertEquals(0, res);
      testContext.completeNow();
    })));
  }

  /**
   * Multiple Attribute Search test, having invalid attribute in "value"
   * (property=[valid-attribute1,valid-attribute2]&value=[[invalid-value1][valid-value2]]).
   *
   * @param testContext handles operations in Vert.x web
   */
 // @Test
 // @DisplayName("Testing Simple Attribute NonExistingId Search")
  /*void multiAttributeSearchInvalidId(VertxTestContext testContext) {

    /* Constructing request Json Body */
 /*   JsonObject request = new JsonObject().put(SEARCH_TYPE, SEARCH_TYPE_ATTRIBUTE)
            .put(PROPERTY, new JsonArray().add(TAGS).add(DEVICEID_KEY))
            .put(VALUE, new JsonArray().add(new JsonArray().add(TAG_AQM.concat("invalidTag")))
                    .add(new JsonArray().add("8cff12b2-b8be-1230-c5f6-ca96b4e4e441").add("climo")));

    /* requesting db service */
  /*  dbService.searchQuery(request, testContext.succeeding(response -> testContext.verify(() -> {
      Integer res = response.getInteger(TOTAL_HITS);
      assertEquals(0, res);
      testContext.completeNow();
    })));
  }

/**
 * Complex Attribute with ResponseFilter search test
 * (property=[attribute1,attribute2]&value=[value1,value2]&filter=[attribute1,attribute2,attribute3]).
 *
 * @param testContext handles operations in Vert.x web
 */
/*  @Test
  @DisplayName("Testing Complex (Attribute + Response Filter) Search")
  void searchComplexAttribute(VertxTestContext testContext) {

    /* Constructing request Json Body */
   /* JsonObject request = new JsonObject()
        .put(SEARCH_TYPE, RESPONSE_FILTER.concat(SEARCH_TYPE_ATTRIBUTE))
        .put(PROPERTY, new JsonArray().add(TAGS).add(TYPE))
        .put(VALUE, new JsonArray().add(new JsonArray().add(TAG_AQM))
            .add(new JsonArray().add("iudx:Resource").add("climo")))
        .put(FILTER, new JsonArray().add(ID).add(TAGS).add(LOCATION));

    Set<String> attrs = new HashSet<>();
    attrs.add(ID);
    attrs.add(TAGS);
    attrs.add(LOCATION);

    dbService.searchQuery(request, testContext.succeeding(response -> {
      Set<String> resAttrs = new HashSet<>();
      for (Object obj : response.getJsonArray(RESULT)) {
        JsonObject jsonObj = (JsonObject) obj;
        if (resAttrs != attrs) {
          resAttrs = jsonObj.fieldNames();
        }
      }
      Set<String> finalResAttrs = resAttrs;
      testContext.verify(() -> {
        assertEquals(73.927,
            response.getJsonArray(RESULT).getJsonObject(0).getJsonObject(LOCATION)
                .getJsonObject(GEOMETRY).getJsonArray(COORDINATES_KEY).getDouble(0));
        assertEquals(attrs, finalResAttrs);
        testContext.completeNow();
      });
    }));
  }

*/
/**
 * Complex Attribute with Geo and ResponseFilter search test
 * (property=[attribute1,attribute2]&value=[value1,value2]&geoPointSearchAttributes
 * &filter=[attribute1,attribute2,attribute3]).
 *
 * @param testContext handles operations in Vert.x web
 */
 /* @Test
  @DisplayName("Testing Complex (Attribute + Geo + Response Filter) Search")
  void searchComplexAttributeGeo(VertxTestContext testContext) {

    /* Constructing request Json Body */
   /* JsonObject request = new JsonObject()
        .put(SEARCH_TYPE, RESPONSE_FILTER.concat(SEARCH_TYPE_ATTRIBUTE).concat(SEARCH_TYPE_GEO))
        .put(PROPERTY, new JsonArray().add(TAGS).add(TYPE))
        .put(VALUE, new JsonArray().add(new JsonArray().add(TAG_AQM))
            .add(new JsonArray().add("iudx:Resource").add("climo")))
        .put(FILTER, new JsonArray().add(ID).add(TAGS).add(LOCATION))
        .put(COORDINATES_KEY, new JsonArray().add(73.927285).add(18.502712)).put(MAX_DISTANCE, 5000)
        .put(GEOMETRY, POINT).put(GEORELATION, INTERSECTS).put(GEOPROPERTY, LOCATION);

    Set<String> attrs = new HashSet<>();
    attrs.add(ID);
    attrs.add(TAGS);
    attrs.add(LOCATION);

    dbService.searchQuery(request, testContext.succeeding(response -> {
      Set<String> resAttrs = new HashSet<>();
      for (Object obj : response.getJsonArray(RESULT)) {
        JsonObject jsonObj = (JsonObject) obj;
        if (resAttrs != attrs) {
          resAttrs = jsonObj.fieldNames();
        }
      }
      Set<String> finalResAttrs = resAttrs;
      testContext.verify(() -> {
        assertEquals(73.927,
            response.getJsonArray(RESULT).getJsonObject(0).getJsonObject(LOCATION)
                .getJsonObject(GEOMETRY).getJsonArray(COORDINATES_KEY).getDouble(0));
        assertEquals(attrs, finalResAttrs);
        testContext.completeNow();
      });
    }));
  }*/
/**
 * Complex Attribute with Text and ResponseFilter search test
 * (property=[attribute1,attribute2]&value=[value1,value2]&q=string-value
 * &filter=[attribute1,attribute2,attribute3]).
 *
 * @param testContext handles operations in Vert.x web
 */
/*  @Test
  @DisplayName("Testing Complex (Attribute + Text + Response Filter) Search")
  void searchComplexAttributeText(VertxTestContext testContext) {

    /* Constructing request Json Body */
   /* JsonObject request = new JsonObject()
        .put(SEARCH_TYPE, RESPONSE_FILTER.concat(SEARCH_TYPE_ATTRIBUTE).concat(SEARCH_TYPE_TEXT))
        .put(PROPERTY, new JsonArray().add(TAGS).add(TYPE))
        .put(VALUE, new JsonArray().add(new JsonArray().add(TAG_AQM))
            .add(new JsonArray().add("iudx:Resource")))
        .put(Q_VALUE, "climo").put(FILTER, new JsonArray().add(ID).add(TAGS).add(LOCATION));


    Set<String> attrs = new HashSet<>();
    attrs.add(ID);
    attrs.add(TAGS);
    attrs.add(LOCATION);

    dbService.searchQuery(request, testContext.succeeding(response -> {
      Set<String> resAttrs = new HashSet<>();
      for (Object obj : response.getJsonArray(RESULT)) {
        JsonObject jsonObj = (JsonObject) obj;
        if (resAttrs != attrs) {
          resAttrs = jsonObj.fieldNames();
        }
      }
      Set<String> finalResAttrs = resAttrs;
      testContext.verify(() -> {
        assertEquals(73.927,
            response.getJsonArray(RESULT).getJsonObject(0).getJsonObject(LOCATION)
                .getJsonObject(GEOMETRY).getJsonArray(COORDINATES_KEY).getDouble(0));
        assertEquals(attrs, finalResAttrs);
        testContext.completeNow();
      });
    }));
  }
*/

/**
 * Complex Text with Geo and ResponseFilter search test (q=string-value&geoPointSearchAttributes
 * &filter=[attribute1,attribute2,attribute3]).
 *
 * @param testContext handles operations in Vert.x web
 */
/*  @Test
  @DisplayName("Testing Complex (Geo + Text + Response Filter) Search")
  void searchComplexGeoText(VertxTestContext testContext) {

    /* Constructing request Json Body */
 /*   JsonObject request =
        new JsonObject()
            .put(SEARCH_TYPE, RESPONSE_FILTER.concat(SEARCH_TYPE_TEXT).concat(SEARCH_TYPE_GEO))
            .put(FILTER, new JsonArray().add(ID).add(TAGS).add(LOCATION))
            .put(COORDINATES_KEY, new JsonArray().add(73.927285).add(18.502712))
            .put(MAX_DISTANCE, 500).put(GEOMETRY, POINT).put(GEORELATION, GEOREL_WITHIN)
            .put(GEOPROPERTY, LOCATION).put(Q_VALUE, "aqm_test_4");


    Set<String> attrs = new HashSet<>();
    attrs.add(ID);
    attrs.add(TAGS);
    attrs.add(LOCATION);

    dbService.searchQuery(request, testContext.succeeding(response -> {
      Set<String> resAttrs = new HashSet<>();
      for (Object obj : response.getJsonArray(RESULT)) {
        JsonObject jsonObj = (JsonObject) obj;
        if (resAttrs != attrs) {
          resAttrs = jsonObj.fieldNames();
        }
      }
      Set<String> finalResAttrs = resAttrs;
      testContext.verify(() -> {
        assertEquals(73.927,
            response.getJsonArray(RESULT).getJsonObject(0).getJsonObject(LOCATION)
                .getJsonObject(GEOMETRY).getJsonArray(COORDINATES_KEY).getDouble(0));
        assertEquals(attrs, finalResAttrs);
        testContext.completeNow();
      });
    }));
  }
*/
/**
 * Complex Text with ResponseFilter search test
 * (q=string-value&filter=[attribute1,attribute2,attribute3]).
 *
 * @param testContext handles operations in Vert.x web
 */
/*  @Test
  @DisplayName("Testing Complex (Text + Response Filter) Search")
  void searchComplexText(VertxTestContext testContext) {

    /* Constructing request Json Body */
  /*  JsonObject request = new JsonObject()
        .put(SEARCH_TYPE, RESPONSE_FILTER.concat(SEARCH_TYPE_TEXT))
        .put(FILTER, new JsonArray().add(ID).add(TAGS).add(LOCATION))
        .put(Q_VALUE, "aqm_test_2");


    Set<String> attrs = new HashSet<>();
    attrs.add(ID);
    attrs.add(TAGS);
    attrs.add(LOCATION);

    dbService.searchQuery(request, testContext.succeeding(response -> {
      Set<String> resAttrs = new HashSet<>();
      for (Object obj : response.getJsonArray(RESULT)) {
        JsonObject jsonObj = (JsonObject) obj;
        if (resAttrs != attrs) {
          resAttrs = jsonObj.fieldNames();
        }
      }
      Set<String> finalResAttrs = resAttrs;
      testContext.verify(() -> {
        assertEquals(73.874,
            response.getJsonArray(RESULT).getJsonObject(0).getJsonObject(LOCATION)
                .getJsonObject(GEOMETRY).getJsonArray(COORDINATES_KEY).getDouble(0));
        assertEquals(attrs, finalResAttrs);
        testContext.completeNow();
      });
    }));
  }
*/
  /* @Test
 @DisplayName("Testing Complex (Attribute + Geo + Text + Response Filter) Search")
 void searchComplexAttributeGeoText(VertxTestContext testContext) {

   /* Constructing request Json Body */
  /*  JsonObject request = new JsonObject()
        .put(SEARCH_TYPE,
            RESPONSE_FILTER.concat(SEARCH_TYPE_ATTRIBUTE).concat(SEARCH_TYPE_GEO)
                .concat(SEARCH_TYPE_TEXT))
        .put(PROPERTY, new JsonArray().add(TAGS).add(TYPE))
        .put(VALUE, new JsonArray().add(new JsonArray().add(TAG_AQM))
            .add(new JsonArray().add("iudx:Resource").add("climo")))
        .put(FILTER, new JsonArray().add(ID).add(TAGS).add(LOCATION))
        .put(COORDINATES_KEY, new JsonArray().add(73.927285).add(18.502712)).put(MAX_DISTANCE, 5000)
        .put(GEOMETRY, POINT).put(GEORELATION, INTERSECTS).put(GEOPROPERTY, LOCATION)
        .put(Q_VALUE, "aqm_test_4");

    Set<String> attrs = new HashSet<>();
    attrs.add(ID);
    attrs.add(TAGS);
    attrs.add(LOCATION);

    dbService.searchQuery(request, testContext.succeeding(response -> {
      Set<String> resAttrs = new HashSet<>();
      for (Object obj : response.getJsonArray(RESULT)) {
        JsonObject jsonObj = (JsonObject) obj;
        if (resAttrs != attrs) {
          resAttrs = jsonObj.fieldNames();
        }
      }
      Set<String> finalResAttrs = resAttrs;
      testContext.verify(() -> {
        assertEquals(73.927,
            response.getJsonArray(RESULT).getJsonObject(0).getJsonObject(LOCATION)
                .getJsonObject(GEOMETRY).getJsonArray(COORDINATES_KEY).getDouble(0));
        assertEquals(attrs, finalResAttrs);
        testContext.completeNow();
      });
    }));
  }


 @Test
  @DisplayName("List Resource Relationship")
  void listResourceRelationshipTest(VertxTestContext testContext) {

    /* Constructing request Json Body */
    /*JsonObject request = new JsonObject().put(ID,
        "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/aqm-bosch-climo")
        .put(RELATIONSHIP, RESOURCE);

    dbService.listRelationship(request, testContext.succeeding(response -> {

      testContext.verify(() -> {
        assertEquals(73.927,
            response.getJsonArray(RESULT).getJsonObject(0).getJsonObject(LOCATION)
                .getJsonObject(GEOMETRY).getJsonArray(COORDINATES_KEY).getDouble(0));
        testContext.completeNow();
      });
    }));
  }


  @Test
   @DisplayName("List ResourceGroup Relationship")
   void listResourceGroupRelationshipTest(VertxTestContext testContext) {

     /* Constructing request Json Body */
  /*  JsonObject request = new JsonObject()
        .put(ID,
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86"
                + "/rs.iudx.io/aqm-bosch-climo/Sadhu_Wasvani_Square_24")
        .put(RELATIONSHIP, RESOURCE_GRP);

    dbService.listRelationship(request, testContext.succeeding(response -> {

      testContext.verify(() -> {
        assertEquals(
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/aqm-bosch-climo",
            response.getJsonArray(RESULT).getJsonObject(0).getString(ID));
        testContext.completeNow();
      });
    }));
  }

 @Test
  @DisplayName("List Provider Relationship for resourceId")
  void listProviderResourceIdRelationshipTest(VertxTestContext testContext) {

  /* Constructing request Json Body */

  /* JsonObject request = new JsonObject()
        .put(ID,
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86"
                + "/rs.iudx.io/aqm-bosch-climo/Sadhu_Wasvani_Square_24")
        .put(RELATIONSHIP, PROVIDER);

    dbService.listRelationship(request, testContext.succeeding(response -> {

      testContext.verify(() -> {
      assertEquals("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86",
            response.getJsonArray(RESULT).getJsonObject(0).getString(ID));
        testContext.completeNow();
      });
    }));
  }*/


  /* @Test
 @DisplayName("List Provider Relationship for resourceGroupId")
 void listProviderResourceGroupIdRelationshipTest(VertxTestContext testContext) {

   /* Constructing request Json Body */
     /* JsonObject request = new JsonObject()
        .put(ID,
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86"
                + "/rs.iudx.io/aqm-bosch-climo")
        .put(RELATIONSHIP, PROVIDER);

    dbService.listRelationship(request, testContext.succeeding(response -> {

      testContext.verify(() -> {
        assertEquals("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86",
            response.getJsonArray(RESULT).getJsonObject(0).getString(ID));
        testContext.completeNow();
      });
    }));
  }


 @Test
  @DisplayName("List ResourceServer Relationship for resourceId")
  void listResourceServerResourceIdRelationshipTest(VertxTestContext testContext) {

    /* Constructing request Json Body */
   /* JsonObject request = new JsonObject()
        .put(ID,
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86"
                + "/rs.iudx.io/aqm-bosch-climo/Sadhu_Wasvani_Square_24")
        .put(RELATIONSHIP, RESOURCE_SVR);

    dbService.listRelationship(request, testContext.succeeding(response -> {

      testContext.verify(() -> {
        assertEquals("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io",
            response.getJsonArray(RESULT).getJsonObject(0).getString(ID));
        testContext.completeNow();
      });
    }));
  }

 @Test
  @DisplayName("List ResourceServer Relationship for resourceGroupId")
  void listResourceServerResourceGroupIdRelationshipTest(VertxTestContext testContext) {

    /* Constructing request Json Body */
   /* JsonObject request = new JsonObject()
        .put(ID,
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/aqm-bosch-climo")
        .put(RELATIONSHIP, RESOURCE_SVR);

    dbService.listRelationship(request, testContext.succeeding(response -> {

      testContext.verify(() -> {
        assertEquals("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io",
            response.getJsonArray(RESULT).getJsonObject(0).getString(ID));
        testContext.completeNow();
      });
    }));
  }

 @Test
  @DisplayName("List Type Relationship for resourceId")
  void listTypeResourceIdRelationshipTest(VertxTestContext testContext) {

    /* Constructing request Json Body */
   /* JsonObject request = new JsonObject()
        .put(ID,
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86"
                + "/rs.iudx.io/aqm-bosch-climo/aqm_test_2")
        .put(RELATIONSHIP, TYPE);

    dbService.listRelationship(request, testContext.succeeding(response -> {

      testContext.verify(() -> {
        assertEquals("iudx:Resource",
            response.getJsonArray(RESULT).getJsonObject(0).getJsonArray(TYPE_KEY).getString(0));
        testContext.completeNow();
      });
    }));
  }

  @Test
  @DisplayName("List ResourceServer Relationship for resourceGroupId")
  void listTypeResourceGroupIdRelationshipTest(VertxTestContext testContext) {

    /* Constructing request Json Body */
  /*  JsonObject request = new JsonObject()
        .put(ID,
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86"
                + "/rs.iudx.io/aqm-bosch-climo")
        .put(RELATIONSHIP, TYPE);

    dbService.listRelationship(request, testContext.succeeding(response -> {

      testContext.verify(() -> {
        assertEquals("iudx:ResourceGroup",
            response.getJsonArray(RESULT).getJsonObject(0).getJsonArray(TYPE_KEY).getString(0));
        testContext.completeNow();
      });
    }));
  }
*/
/*@Test
@Order(7)
@DisplayName("Relationship search Provider")
void listRelSearchProviderTest(VertxTestContext testContext) {

  /* Constructing request Json Body */
/*  JsonObject request =
          new JsonObject()
                  .put(RELATIONSHIP, new JsonArray().add(PROVIDER.concat(".").concat(NAME)))
                  .put(VALUE, new JsonArray().add(new JsonArray().add("IUDXAdmin")));

  dbService.relSearch(request, testContext.succeeding(response -> {

    testContext.verify(() -> {
      assertEquals(TYPE_SUCCESS,
              response.getString(TYPE));
      testContext.completeNow();
    });
  }));
}
  @Test
  @Order(8)
  @DisplayName("Relationship search ResourceGroup")
  void listRelSearchResourceGroupTest(VertxTestContext testContext) {

    JsonObject request = new JsonObject()
            .put(RELATIONSHIP, new JsonArray().add("resourceGroup.accessObjectInfo.accessObjectType"))
            .put(VALUE, new JsonArray().add(new JsonArray().add("openAPI")));

    dbService.relSearch(request, testContext.succeeding(response -> {

      testContext.verify(() -> {
        LOGGER.debug(response);
        for (int i=0; i<response.getJsonArray(RESULT).size(); i++) {
          if (response.getJsonArray(RESULT).getJsonObject(i).containsKey("resourceGroup")) {
            assertEquals("datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs.iudx.io/aqm-bosch-climo",
                    response.getJsonArray(RESULT).getJsonObject(i).getString("resourceGroup"));
          }
          testContext.completeNow();
        }
      });
    }));
  }*/



}

