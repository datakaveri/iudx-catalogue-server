package iudx.catalogue.server.database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.Request;

import iudx.catalogue.server.database.ElasticClient;

import static iudx.catalogue.server.database.Constants.*;


/**
 * The Database Service Implementation.
 *
 * <h1>Database Service Implementation</h1>
 *
 * <p>
 * The Database Service implementation in the IUDX Catalogue Server implements the definitions of
 * the {@link iudx.catalogue.server.database.DatabaseService}.
 *
 * @version 1.0
 * @since 2020-05-31
 */
public class DatabaseServiceImpl implements DatabaseService {

  private static final Logger LOGGER = LogManager.getLogger(DatabaseServiceImpl.class);
  private final ElasticClient client;

  public DatabaseServiceImpl(ElasticClient client) {
    this.client = client;
  }



  @Override
  public DatabaseService searchQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    LOGGER.debug("Info: searchQuery;" + request.toString());

    /* Initialize elastic clients and JsonObjects */
    JsonObject errorJson = new JsonObject();
    request.put(SEARCH, true);
    /* Validate the Request */
    if (!request.containsKey(SEARCH_TYPE)) {
      errorJson.put(STATUS, FAILED).put(DESCRIPTION,
          NO_SEARCH_TYPE_FOUND);
      handler.handle(Future.failedFuture(errorJson.toString()));
      return null;
    }

    /* Construct the query to be made */
    JsonObject query = queryDecoder(request);
    if (query.containsKey(ERROR)) {
      LOGGER.error("Fail: Query returned with an error");
      errorJson.put(STATUS, FAILED).put(DESCRIPTION,
          query.getString(ERROR));
      handler.handle(Future.failedFuture(errorJson.toString()));
      return null;
    }
    LOGGER.debug("Info: Query constructed;" + query.toString());

    client.searchAsync(CAT_INDEX_NAME, query.toString(), searchRes -> {
      if (searchRes.succeeded()) {
        LOGGER.debug("Success: Successful DB request");
        handler.handle(Future.succeededFuture(searchRes.result()));
      } else {
          handler.handle(Future.failedFuture(errorJson.toString()));
      }
    });
    return this;
  }

  @Override
  public DatabaseService countQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    /* Initialize elastic clients and JsonObjects */
    JsonObject errorJson = new JsonObject();
    request.put(SEARCH, true);
    /* Validate the Request */
    if (!request.containsKey(SEARCH_TYPE)) {
      errorJson.put(STATUS, FAILED).put(DESCRIPTION,
          NO_SEARCH_TYPE_FOUND);
      handler.handle(Future.failedFuture(errorJson.toString()));
      return null;
    }

    /* Construct the query to be made */
    JsonObject query = queryDecoder(request);
    if (query.containsKey(ERROR)) {
      LOGGER.error("Fail: Query returned with an error");
      errorJson.put(STATUS, FAILED).put(DESCRIPTION,
          query.getString(ERROR));
      handler.handle(Future.failedFuture(errorJson.toString()));
      return null;
    }
    LOGGER.debug("Info: Query constructed;" + query.toString());

    client.countAsync(CAT_INDEX_NAME, query.toString(), searchRes -> {
      if (searchRes.succeeded()) {
        LOGGER.debug("Success: Successful DB request");
        handler.handle(Future.succeededFuture(searchRes.result()));
      } else {
          handler.handle(Future.failedFuture(errorJson.toString()));
      }
    });
    return this;
  }


  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService createItem(JsonObject doc, Handler<AsyncResult<JsonObject>> handler) {
    JsonObject checkQuery = new JsonObject();
    JsonObject errorJson = new JsonObject();
    String id = doc.getString("id");

    errorJson.put(STATUS, FAILED).put(RESULTS,
        new JsonArray().add(new JsonObject().put(ID, id)
            .put(METHOD, INSERT).put(STATUS, FAILED)));


    checkQuery.put(SOURCE, "[\"\"]").put(QUERY_KEY,
        new JsonObject().put(TERM, new JsonObject().put(ID_KEYWORD, id)));

    client.searchAsync(CAT_INDEX_NAME, checkQuery.toString(), checkRes -> {
      if (checkRes.succeeded()) {
        LOGGER.debug("Success: Check index for doc");
        if (checkRes.result().getInteger(TOTAL_HITS) != 0) {
          handler.handle(Future.failedFuture("Fail: Doc Exists"));
          return;
        }
        /* Insert document */
        client.docPostAsync(CAT_INDEX_NAME, doc.toString(), postRes -> {
          if (postRes.succeeded()) {
            LOGGER.info("Success: Inserted doc");
            JsonObject responseJson = new JsonObject();
            responseJson.put(STATUS, SUCCESS)
                        .put(RESULTS, new JsonArray()
                                        .add(new JsonObject().put(ID, id)
                                                              .put(METHOD, INSERT)
                                                              .put(STATUS, SUCCESS)));
            handler.handle(Future.succeededFuture(responseJson));
          } else {
            handler.handle(Future.failedFuture(errorJson.toString()));
            LOGGER.error("Fail: Insertion failed");
          }
        });
      } else {
        handler.handle(Future.failedFuture("Fail: Failed checking doc existence"));
        LOGGER.error("Fail: Insertion failed");
      }
    });
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService updateItem(JsonObject doc, Handler<AsyncResult<JsonObject>> handler) {
    JsonObject checkQuery = new JsonObject();
    JsonObject errorJson = new JsonObject();
    String id = doc.getString("id");

    errorJson.put(STATUS, FAILED).put(RESULTS,
        new JsonArray().add(new JsonObject().put(ID, id)
            .put(METHOD, UPDATE).put(STATUS, FAILED)));

    checkQuery.put(SOURCE, "[\"\"]").put(QUERY_KEY,
        new JsonObject().put(TERM, new JsonObject().put(ID_KEYWORD, id)));

    client.searchGetId(CAT_INDEX_NAME, checkQuery.toString(), checkRes -> {
      if (checkRes.succeeded()) {
        LOGGER.debug("Success: Check index for doc");
        if (checkRes.result().getInteger(TOTAL_HITS) != 1) {
          LOGGER.error("Fail: Doc doesn't exist, can't update");
          handler.handle(Future.failedFuture("Fail: Doc doesn't exist"));
          return;
        }
        String docId = checkRes.result().getJsonArray(RESULTS).getString(0);
        client.docPutAsync(CAT_INDEX_NAME, docId, doc.toString(), putRes -> {
          if (putRes.succeeded()) {
            LOGGER.info("Success: Updated doc");
            JsonObject responseJson = new JsonObject();
            responseJson.put(STATUS, SUCCESS)
                        .put(RESULTS, new JsonArray()
                                        .add(new JsonObject().put(ID, id)
                                                              .put(METHOD, UPDATE)
                                                              .put(STATUS, SUCCESS)));
            handler.handle(Future.succeededFuture(responseJson));
          } else {
            handler.handle(Future.failedFuture(errorJson.toString()));
            LOGGER.error("Fail: Updation failed");
          }
        });
      }
    });
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService deleteItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    JsonObject checkQuery = new JsonObject();
    JsonObject errorJson = new JsonObject();
    String id = request.getString("id");

    errorJson.put(STATUS, FAILED).put(RESULTS,
        new JsonArray().add(new JsonObject().put(ID, id)
            .put(METHOD, UPDATE).put(STATUS, FAILED)));

    checkQuery.put(SOURCE, "[\"\"]").put(QUERY_KEY,
        new JsonObject().put(TERM, new JsonObject().put(ID_KEYWORD, id)));

    client.searchGetId(CAT_INDEX_NAME, checkQuery.toString(), checkRes -> {
      if (checkRes.succeeded()) {
        LOGGER.debug("Success: Check index for doc");
        if (checkRes.result().getInteger(TOTAL_HITS) != 1) {
          LOGGER.error("Fail: Doc doesn't exist, can't delete");
          handler.handle(Future.failedFuture("Fail: Doc doesn't exist"));
          return;
        }
        String docId = checkRes.result().getJsonArray(RESULTS).getString(0);
        client.docDelAsync(CAT_INDEX_NAME, docId, delRes -> {
          if (delRes.succeeded()) {
            LOGGER.info("Success: Deleted doc");
            JsonObject responseJson = new JsonObject();
            responseJson.put(STATUS, SUCCESS)
                        .put(RESULTS, new JsonArray()
                                        .add(new JsonObject().put(ID, id)
                                                              .put(METHOD, DELETE)
                                                              .put(STATUS, SUCCESS)));
            handler.handle(Future.succeededFuture(responseJson));
          } else {
            handler.handle(Future.failedFuture(errorJson.toString()));
            LOGGER.error("Fail: Deletion failed");
          }
        });
      }
    });
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public DatabaseService getItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String itemId = request.getString(Constants.ID);
    JsonObject req = new JsonObject();
    req.put(
        Constants.QUERY_KEY,
        new JsonObject().put(Constants.TERM, new JsonObject().put(Constants.ID_KEYWORD, itemId)));
    System.out.println(req.toString());
    client.searchAsync(
        Constants.CAT_INDEX_NAME,
        req.toString(),
        clientHandler -> {
          if (clientHandler.succeeded()) {
            LOGGER.info("Successful DB request");
            JsonObject responseJson = clientHandler.result();
            System.out.println(responseJson);
            handler.handle(Future.succeededFuture(responseJson));
          } else {
            LOGGER.info("DB request has failed. ERROR:\n");
            /* Handle request error */
            handler.handle(
                Future.failedFuture(
                    new JsonObject().put(Constants.STATUS, Constants.FAILED).toString()));
          }
        });
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public DatabaseService listItems(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String itemType = request.getString(Constants.ITEM_TYPE);
    String type = request.getString(Constants.TYPE_KEY);
    String instanceID = request.getString(Constants.INSTANCE_ID_KEY);
    JsonObject req = new JsonObject();
    if (itemType.equalsIgnoreCase("instances")) {
      req.put(Constants.SIZE, 0)
          .put(
              Constants.AGGREGATION_KEY,
              new JsonObject()
                  .put(
                      Constants.RESULTS,
                      new JsonObject()
                          .put(
                              Constants.TERMS_KEY,
                              new JsonObject()
                                  .put(Constants.FIELD, Constants.INSTANCE_ID_KEYWORD)
                                  .put(Constants.SIZE, 10000))));
    } else if (itemType.equalsIgnoreCase(Constants.TAGS)) {
      req.put(
              Constants.QUERY_KEY,
              new JsonObject()
                  .put(
                      Constants.BOOL_KEY,
                      new JsonObject()
                          .put(
                              Constants.FILTER_KEY,
                              new JsonObject()
                                  .put(
                                      Constants.TERM,
                                      new JsonObject()
                                          .put(Constants.INSTANCE_ID_KEY, instanceID)))))
          .put(
              Constants.AGGREGATION_KEY,
              new JsonObject()
                  .put(
                      Constants.RESULTS,
                      new JsonObject()
                          .put(
                              Constants.TERMS_KEY,
                              new JsonObject()
                                  .put(Constants.FIELD, Constants.TAGS_KEYWORD)
                                  .put(Constants.SIZE_KEY, 10000))));
    } else {
      req.put(
              Constants.QUERY_KEY,
              new JsonObject()
                  .put(
                      Constants.BOOL_KEY,
                      new JsonObject()
                          .put(
                              Constants.FILTER_KEY,
                              new JsonArray()
                                  .add(
                                      new JsonObject()
                                          .put(
                                              Constants.TERM,
                                              new JsonObject()
                                                  .put(Constants.INSTANCE_ID_KEY, instanceID)))
                                  .add(
                                      new JsonObject()
                                          .put(
                                              Constants.MATCH_KEY,
                                              new JsonObject().put(Constants.TYPE_KEY, type))))))
          .put(
              Constants.AGGREGATION_KEY,
              new JsonObject()
                  .put(
                      Constants.RESULTS,
                      new JsonObject()
                          .put(
                              Constants.TERMS_KEY,
                              new JsonObject()
                                  .put(Constants.FIELD, Constants.ID_KEYWORD)
                                  .put(Constants.SIZE_KEY, 10000))));
    }
    System.out.println(req.toString());
    System.out.println(req.toString());
    client.listAggregationAsync(
        Constants.CAT_INDEX_NAME,
        req.toString(),
        clientHandler -> {
          if (clientHandler.succeeded()) {
            LOGGER.info("Successful DB request");
            JsonObject responseJson = clientHandler.result();
            System.out.println(responseJson);
            handler.handle(Future.succeededFuture(responseJson));
          } else {
            LOGGER.info("DB request has failed. ERROR:\n");
            /* Handle request error */
            handler.handle(
                Future.failedFuture(
                    new JsonObject().put(Constants.STATUS, Constants.FAILED).toString()));
          }
        });
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

    String result = "{ \"status\": \"success\", \"results\": [ \"rg-1\", \"rg-2\" ] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listResourceServerRelationship(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    String result = "{ \"status\": \"success\", \"results\": [ \"rg-1\", \"rg-2\" ] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listTypes(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    String result = "{ \"status\": \"success\", \"results\": [ \"rg-1\", \"rg-2\" ] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

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
    String searchType = request.getString(SEARCH_TYPE);
    JsonObject elasticQuery = new JsonObject();
    Boolean match = false;
    JsonObject boolObject = new JsonObject().put(BOOL_KEY, new JsonObject());
    /* TODO: Pagination for large result set */
    if (request.getBoolean(SEARCH)) {
      elasticQuery.put(SIZE_KEY, 10);
    }
    // Will be used for multi-tenancy
    // String instanceId = request.getString("instanceId");
    JsonArray filterQuery = new JsonArray();
    JsonArray mustQuery = new JsonArray();
    // Will be used for multi-tenancy
    // JsonObject termQuery =
    // new JsonObject().put("term", new JsonObject()
    // .put(INSTANCE_ID_KEY + ".keyword", instanceId));
    // filterQuery.add(termQuery);

    /* Handle the search type */
    if (searchType.matches(GEOSEARCH_REGEX)) {
      LOGGER.info("In geoSearch block---------");
      match = true;
      JsonObject shapeJson = new JsonObject();
      JsonObject geoSearch = new JsonObject();
      String relation;
      JsonArray coordinates;
      /* Construct the search query */
      if (request.containsKey(GEOMETRY)
          && request.getString(GEOMETRY).equalsIgnoreCase(POINT)
          && request.containsKey(GEORELATION)
          && request.containsKey(COORDINATES_KEY)
          && request.containsKey(GEOPROPERTY)
          && request.containsKey(MAX_DISTANCE)) {
        /* Construct the query for Circle */
        coordinates = request.getJsonArray(COORDINATES_KEY);
        int radius = request.getInteger(MAX_DISTANCE);
        // int radius = Integer.parseInt(request.getString(MAX_DISTANCE));
        relation = request.getString(GEORELATION);
        shapeJson
            .put(SHAPE_KEY,
                new JsonObject().put(TYPE_KEY, GEO_CIRCLE)
                    .put(COORDINATES_KEY, coordinates)
                    .put(GEO_RADIUS, radius + DISTANCE_IN_METERS))
            .put(GEO_RELATION_KEY, relation);
      } else if (request.containsKey(GEOMETRY)
          && (request.getString(GEOMETRY).equalsIgnoreCase(POLYGON)
              || request.getString(GEOMETRY).equalsIgnoreCase(LINESTRING))
          && request.containsKey(GEORELATION)
          && request.containsKey(COORDINATES_KEY)
          && request.containsKey(GEOPROPERTY)) {
        /* Construct the query for Line String, Polygon */
        String geometry = request.getString(GEOMETRY);
        relation = request.getString(GEORELATION);
        coordinates = request.getJsonArray(COORDINATES_KEY);
        int length = coordinates.getJsonArray(0).size();
        if (geometry.equalsIgnoreCase(POLYGON)
            && !coordinates.getJsonArray(0).getJsonArray(0).getDouble(0)
                .equals(coordinates.getJsonArray(0).getJsonArray(length - 1).getDouble(0))
            && !coordinates.getJsonArray(0).getJsonArray(0).getDouble(1)
                .equals(coordinates.getJsonArray(0).getJsonArray(length - 1).getDouble(1))) {
          return new JsonObject().put(ERROR, ERROR_INVALID_COORDINATE_POLYGON);
        }
        shapeJson
            .put(SHAPE_KEY, new JsonObject().put(TYPE_KEY, geometry)
                .put(COORDINATES_KEY, coordinates))
            .put(GEO_RELATION_KEY, relation);
      } else if (request.containsKey(GEOMETRY)
          && request.getString(GEOMETRY).equalsIgnoreCase(BBOX)
          && request.containsKey(GEORELATION)
          && request.containsKey(COORDINATES_KEY)
          && request.containsKey(GEOPROPERTY)) {
        /* Construct the query for BBOX */
        relation = request.getString(GEORELATION);
        coordinates = request.getJsonArray(COORDINATES_KEY);
        shapeJson = new JsonObject();
        shapeJson
            .put(SHAPE_KEY,
                new JsonObject().put(TYPE_KEY, GEO_BBOX)
                    .put(COORDINATES_KEY, coordinates))
            .put(GEO_RELATION_KEY, relation);

      } else {
        return new JsonObject().put(ERROR, ERROR_INVALID_GEO_PARAMETER);
      }
      geoSearch.put(GEO_SHAPE_KEY, new JsonObject().put(GEO_KEY, shapeJson));
      filterQuery.add(geoSearch);

    }

    /* Construct the query for text based search */
    if (searchType.matches(TEXTSEARCH_REGEX)) {
      LOGGER.info("Text search block");

      match = true;
      /* validating tag search attributes */
      if (request.containsKey(Q_KEY) && !request.getString(Q_KEY).isBlank()) {

        /* fetching values from request */
        String textAttr = request.getString(Q_KEY);

        /* constructing db queries */
        mustQuery.add(new JsonObject().put(STRING_QUERY_KEY,
            new JsonObject().put(QUERY_KEY, textAttr)));
      }
    }

    /* Construct the query for attribute based search */
    if (searchType.matches(ATTRIBUTE_SEARCH_REGEX)) {
      LOGGER.info("Attribute search block");

      match = true;

      /* validating tag search attributes */
      if (request.containsKey(PROPERTY)
          && !request.getJsonArray(PROPERTY).isEmpty()
          && request.containsKey(VALUE)
          && !request.getJsonArray(VALUE).isEmpty()) {

        /* fetching values from request */
        JsonArray propertyAttrs = request.getJsonArray(PROPERTY);
        JsonArray valueAttrs = request.getJsonArray(VALUE);

        /* For attribute property and values search */
        if (propertyAttrs.size() == valueAttrs.size()) {

          /* Mapping and constructing the value attributes with the property attributes for query */
          for (int i = 0; i < valueAttrs.size(); i++) {
            JsonObject boolQuery = new JsonObject();
            JsonArray shouldQuery = new JsonArray();
            JsonArray valueArray = valueAttrs.getJsonArray(i);

            for (int j = 0; j < valueArray.size(); j++) {
              JsonObject matchQuery = new JsonObject();

              /* Attribute related queries using "match" and without the ".keyword" */
              if (propertyAttrs.getString(i).equals(TAGS)
                  || propertyAttrs.getString(i).equals(DESCRIPTION_ATTR)
                  || propertyAttrs.getString(i).startsWith(LOCATION)) {

                matchQuery.put(propertyAttrs.getString(i), valueArray.getString(j));
                shouldQuery.add(new JsonObject().put(MATCH_KEY, matchQuery));

                /* Attribute related queries using "match" and with the ".keyword" */
              } else {
                /* checking keyword in the query paramters */
                if (propertyAttrs.getString(i).endsWith(KEYWORD_KEY)) {
                  matchQuery.put(propertyAttrs.getString(i), valueArray.getString(j));

                }else {

                  /* add keyword if not avaialble */
                  matchQuery.put(propertyAttrs.getString(i).concat(KEYWORD_KEY),
                      valueArray.getString(j));
                }
                shouldQuery.add(new JsonObject().put(MATCH_KEY, matchQuery));
              }
            }
            mustQuery.add(new JsonObject().put(BOOL_KEY,
                boolQuery.put(SHOULD_KEY, shouldQuery)));
          }
        } else {
          return new JsonObject().put(ERROR, ERROR_INVALID_PARAMETER);
        }
      }
    }

    /* checking the requests for limit attribute */
    if (request.containsKey(LIMIT)) {
      Integer sizeFilter = request.getInteger(LIMIT);
      elasticQuery.put(SIZE, sizeFilter);
    }

    /* checking the requests for offset attribute */
    if (request.containsKey(OFFSET)) {
      Integer offsetFilter = request.getInteger(OFFSET);
      elasticQuery.put(FROM, offsetFilter);
    }

    if (searchType.matches(RESPONSE_FILTER_REGEX)) {
      /* Construct the filter for response */
      LOGGER.info("In responseFilter block---------");
      match = true;
      if (!request.getBoolean(SEARCH)) {
        return new JsonObject().put("Error", COUNT_UNSUPPORTED);
      }
      if (request.containsKey(ATTRIBUTE)) {
        JsonArray sourceFilter = request.getJsonArray(ATTRIBUTE);
        elasticQuery.put(SOURCE, sourceFilter);
      } else if (request.containsKey(FILTER_KEY)) {
        JsonArray sourceFilter = request.getJsonArray(FILTER_KEY);
        elasticQuery.put(SOURCE, sourceFilter);
        elasticQuery.put(SOURCE, sourceFilter);
      } else {
        return new JsonObject().put(ERROR, ERROR_INVALID_RESPONSE_FILTER);
      }
    }

    if (!match) {
      return new JsonObject().put("Error", INVALID_SEARCH);
    } else {
      /* return fully formed elastic query */
      boolObject.getJsonObject(BOOL_KEY).put(FILTER_KEY, filterQuery)
          .put(MUST_KEY, mustQuery);
      return elasticQuery.put(QUERY_KEY, boolObject);
    }
  }
}
