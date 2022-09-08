package iudx.catalogue.server.validator;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static iudx.catalogue.server.util.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.core.Vertx;
import io.vertx.core.file.FileSystem;
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
  private static String docIndex;
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
    docIndex = validatorConfig.getString(DOC_INDEX);

    // TODO : Need to enable TLS using xpack security
    client = new ElasticClient(databaseIP, databasePort, docIndex, databaseUser, databasePassword);
    validator = new ValidatorServiceImpl(client);
    testContext.completeNow();
  }

  @AfterEach
  public void finish(VertxTestContext testContext) {
    LOGGER.info("Finishing....");
    vertxObj.close(testContext.succeeding(response -> testContext.completeNow()));
  }

/*  @Test
  @Order(1)
  @DisplayName("Test Link Validation [Resource]")
  void validResourceLinkTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request
        .put("type", new JsonArray().add("iudx:Resource"))
        .put(
            "id",
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/aqm-bosch-climo/aqm_test_1")
        .put(
            "resourceGroup",
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/aqm-bosch-climo")
        .put("provider", "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86");
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
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/aqm-bosch-climo")
        .put(
            "provider",
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86")
        .put(
            "resourceServer", "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io");
    validator.validateItem(
        request,
        testContext.succeeding(
            response ->
                testContext.verify(
                    () -> {
                      testContext.completeNow();
                    })));
  }*/

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
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.iudx.org.in/sensors123")
        .put("provider", "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86");
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
        fileSystem.readFileBlocking("./src/test/resources/resourceGroup.json").toJsonArray().getJsonObject(0);

    LOGGER.debug(resourceGrp.toString());

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
      LOGGER.debug(request.toString());
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

  @Test
  @Order(9)
  @DisplayName("Valid Schema Test, large name [Resource]")
  void validResourceSchemaTestName(VertxTestContext testContext) {

    JsonObject resource = fileSystem.readFileBlocking("./src/test/resources/resources.json")
        .toJsonArray().getJsonObject(0);
    resource.put("name",
        "2AgEk25l7odg91lTOolXHouSDUjbB_JbNvqYrhQUjfIfAbkv03tBqBW_EQK7f733MdtfNcqti7K1xt4o3rLEFVQqVrQZHTm4vf8fAGs8KsqVjDFPGcJM/UgDxrcF7YMnbTdePN7pRr8/9T1o_2bUkH3DoktbOTk3FkD8IsdHm_OdKIuGEvjeMis0oqQiEEXqPNqdUpPA5lqjV1c76ihPoOmO/1XzJkWRcgc_MXSWy8Q2u/2FAPTmOKGSW5LE6wHXIDt/0hPlwCByXNazmQxcO/GRdAznMJKo_Xj7BtJHsx3m/oYus9cJYj1KDTJt2qL98mQ1Z0Al_PsknycOspHWplfesuVSsebZ92Xe5wbpy/4OFSHxUjxevkCSUm38Q/XkUTa1zfByV6P2VOSz3Fc_VN0kRHZyNx32NwcG76CV3QDoUoDDyHNvsK8vgdR3Z/AVSP4P/h/IoX3s6o/rcLLYWD7ioOMHDPYqMLcarkSDMKOG_PvLVCGdJbh44n583VbY");

    validator.validateSchema(resource, testContext.failing(response -> testContext.verify(() -> {
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Valid Rating schema")
  void validRatingSchemaTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("rating",4.5)
        .put("comment","some-comment")
        .put("id", "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/pune-env-flood")
        .put("userID", "some-user")
        .put("status", "pending");

    validator.validateRating(request, testContext.succeeding(response -> testContext.verify(() -> {
      String status = response.getString(TITLE);
      assertEquals(SUCCESS, status);
      testContext.completeNow();
    })));
  }

  @Test
  @DisplayName("Invalid Rating schema")
  void invalidRatingSchemaTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject()
        .put("rating",4.5)
        .put("comment","some-comment")
        .put("userID", "some-user")
        .put("status", "pending");

    validator.validateRating(request, testContext.failing(response -> testContext.verify(() -> {
      testContext.completeNow();
    })));
  }
}
