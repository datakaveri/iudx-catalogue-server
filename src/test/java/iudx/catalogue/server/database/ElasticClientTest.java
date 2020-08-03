package iudx.catalogue.server.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.elasticsearch.client.Request;

import iudx.catalogue.server.database.ElasticClient;

@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ElasticClientTest {
  private static Logger logger = LoggerFactory.getLogger(ElasticClientTest.class);
  private static ElasticClient client;
  private static Properties properties;
  private static InputStream inputstream;
  private static String databaseIP;
  private static int databasePort;
  private Request elasticRequest;

  @BeforeAll
  @DisplayName("")
  static void initClient(Vertx vertx, VertxTestContext testContext) {
    /* Read the configuration and set the rabbitMQ server properties. */
    properties = new Properties();

    try {
      inputstream = new FileInputStream(Constants.CONFIG_FILE);
      properties.load(inputstream);
      databaseIP = properties.getProperty(Constants.DATABASE_IP);
      databasePort = Integer.parseInt(properties.getProperty(Constants.DATABASE_PORT));
      client = new ElasticClient(databaseIP, databasePort);

      logger.info("Read config file");
      logger.info("IP is " + databaseIP);

    } catch (Exception ex) {

      logger.info(ex.toString());
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
        logger.info("Succeeded");
        logger.info(res.result());
        logger.info("Computed size = " + res.result().getJsonArray("results").size());
        testContext.completeNow();
      } else {
        logger.info("Failed");
        logger.info(res.cause());
        testContext.failed();
      }
    });

  }
}
