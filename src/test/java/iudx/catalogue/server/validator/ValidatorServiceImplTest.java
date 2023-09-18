package iudx.catalogue.server.validator;

import static iudx.catalogue.server.util.Constants.*;
import static junit.framework.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.catalogue.server.Configuration;
import iudx.catalogue.server.database.ElasticClient;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jdk.jfr.Description;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class ValidatorServiceImplTest {

  static ValidatorServiceImpl validatorService;
  static ValidatorServiceImpl validatorServiceSpy;
  static AsyncResult<JsonObject> asyncResult;
  private static Logger LOGGER = LogManager.getLogger(ValidatorServiceImplTest.class);
  private static Vertx vertxObj;
  private static ElasticClient client;
  private static String databaseIP;
  private static String docIndex;
  private static int databasePort;
  private static String databaseUser;
  private static String databasePassword;
  private static FileSystem fileSystem;
  private static boolean isUacInstance;
  @Mock Handler<AsyncResult<JsonObject>> handler;
  private static JsonObject jsonMock;
  private static JsonArray jsonArrayMock;
  private static Stream<Object> streamMock;

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
    isUacInstance = false;

    //    client = new ElasticClient(databaseIP, databasePort, docIndex, databaseUser,
    // databasePassword);
    client = mock(ElasticClient.class);
    validatorService = new ValidatorServiceImpl(client, docIndex, isUacInstance);

    asyncResult = mock(AsyncResult.class);
    doAnswer(
            new Answer<AsyncResult<JsonObject>>() {
              @Override
              public AsyncResult<JsonObject> answer(InvocationOnMock arg0) throws Throwable {
                ((Handler<AsyncResult<JsonObject>>) arg0.getArgument(2)).handle(asyncResult);
                return null;
              }
            })
        .when(client)
        .searchAsync(any(), any(), any());
    testContext.completeNow();
  }

  private static Stream<Arguments> itemTypes() {
    return Stream.of(
        Arguments.of(ITEM_TYPE_RESOURCE),
        Arguments.of(ITEM_TYPE_RESOURCE_GROUP),
        Arguments.of(ITEM_TYPE_PROVIDER),
        Arguments.of(ITEM_TYPE_RESOURCE_SERVER),
        Arguments.of(ITEM_TYPE_COS),
        Arguments.of(ITEM_TYPE_OWNER));
  }

  private static JsonObject requestBody() {

    return new JsonObject()
        .put(TYPE, new JsonArray().add(ITEM_TYPE_RESOURCE_SERVER))
        .put(COS_ITEM, "cos-id")
        .put(NAME, "dummy")
        .put(RESOURCE_SVR, "0fdeb952-398c-4020-af20-0843b616f415")
        .put(PROVIDER, "provider-id")
        .put(RESOURCE_GRP, "rg-id")
//        .put("id", "40bbe849-9f35-32b9-8f64-00d5e62db473")
        .put(COS_ITEM, "cos-id");
  }

  @ParameterizedTest
  @MethodSource("itemTypes")
  @Description("testing the method validateSchema when itemType equals ITEM_TYPE_RESOURCE_SERVER")
  public void testValidateSchema(String itemType, VertxTestContext vertxTestContext) {
    validatorService = new ValidatorServiceImpl(client, docIndex, isUacInstance);
    JsonObject request = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(itemType);
    request.put(TYPE, jsonArray);
    assertNotNull(validatorService.validateSchema(request, handler));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing the method validateItem when itemType equals ITEM_TYPE_RESOURCE_SERVER")
  public void testValidateItemRESOURCE_SERVER(VertxTestContext vertxTestContext) {
    JsonObject request = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(ITEM_TYPE_RESOURCE_SERVER);
    request.put(TYPE, jsonArray);
    request.put(COS, "cos-id");
    request.put(NAME, "dummy");

    when(asyncResult.result()).thenReturn(jsonMock);

    validatorService.validateItem(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(client, times(4)).searchGetId(anyString(), any(), any());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description(
      "testing the method validateItem when itemType equals ITEM_TYPE_PROVIDER and id is generated")
  public void testValidateItemPROVIDER(VertxTestContext vertxTestContext) {
    JsonObject request = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(ITEM_TYPE_PROVIDER);
    request
        .put(TYPE, jsonArray)
        .put("name", "provider name")
        .put(RESOURCE_SVR, "0fdeb952-398c-4020-af20-0843b616f415");

    when(asyncResult.result())
        .thenReturn(
            new JsonObject()
                .put(TOTAL_HITS, 1)
                .put(RESULTS, new JsonArray().add(new JsonObject().put(TYPE, ITEM_TYPE_RESOURCE_SERVER))));

    validatorService.validateItem(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(client, times(1)).searchAsync(anyString(), any(), any());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description(
      "testing the method validateItem when itemType equals ITEM_TYPE_PROVIDER and id is validated")
  public void testValidateItemProviderId(VertxTestContext vertxTestContext) {
    JsonObject request = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(ITEM_TYPE_PROVIDER);
    request
        .put(TYPE, jsonArray)
        .put("name", "provider name")
        .put(RESOURCE_SVR, "0fdeb952-398c-4020-af20-0843b616f415");

    assertNotNull(validatorService.validateItem(request, handler));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing the method validateItem when itemType equals resource server and hits is 1")
  public void testValidateItemResourceServer(VertxTestContext vertxTestContext) {
    JsonObject request = requestBody();
    request.put(PROVIDER, "cos-id");
    JsonObject json = new JsonObject();
    json.put(TOTAL_HITS, 1);

    assertNotNull(validatorService.validateItem(request, handler));
    vertxTestContext.completeNow();
  }

  @Test
  @Description(
      "testing the method validateItem when itemType equals ITEM_TYPE_RESOURCE_GROUP and hits is 1")
  public void testValidateItemRESOURCE_GROUP(VertxTestContext vertxTestContext) {
    JsonObject request = requestBody();
    request.put(TYPE, new JsonArray().add(ITEM_TYPE_RESOURCE_GROUP));
    JsonObject json = new JsonObject();
    json.put(TOTAL_HITS, 1);
    when(asyncResult.failed()).thenReturn(false);
    when(asyncResult.result()).thenReturn(json);

    validatorService.validateItem(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(client, times(8)).searchGetId(anyString(), any(), any());
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
    JsonObject request = requestBody();
    request.put(TYPE, new JsonArray().add(ITEM_TYPE_RESOURCE_GROUP));
    JsonObject json = new JsonObject();
    json.put(TOTAL_HITS, 0);
    when(asyncResult.result()).thenReturn(json);

    validatorService.validateItem(
        request,
        handler -> {
          if (handler.failed()) {
            verify(client, times(3)).searchGetId(anyString(), any(), any());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }

  @Test
  @Description(
      "testing the method validateItem when itemType equals ITEM_TYPE_RESOURCE and hits=3 ")
  public void testValidateItem(VertxTestContext vertxTestContext) {
    JsonObject request = requestBody();
    request.put(TYPE, new JsonArray().add(ITEM_TYPE_RESOURCE));
    JsonObject json = new JsonObject();
    json.put(TOTAL_HITS, 3);
    when(asyncResult.failed()).thenReturn(false);
    when(asyncResult.result()).thenReturn(json);

    validatorService.validateItem(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(client, times(10)).searchGetId(anyString(), any(), any());
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
    JsonObject request = requestBody();
    request.put(TYPE, new JsonArray().add(ITEM_TYPE_RESOURCE));
    JsonObject json = new JsonObject();
    json.put(TOTAL_HITS, 4);
    when(asyncResult.result()).thenReturn(json);

    validatorService.validateItem(
        request,
        handler -> {
          LOGGER.debug(handler.failed());
          if (handler.failed()) {
            verify(client, times(1)).searchGetId(anyString(), any(), any());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }

  @Test
  @DisplayName("Test validate cos item")
  public void testValidateCosItem(VertxTestContext testContext) {
    JsonObject request = requestBody();
    request.put(TYPE, new JsonArray().add(ITEM_TYPE_COS))
            .put(OWNER, "owner-id");

    when(asyncResult.failed()).thenReturn(false);
    when(asyncResult.result()).thenReturn(new JsonObject().put(TOTAL_HITS, 1));

    validatorService.validateItem(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(client, times(6)).searchGetId(anyString(), any(), any());
            testContext.completeNow();
          } else {
            testContext.failNow("Fail");
          }
        });
  }

  @Test
  @DisplayName("Test validate owner item")
  public void testValidateOwnerItem(VertxTestContext testContext) {
    JsonObject request = requestBody();
    request.put(TYPE, new JsonArray().add(ITEM_TYPE_OWNER));
    when(asyncResult.result()).thenReturn(new JsonObject().put(TOTAL_HITS, 1));

    assertNotNull(validatorService.validateItem(request, handler));
    testContext.completeNow();
  }

  @Test
  @Description("testing the method validateItem when item type mismatch")
  public void testValidateItemCatch(VertxTestContext vertxTestContext) {
    JsonObject request = new JsonObject();
    assertNotNull(validatorService.validateItem(request, handler));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing the method validateItem when handler failed")
  public void testValidateItemFailure(VertxTestContext vertxTestContext) {
    JsonObject request = requestBody();
    request.put(TYPE, new JsonArray().add(ITEM_TYPE_RESOURCE));
    JsonObject json = new JsonObject();
    json.put(TOTAL_HITS, 0);
    when(asyncResult.failed()).thenReturn(true);

    validatorService.validateItem(
        request,
        handler -> {
          if (handler.failed()) {
            verify(client, times(5)).searchGetId(anyString(), any(), any());
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("Fail");
          }
        });
  }
}
