package iudx.catalogue.server.validator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import static iudx.catalogue.server.util.Constants.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.file.FileSystem;
import iudx.catalogue.server.Configuration;
import iudx.catalogue.server.database.ElasticClient;

@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ValidatorServiceTest {

  private static final Logger LOGGER = LogManager.getLogger(ValidatorServiceTest.class);
  private static ValidatorService validator;
  private static Vertx vertxObj;
  private static ElasticClient client;
  private static String databaseIP;
  private static int databasePort;
  private static String databaseUser;
  private static String databasePassword;
  private static FileSystem fileSystem;

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

    // TODO : Need to enable TLS using xpack security
    client = new ElasticClient(databaseIP, databasePort, databaseUser, databasePassword);
    validator = new ValidatorServiceImpl(client);
    testContext.completeNow();
  }

  @AfterEach
  public void finish(VertxTestContext testContext) {
    LOGGER.info("Finishing....");
    vertxObj.close(testContext.succeeding(response -> testContext.completeNow()));
  }

  @Test
  @Order(1)
  @DisplayName("Test Link Validation [Resource]")
  void validResourceLinkTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request
        .put("type", new JsonArray().add("iudx:Resource"))
        .put(
            "id",
            "datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs.iudx.io/aqm-bosch-climo/aqm_test_1")
        .put(
            "resourceGroup",
            "datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs.iudx.io/aqm-bosch-climo");
    validator.validateItem(
        request,
        testContext.succeeding(
            response ->
                testContext.verify(
                    () -> {
                      testContext.completeNow();
                    })));
  }

  @Test
  @Order(2)
  @DisplayName("Test Link Validation [ResourceGroup]")
  void validResourceGroupLinkTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request
        .put("type", new JsonArray().add("iudx:ResourceGroup"))
        .put("id",
            "datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs.iudx.io/aqm-bosch-climo")
        .put(
            "provider",
            "datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc")
        .put(
            "resourceServer", "datakaveri.org/f7e044eee8122b5c87dce6e7ad64f3266afa41dc/rs.iudx.io");
    validator.validateItem(
        request,
        testContext.succeeding(
            response ->
                testContext.verify(
                    () -> {
                      testContext.completeNow();
                    })));
  }

  @Test
  @Order(3)
  @DisplayName("Test Invalid Link [Resource]")
  void invalidResourceLinkTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request
        .put("type", new JsonArray().add("iudx:Resource"))
        .put(
            "id",
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.iudx.org.in/sensors/sensorA")
        .put(
            "resourceGroup",
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.iudx.org.in/sensors123");
    validator.validateItem(
        request,
        testContext.failing(
            response ->
                testContext.verify(
                    () -> {
                      testContext.completeNow();
                    })));
  }

  @Test
  @Order(4)
  @DisplayName("Test Invalid Link [ResourceGroup]")
  void invalidResourceGroupLinkTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request
        .put("type", new JsonArray().add("iudx:ResourceGroup"))
        .put("id", "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.iudx.org.in/sensors")
        .put(
            "provider",
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.iudx.org.in/sensors")
        .put(
            "resourceServer",
            "rbccps.org/00D75505FD5256B142AFD9C0E32790FA7180D500/rs.invalid.org.in");
    validator.validateItem(
        request,
        testContext.failing(
            response ->
                testContext.verify(
                    () -> {
                      testContext.completeNow();
                    })));
  }

  @Test
  @Order(5)
  @DisplayName("Valid Schema Test [Resource]")
  void validResourceSchemaTest(VertxTestContext testContext) {

    JsonObject resource = fileSystem.readFileBlocking("./src/test/resources/resources.json")
        .toJsonArray().getJsonObject(0);

    validator.validateSchema(resource, testContext.succeeding(response -> testContext.verify(() -> {
      testContext.completeNow();
    })));
  }

  @Test
  @Order(7)
  @DisplayName("Valid Schema Test [ResourceGroup]")
  void validResourceGroupSchemaTest(VertxTestContext testContext) {

    JsonObject resourceGrp =
        fileSystem.readFileBlocking("./src/test/resources/resourceGroup.json").toJsonObject();

    System.out.println(resourceGrp.toString());

    validator.validateSchema(resourceGrp,
        testContext.succeeding(response -> testContext.verify(() -> {
          testContext.completeNow();
        })));
  }

  @Test
  @Order(8)
  @DisplayName("Invalid Schema Test")
  void invalidSchemaTest(VertxTestContext testContext) {
    try {
      JsonObject request =
          new JsonObject(
              "{\"id\":\"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.iudx.org.in/sensors\",\"description\": \"Description of this resource group\",\"name\": \"sensors\",\"tags\": \"sensor, sensing, resource, battery operated\",\"itemStatus\": \"ACTIVE\",\"provider\": \"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531\",\"resourceServer\": \"rbccps.org/00D75505FD5256B142AFD9C0E32790FA7180D500/rs.iudx.org.in\",\"resourceAuthControlLevel\": \"INDIVIDUAL\",\"resourceType\": \"messageStream\",\"authServerInfo\": {\"type\": [\"AuthServerInfoValue\"],\"authServerURL\": \"https://auth.iudx.org.in\",\"authType\": \"iudx-auth\"},\"accessObjectInfo\":{\"type\": [\"AccessObjectInfoValue\"],\"accessObject\": \"https://example.com/sensorsApis.json\",\"additionalInfoURL\": \"https://example.com/sensorsApis\",\"accessObjectType\": \"openAPI\"},\"iudxResourceAPIs\": [\"attribute\", \"temporal\"],\"itemCreatedAt\": \"2019-02-20T10:30:06.093121\",\"location\": {\"type\": \"Place\",\"address\": \"Bangalore\"}}");
      System.out.println(request.toString());
      validator.validateSchema(
          request,
          testContext.failing(
              response ->
                  testContext.verify(
                      () -> {
                        testContext.completeNow();
                      })));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
