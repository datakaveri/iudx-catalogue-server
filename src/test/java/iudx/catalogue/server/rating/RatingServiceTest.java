package iudx.catalogue.server.rating;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.pgclient.PgPool;
import iudx.catalogue.server.Configuration;
import iudx.catalogue.server.database.DatabaseService;
import iudx.catalogue.server.database.postgres.PostgresService;
import iudx.catalogue.server.databroker.DataBrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class RatingServiceTest {

  private static Logger LOGGER = LogManager.getLogger(RatingServiceTest.class);
  private static JsonObject config;
  private static String exchangeName;
  private static String rsauditingtable;
  private static int minReadNumber;
  private static RatingServiceImpl ratingService, ratingServiceSpy;
  private static PgPool pgPool;
  private static AsyncResult<JsonObject> asyncResult;
  private static DatabaseService databaseService;
  private static DataBrokerService dataBrokerService;
  private static PostgresService postgresService;

  @BeforeAll
  @DisplayName("Initialize vertx and deploy verticle")
  public static void init(Vertx vertx, VertxTestContext testContext) {
    config = Configuration.getConfiguration("./configs/config-test.json", 7);
    exchangeName = config.getString("exchangeName");
    rsauditingtable = config.getString("rsAuditingTableName");
    minReadNumber = config.getInteger("minReadNumber");
    databaseService = mock(DatabaseService.class);
    dataBrokerService = mock(DataBrokerService.class);
    postgresService = mock(PostgresService.class);
    asyncResult = mock(AsyncResult.class);
    ratingService =
        new RatingServiceImpl(
            exchangeName,
            rsauditingtable,
            minReadNumber,
            databaseService,
            dataBrokerService,
            postgresService);
    ratingServiceSpy = spy(ratingService);
    testContext.completeNow();
  }

  @Test
  @DisplayName("testing setup")
  public void shouldSucceed(VertxTestContext testContext) {
    LOGGER.info("setup test is passing");
    testContext.completeNow();
  }

  private JsonObject requestJson() {
    return new JsonObject()
        .put("rating", 4.5)
        .put("comment", "some comment")
        .put("id", "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood")
        .put("userID", "some-user")
        .put("status", "pending");
  }

  @Test
  @DisplayName("Success: test create rating")
  void successfulRatingCreationTest(VertxTestContext testContext) {
    JsonObject request = requestJson();
    JsonObject auditInfo = new JsonObject().put("totalHits", minReadNumber + 1);

    doAnswer(Answer -> Future.succeededFuture(auditInfo))
        .when(ratingServiceSpy)
        .getAuditingInfo(any());

    doAnswer(Answer -> Future.succeededFuture()).when(ratingServiceSpy).publishMessage(any());

    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(new JsonObject());

    Mockito.doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .createRating(any(), any());

    ratingServiceSpy.createRating(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(ratingServiceSpy, times(2)).getAuditingInfo(any());
            verify(databaseService, times(1)).createRating(any(), any());
            testContext.completeNow();
          } else {
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("Failure testing rating creation")
  void failureTestingRatingCreation(VertxTestContext testContext) {
    JsonObject request = requestJson();
    JsonObject auditInfo = new JsonObject().put("totalHits", 1);

    doAnswer(Answer -> Future.succeededFuture(auditInfo))
        .when(ratingServiceSpy)
        .getAuditingInfo(any());

    when(asyncResult.succeeded()).thenReturn(false);

    Mockito.doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .createRating(any(), any());

    ratingServiceSpy.createRating(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(ratingServiceSpy, times(1)).getAuditingInfo(any());
            verify(databaseService, times(1)).createRating(any(), any());
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          } else {
            testContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("test create rating - get audit info failed")
  void testAuditInfoFailed(VertxTestContext testContext) {

    doAnswer(Answer -> Future.failedFuture(new Throwable("empty message")))
        .when(ratingServiceSpy)
        .getAuditingInfo(any());

    ratingServiceSpy.createRating(
        requestJson(),
        handler -> {
          if (handler.succeeded()) {
            verify(ratingServiceSpy, times(1)).getAuditingInfo(any());
            testContext.failNow(handler.cause());
          } else {
            testContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("Success: test get rating")
  void testGetingRating(VertxTestContext testContext) {
    JsonObject request = requestJson();

    when(asyncResult.succeeded()).thenReturn(true);

    Mockito.doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .getRatings(any(), any());

    ratingServiceSpy.getRating(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(ratingServiceSpy, times(2)).getRating(any(), any());
            testContext.completeNow();
          } else {
            LOGGER.error("Fail");
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("failure testing get rating")
  void failureTestingGetRating(VertxTestContext testContext) {
    JsonObject request = requestJson();

    when(asyncResult.succeeded()).thenReturn(false);

    Mockito.doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .getRatings(any(), any());

    ratingServiceSpy.getRating(
        request,
        handler -> {
          if (handler.failed()) {
            verify(ratingServiceSpy, times(1)).getRating(any(), any());
            testContext.completeNow();
          } else {
            testContext.failNow("Fail");
          }
        });
  }

  @Test
  @DisplayName("Success: test update rating")
  void successfulRatingUpdationTest(VertxTestContext testContext) {
    JsonObject request = requestJson();

    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(new JsonObject());

    Mockito.doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .updateRating(any(), any());

    ratingServiceSpy.updateRating(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(2)).updateRating(any(), any());
            testContext.completeNow();
          } else {
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("Failure testing rating updation")
  void failureTestingRatingUpdation(VertxTestContext testContext) {
    JsonObject request = requestJson();

    when(asyncResult.succeeded()).thenReturn(false);

    Mockito.doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .updateRating(any(), any());

    ratingServiceSpy.updateRating(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).updateRating(any(), any());
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          } else {
            testContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("Success: test delete rating")
  void successfulRatingDeletionTest(VertxTestContext testContext) {
    JsonObject request = requestJson();

    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(new JsonObject());

    Mockito.doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .deleteRating(any(), any());

    ratingServiceSpy.deleteRating(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).deleteRating(any(), any());
            testContext.completeNow();
          } else {
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("Failure testing rating updation")
  void failureTestingRatingDeletion(VertxTestContext testContext) {
    JsonObject request = requestJson();

    when(asyncResult.succeeded()).thenReturn(false);

    Mockito.doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .deleteRating(any(), any());

    ratingServiceSpy.deleteRating(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(2)).deleteRating(any(), any());
            testContext.failNow("Fail");
          } else {
            testContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("Success: Test publish message")
  void testPublishMessage(VertxTestContext testContext) {

    when(asyncResult.succeeded()).thenReturn(true);

    Mockito.doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(3)).handle(asyncResult);
                return null;
              }
            })
        .when(dataBrokerService)
        .publishMessage(any(), eq("#"), anyString(), any());

    ratingServiceSpy.publishMessage(requestJson());
    testContext.completeNow();
  }

  @Test
  @DisplayName("Success: Test get auditing info future")
  public void testGetAuditingInfo(VertxTestContext testContext) {
    StringBuilder query = new StringBuilder("select * from nosuchtable");
    when(asyncResult.succeeded()).thenReturn(true);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0)
                  throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1))
                    .handle(asyncResult);
                return null;
              }
            })
        .when(postgresService)
        .executeCountQuery(anyString(), any());

    ratingService
        .getAuditingInfo(query)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                verify(postgresService, times(1)).executeCountQuery(anyString(), any());
                testContext.completeNow();
              } else {
                testContext.failNow("get auditing info test failed");
              }
            });
  }
}
