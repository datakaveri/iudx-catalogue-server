package iudx.catalogue.server.validator;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

import iudx.catalogue.server.database.ElasticClient;
import static iudx.catalogue.server.Constants.*;

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
  private static Properties properties;
  private static InputStream inputstream;

  @BeforeAll
  @DisplayName("Deploying Verticle")
  static void startVertx(Vertx vertx, VertxTestContext testContext) {
    vertxObj = vertx;
    /* Read the configuration and set the rabbitMQ server properties. */
    properties = new Properties();
    inputstream = null;

    try {

      inputstream = new FileInputStream(CONFIG_FILE);
      properties.load(inputstream);

      databaseIP = properties.getProperty(DATABASE_IP);
      databasePort = Integer.parseInt(properties.getProperty(DATABASE_PORT));

    } catch (Exception ex) {

      LOGGER.info(ex.toString());
    }

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
  @DisplayName("Test Link Validation [Resource]")
  void validResourceLinkTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request
        .put("type", new JsonArray().add("iudx:Resource"))
        .put(
            "id",
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.iudx.org.in/sensors/sensorA")
        .put(
            "resourceGroup",
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.iudx.org.in/sensors");
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
  @DisplayName("Test Link Validation [ResourceGroup]")
  void validResourceGroupLinkTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request
        .put("type", new JsonArray().add("iudx:ResourceGroup"))
        .put("id", "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.iudx.org.in/sensors")
        .put(
            "provider",
            "rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.iudx.org.in/sensors")
        .put(
            "resourceServer", "rbccps.org/00D75505FD5256B142AFD9C0E32790FA7180D500/rs.iudx.org.in");
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
  @DisplayName("Test Invalid Link [ResourceGroup]")
  void invalidResourceGroupLinkTest(VertxTestContext testContext) {
    JsonObject request = new JsonObject();
    request
        .put("type", new JsonArray().add("iudx:Resource"))
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
  @DisplayName("Valid Schema Test [Resource]")
  void validResourceSchemaTest(VertxTestContext testContext) {
    try {
      JsonObject request =
          new JsonObject(
              "{\"@context\": \"https://voc.iudx.org.in/\",\"type\": [\"iudx:Resource\"],\"id\": \"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.iudx.org.in/sensors/sensorA\",\"name\": \"sensorA\",\"description\": \"Description of this resource\",\"tags\": \"sensor, sensing, resource, battery operated\",\"itemStatus\": \"ACTIVE\",\"resourceGroup\": \"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.iudx.org.in/sensors\",\"provider\": \"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531\",\"itemCreatedAt\": \"2020-07-01T10:03:26+0000\",\"location\": {\"type\": \"Place\",\"address\": \"IISc, Bangalore-560092, India\",\"geometry\": {\"type\": \"Point\",\"coordinates\": [75.92,14.5]}},\"itemModifiedAt\": \"2020-07-01T10:03:26+0000\"}");
      System.out.println(request.toString());
      validator.validateSchema(
          request,
          testContext.succeeding(
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
  @DisplayName("Valid Schema Test [ResourceGroup]")
  void validResourceGroupSchemaTest(VertxTestContext testContext) {
    try {
      JsonObject request =
          new JsonObject(
              "{\"@context\": \"https://voc.iudx.org.in/\",\"type\": [\"iudx:ResourceGroup\"],\"id\":\"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531/rs.iudx.org.in/sensors\",\"description\": \"Description of this resource group\",\"name\": \"sensors\",\"tags\": \"sensor, sensing, resource, battery operated\",\"itemStatus\": \"ACTIVE\",\"provider\": \"rbccps.org/aa9d66a000d94a78895de8d4c0b3a67f3450e531\",\"resourceServer\": \"rbccps.org/00D75505FD5256B142AFD9C0E32790FA7180D500/rs.iudx.org.in\",\"resourceAuthControlLevel\": \"INDIVIDUAL\",\"resourceType\": \"messageStream\",\"authServerInfo\": {\"type\": [\"AuthServerInfoValue\"],\"authServerURL\": \"https://auth.iudx.org.in\",\"authType\": \"iudx-auth\"},\"accessObjectInfo\":{\"type\": [\"AccessObjectInfoValue\"],\"accessObject\": \"https://example.com/sensorsApis.json\",\"additionalInfoURL\": \"https://example.com/sensorsApis\",\"accessObjectType\": \"openAPI\"},\"iudxResourceAPIs\": [\"attribute\", \"temporal\"],\"itemCreatedAt\": \"2019-02-20T10:30:06.093121\",\"location\": {\"type\": \"Place\",\"address\": \"Bangalore\"}}");
      System.out.println(request.toString());
      validator.validateSchema(
          request,
          testContext.succeeding(
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
