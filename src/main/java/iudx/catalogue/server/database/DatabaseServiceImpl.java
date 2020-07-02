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
 * <p>
 * The Database Service implementation in the IUDX Catalogue Server implements the definitions of
 * the {@link iudx.catalogue.server.database.DatabaseService}.
 * </p>
 *
 * @version 1.0
 * @since 2020-05-31
 */
public class DatabaseServiceImpl implements DatabaseService {

  private static final Logger logger = LoggerFactory.getLogger(DatabaseServiceImpl.class);
  private final RestClient client;

  public DatabaseServiceImpl(RestClient client) {
    this.client = client;
  }

  @Override
  public DatabaseService searchQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    /* Initialize elastic clients and JsonObjects */
    Request elasticRequest;
    JsonObject query;
    JsonObject errorJson = new JsonObject();
    // TODO: Stub code, to be removed

    // if (!request.containsKey("instanceId")) {
    // errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.DESCRIPTION, "No instanceId
    // found");
    // handler.handle(Future.failedFuture(errorJson.toString()));
    // return null;
    // }

    /* Validate the Request */
    if (!request.containsKey(Constants.SEARCH_TYPE)) {
      errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.DESCRIPTION,
          Constants.NO_SEARCH_TYPE_FOUND);
      handler.handle(Future.failedFuture(errorJson.toString()));
      return null;
    }
    /* Construct an elastic client request with index to query */
    elasticRequest =
        new Request(Constants.REQUEST_GET, Constants.CAT_TEST_SEARCH_INDEX + Constants.FILTER_PATH);
    /* Construct the query to be made */
    query = queryDecoder(request);
    if (query.containsKey(Constants.ERROR)) {
      logger.info("Query returned with an error");
      errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.DESCRIPTION,
          query.getString(Constants.ERROR));
      handler.handle(Future.failedFuture(errorJson.toString()));
      return null;
    }
    logger.info("Query constructed: " + query.toString());
    /* Set the elastic client with the query to perform */
    elasticRequest.setJsonEntity(query.toString());
    /* Execute the query */
    client.performRequestAsync(elasticRequest, new ResponseListener() {
      @Override
      public void onSuccess(Response response) {
        logger.info("Successful DB request");
        JsonArray dbResponse = new JsonArray();
        JsonObject dbResponseJson = new JsonObject();
        try {
          int statusCode = response.getStatusLine().getStatusCode();
          /* Validate the response */
          if (statusCode != 200 && statusCode != 204) {
            handler.handle(Future.failedFuture("Status code is not 2xx"));
            return;
          }
          JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));
          if (responseJson.getJsonObject(Constants.HITS).getJsonObject(Constants.TOTAL)
              .getInteger(Constants.VALUE) == 0) {
            errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.DESCRIPTION,
                Constants.EMPTY_RESPONSE);
            handler.handle(Future.failedFuture(errorJson.toString()));
            return;
          }
          JsonArray responseHits =
              responseJson.getJsonObject(Constants.HITS).getJsonArray(Constants.HITS);
          /* Construct the client response, remove the _source field */
          for (Object json : responseHits) {
            JsonObject jsonTemp = (JsonObject) json;
            dbResponse.add(jsonTemp.getJsonObject(Constants.SOURCE));
          }
          dbResponseJson.put(Constants.STATUS, Constants.SUCCESS)
              .put(Constants.TOTAL_HITS, responseJson.getJsonObject(Constants.HITS)
                  .getJsonObject(Constants.TOTAL).getInteger(Constants.VALUE))
              .put(Constants.RESULT, dbResponse);
          /* Send the response */
          handler.handle(Future.succeededFuture(dbResponseJson));
        } catch (IOException e) {
          logger.info("DB ERROR:\n");
          e.printStackTrace();
          /* Handle request error */
          errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.DESCRIPTION,
              Constants.DATABASE_ERROR);
          handler.handle(Future.failedFuture(errorJson.toString()));
        }
      }

      @Override
      public void onFailure(Exception e) {
        logger.info("DB request has failed. ERROR:\n");
        e.printStackTrace();
        /* Handle request error */
        errorJson.put(Constants.STATUS, Constants.FAILED).put(Constants.DESCRIPTION,
            Constants.DATABASE_ERROR);
        handler.handle(Future.failedFuture(errorJson.toString()));
      }
    });
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService countQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService createItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use
    String result = "{ \"status\": \"success\"," + "\"results\": [ " + "{ \"id\": \"123123\","
        + "\"method\": \"insert\", \"status\": \"success\" } ] }";

    handler.handle(Future.succeededFuture(new JsonObject(result)));
    return null;

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService updateItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use
    String result = "{ \"status\": \"success\"," + "\"results\": [ " + "{ \"id\": \"123123\","
        + "\"method\": \"update\", \"status\": \"success\" } ] }";

    handler.handle(Future.succeededFuture(new JsonObject(result)));
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService deleteItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use
    String result = "{ \"status\": \"success\"," + "\"results\": [ " + "{ \"id\": \"123123\","
        + "\"method\": \"delete\", \"status\": \"success\" } ] }";

    handler.handle(Future.succeededFuture(new JsonObject(result)));
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use

    String result =
        "{ \"status\": \"success\", \"totalHits\": 100," + "\"limit\": 10, \"offset\": 100,"
            + "\"results\": [" + "{ \"id\": \"abc/123\", \"tags\": [ \"a\", \"b\"] } ] }";

    String errResult = " { \"status\": \"invalidValue\", \"results\": [] }";

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
  public DatabaseService listTags(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use
    String result = "{ \"status\": \"success\", \"results\": [ \"environment\", \"civic\" ] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listDomains(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use
    String result = "{ \"status\": \"success\", \"results\": [ \"environment\", \"civic\" ] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listCities(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
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
  public DatabaseService listTypes(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

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
    String searchType = request.getString(Constants.SEARCH_TYPE);
    JsonObject elasticQuery = new JsonObject();
    elasticQuery.put(Constants.SIZE, 10);
    // Will be used for multi-tenancy
    // String instanceId = request.getString("instanceId");
    JsonArray filterQuery = new JsonArray();
    // Will be used for multi-tenancy
    // JsonObject termQuery =
    // new JsonObject().put("term", new JsonObject()
    // .put(INSTANCE_ID_KEY + ".keyword", instanceId));
    // filterQuery.add(termQuery);

    /* Handle the search type */
    if (searchType.matches(Constants.GEOSEARCH_REGEX)) {
      logger.info("In geoSearch block---------");
      JsonObject shapeJson = new JsonObject();
      JsonObject geoSearch = new JsonObject();
      String relation;
      JsonArray coordinates;
      /* Construct the search query */
      if (request.containsKey(Constants.GEOMETRY)
          && request.getString(Constants.GEOMETRY).equalsIgnoreCase(Constants.POINT)
          && request.containsKey(Constants.GEORELATION)
          && request.containsKey(Constants.COORDINATES)
          && request.containsKey(Constants.GEOPROPERTY)
          && request.containsKey(Constants.MAX_DISTANCE)) {
        /* Construct the query for Circle */
        coordinates = request.getJsonArray(Constants.COORDINATES);
        int radius = request.getInteger(Constants.MAX_DISTANCE);
        relation = request.getString(Constants.GEORELATION);
        shapeJson
            .put(Constants.SHAPE_KEY,
                new JsonObject().put(Constants.TYPE_KEY, Constants.GEO_CIRCLE)
                    .put(Constants.COORDINATES_KEY, coordinates)
                    .put(Constants.GEO_RADIUS, radius + Constants.DISTANCE_IN_METERS))
            .put(Constants.GEO_RELATION_KEY, relation);
      } else if (request.containsKey(Constants.GEOMETRY)
          && (request.getString(Constants.GEOMETRY).equalsIgnoreCase(Constants.POLYGON)
              || request.getString(Constants.GEOMETRY).equalsIgnoreCase(Constants.LINESTRING))
          && request.containsKey(Constants.GEORELATION)
          && request.containsKey(Constants.COORDINATES)
          && request.containsKey(Constants.GEOPROPERTY)) {
        /* Construct the query for Line String, Polygon */
        String geometry = request.getString(Constants.GEOMETRY);
        relation = request.getString(Constants.GEORELATION);
        coordinates = request.getJsonArray(Constants.COORDINATES);
        int length = coordinates.getJsonArray(0).size();
        if (geometry.equalsIgnoreCase(Constants.POLYGON)
            && !coordinates.getJsonArray(0).getJsonArray(0).getDouble(0)
                .equals(coordinates.getJsonArray(0).getJsonArray(length - 1).getDouble(0))
            && !coordinates.getJsonArray(0).getJsonArray(0).getDouble(1)
                .equals(coordinates.getJsonArray(0).getJsonArray(length - 1).getDouble(1))) {
          return new JsonObject().put(Constants.ERROR, Constants.ERROR_INVALID_COORDINATE_POLYGON);
        }
        shapeJson
            .put(Constants.SHAPE_KEY, new JsonObject().put(Constants.TYPE_KEY, geometry)
                .put(Constants.COORDINATES_KEY, coordinates))
            .put(Constants.GEO_RELATION_KEY, relation);
      } else if (request.containsKey(Constants.GEOMETRY)
          && request.getString(Constants.GEOMETRY).equalsIgnoreCase(Constants.BBOX)
          && request.containsKey(Constants.GEORELATION)
          && request.containsKey(Constants.COORDINATES)
          && request.containsKey(Constants.GEOPROPERTY)) {
        /* Construct the query for BBOX */
        relation = request.getString(Constants.GEORELATION);
        coordinates = request.getJsonArray(Constants.COORDINATES);
        shapeJson = new JsonObject();
        shapeJson
            .put(Constants.SHAPE_KEY,
                new JsonObject().put(Constants.TYPE_KEY, Constants.GEO_BBOX)
                    .put(Constants.COORDINATES_KEY, coordinates))
            .put(Constants.GEO_RELATION_KEY, relation);

      } else {
        return new JsonObject().put(Constants.ERROR, Constants.ERROR_INVALID_GEO_PARAMETER);
      }
      geoSearch.put(Constants.GEO_SHAPE_KEY, new JsonObject().put(Constants.GEO_KEY, shapeJson));
      filterQuery.add(geoSearch);
    }
    if (searchType.matches(Constants.RESPONSE_FILTER_REGEX)) {
      /* Construct the filter for response */
      logger.info("In responseFilter block---------");
      if (request.containsKey(Constants.ATTRIBUTE)) {
        JsonArray sourceFilter = request.getJsonArray(Constants.ATTRIBUTE);
        elasticQuery.put(Constants.SOURCE_FILTER_KEY, sourceFilter);
      } else {
        return new JsonObject().put(Constants.ERROR, Constants.ERROR_INVALID_RESPONSE_FILTER);
      }
    }

    elasticQuery.put(Constants.QUERY_KEY, new JsonObject().put(Constants.BOOL_KEY,
        new JsonObject().put(Constants.FILTER_KEY, filterQuery)));

    return elasticQuery;

  }
}