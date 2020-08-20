package iudx.catalogue.server.database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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
  private final QueryDecoder queryDecoder = new QueryDecoder();

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
      errorJson.put(STATUS, FAILED).put(DESCRIPTION, NO_SEARCH_TYPE_FOUND);
      handler.handle(Future.failedFuture(errorJson.toString()));
      return null;
    }

    /* Construct the query to be made */
    JsonObject query = queryDecoder.searchQuery(request);
    if (query.containsKey(ERROR)) {
      LOGGER.error("Fail: Query returned with an error");
      errorJson.put(STATUS, FAILED).put(DESCRIPTION, query.getString(ERROR));
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
        handler.handle(Future.failedFuture(searchRes.cause().getMessage()));
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
      errorJson.put(STATUS, FAILED).put(DESCRIPTION, NO_SEARCH_TYPE_FOUND);
      handler.handle(Future.failedFuture(errorJson.toString()));
      return null;
    }

    /* Construct the query to be made */
    JsonObject query = queryDecoder.searchQuery(request);
    if (query.containsKey(ERROR)) {
      LOGGER.error("Fail: Query returned with an error");
      errorJson.put(STATUS, FAILED).put(DESCRIPTION, query.getString(ERROR));
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

    JsonObject errorJson = new JsonObject();

    String id = doc.getString("id");
    String instanceId = doc.getString("instance");

    errorJson.put(STATUS, FAILED).put(RESULTS,
        new JsonArray().add(new JsonObject().put(ID, id).put(METHOD, INSERT).put(STATUS, FAILED)));


    // checkItem.put(SOURCE, "[\"\"]").put(QUERY_KEY,
    // new JsonObject().put(TERM, new JsonObject().put(ID_KEYWORD, id)));

    String checkItem = TERM_COMPLEX_QUERY.replace("$1", id).replace("$2", "");

    var isInstanceValid = new Object() {
      boolean value = true;
    };
    if (instanceId == null) {
      LOGGER.debug("Info: InstanceID null. Provider Item");
    } else {
      if (!instanceId.equals("")) {
        String checkInstance = TERM_COMPLEX_QUERY.replace("$1", instanceId).replace("$2", "");
        client.searchAsync(CAT_INDEX_NAME, checkInstance, checkRes -> {
          if (checkRes.failed()) {
            handler.handle(Future.failedFuture("Fail: Doc Exists"));
            isInstanceValid.value = false;
            return;
          } else {
            if (checkRes.result().getInteger(TOTAL_HITS) == 0) {
              LOGGER.debug("Info: No instance exists");
              isInstanceValid.value = false;
            }
          }
        });
      }
    }

    LOGGER.debug("Info: Instance info;" + isInstanceValid.value);

    client.searchAsync(CAT_INDEX_NAME, checkItem.toString(), checkRes -> {
      if (checkRes.failed()) {
        handler.handle(Future.failedFuture(errorJson.toString()));
      }
      if (checkRes.succeeded()) {
        if (checkRes.result().getInteger(TOTAL_HITS) != 0) {
          handler.handle(Future.failedFuture("Fail: Doc Exists"));
          LOGGER.error("Fail: Insertion failed;" + checkRes.cause());
          LOGGER.error("Fail: Insertion failed");
          return;
        }
        if (isInstanceValid.value == false) {
          handler.handle(Future.failedFuture(errorJson.toString()));
          LOGGER.error("Fail: Invalid Instance Insertion failed");
          return;
        }
        /* Insert document */
        client.docPostAsync(CAT_INDEX_NAME, doc.toString(), postRes -> {
          if (postRes.succeeded()) {
            LOGGER.info("Success: Inserted doc");
            JsonObject responseJson = new JsonObject();
            responseJson.put(STATUS, SUCCESS).put(RESULTS, new JsonArray()
                .add(new JsonObject().put(ID, id).put(METHOD, INSERT).put(STATUS, SUCCESS)));
            handler.handle(Future.succeededFuture(responseJson));
          } else {
            handler.handle(Future.failedFuture(errorJson.toString()));
            LOGGER.error("Fail: Insertion failed;" + postRes.cause());
          }
        });
      } else {
        handler.handle(Future.failedFuture("Fail: Failed checking doc existence"));
        LOGGER.error("Fail: Insertion failed;" + checkRes.cause());
      }
    });
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService updateItem(JsonObject doc, Handler<AsyncResult<JsonObject>> handler) {

    JsonObject errorJson = new JsonObject();
    String id = doc.getString("id");

    String checkQuery = TERM_COMPLEX_QUERY.replace("$1", id).replace("$2", "\"" + id + "\"");

    errorJson.put(STATUS, FAILED).put(RESULTS,
        new JsonArray().add(new JsonObject().put(ID, id).put(METHOD, UPDATE).put(STATUS, FAILED)));


    client.searchGetId(CAT_INDEX_NAME, checkQuery, checkRes -> {
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
            responseJson.put(STATUS, SUCCESS).put(RESULTS, new JsonArray()
                .add(new JsonObject().put(ID, id).put(METHOD, UPDATE).put(STATUS, SUCCESS)));
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
    // JsonObject checkQuery = new JsonObject();
    JsonObject errorJson = new JsonObject();
    String id = request.getString("id");

    LOGGER.debug("Info: Updating item");

    errorJson.put(STATUS, FAILED).put(RESULTS,
        new JsonArray().add(new JsonObject().put(ID, id).put(METHOD, UPDATE).put(STATUS, FAILED)));

    String checkQuery = TERM_COMPLEX_QUERY.replace("$1", id).replace("$2", "");

    client.searchGetId(CAT_INDEX_NAME, checkQuery, checkRes -> {
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
            responseJson.put(STATUS, SUCCESS).put(RESULTS, new JsonArray()
                .add(new JsonObject().put(ID, id).put(METHOD, DELETE).put(STATUS, SUCCESS)));
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

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService getItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    String itemId = request.getString(ID);
    String getQuery = TERM_COMPLEX_QUERY.replace("$1", itemId).replace("$2", "");

    client.searchAsync(CAT_INDEX_NAME, getQuery, clientHandler -> {
      if (clientHandler.succeeded()) {
        LOGGER.info("Success: Retreived item");
        JsonObject responseJson = clientHandler.result();
        handler.handle(Future.succeededFuture(responseJson));
      } else {
        LOGGER.error("Fail: Failed getting item");
        /* Handle request error */
        handler.handle(Future.failedFuture(new JsonObject().put(STATUS, FAILED).toString()));
      }
    });
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listItems(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    LOGGER.debug("Info: Reached list items;" + request.toString());
    String itemType = request.getString(ITEM_TYPE);
    String type = request.getString(TYPE_KEY);
    String instanceID = request.getString(INSTANCE);
    String req = "";


    if (itemType.equalsIgnoreCase(TAGS)) {
      if (instanceID == null || instanceID == "") {
        req = LIST_TAGS_QUERY;
      } else {
        req = LIST_INSTANCE_TAGS_QUERY.replace("$1", instanceID);
      }
    } else {
      if (instanceID == null || instanceID == "") {
        req = LIST_TYPES_QUERY.replace("$1", type);
      } else {
        req = LIST_INSTANCE_TYPES_QUERY.replace("$1", type).replace("$2", instanceID);
      }
    }
    LOGGER.debug("Info: Listing items;" + req);
    client.listAggregationAsync(CAT_INDEX_NAME, req, clientHandler -> {
      if (clientHandler.succeeded()) {
        LOGGER.info("Success: List request");
        JsonObject responseJson = clientHandler.result();
        handler.handle(Future.succeededFuture(responseJson));
      } else {
        LOGGER.error("Fail: DB request has failed");
        /* Handle request error */
        handler.handle(Future.failedFuture(new JsonObject().put(STATUS, FAILED).toString()));
      }
    });
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listRelationship(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    /* <resourceGroupId>/resource */
    /* Initialize JsonObjects & JsonArrays */
    JsonObject errorJson = new JsonObject();

    String elasticQuery = queryDecoder.listRelationshipQuery(request);

    LOGGER.debug("Info: Query constructed;" + elasticQuery);

    client.searchAsync(REL_API_INDEX_NAME, elasticQuery, searchRes -> {
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
  public DatabaseService relSearch(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    /* Initialize elastic clients and JsonObjects */
    JsonObject errorJson = new JsonObject();
    JsonObject boolObject = new JsonObject();
    String subQuery = null;

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

        subQuery = TERM_QUERY.replace("$1", TYPE_KEYWORD)
                             .replace("$2", typeValue)
                             + "," + 
                   MATCH_QUERY.replace("$1", relReqsKey)
                              .replace("$2", relReqsValue);

      } else {
        handler.handle(Future.failedFuture(errorJson.toString()));
        return null;
      }

      JsonObject elasticQuery =
          new JsonObject(BOOL_MUST_QUERY.replace("$1", subQuery)).put(SOURCE, ID);

      /* Initial db query to filter matching attributes */
      client.searchAsync(CAT_INDEX_NAME, elasticQuery.toString(), searchRes -> {
        if (searchRes.succeeded()) {

          JsonArray resultValues = searchRes.result().getJsonArray(RESULTS);
          elasticQuery.clear();
          boolObject.clear();
          JsonArray idCollection = new JsonArray();

          /* iterating over the filtered response json array */
          if (!resultValues.isEmpty()) {

            for (Object idIndex : resultValues) {
              JsonObject id = (JsonObject) idIndex;
              if (!id.isEmpty()) {
                idCollection.add(new JsonObject().put(WILDCARD_KEY,
                    new JsonObject().put(ID_KEYWORD, id.getString(ID).concat("*"))));
              }
            }
          } else {
            handler.handle(Future.succeededFuture(searchRes.result()));
          }

          /* constructing the db query */
          boolObject.put(BOOL_KEY, new JsonObject().put(SHOULD_KEY, idCollection));
          elasticQuery.put(QUERY_KEY, boolObject);

          /* checking the requests for limit attribute */
          if (request.containsKey(LIMIT)) {
            Integer sizeFilter = request.getInteger(LIMIT);
            elasticQuery.put(SIZE_KEY, sizeFilter);
          }

          /* checking the requests for offset attribute */
          if (request.containsKey(OFFSET)) {
            Integer offsetFilter = request.getInteger(OFFSET);
            elasticQuery.put(FROM, offsetFilter);
          }

          LOGGER.debug("INFO: Query constructed;" + elasticQuery.toString());

          /* db query to find the relationship to the initial query */
          client.searchAsync(CAT_INDEX_NAME, elasticQuery.toString(), relSearchRes -> {
            if (relSearchRes.succeeded()) {

              LOGGER.debug("Success: Successful DB request");
              handler.handle(Future.succeededFuture(relSearchRes.result()));
            } else if (relSearchRes.failed()) {
              handler.handle(Future.failedFuture(relSearchRes.cause().getMessage()));
            }
          });
        } else {
          handler.handle(Future.failedFuture(searchRes.cause().getMessage()));
        }
      });
    }
    return this;
  }
}
