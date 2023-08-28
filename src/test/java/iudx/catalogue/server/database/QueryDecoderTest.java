package iudx.catalogue.server.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.core.Vertx;
import iudx.catalogue.server.Configuration;
import jdk.jfr.Description;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.util.Constants.*;

@ExtendWith(VertxExtension.class)
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class QueryDecoderTest {

  private static QueryDecoder queryDecoder;

  @BeforeAll
  @DisplayName("Deploying Verticle")
  static void startVertx(Vertx vertx, VertxTestContext testContext) {

    JsonObject elasticConfig = Configuration.getConfiguration("./configs/config-test.json", 0);

    vertx.deployVerticle(new DatabaseVerticle(), new
            DeploymentOptions().setConfig(elasticConfig), testContext.completing());
    queryDecoder = new QueryDecoder();
    testContext.completed();
  }

  @Test
  @Order(1)
  @DisplayName("GeoPoint request to DbQuery")
  public void searchGeoPointTest(VertxTestContext testContext) {

    JsonObject requests = new JsonObject()
            .put(GEOPROPERTY, LOCATION)
            .put(GEORELATION, GEOREL_WITHIN)
            .put(MAX_DISTANCE, 5000)
            .put(GEOMETRY, POINT)
            .put(COORDINATES, new JsonArray().add(73.927285).add(18.502712))
            .put(SEARCH_TYPE, SEARCH_TYPE_GEO)
            .put(SEARCH, true);

    JsonObject json = queryDecoder.searchQuery(requests);

    assertEquals(GEO_CIRCLE,
            json.getJsonObject(QUERY_KEY).getJsonObject("bool").getJsonArray(FILTER).getJsonObject(0)
                    .getJsonObject(GEO_SHAPE_KEY).getJsonObject("location.geometry")
                    .getJsonObject(SHAPE_KEY).getString(TYPE));
    testContext.completeNow();
  }

  @Test
  @Order(2)
  @DisplayName("GeoPolygon request to DbQuery")
  public void searchGeoPolygonTest(VertxTestContext testContext) {

    JsonObject requests = new JsonObject()
            .put(GEOPROPERTY, LOCATION)
            .put(GEORELATION, GEOREL_WITHIN)
            .put(MAX_DISTANCE, 5000)
            .put(GEOMETRY, POLYGON)
            .put(COORDINATES,
                    new JsonArray().add(new JsonArray().add(new JsonArray().add(75.9).add(14.5))
                            .add(new JsonArray().add(72).add(13)).add(new JsonArray().add(73).add(20))
                            .add(new JsonArray().add(75.9).add(14.5))))
            .put(SEARCH_TYPE, SEARCH_TYPE_GEO)
            .put(SEARCH, true);

    JsonObject json = queryDecoder.searchQuery(requests);

    assertEquals(POLYGON,
            json.getJsonObject(QUERY_KEY).getJsonObject("bool").getJsonArray(FILTER).getJsonObject(0)
                    .getJsonObject(GEO_SHAPE_KEY).getJsonObject("location.geometry")
                    .getJsonObject(SHAPE_KEY).getString(TYPE));
    testContext.completeNow();
  }

  @Test
  @Order(3)
  @DisplayName("GeoBbox request to DbQuery")
  public void searchGeoBboxTest(VertxTestContext testContext) {

    JsonObject requests = new JsonObject()
            .put(GEOPROPERTY, LOCATION)
            .put(GEORELATION, GEOREL_WITHIN)
            .put(MAX_DISTANCE, 5000)
            .put(GEOMETRY, BBOX)
            .put(COORDINATES,
                    new JsonArray().add(new JsonArray().add(73).add(20))
                            .add(new JsonArray().add(75).add(14)))
            .put(SEARCH_TYPE, SEARCH_TYPE_GEO)
            .put(SEARCH, true);

    JsonObject json = queryDecoder.searchQuery(requests);

    assertEquals(GEO_BBOX,
            json.getJsonObject(QUERY_KEY).getJsonObject("bool").getJsonArray(FILTER).getJsonObject(0)
                    .getJsonObject(GEO_SHAPE_KEY).getJsonObject("location.geometry")
                    .getJsonObject(SHAPE_KEY).getString(TYPE));
    testContext.completeNow();
  }

  @Test
  @Order(4)
  @DisplayName("GeoLineString request to DbQuery")
  public void searchGeoLineStringTest(VertxTestContext testContext) {

    JsonObject requests = new JsonObject()
            .put(GEOPROPERTY, LOCATION)
            .put(GEORELATION, GEOREL_WITHIN)
            .put(MAX_DISTANCE, 5000)
            .put(GEOMETRY, LINESTRING)
            .put(COORDINATES,
                    new JsonArray().add(new JsonArray().add(73.874537).add(18.528311))
                            .add(new JsonArray().add(73.836808).add(18.572797))
                            .add(new JsonArray().add(73.876484).add(18.525007)))
            .put(SEARCH_TYPE, SEARCH_TYPE_GEO)
            .put(SEARCH, true);

    JsonObject json = queryDecoder.searchQuery(requests);

    assertEquals(LINESTRING,
            json.getJsonObject(QUERY_KEY).getJsonObject("bool").getJsonArray(FILTER).getJsonObject(0)
                    .getJsonObject(GEO_SHAPE_KEY).getJsonObject("location.geometry")
                    .getJsonObject(SHAPE_KEY).getString(TYPE));
    testContext.completeNow();
  }

  @Test
  @Order(5)
  @DisplayName("Invalid QueryParameters")
  public void searchInvalidQueryParametersTest(VertxTestContext testContext) {

    JsonObject requests = new JsonObject()
            .put(GEOPROPERTY, LOCATION)
            .put(GEORELATION, GEOREL_WITHIN)
            .put(MAX_DISTANCE, 5000)
            .put(GEOMETRY, SHAPE_KEY)
            .put(COORDINATES,
                    new JsonArray().add(new JsonArray().add(73.874537).add(18.528311))
                            .add(new JsonArray().add(73.836808).add(18.572797))
                            .add(new JsonArray().add(73.876484).add(18.525007)))
            .put(SEARCH_TYPE, SEARCH_TYPE_GEO)
            .put(SEARCH, true);

    JsonObject json = queryDecoder.searchQuery(requests);

    //  assertEquals(DETAIL_INVALID_GEO_PARAMETER, json.getString(ERROR));
    testContext.completeNow();
  }


  @Test
  @Order(6)
  @DisplayName("Text search request to DbQuery")
  public void searchTextTest(VertxTestContext testContext) {

    JsonObject requests = new JsonObject()
            .put(Q_VALUE, "Golibar Square")
            .put(SEARCH_TYPE, SEARCH_TYPE_TEXT)
            .put(SEARCH, true);

    JsonObject json = queryDecoder.searchQuery(requests);

    assertEquals("Golibar Square",
            json.getJsonObject(QUERY_KEY).getJsonObject("bool").getJsonArray("must").getJsonObject(0)
                    .getJsonObject("query_string").getString(QUERY_KEY));
    testContext.completeNow();
  }

  @Test
  @Order(7)
  @DisplayName("Attribute search request to DbQuery")
  public void searchAttributeTest(VertxTestContext testContext) {

    JsonObject requests = new JsonObject()
            .put(PROPERTY, new JsonArray().add(ID))
            .put(VALUE,
                    new JsonArray().add(
                            new JsonArray().add("iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs."
                                    + "iudx.io/aqm-bosch-climo/Ambedkar society circle_29")))
            .put(SEARCH_TYPE, SEARCH_TYPE_ATTRIBUTE)
            .put(SEARCH, true);

    JsonObject json = queryDecoder.searchQuery(requests);

    assertEquals(
            "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/rs."
                    + "iudx.io/aqm-bosch-climo/Ambedkar society circle_29",
            json.getJsonObject(QUERY_KEY).getJsonObject("bool").getJsonArray("must").getJsonObject(0)
                    .getJsonObject("bool").getJsonArray("should").getJsonObject(0).getJsonObject(MATCH_KEY)
                    .getString(ID_KEYWORD));
    testContext.completeNow();
  }

  @Test
  @Order(8)
  @DisplayName("Tag search request to DbQuery")
  public void searchTagTest(VertxTestContext testContext) {

    JsonObject requests = new JsonObject().put(PROPERTY, new JsonArray().add(TAGS))
            .put(VALUE, new JsonArray().add(new JsonArray().add("pollution")))
            .put(SEARCH_TYPE, SEARCH_TYPE_ATTRIBUTE).put(SEARCH, true);

    JsonObject json = queryDecoder.searchQuery(requests);

    assertEquals("pollution",
            json.getJsonObject(QUERY_KEY).getJsonObject("bool").getJsonArray("must").getJsonObject(0)
                    .getJsonObject("bool").getJsonArray("should").getJsonObject(0).getJsonObject(MATCH_KEY)
                    .getString(TAGS));
    testContext.completeNow();
  }


  @Test
  @Order(9)
  @DisplayName("Relationship search request to DbQuery")
  public void searchRelationshipTest(VertxTestContext testContext) {

    JsonObject requests = new JsonObject().put(ID,
                            "2596264a-ff2a-40f7-90cc-17a57b2adffe")
        .put(ITEM_TYPE, "iudx:Resource")
        .put(RESOURCE_GRP, "rg-id")
            .put(RELATIONSHIP, RESOURCE_GRP);

    JsonObject json = new JsonObject(queryDecoder.listRelationshipQuery(requests));

    assertEquals(ITEM_TYPE_RESOURCE_GROUP,
            json.getJsonObject(QUERY_KEY).getJsonObject("bool").getJsonArray("must").getJsonObject(1)
                    .getJsonObject(TERM).getString("type.keyword"));
    testContext.completeNow();
  }


  @Test
  @Order(10)
  @DisplayName("Relationship search request to DbQuery")
  public void searchRelationshipTest2(VertxTestContext testContext) {

    JsonObject requests = new JsonObject()
            .put(ID, "2596264a-ff2a-40f7-90cc-17a57b2adffe")
        .put(ITEM_TYPE, "iudx:Resource")
        .put(RESOURCE_SVR, "rs-id")
            .put(RELATIONSHIP, RESOURCE_SVR);

    JsonObject json = new JsonObject(queryDecoder.listRelationshipQuery(requests));

    System.out.println(json);
    assertEquals(ITEM_TYPE_RESOURCE_SERVER, json.getJsonObject(QUERY_KEY).getJsonObject("bool")
            .getJsonArray("must").getJsonObject(1).getJsonObject(TERM).getString("type.keyword"));
    testContext.completeNow();
  }


  @Test
  @Order(11)
  @DisplayName("Relationship search request to DbQuery")
  public void listItemTagTest(VertxTestContext testContext) {

    JsonObject requests = new JsonObject()
            .put(ITEM_TYPE, TAGS)
            .put(TYPE, TAGS);

    JsonObject json = new JsonObject(queryDecoder.listItemQuery(requests));

    assertEquals(TAGS + KEYWORD_KEY, json.getJsonObject(AGGREGATION_KEY).getJsonObject(RESULTS)
            .getJsonObject(TERMS_KEY).getString("field"));
    testContext.completeNow();
  }
  @Test
  @Description("test listItemQuery method when itemType equals TAGS")
  public void testListItemQueryTag(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    request.put(ITEM_TYPE,TAGS);
    request.put(INSTANCE,"dummy").put(LIMIT,2).put(OFFSET,1);
    String elasticQuery=LIST_INSTANCE_TAGS_QUERY.replace("$1", request.getString(INSTANCE)).replace("$size",request.getInteger(LIMIT,FILTER_PAGINATION_SIZE-request.getInteger(OFFSET,0)).toString());
    assertEquals(elasticQuery,queryDecoder.listItemQuery(request));
    vertxTestContext.completeNow();
  }
  @Test
  @Description("test listItemQuery method when itemType not equals TAGS")
  public void testListItemQuery(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    request.put(ITEM_TYPE,"dummy")
            .put(TYPE_KEY,"dummy").put(LIMIT,2).put(OFFSET,1);
    request.put(INSTANCE,null);
    String elasticQuery=LIST_TYPES_QUERY.replace("$1",request.getString(TYPE_KEY)).replace("$size",request.getInteger(LIMIT,FILTER_PAGINATION_SIZE-request.getInteger(OFFSET,0)).toString());
    assertEquals(elasticQuery,queryDecoder.listItemQuery(request));
    vertxTestContext.completeNow();
  }
  @Test
  @Description("test listItemQuery method when itemType not equals TAGS and instanceID is not null/empty")
  public void testListItemQueryInstance(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    request.put(ITEM_TYPE,"dummy")
            .put(TYPE_KEY,"dummy")
            .put(LIMIT,2).put(OFFSET,1);
    request.put(INSTANCE,"dummy");
    String elasticQuery=LIST_INSTANCE_TYPES_QUERY.replace("$1", request.getString(TYPE_KEY)).replace("$2", request.getString(INSTANCE)).replace("$size",request.getInteger(LIMIT,FILTER_PAGINATION_SIZE-request.getInteger(OFFSET,0)).toString());
    assertEquals(elasticQuery,queryDecoder.listItemQuery(request));
    vertxTestContext.completeNow();
  }
  @Test
  @Description("test SearchQuery method when searchType equals GEOSEARCH_REGEX")
  public void testSearchQueryGeosearch(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    request.put(SEARCH_TYPE,GEOSEARCH_REGEX)
            .put(ITEM_TYPE,"dummy")
            .put(SEARCH,false);

    assertEquals(new JsonObject().put(ERROR,new RespBuilder().withType(TYPE_INVALID_GEO_PARAM).withTitle(TITLE_INVALID_GEO_PARAM).getJsonResponse()),queryDecoder.searchQuery(request));
    vertxTestContext.completeNow();
  }
  @Test
  @Description("test listItemQuery method when itemType not equals TAGS and instanceID is not null/empty")
  public void testSearchQueryTextSearch(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    request.put(SEARCH_TYPE,TEXTSEARCH_REGEX)
            .put(SEARCH,false);
    assertEquals(new JsonObject().put(ERROR,new RespBuilder().withType(TYPE_BAD_TEXT_QUERY).withTitle(TITLE_BAD_TEXT_QUERY).getJsonResponse()),queryDecoder.searchQuery(request));
    vertxTestContext.completeNow();
  }
  @Test
  @Description("test listItemQuery method when itemType not equals TAGS and instanceID is not null/empty")
  public void testSearchQuery(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    JsonArray jsonArray=new JsonArray();
    jsonArray.add("dummy value");
    jsonArray.add("dummy value");
    JsonArray jsonArray2=new JsonArray();
    jsonArray2.add("dummy");
    request.put(SEARCH_TYPE,ATTRIBUTE_SEARCH_REGEX)
            .put(SEARCH,false)
            .put(PROPERTY,jsonArray)
            .put(VALUE,jsonArray2);
    assertEquals(new JsonObject().put(ERROR,new RespBuilder().withType(TYPE_INVALID_PROPERTY_VALUE).withTitle(TITLE_INVALID_PROPERTY_VALUE).getJsonResponse()),queryDecoder.searchQuery(request));
    vertxTestContext.completeNow();
  }
  @Test
  @Description("test listItemQuery method when itemType not equals TAGS and instanceID is not null/empty")
  public void testSearchQueryAttributeSearch(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    JsonArray jsonArray=new JsonArray();
    jsonArray.add(0,KEYWORD_KEY);
    JsonArray jsonArray2=new JsonArray();
    JsonArray jsonArray3=new JsonArray();
    jsonArray3.add(0,"dummy");
    jsonArray2.add(0,jsonArray3);
    request.put(SEARCH_TYPE,ATTRIBUTE_SEARCH_REGEX)
            .put(SEARCH,false)
            .put(PROPERTY,jsonArray)
            .put(VALUE,jsonArray2);
   /* String matchQuery=MATCH_QUERY.replace("$1",request.getJsonArray(PROPERTY).getString(0)).replace("$2",request.getJsonArray(VALUE).getString(0));
     JsonArray shouldQuery=new JsonArray();
     shouldQuery.add(new JsonObject(matchQuery));
     JsonArray mustQuery=new JsonArray();
    mustQuery.add(new JsonObject(SHOULD_QUERY.replace("$1", shouldQuery.toString())));
    JsonObject elasticQuery=new JsonObject();
    JsonObject boolQuery = new JsonObject(MUST_QUERY.replace("$1", mustQuery.toString()));
    */
    queryDecoder.searchQuery(request);
    vertxTestContext.completeNow();
  }
  @Test
  @Description("test searchquery when instanceId is not null")
  public void testSearchInstance(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    request.put(INSTANCE,"dummy")
            .put(SEARCH,false)
            .put(SEARCH_TYPE,"dummy")
            .put(OFFSET,100);
    assertEquals(new JsonObject().put(ERROR, new RespBuilder()
            .withType(TYPE_INVALID_SYNTAX)
            .withTitle(TITLE_INVALID_SYNTAX)
            .getJsonResponse()),queryDecoder.searchQuery(request));
    vertxTestContext.completeNow();
  }
  @Test
  @Description("testing listQueryRelationship test")
  public void testListRelationshipQuery(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    JsonArray jsonArray=new JsonArray();
    request.put(RELATIONSHIP,TYPE_KEY);
    request.put(ID,"dummy")
//        .put(ITEM_TYPE, "item:Resource")
            .put(LIMIT,100)
            .put(OFFSET,100)
            .put(FILTER,jsonArray)
            .put(LIMIT,2).put(OFFSET,1);

    queryDecoder.listRelationshipQuery(request);
    vertxTestContext.completeNow();
  }
  @Test
  @Description("testing seachquery method with limit set to 100")
  public void testsearchQuery(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    request.put(LIMIT,100).put(SEARCH_TYPE,"dummy").put(SEARCH,false);
    request.put(INSTANCE,"dummy");
    JsonObject elasticQuery=new JsonObject();
    elasticQuery.put(SIZE_KEY, request.getInteger(LIMIT));
    assertEquals(new JsonObject().put(ERROR, new RespBuilder()
            .withType(TYPE_INVALID_SYNTAX)
            .withTitle(TITLE_INVALID_SYNTAX)
            .getJsonResponse()),queryDecoder.searchQuery(request));
    vertxTestContext.completeNow();
  }
  @Test
  @Description("testing seachQuery method with searchType equals RESPONSE_FILTER_REGEX")
  public void testsearchQueryFilter_Regex(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    JsonArray jsonArray=new JsonArray();
    jsonArray.add("dummy");
    request.put(SEARCH_TYPE,RESPONSE_FILTER_REGEX)
            .put(SEARCH,true).put(OFFSET,100)
            .put(ATTRIBUTE,jsonArray)
            .put(INSTANCE,"dummy");
    Integer limit =
            request.getInteger(LIMIT, FILTER_PAGINATION_SIZE - request.getInteger(OFFSET, 0));
    JsonObject elasticQuery=new JsonObject();
    JsonArray mustQuery=new JsonArray();
    mustQuery.add(new JsonObject(INSTANCE_FILTER.replace("$1", request.getString(INSTANCE))));
    JsonArray sourceFilter=request.getJsonArray(ATTRIBUTE);
    JsonObject boolQuery = new JsonObject(MUST_QUERY.replace("$1", mustQuery.toString()));
    elasticQuery.put(SIZE_KEY, limit).put(FROM,request.getInteger(OFFSET)).put(SOURCE,sourceFilter);
    assertEquals(elasticQuery.put(QUERY_KEY, boolQuery),queryDecoder.searchQuery(request));
    vertxTestContext.completeNow();
  }
  @Test
  @Description("testing seachQuery method with searchType equals RESPONSE_FILTER_REGEX")
  public void testsearchQueryFilter_Regex2(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    JsonArray jsonArray=new JsonArray();
    jsonArray.add("dummy");
    request.put(SEARCH_TYPE,RESPONSE_FILTER_REGEX)
            .put(SEARCH,true)
            .put(OFFSET,100).
            put(FILTER,jsonArray)
            .put(INSTANCE,"dummy");
    Integer limit =
            request.getInteger(LIMIT, FILTER_PAGINATION_SIZE - request.getInteger(OFFSET, 0));
    JsonObject elasticQuery=new JsonObject();
    JsonArray mustQuery=new JsonArray();
    mustQuery.add(new JsonObject(INSTANCE_FILTER.replace("$1", request.getString(INSTANCE))));
    JsonArray sourceFilter=request.getJsonArray(FILTER);
    JsonObject boolQuery = new JsonObject(MUST_QUERY.replace("$1", mustQuery.toString()));
    elasticQuery.put(SIZE_KEY, limit).put(FROM,request.getInteger(OFFSET)).put(SOURCE,sourceFilter);
    assertEquals(elasticQuery.put(QUERY_KEY, boolQuery),queryDecoder.searchQuery(request));

    vertxTestContext.completeNow();
  }
  @Test
  @Description("testing seachQuery method with searchType equals RESPONSE_FILTER_REGEX")
  public void testListRelationshipQueryID(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    request.put(RELATIONSHIP,RESOURCE)
            .put(ID,"dummy")
            .put(ITEM_TYPE,"iudx:ResourceGroup");
    String subQuery = TERM_QUERY.replace("$1", RESOURCE_GRP + KEYWORD_KEY)
            .replace("$2", request.getString(ID))
            + "," +
            TERM_QUERY.replace("$1", TYPE_KEYWORD)
                    .replace("$2", ITEM_TYPE_RESOURCE);
    String elasticQuery = BOOL_MUST_QUERY.replace("$1", subQuery);
    Integer limit =
            request.getInteger(LIMIT, FILTER_PAGINATION_SIZE - request.getInteger(OFFSET, 0));
    JsonObject tempQuery = new JsonObject(elasticQuery).put(SIZE_KEY, limit.toString());


    assertEquals(tempQuery.toString(),queryDecoder.listRelationshipQuery(request));
    vertxTestContext.completeNow();
  }
  @Test
  @Description("testing seachQuery method with searchType equals RESPONSE_FILTER_REGEX")
  public void testListRelationshipQueryProvider(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    request.put(RELATIONSHIP,PROVIDER)
            .put(ID,"abcd/abcd")
            .put(PROVIDER,"abcd/abc");
    String id=request.getString(ID);
    String providerId = StringUtils.substring(id, 0, id.indexOf("/", id.indexOf("/") + 1));
    String subQuery = TERM_QUERY.replace("$1", ID_KEYWORD)
            .replace("$2", providerId)
            + "," +
            TERM_QUERY.replace("$1", TYPE_KEYWORD)
                    .replace("$2", ITEM_TYPE_PROVIDER);
    String elasticQuery = BOOL_MUST_QUERY.replace("$1", subQuery);
    Integer limit =
            request.getInteger(LIMIT, FILTER_PAGINATION_SIZE - request.getInteger(OFFSET, 0));
    JsonObject tempQuery = new JsonObject(elasticQuery).put(SIZE_KEY, limit.toString());


    assertEquals(tempQuery.toString(),queryDecoder.listRelationshipQuery(request));
    vertxTestContext.completeNow();
  }
  @Test
  @Description("testing listRelationshipQuery method when realtionshipType is resource and itemType is provider")
  public void testListRelationshipResource(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    request.put(RELATIONSHIP,RESOURCE)
            .put(ID,"dummy")
            .put(ITEM_TYPE,"iudx:Provider");
    String subQuery = TERM_QUERY.replace("$1", PROVIDER + KEYWORD_KEY)
            .replace("$2", request.getString(ID))
            + "," +
            TERM_QUERY.replace("$1", TYPE_KEYWORD)
                    .replace("$2", ITEM_TYPE_RESOURCE);
    String elasticQuery = BOOL_MUST_QUERY.replace("$1", subQuery);
    Integer limit =
            request.getInteger(LIMIT, FILTER_PAGINATION_SIZE - request.getInteger(OFFSET, 0));
    JsonObject tempQuery = new JsonObject(elasticQuery).put(SIZE_KEY, limit.toString());


    assertEquals(tempQuery.toString(),queryDecoder.listRelationshipQuery(request));
    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing listRelationshipQuery method when realtionshipType is resource group and itemType is resource")
  public void testListRelationshipResourceGroup(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    request.put(RELATIONSHIP,RESOURCE_GRP)
            .put(ID,"dummy")
            .put(ITEM_TYPE,"iudx:Resource")
            .put("resourceGroup","dummy id");
    String  subQuery = TERM_QUERY.replace("$1", ID_KEYWORD)
            .replace("$2", request.getString("resourceGroup"))
            + ","
            + TERM_QUERY.replace("$1", TYPE_KEYWORD)
            .replace("$2", ITEM_TYPE_RESOURCE_GROUP);
    String elasticQuery = BOOL_MUST_QUERY.replace("$1", subQuery);
    Integer limit =
            request.getInteger(LIMIT, FILTER_PAGINATION_SIZE - request.getInteger(OFFSET, 0));
    JsonObject tempQuery = new JsonObject(elasticQuery).put(SIZE_KEY, limit.toString());


    assertEquals(tempQuery.toString(),queryDecoder.listRelationshipQuery(request));
    vertxTestContext.completeNow();
  }
  @Test
  @Description("testing listRelationshipQuery method when realtionshipType is resource group and itemType is provider")
  public void testListRelationshipItemProvider(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    request.put(RELATIONSHIP,RESOURCE_GRP)
            .put(ID,"dummy")
            .put(ITEM_TYPE,"iudx:Provider")
            .put("resourceGroup","dummy id");
    String   subQuery = TERM_QUERY.replace("$1", PROVIDER + KEYWORD_KEY)
            .replace("$2", request.getString(ID))
            + ","
            + TERM_QUERY.replace("$1", TYPE_KEYWORD)
            .replace("$2", ITEM_TYPE_RESOURCE_GROUP);
    String elasticQuery = BOOL_MUST_QUERY.replace("$1", subQuery);
    Integer limit =
            request.getInteger(LIMIT, FILTER_PAGINATION_SIZE - request.getInteger(OFFSET, 0));
    JsonObject tempQuery = new JsonObject(elasticQuery).put(SIZE_KEY, limit.toString());


    assertEquals(tempQuery.toString(),queryDecoder.listRelationshipQuery(request));
    vertxTestContext.completeNow();
  }
  @Test
  @Description("testing listRelationshipQuery method when realtionshipType is resourceGrp and itemType is rescource server")
  public void testListRelationshipItemResourceServer(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    request
        .put(RELATIONSHIP, RESOURCE_GRP)
        .put(ID, "dummy")
        .put("providerIds", new JsonArray().add(new JsonObject().put("id","provider-id")))
        .put(ITEM_TYPE, "iudx:ResourceServer");
    String subQuery = GET_RS1 + GET_RS2.replace("$1", "provider-id").replace(",", "");
    String elasticQuery = subQuery + GET_RS3;

    assertEquals(elasticQuery.toString(),queryDecoder.listRelationshipQuery(request));
    vertxTestContext.completeNow();
  }
  @Test
  @Description("testing seachQuery method with searchType equals RESPONSE_FILTER_REGEX")
  public void testsearchQueryGetItemType(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    JsonArray jsonArray=new JsonArray();
    jsonArray.add("dummy");
    request.put(SEARCH_TYPE,"getItemType")
            .put(ID,"id");
    JsonObject elasticQuery =
        new JsonObject(
            GET_DOC_QUERY
                .replace("$1", request.getString(ID))
                .replace("$2", "\"type\",\"provider\",\"providerKcId\",\"resourceGroup\",\"resourceServer\", \"resourceServerURL\", \"owner\", \"cos_admin\""));
    assertEquals(elasticQuery,queryDecoder.searchQuery(request));

    vertxTestContext.completeNow();
  }
  @Test
  @Description("testing seachQuery method with searchType equals RESPONSE_FILTER_REGEX")
  public void testsearchQueryGetUrl(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    JsonArray jsonArray=new JsonArray();
    jsonArray.add("dummy");
    request.put(SEARCH_TYPE,"getRsUrl")
            .put(ID,"id")
            .put(ITEM_TYPE,"iudx:Provider");
    JsonObject elasticQuery =
        new JsonObject(
            GET_DOC_QUERY
                .replace("$1", request.getString(ID))
                .replace("$2", "\"resourceServers.resourceServerURL\", \"owner\", \"cos_admin\""));
    assertEquals(elasticQuery,queryDecoder.searchQuery(request));

    vertxTestContext.completeNow();
  }

  @Test
  @Description("testing seachQuery method with searchType equals RESPONSE_FILTER_REGEX")
  public void testsearchQueryResourceGrp(VertxTestContext vertxTestContext) {
    queryDecoder=new QueryDecoder();
    JsonObject request=new JsonObject();
    JsonArray jsonArray=new JsonArray();
    jsonArray.add("dummy");
    request.put(SEARCH_TYPE,"getRsUrl")
            .put(ID,"id")
            .put(ITEM_TYPE, "iudx:ResourceGroup");
    JsonObject elasticQuery =
        new JsonObject(
            GET_DOC_QUERY.replace("$1", request.getString(ID)).replace("$2", "\"resourceServers.resourceServerURL\", \"owner\", \"cos_admin\""));
    assertEquals(elasticQuery,queryDecoder.searchQuery(request));

    vertxTestContext.completeNow();
  }



}
