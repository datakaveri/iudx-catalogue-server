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

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class ValidatorServiceImplTest {

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
  static ValidatorServiceImpl validatorService;
  @Mock Handler<AsyncResult<JsonObject>> handler;
  static AsyncResult<JsonObject> asyncResult;

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

//    client = new ElasticClient(databaseIP, databasePort, docIndex, databaseUser, databasePassword);
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
        .searchGetId(any(), any(), any());
    testContext.completeNow();
  }

  @Test
  @Description("testing the method validateSchema when itemType equals ITEM_TYPE_RESOURCE_SERVER")
  public void testValidateSchema(VertxTestContext vertxTestContext) {
    validatorService = new ValidatorServiceImpl(client, docIndex, isUacInstance);
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
    JsonObject request = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(ITEM_TYPE_PROVIDER);
    request.put(TYPE, jsonArray);
    assertNotNull(validatorService.validateSchema(request, handler));
    vertxTestContext.completeNow();
  }

  private static JsonObject requestBody() {

    return new JsonObject()
        .put(TYPE, new JsonArray().add(ITEM_TYPE_RESOURCE_SERVER))
        .put(OWNER, "owner-id")
        .put(NAME, "dummy")
        .put(RESOURCE_SVR, "0fdeb952-398c-4020-af20-0843b616f415")
        .put(PROVIDER, "provider-id")
        .put(RESOURCE_GRP, "rg-id")
        .put("id", "40bbe849-9f35-32b9-8f64-00d5e62db473")
        .put(OWNER, "owner-id");
  }

  @Test
  @Description("testing the method validateItem when itemType equals ITEM_TYPE_RESOURCE_SERVER")
  public void testValidateItemRESOURCE_SERVER(VertxTestContext vertxTestContext) {
    JsonObject request = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(ITEM_TYPE_RESOURCE_SERVER);
    request.put(TYPE, jsonArray);
    request.put(PROVIDER, "owner-id");
    request.put(NAME, "dummy");

    assertNotNull(validatorService.validateItem(request, handler));
    vertxTestContext.completeNow();
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

    when(asyncResult.result()).thenReturn(new JsonObject().put(TOTAL_HITS, 1));

    validatorService.validateItem(
        request,
        handler -> {
          if (handler.succeeded()) {
            verify(client, times(2)).searchGetId(anyString(), any(), any());
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
    request.put(PROVIDER, "owner-id");
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
            verify(client, times(7)).searchGetId(anyString(), any(), any());
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
            verify(client, times(9)).searchGetId(anyString(), any(), any());
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
    json.put(TOTAL_HITS, 0);
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
