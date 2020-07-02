package iudx.catalogue.server.database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.io.IOException;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;

/**
 * The Database Service Implementation.
 *
 * <h1>Database Service Implementation</h1>
 *
 * <p> The Database Service implementation in the IUDX Catalogue Server implements
 * the definitions of the
 * {@link iudx.catalogue.server.database.DatabaseService}.
 * </p>
 *
 * @version 1.0
 * @since 2020-05-31
 */
public class DatabaseServiceImpl implements DatabaseService {

  private static final Logger logger = LoggerFactory.getLogger(DatabaseServiceImpl.class);
  private final RestClient client;
  private JsonObject query;


  public DatabaseServiceImpl(RestClient client) {
    this.client = client;
  }

  @Override
  public DatabaseService searchQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    Request elasticRequest;
    JsonObject errorJson = new JsonObject();
    // TODO: Stub code, to be removed

    //    if (!request.containsKey("instanceId")) {
    //      errorJson.put("status", "failed").put("desc", "No instanceId found");
    //      handler.handle(Future.failedFuture(errorJson.toString()));
    //      return null;
    //    }
    if (!request.containsKey("searchType")) {
      errorJson.put("status", "failed").put("desc", "No searchType found");
      handler.handle(Future.failedFuture(errorJson.toString()));
      return null;
    }
    elasticRequest = new Request("GET", CAT_TEST_SEARCH_INDEX + FILTER_PATH);
    query = queryDecoder(request);
    if (query.containsKey("Error")) {
      logger.info("Query returned with an error");
      errorJson.put("status", "failed").put("desc", query.getString("Error"));
      handler.handle(Future.failedFuture(errorJson.toString()));
      return null;
    }
    logger.info("Query constructed: " + query.toString());
    elasticRequest.setJsonEntity(query.toString());
    client.performRequestAsync(elasticRequest, new ResponseListener() {
      @Override
      public void onSuccess(Response response) {
        logger.info("Successful DB request");
        JsonArray dbResponse = new JsonArray();
        JsonObject dbResponseJson = new JsonObject();
        try {
          int statusCode = response.getStatusLine().getStatusCode();
          if (statusCode != 200 && statusCode != 204) {
            handler.handle(Future.failedFuture("Status code is not 2xx"));
            return;
          }
          JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));
          if (responseJson.getJsonObject("hits").getJsonObject("total")
              .getInteger("value") == 0) {
            errorJson.put("status", "failed").put("desc", "Empty response");
            handler.handle(Future.failedFuture(errorJson.toString()));
            return;
          }
          JsonArray responseHits = responseJson.getJsonObject("hits").getJsonArray("hits");
          for (Object json : responseHits) {
            JsonObject jsonTemp = (JsonObject) json;
            dbResponse.add(jsonTemp.getJsonObject("_source"));
          }
          dbResponseJson.put("status", "success").put("totalHits",
              responseJson.getJsonObject("hits").getJsonObject(
              "total").getInteger("value")).put("results", dbResponse);
          handler.handle(Future.succeededFuture(dbResponseJson));
        } catch (IOException e) {
          logger.info("DB ERROR:\n");
          e.printStackTrace();
          errorJson.put("status", "failed").put("desc", "DB Error. Check logs for more "
              + "information");
          handler.handle(Future.failedFuture(errorJson.toString()));
        }
      }

      @Override
      public void onFailure(Exception e) {
        logger.info("DB request has failed. ERROR:\n");
        e.printStackTrace();
        errorJson.put("status", "failed").put("desc", "DB request has failed. Check logs for more"
            + " information");
        handler.handle(Future.failedFuture(errorJson.toString()));
      }
    });
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService countQuery(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService createItem(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use
    String result = 
        "{ \"status\": \"success\","
          + "\"results\": [ "
          + "{ \"id\": \"123123\","
          + "\"method\": \"insert\", \"status\": \"success\" } ] }";

    handler.handle(Future.succeededFuture(new JsonObject(result)));
    return null;

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService updateItem(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use
    String result = 
        "{ \"status\": \"success\","
          + "\"results\": [ "
          + "{ \"id\": \"123123\","
          + "\"method\": \"update\", \"status\": \"success\" } ] }";

    handler.handle(Future.succeededFuture(new JsonObject(result)));
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService deleteItem(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use
    String result = 
        "{ \"status\": \"success\","
          + "\"results\": [ "
          + "{ \"id\": \"123123\","
          + "\"method\": \"delete\", \"status\": \"success\" } ] }";

    handler.handle(Future.succeededFuture(new JsonObject(result)));
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listItem(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use

    String result = 
        "{ \"status\": \"success\", \"totalHits\": 100,"
          + "\"limit\": 10, \"offset\": 100,"
          + "\"results\": ["
          + "{ \"id\": \"abc/123\", \"tags\": [ \"a\", \"b\"] } ] }";

    String errResult = 
        " { \"status\": \"invalidValue\", \"results\": [] }";

    if (request.getString("id").contains("/")) {
      handler.handle(Future.succeededFuture(new JsonObject(result)));
    } else {
      handler.handle(Future.succeededFuture(new JsonObject(errResult)));
    }

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listTags(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use
    String result = "{ \"status\": \"success\", \"results\": [ \"environment\", \"civic\" ] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listDomains(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use
    String result = "{ \"status\": \"success\", \"results\": [ \"environment\", \"civic\" ] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listCities(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use
    String result = "{ \"status\": \"success\", \"results\": [ \"Pune\", \"Varanasi\" ] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listResourceServers(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use
    String result = "{ \"status\": \"success\", \"results\": [ \"server-1\", \"server-2\" ] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listProviders(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use [was not part of master code]
    String result = "{ \"status\": \"success\", \"results\": [ \"pr-1\", \"pr-2\" ] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listResourceGroups(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use [was not part of master code]
    String result = "{ \"status\": \"success\", \"results\": [ \"rg-1\", \"rg-2\" ] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listResourceRelationship(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use [was not part of master code]
    String result = "{ \"status\": \"success\", \"results\": [ \"rg-1\", \"rg-2\" ] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listResourceGroupRelationship(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use [was not part of master code]
    String result = "{ \"status\": \"success\", \"results\": [ { \"id\": \"abc/123\" }] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listProviderRelationship(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listResourceServerRelationship(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listTypes(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * Decodes and constructs ElasticSearch Search/Count query based on the parameters passed in the
   * request.
   *
   * @param request Json object containing various fields related to query-type.
   * @return JsonObject which contains fully formed ElasticSearch query.
   */

  public JsonObject queryDecoder(JsonObject request) {
    String searchType = request.getString("searchType");
    JsonObject elasticQuery = new JsonObject();
    elasticQuery.put("size", 10);
    //    String instanceId = request.getString("instanceId");
    JsonArray filterQuery = new JsonArray();
    //    JsonObject termQuery =
    //        new JsonObject().put("term", new JsonObject()
    //        .put(INSTANCE_ID_KEY + ".keyword", instanceId));
    //    filterQuery.add(termQuery);

    if (searchType.matches("(.*)geoSearch(.*)")) {
      logger.info("In geoSearch block---------");
      JsonObject shapeJson = new JsonObject();
      JsonObject geoSearch = new JsonObject();
      String relation;
      JsonArray coordinates;
      if (request.containsKey("geometry")
          && request.getString("geometry").equalsIgnoreCase("point")
          && request.containsKey("georel") && request.containsKey("coordinates")
          && request.containsKey("geoproperty") && request.containsKey("maxDistance")) {
        coordinates = request.getJsonArray("coordinates");
        int radius = request.getInteger("maxDistance");
        relation = request.getString("georel");
        shapeJson.put(SHAPE_KEY, new JsonObject().put(TYPE_KEY, GEO_CIRCLE)
            .put(COORDINATES_KEY, coordinates).put(GEO_RADIUS, radius + "m"))
            .put(GEO_RELATION_KEY, relation);
      } else if (request.containsKey("geometry")
          && (request.getString("geometry").equalsIgnoreCase("polygon")
          || request.getString("geometry").equalsIgnoreCase("linestring"))
          && request.containsKey("georel") && request.containsKey("coordinates")
          && request.containsKey("geoproperty")) {
        String geometry = request.getString("geometry");
        relation = request.getString("georel");
        coordinates = request.getJsonArray("coordinates");
        int length = coordinates.getJsonArray(0).size();
        if (geometry.equalsIgnoreCase("polygon") && !coordinates.getJsonArray(0)
            .getJsonArray(0).getDouble(0).equals(coordinates.getJsonArray(0)
                .getJsonArray(length - 1).getDouble(0)) && !coordinates.getJsonArray(0)
            .getJsonArray(0).getDouble(1).equals(coordinates.getJsonArray(0)
                .getJsonArray(length - 1).getDouble(1))) {
          return new JsonObject().put("Error", "Coordinate mismatch (Polygon)");
        }
        shapeJson.put(SHAPE_KEY, new JsonObject().put(TYPE_KEY, geometry)
            .put(COORDINATES_KEY, coordinates)).put(GEO_RELATION_KEY, relation);
      } else if (request.containsKey("geometry")
          && request.getString("geometry").equalsIgnoreCase("bbox") && request.containsKey("georel")
          && request.containsKey("coordinates") && request.containsKey("geoproperty")) {
        relation = request.getString("georel");
        coordinates = request.getJsonArray("coordinates");
        shapeJson = new JsonObject();
        shapeJson
            .put(SHAPE_KEY,
                new JsonObject().put(TYPE_KEY, GEO_BBOX).put(COORDINATES_KEY, coordinates))
            .put(GEO_RELATION_KEY, relation);

      } else {
        return new JsonObject().put("Error", "Missing/Invalid geo parameters");
      }
      geoSearch.put(GEO_SHAPE_KEY, new JsonObject().put(GEO_KEY, shapeJson));
      filterQuery.add(geoSearch);
    }
    if (searchType.matches("(.*)responseFilter(.*)")) {
      logger.info("In responseFilter block---------");
      if (request.containsKey("attrs")) {
        JsonArray sourceFilter = request.getJsonArray("attrs");
        elasticQuery.put(SOURCE_FILTER_KEY, sourceFilter);
      } else {
        return new JsonObject().put("Error", "Missing/Invalid responseFilter parameters");
      }
    }

    elasticQuery.put(QUERY_KEY,
        new JsonObject().put(BOOL_KEY, new JsonObject().put(FILTER_KEY, filterQuery)));

    return elasticQuery;

  }
}
