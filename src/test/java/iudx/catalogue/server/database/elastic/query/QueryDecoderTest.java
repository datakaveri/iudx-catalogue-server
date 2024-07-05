package iudx.catalogue.server.database.elastic.query;

import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.util.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.catalogue.server.Configuration;
import iudx.catalogue.server.database.RespBuilder;
import iudx.catalogue.server.database.elastic.ElasticsearchVerticle;
import iudx.catalogue.server.database.elastic.model.QueryAndAggregation;
import jakarta.json.JsonValue;
import java.util.List;
import java.util.stream.Stream;
import jdk.jfr.Description;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class QueryDecoderTest {
  public static final Logger LOGGER = LogManager.getLogger(QueryDecoderTest.class);

  private static QueryDecoder queryDecoder;

  @BeforeAll
  @DisplayName("Deploying Verticle")
  static void startVertx(Vertx vertx, VertxTestContext testContext) {

    JsonObject elasticConfig = Configuration.getConfiguration("./configs/config-test.json", 0);

    vertx.deployVerticle(
        new ElasticsearchVerticle(),
        new DeploymentOptions().setConfig(elasticConfig),
        testContext.completing());
    queryDecoder = new QueryDecoder();
    testContext.completed();
  }

  static Stream<Arguments> mustQuery() {
    return Stream.of(
        Arguments.of(RESOURCE, ITEM_TYPE_RESOURCE),
        Arguments.arguments(RESOURCE_GRP, ITEM_TYPE_RESOURCE_GROUP),
        Arguments.arguments(RESOURCE_SVR, ITEM_TYPE_RESOURCE_SERVER),
        Arguments.arguments(PROVIDER, ITEM_TYPE_PROVIDER));
  }

  static Stream<Arguments> shouldQuery() {
    return Stream.of(
        Arguments.arguments(RESOURCE_GRP),
        Arguments.arguments(PROVIDER),
        Arguments.arguments(RESOURCE_SVR),
        Arguments.arguments(COS_ITEM));
  }

  @Test
  @Order(1)
  @DisplayName("GeoPoint request to DbQuery")
  public void searchGeoPointTest(VertxTestContext testContext) {

    JsonObject requests =
        new JsonObject()
            .put(GEOPROPERTY, LOCATION)
            .put(GEORELATION, GEOREL_WITHIN)
            .put(MAX_DISTANCE, 5000)
            .put(GEOMETRY, POINT)
            .put(COORDINATES, new JsonArray().add(73.927285).add(18.502712))
            .put(SEARCH_TYPE, SEARCH_TYPE_GEO)
            .put(SEARCH, true);

    JsonObject q = queryDecoder.searchQuery(requests);
    Query query = (Query) q.getValue("query");

    assertNotNull(query);
    testContext.completeNow();
  }

  @Test
  @Order(2)
  @DisplayName("GeoPolygon request to DbQuery")
  public void searchGeoPolygonTest(VertxTestContext testContext) {
    JsonArray coordinates =
        new JsonArray()
            .add(
                new JsonArray()
                    .add(new JsonArray().add(75.9).add(14.5))
                    .add(new JsonArray().add(72).add(13))
                    .add(new JsonArray().add(73).add(20))
                    .add(new JsonArray().add(75.9).add(14.5)));
    JsonObject requests =
        new JsonObject()
            .put(GEOPROPERTY, LOCATION)
            .put(GEORELATION, GEOREL_WITHIN)
            .put(MAX_DISTANCE, 5000)
            .put(GEOMETRY, POLYGON)
            .put(COORDINATES, coordinates)
            .put(SEARCH_TYPE, SEARCH_TYPE_GEO)
            .put(SEARCH, true);

    JsonObject q = queryDecoder.searchQuery(requests);
    Query query = (Query) q.getValue("query");
    // Extract the Bool query
    BoolQuery boolQuery = query.bool();
    List<Query> filters = boolQuery.filter();

    // Find the GeoShape query within the filters
    GeoShapeQuery geoShapeQuery = null;
    for (Query filterQuery : filters) {
      Query.Kind GeoShapeQuery = Query.Kind.GeoShape;
      if (filterQuery._kind().equals(GeoShapeQuery)) {
        geoShapeQuery = filterQuery.geoShape();
        // LOGGER.debug(geoShapeQuery);
        break;
      }
    }

    assertNotNull(geoShapeQuery);
    // Convert the shape to JsonObject and extract the coordinates
    String shapeString = geoShapeQuery.shape().shape().toJson().toString();
    JsonObject shapeJson = new JsonObject(shapeString);
    JsonArray extractedCoordinates =
        new JsonArray(shapeJson.getJsonArray("coordinates").toString());

    // Log and assert the coordinates
    LOGGER.debug("Expected Coordinates: {}", coordinates);
    LOGGER.debug("Extracted Coordinates: {}", extractedCoordinates);
    assertEquals(coordinates, extractedCoordinates);
    testContext.completeNow();
  }

  @Test
  @Order(3)
  @DisplayName("GeoBbox request to DbQuery")
  public void searchGeoBboxTest(VertxTestContext testContext) {

    JsonObject requests =
        new JsonObject()
            .put(GEOPROPERTY, LOCATION)
            .put(GEORELATION, GEOREL_WITHIN)
            .put(MAX_DISTANCE, 5000)
            .put(GEOMETRY, BBOX)
            .put(
                COORDINATES,
                new JsonArray()
                    .add(new JsonArray().add(73).add(20))
                    .add(new JsonArray().add(75).add(14)))
            .put(SEARCH_TYPE, SEARCH_TYPE_GEO)
            .put(SEARCH, true);

    JsonObject q = queryDecoder.searchQuery(requests);
    Query query = (Query) q.getValue("query");
    // Extract the Bool query
    BoolQuery boolQuery = query.bool();
    List<Query> filters = boolQuery.filter();

    // Find the GeoShape query within the filters
    GeoShapeQuery geoShapeQuery = null;
    for (Query filterQuery : filters) {
      Query.Kind GeoShapeQuery = Query.Kind.GeoShape;
      LOGGER.debug(filterQuery._kind());
      LOGGER.debug(GeoShapeQuery);
      if (filterQuery._kind().equals(GeoShapeQuery)) {
        geoShapeQuery = filterQuery.geoShape();
        LOGGER.debug(geoShapeQuery);
        break;
      }
    }

    assertNotNull(geoShapeQuery);
    JsonValue shapeJson = geoShapeQuery.shape().shape().toJson();
    assertEquals(GEO_BBOX, shapeJson.asJsonObject().getString("type"));
    testContext.completeNow();
  }

  @Test
  @Order(4)
  @DisplayName("GeoLineString request to DbQuery")
  public void searchGeoLineStringTest(VertxTestContext testContext) {

    JsonObject requests =
        new JsonObject()
            .put(GEOPROPERTY, LOCATION)
            .put(GEORELATION, GEOREL_WITHIN)
            .put(MAX_DISTANCE, 5000)
            .put(GEOMETRY, LINESTRING)
            .put(
                COORDINATES,
                new JsonArray()
                    .add(new JsonArray().add(73.874537).add(18.528311))
                    .add(new JsonArray().add(73.836808).add(18.572797))
                    .add(new JsonArray().add(73.876484).add(18.525007)))
            .put(SEARCH_TYPE, SEARCH_TYPE_GEO)
            .put(SEARCH, true);

    JsonObject q = queryDecoder.searchQuery(requests);
    Query query = (Query) q.getValue("query");
    // Extract the Bool query
    BoolQuery boolQuery = query.bool();
    List<Query> filters = boolQuery.filter();

    // Find the GeoShape query within the filters
    GeoShapeQuery geoShapeQuery = null;
    for (Query filterQuery : filters) {
      Query.Kind GeoShapeQuery = Query.Kind.GeoShape;
      LOGGER.debug(filterQuery._kind());
      LOGGER.debug(GeoShapeQuery);
      if (filterQuery._kind().equals(GeoShapeQuery)) {
        geoShapeQuery = filterQuery.geoShape();
        LOGGER.debug(geoShapeQuery);
        break;
      }
    }

    assertNotNull(geoShapeQuery);
    JsonValue shapeJson = geoShapeQuery.shape().shape().toJson();
    assertEquals(LINESTRING, shapeJson.asJsonObject().getString("type"));
    testContext.completeNow();
  }

  @Test
  @Order(5)
  @DisplayName("Invalid QueryParameters")
  public void searchInvalidQueryParametersTest(VertxTestContext testContext) {

    JsonObject requests =
        new JsonObject()
            .put(GEOPROPERTY, LOCATION)
            .put(GEORELATION, GEOREL_WITHIN)
            .put(MAX_DISTANCE, 5000)
            .put(GEOMETRY, SHAPE_KEY)
            .put(
                COORDINATES,
                new JsonArray()
                    .add(new JsonArray().add(73.874537).add(18.528311))
                    .add(new JsonArray().add(73.836808).add(18.572797))
                    .add(new JsonArray().add(73.876484).add(18.525007)))
            .put(SEARCH_TYPE, SEARCH_TYPE_GEO)
            .put(SEARCH, true);

    JsonObject q = queryDecoder.searchQuery(requests);

    assertEquals(DETAIL_INVALID_GEO_PARAMETER, q.getJsonObject(ERROR).getString("detail"));
    testContext.completeNow();
  }

  @Test
  @Order(6)
  @DisplayName("Text search request to DbQuery")
  public void searchTextTest(VertxTestContext testContext) {

    JsonObject requests =
        new JsonObject()
            .put(Q_VALUE, "Golibar Square")
            .put(SEARCH_TYPE, SEARCH_TYPE_TEXT)
            .put(SEARCH, true);

    JsonObject q = queryDecoder.searchQuery(requests);
    Query query = (Query) q.getValue("query");
    assertEquals("Golibar Square", query.bool().filter().get(0).queryString().query());
    testContext.completeNow();
  }

  @Test
  @Order(7)
  @DisplayName("Attribute search request to DbQuery")
  public void searchAttributeTest(VertxTestContext testContext) {

    JsonObject requests =
        new JsonObject()
            .put(PROPERTY, new JsonArray().add(ID))
            .put(
                VALUE,
                new JsonArray()
                    .add(
                        new JsonArray()
                            .add(
                                "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs."
                                    + "iudx.io/aqm-bosch-climo/Ambedkar society circle_29")))
            .put(SEARCH_TYPE, SEARCH_TYPE_ATTRIBUTE)
            .put(SEARCH, true);

    JsonObject q = queryDecoder.searchQuery(requests);
    Query query = (Query) q.getValue("query");
    assertEquals(
        "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs.iudx.io/aqm-bosch-climo/Ambedkar society circle_29",
        query
            .bool()
            .filter()
            .get(0)
            .bool()
            .must()
            .get(0)
            .bool()
            .should()
            .get(0)
            .match()
            .query()
            .stringValue());
    testContext.completeNow();
  }

  @Test
  @Order(8)
  @DisplayName("Tag search request to DbQuery")
  public void searchTagTest(VertxTestContext testContext) {

    JsonObject requests =
        new JsonObject()
            .put(PROPERTY, new JsonArray().add(TAGS))
            .put(VALUE, new JsonArray().add(new JsonArray().add("pollution")))
            .put(SEARCH_TYPE, SEARCH_TYPE_ATTRIBUTE)
            .put(SEARCH, true);

    JsonObject q = queryDecoder.searchQuery(requests);
    Query query = (Query) q.getValue("query");
    assertNotNull(query);
    assertEquals(
        "pollution",
        query
            .bool()
            .filter()
            .get(0)
            .bool()
            .must()
            .get(0)
            .bool()
            .should()
            .get(0)
            .match()
            .query()
            .stringValue());
    testContext.completeNow();
  }

  @Test
  @Order(9)
  @DisplayName("Relationship search request to DbQuery")
  public void searchRelationshipTest(VertxTestContext testContext) {

    JsonObject requests =
        new JsonObject()
            .put(ID, "2596264a-ff2a-40f7-90cc-17a57b2adffe")
            .put(ITEM_TYPE, "iudx:Resource")
            .put(RESOURCE_GRP, "rg-id")
            .put(RELATIONSHIP, RESOURCE_GRP);

    Query query = queryDecoder.listRelationshipQuery(requests);
    assertNotNull(query);
    assertEquals(ITEM_TYPE_RESOURCE_GROUP, query.bool().must().get(1).term().value().stringValue());
    testContext.completeNow();
  }

  @Test
  @Order(10)
  @DisplayName("Relationship search request to DbQuery")
  public void searchRelationshipTest2(VertxTestContext testContext) {

    JsonObject requests =
        new JsonObject()
            .put(ID, "2596264a-ff2a-40f7-90cc-17a57b2adffe")
            .put(ITEM_TYPE, "iudx:Resource")
            .put(RESOURCE_SVR, "rs-id")
            .put(RELATIONSHIP, RESOURCE_SVR);

    Query query = queryDecoder.listRelationshipQuery(requests);

    assertNotNull(query);
    assertEquals(ITEM_TYPE_RESOURCE_SERVER, query.bool().must().get(1).term().value().stringValue());
    testContext.completeNow();
  }

  @Test
  @Order(11)
  @DisplayName("Relationship search request to DbQuery")
  public void listItemTagTest(VertxTestContext testContext) {

    JsonObject requests = new JsonObject().put(ITEM_TYPE, TAGS).put(TYPE, TAGS);

    QueryAndAggregation queryAndAggregation = queryDecoder.listItemQuery(requests);
    Query query = queryAndAggregation.getQuery();
    Aggregation aggregation = queryAndAggregation.getAggregation();
    LOGGER.debug("Query: " + query);
    LOGGER.debug("Aggregation: " + aggregation);

    assertEquals(TAGS + KEYWORD_KEY, aggregation.terms().field());
    testContext.completeNow();
  }

  @Test
  @Description("test listItemQuery method when itemType equals TAGS")
  public void testListItemQueryTag(VertxTestContext vertxTestContext) {
    queryDecoder = new QueryDecoder();
    JsonObject request = new JsonObject();
    request.put(ITEM_TYPE, TAGS);
    request.put(INSTANCE, "dummy").put(LIMIT, 2).put(OFFSET, 1);
    QueryAndAggregation queryAndAggregation = queryDecoder.listItemQuery(request);
    Query query = queryAndAggregation.getQuery();
    Aggregation aggregation = queryAndAggregation.getAggregation();
    LOGGER.debug("Query: " + query);
    LOGGER.debug("Aggregation: " + aggregation);
    assertEquals(INSTANCE + KEYWORD_KEY, query.term().field());
    assertEquals("dummy", query.term().value().stringValue());
    assertEquals(TAGS + KEYWORD_KEY, aggregation.terms().field());
    assertEquals(request.getInteger(LIMIT), aggregation.terms().size());
    vertxTestContext.completeNow();
  }

  @Test
  @Description("test listItemQuery method when itemType not equals TAGS")
  public void testListItemQuery(VertxTestContext vertxTestContext) {
    queryDecoder = new QueryDecoder();
    JsonObject request = new JsonObject();
    request.put(ITEM_TYPE, "dummy").put(TYPE_KEY, "dummy").put(LIMIT, 2).put(OFFSET, 1);
    request.put(INSTANCE, null);
    QueryAndAggregation queryAndAggregation = queryDecoder.listItemQuery(request);
    Query query = queryAndAggregation.getQuery();
    Aggregation aggregation = queryAndAggregation.getAggregation();

    assertEquals("dummy", query.bool().filter().get(0).match().query().stringValue());
    assertEquals(ID_KEYWORD, aggregation.terms().field());
    assertEquals(request.getInteger(LIMIT), aggregation.terms().size());
    vertxTestContext.completeNow();
  }

  @Test
  @Description(
      "test listItemQuery method when itemType not equals TAGS and instanceID is not null/empty")
  public void testListItemQueryInstance(VertxTestContext vertxTestContext) {
    queryDecoder = new QueryDecoder();
    JsonObject request = new JsonObject();
    request.put(ITEM_TYPE, "dy").put(TYPE_KEY, "dumy").put(LIMIT, 2).put(OFFSET, 1);
    request.put(INSTANCE, "dummy");
    QueryAndAggregation queryAndAggregation = queryDecoder.listItemQuery(request);
    Query query = queryAndAggregation.getQuery();
    Aggregation aggregation = queryAndAggregation.getAggregation();

    assertEquals(TYPE_KEY, query.bool().filter().get(0).match().field());
    assertEquals(
        request.getString(TYPE_KEY), query.bool().filter().get(0).match().query().stringValue());
    assertEquals(INSTANCE + KEYWORD_KEY, query.bool().filter().get(1).term().field());
    assertEquals(
        request.getString(INSTANCE), query.bool().filter().get(1).term().value().stringValue());
    assertEquals(ID_KEYWORD, aggregation.terms().field());
    assertEquals(request.getInteger(LIMIT), aggregation.terms().size());
    vertxTestContext.completeNow();
  }

  @Test
  @Description("test SearchQuery method when searchType equals GEOSEARCH_REGEX")
  public void testSearchQueryGeosearch(VertxTestContext vertxTestContext) {
    queryDecoder = new QueryDecoder();
    JsonObject request = new JsonObject();
    request.put(SEARCH_TYPE, GEOSEARCH_REGEX).put(ITEM_TYPE, "dummy").put(SEARCH, false);

    assertEquals(
        new JsonObject()
            .put(
                ERROR,
                new RespBuilder()
                    .withType(TYPE_INVALID_GEO_PARAM)
                    .withTitle(TITLE_INVALID_GEO_PARAM)
                    .withDetail("Missing/Invalid geo parameters")
                    .getJsonResponse()),
        queryDecoder.searchQuery(request));
    vertxTestContext.completeNow();
  }

  @Test
  @Description(
      "test listItemQuery method when itemType not equals TAGS and instanceID is not null/empty")
  public void testSearchQueryTextSearch(VertxTestContext vertxTestContext) {
    queryDecoder = new QueryDecoder();
    JsonObject request = new JsonObject();
    request.put(SEARCH_TYPE, TEXTSEARCH_REGEX).put(SEARCH, false);
    assertEquals(
        new JsonObject()
            .put(
                ERROR,
                new RespBuilder()
                    .withType(TYPE_BAD_TEXT_QUERY)
                    .withTitle(TITLE_BAD_TEXT_QUERY)
                    .withDetail("bad text query values")
                    .getJsonResponse()),
        queryDecoder.searchQuery(request));
    vertxTestContext.completeNow();
  }

  @Test
  @Description(
      "test listItemQuery method when itemType not equals TAGS and instanceID is not null/empty")
  public void testSearchQuery(VertxTestContext vertxTestContext) {
    queryDecoder = new QueryDecoder();
    JsonObject request = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    jsonArray.add("dummy value");
    jsonArray.add("dummy value");
    JsonArray jsonArray2 = new JsonArray();
    jsonArray2.add("dummy");
    request
        .put(SEARCH_TYPE, ATTRIBUTE_SEARCH_REGEX)
        .put(SEARCH, false)
        .put(PROPERTY, jsonArray)
        .put(VALUE, jsonArray2);
    assertEquals(
        new JsonObject()
            .put(
                ERROR,
                new RespBuilder()
                    .withType(TYPE_INVALID_PROPERTY_VALUE)
                    .withTitle(TITLE_INVALID_PROPERTY_VALUE)
                    .withDetail("Invalid Property Value")
                    .getJsonResponse()),
        queryDecoder.searchQuery(request));
    vertxTestContext.completeNow();
  }

  @Test
  @Description(
      "test listItemQuery method when itemType not equals TAGS and instanceID is not null/empty")
  public void testSearchQueryAttributeSearch(VertxTestContext vertxTestContext) {
    queryDecoder = new QueryDecoder();
    JsonObject request = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    jsonArray.add(0, KEYWORD_KEY);
    JsonArray jsonArray2 = new JsonArray();
    JsonArray jsonArray3 = new JsonArray();
    jsonArray3.add(0, "dummy");
    jsonArray2.add(0, jsonArray3);
    request
        .put(SEARCH_TYPE, ATTRIBUTE_SEARCH_REGEX)
        .put(SEARCH, false)
        .put(PROPERTY, jsonArray)
        .put(VALUE, jsonArray2);
    /* String matchQuery=MATCH_QUERY.replace("$1",request.getJsonArray(PROPERTY).getString(0)).replace("$2",request.getJsonArray(VALUE).getString(0));
     JsonArray shouldQuery=new JsonArray();
     shouldQuery.add(new JsonObject(matchQuery));
     JsonArray mustQuery=new JsonArray();
    mustQuery.add(new JsonObject(SHOULD_QUERY.replace("$1", shouldQuery.toString())));
    JsonObject elasticQuery=new JsonObject();
    JsonObject boolQuery = new JsonObject(MUST_QUERY.replace("$1", mustQuery.toString()));
    */
    Query query = (Query) queryDecoder.searchQuery(request).getValue("query");
    LOGGER.debug("test: " + query);
    vertxTestContext.completeNow();
  }

  @Test
  @Description("test searchquery when instanceId is not null")
  public void testSearchInstance(VertxTestContext vertxTestContext) {
    queryDecoder = new QueryDecoder();
    JsonObject request = new JsonObject();
    request.put(INSTANCE, "dummy").put(SEARCH, false).put(SEARCH_TYPE, "dummy").put(OFFSET, 100);
    assertEquals(
        new JsonObject()
            .put(
                ERROR,
                new RespBuilder()
                    .withType(TYPE_INVALID_SYNTAX)
                    .withTitle(TITLE_INVALID_SYNTAX)
                    .withDetail(TITLE_INVALID_SYNTAX)
                    .getJsonResponse()),
        queryDecoder.searchQuery(request));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing listQueryRelationship test")
  public void testListRelationshipQuery(VertxTestContext vertxTestContext) {
    queryDecoder = new QueryDecoder();
    JsonObject request = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    request.put(RELATIONSHIP, TYPE_KEY);
    request
        .put(ID, "dummy")
        //        .put(ITEM_TYPE, "item:Resource")
        .put(LIMIT, 100)
        .put(OFFSET, 100)
        .put(FILTER, jsonArray)
        .put(LIMIT, 2)
        .put(OFFSET, 1);

    queryDecoder.listRelationshipQuery(request);
    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing seachquery method with limit set to 100")
  public void testsearchQuery(VertxTestContext vertxTestContext) {
    queryDecoder = new QueryDecoder();
    JsonObject request = new JsonObject();
    request.put(LIMIT, 100).put(SEARCH_TYPE, "dummy").put(SEARCH, false);
    request.put(INSTANCE, "dummy");
    JsonObject elasticQuery = new JsonObject();
    elasticQuery.put(SIZE_KEY, request.getInteger(LIMIT));
    assertEquals(
        new JsonObject()
            .put(
                ERROR,
                new RespBuilder()
                    .withType(TYPE_INVALID_SYNTAX)
                    .withTitle(TITLE_INVALID_SYNTAX)
                    .withDetail(TITLE_INVALID_SYNTAX)
                    .getJsonResponse()),
        queryDecoder.searchQuery(request));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing seachQuery method with searchType equals RESPONSE_FILTER_REGEX")
  public void testsearchQueryFilter_Regex(VertxTestContext vertxTestContext) {
    queryDecoder = new QueryDecoder();
    JsonObject request = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    jsonArray.add("dummy");
    request
        .put(SEARCH_TYPE, RESPONSE_FILTER_REGEX)
        .put(SEARCH, true)
        .put(OFFSET, 100)
        .put(ATTRIBUTE, jsonArray)
        .put(INSTANCE, "dummy");
    Query query = (Query) queryDecoder.searchQuery(request).getValue("query");

    assertEquals(INSTANCE, query.bool().must().get(0).term().field());
    assertEquals(
        request.getString(INSTANCE), query.bool().must().get(0).term().value().stringValue());
    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing seachQuery method with searchType equals RESPONSE_FILTER_REGEX")
  public void testListRelationshipQueryID(VertxTestContext vertxTestContext) {
    queryDecoder = new QueryDecoder();
    JsonObject request = new JsonObject();
    request.put(RELATIONSHIP, RESOURCE).put(ID, "dummy").put(ITEM_TYPE, "iudx:ResourceGroup");
    Query query = queryDecoder.listRelationshipQuery(request);
    LOGGER.debug("Query: " + query);

    assertEquals(RESOURCE_GRP + KEYWORD_KEY, query.bool().must().get(0).term().field());
    assertEquals(request.getString(ID), query.bool().must().get(0).term().value().stringValue());
    assertEquals(TYPE_KEYWORD, query.bool().must().get(1).term().field());
    assertEquals(ITEM_TYPE_RESOURCE, query.bool().must().get(1).term().value().stringValue());
    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing seachQuery method with searchType equals RESPONSE_FILTER_REGEX")
  public void testListRelationshipQueryProvider(VertxTestContext vertxTestContext) {
    queryDecoder = new QueryDecoder();
    JsonObject request = new JsonObject();
    request.put(RELATIONSHIP, PROVIDER).put(ID, "abcd/abcd").put(PROVIDER, "abcd/abc");
    Query query = queryDecoder.listRelationshipQuery(request);
    LOGGER.debug("Query: " + query);

    assertEquals(ID_KEYWORD, query.bool().must().get(0).term().field());
    assertEquals(
        request.getString(PROVIDER), query.bool().must().get(0).term().value().stringValue());
    assertEquals(TYPE_KEYWORD, query.bool().must().get(1).term().field());
    assertEquals(ITEM_TYPE_PROVIDER, query.bool().must().get(1).term().value().stringValue());
    vertxTestContext.completeNow();
  }

  @Test
  @Description(
      "testing listRelationshipQuery method when realtionshipType is resource and itemType is provider")
  public void testListRelationshipResource(VertxTestContext vertxTestContext) {
    queryDecoder = new QueryDecoder();
    JsonObject request = new JsonObject();
    request.put(RELATIONSHIP, RESOURCE).put(ID, "dummy").put(ITEM_TYPE, "iudx:Provider");
    Query query = queryDecoder.listRelationshipQuery(request);

    assertEquals(PROVIDER + KEYWORD_KEY, query.bool().must().get(0).term().field());
    assertEquals(request.getString(ID), query.bool().must().get(0).term().value().stringValue());
    assertEquals(TYPE_KEYWORD, query.bool().must().get(1).term().field());
    assertEquals(ITEM_TYPE_RESOURCE, query.bool().must().get(1).term().value().stringValue());
    vertxTestContext.completeNow();
  }

  @Test
  @Description(
      "testing listRelationshipQuery method when realtionshipType is resource group and itemType is resource")
  public void testListRelationshipResourceGroup(VertxTestContext vertxTestContext) {
    queryDecoder = new QueryDecoder();
    JsonObject request = new JsonObject();
    request
        .put(RELATIONSHIP, RESOURCE_GRP)
        .put(ID, "dummy")
        .put(ITEM_TYPE, "iudx:Resource")
        .put("resourceGroup", "dummy id");
    Query query = queryDecoder.listRelationshipQuery(request);

    assertEquals(ID_KEYWORD, query.bool().must().get(0).term().field());
    assertEquals(
        request.getString("resourceGroup"),
        query.bool().must().get(0).term().value().stringValue());
    assertEquals(TYPE_KEYWORD, query.bool().must().get(1).term().field());
    assertEquals(ITEM_TYPE_RESOURCE_GROUP, query.bool().must().get(1).term().value().stringValue());
    vertxTestContext.completeNow();
  }

  @Test
  @Description(
      "testing listRelationshipQuery method when realtionshipType is resource group and itemType is provider")
  public void testListRelationshipItemProvider(VertxTestContext vertxTestContext) {
    queryDecoder = new QueryDecoder();
    JsonObject request = new JsonObject();
    request
        .put(RELATIONSHIP, RESOURCE_GRP)
        .put(ID, "dummy")
        .put(ITEM_TYPE, "iudx:Provider")
        .put("resourceGroup", "dummy id");
    Query query = queryDecoder.listRelationshipQuery(request);

    assertEquals(PROVIDER + KEYWORD_KEY, query.bool().must().get(0).term().field());
    assertEquals(request.getString(ID), query.bool().must().get(0).term().value().stringValue());
    assertEquals(TYPE_KEYWORD, query.bool().must().get(1).term().field());
    assertEquals(ITEM_TYPE_RESOURCE_GROUP, query.bool().must().get(1).term().value().stringValue());
    vertxTestContext.completeNow();
  }

  @Test
  @Description(
      "testing listRelationshipQuery method when realtionshipType is resourceGrp and itemType is rescource server")
  public void testListRelationshipItemResourceServer(VertxTestContext vertxTestContext) {
    queryDecoder = new QueryDecoder();
    JsonObject request = new JsonObject();
    request
        .put(RELATIONSHIP, RESOURCE_GRP)
        .put(ID, "dummy")
        .put("providerIds", new JsonArray().add(new JsonObject().put("id", "provider-id")))
        .put(ITEM_TYPE, "iudx:ResourceServer");
    Query query = queryDecoder.listRelationshipQuery(request);
    LOGGER.debug("Query: " + query);

    assertEquals(PROVIDER + KEYWORD_KEY, query.bool().should().get(0).match().field());
    assertEquals(
        request.getJsonArray("providerIds").getJsonObject(0).getString("id"),
        query.bool().should().get(0).match().query().stringValue());
    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing seachQuery method with searchType equals RESPONSE_FILTER_REGEX")
  public void testsearchQueryGetItemType(VertxTestContext vertxTestContext) {
    queryDecoder = new QueryDecoder();
    JsonObject request = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    jsonArray.add("dummy");
    request.put(SEARCH_TYPE, "getParentObjectInfo").put(ID, "id");
    Query query = (Query) queryDecoder.searchQuery(request).getValue("query");
    LOGGER.debug("Query: " + query);
    assertEquals(ID_KEYWORD, query.term().field());
    assertEquals(request.getString(ID), query.term().value().stringValue());

    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing listRelationshipQuery method with searchType equals RESPONSE_FILTER_REGEX")
  public void testListRelationshipQueryType(VertxTestContext vertxTestContext) {
    queryDecoder = new QueryDecoder();
    JsonObject request = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    jsonArray.add("dummy");
    request
        .put(RELATIONSHIP, "cos")
        .put(SEARCH_TYPE, "getParentObjectInfo")
        .put(ID, "id")
        .put(COS_ITEM, "value");
    Query query = (Query) queryDecoder.searchQuery(request).getValue("query");
    LOGGER.debug("Query: " + query);
    assertEquals(ID_KEYWORD, query.term().field());
    assertEquals(request.getString(ID), query.term().value().stringValue());
    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing listRelationshipQuery method with item type cos")
  public void testListRelationshipCosQueryType(VertxTestContext vertxTestContext) {
    queryDecoder = new QueryDecoder();
    JsonObject request = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    jsonArray.add("dummy");
    request.put(ID, "id").put(ITEM_TYPE, ITEM_TYPE_COS);
    assertEquals(null, queryDecoder.listRelationshipQuery(request));
    vertxTestContext.completeNow();
  }

  @ParameterizedTest
  @MethodSource("mustQuery")
  @Description("testing listRelationshipQuery method with item type cos and relType resource")
  public void testListRelationshipCosResourceType(
      String input, String actualOutput, VertxTestContext vertxTestContext) {
    queryDecoder = new QueryDecoder();
    JsonObject request = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    jsonArray.add("dummy");
    request
        .put(ID, "id")
        .put(SEARCH_TYPE, "getParentObjectInfo")
        .put(ITEM_TYPE_COS, "value")
        .put(ITEM_TYPE, ITEM_TYPE_COS)
        .put(RELATIONSHIP, input);
    Query query = (Query) queryDecoder.searchQuery(request).getValue("query");
    LOGGER.debug("Query: " + query);

    assertEquals(ID_KEYWORD, query.term().field());
    assertEquals(request.getString(ID), query.term().value().stringValue());
    vertxTestContext.completeNow();
  }

  @ParameterizedTest
  @MethodSource("shouldQuery")
  @Description("testing listRelationshipQuery method with item type cos and relType all")
  public void testListRelationshipCosAllType(String input, VertxTestContext vertxTestContext) {
    queryDecoder = new QueryDecoder();
    JsonObject request = new JsonObject();
    JsonArray jsonArray = new JsonArray();
    jsonArray.add("dummy");
    request
        .put(ID, "id")
        .put(SEARCH_TYPE, "getParentObjectInfo")
        .put(RELATIONSHIP, ALL)
        .put(input, "dummy");
    Query query = (Query) queryDecoder.searchQuery(request).getValue("query");
    LOGGER.debug("Query: " + query);

    assertEquals(ID_KEYWORD, query.term().field());
    assertEquals(request.getString(ID), query.term().value().stringValue());
    vertxTestContext.completeNow();
  }
}
