package iudx.catalogue.server.validator;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.catalogue.server.Configuration;
import iudx.catalogue.server.apiserver.RelationshipApis;
import iudx.catalogue.server.database.ElasticClient;
import jdk.jfr.Description;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.HashSet;
import java.util.Set;

import static iudx.catalogue.server.util.Constants.*;
import static junit.framework.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class ValidatorServiceImplTest {
  private static ValidatorService validator;
  private static Vertx vertxObj;
  private static ElasticClient client;
  private static String databaseIP;
  private static String docIndex;
  private static int databasePort;
  private static String databaseUser;
  private static String databasePassword;
  private static FileSystem fileSystem;
  ValidatorServiceImpl validatorService;
  @Mock Handler<AsyncResult<JsonObject>> handler;
  @Mock AsyncResult<JsonObject> asyncResult;
  @Mock Throwable throwable;
  @Mock AsyncResult<JsonObject> asyncResultServerRes;

  @BeforeAll
  @DisplayName("Deploying Verticle")
  static void startVertx(Vertx vertx, VertxTestContext testContext) {
    vertxObj = vertx;
    fileSystem = vertx.fileSystem();
    JsonObject validatorConfig = Configuration.getConfiguration("./configs/config-test.json", 2);

    /* Configuration setup */
    databaseIP = validatorConfig.getString(DATABASE_IP);
    databasePort = validatorConfig.getInteger(DATABASE_PORT);
    databaseUser = validatorConfig.getString(DATABASE_UNAME);
    databasePassword = validatorConfig.getString(DATABASE_PASSWD);
    docIndex = validatorConfig.getString(DOC_INDEX);

    // TODO : Need to enable TLS using xpack security
    client = new ElasticClient(databaseIP, databasePort, docIndex, databaseUser, databasePassword);
    validator = new ValidatorServiceImpl(client, docIndex);
    testContext.completeNow();
  }

  @Test
  @Description("testing the method validateSchema when itemType equals ITEM_TYPE_RESOURCE_SERVER")
  public void testValidateSchema(VertxTestContext vertxTestContext) {
    validatorService = new ValidatorServiceImpl(client, docIndex);
    JsonObject request = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(ITEM_TYPE_RESOURCE_SERVER);
    request.put(TYPE, jsonArray);
    assertNotNull(validatorService.validateSchema(request, handler));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing the method validateSchema when itemType equals ITEM_TYPE_PROVIDER")
  public void testValidateSchemaProvider(VertxTestContext vertxTestContext) {
    validatorService = new ValidatorServiceImpl(client, docIndex);
    JsonObject request = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(ITEM_TYPE_PROVIDER);
    request.put(TYPE, jsonArray);
    assertNotNull(validatorService.validateSchema(request, handler));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing the method validateItem when itemType equals ITEM_TYPE_RESOURCE_SERVER")
  public void testValidateItemRESOURCE_SERVER(VertxTestContext vertxTestContext) {
    validatorService = new ValidatorServiceImpl(client, docIndex);
    JsonObject request = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(ITEM_TYPE_RESOURCE_SERVER);
    request.put(TYPE, jsonArray);
    request.put(PROVIDER, "dummy");
    request.put(NAME, "dummy");

    assertNotNull(validatorService.validateItem(request, handler));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing the method validateItem when itemType equals ITEM_TYPE_PROVIDER")
  public void testValidateItemPROVIDER(VertxTestContext vertxTestContext) {
    validatorService = new ValidatorServiceImpl(client, docIndex);
    JsonObject request = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(ITEM_TYPE_PROVIDER);
    request.put(TYPE, jsonArray);

    assertNotNull(validatorService.validateItem(request, handler));
    vertxTestContext.completeNow();
  }

  @Test
  @Description(
      "testing the method validateItem when itemType equals ITEM_TYPE_RESOURCE_GROUP and hits is 1")
  public void testValidateItemRESOURCE_GROUP(VertxTestContext vertxTestContext) {
    validatorService = new ValidatorServiceImpl(client, docIndex);
    JsonObject request = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    JsonObject json = new JsonObject();
    json.put(TOTAL_HITS, 1);
    jsonArray.add(ITEM_TYPE_RESOURCE_GROUP);
    request.put(TYPE, jsonArray);
    request.put(RESOURCE_SVR, "abcd/abcd/abcd");
    request.put(PROVIDER, "dummy");
    request.put(NAME, "dummy");
    ValidatorServiceImpl.client = mock(ElasticClient.class);
    when(asyncResult.result()).thenReturn(json);

    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(ValidatorServiceImpl.client)
        .searchGetId(any(), any(), any());
    validatorService.validateItem(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(ValidatorServiceImpl.client, times(2)).searchGetId(anyString(), any(), any());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description(
      "testing the method validateItem when itemType equals ITEM_TYPE_RESOURCE_GROUP and hits is not 1")
  public void testValidateItemRESOURCE_GROUP2(VertxTestContext vertxTestContext) {
    validatorService = new ValidatorServiceImpl(client, docIndex);
    JsonObject request = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    JsonObject json = new JsonObject();
    json.put(TOTAL_HITS, 0);
    jsonArray.add(ITEM_TYPE_RESOURCE_GROUP);
    request.put(TYPE, jsonArray);
    request.put(RESOURCE_SVR, "abcd/abcd/abcd");
    request.put(PROVIDER, "dummy");
    request.put(NAME, "dummy");
    ValidatorServiceImpl.client = mock(ElasticClient.class);
    when(asyncResult.result()).thenReturn(json);

    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(ValidatorServiceImpl.client)
        .searchGetId(any(), any(), any());
    validatorService.validateItem(
        request,
        handler -> {
          if (handler.failed()) {
            verify(ValidatorServiceImpl.client, times(1)).searchGetId(anyString(), any(), any());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description(
      "testing the method validateItem when itemType equals ITEM_TYPE_RESOURCE and hits=1 ")
  public void testValidateItem(VertxTestContext vertxTestContext) {
    validatorService = new ValidatorServiceImpl(client, docIndex);
    JsonObject request = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    JsonObject json = new JsonObject();
    json.put(TOTAL_HITS, 1);
    jsonArray.add(ITEM_TYPE_RESOURCE);
    request.put(TYPE, jsonArray);
    request.put(NAME, "name");
    request.put(RESOURCE_GRP, "abcd/abcd/abcd");
    request.put(PROVIDER, "abcd/abcd");
    request.put(NAME, "dummy");
    ValidatorServiceImpl.client = mock(ElasticClient.class);
    when(asyncResult.result()).thenReturn(json);

    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(ValidatorServiceImpl.client)
        .searchGetId(any(), any(), any());
    validatorService.validateItem(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(ValidatorServiceImpl.client, times(1)).searchGetId(anyString(), any(), any());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description(
      "testing the method validateItem when itemType equals ITEM_TYPE_RESOURCE and hits !=1 ")
  public void testValidateItemITEM_TYPE_RESOURCE(VertxTestContext vertxTestContext) {
    validatorService = new ValidatorServiceImpl(client, docIndex);
    JsonObject request = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    JsonObject json = new JsonObject();
    json.put(TOTAL_HITS, 0);
    jsonArray.add(ITEM_TYPE_RESOURCE);
    request.put(TYPE, jsonArray);
    request.put(NAME, "name");
    request.put(RESOURCE_GRP, "abcd/abcd/abcd");
    request.put(PROVIDER, "abcd/abcd");
    request.put(NAME, "dummy");
    ValidatorServiceImpl.client = mock(ElasticClient.class);
    when(asyncResult.result()).thenReturn(json);

    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(ValidatorServiceImpl.client)
        .searchGetId(any(), any(), any());
    validatorService.validateItem(
        request,
        handler -> {
          if (handler.failed()) {
            verify(ValidatorServiceImpl.client, times(1)).searchGetId(anyString(), any(), any());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description("testing the method validateItem when item type mismatch")
  public void testValidateItemCatch(VertxTestContext vertxTestContext) {
    validatorService = new ValidatorServiceImpl(client, docIndex);
    JsonObject request = new JsonObject();
    assertNotNull(validatorService.validateItem(request, handler));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing the method validateProvider ")
  public void testValidateProvider(VertxTestContext vertxTestContext) {
    validatorService = new ValidatorServiceImpl(client, docIndex);
    JsonObject request = new JsonObject();
    assertNull(validatorService.validateProvider(request, handler));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing the method validateItem when handler failed")
  public void testValidateItemFailure(VertxTestContext vertxTestContext) {
    validatorService = new ValidatorServiceImpl(client, docIndex);
    JsonObject request = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    JsonObject json = new JsonObject();
    json.put(TOTAL_HITS, 0);
    jsonArray.add(ITEM_TYPE_RESOURCE);
    request.put(TYPE, jsonArray);
    request.put(NAME, "name");
    request.put(RESOURCE_GRP, "abcd/abcd/abcd");
    request.put(PROVIDER, "abcd/abcd");
    request.put(NAME, "dummy");
    ValidatorServiceImpl.client = mock(ElasticClient.class);
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
        .when(ValidatorServiceImpl.client)
        .searchGetId(any(), any(), any());
    validatorService.validateItem(
        request,
        handler -> {
          if (handler.failed()) {
            verify(ValidatorServiceImpl.client, times(1)).searchGetId(anyString(), any(), any());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }
  @Test
  @Description("testing the method validate mlayer instance")
  public void testValidateMlayerInstance(VertxTestContext vertxTestContext) {
    validatorService = new ValidatorServiceImpl(client, docIndex);
    JsonObject request = new JsonObject();
    assertNull(validatorService.validateMlayerInstance(request, handler));
    vertxTestContext.completeNow();
  }
  @Test
  @Description("testing the method validate mlayer domain")
  public void testValidateMlayerDomain(VertxTestContext vertxTestContext) {
    validatorService = new ValidatorServiceImpl(client, docIndex);
    JsonObject request = new JsonObject();
    assertNotNull(validatorService.validateMlayerDomain(request, handler));
    vertxTestContext.completeNow();
  }
  @Test
  @Description("testing the method validate mlayer geo-query")
  public void testValidateMlayerGeoQuery(VertxTestContext vertxTestContext) {
    validatorService = new ValidatorServiceImpl(client, docIndex);
    JsonObject request = new JsonObject();
    assertNotNull(validatorService.validateMlayerGeoQuery(request, handler));
    vertxTestContext.completeNow();
  }
  @Test
  @Description("testing the method validate mlayer dataset_id")
  public void testValidateMlayerDatasetId(VertxTestContext vertxTestContext) {
    validatorService = new ValidatorServiceImpl(client, docIndex);
    JsonObject request = new JsonObject();
    assertNotNull(validatorService.validateMlayerDatasetId(request, handler));
    vertxTestContext.completeNow();
  }
}
