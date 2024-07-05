package iudx.catalogue.server.database.elastic;

import static iudx.catalogue.server.database.Constants.ID_KEYWORD;
import static iudx.catalogue.server.database.elastic.query.Queries.*;
import static iudx.catalogue.server.geocoding.util.Constants.*;
import static iudx.catalogue.server.util.Constants.*;
import static junit.framework.Assert.assertNotNull;

import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.AggregationBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.catalogue.server.Configuration;
import iudx.catalogue.server.apiserver.stack.QueryBuilder;
import iudx.catalogue.server.util.Constants;
import java.util.List;
import jdk.jfr.Description;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
  private static final Logger LOGGER = LogManager.getLogger(ElasticClientTest.class);
  private static ElasticClient elasticClient;
  private static String databaseIP;
  private static int databasePort;
  private static String docIndex, ratingIndex;
  private static String databaseUser;
  private static String databasePassword;
  private static String databaseIndex;
  @Mock Handler<AsyncResult<JsonObject>> handler;

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

    elasticClient =
        new ElasticClient(databaseIP, databasePort, docIndex, databaseUser, databasePassword);

    LOGGER.info("Read config file");
    testContext.completeNow();
  }

  @Test
  @Order(1)
  @DisplayName("Test Get all")
  void TestGetAll(VertxTestContext testContext) {
    JsonObject query =
        new JsonObject().put("query", new JsonObject().put("match_all", new JsonObject()));
    elasticClient.searchAsync(
        query.toString(),
        docIndex,
        res -> {
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

    Query query =
        new Query.Builder()
            .bool(b -> b.filter(f -> f.match(m -> m.field("type").query(ITEM_TYPE_RESOURCE_GROUP))))
            .build();
    Aggregation aggregation =
        Aggregation.of(a -> a.terms(t -> t.field("id.keyword").size(FILTER_PAGINATION_SIZE)));

    LOGGER.debug("query is " + query);
    LOGGER.debug("Aggregation query is " + aggregation);
    elasticClient.listAggregationAsync(
        query,
        aggregation,
        res -> {
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

    Query matchQuery =
        QueryBuilders.match(m -> m.field(ID_KEYWORD).query("b58da193-23d9-43eb-b98a-a103d4b6103c"));
    Query avgQuery =
        QueryBuilders.bool(
            b ->
                b.should(matchQuery)
                    .minimumShouldMatch("1")
                    .must(QueryBuilders.match(m -> m.field("status").query("approved"))));
    LOGGER.debug(avgQuery);
    elasticClient.ratingAggregationAsync(
        avgQuery,
        buildAvgRatingAggregation(),
        ratingIndex,
        res -> {
          if (res.succeeded()) {
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
    JsonArray queryVector = new JsonArray();
    assertNotNull(elasticClient.scriptSearch(queryVector, handler));
    vertxTestContext.completeNow();
  }

  JsonArray newWordVector() {
    JsonArray result = new JsonArray();
    for (int i = 0; i < 100; i++) result.add(i, Math.random() * 5.0);
    return result;
  }

  @Test
  @Description("test script location search")
  public void testScriptLocationSearch(VertxTestContext vertxTestContext) {
    JsonArray wordVector = newWordVector();
    JsonObject params =
        new JsonObject()
            .put(COUNTRY, "India")
            .put(REGION, "Karnataka")
            .put(COUNTY, "Bangalore")
            .put(LOCALITY, "Bangalore")
            .put(BOROUGH, "East Bangalore")
            .put(
                Constants.BBOX,
                new JsonArray().add(0, 77.5).add(1, 13.0).add(2, 77.6).add(3, 13.1));

    elasticClient
        .scriptLocationSearch(wordVector, params)
        .onComplete(
            handler -> {
              if (handler.succeeded()) {
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @Description("testing ratingAggreagationAsync method")
  public void testRatingAggreagate(VertxTestContext vertxTestContext) {
    Query matchQuery =
        QueryBuilders.match(m -> m.field(ID_KEYWORD).query("b58da193-23d9-43eb-b98a-a103d4b6103c"));
    Query avgQuery =
        QueryBuilders.bool(
            b ->
                b.should(matchQuery)
                    .minimumShouldMatch("1")
                    .must(QueryBuilders.match(m -> m.field("status").query("approved"))));
    LOGGER.debug(avgQuery);
    assertNotNull(
        elasticClient.ratingAggregationAsync(
            avgQuery, buildAvgRatingAggregation(), ratingIndex, handler));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing searchAsyncGeoQuery method")
  public void testSearchAsyncGeoQuery(VertxTestContext vertxTestContext) {
    String index = "dummy index";
    Query query = MatchQuery.of(m -> m.field("dummy query").query(""))._toQuery();
    assertNotNull(
        elasticClient.searchAsyncGeoQuery(
            query,
            buildSourceConfig(List.of("id", "location", "instance", "label")),
            index,
            handler));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing searchAsyncGetId method")
  public void testSearchAsyncGetId(VertxTestContext vertxTestContext) {
    Query query = MatchQuery.of(m -> m.field("dummy query").query(""))._toQuery();
    String index = "dummy index";
    assertNotNull(
        elasticClient.searchAsyncGetId(
            query,
            buildSourceConfig(List.of("domainId", "description", "icon", "label", "name")),
            index,
            handler));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("test countAsync method")
  public void testCountAsync(VertxTestContext vertxTestContext) {
    Query query = Query.of(f -> f.term(t -> t.field("dummy").value("")));
    assertNotNull(elasticClient.countAsync(query, docIndex, handler));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("test searchAsyncDataset method")
  public void testSearchAsync(VertxTestContext vertxTestContext) {
    Query query = buildMlayerDatasetQuery("id", "providerId", "cosId");
    assertNotNull(
        elasticClient.searchAsyncDataset(
            query, buildSourceConfig(List.of()), FILTER_PAGINATION_SIZE, docIndex, handler));
    assertNotNull(elasticClient.searchAsync(query, buildAvgRatingAggregation(), docIndex, handler));
    assertNotNull(
        elasticClient.searchAsync(
            query,
            buildSourceConfig(List.of("type", "id")),
            SortOptions.of(s -> s.field(f -> f.field("name").order(SortOrder.Asc))),
            docIndex,
            handler));
    assertNotNull(
        elasticClient.searchAsync(
            query,
            buildSourceConfig(List.of("type")),
            FILTER_PAGINATION_SIZE,
            0,
            docIndex,
            handler));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("test docPostAsync method")
  public void testDocPostAsync(VertxTestContext vertxTestContext) {
    JsonObject query = new JsonObject().put("id", "dummy-id");
    assertNotNull(elasticClient.docPostAsync(docIndex, query.toString(), handler));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("test docPutAsync method")
  public void testDocPutAsync(VertxTestContext vertxTestContext) {
    String query = "dummy";
    JsonObject doc = new JsonObject();
    assertNotNull(elasticClient.docPutAsync(query, docIndex, doc, handler));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("test docPatchAsync method")
  public void testDocPatchAsync(VertxTestContext vertxTestContext) {
    String query = "dummy";
    JsonObject doc =
        new JsonObject()
            .put("type", "dummy")
            .put("titile", "test")
            .put("href", "abc/abc")
            .put("rel", "abc/abcd");
    assertNotNull(
        elasticClient.docPatchAsync(
            query, docIndex, new QueryBuilder().getPatchQuery(doc), handler));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("test docDelAsync method")
  public void testDocDelAsync(VertxTestContext vertxTestContext) {
    String query = "dummy";
    assertNotNull(elasticClient.docDelAsync(query, docIndex, handler));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("test searchAsyncResourceGroupAndProvider method")
  public void testSearchAsyncResourceGroupAndProvider(VertxTestContext vertxTestContext) {
    Query query = Query.of(f -> f.term(t -> t.field("dummy").value("")));
    Aggregation aggregation =
        AggregationBuilders.cardinality().field("dummy").build()._toAggregation();
    assertNotNull(
        elasticClient.searchAsyncResourceGroupAndProvider(
            query,
            aggregation,
            buildSourceConfig(List.of()),
            FILTER_PAGINATION_SIZE,
            docIndex,
            handler));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("test resourceAggregationAsync method")
  public void testResourceAggregationAsync(VertxTestContext vertxTestContext) {
    assertNotNull(
        elasticClient.resourceAggregationAsync(
            buildResourceAccessPolicyCountQuery(), FILTER_PAGINATION_SIZE, docIndex, handler));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("test resourceAggregationAsync method")
  public void testResourceAggregationAsync2(VertxTestContext vertxTestContext) {
    Query query = Query.of(f -> f.term(t -> t.field("dummy").value("")));
    assertNotNull(
        elasticClient.searchGetId(query, buildSourceConfig(List.of("id")), docIndex, handler));
    vertxTestContext.completeNow();
  }
}
