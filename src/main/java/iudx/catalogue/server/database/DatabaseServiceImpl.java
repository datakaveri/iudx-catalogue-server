package iudx.catalogue.server.database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
          LOGGER.error("Fail: DB Request;" + errorJson.toString());
          handler.handle(Future.failedFuture(errorJson.toString()));
      }
    });
    return this;
  }

  @Override
  public DatabaseService countQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    /* Initialize elastic clients and JsonObjects */
    JsonObject errorJson = new JsonObject();
    request.put(SEARCH, false);
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
          LOGGER.error("Fail: DB Request;" + searchRes.cause());
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
      if (checkRes.failed()) {
        handler.handle(Future.failedFuture(errorJson.toString()));
      }
      if (checkRes.succeeded()) {
        if (checkRes.result().getInteger(TOTAL_HITS) != 0) {
          handler.handle(Future.failedFuture("Fail: Doc Exists"));
          LOGGER.error("Fail: Insertion failed");
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
    String checkQuery = "{\"_source\": [\"id\"],"
                        +"\"query\": {\"term\": {\"id.keyword\": \"$1\"}}}";
    JsonObject errorJson = new JsonObject();
    String id = doc.getString("id");

    errorJson.put(STATUS, FAILED).put(RESULTS,
        new JsonArray().add(new JsonObject().put(ID, id)
            .put(METHOD, UPDATE).put(STATUS, FAILED)));


    client.searchGetId(CAT_INDEX_NAME, checkQuery.replace("$1", id), checkRes -> {
      if (checkRes.failed()) {
        LOGGER.error("Fail: Check query fail");
        handler.handle(Future.failedFuture(errorJson.toString()));
        return;
      }
      if (checkRes.succeeded()) {
        LOGGER.debug("Success: Check index for doc");
        if (checkRes.result().getInteger(TOTAL_HITS) != 1) {
          LOGGER.error("Fail: Doc doesn't exist, can't update");
          handler.handle(Future.failedFuture(errorJson.toString()));
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

    LOGGER.debug("Info: Updating item");

    errorJson.put(STATUS, FAILED).put(RESULTS,
        new JsonArray().add(new JsonObject().put(ID, id)
            .put(METHOD, UPDATE).put(STATUS, FAILED)));

    checkQuery.put(SOURCE, "[\"\"]").put(QUERY_KEY,
        new JsonObject().put(TERM, new JsonObject().put(ID_KEYWORD, id)));

    client.searchGetId(CAT_INDEX_NAME, checkQuery.toString(), checkRes -> {
      if (checkRes.failed()) {
        LOGGER.info(checkRes.cause());
        handler.handle(Future.failedFuture(errorJson.toString()));
      }
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
    String itemId = request.getString(ID);
    JsonObject req = new JsonObject();
    req.put(
        QUERY_KEY,
        new JsonObject().put(TERM, new JsonObject().put(ID_KEYWORD, itemId)));
    client.searchAsync(
        CAT_INDEX_NAME,
        req.toString(),
        clientHandler -> {
          if (clientHandler.succeeded()) {
            LOGGER.info("Success: Retreived item");
            JsonObject responseJson = clientHandler.result();
            handler.handle(Future.succeededFuture(responseJson));
          } else {
            LOGGER.error("Fail: Failed getting item");
            /* Handle request error */
            handler.handle(
                Future.failedFuture(
                    new JsonObject().put(STATUS, FAILED).toString()));
          }
        });
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public DatabaseService listItems(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    LOGGER.debug("Info: Reached list items;" + request.toString());
    String itemType = request.getString(ITEM_TYPE);
    String type = request.getString(TYPE_KEY);
    String instanceID = request.getString(INSTANCE);
    String req = "";


    if (itemType.equalsIgnoreCase("instances")) {
      req = LIST_INSTANCES_QUERY;
    } else if (itemType.equalsIgnoreCase(TAGS)) {
      if (instanceID == null || instanceID == "") {
        req = LIST_TAGS_QUERY;
      } else {
        req = LIST_INSTANCE_TAGS_QUERY.replace("$1", instanceID);
      }
    } else {
      if (instanceID == null || instanceID == "") {
        req = LIST_TYPES_QUERY.replace("$1", type);
      } else {
        req = LIST_INSTANCE_TYPES_QUERY.replace("$1", type)
                                      .replace("$2", instanceID);
      }
    }
    LOGGER.debug("Info: Listing items;" + req);
    client.listAggregationAsync(
        CAT_INDEX_NAME,
        req,
        clientHandler -> {
          if (clientHandler.succeeded()) {
            LOGGER.info("Success: List request");
            JsonObject responseJson = clientHandler.result();
            handler.handle(Future.succeededFuture(responseJson));
          } else {
            LOGGER.error("Fail: DB request has failed");
            /* Handle request error */
            handler.handle(
                Future.failedFuture(
                    new JsonObject().put(STATUS, FAILED).toString()));
          }
        });
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listResourceRelationship(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    /* <resourceGroupId>/resource */
    /* Initialize JsonObjects & JsonArrays */
    JsonObject errorJson = new JsonObject();
    JsonObject elasticQuery = new JsonObject();
    JsonObject boolObject = new JsonObject();

    /* Validating the request */
    if (request.containsKey(ID) && request.getString(RELATIONSHIP).equals(REL_RESOURCE)) {

      /* parsing resourceGroupId from the request */
      String resourceGroupId = request.getString(ID);

      /* Constructing db queries */
      boolObject.put(BOOL_KEY, new JsonObject().put(MUST_KEY,
              new JsonArray()
              .add(new JsonObject().put(TERM,
                  new JsonObject().put(REL_RESOURCE_GRP.concat(KEYWORD_KEY),
                          resourceGroupId)))
              .add(new JsonObject().put(TERM,
                  new JsonObject().put(TYPE_KEY.concat(KEYWORD_KEY), ITEM_TYPE_RESOURCE)))));

      elasticQuery.put(QUERY_KEY, boolObject);

      LOGGER.debug("Info: Query constructed;" + elasticQuery.toString());

      client.searchAsync(REL_API_INDEX_NAME, elasticQuery.toString(), searchRes -> {
        if (searchRes.succeeded()) {
          LOGGER.debug("Success: Successful DB request");
          handler.handle(Future.succeededFuture(searchRes.result()));
        } else {
            handler.handle(Future.failedFuture(errorJson.toString()));
        }
      });
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listResourceGroupRelationship(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    /* <resourceId>/resourceGroup */
    /* Initialize JsonObjects and JsonArrays */
    JsonObject errorJson = new JsonObject();
    JsonObject elasticQuery = new JsonObject();
    JsonObject boolObject = new JsonObject();

    /* Validating the request */
    if (request.containsKey(ID) && request.getString(RELATIONSHIP).equals(REL_RESOURCE_GRP)) {

      /* parsing resourceGroupId from the request ID */
      String resourceGroupId =
          StringUtils.substringBeforeLast(request.getString(ID), FORWARD_SLASH);

      /* constructing the query */
      boolObject.put(BOOL_KEY, new JsonObject().put(MUST_KEY,
              new JsonArray()
              .add(new JsonObject().put(TERM, new JsonObject().put(ID_KEYWORD, resourceGroupId)))
              .add(new JsonObject().put(TERM,
                  new JsonObject().put(TYPE_KEY.concat(KEYWORD_KEY), ITEM_TYPE_RESOURCE_GROUP)))));

      elasticQuery.put(QUERY_KEY, boolObject);

      LOGGER.debug("Info: Query constructed;" + elasticQuery.toString());

      client.searchAsync(REL_API_INDEX_NAME, elasticQuery.toString(), searchRes -> {
        if (searchRes.succeeded()) {
          LOGGER.debug("Success: Successful DB request");
          handler.handle(Future.succeededFuture(searchRes.result()));
        } else {
          handler.handle(Future.failedFuture(errorJson.toString()));
        }
      });
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listProviderRelationship(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    /* <resourceId>/provider */
    /* Initialize JsonObjects and JsonArrays */
    JsonObject errorJson = new JsonObject();
    JsonObject elasticQuery = new JsonObject();
    JsonObject boolObject = new JsonObject();

    /* Validating the request */
    if (request.containsKey(ID) && request.getString(RELATIONSHIP).equals(REL_PROVIDER)) {

      /* parsing id from the request */
      String id = request.getString(ID);

      /* parsing providerId from the request */
      String providerId = StringUtils.substring(id, 0, id.indexOf("/", id.indexOf("/") + 1));

      /* constructing the query */
      boolObject.put(BOOL_KEY,
          new JsonObject().put(MUST_KEY,
              new JsonArray()
                  .add(new JsonObject().put(TERM, new JsonObject().put(ID_KEYWORD, providerId)))
                  .add(new JsonObject().put(TERM,
                      new JsonObject().put(TYPE_KEY.concat(KEYWORD_KEY), ITEM_TYPE_PROVIDER)))));

      elasticQuery.put(QUERY_KEY, boolObject);

      LOGGER.debug("Info: Query constructed;" + elasticQuery.toString());

      client.searchAsync(REL_API_INDEX_NAME, elasticQuery.toString(), searchRes -> {
        if (searchRes.succeeded()) {
          LOGGER.debug("Success: Successful DB request");
          handler.handle(Future.succeededFuture(searchRes.result()));
        } else {
          handler.handle(Future.failedFuture(errorJson.toString()));
        }
      });
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listResourceServerRelationship(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    /* <resourceId or resourceGroupId>/resourceServer */
    /* Initialize JsonObjects and JsonArrays */
    JsonObject errorJson = new JsonObject();
    JsonObject elasticQuery = new JsonObject();
    JsonObject boolObject = new JsonObject();

    /* Validating the request */
    if (request.containsKey(ID) && request.getString(RELATIONSHIP).equals(REL_RESOURCE_SVR)) {

      /* parsing id from the request */
      String[] id = request.getString(ID).split(FORWARD_SLASH);

      /* constructing the query */
      boolObject.put(BOOL_KEY, new JsonObject().put(MUST_KEY,
          new JsonArray().add(new JsonObject().put(MATCH_KEY, new JsonObject().put(ID, id[0])))
              .add(new JsonObject().put(MATCH_KEY, new JsonObject().put(ID, id[2])))
              .add(new JsonObject().put(TERM,
                  new JsonObject().put(TYPE_KEY.concat(KEYWORD_KEY), ITEM_TYPE_RESOURCE_SERVER)))));

      elasticQuery.put(QUERY_KEY, boolObject);

      LOGGER.debug("Info: Query constructed;" + elasticQuery.toString());

      client.searchAsync(REL_API_INDEX_NAME, elasticQuery.toString(), searchRes -> {
        if (searchRes.succeeded()) {
          LOGGER.debug("Success: Successful DB request");
          handler.handle(Future.succeededFuture(searchRes.result()));
        } else {
          handler.handle(Future.failedFuture(errorJson.toString()));
        }
      });
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listTypes(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    /* Initialize JsonObjects and JsonArrays */
    JsonObject errorJson = new JsonObject();
    JsonObject elasticQuery = new JsonObject();
    JsonObject boolObject = new JsonObject();

    /* Validating the request */
    if (request.containsKey(ID) && request.getString(RELATIONSHIP).equals(TYPE_KEY)) {

      /* parsing id from the request */
      String itemId = request.getString(ID);

      /* constructing the query */
      boolObject.put(BOOL_KEY, new JsonObject().put(MUST_KEY, new JsonArray()
          .add(new JsonObject().put(TERM, new JsonObject().put(ID_KEYWORD, itemId)))));

      elasticQuery.put(QUERY_KEY, boolObject);

      LOGGER.debug("Info: Query constructed;" + elasticQuery.toString());

      client.searchAsync(REL_API_INDEX_NAME, elasticQuery.toString(), searchRes -> {
        if (searchRes.succeeded()) {
          LOGGER.debug("Success: Successful DB request");
          handler.handle(Future.succeededFuture(searchRes.result()));
        } else {
          handler.handle(Future.failedFuture(errorJson.toString()));
        }
      });
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService relSearch(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    /* Initialize elastic clients and JsonObjects */
    JsonObject errorJson = new JsonObject();
    JsonObject elasticQuery = new JsonObject();
    JsonObject boolObject = new JsonObject();

    /* Validating the request */
    if (request.containsKey(RELATIONSHIP) && request.containsKey(VALUE)) {

      /* parsing data parameters from the request */
      String relReq = request.getJsonArray(RELATIONSHIP).getString(0);

      if (relReq.contains(".")) {

        LOGGER.debug("Info: Reached relationship search dbServiceImpl");

        String typeValue = null;
        String[] relReqs = relReq.split("\\.", 2);
        String relReqsKey = relReqs[1];
        String relReqsValue = request.getJsonArray(VALUE).getJsonArray(0).getString(0);

        if (relReqs[0].equalsIgnoreCase(REL_PROVIDER)) {
          typeValue = ITEM_TYPE_PROVIDER;

        } else if (relReqs[0].equalsIgnoreCase(REL_RESOURCE)) {
          typeValue = ITEM_TYPE_RESOURCE;

        } else if (relReqs[0].equalsIgnoreCase(REL_RESOURCE_GRP)) {
          typeValue = ITEM_TYPE_RESOURCE_GROUP;

        } else if (relReqs[0].equalsIgnoreCase(REL_RESOURCE_SVR)) {
          typeValue = ITEM_TYPE_RESOURCE_SERVER;

        } else {
          /* Constructing error response */
          errorJson.put(STATUS, FAILED).put(DESCRIPTION, ERROR_INVALID_PARAMETER);

          handler.handle(Future.failedFuture(errorJson.toString()));
          return null;
        }

        /* Constructing the db query */
        boolObject.put(BOOL_KEY,
            new JsonObject().put(MUST_KEY, new JsonArray()
                .add(new JsonObject().put(TERM,
                    new JsonObject().put(TYPE_KEY.concat(KEYWORD_KEY), typeValue)))
                .add(new JsonObject().put(MATCH_KEY,
                    new JsonObject().put(relReqsKey, relReqsValue)))));

      } else {
        handler.handle(Future.failedFuture(errorJson.toString()));
        return null;
      }
      
      elasticQuery.put(QUERY_KEY, boolObject).put(SOURCE, ID);
      
      /* Initial db query to filter matching attributes */
      client.searchAsync(REL_API_INDEX_NAME, elasticQuery.toString(), searchRes -> {
        if (searchRes.succeeded()) {

          JsonArray resultValues = searchRes.result().getJsonArray(RESULTS);
          elasticQuery.clear();
          boolObject.clear();
          JsonArray idCollection = new JsonArray();

          /* iterating over the filtered response json array */
          for (Object idIndex : resultValues) {
            JsonObject id = (JsonObject) idIndex;

            if (!id.isEmpty()) {
              idCollection
                  .add(new JsonObject().put(WILDCARD_KEY,
                      new JsonObject().put(ID_KEYWORD, id.getString(ID).concat("*"))));
            }
          }
          
          /* constructing the db query */
          boolObject.put(BOOL_KEY, new JsonObject().put(SHOULD_KEY, idCollection));
          elasticQuery.put(QUERY_KEY, boolObject);
          
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
          
          LOGGER.debug("INFO: Query constructed;" + elasticQuery.toString());

          /* db query to find the relationship to the initial query */
          client.searchAsync(REL_API_INDEX_NAME, elasticQuery.toString(), relSearchRes -> {
            if (relSearchRes.succeeded()) {
              
              LOGGER.debug("Success: Successful DB request");
              handler.handle(Future.succeededFuture(relSearchRes.result()));
            }
            else if (relSearchRes.failed()) {
              handler.handle(Future.failedFuture(relSearchRes.cause().getMessage()));
            }
          });
        } else {
          handler.handle(Future.failedFuture(errorJson.toString()));
        }
      });
    }
    return this;
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
    String instanceId = request.getString(INSTANCE);
    JsonArray filterQuery = new JsonArray();
    JsonArray mustQuery = new JsonArray();

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

    if (instanceId != null) {
      String instanceFilter = "{\"match\":"
                              + "{\"instance\": \"" + instanceId + "\"}}";
      LOGGER.debug("Info: Instance found in query;" + instanceFilter);
      mustQuery.add(new JsonObject(instanceFilter));

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
