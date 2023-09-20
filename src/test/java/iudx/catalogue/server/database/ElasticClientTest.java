package iudx.catalogue.server.database;

import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.geocoding.util.Constants.*;
import static iudx.catalogue.server.util.Constants.*;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.unit.Async;
import iudx.catalogue.server.util.Constants;
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
  private static String docIndex,ratingIndex;
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
    ratingIndex = elasticConfig.getString(RATING_INDEX);

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
    elasticClient.searchAsync(query.toString(), docIndex, res -> {
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
  @DisplayName("test get rating aggs")
  void testGetRatingAggregations(VertxTestContext testContext) {

    StringBuilder req = new StringBuilder(GET_AVG_RATING_PREFIX).append(GET_AVG_RATING_MATCH_QUERY.replace("$1", "b58da193-23d9-43eb-b98a-a103d4b6103c"));
    req.deleteCharAt(req.lastIndexOf(","));
    req.append(GET_AVG_RATING_SUFFIX);
    LOGGER.debug(req);
    elasticClient.ratingAggregationAsync(req.toString(), ratingIndex, res -> {
      if(res.succeeded()) {
        LOGGER.debug(res.result());
        testContext.completeNow();
      } else {
        testContext.failNow("test failed");
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

  JsonArray newWordVector() {
    JsonArray result = new JsonArray();
    for(int i=0;i<100;i++)
      result.add(i,Math.random()*5.0);
    return result;
  }


  @Test
  @Description("test script location search")
  public void testScriptLocationSearch(VertxTestContext vertxTestContext) {
    JsonArray wordVector = newWordVector();
    JsonObject params = new JsonObject()
        .put(COUNTRY,"India")
        .put(REGION,"Karnataka")
        .put(COUNTY, "Bangalore")
        .put(LOCALITY, "Bangalore")
        .put(BOROUGH, "East Bangalore")
        .put(Constants.BBOX, new JsonArray().add(0, 77.5).add(1,13.0).add(2,77.6).add(3,13.1));

    elasticClient.scriptLocationSearch(wordVector, params).onComplete(handler -> {
      if(handler.succeeded()) {
        vertxTestContext.completeNow();
      } else {
        vertxTestContext.failNow(handler.cause());
      }
    });


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
  @Description("testing searchAsyncGeoQuery method")
  public void testSearchAsyncGeoQuery(VertxTestContext vertxTestContext) {
    String query = "dummy query";
    String index = "dummy index";
    assertNotNull(elasticClient.searchAsyncGeoQuery(query,index,handler));
    vertxTestContext.completeNow();
  }
  @Test
  @Description("testing searchAsyncGetId method")
  public void testSearchAsyncGetId(VertxTestContext vertxTestContext) {
    String query = "dummy query";
    String index = "dummy index";
    assertNotNull(elasticClient.searchAsyncGetId(query,index,handler));
    vertxTestContext.completeNow();
  }
  @Test
  @Description("test countAsync method")
  public void testCountAsync(VertxTestContext vertxTestContext) {
    String query="dummy";
    assertNotNull(elasticClient.countAsync(query,docIndex, handler));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("test searchAsyncDataset method")
  public void testSearchAsync(VertxTestContext vertxTestContext) {
    String query="dummy";
    assertNotNull(elasticClient.searchAsyncDataset(query,docIndex, handler));
    vertxTestContext.completeNow();
  }
  @Test
  @Description("test docPostAsync method")
  public void testDocPostAsync(VertxTestContext vertxTestContext) {
    String query="dummy";
    assertNotNull(elasticClient.docPostAsync(query,docIndex, handler));
    vertxTestContext.completeNow();
  }
  @Test
  @Description("test docPutAsync method")
  public void testDocPutAsync(VertxTestContext vertxTestContext) {
    String query="dummy";
    String doc = "dummy";
    assertNotNull(elasticClient.docPutAsync(query,docIndex,doc, handler));
    vertxTestContext.completeNow();
  }
  @Test
  @Description("test docDelAsync method")
  public void testDocDelAsync(VertxTestContext vertxTestContext) {
    String query="dummy";
    assertNotNull(elasticClient.docDelAsync(query,docIndex, handler));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing resourceAggAsync method")
  public void testResourceAggAsync(VertxTestContext vertxTestContext) {
    String query = "dummy query";
    String index = "dummy index";
    assertNotNull(elasticClient.resourceAggregationAsync(query,index,handler));
    vertxTestContext.completeNow();
  }
}
