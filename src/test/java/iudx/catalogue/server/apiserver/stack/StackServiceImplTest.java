package iudx.catalogue.server.apiserver.stack;

import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.util.Constants.DATABASE_ERROR;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.catalogue.server.database.elastic.ElasticClient;
import iudx.catalogue.server.database.RespBuilder;
import jdk.jfr.Description;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
class StackServiceImplTest {
  static StacSevice stackSevice;
  @Mock private static ElasticClient mockElasticClient;
  @Mock private static RespBuilder mockRespBuilder;
  @Mock JsonObject mockJson;
  String notFoundERRor =
      new RespBuilder()
          .withType(TYPE_ITEM_NOT_FOUND)
          .withTitle(TITLE_ITEM_NOT_FOUND)
          .withDetail("Fail: Stac doesn't exist")
          .getResponse();
  String dbError =
      new RespBuilder()
          .withType(FAILED)
          .withResult("stackId", REQUEST_GET, FAILED)
          .withDetail(DATABASE_ERROR)
          .getResponse();

  @BeforeAll
  @DisplayName("Deploying Verticle")
  static void startVertx(Vertx vertx, VertxTestContext testContext) {
    mockElasticClient = Mockito.mock(ElasticClient.class);
    mockRespBuilder = Mockito.mock(RespBuilder.class);
    // stackSevice = new StackServiceImpl(mockElasticClient, "index");
    testContext.completeNow();
  }

  @Test
  @Description("Success: get() stack")
  public void testGetStack4Success(VertxTestContext vertxTestContext) {
    JsonObject sampleResult = new JsonObject().put("totalHits", 1).put("value", "value");

    // Stubbing the searchAsync method with thenAnswer
    when(mockElasticClient.searchAsync(any(), any(), anyInt(), anyInt(), anyString(), any()))
        .thenAnswer(
            invocation -> {
              Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(5);
              handler.handle(Future.succeededFuture(sampleResult));
              return null;
            });

    StacServiceImpl stackService = new StacServiceImpl(mockElasticClient, "Index");
    stackService.respBuilder = mockRespBuilder;

    Future<JsonObject> resultFuture = stackService.get("uuid");

    assertTrue(resultFuture.succeeded());
    assertEquals(sampleResult, resultFuture.result());
    vertxTestContext.completeNow();
  }

  @Test
  @Description("Failed: get() stack not found")
  public void testGetStack4NotFound(VertxTestContext vertxTestContext) {
    JsonObject sampleResult = new JsonObject().put("totalHits", 0).put("value", "value");

    // Stubbing the searchAsync method with thenAnswer
    when(mockElasticClient.searchAsync(any(), any(), anyInt(), anyInt(), anyString(), any()))
        .thenAnswer(
            invocation -> {
              Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(5);
              handler.handle(Future.succeededFuture(sampleResult));
              return null;
            });

    StacServiceImpl stackService = new StacServiceImpl(mockElasticClient, "index");
    stackService.respBuilder = mockRespBuilder;

    Future<JsonObject> resultFuture = stackService.get("stackId");

    System.out.println(resultFuture.cause().getMessage());

    assertTrue(resultFuture.failed());
    assertEquals(notFoundERRor, resultFuture.cause().getMessage());
    vertxTestContext.completeNow();
  }

  @Test
  @Description("Failed: get() Db error")
  public void testGetStack4DbError(VertxTestContext vertxTestContext) {
    // Stubbing the searchAsync method with thenAnswer
    when(mockElasticClient.searchAsync(any(), any(), anyInt(), anyInt(), anyString(), any()))
        .thenAnswer(
            invocation -> {
              Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(5);
              handler.handle(Future.failedFuture("sampleResult"));
              return null;
            });

    StacServiceImpl stackService = new StacServiceImpl(mockElasticClient, "index");
    stackService.respBuilder = mockRespBuilder;

    Future<JsonObject> resultFuture = stackService.get("stackId");

    assertTrue(resultFuture.failed());
    assertEquals(dbError, resultFuture.cause().getMessage());
    vertxTestContext.completeNow();
  }

  @Test
  @Description("Success: create() stack creation")
  void testCreateSuccess(VertxTestContext testContext) {
    // Prepare sample data
    JsonObject emptySearchResult = new JsonObject().put("totalHits", 0);

    // Stubbing the searchAsync method to return an empty result
    when(mockElasticClient.searchAsync(any(), any(), anyInt(), anyInt(), anyString(), any()))
        .thenAnswer(
            invocation -> {
              Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(5);
              handler.handle(Future.succeededFuture(emptySearchResult));
              return null;
            });

    // Stubbing the docPostAsync method to return a success result
    when(mockElasticClient.docPostAsync(anyString(), anyString(), any()))
        .thenAnswer(
            invocation -> {
              Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(2);
              handler.handle(Future.succeededFuture(new JsonObject().put("id", "generatedId")));
              return null;
            });

    StacServiceImpl stackService = new StacServiceImpl(mockElasticClient, "Index");

    // Stubbing other methods as needed
    JsonObject self = new JsonObject().put("href", "dummy_href").put("rel", "self");
    JsonObject root = new JsonObject().put("href", "dummy_href").put("rel", "root");
    JsonArray links = new JsonArray().add(self).add(root);
    when(mockJson.getJsonArray(anyString())).thenReturn(links);

    // Execute the method under test
    Future<JsonObject> resultFuture =
        stackService
            .create(mockJson)
            .onComplete(
                handler -> {
                  if (handler.succeeded()) {
                    JsonObject result = handler.result();
                    assertEquals(SUCCESS, result.getString(TYPE));
                    assertEquals(STAC_CREATION_SUCCESS, result.getString(DETAIL));
                    testContext.completeNow();
                  } else {
                    testContext.failNow("failed: " + handler.cause().getMessage());
                  }
                });
  }

  @Test
  @Description("Failed: docPostAsync() failure during stack creation")
  void testCreate4SFailureDbError(VertxTestContext testContext) {
    // Prepare sample data
    JsonObject emptySearchResult = new JsonObject().put("totalHits", 0);

    // Stubbing the searchAsync method to return an empty result
    when(mockElasticClient.searchAsync(any(), any(), anyInt(), anyInt(), anyString(), any()))
        .thenAnswer(
            invocation -> {
              Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(5);
              handler.handle(Future.succeededFuture(emptySearchResult));
              return null;
            });

    // Stubbing the docPostAsync method to return a success result
    when(mockElasticClient.docPostAsync(anyString(), anyString(), any()))
        .thenAnswer(
            invocation -> {
              Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(2);
              handler.handle(Future.failedFuture("Db Error"));
              return null;
            });

    StacServiceImpl stackService = new StacServiceImpl(mockElasticClient, "Index");

    // Stubbing other methods as needed
    JsonObject self = new JsonObject().put("href", "dummy_href").put("rel", "self");
    JsonObject root = new JsonObject().put("href", "dummy_href").put("rel", "root");
    JsonArray links = new JsonArray().add(self).add(root);
    when(mockJson.getJsonArray(anyString())).thenReturn(links);

    // Execute the method under test
    Future<JsonObject> resultFuture =
        stackService
            .create(mockJson)
            .onComplete(
                handler -> {
                  if (handler.failed()) {
                    JsonObject result = new JsonObject(handler.cause().getMessage());
                    assertEquals(FAILED, result.getString(TYPE));
                    assertEquals(DATABASE_ERROR, result.getString(DETAIL));
                    testContext.completeNow();
                  } else {
                    testContext.failNow("failed: ");
                  }
                });
  }

  @Test
  @Description("Failed: conflicts during stack creation")
  void testCreate4ConflictSFailure(VertxTestContext testContext) {
    // Prepare sample data
    JsonObject emptySearchResult = new JsonObject().put("totalHits", 1);

    // Stubbing the searchAsync method to return an empty result
    when(mockElasticClient.searchAsync(any(), any(), anyInt(), anyInt(), anyString(), any()))
        .thenAnswer(
            invocation -> {
              Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(5);
              handler.handle(Future.succeededFuture(emptySearchResult));
              return null;
            });

    StacServiceImpl stackService = new StacServiceImpl(mockElasticClient, "Index");

    // Stubbing other methods as needed
    JsonObject self = new JsonObject().put("href", "dummy_href").put("rel", "self");
    JsonObject root = new JsonObject().put("href", "dummy_href").put("rel", "root");
    JsonArray links = new JsonArray().add(self).add(root);
    when(mockJson.getJsonArray(anyString())).thenReturn(links);

    // Execute the method under test
    Future<JsonObject> resultFuture =
        stackService
            .create(mockJson)
            .onComplete(
                handler -> {
                  if (handler.failed()) {
                    JsonObject result = new JsonObject(handler.cause().getMessage());
                    assertEquals(TYPE_CONFLICT, result.getString(TYPE));
                    assertEquals(DETAIL_CONFLICT, result.getString(TITLE));
                    testContext.completeNow();
                  } else {
                    testContext.failNow("failed: ");
                  }
                });
  }

  @Test
  @Description("Failed: Db Error during searchAsync while stack creation")
  void testCreate4DbErrorFailure(VertxTestContext testContext) {
    // Stubbing the searchAsync method to return an empty result
    when(mockElasticClient.searchAsync(any(), any(), anyInt(), anyInt(), anyString(), any()))
        .thenAnswer(
            invocation -> {
              Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(5);
              handler.handle(Future.failedFuture("Db Error during  searchAsync"));
              return null;
            });

    StacServiceImpl stackService = new StacServiceImpl(mockElasticClient, "Index");

    // Stubbing other methods as needed
    JsonObject self = new JsonObject().put("href", "dummy_href").put("rel", "self");
    JsonObject root = new JsonObject().put("href", "dummy_href").put("rel", "root");
    JsonArray links = new JsonArray().add(self).add(root);
    when(mockJson.getJsonArray(anyString())).thenReturn(links);

    // Execute the method under test
    Future<JsonObject> resultFuture =
        stackService
            .create(mockJson)
            .onComplete(
                handler -> {
                  if (handler.failed()) {
                    JsonObject result = new JsonObject(handler.cause().getMessage());
                    assertEquals(FAILED, result.getString(TYPE));
                    assertEquals(DATABASE_ERROR, result.getString(DETAIL));
                    testContext.completeNow();
                  } else {
                    testContext.failNow("failed: ");
                  }
                });
  }

  @Test
  @Description("Success: stack [patch] ")
  void testUpdate(VertxTestContext vertxTestContext) {
    stackSevice = new StacServiceImpl(mockElasticClient, "index");
    // Mocking data
    JsonObject stack = new JsonObject().put("id", "someId").put("rel", "child").put("href", "href");

    JsonArray links =
        new JsonArray().add(new JsonObject().put("rel", "child").put("href", "someHref"));

    JsonObject json = new JsonObject().put("links", links);

    JsonObject existResult =
        new JsonObject()
            .put("totalHits", 1)
            .put(
                "results",
                new JsonArray()
                    .add(
                        new JsonObject()
                            .put("id", "someId")
                            .put(StackConstants.DOC_ID, "someDocId")
                            .put("_source", json)));

    // Stubbing the searchAsync method to return an empty result
    when(mockElasticClient.searchAsyncGetId(any(), any(), any(), any()))
        .thenAnswer(
            invocation -> {
              Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
              handler.handle(Future.succeededFuture(existResult));
              return null;
            });

    // Stubbing the docPatchAsync method to return an empty result
    when(mockElasticClient.docPatchAsync(any(), any(), any(), any()))
        .thenAnswer(
            invocation -> {
              Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
              handler.handle(Future.succeededFuture(new JsonObject().put("someKey", "someValue")));
              return null;
            });

    // Testing the update method
    Future<JsonObject> updateFuture = stackSevice.update(stack);

    updateFuture.onComplete(
        handler -> {
          if (handler.succeeded()) {
            JsonObject result = handler.result();
            assertEquals(TYPE_SUCCESS, result.getString(TYPE));
            assertEquals(TITLE_SUCCESS, result.getString(TITLE));
            vertxTestContext.completeNow();
          } else {
            Throwable error = handler.cause();
            vertxTestContext.failNow(error);
          }
        });
  }

  @Test
  @Description("Conflict : stack [patch] ")
  void testUpdate4ExistingNotAllowed(VertxTestContext vertxTestContext) {
    stackSevice = new StacServiceImpl(mockElasticClient, "index");
    // Mocking data
    JsonObject stack = new JsonObject().put("id", "someId").put("rel", "child").put("href", "href");

    JsonArray links = new JsonArray().add(new JsonObject().put("rel", "child").put("href", "href"));

    JsonObject json = new JsonObject().put("links", links);

    JsonObject existResult =
        new JsonObject()
            .put("totalHits", 1)
            .put(
                "results",
                new JsonArray()
                    .add(
                        new JsonObject()
                            .put("id", "someId")
                            .put(StackConstants.DOC_ID, "someDocId")
                            .put("_source", json)));

    // Stubbing the searchAsync method to return an empty result
    when(mockElasticClient.searchAsyncGetId(any(), any(), anyString(), any()))
        .thenAnswer(
            invocation -> {
              Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
              handler.handle(Future.succeededFuture(existResult));
              return null;
            });

    // Testing the update method
    Future<JsonObject> updateFuture = stackSevice.update(stack);

    updateFuture.onComplete(
        handler -> {
          if (handler.succeeded()) {
            JsonObject result = handler.result();
            vertxTestContext.failNow("Failed: " + result);

          } else {
            String error = handler.cause().getMessage();
            JsonObject result = new JsonObject(error);
            assertEquals(TYPE_CONFLICT, result.getString(TYPE));
            assertEquals(TITLE_ALREADY_EXISTS, result.getString(TITLE));
            vertxTestContext.completeNow();
          }
        });
  }

  @Test
  @Description("NotFound : stack [patch] ")
  void testUpdate4ItemNotFound(VertxTestContext vertxTestContext) {
    stackSevice = new StacServiceImpl(mockElasticClient, "index");
    // Mocking data
    JsonObject stack = new JsonObject().put("id", "someId").put("rel", "child").put("href", "href");

    JsonArray links = new JsonArray().add(new JsonObject().put("rel", "child").put("href", "href"));

    JsonObject json = new JsonObject().put("links", links);

    JsonObject existResult =
        new JsonObject()
            .put("totalHits", 0)
            .put(
                "results",
                new JsonArray()
                    .add(
                        new JsonObject()
                            .put("id", "someId")
                            .put(StackConstants.DOC_ID, "someDocId")
                            .put("_source", json)));

    // Stubbing the searchAsync method to return an empty result
    when(mockElasticClient.searchAsyncGetId(any(), any(), anyString(), any()))
        .thenAnswer(
            invocation -> {
              Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
              handler.handle(Future.succeededFuture(existResult));
              return null;
            });

    // Testing the update method
    Future<JsonObject> updateFuture = stackSevice.update(stack);

    updateFuture.onComplete(
        handler -> {
          if (handler.succeeded()) {
            JsonObject result = handler.result();
            vertxTestContext.failNow("Failed: {}" + result);
          } else {
            String error = handler.cause().getMessage();
            JsonObject result = new JsonObject(error);
            assertEquals(TYPE_ITEM_NOT_FOUND, result.getString(TYPE));
            assertEquals(TITLE_ITEM_NOT_FOUND, result.getString(TITLE));
            vertxTestContext.completeNow();
          }
        });
  }

  @Test
  @Description("Success: stack deletion")
  void testDelete4SuccessfulDeletion(VertxTestContext vertxTestContext) {
    String stackId = "someId";
    JsonObject existResult =
        new JsonObject()
            .put("totalHits", 1)
            .put(
                "results",
                new JsonArray()
                    .add(
                        new JsonObject()
                            .put("id", "someId")
                            .put(StackConstants.DOC_ID, "someDocId")));

    // Stubbing the searchAsyncGetId method to return the existResult
    when(mockElasticClient.searchAsyncGetId(any(), any(), anyString(), any()))
        .thenAnswer(
            invocation -> {
              Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
              handler.handle(Future.succeededFuture(existResult));
              return null;
            });

    // Stubbing the docDelAsync method to return success
    when(mockElasticClient.docDelAsync(anyString(), anyString(), any()))
        .thenAnswer(
            invocation -> {
              Handler<AsyncResult<Void>> handler = invocation.getArgument(2);
              handler.handle(Future.succeededFuture());
              return null;
            });

    stackSevice = new StacServiceImpl(mockElasticClient, "index");

    // Testing the delete method
    Future<JsonObject> deleteFuture = stackSevice.delete(stackId);

    deleteFuture.onComplete(
        handler -> {
          if (handler.succeeded()) {
            JsonObject result = handler.result();
            assertEquals(STAC_DELETION_SUCCESS, result.getString(DETAIL));
            assertEquals(SUCCESS, result.getString("type"));
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Failed: {}" + handler.cause().getMessage());
          }
        });
  }

  @Test
  @Description("Failed: stack deletion")
  void testDelete4DeletionFailure(VertxTestContext vertxTestContext) {
    String stackId = "someId";
    JsonObject existResult = new JsonObject().put("totalHits", 0);

    // Stubbing the searchAsyncGetId method to return the existResult
    when(mockElasticClient.searchAsyncGetId(any(), any(), any(), any()))
        .thenAnswer(
            invocation -> {
              Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
              handler.handle(Future.succeededFuture(existResult));
              return null;
            });

    stackSevice = new StacServiceImpl(mockElasticClient, "index");

    // Testing the delete method
    Future<JsonObject> deleteFuture = stackSevice.delete(stackId);

    deleteFuture.onComplete(
        handler -> {
          if (handler.succeeded()) {
            vertxTestContext.failNow("Failed: {}" + handler.cause().getMessage());

          } else {
            String errorMessage = handler.cause().getMessage();
            JsonObject result = new JsonObject(errorMessage);
            assertEquals(TYPE_ITEM_NOT_FOUND, result.getString(TYPE));
            assertEquals("Item not found, can't delete", result.getString(DETAIL));
            vertxTestContext.completeNow();
          }
        });
  }

  @Test
  @Description("Failed: stack deletion due to db error")
  void testDelete4FailedDeletionDbError(VertxTestContext vertxTestContext) {
    String stackId = "someId";
    JsonObject existResult =
        new JsonObject()
            .put("totalHits", 1)
            .put(
                "results",
                new JsonArray()
                    .add(
                        new JsonObject()
                            .put("id", "someId")
                            .put(StackConstants.DOC_ID, "someDocId")));

    // Stubbing the searchAsyncGetId method to return the existResult
    when(mockElasticClient.searchAsyncGetId(any(), any(), any(), any()))
        .thenAnswer(
            invocation -> {
              Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
              handler.handle(Future.succeededFuture(existResult));
              return null;
            });

    // Stubbing the docDelAsync method to return success
    when(mockElasticClient.docDelAsync(anyString(), anyString(), any()))
        .thenAnswer(
            invocation -> {
              Handler<AsyncResult<Void>> handler = invocation.getArgument(2);
              handler.handle(Future.failedFuture("Failed: db error"));
              return null;
            });

    stackSevice = new StacServiceImpl(mockElasticClient, "index");

    // Testing the delete method
    Future<JsonObject> deleteFuture = stackSevice.delete(stackId);

    deleteFuture.onComplete(
        handler -> {
          if (handler.succeeded()) {
            vertxTestContext.failNow("Failed: {}" + handler.result());
          } else {

            String errorMessage = handler.cause().getMessage();
            JsonObject result = new JsonObject(errorMessage);
            assertEquals(DATABASE_ERROR, result.getString(DETAIL));
            assertEquals(FAILED, result.getString("type"));
            vertxTestContext.completeNow();
          }
        });
  }

  @Test
  @Description("Failed: stack deletion searchAsyncGetId() failure")
  void testDelete4DeletionAsyncFailure(VertxTestContext vertxTestContext) {
    String stackId = "someId";
    JsonObject existResult = new JsonObject().put("totalHits", 0);

    // Stubbing the searchAsyncGetId method to return the existResult
    when(mockElasticClient.searchAsyncGetId(any(), any(), anyString(), any()))
        .thenAnswer(
            invocation -> {
              Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
              handler.handle(Future.failedFuture("failed: async failure"));
              return null;
            });

    stackSevice = new StacServiceImpl(mockElasticClient, "index");

    // Testing the delete method
    Future<JsonObject> deleteFuture = stackSevice.delete(stackId);

    deleteFuture.onComplete(
        handler -> {
          if (handler.succeeded()) {
            vertxTestContext.failNow("Failed: {}" + handler.cause().getMessage());

          } else {
            String errorMessage = handler.cause().getMessage();
            JsonObject result = new JsonObject(errorMessage);
            assertEquals(TYPE_ITEM_NOT_FOUND, result.getString(TYPE));
            assertEquals("Item not found, can't delete", result.getString(DETAIL));
            vertxTestContext.completeNow();
          }
        });
  }

  @Test
  @Description("Failed: stack deletion Json decode() failure")
  void testDelete4DeletionJsonDecodeFailure(VertxTestContext vertxTestContext) {
    String stackId = "someId";
    JsonObject existResult = new JsonObject().put("totalHits", 1);

    // Stubbing the searchAsyncGetId method to return the existResult
    when(mockElasticClient.searchAsyncGetId(any(), any(), any(), any()))
        .thenAnswer(
            invocation -> {
              Handler<AsyncResult<JsonObject>> handler = invocation.getArgument(3);
              handler.handle(Future.succeededFuture(existResult));
              return null;
            });

    stackSevice = new StacServiceImpl(mockElasticClient, "index");

    // Testing the delete method
    Future<JsonObject> deleteFuture = stackSevice.delete(stackId);

    deleteFuture.onComplete(
        handler -> {
          if (handler.succeeded()) {
            vertxTestContext.failNow("Failed: {}" + handler.cause().getMessage());

          } else {
            String errorMessage = handler.cause().getMessage();
            JsonObject result = new JsonObject(errorMessage);
            assertEquals(TYPE_ITEM_NOT_FOUND, result.getString(TYPE));
            assertEquals("Item not found, can't delete", result.getString(DETAIL));
            vertxTestContext.completeNow();
          }
        });
  }
}
