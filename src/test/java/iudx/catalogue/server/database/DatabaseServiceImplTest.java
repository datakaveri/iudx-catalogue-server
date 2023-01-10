package iudx.catalogue.server.database;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.catalogue.server.Configuration;
import iudx.catalogue.server.geocoding.GeocodingService;
import iudx.catalogue.server.geocoding.GeocodingServiceImpl;
import iudx.catalogue.server.nlpsearch.NLPSearchService;
import iudx.catalogue.server.validator.ValidatorServiceImpl;
import jdk.jfr.Description;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.Timer;

import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.util.Constants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static iudx.catalogue.server.util.Constants.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class DatabaseServiceImplTest {
  private static final Logger LOGGER = LogManager.getLogger(DatabaseServiceTest.class);
  private static DatabaseService dbService;
  private static Vertx vertxObj;
  private static ElasticClient client;

  private static String docIndex;
  private static String ratingIndex;
  private static String mlayerInstanceIndex;
  private static String databaseIP;
  private static int databasePort;
  private static String databaseUser;
  private static String databasePassword;
  private String ratingID;
  private static Configuration config;
  private static WebClient webClient;

  private static JsonArray optionalModules;

  DatabaseServiceImpl databaseService;
  @Mock Handler<AsyncResult<JsonObject>> handler;
  @Mock Handler<AsyncResult<Boolean>> boolHandler;
  @Mock AsyncResult<JsonObject> asyncResult;
  @Mock NLPSearchService nlpService;
  @Mock GeocodingService geoService;
  @Mock AsyncResult<String> asyncResultString;
  @Mock Future<Boolean> jsonObjectFuture;
  @Mock AsyncResult<Boolean> asyncResultBoolean;
  @Mock Throwable throwable;

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
      dbService = new DatabaseServiceImpl(client, docIndex, ratingIndex, mlayerInstanceIndex,nlpService, geoService);
    } else {
      dbService = new DatabaseServiceImpl(client, docIndex, ratingIndex,mlayerInstanceIndex);
    }

    testContext.completeNow();
  }

  @Test
  @Description("test nlpSearchQuery when handler succeeded ")
  public void testNlpSerchQuery(VertxTestContext vertxTestContext) {
    databaseService = new DatabaseServiceImpl(client, docIndex, ratingIndex,mlayerInstanceIndex);
    JsonArray request = new JsonArray();
    JsonArray jsonArray = new JsonArray();
    request.add(0, jsonArray);
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    when(asyncResult.succeeded()).thenReturn(true);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(DatabaseServiceImpl.client)
        .scriptSearch(any(), any());
    databaseService.nlpSearchQuery(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(DatabaseServiceImpl.client, times(1)).scriptSearch(any(), any());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description("test nlpSearchQuery when handler failed ")
  public void testNlpSerchQueryFailed(VertxTestContext vertxTestContext) {
    databaseService = new DatabaseServiceImpl(client, docIndex, ratingIndex,mlayerInstanceIndex);
    JsonArray request = new JsonArray();
    JsonArray jsonArray = new JsonArray();
    request.add(0, jsonArray);
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("dummy");
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(DatabaseServiceImpl.client)
        .scriptSearch(any(), any());
    databaseService.nlpSearchQuery(
        request,
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).scriptSearch(any(), any());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description("test nlpSearchLocationQuery when handler succeded ")
  public void testSearchLocationQuery(VertxTestContext vertxTestContext) {
    databaseService = new DatabaseServiceImpl(client, docIndex, ratingIndex, mlayerInstanceIndex);
    JsonArray request = new JsonArray();
    JsonArray jsonArray = new JsonArray().add(new JsonObject().put("country","India"));
    JsonObject jo = new JsonObject().put(RESULTS, jsonArray);
    request.add(0, jsonArray);
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    doAnswer(Answer -> Future.succeededFuture(jo))
        .when(DatabaseServiceImpl.client)
            .scriptLocationSearch(any(),any());

    databaseService.nlpSearchLocationQuery(
        request,
        jo,
        handler -> {
          if (handler.succeeded()) {
            verify(DatabaseServiceImpl.client, times(1)).scriptLocationSearch(any(), any());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description("test getItem when handler failed ")
  public void testGetItemFailed(VertxTestContext vertxTestContext) {
    databaseService = new DatabaseServiceImpl(client, docIndex, ratingIndex,mlayerInstanceIndex);
    JsonObject request = new JsonObject();
    request.put(ID, "dummyid");
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    when(asyncResult.succeeded()).thenReturn(false);
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
    databaseService.getItem(
        request,
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).searchAsync(any(), any(), any());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description("test getItem when handler succeeded ")
  public void testGetItem(VertxTestContext vertxTestContext) {
    databaseService = new DatabaseServiceImpl(client, docIndex, ratingIndex,mlayerInstanceIndex);
    JsonObject request = new JsonObject();
    request.put(ID, "dummyid");
    DatabaseServiceImpl.client = mock(ElasticClient.class);
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
    databaseService.getItem(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(DatabaseServiceImpl.client, times(1)).searchAsync(anyString(), any(), any());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description("test listItem when handler succeeded ")
  public void testListItem(VertxTestContext vertxTestContext) {
    databaseService = new DatabaseServiceImpl(client, docIndex, ratingIndex,mlayerInstanceIndex);
    JsonObject request = new JsonObject();
    request.put(ITEM_TYPE, TAGS);
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    when(asyncResult.succeeded()).thenReturn(true);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(DatabaseServiceImpl.client)
        .listAggregationAsync(any(), any());
    databaseService.listItems(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(DatabaseServiceImpl.client, times(1)).listAggregationAsync(any(), any());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("fail");
          }
        });
  }

  @Test
  @Description("test listItem method")
  public void testListItemFailed(VertxTestContext vertxTestContext) {
    databaseService = new DatabaseServiceImpl(client, docIndex, ratingIndex,mlayerInstanceIndex);
    JsonObject request = new JsonObject();
    request.put(ITEM_TYPE, TAGS);
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    when(asyncResult.succeeded()).thenReturn(false);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(DatabaseServiceImpl.client)
        .listAggregationAsync(any(), any());
    databaseService.listItems(
        request,
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).listAggregationAsync(anyString(), any());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description("test countQuery when method returns Null")
  public void testCountQueryHandler(VertxTestContext vertxTestContext) {
    databaseService = new DatabaseServiceImpl(client, docIndex, ratingIndex,mlayerInstanceIndex);
    JsonObject request = new JsonObject();
    // request.put(SEARCH_TYPE,GEOSEARCH_REGEX);
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    assertNull(databaseService.countQuery(request, handler));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("test countQuery when handler succeeded")
  public void testCountQuerySuceeded(VertxTestContext vertxTestContext) {
    databaseService = new DatabaseServiceImpl(client, docIndex, ratingIndex,mlayerInstanceIndex);
    JsonObject request = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    request.put(SEARCH_TYPE, GEOSEARCH_REGEX);
    request.put(GEOMETRY, BBOX);
    request.put(GEORELATION, "dummy");
    request.put(COORDINATES_KEY, jsonArray);
    DatabaseServiceImpl.client = mock(ElasticClient.class);
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
        .countAsync(any(), any(), any());
    databaseService.countQuery(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(DatabaseServiceImpl.client, times(1)).countAsync(anyString(), any(), any());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description("test verifyInstance when handler failed")
  public void testVerifyInstanceFailed(VertxTestContext vertxTestContext) {
    databaseService = new DatabaseServiceImpl(client, docIndex, ratingIndex,mlayerInstanceIndex);
    String instanceId = "dummy";
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    when(asyncResult.failed()).thenReturn(true);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("dummy");
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
    databaseService
        .verifyInstance(instanceId)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                databaseService.verifyInstance(instanceId);
                verify(DatabaseServiceImpl.client, times(2)).searchAsync(any(), any(), any());
                assertEquals(TYPE_INTERNAL_SERVER_ERROR, handler.cause().getMessage());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow("Fail");
              }
            });
  }

  @Test
  @Description("test verifyInstance when Total hits is 0")
  public void testVerifyInstance0Hits(VertxTestContext vertxTestContext) {
    databaseService = new DatabaseServiceImpl(client, docIndex, ratingIndex,mlayerInstanceIndex);
    String instanceId = "dummy";
    JsonObject json = new JsonObject();
    json.put(TOTAL_HITS, 0);
    DatabaseServiceImpl.client = mock(ElasticClient.class);
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
        .searchAsync(any(), any(), any());

    databaseService
        .verifyInstance(instanceId)
        .onComplete(
            boolHandler -> {
              if (boolHandler.failed()) {
                assertEquals(json, asyncResult.result());
                verify(DatabaseServiceImpl.client, times(1)).searchAsync(anyString(), any(), any());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow("Fail");
              }
            });
  }

  @Test
  @Description("test verifyInstance when Total hits is 0")
  public void testVerifyInstance(VertxTestContext vertxTestContext) {
    databaseService = new DatabaseServiceImpl(client, docIndex, ratingIndex,mlayerInstanceIndex);
    String instanceId = "dummy";
    JsonObject json = new JsonObject();
    json.put(TOTAL_HITS, 100);
    DatabaseServiceImpl.client = mock(ElasticClient.class);
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
        .searchAsync(any(), any(), any());
    databaseService
        .verifyInstance(instanceId)
        .onComplete(
            boolHandler -> {
              if (boolHandler.succeeded()) {
                assertEquals(json, asyncResult.result());
                verify(DatabaseServiceImpl.client, times(1)).searchAsync(anyString(), any(), any());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow("Fail");
              }
            });
  }

  @Test
  @Description("test updateItem method")
  public void testListRelationship(VertxTestContext vertxTestContext) {
    databaseService = new DatabaseServiceImpl(client, docIndex, ratingIndex,mlayerInstanceIndex);
    JsonObject json = new JsonObject();
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    when(asyncResult.succeeded()).thenReturn(false);
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
    databaseService.listRelationship(
        json,
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).searchAsync(any(), any(), any());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description("test updateItem method")
  public void testRelSearch(VertxTestContext vertxTestContext) {
    databaseService = new DatabaseServiceImpl(client, docIndex, ratingIndex,mlayerInstanceIndex);
    JsonObject json = new JsonObject();

    JsonArray jsonArray = new JsonArray();
    JsonArray jsonArray2 = new JsonArray();
    JsonArray jsonArray3 = new JsonArray();
    jsonArray3.add(0, "dummy");
    jsonArray2.add(0, jsonArray3);
    jsonArray.add(0, "dummy");
    json.put(RELATIONSHIP, jsonArray);
    json.put(VALUE, jsonArray2);
    assertNull(databaseService.relSearch(json, handler));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("test updateItem method when checkRes handler failed")
  public void testCreateRating(VertxTestContext vertxTestContext) {
    databaseService = new DatabaseServiceImpl(client, docIndex, ratingIndex,mlayerInstanceIndex);
    JsonObject json = new JsonObject();
    json.put("ratingID", "dummy");
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    when(asyncResult.failed()).thenReturn(true);
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
    databaseService.createRating(
        json,
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).searchAsync(any(), any(), any());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description("test updateItem method when postRes handler failed")
  public void testCreateRatingPostRes(VertxTestContext vertxTestContext) {
    databaseService = new DatabaseServiceImpl(client, docIndex, ratingIndex,mlayerInstanceIndex);
    JsonObject json = new JsonObject();
    json.put("ratingID", "dummy");
    json.put(TOTAL_HITS, 0);
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    when(asyncResult.failed()).thenReturn(false);
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

    databaseService.createRating(
        json,
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).docPostAsync(any(), any(), any());
            verify(DatabaseServiceImpl.client, times(1)).searchAsync(any(), any(), any());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description("test getRatings method when getRes handler succeeded ")
  public void testGetRatings(VertxTestContext vertxTestContext) {
    databaseService = new DatabaseServiceImpl(client, docIndex, ratingIndex,mlayerInstanceIndex);
    JsonObject json = new JsonObject();
    json.put(ID, "dummyId");
    json.put(TYPE, "average");

    DatabaseServiceImpl.client = mock(ElasticClient.class);
    when(asyncResult.succeeded()).thenReturn(true);
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
        .ratingAggregationAsync(any(), any(), any());
    databaseService.getRatings(
        json,
        handler -> {
          if (handler.succeeded()) {
            verify(DatabaseServiceImpl.client, times(1))
                .ratingAggregationAsync(any(), any(), any());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description("test getRatings method when getRes handler succeeded ")
  public void testUpdateRating(VertxTestContext vertxTestContext) {
    databaseService = new DatabaseServiceImpl(client, docIndex, ratingIndex,mlayerInstanceIndex);
    JsonObject json = new JsonObject();
    json.put("ratingID", "dummyId");
    json.put(TYPE, "average");
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    when(asyncResult.failed()).thenReturn(true);
    when(asyncResult.cause()).thenReturn(throwable);
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
    databaseService.updateRating(
        json,
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).searchGetId(any(), any(), any());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description("test createitem method when handler failed")
  public void testCreateItem(VertxTestContext vertxTestContext) {
    databaseService = new DatabaseServiceImpl(client, docIndex, ratingIndex,mlayerInstanceIndex);
    JsonObject json = new JsonObject();
    json.put("id", "dummyId");
    json.put(INSTANCE, "average");

    DatabaseServiceImpl.client = mock(ElasticClient.class);
    when(asyncResult.failed()).thenReturn(true);
    when(asyncResult.cause()).thenReturn(throwable);
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

    databaseService.createItem(
        json,
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).searchAsync(any(), any(), any());
            vertxTestContext.completeNow();
          } else vertxTestContext.failNow("Fail");
        });
  }

  @Test
  @Description("test relSearch method when typeValue equals ITEM_TYPE_RESOURCE")
  public void testRelSearchResource(VertxTestContext vertxTestContext) {
    databaseService = new DatabaseServiceImpl(client, docIndex, ratingIndex,mlayerInstanceIndex);
    JsonObject json = new JsonObject();
    json.put(LIMIT, 100);
    json.put(OFFSET, 100);
    JsonObject jsonObject = new JsonObject();
    JsonObject jsonObject2 = new JsonObject();
    jsonObject2.put("dummy key", "dummy value");
    JsonArray jsonArray4 = new JsonArray();
    jsonArray4.add(jsonObject2);
    jsonObject.put(RESULT, jsonArray4);
    JsonArray jsonArray = new JsonArray();
    JsonArray jsonArray2 = new JsonArray();
    JsonArray jsonArray3 = new JsonArray();
    jsonArray3.add(0, "dummy");
    jsonArray2.add(0, jsonArray3);
    jsonArray.add(0, "RESOURCE.RESOURCE");
    json.put(RELATIONSHIP, jsonArray).put(VALUE, jsonArray2);
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(jsonObject);

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
    databaseService.relSearch(
        json,
        handler -> {
          if (handler.succeeded()) {
            verify(DatabaseServiceImpl.client, times(2)).searchAsync(any(), any(), any());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description("test relSearch method when typeValue equals Random")
  public void testRelSearchRandom(VertxTestContext vertxTestContext) {
    databaseService = new DatabaseServiceImpl(client, docIndex, ratingIndex,mlayerInstanceIndex);
    JsonObject json = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    JsonArray jsonArray2 = new JsonArray();
    JsonArray jsonArray3 = new JsonArray();
    jsonArray3.add(0, "dummy");
    jsonArray2.add(0, jsonArray3);
    jsonArray.add(0, "abcd.abcd");
    json.put(RELATIONSHIP, jsonArray).put(VALUE, jsonArray2);
    assertNull(databaseService.relSearch(json, handler));
    vertxTestContext.completeNow();
  }

  @Test
  @Description(
      "test relSearch method when typeValue equals ITEM_TYPE_RESOURCE and searchRes failed")
  public void testRelSearchResourceFailed(VertxTestContext vertxTestContext) {
    databaseService = new DatabaseServiceImpl(client, docIndex, ratingIndex,mlayerInstanceIndex);
    JsonObject json = new JsonObject();
    JsonObject jsonObject = new JsonObject();
    JsonObject jsonObject2 = new JsonObject();
    jsonObject2.put("dummy key", "dummy value");
    JsonArray jsonArray4 = new JsonArray();
    jsonArray4.add(jsonObject2);
    jsonObject.put(RESULT, jsonArray4);
    JsonArray jsonArray = new JsonArray();
    JsonArray jsonArray2 = new JsonArray();
    JsonArray jsonArray3 = new JsonArray();
    jsonArray3.add(0, "dummy");
    jsonArray2.add(0, jsonArray3);
    jsonArray.add(0, "RESOURCE.RESOURCE");
    json.put(RELATIONSHIP, jsonArray).put(VALUE, jsonArray2);
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("dummy");

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
    databaseService.relSearch(
        json,
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).searchAsync(any(), any(), any());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description("test deleteRating method with handler failure ")
  public void testDeleteRating(VertxTestContext vertxTestContext) {
    databaseService = new DatabaseServiceImpl(client, docIndex, ratingIndex,mlayerInstanceIndex);
    JsonObject json = new JsonObject();
    json.put("ratingID", "dummy");

    json.put(TOTAL_HITS, 0);
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    when(asyncResult.failed()).thenReturn(true);
    when(asyncResult.cause()).thenReturn(throwable);

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
    databaseService.deleteRating(
        json,
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).searchGetId(any(), any(), any());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description("test deleteRating method with handler failure ")
  public void testDeleteRatingfailure(VertxTestContext vertxTestContext) {
    databaseService = new DatabaseServiceImpl(client, docIndex, ratingIndex,mlayerInstanceIndex);
    JsonObject json = new JsonObject();
    json.put("ratingID", "dummy");
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(0, "dummy");
    json.put(RESULTS, jsonArray);

    json.put(TOTAL_HITS, 1);
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    when(asyncResult.failed()).thenReturn(false);
    when(asyncResult.result()).thenReturn(json);
    when(asyncResult.cause()).thenReturn(throwable);

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
    databaseService.deleteRating(
        json,
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).docDelAsync(any(), any(), any());
            verify(DatabaseServiceImpl.client, times(1)).searchGetId(any(), any(), any());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description("test deleteRating method with handler failure ")
  public void testDeleteRatingFailure(VertxTestContext vertxTestContext) {
    databaseService = new DatabaseServiceImpl(client);
    JsonObject json = new JsonObject();
    json.put("ratingID", "dummy");
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(0, "dummy");
    json.put(RESULTS, jsonArray);

    json.put(TOTAL_HITS, 1);
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    when(asyncResult.failed()).thenReturn(false);
    when(asyncResult.result()).thenReturn(json);
    when(asyncResult.cause()).thenReturn(throwable);

    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(3)).handle(asyncResult);
                return null;
              }
            })
        .when(DatabaseServiceImpl.client)
        .docPutAsync(any(), any(), any(), any());

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

    databaseService.updateRating(
        json,
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).searchGetId(any(), any(), any());
            verify(DatabaseServiceImpl.client, times(1)).docPutAsync(any(), any(), any(), any());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description("test getRatings method when getRes handler failed")
  public void testGetRatingsFailed2(VertxTestContext vertxTestContext) {
    databaseService =
        new DatabaseServiceImpl(client, docIndex, ratingIndex, mlayerInstanceIndex,nlpService, geoService);
    JsonObject json = new JsonObject();
    json.put("ratingID", "dummy");
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(0, "dummy");
    //   json.put(RESULTS,jsonArray);

    json.put(TOTAL_HITS, 1);
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(throwable);

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

    databaseService.getRatings(
        json,
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).searchAsync(any(), any(), any());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description("test searchQuery method when handler failed")
  public void testSearchQuery(VertxTestContext vertxTestContext) {
    databaseService =
        new DatabaseServiceImpl(client, docIndex, ratingIndex,mlayerInstanceIndex, nlpService, geoService);
    JsonObject json = new JsonObject();
    json.put(SEARCH, false);
    json.put(SEARCH_TYPE, ATTRIBUTE_SEARCH_REGEX);
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("dummy");
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
    databaseService.searchQuery(
        json,
        handler -> {
          if (handler.failed()) {
            assertEquals("dummy", asyncResult.cause().getMessage());
            verify(DatabaseServiceImpl.client, times(1)).searchAsync(any(), any(), any());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description("test countAsync method when handler failed")
  public void testCountQuery(VertxTestContext vertxTestContext) {
    databaseService =
        new DatabaseServiceImpl(client, docIndex, ratingIndex, mlayerInstanceIndex,nlpService, geoService);
    JsonObject json = new JsonObject();
    json.put(SEARCH, false);
    json.put(SEARCH_TYPE, ATTRIBUTE_SEARCH_REGEX);
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    when(asyncResult.succeeded()).thenReturn(false);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("dummy");
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(DatabaseServiceImpl.client)
        .countAsync(any(), any(), any());
    databaseService.countQuery(
        json,
        handler -> {
          if (handler.failed()) {
            assertEquals("dummy", asyncResult.cause().getMessage());
            verify(DatabaseServiceImpl.client, times(1)).countAsync(any(), any(), any());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description("test listRelationship method when handler succeeded")
  public void testListRealtionship(VertxTestContext vertxTestContext) {
    databaseService =
        new DatabaseServiceImpl(client, docIndex, ratingIndex, mlayerInstanceIndex,nlpService, geoService);
    JsonObject json = new JsonObject();
    json.put(SEARCH, false);
    json.put(SEARCH_TYPE, ATTRIBUTE_SEARCH_REGEX);
    DatabaseServiceImpl.client = mock(ElasticClient.class);
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
    databaseService.listRelationship(
        json,
        handler -> {
          if (handler.succeeded()) {
            verify(DatabaseServiceImpl.client, times(1)).searchAsync(any(), any(), any());
            vertxTestContext.completeNow();

          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description("test listRelationship method when handler succeeded")
  public void testGetRatingsFailed(VertxTestContext vertxTestContext) {
    databaseService =
        new DatabaseServiceImpl(client, docIndex, ratingIndex, mlayerInstanceIndex,nlpService, geoService);
    JsonObject json = new JsonObject();
    json.put(ID, "dummy id");
    json.put(TYPE, "average");
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    when(asyncResult.succeeded()).thenReturn(false);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(DatabaseServiceImpl.client)
        .ratingAggregationAsync(any(), any(), any());
    databaseService.getRatings(
        json,
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1))
                .ratingAggregationAsync(any(), any(), any());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description("test createItem method when handler checkres failed")
  public void testCreateItemFailed(VertxTestContext vertxTestContext) {
    databaseService =
        new DatabaseServiceImpl(client, docIndex, ratingIndex, mlayerInstanceIndex,nlpService, geoService);
    JsonObject json = new JsonObject();
    json.put("id", "dummy id");
    json.put(TYPE, "average");
    String instanceId = "dummy";
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    when(asyncResult.failed()).thenReturn(true);
    when(asyncResult.cause()).thenReturn(throwable);
    when(throwable.getMessage()).thenReturn("dummy");

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
    databaseService.createItem(json, handler);
    databaseService
        .verifyInstance(instanceId)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                verify(DatabaseServiceImpl.client, times(2)).searchAsync(any(), any(), any());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow("Fail");
              }
            });
  }

  @Test
  @Description("test createItem method when checkRes handler succeeded")
  public void testCreateItemSucceeded(VertxTestContext vertxTestContext) {
    databaseService =
        new DatabaseServiceImpl(client, docIndex, ratingIndex, mlayerInstanceIndex,nlpService, geoService);
    JsonObject json = new JsonObject();
    json.put("id", "dummy id");
    json.put(TYPE, "average").put(TOTAL_HITS, 1);
    String instanceId = "dummy";
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    when(asyncResult.succeeded()).thenReturn(true);
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
        .searchAsync(any(), any(), any());
    databaseService.createItem(json, handler);
    databaseService
        .verifyInstance(instanceId)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                verify(DatabaseServiceImpl.client, times(2)).searchAsync(any(), any(), any());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow("Fail");
              }
            });
  }

  @Test
  @Description("test createItem method when handler succeeded and total_hits equals 0")
  public void testCreateItemHits0(VertxTestContext vertxTestContext) {
    databaseService =
        new DatabaseServiceImpl(client, docIndex, ratingIndex,mlayerInstanceIndex, nlpService, geoService);
    JsonObject json = new JsonObject();
    json.put("id", "dummy id").put(INSTANCE, "pune");
    json.put(TYPE, "average").put(TOTAL_HITS, 0);
    String instanceId = "pune";
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    when(asyncResult.failed()).thenReturn(false);
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
        .searchAsync(any(), any(), any());


    databaseService.createItem(json, handler);
    databaseService
        .verifyInstance(instanceId)
        .onComplete(
            handler -> {
              if (handler.failed()) {
                verify(DatabaseServiceImpl.client, times(2)).searchAsync(any(), any(), any());
                verify(nlpService, times(0)).getEmbedding(any(), any());
                verify(geoService, times(0)).geoSummarize(any(), any());
                verify(DatabaseServiceImpl.client, times(0)).docPostAsync(any(), any(), any());

                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow("Fail");
              }
            });
  }
}
