package iudx.catalogue.server.database;

import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.catalogue.server.Configuration;
import iudx.catalogue.server.geocoding.GeocodingService;
import iudx.catalogue.server.nlpsearch.NLPSearchService;
import jdk.jfr.Description;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.List;
import java.util.stream.Stream;

import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.mlayer.util.Constants.*;
import static iudx.catalogue.server.util.Constants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
  private static String mlayerDomainIndex;
  private static String databaseIP;
  private static int databasePort;
  private static String databaseUser;
  private static String databasePassword;

  private static JsonArray optionalModules;

  @Mock Handler<AsyncResult<JsonObject>> handler;
  @Mock AsyncResult<JsonObject> asyncResult;
  @Mock NLPSearchService nlpService;
  @Mock GeocodingService geoService;

  @Mock Throwable throwable;
  @Mock AsyncResult<Boolean> instanceHandler;

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
    mlayerInstanceIndex = dbConfig.getString(MLAYER_INSTANCE_INDEX);
    mlayerDomainIndex = dbConfig.getString(MLAYER_DOMAIN_INDEX);
    optionalModules = dbConfig.getJsonArray(OPTIONAL_MODULES);

    client = new ElasticClient(databaseIP, databasePort, docIndex, databaseUser, databasePassword);

    if (optionalModules.contains(NLPSEARCH_PACKAGE_NAME)
        && optionalModules.contains(GEOCODING_PACKAGE_NAME)) {
      NLPSearchService nlpService = NLPSearchService.createProxy(vertx, NLP_SERVICE_ADDRESS);
      GeocodingService geoService = GeocodingService.createProxy(vertx, GEOCODING_SERVICE_ADDRESS);
      dbService =
          new DatabaseServiceImpl(
              client,
              docIndex,
              ratingIndex,
              mlayerInstanceIndex,
              mlayerDomainIndex,
              nlpService,
              geoService);
    } else {
      dbService =
          new DatabaseServiceImpl(
              client, docIndex, ratingIndex, mlayerInstanceIndex, mlayerDomainIndex);
    }

    testContext.completeNow();
  }

  @Test
  @Description("test nlpSearchQuery when handler succeeded ")
  public void testNlpSerchQuery(VertxTestContext vertxTestContext) {
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
    dbService.nlpSearchQuery(
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
    dbService.nlpSearchQuery(
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
    JsonArray request = new JsonArray();
    JsonArray jsonArray = new JsonArray().add(new JsonObject().put("country", "India"));
    JsonObject jo = new JsonObject().put(RESULTS, jsonArray);
    request.add(0, jsonArray);
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    doAnswer(Answer -> Future.succeededFuture(jo))
        .when(DatabaseServiceImpl.client)
        .scriptLocationSearch(any(), any());

    dbService.nlpSearchLocationQuery(
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
    dbService.getItem(
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
    dbService.getItem(
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
    dbService.listItems(
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
    dbService.listItems(
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
    JsonObject request = new JsonObject();
    // request.put(SEARCH_TYPE,GEOSEARCH_REGEX);
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    assertNull(dbService.countQuery(request, handler));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("test countQuery when handler succeeded")
  public void testCountQuerySuceeded(VertxTestContext vertxTestContext) {
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
    dbService.countQuery(
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
    DatabaseServiceImpl databaseService =
        new DatabaseServiceImpl(
            client, docIndex, ratingIndex, mlayerInstanceIndex, mlayerDomainIndex);
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
    DatabaseServiceImpl databaseService =
        new DatabaseServiceImpl(
            client, docIndex, ratingIndex, mlayerInstanceIndex, mlayerDomainIndex);
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
    DatabaseServiceImpl databaseService =
        new DatabaseServiceImpl(
            client, docIndex, ratingIndex, mlayerInstanceIndex, mlayerDomainIndex);
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
  public void testRelSearch(VertxTestContext vertxTestContext) {
    JsonObject json = new JsonObject();

    JsonArray jsonArray = new JsonArray();
    JsonArray jsonArray2 = new JsonArray();
    JsonArray jsonArray3 = new JsonArray();
    jsonArray3.add(0, "dummy");
    jsonArray2.add(0, jsonArray3);
    jsonArray.add(0, "dummy");
    json.put(RELATIONSHIP, jsonArray);
    json.put(VALUE, jsonArray2);
    assertNull(dbService.relSearch(json, handler));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("test updateItem method when checkRes handler failed")
  public void testCreateRating(VertxTestContext vertxTestContext) {
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
    dbService.createRating(
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

    dbService.createRating(
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
    JsonObject json = new JsonObject();
    json.put(ID, "dummyId");
    json.put(TYPE, "average");

    JsonObject jsonObjectMock = mock(JsonObject.class);
    JsonArray jsonArrayMock = mock(JsonArray.class);
    Stream streamMock = mock(Stream.class);
    when(jsonObjectMock.getJsonArray(RESULTS)).thenReturn(jsonArrayMock);
    when(jsonArrayMock.stream()).thenReturn(streamMock);
    when(streamMock.map(any())).thenReturn(streamMock);
    when(streamMock.collect(any()))
        .thenReturn(
            List.of(
                "b58da193-23d9-43eb-b98a-a103d4b6103c", "5b7556b5-0779-4c47-9cf2-3f209779aa22"));
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(jsonObjectMock);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock invocationOnMock)
                  throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) invocationOnMock.getArgument(2))
                    .handle(asyncResult);
                return null;
              }
            })
        .when(DatabaseServiceImpl.client)
        .searchAsync(anyString(), anyString(), any());
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
    dbService.getRatings(
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
    dbService.updateRating(
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

    dbService.createItem(
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
    dbService.relSearch(
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
    JsonObject json = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    JsonArray jsonArray2 = new JsonArray();
    JsonArray jsonArray3 = new JsonArray();
    jsonArray3.add(0, "dummy");
    jsonArray2.add(0, jsonArray3);
    jsonArray.add(0, "abcd.abcd");
    json.put(RELATIONSHIP, jsonArray).put(VALUE, jsonArray2);
    assertNull(dbService.relSearch(json, handler));
    vertxTestContext.completeNow();
  }

  @Test
  @Description(
      "test relSearch method when typeValue equals ITEM_TYPE_RESOURCE and searchRes failed")
  public void testRelSearchResourceFailed(VertxTestContext vertxTestContext) {
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
    dbService.relSearch(
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
    dbService.deleteRating(
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
    dbService.deleteRating(
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

    dbService.updateRating(
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

    dbService.getRatings(
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
    dbService.searchQuery(
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
    dbService.countQuery(
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

    JsonArray typeArray = new JsonArray();
    JsonObject jsonObject = new JsonObject().put(TYPE, typeArray);
    JsonArray resultArray = new JsonArray().add(jsonObject);
    JsonObject json =
        new JsonObject()
            .put("id", "dummy id")
            .put(TOTAL_HITS, 1)
            .put("results", resultArray)
            .put(RELATIONSHIP, "resources");
    json.put(SEARCH, false);
    json.put(SEARCH_TYPE, ATTRIBUTE_SEARCH_REGEX);
    DatabaseServiceImpl.client = mock(ElasticClient.class);
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
    dbService.listRelationship(
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
  @Description("test listRelationship method when item is not present")
  public void testListRealtionshipHits0(VertxTestContext vertxTestContext) {

    JsonArray typeArray = new JsonArray();
    JsonObject jsonObject = new JsonObject().put(TYPE, typeArray);
    JsonArray resultArray = new JsonArray().add(jsonObject);
    JsonObject json =
        new JsonObject()
            .put("id", "dummy id")
            .put(TOTAL_HITS, 0)
            .put("results", resultArray)
            .put(RELATIONSHIP, "resources");
    json.put(SEARCH, false);
    json.put(SEARCH_TYPE, ATTRIBUTE_SEARCH_REGEX);
    DatabaseServiceImpl.client = mock(ElasticClient.class);
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
    dbService.listRelationship(
        json,
        handler -> {
          if (handler.succeeded()) {
            verify(DatabaseServiceImpl.client, times(2)).searchAsync(any(), any(), any());
            vertxTestContext.failNow("Fail");

          } else {
            vertxTestContext.completeNow();
          }
        });
  }

  @Test
  @Description("test listRelationship method when item is type resource")
  public void testListRelationshipResource(VertxTestContext vertxTestContext) {
    DatabaseServiceImpl.client = mock(ElasticClient.class);

    JsonArray typeArray = new JsonArray();
    typeArray.add("iudx:Resource");
    JsonObject jsonObject = new JsonObject().put(TYPE, typeArray);
    JsonArray resultArray = new JsonArray().add(jsonObject);
    JsonObject json =
        new JsonObject()
            .put("id", "dummy id")
            .put(TOTAL_HITS, 1)
            .put("results", resultArray)
            .put(RELATIONSHIP, "resource");
    json.put(SEARCH, false);
    json.put(SEARCH_TYPE, ATTRIBUTE_SEARCH_REGEX);
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
    dbService.listRelationship(
        json,
        handler -> {
          if (handler.succeeded()) {
            verify(DatabaseServiceImpl.client, times(2)).searchAsync(any(), any(), any());
            vertxTestContext.failNow("Fail");

          } else {
            vertxTestContext.completeNow();
          }
        });
  }

  @Test
  @Description("test listRelationship method when item is resource Group")
  public void testListRelationResourceGroup(VertxTestContext vertxTestContext) {
    JsonArray typeArray = new JsonArray().add("iudx:ResourceGroup");
    JsonObject jsonObject = new JsonObject().put(TYPE, typeArray);
    JsonArray resultArray = new JsonArray().add(jsonObject);
    JsonObject json =
        new JsonObject()
            .put("id", "dummy id")
            .put(TOTAL_HITS, 1)
            .put("results", resultArray)
            .put(RELATIONSHIP, "resourceGroup");
    json.put(SEARCH, false);
    json.put(SEARCH_TYPE, ATTRIBUTE_SEARCH_REGEX);
    DatabaseServiceImpl.client = mock(ElasticClient.class);
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
    dbService.listRelationship(
        json,
        handler -> {
          if (handler.succeeded()) {
            verify(DatabaseServiceImpl.client, times(2)).searchAsync(any(), any(), any());
            vertxTestContext.failNow("Fail");

          } else {
            vertxTestContext.completeNow();
          }
        });
  }

  @Test
  @Description("test listRelationship method when item is resource server")
  public void testListRelationshipResourceServer(VertxTestContext vertxTestContext) {
    JsonArray typeArray = new JsonArray().add("iudx:ResourceServer");
    JsonObject jsonObject = new JsonObject().put(TYPE, typeArray);
    JsonArray resultArray = new JsonArray().add(jsonObject);
    JsonObject json =
        new JsonObject()
            .put("id", "dummy id")
            .put(TOTAL_HITS, 1)
            .put("results", resultArray)
            .put(RELATIONSHIP, "resourceServer");
    json.put(SEARCH, false);
    json.put(SEARCH_TYPE, ATTRIBUTE_SEARCH_REGEX);
    DatabaseServiceImpl.client = mock(ElasticClient.class);
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
    dbService.listRelationship(
        json,
        handler -> {
          if (handler.succeeded()) {
            verify(DatabaseServiceImpl.client, times(2)).searchAsync(any(), any(), any());
            vertxTestContext.failNow("Fail");

          } else {
            vertxTestContext.completeNow();
          }
        });
  }

  @Test
  @Description("test listRelationship method when item is resource server")
  public void testListRelationshipItemRS(VertxTestContext vertxTestContext) {
    JsonArray typeArray = new JsonArray().add("iudx:ResourceServer");
    JsonObject jsonObject = new JsonObject().put(TYPE, typeArray);
    JsonArray resultArray = new JsonArray().add(jsonObject);
    JsonObject json =
        new JsonObject()
            .put("id", "dummy id")
            .put(TOTAL_HITS, 1)
            .put("results", resultArray)
            .put(RELATIONSHIP, "provider");
    json.put(SEARCH, false);
    json.put(SEARCH_TYPE, ATTRIBUTE_SEARCH_REGEX);
    DatabaseServiceImpl.client = mock(ElasticClient.class);
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
    dbService.listRelationship(
        json,
        handler -> {
          if (handler.succeeded()) {
            verify(DatabaseServiceImpl.client, times(2)).searchAsync(any(), any(), any());
            vertxTestContext.failNow("Fail");

          } else {
            vertxTestContext.completeNow();
          }
        });
  }

  @Test
  @Description("test listRelationship method when item is resource")
  public void testListRelationshipItem2(VertxTestContext vertxTestContext) {
    JsonArray typeArray = new JsonArray().add("iudx:Resource");
    JsonObject jsonObject = new JsonObject().put(TYPE, typeArray).put("resourceGroup", "dummy id");
    JsonArray resultArray = new JsonArray().add(jsonObject);
    JsonObject json =
        new JsonObject()
            .put("id", "dummy id")
            .put(TOTAL_HITS, 1)
            .put("results", resultArray)
            .put(RELATIONSHIP, "resourceServer")
            .put("resourceServer", "dummy id")
            .put(ITEM_TYPE, "abc");
    json.put(SEARCH, false);
    json.put(SEARCH_TYPE, ATTRIBUTE_SEARCH_REGEX);
    DatabaseServiceImpl.client = mock(ElasticClient.class);
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
    dbService.listRelationship(
        json,
        handler -> {
          if (handler.succeeded()) {
            verify(DatabaseServiceImpl.client, times(3)).searchAsync(any(), any(), any());
            vertxTestContext.completeNow();

          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description("test listRelationship method when item and item type is resource server")
  public void testListRelationshipItem(VertxTestContext vertxTestContext) {
    JsonArray typeArray = new JsonArray().add("iudx:ResourceServer");
    JsonObject jsonObject =
        new JsonObject()
            .put(TYPE, typeArray)
            .put("resourceServer", "dummy id")
            .put("id", "dummy id");
    JsonArray resultArray = new JsonArray().add(jsonObject);
    JsonObject json =
        new JsonObject()
            .put("id", "dummy id")
            .put(TOTAL_HITS, 1)
            .put("results", resultArray)
            .put(RELATIONSHIP, "resource")
            .put("resourceServer", "dummy id")
            .put(ITEM_TYPE, "iudx:ResourceServer");
    json.put(SEARCH, false);
    json.put(SEARCH_TYPE, ATTRIBUTE_SEARCH_REGEX);
    DatabaseServiceImpl.client = mock(ElasticClient.class);
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
    dbService.listRelationship(
        json,
        handler -> {
          if (handler.succeeded()) {
            verify(DatabaseServiceImpl.client, times(3)).searchAsync(any(), any(), any());
            vertxTestContext.completeNow();

          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description("test listRelationship method when handler succeeded")
  public void testGetRatingsFailed(VertxTestContext vertxTestContext) {

    JsonObject json = new JsonObject();
    json.put(ID, "dummy id");
    json.put(TYPE, "average");
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    when(asyncResult.succeeded()).thenReturn(false);
    AsyncResult<JsonObject> asyncResult1 = mock(AsyncResult.class);
    JsonObject jsonObjectMock = mock(JsonObject.class);
    JsonArray jsonArrayMock = mock(JsonArray.class);
    Stream streamMock = mock(Stream.class);
    when(jsonObjectMock.getJsonArray(RESULTS)).thenReturn(jsonArrayMock);
    when(jsonArrayMock.stream()).thenReturn(streamMock);
    when(streamMock.map(any())).thenReturn(streamMock);
    when(streamMock.collect(any()))
        .thenReturn(
            List.of(
                "b58da193-23d9-43eb-b98a-a103d4b6103c", "5b7556b5-0779-4c47-9cf2-3f209779aa22"));
    when(asyncResult1.succeeded()).thenReturn(true);
    when(asyncResult1.result()).thenReturn(jsonObjectMock);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock invocationOnMock)
                  throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) invocationOnMock.getArgument(2))
                    .handle(asyncResult1);
                return null;
              }
            })
        .when(DatabaseServiceImpl.client)
        .searchAsync(anyString(), anyString(), any());
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
    dbService.getRatings(
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
    DatabaseServiceImpl databaseService =
        new DatabaseServiceImpl(
            client,
            docIndex,
            ratingIndex,
            mlayerInstanceIndex,
            mlayerDomainIndex,
            nlpService,
            geoService);
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
    dbService.createItem(json, handler);
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
    DatabaseServiceImpl databaseService =
        new DatabaseServiceImpl(
            client,
            docIndex,
            ratingIndex,
            mlayerInstanceIndex,
            mlayerDomainIndex,
            nlpService,
            geoService);
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
    dbService.createItem(json, handler);
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
    DatabaseServiceImpl databaseService =
        new DatabaseServiceImpl(
            client,
            docIndex,
            ratingIndex,
            mlayerInstanceIndex,
            mlayerDomainIndex,
            nlpService,
            geoService);
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

    dbService.createItem(json, handler);
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

  @Test
  @Description("test createMlayerInstance method when instance already exists")
  public void testCreateInstanceWhenInstanceExists(VertxTestContext testContext) {
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    JsonObject json = new JsonObject();
    JsonObject json2 = new JsonObject();
    json2.put(INSTANCE_ID, "dummy id");
    JsonArray jsonArray = new JsonArray().add(json2);
    json.put(INSTANCE_ID, "dummy")
        .put(TOTAL_HITS, 1)
        .put(MLAYER_ID, "dummy")
        .put(RESULTS, jsonArray);
    json.put(MLAYER_INSTANCE_INDEX, "dummy");
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
    dbService.createMlayerInstance(
        json,
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).searchAsync(any(), any(), any());
            testContext.completeNow();
          } else {
            testContext.failNow("fail");
          }
        });
  }

  @Test
  @Description("test createMlayerInstance method when the instance is created")
  public void testCreateMlayerInstance(VertxTestContext testContext) {

    DatabaseServiceImpl.client = mock(ElasticClient.class);
    JsonObject json = new JsonObject();
    json.put(INSTANCE_ID, "dummy").put(TOTAL_HITS, 0).put(MLAYER_ID, "dummy");
    json.put(MLAYER_INSTANCE_INDEX, "dummy");
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
    dbService.createMlayerInstance(
        json,
        handler -> {
          if (handler.succeeded()) {
            verify(DatabaseServiceImpl.client, times(1)).searchAsync(any(), any(), any());
            verify(DatabaseServiceImpl.client, times(1)).docPostAsync(any(), any(), any());
            testContext.completeNow();
          } else {
            testContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description("test createMlayerInstance method when instance creation fails")
  public void testCreateMlayerInstanceFailure(VertxTestContext testContext) {
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    JsonObject json = new JsonObject();
    json.put(INSTANCE_ID, "dummy").put(TOTAL_HITS, 0).put(MLAYER_ID, "dummy");
    json.put(MLAYER_INSTANCE_INDEX, "dummy");
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
    dbService.createMlayerInstance(
        json,
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).searchAsync(any(), any(), any());
            testContext.completeNow();

          } else {
            testContext.failNow("fail");
          }
        });
  }

  @Test
  @Description("test createMlayerInstance method when docPostAsync fails")
  public void testCreateMlayerInstanceDocPostAsyncFailure(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    when(asyncResult.failed()).thenReturn(false);
    when(asyncResult.result()).thenReturn(request);
    request.put(INSTANCE_ID, "dummy").put(MLAYER_ID, "dummy").put(TOTAL_HITS, 0);
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
    dbService.createMlayerInstance(
        request,
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).docPostAsync(any(), any(), any());
            verify(DatabaseServiceImpl.client, times(1)).searchAsync(any(), any(), any());

            testContext.completeNow();

          } else {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Description("test getMlayerInstance method when the DB Request is Successful")
  public void testGetMlayerInstance(VertxTestContext testContext) {

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
    dbService.getMlayerInstance(
        handler -> {
          if (handler.succeeded()) {
            verify(DatabaseServiceImpl.client, times(1)).searchAsync(any(), any(), any());
            testContext.completeNow();
          } else {
            testContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description("test getMlayerInstance method when get instance DB Request fails")
  public void testGetMlayerInstanceFailure(VertxTestContext testContext) {
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
    dbService.getMlayerInstance(
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).searchAsync(any(), any(), any());
            testContext.completeNow();

          } else {
            testContext.failNow("fail");
          }
        });
  }

  @Test
  @Description("test deleteMlayerInstance method when handler fails")
  public void testDeleteMlayerInstancefailure(VertxTestContext testContext) {

    DatabaseServiceImpl.client = mock(ElasticClient.class);
    JsonObject json = new JsonObject();
    json.put("InstanceID", "dummy id");
    String request = "dummy";
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
        .searchGetId(any(), any(), any());
    dbService.deleteMlayerInstance(
        request,
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).searchGetId(any(), any(), any());
            testContext.completeNow();
          } else {
            testContext.failNow("fail");
          }
        });
  }

  @Test
  @Description("test deleteMlayerInstance method when instance does not exist")
  public void testDeleteMlayerNoInstance(VertxTestContext testContext) {

    DatabaseServiceImpl.client = mock(ElasticClient.class);
    JsonObject json = new JsonObject();
    json.put("InstanceID", "dummy id").put(TOTAL_HITS, 0);
    when(asyncResult.failed()).thenReturn(false);
    when(asyncResult.result()).thenReturn(json);
    String request = "dummy";

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
    dbService.deleteMlayerInstance(
        request,
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).searchGetId(any(), any(), any());
            testContext.completeNow();
          } else {
            testContext.failNow("fail");
          }
        });
  }

  @Test
  @Description("test deleteMlayerInstance method when instance is deleted")
  public void testDeleteMlayerInstance(VertxTestContext testContext) {

    DatabaseServiceImpl.client = mock(ElasticClient.class);
    JsonObject json = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(0, "dummy string");
    json.put("InstanceID", "dummy id").put(TOTAL_HITS, 1).put(RESULTS, jsonArray);
    when(asyncResult.failed()).thenReturn(false);
    when(asyncResult.result()).thenReturn(json);
    String request = "dummy";
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
    dbService.deleteMlayerInstance(
        request,
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).searchGetId(any(), any(), any());
            verify(DatabaseServiceImpl.client, times(1)).docDelAsync(any(), any(), any());
            testContext.completeNow();
          } else {
            testContext.failNow("fail");
          }
        });
  }

  @Test
  @Description("test deleteMlayerInstance method when instance is deleted")
  public void testDeleteMlayerInstanceSuccess(VertxTestContext testContext) {

    DatabaseServiceImpl.client = mock(ElasticClient.class);
    JsonObject json = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(0, "dummy string");
    json.put("InstanceID", "dummy id").put(TOTAL_HITS, 1).put(RESULTS, jsonArray);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(json);
    String request = "dummy";
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
    dbService.deleteMlayerInstance(
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

  @Test
  @Description("test updateMlayerInstance method when handler fails")
  public void testUpdateMlayerInstancefailure(VertxTestContext testContext) {

    DatabaseServiceImpl.client = mock(ElasticClient.class);
    JsonObject json = new JsonObject();
    json.put(INSTANCE_ID, "dummy instance id").put(MLAYER_ID, "dummy id");
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
        .searchAsyncGetId(any(), any(), any());
    dbService.updateMlayerInstance(
        json,
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).searchAsyncGetId(any(), any(), any());
            testContext.completeNow();
          } else {
            testContext.failNow("fail");
          }
        });
  }

  @Test
  @Description("test updateMlayerInstance method when instance does not exist")
  public void testUpdateMlayerNoInstance(VertxTestContext testContext) {

    DatabaseServiceImpl.client = mock(ElasticClient.class);
    JsonObject json = new JsonObject();
    json.put(INSTANCE_ID, "dummy instance id").put(TOTAL_HITS, 0).put(MLAYER_ID, "dummy id");
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
        .searchAsyncGetId(any(), any(), any());
    dbService.updateMlayerInstance(
        json,
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).searchAsyncGetId(any(), any(), any());
            testContext.completeNow();
          } else {
            testContext.failNow("fail");
          }
        });
  }

  @Test
  @Description(
      "test updateMlayerInstance method when requsted body instance name does not match with parameter body")
  public void testUpdateMlayerInstanceNotMatch(VertxTestContext testContext) {

    DatabaseServiceImpl.client = mock(ElasticClient.class);
    JsonObject json = new JsonObject();
    JsonObject json2 = new JsonObject();
    JsonObject json3 = new JsonObject();
    json3.put("name", "domain");
    json2.put(SOURCE, json3);
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(0, json2);
    json.put(INSTANCE_ID, "dummy instance id")
        .put(TOTAL_HITS, 1)
        .put(RESULTS, jsonArray)
        .put(MLAYER_ID, "id")
        .put("name", "domain-name");
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
        .searchAsyncGetId(any(), any(), any());

    dbService.updateMlayerInstance(
        json,
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).searchAsyncGetId(any(), any(), any());
            testContext.completeNow();
          } else {
            testContext.failNow("fail");
          }
        });
  }

  @Test
  @Description("test updateMlayerInstance method when instance is updated")
  public void testUpdateMlayerInstance(VertxTestContext testContext) {

    DatabaseServiceImpl.client = mock(ElasticClient.class);
    JsonObject json = new JsonObject();
    JsonObject source = new JsonObject();
    JsonObject name = new JsonObject();
    name.put("name", "instance-name");
    source.put(SOURCE, name);
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(0, source);
    json.put(INSTANCE_ID, "dummy instance id")
        .put(TOTAL_HITS, 1)
        .put(RESULTS, jsonArray)
        .put(MLAYER_ID, "id")
        .put("name", "instance-name");
    when(asyncResult.failed()).thenReturn(false);
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
        .searchAsyncGetId(any(), any(), any());
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
    dbService.updateMlayerInstance(
        json,
        handler -> {
          if (handler.succeeded()) {
            verify(DatabaseServiceImpl.client, times(1)).searchAsyncGetId(any(), any(), any());
            verify(DatabaseServiceImpl.client, times(1)).docPutAsync(any(), any(), any(), any());
            testContext.completeNow();
          } else {
            testContext.failNow("fail");
          }
        });
  }

  @Test
  @Description("test updateMlayerInstance method when docPutAsync fails")
  public void testUpdateMlayerInstanceFails(VertxTestContext testContext) {

    DatabaseServiceImpl.client = mock(ElasticClient.class);
    JsonObject json = new JsonObject();
    JsonObject source = new JsonObject();
    JsonObject name = new JsonObject();
    name.put("name", "instance-name");
    source.put(SOURCE, name);
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(0, source);
    json.put("instanceId", "dummy instance id")
        .put(TOTAL_HITS, 1)
        .put(RESULTS, jsonArray)
        .put(MLAYER_ID, "id")
        .put("name", "instance-name");
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
        .searchAsyncGetId(any(), any(), any());
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
    dbService.updateMlayerInstance(
        json,
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).searchAsyncGetId(any(), any(), any());
            verify(DatabaseServiceImpl.client, times(1)).docPutAsync(any(), any(), any(), any());
            testContext.completeNow();
          } else {
            testContext.failNow("fail");
          }
        });
  }

  @Test
  @Description("test createMlayerDomain method when docPostAsync fails")
  public void testCreateMlayerDomainFailure(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    when(asyncResult.failed()).thenReturn(false);
    when(asyncResult.result()).thenReturn(request);
    request.put(DOMAIN_ID, "dummy").put(MLAYER_ID, "dummy").put(TOTAL_HITS, 0);
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
    dbService.createMlayerDomain(
        request,
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).docPostAsync(any(), any(), any());
            verify(DatabaseServiceImpl.client, times(1)).searchAsync(any(), any(), any());

            testContext.completeNow();

          } else {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Description("test createMlayerDomain method when the domain is created")
  public void testCreateMlayerDomain(VertxTestContext testContext) {

    DatabaseServiceImpl.client = mock(ElasticClient.class);
    JsonObject json = new JsonObject();
    json.put(DOMAIN_ID, "dummy").put(TOTAL_HITS, 0).put(MLAYER_ID, "dummy");
    json.put(MLAYER_DOMAIN_INDEX, "dummy");
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
    dbService.createMlayerDomain(
        json,
        handler -> {
          if (handler.succeeded()) {
            verify(DatabaseServiceImpl.client, times(1)).searchAsync(any(), any(), any());
            verify(DatabaseServiceImpl.client, times(1)).docPostAsync(any(), any(), any());
            testContext.completeNow();
          } else {
            testContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description("test createMlayerDomain method when searchAsync fails")
  public void testCreateMlayerDomainfail(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    when(asyncResult.failed()).thenReturn(true);
    request.put(DOMAIN_ID, "dummy").put(MLAYER_ID, "dummy");
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

    dbService.createMlayerDomain(
        request,
        handler -> {
          if (handler.failed()) {

            verify(DatabaseServiceImpl.client, times(1)).searchAsync(any(), any(), any());

            testContext.completeNow();

          } else {
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @Description("test createMlayerDomain method when domain already exists")
  public void testCreateInstanceWhenDomainExists(VertxTestContext testContext) {
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    JsonObject json = new JsonObject();
    JsonObject json2 = new JsonObject();
    json2.put(DOMAIN_ID, "dummy id");
    JsonArray jsonArray = new JsonArray().add(json2);
    json.put(MLAYER_ID, "dummy").put(TOTAL_HITS, 1).put("ID", "dummy").put(RESULTS, jsonArray);
    json.put(MLAYER_DOMAIN_INDEX, "dummy");
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
    dbService.createMlayerDomain(
        json,
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).searchAsync(any(), any(), any());
            testContext.completeNow();
          } else {
            testContext.failNow("fail");
          }
        });
  }

  @Test
  @Description("test getMlayerDomain method when the DB Request is Successful")
  public void testGetMlayerDomain(VertxTestContext testContext) {

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
    dbService.getMlayerDomain(
        handler -> {
          if (handler.succeeded()) {
            verify(DatabaseServiceImpl.client, times(1)).searchAsync(any(), any(), any());
            testContext.completeNow();
          } else {
            testContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description("test getMlayerDomain method when get instance DB Request fails")
  public void testGetMlayerDomainFailure(VertxTestContext testContext) {
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
    dbService.getMlayerDomain(
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).searchAsync(any(), any(), any());
            testContext.completeNow();

          } else {
            testContext.failNow("fail");
          }
        });
  }

  @Test
  @Description("test deleteMlayerDomain method when handler fails")
  public void testDeleteMlayerDomainfailure(VertxTestContext testContext) {

    DatabaseServiceImpl.client = mock(ElasticClient.class);
    JsonObject json = new JsonObject();
    json.put("DomainID", "dummy id");
    String request = "dummy";
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
        .searchGetId(any(), any(), any());
    dbService.deleteMlayerDomain(
        request,
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).searchGetId(any(), any(), any());
            testContext.completeNow();
          } else {
            testContext.failNow("fail");
          }
        });
  }

  @Test
  @Description("test deleteMlayerDomain method when domain does not exist")
  public void testDeleteMlayerNoDomain(VertxTestContext testContext) {

    DatabaseServiceImpl.client = mock(ElasticClient.class);
    JsonObject json = new JsonObject();
    json.put("DomainID", "dummy id").put(TOTAL_HITS, 0);
    when(asyncResult.failed()).thenReturn(false);
    when(asyncResult.result()).thenReturn(json);
    String request = "dummy";

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
    dbService.deleteMlayerDomain(
        request,
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).searchGetId(any(), any(), any());
            testContext.completeNow();
          } else {
            testContext.failNow("fail");
          }
        });
  }

  @Test
  @Description("test deleteMlayerDomain method when domain is deleted")
  public void testDeleteMlayerDomain(VertxTestContext testContext) {

    DatabaseServiceImpl.client = mock(ElasticClient.class);
    JsonObject json = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(0, "dummy string");
    json.put("DomainID", "dummy id").put(TOTAL_HITS, 1).put(RESULTS, jsonArray);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(json);
    String request = "dummy";
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
    dbService.deleteMlayerDomain(
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

  @Test
  @Description("test deleteMlayerDomain method when database Request fails")
  public void testDeleteMlayerDomainDBFails(VertxTestContext testContext) {

    DatabaseServiceImpl.client = mock(ElasticClient.class);
    JsonObject json = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(0, "dummy string");
    json.put("DomainID", "dummy id").put(TOTAL_HITS, 1).put(RESULTS, jsonArray);
    when(asyncResult.failed()).thenReturn(false);
    when(asyncResult.result()).thenReturn(json);
    String request = "dummy";
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
    dbService.deleteMlayerDomain(
        request,
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).searchGetId(any(), any(), any());
            verify(DatabaseServiceImpl.client, times(1)).docDelAsync(any(), any(), any());
            testContext.completeNow();
          } else {
            testContext.failNow("fail");
          }
        });
  }

  @Test
  @Description("test updateMlayerDomain method when handler fails")
  public void testUpdateMlayerDomainfailure(VertxTestContext testContext) {

    DatabaseServiceImpl.client = mock(ElasticClient.class);
    JsonObject json = new JsonObject();
    json.put(DOMAIN_ID, "dummy domain id").put(MLAYER_ID, "dummy id");
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
        .searchAsyncGetId(any(), any(), any());
    dbService.updateMlayerDomain(
        json,
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).searchAsyncGetId(any(), any(), any());
            testContext.completeNow();
          } else {
            testContext.failNow("fail");
          }
        });
  }

  @Test
  @Description("test updateMlayerDomain method when domain does not exist")
  public void testUpdateMlayerNoDomain(VertxTestContext testContext) {

    DatabaseServiceImpl.client = mock(ElasticClient.class);
    JsonObject json = new JsonObject();
    json.put(DOMAIN_ID, "dummy domain id").put(TOTAL_HITS, 0).put(MLAYER_ID, "dummy id");
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
        .searchAsyncGetId(any(), any(), any());
    dbService.updateMlayerDomain(
        json,
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).searchAsyncGetId(any(), any(), any());
            testContext.completeNow();
          } else {
            testContext.failNow("fail");
          }
        });
  }

  @Test
  @Description("test updateMlayerDomain method when doamin is updated")
  public void testUpdateMlayerDomain(VertxTestContext testContext) {

    DatabaseServiceImpl.client = mock(ElasticClient.class);
    JsonObject json = new JsonObject();
    JsonObject json2 = new JsonObject();
    JsonObject json3 = new JsonObject();
    json3.put("name", "domain-name");
    json2.put(SOURCE, json3);
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(0, json2);
    json.put(DOMAIN_ID, "dummy domain id")
        .put(TOTAL_HITS, 1)
        .put(RESULTS, jsonArray)
        .put(MLAYER_ID, "id")
        .put("name", "domain-name");
    when(asyncResult.failed()).thenReturn(false);
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
        .searchAsyncGetId(any(), any(), any());
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
    dbService.updateMlayerDomain(
        json,
        handler -> {
          if (handler.succeeded()) {
            verify(DatabaseServiceImpl.client, times(1)).searchAsyncGetId(any(), any(), any());
            verify(DatabaseServiceImpl.client, times(1)).docPutAsync(any(), any(), any(), any());

            testContext.completeNow();
          } else {
            testContext.failNow("fail");
          }
        });
  }

  @Test
  @Description(
      "test updateMlayerDomain method when requsted body domain name does not match with parameter body")
  public void testUpdateMlayerDomainNotMatch(VertxTestContext testContext) {

    DatabaseServiceImpl.client = mock(ElasticClient.class);
    JsonObject json = new JsonObject();
    JsonObject json2 = new JsonObject();
    JsonObject json3 = new JsonObject();
    json3.put("name", "domain");
    json2.put(SOURCE, json3);
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(0, json2);
    json.put(DOMAIN_ID, "dummy domain id")
        .put(TOTAL_HITS, 1)
        .put(RESULTS, jsonArray)
        .put(MLAYER_ID, "id")
        .put("name", "domain-name");
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
        .searchAsyncGetId(any(), any(), any());

    dbService.updateMlayerDomain(
        json,
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).searchAsyncGetId(any(), any(), any());
            testContext.completeNow();
          } else {
            testContext.failNow("fail");
          }
        });
  }

  @Test
  @Description("test updateMlayerDomain method when docPutAsync fails")
  public void testUpdateMlayerDomainFails(VertxTestContext testContext) {

    DatabaseServiceImpl.client = mock(ElasticClient.class);
    JsonObject json = new JsonObject();
    JsonObject source = new JsonObject();
    JsonObject name = new JsonObject();
    name.put("name", "domain-name");
    source.put(SOURCE, name);
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(0, source);
    json.put(DOMAIN_ID, "dummy domain id")
        .put(TOTAL_HITS, 1)
        .put(RESULTS, jsonArray)
        .put(MLAYER_ID, "id")
        .put("name", "domain-name");
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
        .searchAsyncGetId(any(), any(), any());
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
    dbService.updateMlayerDomain(
        json,
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).searchAsyncGetId(any(), any(), any());
            verify(DatabaseServiceImpl.client, times(1)).docPutAsync(any(), any(), any(), any());
            testContext.completeNow();
          } else {
            testContext.failNow("fail");
          }
        });
  }

  @Test
  @Description("test getMlayerProviders method when the DB Request is Successful")
  public void testGetMlayerProviders(VertxTestContext testContext) {

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
    dbService.getMlayerProviders(
        handler -> {
          if (handler.succeeded()) {
            verify(DatabaseServiceImpl.client, times(1)).searchAsync(any(), any(), any());
            testContext.completeNow();
          } else {
            testContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description("test getMlayerProviders method when get providers DB Request fails")
  public void testGetMlayerProvidersFailure(VertxTestContext testContext) {
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
    dbService.getMlayerProviders(
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).searchAsync(any(), any(), any());
            testContext.completeNow();

          } else {
            testContext.failNow("fail");
          }
        });
  }

  @Test
  @Description("test getMlayerGeoQuery method when the DB Request is Successful")
  public void testgetMlayerGeoQuery(VertxTestContext testContext) {

    DatabaseServiceImpl.client = mock(ElasticClient.class);
    JsonObject request = new JsonObject();
    JsonArray id = new JsonArray();
    id.add(0, "dummy id");
    request.put(INSTANCE, "instance").put(MLAYER_ID, id);
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
        .searchAsyncGeoQuery(any(), any(), any());
    dbService.getMlayerGeoQuery(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(DatabaseServiceImpl.client, times(1)).searchAsyncGeoQuery(any(), any(), any());
            testContext.completeNow();
          } else {
            testContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description("test getMlayerGeoQuery method when DB Request fails")
  public void testGetMlayerGeoQueryFailure(VertxTestContext testContext) {
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    JsonObject request = new JsonObject();
    JsonArray id = new JsonArray();
    id.add(0, "dummy id");
    request.put(INSTANCE, "instance").put(MLAYER_ID, id);
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
        .searchAsyncGeoQuery(any(), any(), any());
    dbService.getMlayerGeoQuery(
        request,
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).searchAsyncGeoQuery(any(), any(), any());
            testContext.completeNow();

          } else {
            testContext.failNow("fail");
          }
        });
  }

  @Test
  @Description("test getMlayerDataset method when DB Request is successful")
  public void testGetMlayerDataset(VertxTestContext testContext) {
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    String datasetId = "query";
    JsonObject datasetJson = new JsonObject().put(INSTANCE, "dummy instance");
    JsonObject json =
        new JsonObject()
            .put("provider", "provider id")
            .put("dataset", datasetJson)
            .put("icon", "icon-path")
                    .put("resourceServer","resourceServer uid");
    JsonArray jsonArray = new JsonArray().add(0, json);
    JsonObject request = new JsonObject().put(TOTAL_HITS, 0).put(RESULTS, jsonArray);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(request);
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
    dbService.getMlayerDataset(
        datasetId,
        handler -> {
          if (handler.succeeded()) {
            verify(DatabaseServiceImpl.client, times(1)).searchAsync(any(), any(), any());
            testContext.failNow("fail");

          } else {
            testContext.completeNow();
          }
        });
  }

  @Test
  @Description("test getMlayerDataset method when DB Request fails")
  public void testGetMlayerDatasetFailure(VertxTestContext testContext) {
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    String dataset_id = "dataset id";
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
    dbService.getMlayerDataset(
        dataset_id,
        handler -> {
          if (handler.failed()) {
            verify(DatabaseServiceImpl.client, times(1)).searchAsync(any(), any(), any());
            testContext.completeNow();

          } else {
            testContext.failNow("fail");
          }
        });
  }

  @Test
  @Description(
      "test getMlayerAllDatasets method when DB Request is successful and type equals provider")
  public void testGetMlayerAllDatasetsSuccess(VertxTestContext testContext) {
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    JsonObject request = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    JsonArray provider = new JsonArray();
    provider.add("iudx:Provider");
    JsonObject dataset_record = new JsonObject();
    dataset_record
        .put(INSTANCE, "dummy instance")
        .put(PROVIDER, "dummy provider")
        .put(TYPE, provider)
            .put("name","dummy name");
    jsonArray.add(dataset_record);
    request.put(RESULTS, jsonArray);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(request);
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

    dbService.getMlayerAllDatasets(
        handler -> {
          if (handler.succeeded()) {
            // verify(DatabaseServiceImpl.client, times(1)).searchAsyncDataset(any(), any(), any());
            verify(DatabaseServiceImpl.client, times(3)).searchAsync(any(), any(), any());

            testContext.completeNow();

          } else {
            testContext.failNow("fail");
          }
        });
  }

  @Test
  @Description(
      "test getMlayerAllDatasets method when DB Request is successful and type equals resource")
  public void testGetMlayerAllDatasetsResourceSuccess(VertxTestContext testContext) {
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    JsonObject request = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    JsonArray resource = new JsonArray();
    resource.add("iudx:Resource");
    JsonObject dataset_record = new JsonObject();
    dataset_record
        .put(INSTANCE, "dummy instance")
        .put(PROVIDER, "dummy provider")
        .put(TYPE, resource)
            .put("name", "dummy");
    jsonArray.add(dataset_record);
    request.put(RESULTS, jsonArray);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(request);
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

    dbService.getMlayerAllDatasets(
        handler -> {
          if (handler.succeeded()) {
            // verify(DatabaseServiceImpl.client, times(1)).searchAsyncDataset(any(), any(), any());
            verify(DatabaseServiceImpl.client, times(3)).searchAsync(any(), any(), any());

            testContext.completeNow();

          } else {
            testContext.failNow("fail");
          }
        });
  }

  @Test
  @Description("test getMlayerAllDatasets method when DB Request fails")
  public void testGetMlayerAllDatasetsResourceFails(VertxTestContext testContext) {
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    JsonObject request = new JsonObject();

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

    dbService.getMlayerAllDatasets(
        handler -> {
          if (handler.failed()) {
            // verify(DatabaseServiceImpl.client, times(1)).searchAsyncDataset(any(), any(), any());
            verify(DatabaseServiceImpl.client, times(1)).searchAsync(any(), any(), any());

            testContext.completeNow();

          } else {
            testContext.failNow("fail");
          }
        });
  }

  @Test
  @Description("test getMlayerPopularDatasets method when DB Request is successful")
  public void testGetMlayerPopularDatasetsSuccess(VertxTestContext testContext) {
    DatabaseServiceImpl databaseService =
        new DatabaseServiceImpl(
            client,
            docIndex,
            ratingIndex,
            mlayerInstanceIndex,
            mlayerDomainIndex,
            nlpService,
            geoService);
    String instanceName ="";
    JsonObject json =
        new JsonObject().put("resourcegroup", "abcd/abcd/abcd/abcd").put("instance", "instance");
    JsonObject json2 =
        new JsonObject().put("resourcegroup", "abcd/abcd/abcd/abcd").put("instance", "instance");

    JsonArray highestCountResource = new JsonArray().add(json).add(json2);
    DatabaseServiceImpl.client = mock(ElasticClient.class);

    JsonArray resourceArray = new JsonArray();
    JsonArray typeArray = new JsonArray().add(0, "iudx:ResourceGroup");
    JsonObject jsonObject2 =
        new JsonObject()
            .put("itemCreatedAt", "2022-12-15T04:23:38+0530")
            .put(TYPE, typeArray)
            .put("id", "abcd/abcd/abcd/abcd")
            .put("rgid", "abcd/abcd/abcd/abcd")
            .put("instance", "instance")
                .put("name", "agra");

    JsonObject instance =
        new JsonObject()
            .put("name", "agra")
            .put("icon", "path_of_agra-icon.jpg")
            .put(TYPE, typeArray)
            .put("itemCreatedAt", "2022-12-15T04:23:28+0530")
            .put("id", "abcd/abcd/abcd/abcd")
            .put("rgid", "abcd/abcd/abcd/abcd")
            .put("instance", "instance");
    resourceArray.add(instance).add(jsonObject2);
    JsonArray latestDataset = new JsonArray().add(json);

    JsonObject result =
        new JsonObject()
            .put(TOTAL_HITS, 1)
            .put(RESULTS, resourceArray)
            .put("latestDataset", latestDataset); // .put("instanceIconPath",json4);
    when(asyncResult.result()).thenReturn(result);
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
    databaseService.getMlayerPopularDatasets(
            instanceName,
        highestCountResource,
        handler -> {
          if (handler.succeeded()) {
            verify(DatabaseServiceImpl.client, times(3)).searchAsync(any(), any(), any());
            testContext.completeNow();

          } else {
            testContext.failNow("fail");
          }
        });
  }


  @Test
  @Description("test getMlayerPopularDatasets method when DB Request fails")
  public void testGetMlayerPopularDatasetsFailed(VertxTestContext testContext) {
      String instance ="";
    DatabaseServiceImpl databaseService =
        new DatabaseServiceImpl(
            client,
            docIndex,
            ratingIndex,
            mlayerInstanceIndex,
            mlayerDomainIndex,
            nlpService,
            geoService);
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    JsonArray highestCountResource = new JsonArray();

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

    databaseService.getMlayerPopularDatasets(
            instance,
        highestCountResource,
        handler -> {
          if (handler.failed()) {
            // verify(DatabaseServiceImpl.client, times(1)).searchAsyncDataset(any(), any(), any());
            verify(DatabaseServiceImpl.client, times(3)).searchAsync(any(), any(), any());

            testContext.completeNow();

          } else {
            testContext.failNow("fail");
          }
        });
    //  testContext.completeNow();

  }

  @Test
  @Description(
      "test getMlayerPopularDatasets method when DB Request is successful and type equals iudx:Provider")
  public void testGetMlayerPopularDatasetsProviderSuccess(VertxTestContext testContext) {
      String instanceName ="";
    DatabaseServiceImpl databaseService =
        new DatabaseServiceImpl(
            client,
            docIndex,
            ratingIndex,
            mlayerInstanceIndex,
            mlayerDomainIndex,
            nlpService,
            geoService);
    JsonObject json = new JsonObject().put("rgid", "duumy-id");
    JsonObject json2 = new JsonObject().put("rgid", "duumy-id");

    JsonArray highestCountResource = new JsonArray().add(json).add(json2);
    DatabaseServiceImpl.client = mock(ElasticClient.class);

    JsonArray resourceArray = new JsonArray();
    JsonArray typeArray = new JsonArray().add(0, "iudx:Provider");

    JsonObject instance =
        new JsonObject()
            .put("name", "agra")
            .put("icon", "path_of_agra-icon.jpg")
            .put(TYPE, typeArray)
            .put("resourceGroup", "abc");
    resourceArray.add(instance);

    JsonObject result = new JsonObject().put(TOTAL_HITS, 1).put(RESULTS, resourceArray);
    when(asyncResult.result()).thenReturn(result);
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
    databaseService.getMlayerPopularDatasets(
            instanceName,
        highestCountResource,
        handler -> {
          if (handler.succeeded()) {
            verify(DatabaseServiceImpl.client, times(3)).searchAsync(any(), any(), any());

            testContext.completeNow();

          } else {
            testContext.failNow("fail");
          }
        });
  }

  @Test
  @Description("test getMlayerPopularDatasets method when DB Request is successful and type equals iudx:Resource")
  public void testGetMlayerPopularDatasetsResourceSuccess(VertxTestContext testContext) {
    DatabaseServiceImpl databaseService =
        new DatabaseServiceImpl(
            client,
            docIndex,
            ratingIndex,
            mlayerInstanceIndex,
            mlayerDomainIndex,
            nlpService,
            geoService);
    String instanceName ="dummy";
    JsonObject json = new JsonObject().put("rgid", "duumy-id");
    JsonObject json2 = new JsonObject().put("rgid", "duumy-id");

    JsonArray highestCountResource = new JsonArray().add(json).add(json2);
    DatabaseServiceImpl.client = mock(ElasticClient.class);

    JsonArray resourceArray = new JsonArray();
    JsonArray typeArray = new JsonArray().add(0, "iudx:Resource");

    JsonObject instance =
        new JsonObject()
            .put("name", "agra")
            .put("icon", "path_of_agra-icon.jpg")
            .put(TYPE, typeArray)
            .put("resourceGroup", "abc");
    resourceArray.add(instance);

    JsonObject result = new JsonObject().put(TOTAL_HITS, 1).put(RESULTS, resourceArray);
    when(asyncResult.result()).thenReturn(result);
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
    databaseService.getMlayerPopularDatasets(
            instanceName,
        highestCountResource,
        handler -> {
          if (handler.succeeded()) {
            verify(DatabaseServiceImpl.client, times(3)).searchAsync(any(), any(), any());

            testContext.completeNow();

          } else {
            testContext.failNow("fail");
          }
        });
  }

  @Test
  @Description("testing method delete item when item does not exist")
  public void testDeleteItemNotExists(VertxTestContext vertxTestContext) {
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    JsonArray jsonArray = new JsonArray().add("docId");
    JsonObject request =
        new JsonObject().put("id", "item id").put(TOTAL_HITS, 0).put(RESULTS, jsonArray);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(request);
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
    dbService.deleteItem(
        request,
        handler -> {
          if (handler.succeeded()) {
            vertxTestContext.failNow("Fail");

          } else {
            vertxTestContext.completeNow();
          }
        });
  }

  @Test
  @Description("testing method delete item when other items are associated with it")
  public void testDeleteItemFailure(VertxTestContext vertxTestContext) {
    DatabaseServiceImpl.client = mock(ElasticClient.class);
    JsonArray jsonArray = new JsonArray().add("docId");
    JsonObject request =
        new JsonObject().put("id", "item id").put(TOTAL_HITS, 3).put(RESULTS, jsonArray);
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(request);
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
    dbService.deleteItem(
        request,
        handler -> {
          if (handler.succeeded()) {
            vertxTestContext.failNow("Fail");

          } else {
            vertxTestContext.completeNow();
          }
        });
  }

}