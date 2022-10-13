package iudx.catalogue.server.database;

import static iudx.catalogue.server.util.Constants.*;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.unit.Async;
import jdk.jfr.Description;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.core.Vertx;
import iudx.catalogue.server.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ElasticClientTest {
  @Mock
  Handler<AsyncResult<JsonObject>> handler;
  private static final Logger LOGGER = LogManager.getLogger(ElasticClientTest.class);
  private static ElasticClient elasticClient;
  private static String databaseIP;
  private static int databasePort;
  private static String docIndex;
  private static String databaseUser;
  private static String databasePassword;
  private static String databaseIndex;

  @BeforeAll
  @DisplayName("")
  static void initClient(Vertx vertx, VertxTestContext testContext) {
    /* Read the configuration and set the rabbitMQ server properties. */

    JsonObject elasticConfig = Configuration.getConfiguration("./configs/config-test.json", 0);
    databaseIndex =
        Configuration.getConfiguration("./configs/config-test.json").getString("databaseIndex");

    databaseIP = elasticConfig.getString(DATABASE_IP);
    databasePort = elasticConfig.getInteger(DATABASE_PORT);
    databaseUser = elasticConfig.getString(DATABASE_UNAME);
    databasePassword = elasticConfig.getString(DATABASE_PASSWD);
    docIndex = elasticConfig.getString(DOC_INDEX);

    elasticClient = new ElasticClient(databaseIP, databasePort, docIndex, databaseUser, databasePassword);

    LOGGER.info("Read config file");
    testContext.completeNow();
  }

  @Test
  @Order(1)
  @DisplayName("Test Get all")
  void TestGetAll(VertxTestContext testContext) {
    JsonObject query = new JsonObject().put("query", new JsonObject()
                                        .put("match_all", new JsonObject()));
    elasticClient.searchAsync(query.toString(), res -> {
      if (res.succeeded()) {
        LOGGER.info("Succeeded");
        LOGGER.debug(res.result());
        LOGGER.debug("Computed size = " + res.result().getJsonArray("results").size());
        testContext.completeNow();
      } else {
        LOGGER.error("Failed, cause:" + res.cause());
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
    LOGGER.debug("Aggregation query is " + req);
    elasticClient.listAggregationAsync(req, res -> {
      if (res.succeeded()) {
        LOGGER.info("Succeeded");
        LOGGER.debug(res.result());
        LOGGER.debug("Computed size = " + res.result().getJsonArray("results").size());
        testContext.completeNow();
      } else {
        LOGGER.error("Failed, cause:" + res.cause());
        testContext.failed();
      }
    });
  }
  @Test
  @Description("test script search method ")
  public void testScriptSearch(VertxTestContext vertxTestContext) {
    JsonArray queryVector=new JsonArray();
    assertNotNull(elasticClient.scriptSearch(queryVector,handler));
    vertxTestContext.completeNow();
  }
  @Test
  @Description("testing ratingAggreagationAsync method")
  public void testRatingAggreagate(VertxTestContext vertxTestContext) {
    String queryVector="dummy";
    String bbox= "dummy";
    assertNotNull(elasticClient.ratingAggregationAsync(queryVector,bbox,handler));
    vertxTestContext.completeNow();
  }
  @Test
  @Description("test countAsync method")
  public void testCountAsync(VertxTestContext vertxTestContext) {
    String query="dummy";
    assertNotNull(elasticClient.countAsync(query,handler));
    vertxTestContext.completeNow();
  }
}
