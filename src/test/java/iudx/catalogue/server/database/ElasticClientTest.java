package iudx.catalogue.server.database;

import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import static iudx.catalogue.server.Constants.*;

@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ElasticClientTest {
  private static final Logger LOGGER = LogManager.getLogger(ElasticClientTest.class);
  private static ElasticClient client;
  private static Properties properties;
  private static InputStream inputstream;
  private static String databaseIP;
  private static int databasePort;
  private static String databaseUser;
  private static String databasePassword;

  @BeforeAll
  @DisplayName("")
  static void initClient(Vertx vertx, VertxTestContext testContext) {
    /* Read the configuration and set the rabbitMQ server properties. */
    properties = new Properties();

    try {
      inputstream = new FileInputStream(CONFIG_FILE);
      properties.load(inputstream);
      databaseIP = properties.getProperty(DATABASE_IP);
      databasePort = Integer.parseInt(properties.getProperty(DATABASE_PORT));
      client = new ElasticClient(databaseIP, databasePort, databaseUser, databasePassword);

      LOGGER.info("Read config file");
      LOGGER.info("IP is " + databaseIP);

    } catch (Exception ex) {

      LOGGER.info(ex.toString());
    }

    testContext.completeNow();
  }

  @Test
  @Order(1)
  @DisplayName("Test Get all")
  void TestGetAll(VertxTestContext testContext) {
    JsonObject query = new JsonObject().put("query", new JsonObject()
                                        .put("match_all", new JsonObject()));
    client.searchAsync("testindex", query.toString(), res -> {
      if (res.succeeded()) {
        LOGGER.info("Succeeded");
        LOGGER.info(res.result());
        LOGGER.info("Computed size = " + res.result().getJsonArray("results").size());
        testContext.completeNow();
      } else {
        LOGGER.info("Failed");
        LOGGER.info(res.cause());
        testContext.failed();
      }
    });
  }

  @Test
  @Order(2)
  @DisplayName("Test Get aggregations")
  void TestGetAggregations(VertxTestContext testContext) {

    LOGGER.info("Reached get aggregations");

    String req = "{\"query\":{\"bool\":{\"filter\":[{\"match\":{\"type\":\"iudx:ResourceGroup\"}}]}},\"aggs\":{\"results\":{\"terms\":{\"field\":\"id.keyword\",\"size\":10000}}}}";
    LOGGER.info("Aggregation query is " + req);
    client.listAggregationAsync("testindex", req, res -> {
      if (res.succeeded()) {
        LOGGER.info("Succeeded");
        LOGGER.info(res.result());
        LOGGER.info("Computed size = " + res.result().getJsonArray("results").size());
        testContext.completeNow();
      } else {
        LOGGER.info("Failed");
        LOGGER.info(res.cause());
        testContext.failed();
      }
    });
  }

}
