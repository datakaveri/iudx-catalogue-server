package iudx.catalogue.server.mlayer;

import com.google.j2objc.annotations.J2ObjCIncompatible;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.catalogue.server.database.DatabaseService;
import iudx.catalogue.server.rating.RatingServiceTest;
import org.apache.curator.shaded.com.google.common.hash.HashCode;
import org.apache.curator.shaded.com.google.common.hash.HashFunction;
import org.apache.curator.shaded.com.google.common.hash.Hashing;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import static iudx.catalogue.server.mlayer.util.Constants.INSTANCE_ID;
import static iudx.catalogue.server.mlayer.util.Constants.MLAYER_INSTANCE_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class MlayerServiceTest {
  private static Logger LOGGER = LogManager.getLogger(MlayerServiceTest.class);
  MlayerServiceImpl mlayerService;
  @Mock private static AsyncResult<JsonObject> asyncResult;
  @Mock DatabaseService databaseService;

  @Mock JsonObject json;

  private JsonObject requestJson() {
    return new JsonObject()
        .put("name", "pune")
        .put("cover", "path of cover.jpg")
        .put("icon", "path of icon.jpg")
        .put("logo", "path og logo.jpg");
  }

  @Test
  @DisplayName("Success: test create mlayer instance")
  void successfulMlayerInstanceCreationTest(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(databaseService);
    json = requestJson();
    when(asyncResult.succeeded()).thenReturn(true);
    when(asyncResult.result()).thenReturn(new JsonObject());

    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .createMlayerInstance(any(), any());
    mlayerService.createMlayerInstance(
        json,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).createMlayerInstance(any(), any());
            testContext.completeNow();
          } else {
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("Failure: test create mlayer instance")
  void failureMlayerInstanceCreationTest(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(databaseService);
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
        .createMlayerInstance(any(), any());

    mlayerService.createMlayerInstance(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).createMlayerInstance(any(), any());
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          } else {
            testContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("Success: test get all mlayer instance")
  void successfulMlayerInstanceGetTest(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(databaseService);
    when(asyncResult.succeeded()).thenReturn(true);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .getMlayerInstance(any());
    mlayerService.getMlayerInstance(
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).getMlayerInstance(any());
            testContext.completeNow();
          } else {
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("Failure: test get all mlayer instance")
  void failureMlayerInstanceGetTest(VertxTestContext testContext) {
    mlayerService = new MlayerServiceImpl(databaseService);
    when(asyncResult.succeeded()).thenReturn(false);
    Mockito.doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(0)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .getMlayerInstance(any());

    mlayerService.getMlayerInstance(
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).createMlayerInstance(any(), any());
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          } else {
            testContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("Success: test delete mlayer instance")
  void successfulMlayerInstanceDeleteTest(VertxTestContext testContext) {
    String request = "dummy";
    mlayerService = new MlayerServiceImpl(databaseService);
    when(asyncResult.succeeded()).thenReturn(true);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .deleteMlayerInstance(any(), any());

    mlayerService.deleteMlayerInstance(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).deleteMlayerInstance(any(), any());
            testContext.completeNow();
          } else {
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("Failure: test delete mlayer instance")
  void failureMlayerInstanceDeleteTest(VertxTestContext testContext) {
    String request = "dummy";
    mlayerService = new MlayerServiceImpl(databaseService);
    when(asyncResult.succeeded()).thenReturn(false);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .deleteMlayerInstance(any(), any());

    mlayerService.deleteMlayerInstance(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).deleteMlayerInstance(any(), any());
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          } else {
            testContext.completeNow();
          }
        });
  }

  @Test
  @DisplayName("Success: test update mlayer instance")
  void successfulMlayerInstanceUpdateTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request.put("name", "instance name");
    mlayerService = new MlayerServiceImpl(databaseService);
    when(asyncResult.succeeded()).thenReturn(true);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .updateMlayerInstance(any(), any());

    mlayerService.updateMlayerInstance(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).updateMlayerInstance(any(), any());
            testContext.completeNow();
          } else {
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          }
        });
  }

  @Test
  @DisplayName("Failure: test update mlayer instance")
  void failureMlayerInstanceUpdateTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request.put("name", "instance name");
    mlayerService = new MlayerServiceImpl(databaseService);
    when(asyncResult.succeeded()).thenReturn(false);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @SuppressWarnings("unchecked")
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(1)).handle(asyncResult);
                return null;
              }
            })
        .when(databaseService)
        .updateMlayerInstance(any(), any());

    mlayerService.updateMlayerInstance(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(databaseService, times(1)).updateMlayerInstance(any(), any());
            LOGGER.debug("Fail");
            testContext.failNow(handler.cause());
          } else {
            LOGGER.debug("Fail");
            testContext.completeNow();
          }
        });
  }
}
