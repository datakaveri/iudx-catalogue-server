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

    ErrorRespBuilder errorRespBuilder = new ErrorRespBuilder();
    request.put(SEARCH, true);

    /* Validate the Request */
    if (!request.containsKey(SEARCH_TYPE)) {
      handler.handle(Future.failedFuture(errorRespBuilder.withStatus(FAILED)
          .withDescription(NO_SEARCH_TYPE_FOUND).getResponse()));
      return null;
    }

    /* Construct the query to be made */
    JsonObject query = queryDecoder.searchQuery(request);
    if (query.containsKey(ERROR)) {

      LOGGER.error("Fail: Query returned with an error");
      handler.handle(Future.failedFuture(errorRespBuilder.withStatus(FAILED)
          .withDescription(query.getString(ERROR)).getResponse()));
      return null;
    }

    LOGGER.debug("Info: Query constructed;" + query.toString());

    client.searchAsync(CAT_INDEX_NAME, query.toString(), searchRes -> {
      if (searchRes.succeeded()) {
        LOGGER.debug("Success: Successful DB request");
        handler.handle(Future.succeededFuture(searchRes.result()));
      } else {
        LOGGER.error("Fail: DB Request;" + searchRes.cause().getMessage());
        handler.handle(Future.failedFuture(errorRespBuilder.withStatus(FAILED)
            .withDescription(DATABASE_ERROR).getResponse()));
      }
    });
    return this;
  }

  @Override
  public DatabaseService countQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    ErrorRespBuilder errorRespBuilder = new ErrorRespBuilder();
    request.put(SEARCH, false);

    /* Validate the Request */
    if (!request.containsKey(SEARCH_TYPE)) {
      handler.handle(Future.failedFuture(errorRespBuilder.withStatus(FAILED)
          .withDescription(NO_SEARCH_TYPE_FOUND).getResponse()));
      return null;
    }

    /* Construct the query to be made */
    JsonObject query = queryDecoder.searchQuery(request);
    if (query.containsKey(ERROR)) {

      LOGGER.error("Fail: Query returned with an error");

      handler.handle(Future.failedFuture(errorRespBuilder.withStatus(FAILED)
          .withDescription(query.getString(ERROR)).getResponse()));
      return null;
    }

    LOGGER.debug("Info: Query constructed;" + query.toString());

    client.countAsync(CAT_INDEX_NAME, query.toString(), searchRes -> {
      if (searchRes.succeeded()) {
        LOGGER.debug("Success: Successful DB request");
        handler.handle(Future.succeededFuture(searchRes.result()));
      } else {
        LOGGER.error("Fail: DB Request;" + searchRes.cause().getMessage());
        handler.handle(Future.failedFuture(errorRespBuilder.withStatus(FAILED)
            .withDescription(DATABASE_ERROR).getResponse()));
      }
    });
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService createItem(JsonObject doc, Handler<AsyncResult<JsonObject>> handler) {

    ErrorRespBuilder errorRespBuilder = new ErrorRespBuilder();
    String id = doc.getString("id");
    String instanceId = doc.getString("instance");

    String errorJson =
        errorRespBuilder.withStatus(FAILED).withResult(id, INSERT, FAILED).getResponse();

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
        LOGGER.error("Fail: Isertion failed;" + checkRes.cause());
        handler.handle(Future.failedFuture(errorJson));
      }
      if (checkRes.succeeded()) {
        if (checkRes.result().getInteger(TOTAL_HITS) != 0) {
          handler.handle(Future.failedFuture(
              errorRespBuilder.withStatus(ERROR).withResult(id, INSERT, FAILED)
                  .withDescription("Fail: Doc Exists").getResponse()));
          return;
        }
        if (isInstanceValid.value == false) {
          handler.handle(Future.failedFuture(errorJson));
          LOGGER.error("Fail: Invalid Instance Insertion failed");
          return;
        }
        /* Insert document */
        client.docPostAsync(CAT_INDEX_NAME, doc.toString(), postRes -> {
          if (postRes.succeeded()) {
            handler.handle(Future.succeededFuture(
                errorRespBuilder.withStatus(SUCCESS).withResult(id, INSERT, SUCCESS)
                    .getJsonResponse()));
          } else {
            handler.handle(Future.failedFuture(errorJson));
            LOGGER.error("Fail: Insertion failed;" + postRes.cause());
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
  public DatabaseService updateItem(JsonObject doc, Handler<AsyncResult<JsonObject>> handler) {

    ErrorRespBuilder errorRespBuilder = new ErrorRespBuilder();
    String id = doc.getString("id");
    String checkQuery = TERM_COMPLEX_QUERY.replace("$1", id).replace("$2", "\"" + id + "\"");

    String errorJson =
        errorRespBuilder.withStatus(FAILED).withResult(id, UPDATE, FAILED).getResponse();

    client.searchGetId(CAT_INDEX_NAME, checkQuery, checkRes -> {
      if (checkRes.failed()) {
        LOGGER.error("Fail: Check query fail;" + checkRes.cause());
        handler.handle(Future.failedFuture(errorJson));
        return;
      }
      if (checkRes.succeeded()) {
        if (checkRes.result().getInteger(TOTAL_HITS) != 1) {
          LOGGER.error("Fail: Doc doesn't exist, can't update");
          handler.handle(Future.failedFuture(errorJson));
          return;
        }
        String docId = checkRes.result().getJsonArray(RESULTS).getString(0);
        client.docPutAsync(CAT_INDEX_NAME, docId, doc.toString(), putRes -> {
          if (putRes.succeeded()) {
            handler.handle(Future.succeededFuture(
                errorRespBuilder.withStatus(SUCCESS).withResult(id, UPDATE, SUCCESS)
                    .getJsonResponse()));
          } else {
            handler.handle(Future.failedFuture(errorJson));
            LOGGER.error("Fail: Updation failed;" + putRes.cause());
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

    LOGGER.debug("Info: Updating item");

    ErrorRespBuilder errorRespBuilder = new ErrorRespBuilder();
    String id = request.getString("id");
    String errorJson =
        errorRespBuilder.withStatus(FAILED).withResult(id, DELETE, FAILED).getResponse();

    String checkQuery = TERM_COMPLEX_QUERY.replace("$1", id).replace("$2", "");

    client.searchGetId(CAT_INDEX_NAME, checkQuery, checkRes -> {
      if (checkRes.failed()) {
        LOGGER.error("Fail: Check query fail;" + checkRes.cause());
        handler.handle(Future.failedFuture(errorJson));
      }

      if (checkRes.succeeded()) {
        LOGGER.debug("Success: Check index for doc");
        if (checkRes.result().getInteger(TOTAL_HITS) != 1) {
          LOGGER.error("Fail: Doc doesn't exist, can't delete;");
          handler.handle(Future.failedFuture(errorJson));
          return;
        }

        String docId = checkRes.result().getJsonArray(RESULTS).getString(0);
        client.docDelAsync(CAT_INDEX_NAME, docId, delRes -> {
          if (delRes.succeeded()) {
            handler.handle(Future.succeededFuture(
                errorRespBuilder.withStatus(SUCCESS).withResult(id, DELETE, SUCCESS)
                    .getJsonResponse()));
          } else {
            handler.handle(Future.failedFuture(errorJson));
            LOGGER.error("Fail: Deletion failed;" + delRes.cause());
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

    LOGGER.debug("Info: Get item");

    ErrorRespBuilder errorRespBuilder = new ErrorRespBuilder();
    String itemId = request.getString(ID);
    String getQuery = TERM_COMPLEX_QUERY.replace("$1", itemId).replace("$2", "");

    client.searchAsync(CAT_INDEX_NAME, getQuery, clientHandler -> {
      if (clientHandler.succeeded()) {
        LOGGER.debug("Success: Successful DB request");
        JsonObject responseJson = clientHandler.result();
        handler.handle(Future.succeededFuture(responseJson));
      } else {
        LOGGER.error("Fail: Failed getting item;" + clientHandler.cause());
        /* Handle request error */
        handler.handle(
            Future.failedFuture(
                errorRespBuilder.withStatus(FAILED).withDescription(ERROR_DB_REQUEST)
                    .getResponse()));
      }
    });
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listItems(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    ErrorRespBuilder errorRespBuilder = new ErrorRespBuilder();
    String elasticQuery = queryDecoder.listItemQuery(request);

    LOGGER.debug("Info: Listing items;" + elasticQuery);

    client.listAggregationAsync(CAT_INDEX_NAME, elasticQuery, clientHandler -> {
      if (clientHandler.succeeded()) {
        LOGGER.debug("Success: Successful DB request");
        JsonObject responseJson = clientHandler.result();
        handler.handle(Future.succeededFuture(responseJson));
      } else {
        LOGGER.error("Fail: DB request has failed;" + clientHandler.cause());
        /* Handle request error */
        handler.handle(
            Future.failedFuture(errorRespBuilder.withStatus(FAILED)
                .withDescription(ERROR_DB_REQUEST).getResponse()));
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

    ErrorRespBuilder errorRespBuilder = new ErrorRespBuilder();
    String elasticQuery = queryDecoder.listRelationshipQuery(request);

    LOGGER.debug("Info: Query constructed;" + elasticQuery);

    client.searchAsync(CAT_INDEX_NAME, elasticQuery, searchRes -> {
      if (searchRes.succeeded()) {
        LOGGER.debug("Success: Successful DB request");
        handler.handle(Future.succeededFuture(searchRes.result()));
      } else {
        LOGGER.error("Fail: DB request has failed;" + searchRes.cause());
        /* Handle request error */
        handler.handle(
            Future.failedFuture(errorRespBuilder.withStatus(FAILED)
                .withDescription(ERROR_DB_REQUEST).getResponse()));
      }
    });
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService relSearch(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    ErrorRespBuilder errorRespBuilder = new ErrorRespBuilder();
    String subQuery = "";
    String errorJson =
        errorRespBuilder.withStatus(FAILED).withDescription(ERROR_INVALID_PARAMETER).getResponse();

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
          LOGGER.error("Fail: Incorrect/missing query parameters");
          handler.handle(Future.failedFuture(errorJson));
          return null;
        }

        subQuery = TERM_QUERY.replace("$1", TYPE_KEYWORD)
                             .replace("$2", typeValue)
                             + "," + 
                   MATCH_QUERY.replace("$1", relReqsKey)
                              .replace("$2", relReqsValue);
      } else {
        LOGGER.error("Fail: Incorrect/missing query parameters");
        handler.handle(Future.failedFuture(errorJson));
        return null;
      }

      JsonObject elasticQuery =
          new JsonObject(BOOL_MUST_QUERY.replace("$1", subQuery)).put(SOURCE, ID);

      /* Initial db query to filter matching attributes */
      client.searchAsync(CAT_INDEX_NAME, elasticQuery.toString(), searchRes -> {
        if (searchRes.succeeded()) {

          JsonArray resultValues = searchRes.result().getJsonArray(RESULTS);
          elasticQuery.clear();
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

          elasticQuery.put(QUERY_KEY,
              new JsonObject(SHOULD_QUERY.replace("$1", idCollection.toString())));

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
              LOGGER.error("Fail: DB request has failed;" + relSearchRes.cause());
              handler.handle(Future.failedFuture(errorRespBuilder.withStatus(FAILED)
                  .withDescription(ERROR_DB_REQUEST).getResponse()));
            }
          });
        } else {
          LOGGER.error("Fail: DB request has failed;" + searchRes.cause());
          handler.handle(Future.failedFuture(
              errorRespBuilder.withStatus(FAILED).withDescription(ERROR_DB_REQUEST).getResponse()));
        }
      });
    }
    return this;
  }

  /**
   * ErrorRespBuilder Response Message builder for search APIs
   */
  private class ErrorRespBuilder {
    private JsonObject response = new JsonObject();

    public ErrorRespBuilder withStatus(String status) {
      response.put(STATUS, status);
      return this;
    }

    public ErrorRespBuilder withDescription(String description) {
      response.put(DESCRIPTION, description);
      return this;
    }

    public ErrorRespBuilder withResult(String id, String method, String status) {
      JsonObject resultAttrs = new JsonObject().put(ID, id).put(METHOD, method).put(STATUS, status);
      response.put(RESULTS, new JsonArray().add(resultAttrs));
      return this;
    }

    public ErrorRespBuilder withResult() {
      response.put(RESULTS, new JsonArray());
      return this;
    }

    public JsonObject getJsonResponse() {
      return response;
    }

    public String getResponse() {
      return response.toString();
    }
  }
}
