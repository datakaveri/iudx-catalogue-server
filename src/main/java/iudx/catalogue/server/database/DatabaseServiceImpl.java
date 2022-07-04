package iudx.catalogue.server.database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.sf.saxon.trans.SymbolicName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Timer;
import java.util.TimerTask;
import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.database.Constants.*;
import iudx.catalogue.server.nlpsearch.NLPSearchService;
import iudx.catalogue.server.geocoding.GeocodingService;


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
  private ElasticClient client;
  private final QueryDecoder queryDecoder = new QueryDecoder();
  private NLPSearchService nlpService;
  private GeocodingService geoService;
  private boolean nlpPluggedIn;
  private boolean geoPluggedIn;

  private String ratingIndex;

  private static String INTERNAL_ERROR_RESP = new RespBuilder()
                                          .withType(TYPE_INTERNAL_SERVER_ERROR)
                                          .withTitle(TITLE_INTERNAL_SERVER_ERROR)
                                          .getResponse();

  public DatabaseServiceImpl(ElasticClient client, String ratingIndex) {
    this(client);
    this.ratingIndex = ratingIndex;
  }

  public DatabaseServiceImpl(
      ElasticClient client,
      String ratingIndex,
      NLPSearchService nlpService,
      GeocodingService geoService) {
    this(client, nlpService, geoService);
    this.ratingIndex = ratingIndex;
  }

  public DatabaseServiceImpl(ElasticClient client) {
    this.client = client;
    nlpPluggedIn = false;
    geoPluggedIn = false;
  }

  public DatabaseServiceImpl(
      ElasticClient client, NLPSearchService nlpService, GeocodingService geoService) {
    this.client = client;
    this.nlpService = nlpService;
    this.geoService = geoService;
    nlpPluggedIn = true;
    geoPluggedIn = true;
  }

  @Override
  public DatabaseService searchQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    LOGGER.debug("Info: searchQuery");

    RespBuilder respBuilder = new RespBuilder();
    request.put(SEARCH, true);

    /* Validate the Request */
    if (!request.containsKey(SEARCH_TYPE)) {
      handler.handle(Future.failedFuture(
            respBuilder.withType(TYPE_INVALID_SYNTAX)
                        .withTitle(TITLE_INVALID_SYNTAX)
                        .withDetail(NO_SEARCH_TYPE_FOUND)
                        .getResponse()));
      return null;
    }

    /* Construct the query to be made */
    JsonObject query = queryDecoder.searchQuery(request);
    if (query.containsKey(ERROR)) {

      LOGGER.error("Fail: Query returned with an error");
      handler.handle(Future.failedFuture(query.getJsonObject(ERROR).toString()));
      return null;
    }

    LOGGER.debug("Info: Query constructed;" + query.toString());

    client.searchAsync(query.toString(), searchRes -> {
      if (searchRes.succeeded()) {
        LOGGER.debug("Success: Successful DB request");
        handler.handle(Future.succeededFuture(searchRes.result()));
      } else {
        LOGGER.error("Fail: DB Request;" + searchRes.cause().getMessage());
        handler.handle(Future.failedFuture(INTERNAL_ERROR_RESP));
      }
    });
    return this;
  }

  public DatabaseService nlpSearchQuery(JsonArray request, Handler<AsyncResult<JsonObject>> handler) {
    JsonArray embeddings = request.getJsonArray(0);
    client.scriptSearch(embeddings, searchRes -> {
      if(searchRes.succeeded()) {
        LOGGER.debug("Success:Successful DB request");
        handler.handle(Future.succeededFuture(searchRes.result()));
      } else {
        LOGGER.error("Fail: DB request;" + searchRes.cause().getMessage());
        handler.handle(Future.failedFuture(INTERNAL_ERROR_RESP));
      }
    });
    return this;
  }

  public DatabaseService nlpSearchLocationQuery(JsonArray request,
                                                String location,
                                                Handler<AsyncResult<JsonObject>> handler) {
    JsonArray embeddings = request.getJsonArray(0);
    client
    .scriptLocationSearch(embeddings, location, searchRes -> {
      if(searchRes.succeeded()) {
        LOGGER.debug("Success:Successful DB request");
        handler.handle(Future.succeededFuture(searchRes.result()));
      } else {
        LOGGER.error("Fail: DB request;" + searchRes.cause().getMessage());
        handler.handle(Future.failedFuture(INTERNAL_ERROR_RESP));
      }
    });

    return this;
  }

  @Override
  public DatabaseService countQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    request.put(SEARCH, false);

    /* Validate the Request */
    if (!request.containsKey(SEARCH_TYPE)) {
      handler.handle(Future.failedFuture(INTERNAL_ERROR_RESP));
      return null;
    }

    /* Construct the query to be made */
    JsonObject query = queryDecoder.searchQuery(request);
    if (query.containsKey(ERROR)) {

      LOGGER.error("Fail: Query returned with an error");

      handler.handle(Future.failedFuture(query.getJsonObject(ERROR).toString()));
      return null;
    }

    LOGGER.debug("Info: Query constructed;" + query.toString());

    client.countAsync(query.toString(), searchRes -> {
      if (searchRes.succeeded()) {
        LOGGER.debug("Success: Successful DB request");
        handler.handle(Future.succeededFuture(searchRes.result()));
      } else {
        LOGGER.error("Fail: DB Request;" + searchRes.cause().getMessage());
        handler.handle(Future.failedFuture(INTERNAL_ERROR_RESP));
      }
    });
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService createItem(JsonObject doc, Handler<AsyncResult<JsonObject>> handler) {

    RespBuilder respBuilder = new RespBuilder();
    String id = doc.getString("id");
    final String instanceId = doc.getString(INSTANCE);

    String errorJson = respBuilder.withType(FAILED).withResult(id, INSERT, FAILED).getResponse();

    String checkItem = GET_DOC_QUERY.replace("$1", id).replace("$2", "");

    verifyInstance(instanceId).onComplete(instanceHandler -> {
      if (instanceHandler.succeeded()) {
        LOGGER.debug("Info: Instance info;" + instanceHandler.result());

        client.searchAsync(checkItem.toString(), checkRes -> {
          if (checkRes.failed()) {
            LOGGER.error("Fail: Isertion failed;" + checkRes.cause());
            handler.handle(Future.failedFuture(errorJson));
          }
          if (checkRes.succeeded()) {
            if (checkRes.result().getInteger(TOTAL_HITS) != 0) {
              handler.handle(Future.failedFuture(
                  respBuilder.withType(TYPE_ALREADY_EXISTS)
                             .withTitle(TITLE_ALREADY_EXISTS)
                             .withResult(id, INSERT, FAILED, "Fail: Doc Exists")
                             .getResponse()));
              return;
            }

            doc.put(SUMMARY_KEY, Summarizer.summarize(doc));

            /* If geo and nlp services are initialized */
            if (geoPluggedIn && nlpPluggedIn) {
              geoService.geoSummarize(doc, geoHandler -> {
                /* Not going to check if success or fail */
                doc.put(GEOSUMMARY_KEY, geoHandler.result());
                nlpService.getEmbedding(doc, ar-> {
                  if(ar.succeeded()) {
                    LOGGER.debug("Info: Document embeddings created");
                    doc.put(WORD_VECTOR_KEY, ar.result().getJsonArray("result"));
                    /* Insert document */
                    client.docPostAsync(doc.toString(), postRes -> {
                      if (postRes.succeeded()) {
                        handler.handle(Future.succeededFuture(
                              respBuilder.withType(TYPE_SUCCESS)
                              .withTitle(TITLE_SUCCESS)
                              .withResult(id, INSERT, TYPE_SUCCESS)
                              .getJsonResponse()));
                      } else {
                        handler.handle(Future.failedFuture(errorJson));
                        LOGGER.error("Fail: Insertion failed" + postRes.cause());
                      }
                    });
                  } else {
                    LOGGER.error("Error: Document embeddings not created");
                  }
                });
              });
            } else {
              /* Insert document */
              new Timer().schedule(new TimerTask() {
                public void run() {
                  client.docPostAsync(doc.toString(), postRes -> {
                    if (postRes.succeeded()) {
                      handler.handle(Future.succeededFuture(
                          respBuilder.withType(TYPE_SUCCESS)
                                     .withResult(id, INSERT, TYPE_SUCCESS)
                                     .getJsonResponse()));
                    } else {
                      handler.handle(Future.failedFuture(errorJson));
                      LOGGER.error("Fail: Insertion failed" + postRes.cause());
                    }
                  });
                }
              }, STATIC_DELAY_TIME);
            }
          }
        });
      } else if (instanceHandler.failed()) {
        handler.handle(Future.failedFuture(
            respBuilder.withType(TYPE_OPERATION_NOT_ALLOWED)
                        .withTitle(TITLE_OPERATION_NOT_ALLOWED)
                       .withResult(id, INSERT, FAILED, instanceHandler.cause().getLocalizedMessage())
                       .getResponse()));
      }
    });
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService updateItem(JsonObject doc, Handler<AsyncResult<JsonObject>> handler) {

    RespBuilder respBuilder = new RespBuilder();
    String id = doc.getString("id");
    String checkQuery = GET_DOC_QUERY.replace("$1", id).replace("$2", "\"" + id + "\"");


    new Timer().schedule(new TimerTask() {
      public void run() {
        client.searchGetId(checkQuery, checkRes -> {
          if (checkRes.failed()) {
            LOGGER.error("Fail: Check query fail;" + checkRes.cause());
            handler.handle(Future.failedFuture(INTERNAL_ERROR_RESP));
            return;
          }
          if (checkRes.succeeded()) {
            if (checkRes.result().getInteger(TOTAL_HITS) != 1) {
              LOGGER.error("Fail: Doc doesn't exist, can't update");
              handler.handle(Future.failedFuture(
                    respBuilder.withType(TYPE_ITEM_NOT_FOUND)
                                .withTitle(TITLE_ITEM_NOT_FOUND)
                                .withResult(id, UPDATE, FAILED, "Fail: Doc doesn't exist, can't update")
                  .getResponse()));
              return;
            }
            String docId = checkRes.result().getJsonArray(RESULTS).getString(0);
            client.docPutAsync(docId, doc.toString(), putRes -> {
              if (putRes.succeeded()) {
                handler.handle(Future.succeededFuture(
                      respBuilder.withType(TYPE_SUCCESS)
                                  .withTitle(TYPE_SUCCESS)
                    .withResult(id, UPDATE, TYPE_SUCCESS).getJsonResponse()));
              } else {
                handler.handle(Future.failedFuture(INTERNAL_ERROR_RESP));
                LOGGER.error("Fail: Updation failed;" + putRes.cause());
              }
            });
          }
        });
      }
    }, STATIC_DELAY_TIME);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService deleteItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    LOGGER.debug("Info: Deleting item");

    RespBuilder respBuilder = new RespBuilder();
    String id = request.getString("id");

    new Timer().schedule(new TimerTask() {
      public void run() {
        String checkQuery = "";
        var isParent = new Object() {
          boolean value = false;
        };

        if (id.split("/").length < 5) {
          isParent.value = true;
          checkQuery = QUERY_RESOURCE_GRP.replace("$1", id).replace("$2", id);
        } else {
          checkQuery = GET_DOC_QUERY.replace("$1", id).replace("$2", "");
        }

        client.searchGetId(checkQuery, checkRes -> {
          if (checkRes.failed()) {
            LOGGER.error("Fail: Check query fail;" + checkRes.cause().getMessage());
            handler.handle(Future.failedFuture(INTERNAL_ERROR_RESP));
          }

          if (checkRes.succeeded()) {
            LOGGER.debug("Success: Check index for doc");
            if (checkRes.result().getInteger(TOTAL_HITS) > 1 && isParent.value == true) {
              LOGGER.error("Fail: Can't delete, parent doc has associated item;");
              handler
                  .handle(Future.failedFuture(
                    respBuilder.withType(TYPE_OPERATION_NOT_ALLOWED)
                                .withTitle(TITLE_OPERATION_NOT_ALLOWED)
                                .withResult(id, DELETE, FAILED,
                          "Fail: Can't delete, resourceGroup has associated item")
                      .getResponse()));
              return;
            } else if (checkRes.result().getInteger(TOTAL_HITS) != 1) {
              LOGGER.error("Fail: Doc doesn't exist, can't delete;");
              handler.handle(Future.failedFuture(
                    respBuilder.withType(TYPE_ITEM_NOT_FOUND)
                                .withTitle(TITLE_ITEM_NOT_FOUND)
                                .withResult(id, DELETE, FAILED, "Fail: Doc doesn't exist, can't delete")
                  .getResponse()));
              return;
            }
          }

          String docId = checkRes.result().getJsonArray(RESULTS).getString(0);
          client.docDelAsync(docId, delRes -> {
            if (delRes.succeeded()) {
              handler.handle(Future.succeededFuture(
                    respBuilder.withType(TYPE_SUCCESS)
                                .withTitle(TITLE_SUCCESS)
                  .withResult(id, DELETE, TYPE_SUCCESS).getJsonResponse()));
            } else {
              handler.handle(Future.failedFuture(INTERNAL_ERROR_RESP));
              LOGGER.error("Fail: Deletion failed;" + delRes.cause().getMessage());
            }
          });
        });
      }
    }, STATIC_DELAY_TIME);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService getItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    LOGGER.debug("Info: Get item");

    RespBuilder respBuilder = new RespBuilder();
    String itemId = request.getString(ID);
    String getQuery = GET_DOC_QUERY.replace("$1", itemId).replace("$2", "");

    client.searchAsync(getQuery, clientHandler -> {
      if (clientHandler.succeeded()) {
        LOGGER.debug("Success: Successful DB request");
        JsonObject responseJson = clientHandler.result();
        handler.handle(Future.succeededFuture(responseJson));
      } else {
        LOGGER.error("Fail: Failed getting item;" + clientHandler.cause());
        /* Handle request error */
        handler.handle(
            Future.failedFuture(respBuilder.withType(TYPE_INTERNAL_SERVER_ERROR)
                                            .withTitle(TITLE_INTERNAL_SERVER_ERROR)
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

    RespBuilder respBuilder = new RespBuilder();
    String elasticQuery = queryDecoder.listItemQuery(request);

    LOGGER.debug("Info: Listing items;" + elasticQuery);

    client.listAggregationAsync(elasticQuery, clientHandler -> {
      if (clientHandler.succeeded()) {
        LOGGER.debug("Success: Successful DB request");
        JsonObject responseJson = clientHandler.result();
        handler.handle(Future.succeededFuture(responseJson));
      } else {
        LOGGER.error("Fail: DB request has failed;" + clientHandler.cause());
        /* Handle request error */
        handler.handle(
            Future.failedFuture(respBuilder.withType(TYPE_INTERNAL_SERVER_ERROR)
                                            .withTitle(TITLE_INTERNAL_SERVER_ERROR)
                                            .getResponse()));
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

    RespBuilder respBuilder = new RespBuilder();
    String elasticQuery = queryDecoder.listRelationshipQuery(request);

    LOGGER.debug("Info: Query constructed;" + elasticQuery);

    client.searchAsync(elasticQuery, searchRes -> {
      if (searchRes.succeeded()) {
        LOGGER.debug("Success: Successful DB request");
        handler.handle(Future.succeededFuture(searchRes.result()));
      } else {
        LOGGER.error("Fail: DB request has failed;" + searchRes.cause());
        /* Handle request error */
        handler.handle(
            Future.failedFuture(respBuilder.withType(TYPE_INTERNAL_SERVER_ERROR)
                                            .withDetail(TITLE_INTERNAL_SERVER_ERROR)
                                            .getResponse()));
      }
    });
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService relSearch(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    RespBuilder respBuilder = new RespBuilder();
    String subQuery = "";
    String errorJson = respBuilder.withType(FAILED)
                                  .withDetail(ERROR_INVALID_PARAMETER)
                                  .getResponse();

    /* Validating the request */
    if (request.containsKey(RELATIONSHIP) && request.containsKey(VALUE)) {

      /* parsing data parameters from the request */
      String relReq = request.getJsonArray(RELATIONSHIP).getString(0);

      if (relReq.contains(".")) {

        LOGGER.debug("Info: Reached relationship search dbServiceImpl");

        String typeValue = null;
        String[] relReqs = relReq.split("\\.", 2);
        String relReqsKey = relReqs[1];
        String relReqsValue = request.getJsonArray(VALUE)
                                      .getJsonArray(0).getString(0);

        if (relReqs[0].equalsIgnoreCase(PROVIDER)) {
          typeValue = ITEM_TYPE_PROVIDER;

        } else if (relReqs[0].equalsIgnoreCase(RESOURCE)) {
          typeValue = ITEM_TYPE_RESOURCE;

        } else if (relReqs[0].equalsIgnoreCase(RESOURCE_GRP)) {
          typeValue = ITEM_TYPE_RESOURCE_GROUP;

        } else if (relReqs[0].equalsIgnoreCase(RESOURCE_SVR)) {
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
      client.searchAsync(elasticQuery.toString(), searchRes -> {
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
                                    new JsonObject().put(ID_KEYWORD,
                                                          id.getString(ID) + "*")));
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
          client.searchAsync(elasticQuery.toString(), relSearchRes -> {
            if (relSearchRes.succeeded()) {

              LOGGER.debug("Success: Successful DB request");
              handler.handle(Future.succeededFuture(relSearchRes.result()));
            } else if (relSearchRes.failed()) {
              LOGGER.error("Fail: DB request has failed;" + relSearchRes.cause().getMessage());
              handler.handle(Future.failedFuture(INTERNAL_ERROR_RESP));
            }
          });
        } else {
          LOGGER.error("Fail: DB request has failed;" + searchRes.cause().getMessage());
          handler.handle(Future.failedFuture(INTERNAL_ERROR_RESP));
        }
      });
    }
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public DatabaseService createRating(
      JsonObject ratingDoc, Handler<AsyncResult<JsonObject>> handler) {
    RespBuilder respBuilder = new RespBuilder();
    String ratingId = ratingDoc.getString("ratingID");

    String checkForExistingRecord = GET_RDOC_QUERY.replace("$1", ratingId).replace("$2", "");

    client.searchAsync(
        checkForExistingRecord,
        ratingIndex,
        checkRes -> {
          if (checkRes.failed()) {
            LOGGER.error("Fail: Insertion of rating failed: " + checkRes.cause());
            handler.handle(
                Future.failedFuture(
                    respBuilder
                        .withType(FAILED)
                        .withResult(ratingId, INSERT, FAILED)
                        .getResponse()));
          } else {
            if (checkRes.result().getInteger(TOTAL_HITS) != 0) {
              handler.handle(
                  Future.failedFuture(
                      respBuilder
                          .withType(TYPE_ALREADY_EXISTS)
                          .withTitle(TITLE_ALREADY_EXISTS)
                          .withResult(ratingId, INSERT, FAILED, " Fail: Doc Already Exists")
                          .getResponse()));
              return;
            }

            client.docPostAsync(
                ratingDoc.toString(),
                ratingIndex,
                postRes -> {
                  if (postRes.succeeded()) {
                    handler.handle(
                        Future.succeededFuture(
                            respBuilder
                                .withType(TYPE_SUCCESS)
                                .withTitle(TITLE_SUCCESS)
                                .withResult(ratingId, INSERT, TYPE_SUCCESS)
                                .getJsonResponse()));
                  } else {
                    handler.handle(
                        Future.failedFuture(
                            respBuilder
                                .withType(TYPE_FAIL)
                                .withResult(ratingId, INSERT, FAILED)
                                .getResponse()));
                    LOGGER.error("Fail: Insertion failed" + postRes.cause());
                  }
                });
          }
        });
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public DatabaseService updateRating(
      JsonObject ratingDoc, Handler<AsyncResult<JsonObject>> handler) {
    RespBuilder respBuilder = new RespBuilder();
    String ratingId = ratingDoc.getString("ratingID");

    String checkForExistingRecord = GET_RDOC_QUERY.replace("$1", ratingId).replace("$2", "");
    LOGGER.debug(checkForExistingRecord);
    client.searchGetId(
        checkForExistingRecord,
        ratingIndex,
        checkRes -> {
          if (checkRes.failed()) {
            LOGGER.error("Fail: Check query fail;" + checkRes.cause());
            handler.handle(Future.failedFuture(INTERNAL_ERROR_RESP));
          } else {
            if (checkRes.result().getInteger(TOTAL_HITS) != 1) {
              LOGGER.error("Fail: Doc doesn't exist, can't update");
              handler.handle(
                  Future.failedFuture(
                      respBuilder
                          .withType(TYPE_ITEM_NOT_FOUND)
                          .withTitle(TITLE_ITEM_NOT_FOUND)
                          .withResult(
                              ratingId, UPDATE, FAILED, "Fail: Doc doesn't exist, can't update")
                          .getResponse()));
              return;
            }

            String docId = checkRes.result().getJsonArray(RESULTS).getString(0);

            client.docPutAsync(
                docId,
                ratingDoc.toString(),
                ratingIndex,
                putRes -> {
                  if (putRes.succeeded()) {
                    handler.handle(
                        Future.succeededFuture(
                            respBuilder
                                .withType(TYPE_SUCCESS)
                                .withTitle(TITLE_SUCCESS)
                                .withResult(ratingId, UPDATE, TYPE_SUCCESS)
                                .getJsonResponse()));
                  } else {
                    handler.handle(Future.failedFuture(INTERNAL_ERROR_RESP));
                    LOGGER.error("Fail: Updation failed;" + putRes.cause());
                  }
                });
          }
        });
    return this;
  }

  @Override
  public DatabaseService deleteRating(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    RespBuilder respBuilder = new RespBuilder();
    String ratingId = request.getString("ratingID");

    String checkForExistingRecord = GET_RDOC_QUERY.replace("$1", ratingId).replace("$2", "");
    LOGGER.debug(checkForExistingRecord);
    client.searchGetId(
        checkForExistingRecord,
        ratingIndex,
        checkRes -> {
          if (checkRes.failed()) {
            LOGGER.error("Fail: Check query fail;" + checkRes.cause());
            handler.handle(Future.failedFuture(INTERNAL_ERROR_RESP));
          } else {
            if (checkRes.result().getInteger(TOTAL_HITS) != 1) {
              LOGGER.error("Fail: Doc doesn't exist, can't delete");
              handler.handle(
                  Future.failedFuture(
                      respBuilder
                          .withType(TYPE_ITEM_NOT_FOUND)
                          .withTitle(TITLE_ITEM_NOT_FOUND)
                          .withResult(
                              ratingId, DELETE, FAILED, "Fail: Doc doesn't exist, can't delete")
                          .getResponse()));
              return;
            }

            String docId = checkRes.result().getJsonArray(RESULTS).getString(0);

            client.docDelAsync(
                docId,
                ratingIndex,
                putRes -> {
                  if (putRes.succeeded()) {
                    handler.handle(
                        Future.succeededFuture(
                            respBuilder
                                .withType(TYPE_SUCCESS)
                                .withTitle(TITLE_SUCCESS)
                                .withResult(ratingId, DELETE, TYPE_SUCCESS)
                                .getJsonResponse()));
                  } else {
                    handler.handle(Future.failedFuture(INTERNAL_ERROR_RESP));
                    LOGGER.error("Fail: Deletion failed;" + putRes.cause());
                  }
                });
          }
        });
    return this;
  }

  @Override
  public DatabaseService getRatings(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    String query;
    if (request.containsKey("ratingID")) {
      String ratingID = request.getString("ratingID");
      query = GET_RATING_DOCS.replace("$1", "ratingID").replace("$2", ratingID);
    } else {
      String id = request.getString(ID);
      query = GET_RATING_DOCS.replace("$1", "id").replace("$2", id);
    }

    client.searchAsync(
        query,
        ratingIndex,
        getRes -> {
          if (getRes.succeeded()) {
            LOGGER.debug("Success: Successful DB request");
            JsonObject result = getRes.result();
            handler.handle(Future.succeededFuture(result));
          } else {
            LOGGER.error("Fail: failed getting rating: " + getRes.cause());
            handler.handle(Future.failedFuture(INTERNAL_ERROR_RESP));
          }
        });
    return this;
  }

  /* Verify the existance of an instance */
  Future<Boolean> verifyInstance(String instanceId) {

    Promise<Boolean> promise = Promise.promise();

    if (instanceId == null || instanceId.startsWith("\"") || instanceId.isBlank()) {
      LOGGER.debug("Info: InstanceID null. Maybe provider item");
      promise.complete(true);
      return promise.future();
    }

    String checkInstance = GET_DOC_QUERY.replace("$1", instanceId).replace("$2", "");
    client.searchAsync(checkInstance, checkRes -> {
      if (checkRes.failed()) {
        LOGGER.error(ERROR_DB_REQUEST + checkRes.cause().getMessage());
        promise.fail(TYPE_INTERNAL_SERVER_ERROR);
      } else if (checkRes.result().getInteger(TOTAL_HITS) == 0) {
        LOGGER.debug(INSTANCE_NOT_EXISTS);
        promise.fail("Fail: Instance doesn't exist/registered");
      } else {
        promise.complete(true);
      }
      return;
    });

    return promise.future();
  }
}
