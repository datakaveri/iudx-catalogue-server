package iudx.catalogue.server.database;

import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.mlayer.util.Constants.*;
import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.geocoding.GeocodingService;
import iudx.catalogue.server.nlpsearch.NLPSearchService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Database Service Implementation.
 *
 * <h1>Database Service Implementation</h1>
 *
 * <p>The Database Service implementation in the IUDX Catalogue Server implements the definitions of
 * the {@link iudx.catalogue.server.database.DatabaseService}.
 *
 * @version 1.0
 * @since 2020-05-31
 */
public class DatabaseServiceImpl implements DatabaseService {

  private static final Logger LOGGER = LogManager.getLogger(DatabaseServiceImpl.class);
  static ElasticClient client;
  private static String internalErrorResp =
      new RespBuilder()
          .withType(TYPE_INTERNAL_SERVER_ERROR)
          .withTitle(TITLE_INTERNAL_SERVER_ERROR)
          .withDetail(DETAIL_INTERNAL_SERVER_ERROR)
          .getResponse();
  private final QueryDecoder queryDecoder = new QueryDecoder();
  private NLPSearchService nlpService;
  private GeocodingService geoService;
  private boolean nlpPluggedIn;
  private boolean geoPluggedIn;
  private String docIndex;
  private String ratingIndex;
  private String mlayerInstanceIndex;
  private String mlayerDomainIndex;

  /**
   * Constructs a new DatabaseServiceImpl instance with the given ElasticClient and index names.
   *
   * @param client the ElasticClient used for accessing Elasticsearch
   * @param docIndex the name of the index used for document storage
   * @param ratingIndex the name of the index used for rating storage
   * @param mlayerInstanceIndex the name of the index used for ML layer instance storage
   * @param mlayerDomainIndex the name of the index used for ML layer domain storage
   */
  public DatabaseServiceImpl(
      ElasticClient client,
      String docIndex,
      String ratingIndex,
      String mlayerInstanceIndex,
      String mlayerDomainIndex) {
    this(client);
    this.docIndex = docIndex;
    this.ratingIndex = ratingIndex;
    this.mlayerInstanceIndex = mlayerInstanceIndex;
    this.mlayerDomainIndex = mlayerDomainIndex;
  }

  /**
   * client and index names for documents, ratings, mlayer instances, and mlayer domains.
   *
   * @param client the Elasticsearch client used to interact with the Elasticsearch cluster
   * @param docIndex the name of the Elasticsearch index used to store documents
   * @param ratingIndex the name of the Elasticsearch index used to store document ratings
   * @param mlayerInstanceIndex the name of the Elasticsearch index used to store mlayer instances
   * @param mlayerDomainIndex the name of the Elasticsearch index used to store mlayer domains
   * @param nlpService the NLP search service used to perform NLP searches
   * @param geoService the geocoding service used to perform geocoding operations
   */
  public DatabaseServiceImpl(
      ElasticClient client,
      String docIndex,
      String ratingIndex,
      String mlayerInstanceIndex,
      String mlayerDomainIndex,
      NLPSearchService nlpService,
      GeocodingService geoService) {
    this(client, nlpService, geoService);
    this.docIndex = docIndex;
    this.ratingIndex = ratingIndex;
    this.mlayerInstanceIndex = mlayerInstanceIndex;
    this.mlayerDomainIndex = mlayerDomainIndex;
  }

  /**
   * Constructs a new instance of DatabaseServiceImpl with only the ElasticClient provided. This
   * constructor sets the values of nlpPluggedIn and geoPluggedIn to false.
   *
   * @param client the ElasticClient used to interact with the Elasticsearch instance
   */
  public DatabaseServiceImpl(ElasticClient client) {
    this.client = client;
    nlpPluggedIn = false;
    geoPluggedIn = false;
  }

  /**
   * Constructor for initializing DatabaseServiceImpl object.
   *
   * @param client the ElasticClient object for connecting to Elasticsearch
   * @param nlpService the NLPSearchService object for natural language processing searches
   */
  public DatabaseServiceImpl(
      ElasticClient client, NLPSearchService nlpService, GeocodingService geoService) {
    this.client = client;
    this.nlpService = nlpService;
    this.geoService = geoService;
    nlpPluggedIn = true;
    geoPluggedIn = true;
  }

  private static boolean isInvalidRelForGivenItem(JsonObject request, String itemType) {
    if (request.getString(RELATIONSHIP).equalsIgnoreCase("resource")
        && itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE)) {
      return true;
    } else if (request.getString(RELATIONSHIP).equalsIgnoreCase("resourceGroup")
        && itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_GROUP)) {
      return true;
    } else if (request.getString(RELATIONSHIP).equalsIgnoreCase("provider")
        && itemType.equalsIgnoreCase(ITEM_TYPE_PROVIDER)) {
      return true;
    } else if (request.getString(RELATIONSHIP).equalsIgnoreCase("resourceServer")
        && itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_SERVER)) {
      return true;
    } else if (request.getString(RELATIONSHIP).equalsIgnoreCase("cos")
        && itemType.equalsIgnoreCase(ITEM_TYPE_COS)) {
      return true;
    } else if (request.getString(RELATIONSHIP).equalsIgnoreCase("all")
        && itemType.equalsIgnoreCase(ITEM_TYPE_COS)) {
      return true;
    }
    return false;
  }

  @Override
  public DatabaseService searchQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    LOGGER.debug("Info: searchQuery");

    RespBuilder respBuilder = new RespBuilder();
    request.put(SEARCH, true);

    /* Validate the Request */
    if (!request.containsKey(SEARCH_TYPE)) {
      handler.handle(
          Future.failedFuture(
              respBuilder
                  .withType(TYPE_INVALID_SYNTAX)
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

    client.searchAsync(
        query.toString(),
        docIndex,
        searchRes -> {
          if (searchRes.succeeded()) {
            LOGGER.debug("Success: Successful DB request");
            handler.handle(Future.succeededFuture(searchRes.result()));
          } else {
            LOGGER.error("Fail: DB Request;" + searchRes.cause().getMessage());
            handler.handle(Future.failedFuture(internalErrorResp));
          }
        });
    return this;
  }

  /**
   * Executes an NLP search query by passing in the request embeddings and invoking the appropriate
   * search method on the ElasticSearch client.
   *
   * @param request the request embeddings
   * @param handler the handler to be called when the search completes
   * @return the DatabaseService instance
   */
  public DatabaseService nlpSearchQuery(
      JsonArray request, Handler<AsyncResult<JsonObject>> handler) {
    JsonArray embeddings = request.getJsonArray(0);
    client.scriptSearch(
        embeddings,
        searchRes -> {
          if (searchRes.succeeded()) {
            LOGGER.debug("Success:Successful DB request");
            handler.handle(Future.succeededFuture(searchRes.result()));
          } else {
            LOGGER.error("Fail: DB request;" + searchRes.cause().getMessage());
            handler.handle(Future.failedFuture(internalErrorResp));
          }
        });
    return this;
  }

  /**
   * Performs an NLP search for a location query.
   *
   * @param queryParams the query parameters to search with
   * @param handler the handler to call with the response
   * @return the current DatabaseService instance
   */
  public DatabaseService nlpSearchLocationQuery(
      JsonArray request, JsonObject queryParams, Handler<AsyncResult<JsonObject>> handler) {
    JsonArray embeddings = request.getJsonArray(0);
    JsonArray params = queryParams.getJsonArray(RESULTS);
    JsonArray results = new JsonArray();

    // For each geocoding result, make a script search asynchronously
    List<Future> futures = new ArrayList<>();
    params.stream()
        .forEach(
            param -> {
              futures.add(client.scriptLocationSearch(embeddings, (JsonObject) param));
            });

    // For each future, add the result to a result object
    futures.forEach(
        future -> {
          future.onSuccess(
              h -> {
                JsonArray hr = ((JsonObject) h).getJsonArray(RESULTS);
                hr.stream().forEach(r -> results.add(r));
              });
        });

    // When all futures return, respond back with the result object in the response
    CompositeFuture.all(futures)
        .onComplete(
            ar -> {
              if (ar.succeeded()) {
                if (results.isEmpty()) {
                  RespBuilder respBuilder =
                      new RespBuilder()
                          .withType(TYPE_ITEM_NOT_FOUND)
                          .withTitle(TITLE_ITEM_NOT_FOUND)
                          .withDetail("NLP Search Failed");
                  handler.handle(Future.succeededFuture(respBuilder.getJsonResponse()));
                } else {
                  RespBuilder respBuilder =
                      new RespBuilder()
                          .withType(TYPE_SUCCESS)
                          .withTitle(TITLE_SUCCESS)
                          .withResult(results);
                  handler.handle(Future.succeededFuture(respBuilder.getJsonResponse()));
                }
              }
            });

    return this;
  }

  @Override
  public DatabaseService countQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    request.put(SEARCH, false);

    /* Validate the Request */
    if (!request.containsKey(SEARCH_TYPE)) {
      handler.handle(Future.failedFuture(internalErrorResp));
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
    client.countAsync(
        query.toString(),
        docIndex,
        searchRes -> {
          if (searchRes.succeeded()) {
            LOGGER.debug("Success: Successful DB request");
            handler.handle(Future.succeededFuture(searchRes.result()));
          } else {
            LOGGER.error("Fail: DB Request;" + searchRes.cause().getMessage());
            handler.handle(Future.failedFuture(internalErrorResp));
          }
        });
    return this;
  }

  /**
   * {@inheritDoc}
   * */
  @Override
  public DatabaseService createItem(JsonObject doc, Handler<AsyncResult<JsonObject>> handler) {

    RespBuilder respBuilder = new RespBuilder();
    String id = doc.getString("id");
    /* check if the id is present */
    if (id != null) {
      final String instanceId = doc.getString(INSTANCE);

      String errorJson = respBuilder.withType(FAILED).withResult(id, INSERT, FAILED).getResponse();

      String checkItem = GET_DOC_QUERY.replace("$1", id).replace("$2", "");

      verifyInstance(instanceId)
          .onComplete(
              instanceHandler -> {
                if (instanceHandler.succeeded()) {
                  LOGGER.debug("Info: Instance info;" + instanceHandler.result());

                  client.searchAsync(
                      checkItem.toString(),
                      docIndex,
                      checkRes -> {
                        if (checkRes.failed()) {
                          LOGGER.error("Fail: Insertion failed;" + checkRes.cause());
                          handler.handle(Future.failedFuture(errorJson));
                        }
                        if (checkRes.succeeded()) {
                          if (checkRes.result().getInteger(TOTAL_HITS) != 0) {
                            handler.handle(
                                Future.failedFuture(
                                    respBuilder
                                        .withType(TYPE_ALREADY_EXISTS)
                                        .withTitle(TITLE_ALREADY_EXISTS)
                                        .withResult(id, INSERT, FAILED, "Fail: Doc Exists")
                                        .getResponse()));
                            return;
                          }

                          doc.put(SUMMARY_KEY, Summarizer.summarize(doc));

                          /* If geo and nlp services are initialized */
                          if (geoPluggedIn
                              && nlpPluggedIn
                              && !(instanceId == null
                                  || instanceId.isBlank()
                                  || instanceId.isEmpty())) {
                            geoService.geoSummarize(
                                doc,
                                geoHandler -> {
                                  /* Not going to check if success or fail */
                                  JsonObject geoResult;
                                  try {
                                    geoResult = new JsonObject(geoHandler.result());
                                  } catch (Exception e) {
                                    LOGGER.debug("no geocoding result generated");
                                    geoResult = new JsonObject();
                                  }
                                  doc.put(GEOSUMMARY_KEY, geoResult);
                                  nlpService.getEmbedding(
                                      doc,
                                      ar -> {
                                        if (ar.succeeded()) {
                                          LOGGER.debug("Info: Document embeddings created");
                                          doc.put(
                                              WORD_VECTOR_KEY, ar.result().getJsonArray("result"));
                                          /* Insert document */
                                          client.docPostAsync(
                                              docIndex,
                                              doc.toString(),
                                              postRes -> {
                                                if (postRes.succeeded()) {
                                                  handler.handle(
                                                      Future.succeededFuture(
                                                          respBuilder
                                                              .withType(TYPE_SUCCESS)
                                                              .withTitle(TITLE_SUCCESS)
                                                              .withResult(doc)
                                                              .getJsonResponse()));
                                                } else {
                                                  handler.handle(Future.failedFuture(errorJson));
                                                  LOGGER.error(
                                                      "Fail: Insertion failed" + postRes.cause());
                                                }
                                              });
                                        } else {
                                          LOGGER.error("Error: Document embeddings not created");
                                        }
                                      });
                                });
                          } else {
                            /* Insert document */
                            new Timer()
                                .schedule(
                                    new TimerTask() {
                                      public void run() {
                                        client.docPostAsync(
                                            docIndex,
                                            doc.toString(),
                                            postRes -> {
                                              if (postRes.succeeded()) {
                                                handler.handle(
                                                    Future.succeededFuture(
                                                        respBuilder
                                                            .withType(TYPE_SUCCESS)
                                                            .withTitle(TITLE_SUCCESS)
                                                            .withResult(doc)
                                                            .getJsonResponse()));
                                              } else {
                                                handler.handle(Future.failedFuture(errorJson));
                                                LOGGER.error(
                                                    "Fail: Insertion failed" + postRes.cause());
                                              }
                                            });
                                      }
                                    },
                                    STATIC_DELAY_TIME);
                          }
                        }
                      });
                } else if (instanceHandler.failed()) {
                  handler.handle(
                      Future.failedFuture(
                          respBuilder
                              .withType(TYPE_OPERATION_NOT_ALLOWED)
                              .withTitle(TITLE_OPERATION_NOT_ALLOWED)
                              .withResult(
                                  id, INSERT, FAILED, instanceHandler.cause().getLocalizedMessage())
                              .getResponse()));
                }
              });
      return this;
    } else {
      LOGGER.error("Fail : id not present in the request");
      handler.handle(
          Future.failedFuture(
              respBuilder
                  .withType(TYPE_INVALID_SYNTAX)
                  .withTitle(TITLE_INVALID_SYNTAX)
                  .withDetail(DETAIL_ID_NOT_FOUND)
                  .getResponse()));
      return null;
    }
  }

  /**
   * {@inheritDoc}
   * */
  @Override
  public DatabaseService updateItem(JsonObject doc, Handler<AsyncResult<JsonObject>> handler) {

    RespBuilder respBuilder = new RespBuilder();
    String id = doc.getString("id");
    String type = doc.getJsonArray("type").getString(0);
    String checkQuery =
        GET_DOC_QUERY_WITH_TYPE
            .replace("$1", id)
            .replace("$3", type)
            .replace("$2", "id");

    new Timer()
        .schedule(
            new TimerTask() {
              public void run() {
                client.searchGetId(
                    checkQuery,
                    docIndex,
                    checkRes -> {
                      if (checkRes.failed()) {
                        LOGGER.error("Fail: Check query fail;" + checkRes.cause());
                        handler.handle(Future.failedFuture(internalErrorResp));
                        return;
                      }
                      if (checkRes.succeeded()) {
                        if (checkRes.result().getInteger(TOTAL_HITS) != 1) {
                          LOGGER.error("Fail: Doc doesn't exist, can't update");
                          handler.handle(
                              Future.failedFuture(
                                  respBuilder
                                      .withType(TYPE_ITEM_NOT_FOUND)
                                      .withTitle(TITLE_ITEM_NOT_FOUND)
                                      .withResult(
                                          id,
                                          UPDATE,
                                          FAILED,
                                          "Fail: Doc doesn't exist, can't update")
                                      .getResponse()));
                          return;
                        }
                        String docId = checkRes.result().getJsonArray(RESULTS).getString(0);
                        client.docPutAsync(
                            docId,
                            docIndex,
                            doc.toString(),
                            putRes -> {
                              if (putRes.succeeded()) {
                                handler.handle(
                                    Future.succeededFuture(
                                        respBuilder
                                            .withType(TYPE_SUCCESS)
                                            .withTitle(TITLE_SUCCESS)
                                            .withResult(doc)
                                            .getJsonResponse()));
                              } else {
                                handler.handle(Future.failedFuture(internalErrorResp));
                                LOGGER.error("Fail: Updation failed;" + putRes.cause());
                              }
                            });
                      }
                    });
              }
            },
            STATIC_DELAY_TIME);
    return this;
  }

  /**
   * {@inheritDoc}
   * */
  @Override
  public DatabaseService deleteItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    LOGGER.debug("Info: Deleting item");

    RespBuilder respBuilder = new RespBuilder();
    String id = request.getString("id");

    new Timer()
        .schedule(
            new TimerTask() {
              public void run() {
                String checkQuery = "";

                /* the check query checks if any type item is present more than once.
                If it's present then the item cannot be deleted.  */
                checkQuery = QUERY_RESOURCE_GRP.replace("$1", id);

                client.searchGetId(
                    checkQuery,
                    docIndex,
                    checkRes -> {
                      if (checkRes.failed()) {
                        LOGGER.error("Fail: Check query fail;" + checkRes.cause().getMessage());
                        handler.handle(Future.failedFuture(internalErrorResp));
                      }

                      if (checkRes.succeeded()) {
                        LOGGER.debug("Success: Check index for doc");
                        if (checkRes.result().getInteger(TOTAL_HITS) > 1) {
                          LOGGER.error("Fail: Can't delete, doc has associated item;");
                          handler.handle(
                              Future.failedFuture(
                                  respBuilder
                                      .withType(TYPE_OPERATION_NOT_ALLOWED)
                                      .withTitle(TITLE_OPERATION_NOT_ALLOWED)
                                      .withResult(id, "Fail: Can't delete, doc has associated item")
                                      .getResponse()));
                          return;
                        } else if (checkRes.result().getInteger(TOTAL_HITS) < 1) {
                          LOGGER.error("Fail: Doc doesn't exist, can't delete;");
                          handler.handle(
                              Future.failedFuture(
                                  respBuilder
                                      .withType(TYPE_ITEM_NOT_FOUND)
                                      .withTitle(TITLE_ITEM_NOT_FOUND)
                                      .withResult(id, "Fail: Doc doesn't exist, can't delete")
                                      .getResponse()));
                          return;
                        }
                      }

                      String docId = checkRes.result().getJsonArray(RESULTS).getString(0);
                      client.docDelAsync(
                          docId,
                          docIndex,
                          delRes -> {
                            if (delRes.succeeded()) {
                              handler.handle(
                                  Future.succeededFuture(
                                      respBuilder
                                          .withType(TYPE_SUCCESS)
                                          .withTitle(TITLE_SUCCESS)
                                          .withResult(id)
                                          .getJsonResponse()));
                            } else {
                              handler.handle(Future.failedFuture(internalErrorResp));
                              LOGGER.error("Fail: Deletion failed;" + delRes.cause().getMessage());
                            }
                          });
                    });
              }
            },
            STATIC_DELAY_TIME);
    return this;
  }

  /**
  * {@inheritDoc}
  **/
  @Override
  public DatabaseService getItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    LOGGER.debug("Info: Get item");

    RespBuilder respBuilder = new RespBuilder();
    String itemId = request.getString(ID);
    String getQuery = GET_DOC_QUERY.replace("$1", itemId).replace("$2", "");

    client.searchAsync(
        getQuery,
        docIndex,
        clientHandler -> {
          if (clientHandler.succeeded()) {
            LOGGER.debug("Success: Successful DB request");
            JsonObject responseJson = clientHandler.result();
            handler.handle(Future.succeededFuture(responseJson));
          } else {
            LOGGER.error("Fail: Failed getting item;" + clientHandler.cause());
            /* Handle request error */
            handler.handle(
                Future.failedFuture(
                    respBuilder
                        .withType(TYPE_INTERNAL_SERVER_ERROR)
                        .withTitle(TITLE_INTERNAL_SERVER_ERROR)
                        .getResponse()));
          }
        });
    return this;
  }

  /**
   * {@inheritDoc}
   * */
  @Override
  public DatabaseService listItems(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    RespBuilder respBuilder = new RespBuilder();
    String elasticQuery = queryDecoder.listItemQuery(request);

    LOGGER.debug("Info: Listing items;" + elasticQuery);

    client.listAggregationAsync(
        elasticQuery,
        clientHandler -> {
          if (clientHandler.succeeded()) {
            LOGGER.debug("Success: Successful DB request");
            JsonObject responseJson = clientHandler.result();
            handler.handle(Future.succeededFuture(responseJson));
          } else {
            LOGGER.error("Fail: DB request has failed;" + clientHandler.cause());
            /* Handle request error */
            handler.handle(
                Future.failedFuture(
                    respBuilder
                        .withType(TYPE_INTERNAL_SERVER_ERROR)
                        .withTitle(TITLE_INTERNAL_SERVER_ERROR)
                        .getResponse()));
          }
        });
    return this;
  }

  /**
   * {@inheritDoc}
   * */
  @Override
  public DatabaseService listRelationship(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    RespBuilder respBuilder = new RespBuilder();

    StringBuilder typeQuery =
        new StringBuilder(GET_TYPE_SEARCH.replace("$1", request.getString(ID)));
    LOGGER.debug("typeQuery: " + typeQuery);

    client.searchAsync(
        typeQuery.toString(),
        docIndex,
        qeryhandler -> {
          if (qeryhandler.succeeded()) {
            if (qeryhandler.result().getInteger(TOTAL_HITS) == 0) {
              handler.handle(
                  Future.failedFuture(
                      respBuilder
                          .withType(TYPE_ITEM_NOT_FOUND)
                          .withTitle(TITLE_ITEM_NOT_FOUND)
                          .withDetail("Item id given is not present")
                          .getResponse()));
              return;
            }
            JsonObject relType = qeryhandler.result().getJsonArray(RESULTS).getJsonObject(0);

            Set<String> type = new HashSet<String>(relType.getJsonArray(TYPE).getList());
            type.retainAll(ITEM_TYPES);
            String itemType = type.toString().replaceAll("\\[", "").replaceAll("\\]", "");
            LOGGER.debug("Info: itemType: " + itemType);
            relType.put("itemType", itemType);

            if (isInvalidRelForGivenItem(request, itemType)) {
              handler.handle(
                  Future.failedFuture(
                      respBuilder
                          .withType(TYPE_INVALID_SEARCH_ERROR)
                          .withTitle(TITLE_INVALID_SEARCH_ERROR)
                          .withDetail(TITLE_INVALID_SEARCH_ERROR)
                          .getResponse()));
              return;
            }

            if ((request.getString(RELATIONSHIP).equalsIgnoreCase(RESOURCE_SVR)
                    || request.getString(RELATIONSHIP).equalsIgnoreCase(ALL))
                && itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_GROUP)) {
              LOGGER.debug(relType);
              handleRsFetchForResourceGroup(request, handler, respBuilder, relType);
            } else if (request.getString(RELATIONSHIP).equalsIgnoreCase(RESOURCE_GRP)
                && itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_SERVER)) {
              handleResourceGroupFetchForRs(request, handler, respBuilder, relType);
            } else {
              request.mergeIn(relType);
              String elasticQuery = queryDecoder.listRelationshipQuery(request);
              LOGGER.debug("Info: Query constructed;" + elasticQuery);
              if (elasticQuery != null) {
                handleClientSearchAsync(handler, respBuilder, elasticQuery);
              } else {
                handler.handle(
                    Future.failedFuture(
                        respBuilder
                            .withType(TYPE_INVALID_SEARCH_ERROR)
                            .withTitle(TITLE_INVALID_SEARCH_ERROR)
                            .withDetail(TITLE_INVALID_SEARCH_ERROR)
                            .getResponse()));
              }
            }
          } else {
            LOGGER.error(qeryhandler.cause().getMessage());
          }
        });
    return this;
  }

  private void handleClientSearchAsync(
      Handler<AsyncResult<JsonObject>> handler, RespBuilder respBuilder, String elasticQuery) {
    client.searchAsync(
        elasticQuery,
        docIndex,
        searchRes -> {
          if (searchRes.succeeded()) {
            LOGGER.debug("Success: Successful DB request");
            handler.handle(Future.succeededFuture(searchRes.result()));
          } else {
            LOGGER.error("Fail: DB request has failed;" + searchRes.cause());
            /* Handle request error */
            handler.handle(
                Future.failedFuture(
                    respBuilder
                        .withType(TYPE_INTERNAL_SERVER_ERROR)
                        .withDetail(TITLE_INTERNAL_SERVER_ERROR)
                        .getResponse()));
          }
        });
  }

  private void handleResourceGroupFetchForRs(
      JsonObject request,
      Handler<AsyncResult<JsonObject>> handler,
      RespBuilder respBuilder,
      JsonObject relType) {
    StringBuilder typeQuery4RsGroup =
        new StringBuilder(GET_RSGROUP.replace("$1", relType.getString(ID)));
    LOGGER.debug("typeQuery4RsGroup: " + typeQuery4RsGroup);

    client.searchAsync(
        typeQuery4RsGroup.toString(),
        docIndex,
        serverSearch -> {
          if (serverSearch.succeeded()) {
            JsonArray serverResult = serverSearch.result().getJsonArray("results");
            LOGGER.debug("serverResult: " + serverResult);
            request.put("providerIds", serverResult);
            request.mergeIn(relType);
            String elasticQuery = queryDecoder.listRelationshipQuery(request);

            LOGGER.debug("Info: Query constructed;" + elasticQuery);

            handleClientSearchAsync(handler, respBuilder, elasticQuery);
          }
        });
  }

  private void handleRsFetchForResourceGroup(
      JsonObject request,
      Handler<AsyncResult<JsonObject>> handler,
      RespBuilder respBuilder,
      JsonObject relType) {
    StringBuilder typeQuery4Rserver =
        new StringBuilder(GET_TYPE_SEARCH.replace("$1", relType.getString(PROVIDER)));
    LOGGER.debug("typeQuery4Rserver: " + typeQuery4Rserver);

    client.searchAsync(
        typeQuery4Rserver.toString(),
        docIndex,
        serverSearch -> {
          if (serverSearch.succeeded() && serverSearch.result().getInteger(TOTAL_HITS) != 0) {
            JsonObject serverResult =
                serverSearch.result().getJsonArray("results").getJsonObject(0);
            request.mergeIn(serverResult);
            request.mergeIn(relType);
            String elasticQuery = queryDecoder.listRelationshipQuery(request);

            LOGGER.debug("Info: Query constructed;" + elasticQuery);

            if (elasticQuery != null) {
              handleClientSearchAsync(handler, respBuilder, elasticQuery);
            } else {
              handler.handle(
                  Future.failedFuture(
                      respBuilder
                          .withType(TYPE_INVALID_SEARCH_ERROR)
                          .withTitle(TITLE_INVALID_SEARCH_ERROR)
                          .withDetail(TITLE_INVALID_SEARCH_ERROR)
                          .getResponse()));
            }
          } else {
            respBuilder
                .withType(TYPE_ITEM_NOT_FOUND)
                .withTitle(TITLE_ITEM_NOT_FOUND)
                .withDetail("Resource Group for given item not found");
            handler.handle(Future.failedFuture(respBuilder.getResponse()));
          }
        });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService relSearch(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    RespBuilder respBuilder = new RespBuilder();
    String subQuery = "";
    String errorJson =
        respBuilder.withType(FAILED).withDetail(ERROR_INVALID_PARAMETER).getResponse();

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

        subQuery =
            TERM_QUERY.replace("$1", TYPE_KEYWORD).replace("$2", typeValue)
                + ","
                + MATCH_QUERY.replace("$1", relReqsKey).replace("$2", relReqsValue);
      } else {
        LOGGER.error("Fail: Incorrect/missing query parameters");
        handler.handle(Future.failedFuture(errorJson));
        return null;
      }

      JsonObject elasticQuery =
          new JsonObject(BOOL_MUST_QUERY.replace("$1", subQuery)).put(SOURCE, ID);

      /* Initial db query to filter matching attributes */
      client.searchAsync(
          elasticQuery.toString(),
          docIndex,
          searchRes -> {
            if (searchRes.succeeded()) {

              JsonArray resultValues = searchRes.result().getJsonArray(RESULTS);
              elasticQuery.clear();
              JsonArray idCollection = new JsonArray();

              /* iterating over the filtered response json array */
              if (!resultValues.isEmpty()) {

                for (Object idIndex : resultValues) {
                  JsonObject id = (JsonObject) idIndex;
                  if (!id.isEmpty()) {
                    idCollection.add(
                        new JsonObject()
                            .put(
                                WILDCARD_KEY,
                                new JsonObject().put(ID_KEYWORD, id.getString(ID) + "*")));
                  }
                }
              } else {
                handler.handle(Future.succeededFuture(searchRes.result()));
              }

              elasticQuery.put(
                  QUERY_KEY, new JsonObject(SHOULD_QUERY.replace("$1", idCollection.toString())));

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
              client.searchAsync(
                  elasticQuery.toString(),
                  docIndex,
                  relSearchRes -> {
                    if (relSearchRes.succeeded()) {

                      LOGGER.debug("Success: Successful DB request");
                      handler.handle(Future.succeededFuture(relSearchRes.result()));
                    } else if (relSearchRes.failed()) {
                      LOGGER.error(
                          "Fail: DB request has failed;" + relSearchRes.cause().getMessage());
                      handler.handle(Future.failedFuture(internalErrorResp));
                    }
                  });
            } else {
              LOGGER.error("Fail: DB request has failed;" + searchRes.cause().getMessage());
              handler.handle(Future.failedFuture(internalErrorResp));
            }
          });
    }
    return this;
  }

  /**
   * {@inheritDoc}
   * */
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
                    respBuilder.withType(FAILED).withResult(ratingId).getResponse()));
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
                ratingIndex,
                ratingDoc.toString(),
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

  /**
   * {@inheritDoc}
   * */
  @Override
  public DatabaseService updateRating(
      JsonObject ratingDoc, Handler<AsyncResult<JsonObject>> handler) {
    RespBuilder respBuilder = new RespBuilder();
    String ratingId = ratingDoc.getString("ratingID");

    String checkForExistingRecord = GET_RDOC_QUERY.replace("$1", ratingId).replace("$2", "");

    client.searchGetId(
        checkForExistingRecord,
        ratingIndex,
        checkRes -> {
          if (checkRes.failed()) {
            LOGGER.error("Fail: Check query fail;" + checkRes.cause());
            handler.handle(Future.failedFuture(internalErrorResp));
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
                ratingIndex,
                ratingDoc.toString(),
                putRes -> {
                  if (putRes.succeeded()) {
                    handler.handle(
                        Future.succeededFuture(
                            respBuilder
                                .withType(TYPE_SUCCESS)
                                .withTitle(TITLE_SUCCESS)
                                .withResult(ratingId)
                                .getJsonResponse()));
                  } else {
                    handler.handle(Future.failedFuture(internalErrorResp));
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

    client.searchGetId(
        checkForExistingRecord,
        ratingIndex,
        checkRes -> {
          if (checkRes.failed()) {
            LOGGER.error("Fail: Check query fail;" + checkRes.cause());
            handler.handle(Future.failedFuture(internalErrorResp));
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
                                .withResult(ratingId)
                                .getJsonResponse()));
                  } else {
                    handler.handle(Future.failedFuture(internalErrorResp));
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
      String ratingId = request.getString("ratingID");
      query = GET_RATING_DOCS.replace("$1", "ratingID").replace("$2", ratingId);
      LOGGER.debug(query);
    } else {
      String id = request.getString(ID);
      if (request.containsKey(TYPE) && request.getString(TYPE).equalsIgnoreCase("average")) {
        Future<List<String>> getAssociatedIdFuture = getAssociatedIDs(id);
        getAssociatedIdFuture.onComplete(
            ids -> {
              StringBuilder avgQuery = new StringBuilder(GET_AVG_RATING_PREFIX);
              if (ids.succeeded()) {
                ids.result().stream()
                    .forEach(
                        v -> {
                          avgQuery.append(GET_AVG_RATING_MATCH_QUERY.replace("$1", v));
                        });
                avgQuery.deleteCharAt(avgQuery.lastIndexOf(","));
                avgQuery.append(GET_AVG_RATING_SUFFIX);
                LOGGER.debug(avgQuery);
                client.ratingAggregationAsync(
                    avgQuery.toString(),
                    ratingIndex,
                    getRes -> {
                      if (getRes.succeeded()) {
                        LOGGER.debug("Success: Successful DB request");
                        JsonObject result = getRes.result();
                        handler.handle(Future.succeededFuture(result));
                      } else {
                        LOGGER.error("Fail: failed getting average rating: " + getRes.cause());
                        handler.handle(Future.failedFuture(internalErrorResp));
                      }
                    });
              } else {
                handler.handle(Future.failedFuture(internalErrorResp));
              }
            });

        return this;
      } else {
        query = GET_RATING_DOCS.replace("$1", "id.keyword").replace("$2", id);
        LOGGER.debug(query);
      }
    }

    LOGGER.debug(query);

    client.searchAsync(
        query,
        ratingIndex,
        getRes -> {
          if (getRes.succeeded()) {
            LOGGER.debug("Success: Successful DB request");
            JsonObject result = getRes.result();
            if (request.containsKey("ratingID")) {
              result.remove(TOTAL_HITS);
            }
            handler.handle(Future.succeededFuture(result));
          } else {
            LOGGER.error("Fail: failed getting rating: " + getRes.cause());
            handler.handle(Future.failedFuture(internalErrorResp));
          }
        });
    return this;
  }

  private Future<List<String>> getAssociatedIDs(String id) {
    Promise<List<String>> promise = Promise.promise();

    StringBuilder query =
        new StringBuilder(GET_ASSOCIATED_ID_QUERY.replace("$1", id).replace("$2", id));
    LOGGER.debug(query);
    client.searchAsync(
        query.toString(),
        docIndex,
        res -> {
          if (res.succeeded()) {
            List<String> idCollector =
                res.result().getJsonArray(RESULTS).stream()
                    .map(JsonObject.class::cast)
                    .map(d -> d.getString(ID))
                    .collect(Collectors.toList());
            promise.complete(idCollector);
          } else {
            LOGGER.error("Fail: Get average rating failed");
            promise.fail("Fail: Get average rating failed");
          }
        });
    return promise.future();
  }
  /**
   * Creates a new mlayer instance in the Elasticsearch database with the given instance document.
   *
   * @param instanceDoc the JsonObject representing the mlayer instance document
   * @param handler the asynchronous result handler
   * @return the DatabaseService instance
   */

  @Override

  public DatabaseService createMlayerInstance(
      JsonObject instanceDoc, Handler<AsyncResult<JsonObject>> handler) {
    RespBuilder respBuilder = new RespBuilder();
    String instanceId = instanceDoc.getString(INSTANCE_ID);
    String id = instanceDoc.getString(MLAYER_ID);
    String checkForExistingRecord = CHECK_MDOC_QUERY.replace("$1", id).replace("$2", "");
    client.searchAsync(
        checkForExistingRecord,
        mlayerInstanceIndex,
        res -> {
          if (res.failed()) {
            LOGGER.error("Fail: Insertion of mlayer Instance failed: " + res.cause());
            handler.handle(
                Future.failedFuture(
                    respBuilder.withType(FAILED).withResult(MLAYER_ID).getResponse()));

          } else {
            if (res.result().getInteger(TOTAL_HITS) != 0) {
              JsonObject json = new JsonObject(res.result().getJsonArray(RESULTS).getString(0));
              String instanceIdExists = json.getString(INSTANCE_ID);

              handler.handle(
                  Future.failedFuture(
                      respBuilder
                          .withType(TYPE_ALREADY_EXISTS)
                          .withTitle(TITLE_ALREADY_EXISTS)
                          .withResult(instanceIdExists, " Fail: Instance Already Exists")
                          .getResponse()));
              return;
            }
            client.docPostAsync(
                mlayerInstanceIndex,
                instanceDoc.toString(),
                result -> {
                  if (result.succeeded()) {
                    handler.handle(
                        Future.succeededFuture(
                            respBuilder
                                .withType(TYPE_SUCCESS)
                                .withTitle(SUCCESS)
                                .withResult(instanceId, "Instance created Sucesssfully")
                                .getJsonResponse()));
                  } else {

                    handler.handle(
                        Future.failedFuture(
                            respBuilder.withType(TYPE_FAIL).withResult(FAILED).getResponse()));
                    LOGGER.error("Fail: Insertion failed" + result.cause());
                  }
                });
          }
        });

    return this;
  }

  @Override
  public DatabaseService getMlayerInstance(String id, Handler<AsyncResult<JsonObject>> handler) {
    String query = "";
    if (id == null || id.isBlank()) {
      query = GET_ALL_MLAYER_INSTANCE_QUERY;
    } else {
      query = GET_MLAYER_INSTANCE_QUERY.replace("$1", id);
    }
    client.searchAsync(
        query,
        mlayerInstanceIndex,
        resultHandler -> {
          if (resultHandler.succeeded()) {
            LOGGER.debug("Success: Successful DB Request");
            JsonObject result = resultHandler.result();
            handler.handle(Future.succeededFuture(result));
          } else {
            LOGGER.error("Fail: failed DB request");
            handler.handle(Future.failedFuture(internalErrorResp));
          }
        });
    return this;
  }

  @Override
  public DatabaseService deleteMlayerInstance(
      String instanceId, Handler<AsyncResult<JsonObject>> handler) {
    RespBuilder respBuilder = new RespBuilder();

    String checkForExistingRecord =
        CHECK_MDOC_QUERY_INSTANCE.replace("$1", instanceId).replace("$2", "");

    client.searchGetId(
        checkForExistingRecord,
        mlayerInstanceIndex,
        checkRes -> {
          if (checkRes.failed()) {
            LOGGER.error("Fail: Check query fail;" + checkRes.cause());
            handler.handle(Future.failedFuture(internalErrorResp));
          } else {
            if (checkRes.result().getInteger(TOTAL_HITS) != 1) {
              LOGGER.error("Fail: Instance doesn't exist, can't delete");
              handler.handle(
                  Future.failedFuture(
                      respBuilder
                          .withType(TYPE_ITEM_NOT_FOUND)
                          .withTitle(TITLE_ITEM_NOT_FOUND)
                          .withResult(instanceId, "Fail: Instance doesn't exist, can't delete")
                          .getResponse()));
              return;
            }
            String docId = checkRes.result().getJsonArray(RESULTS).getString(0);

            client.docDelAsync(
                docId,
                mlayerInstanceIndex,
                delRes -> {
                  if (delRes.succeeded()) {
                    handler.handle(
                        Future.succeededFuture(
                            respBuilder
                                .withType(TYPE_SUCCESS)
                                .withTitle(SUCCESS)
                                .withResult(instanceId, "Instance deleted Successfully")
                                .getJsonResponse()));
                  } else {
                    handler.handle(Future.failedFuture(internalErrorResp));
                    LOGGER.error("Fail: Deletion failed;" + delRes.cause());
                  }
                });
          }
        });
    return this;
  }

  @Override
  public DatabaseService updateMlayerInstance(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    RespBuilder respBuilder = new RespBuilder();
    String instanceId = request.getString(INSTANCE_ID);
    String checkForExistingRecord =
        CHECK_MDOC_QUERY_INSTANCE.replace("$1", instanceId).replace("$2", "");
    client.searchAsyncGetId(
        checkForExistingRecord,
        mlayerInstanceIndex,
        checkRes -> {
          if (checkRes.failed()) {
            LOGGER.error("Fail: Check query fail;" + checkRes.cause());
            handler.handle(Future.failedFuture(internalErrorResp));
          } else {
            LOGGER.debug(checkRes.result());
            if (checkRes.result().getInteger(TOTAL_HITS) != 1) {
              LOGGER.error("Fail: Instance doesn't exist, can't update");
              handler.handle(
                  Future.failedFuture(
                      respBuilder
                          .withType(TYPE_ITEM_NOT_FOUND)
                          .withTitle(TITLE_ITEM_NOT_FOUND)
                          .withResult(instanceId, "Fail : Instance doesn't exist, can't update")
                          .getResponse()));
              return;
            }
            JsonObject result =
                new JsonObject(checkRes.result().getJsonArray(RESULTS).getString(0));

            String parameterIdName = result.getJsonObject(SOURCE).getString("name").toLowerCase();
            String requestBodyName = request.getString("name").toLowerCase();
            if (parameterIdName.equals(requestBodyName)) {
              String docId = result.getString(DOC_ID);
              client.docPutAsync(
                  docId,
                  mlayerInstanceIndex,
                  request.toString(),
                  putRes -> {
                    if (putRes.succeeded()) {
                      handler.handle(
                          Future.succeededFuture(
                              respBuilder
                                  .withType(TYPE_SUCCESS)
                                  .withTitle(SUCCESS)
                                  .withResult(instanceId, "Instance Updated Successfully")
                                  .getJsonResponse()));
                    } else {
                      handler.handle(Future.failedFuture(internalErrorResp));
                      LOGGER.error("Fail: Updation failed" + putRes.cause());
                    }
                  });
            } else {
              handler.handle(
                  Future.failedFuture(
                      respBuilder
                          .withType(TYPE_FAIL)
                          .withTitle(TITLE_WRONG_INSTANCE_NAME)
                          .withDetail(WRONG_INSTANCE_NAME)
                          .getResponse()));
              LOGGER.error("Fail: Updation Failed" + checkRes.cause());
            }
          }
        });
    return this;
  }

  @Override
  public DatabaseService createMlayerDomain(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    RespBuilder respBuilder = new RespBuilder();
    String domainId = request.getString(DOMAIN_ID);
    String id = request.getString(MLAYER_ID);
    String checkForExistingDomain = CHECK_MDOC_QUERY.replace("$1", id).replace("$2", "");
    client.searchAsync(
        checkForExistingDomain,
        mlayerDomainIndex,
        res -> {
          if (res.failed()) {
            LOGGER.error("Fail: Insertion of mLayer domain failed: " + res.cause());
            handler.handle(
                Future.failedFuture(respBuilder.withType(FAILED).withResult(id).getResponse()));
          } else {
            if (res.result().getInteger(TOTAL_HITS) != 0) {
              JsonObject json = new JsonObject(res.result().getJsonArray(RESULTS).getString(0));
              String domainIdExists = json.getString(DOMAIN_ID);
              handler.handle(
                  Future.failedFuture(
                      respBuilder
                          .withType(TYPE_ALREADY_EXISTS)
                          .withTitle(TITLE_ALREADY_EXISTS)
                          .withResult(domainIdExists, "Fail: Domain Already Exists")
                          .getResponse()));
              return;
            }
            client.docPostAsync(
                mlayerDomainIndex,
                request.toString(),
                result -> {
                  if (result.succeeded()) {
                    handler.handle(
                        Future.succeededFuture(
                            respBuilder
                                .withType(TYPE_SUCCESS)
                                .withTitle(SUCCESS)
                                .withResult(domainId, "domain Created Successfully")
                                .getJsonResponse()));
                  } else {
                    handler.handle(
                        Future.failedFuture(
                            respBuilder.withType(TYPE_FAIL).withResult(FAILED).getResponse()));
                    LOGGER.error("Fail: Insertion failed" + result.cause());
                  }
                });
          }
        });
    return this;
  }

  @Override
  public DatabaseService getMlayerDomain(String id, Handler<AsyncResult<JsonObject>> handler) {
    String query = "";
    if (id == null || id.isBlank()) {
      query = GET_ALL_MLAYER_DOMAIN_QUERY;
    } else {
      query = GET_MLAYER_DOMAIN_QUERY.replace("$1", id);
    }
    client.searchAsync(
        query,
        mlayerDomainIndex,
        resultHandler -> {
          if (resultHandler.succeeded()) {
            LOGGER.debug("Success: Successful DB Request");
            JsonObject result = resultHandler.result();
            handler.handle(Future.succeededFuture(result));
          } else {
            LOGGER.error("Fail: failed DB request");
            handler.handle(Future.failedFuture(internalErrorResp));
          }
        });
    return this;
  }

  @Override
  public DatabaseService updateMlayerDomain(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    RespBuilder respBuilder = new RespBuilder();
    String domainId = request.getString(DOMAIN_ID);
    String checkForExistingRecord =
        CHECK_MDOC_QUERY_DOMAIN.replace("$1", domainId).replace("$2", "");
    client.searchAsyncGetId(
        checkForExistingRecord,
        mlayerDomainIndex,
        checkRes -> {
          if (checkRes.failed()) {
            LOGGER.error("Fail: Check Query Fail");
            handler.handle(Future.failedFuture(internalErrorResp));
          } else {
            LOGGER.debug(checkRes.result());
            if (checkRes.result().getInteger(TOTAL_HITS) != 1) {
              LOGGER.error("Fail: Domain does not exist, can't update");
              handler.handle(
                  Future.failedFuture(
                      respBuilder
                          .withType(TYPE_ITEM_NOT_FOUND)
                          .withTitle(TITLE_ITEM_NOT_FOUND)
                          .withResult(domainId, "Fail: Domain doesn't exist, can't update")
                          .getResponse()));
              return;
            }

            JsonObject result =
                new JsonObject(checkRes.result().getJsonArray(RESULTS).getString(0));

            String parameterIdName = result.getJsonObject(SOURCE).getString("name").toLowerCase();
            String requestBodyName = request.getString("name").toLowerCase();
            if (parameterIdName.equals(requestBodyName)) {
              String docId = result.getString(DOC_ID);
              client.docPutAsync(
                  docId,
                  mlayerDomainIndex,
                  request.toString(),
                  putRes -> {
                    if (putRes.succeeded()) {
                      handler.handle(
                          Future.succeededFuture(
                              respBuilder
                                  .withType(TYPE_SUCCESS)
                                  .withTitle(SUCCESS)
                                  .withResult(domainId, "Domain Updated Successfully")
                                  .getJsonResponse()));
                    } else {
                      handler.handle(Future.failedFuture(internalErrorResp));
                      LOGGER.error("Fail: Updation failed" + putRes.cause());
                    }
                  });
            } else {
              handler.handle(
                  Future.failedFuture(
                      respBuilder
                          .withType(TYPE_FAIL)
                          .withTitle(TITLE_WRONG_INSTANCE_NAME)
                          .withDetail(WRONG_INSTANCE_NAME)
                          .getResponse()));
              LOGGER.error("Fail: Updation Failed" + checkRes.cause());
            }
          }
        });
    return this;
  }

  @Override
  public DatabaseService deleteMlayerDomain(
      String domainId, Handler<AsyncResult<JsonObject>> handler) {
    RespBuilder respBuilder = new RespBuilder();
    LOGGER.debug(domainId);

    String checkForExistingRecord =
        CHECK_MDOC_QUERY_DOMAIN.replace("$1", domainId).replace("$2", "");

    client.searchGetId(
        checkForExistingRecord,
        mlayerDomainIndex,
        checkRes -> {
          if (checkRes.failed()) {
            LOGGER.error("Fail: Check query fail;" + checkRes.cause());
            handler.handle(Future.failedFuture(internalErrorResp));
          } else {
            if (checkRes.result().getInteger(TOTAL_HITS) != 1) {
              LOGGER.error("Fail: Domain doesn't exist, can't delete");
              handler.handle(
                  Future.failedFuture(
                      respBuilder
                          .withType(TYPE_ITEM_NOT_FOUND)
                          .withTitle(TITLE_ITEM_NOT_FOUND)
                          .withResult(domainId, "Fail: Domain doesn't exist, can't delete")
                          .getResponse()));
              return;
            }

            String docId = checkRes.result().getJsonArray(RESULTS).getString(0);

            client.docDelAsync(
                docId,
                mlayerDomainIndex,
                putRes -> {
                  if (putRes.succeeded()) {

                    handler.handle(
                        Future.succeededFuture(
                            respBuilder
                                .withType(TYPE_SUCCESS)
                                .withTitle(SUCCESS)
                                .withResult(domainId, "Domain deleted Successfully")
                                .getJsonResponse()));
                  } else {
                    handler.handle(Future.failedFuture(internalErrorResp));
                    LOGGER.error("Fail: Deletion failed;" + putRes.cause());
                  }
                });
          }
        });
    return this;
  }

  @Override
  public DatabaseService getMlayerProviders(Handler<AsyncResult<JsonObject>> handler) {
    String query = GET_MLAYER_PROVIDERS_QUERY;
    client.searchAsync(
        query,
        docIndex,
        resultHandler -> {
          if (resultHandler.succeeded()) {
            LOGGER.debug("Success: Successful DB Request");
            JsonObject result = resultHandler.result();
            handler.handle(Future.succeededFuture(result));
          } else {
            LOGGER.error("Fail: failed DB request");
            handler.handle(Future.failedFuture(internalErrorResp));
          }
        });
    return this;
  }

  @Override
  public DatabaseService getMlayerGeoQuery(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    LOGGER.debug("request body" + request);

    String instance = request.getString(INSTANCE);
    JsonArray id = request.getJsonArray("id");
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < id.size(); i++) {
      String datasetId = id.getString(i);
      String combinedQuery =
          GET_MLAYER_BOOL_GEOQUERY.replace("$2", instance).replace("$3", datasetId);
      sb.append(combinedQuery).append(",");
    }
    sb.deleteCharAt(sb.length() - 1);
    String query = GET_MLAYER_GEOQUERY.replace("$1", sb);
    client.searchAsyncGeoQuery(
        query,
        docIndex,
        resultHandler -> {
          if (resultHandler.succeeded()) {

            LOGGER.debug("Success: Successful DB Request");
            handler.handle(Future.succeededFuture(resultHandler.result()));

          } else {

            LOGGER.error("Fail: failed DB request");
            handler.handle(Future.failedFuture(internalErrorResp));
          }
        });

    return this;
  }

  @Override
  public DatabaseService getMlayerAllDatasets(
      String query, Handler<AsyncResult<JsonObject>> handler) {

    LOGGER.debug("Getting all the resource group items");
    Promise<JsonObject> datasetResult = Promise.promise();
    Promise<JsonObject> instanceResult = Promise.promise();
    Promise<JsonObject> resourceCount = Promise.promise();

    gettingAllDatasets(query, datasetResult);
    allMlayerInstance(instanceResult);
    gettingResourceCount(resourceCount);

    CompositeFuture.all(instanceResult.future(), datasetResult.future(), resourceCount.future())
        .onComplete(
            ar -> {
              if (ar.succeeded()) {
                JsonObject instanceList = ar.result().resultAt(0);
                JsonObject resourceGroupList = ar.result().resultAt(1);
                JsonObject resourceCountList = ar.result().resultAt(2);
                JsonArray resourceGroupArray = new JsonArray();
                for (int i = 0; i < resourceGroupList.getInteger("resourceGroupCount"); i++) {
                  JsonObject record =
                      resourceGroupList.getJsonArray("resourceGroup").getJsonObject(i);
                  record.put(
                      "icon",
                      record.containsKey(INSTANCE)
                          ? instanceList.getString(record.getString(INSTANCE))
                          : "");
                  record.put(
                      "totalResources",
                      resourceCountList.containsKey(record.getString(ID))
                          ? resourceCountList.getInteger(record.getString(ID))
                          : 0);

                  record.remove(TYPE);
                  resourceGroupArray.add(record);
                }
                RespBuilder respBuilder =
                    new RespBuilder()
                        .withType(TYPE_SUCCESS)
                        .withTitle(SUCCESS)
                        .withTotalHits(resourceGroupList.getInteger("resourceGroupCount"))
                        .withResult(resourceGroupArray);
                handler.handle(Future.succeededFuture(respBuilder.getJsonResponse()));

              } else {
                LOGGER.error("Fail: failed DB request");
                handler.handle(Future.failedFuture(internalErrorResp));
              }
            });
    return this;
  }

  private void gettingResourceCount(Promise<JsonObject> resourceCountResult) {
    LOGGER.debug("Getting resource item count");
    String query = GET_RESOURCE_ITEM_COUNT;
    client.resourceAggregationAsync(
        query,
        docIndex,
        resourceCountRes -> {
          if (resourceCountRes.succeeded()) {
            JsonObject resourceItemCount = new JsonObject();
            int size = resourceCountRes.result().getJsonArray(RESULTS).size();
            for (int i = 0; i < size; i++) {
              JsonObject record = resourceCountRes.result().getJsonArray(RESULTS).getJsonObject(i);
              resourceItemCount.put(record.getString(KEY), record.getInteger("doc_count"));
            }
            resourceCountResult.complete(resourceItemCount);
          } else {
            LOGGER.error("Fail: query fail;" + resourceCountRes.cause());
            resourceCountResult.handle(Future.failedFuture(internalErrorResp));
          }
        });
  }

  private void allMlayerInstance(Promise<JsonObject> instanceResult) {
    LOGGER.debug("Getting all instance name and icons");
    client.searchAsync(
        GET_ALL_MLAYER_INSTANCES,
        mlayerInstanceIndex,
        instanceRes -> {
          if (instanceRes.succeeded()) {
            int instanceSize = instanceRes.result().getJsonArray(RESULTS).size();
            JsonObject instanceIcon = new JsonObject();
            for (int i = 0; i < instanceSize; i++) {
              JsonObject instanceObject =
                  instanceRes.result().getJsonArray(RESULTS).getJsonObject(i);
              instanceIcon.put(
                  instanceObject.getString("name").toLowerCase(), instanceObject.getString("icon"));
            }
            instanceResult.complete(instanceIcon);
          } else {

            LOGGER.error("Fail: query fail;" + instanceRes.cause());
            instanceResult.handle(Future.failedFuture(internalErrorResp));
          }
        });
  }

  private void gettingAllDatasets(String query, Promise<JsonObject> datasetResult) {
    LOGGER.debug("Getting all resourceGroup along with provider description, "
            + "resource server url and cosUrl");
    client.searchAsync(
        query,
        docIndex,
        resultHandler -> {
          if (resultHandler.succeeded()) {
            LOGGER.debug("Success: Successful DB Request");
            int size = resultHandler.result().getJsonArray(RESULTS).size();
            if (size == 0) {
              datasetResult.handle(
                  Future.failedFuture(
                      new RespBuilder()
                          .withType(TYPE_ITEM_NOT_FOUND)
                          .withTitle(TITLE_ITEM_NOT_FOUND)
                          .withDetail("no datasets are present")
                          .getResponse()));
            }
            JsonObject rsUrl = new JsonObject();
            JsonObject providerDescription = new JsonObject();
            JsonObject cosUrl = new JsonObject();
            for (int i = 0; i < size; i++) {
              JsonObject record = resultHandler.result().getJsonArray(RESULTS).getJsonObject(i);
              if (record.getJsonArray(TYPE).getString(0).equals(ITEM_TYPE_PROVIDER)) {
                providerDescription.put(record.getString(ID), record.getString(DESCRIPTION_ATTR));
                rsUrl.put(
                    record.getString(ID),
                    record.containsKey("resourceServerURL")
                        ? record.getString("resourceServerURL")
                        : "");
              } else if (record.getJsonArray(TYPE).getString(0).equals("iudx:COS")) {
                cosUrl.put(record.getString(ID), record.getString("cosURL"));
              }
            }
            int resourceGroupHits = 0;
            JsonArray resourceGroup = new JsonArray();
            for (int i = 0; i < size; i++) {
              JsonObject record = resultHandler.result().getJsonArray(RESULTS).getJsonObject(i);
              if (record.getJsonArray(TYPE).getString(0).equals(ITEM_TYPE_RESOURCE_GROUP)) {
                resourceGroupHits++;
                record.put(
                    "providerDescription",
                    providerDescription.getString(record.getString(PROVIDER)));
                record.put("resourceServerURL", rsUrl.getString(record.getString(PROVIDER)));
                record.put(
                    "cosURL",
                    record.containsKey("cos") ? cosUrl.getString(record.getString("cos")) : "");

                record.remove("cos");
                resourceGroup.add(record);
              }
            }
            JsonObject resourceGroupResult =
                new JsonObject()
                    .put("resourceGroupCount", resourceGroupHits)
                    .put("resourceGroup", resourceGroup);
            datasetResult.complete(resourceGroupResult);
          } else {
            LOGGER.error("Fail: failed DB request");
            datasetResult.handle(Future.failedFuture(internalErrorResp));
          }
        });
  }

  @Override
  public DatabaseService getMlayerDataset(
      JsonObject requestData, Handler<AsyncResult<JsonObject>> handler) {
    if (requestData.containsKey(ID)) {
      LOGGER.debug("dataset Id" + requestData.getString(ID));
      client.searchAsync(
          GET_PROVIDER_AND_RS_ID.replace("$1", requestData.getString(ID)),
          docIndex,
          handlerRes -> {
            if (handlerRes.succeeded()) {
              if (handlerRes.result().getInteger(TOTAL_HITS) == 0) {
                LOGGER.debug("The dataset is not available.");
                handler.handle(
                    Future.failedFuture(
                        new RespBuilder()
                            .withType(TYPE_ITEM_NOT_FOUND)
                            .withTitle(TITLE_ITEM_NOT_FOUND)
                            .withDetail("dataset belonging to Id requested is not present")
                            .getResponse()));
              }
              String providerId =
                  handlerRes.result().getJsonArray(RESULTS).getJsonObject(0).getString("provider");
              String cosId = "";
              if (handlerRes.result().getJsonArray(RESULTS).getJsonObject(0).containsKey("cos")) {
                cosId = handlerRes.result().getJsonArray(RESULTS).getJsonObject(0).getString("cos");

              }

              /*
              query to fetch resource group, provider of the resource group, resource
              items associated with the resource group and cos item.
              */
              String query =
                  GET_MLAYER_DATASET
                      .replace("$1", requestData.getString(ID))
                      .replace("$2", providerId)
                      .replace("$3", cosId);
              LOGGER.debug("Query " + query);
              client.searchAsyncDataset(
                  query,
                  docIndex,
                  resultHandler -> {
                    if (resultHandler.succeeded()) {
                      LOGGER.debug("Success: Successful DB Request");
                      JsonObject record =
                          resultHandler.result().getJsonArray(RESULTS).getJsonObject(0);
                        record.getJsonObject("dataset").put("totalResources",
                                record.getJsonArray("resource").size());
                      String instanceName = "";
                      String instanceCapitalizeName = "";
                      if (record.getJsonObject("dataset").containsKey(INSTANCE)
                          && !(record.getJsonObject("dataset").getString(INSTANCE) == null)
                          && !(record.getJsonObject("dataset").getString(INSTANCE).isBlank())) {

                        instanceName = record.getJsonObject("dataset").getString(INSTANCE);
                        instanceCapitalizeName =
                            instanceName.substring(0, 1).toUpperCase() + instanceName.substring(1);

                        // query to get the icon path of the instance in the  resource group
                        String getIconQuery =
                            GET_MLAYER_INSTANCE_ICON.replace("$1", instanceCapitalizeName);
                        client.searchAsync(
                            getIconQuery,
                            mlayerInstanceIndex,
                            iconResultHandler -> {
                              if (iconResultHandler.succeeded()) {
                                LOGGER.debug("Success: Successful DB Request");
                                JsonObject json = iconResultHandler.result();
                                if (json.getInteger(TOTAL_HITS) == 0) {
                                  LOGGER.debug("The icon path for the instance is not present.");
                                  record.getJsonObject("dataset").put("instance_icon", "");
                                } else {
                                  JsonObject resource =
                                      iconResultHandler
                                          .result()
                                          .getJsonArray(RESULTS)
                                          .getJsonObject(0);
                                  String instancePath = resource.getString("icon");
                                  record
                                      .getJsonObject("dataset")
                                      .put("instance_icon", instancePath);
                                }
                                resultHandler.result().remove(TOTAL_HITS);
                                handler.handle(Future.succeededFuture(resultHandler.result()));
                              } else {
                                LOGGER.error("Fail: failed DB request");
                                handler.handle(Future.failedFuture(internalErrorResp));
                              }
                            });
                      } else {
                        resultHandler.result().remove(TOTAL_HITS);
                        record.getJsonObject("dataset").put("instance_icon", "");
                        handler.handle(Future.succeededFuture(resultHandler.result()));
                      }
                    } else {
                      LOGGER.error("Fail: failed DB request");
                      handler.handle(Future.failedFuture(internalErrorResp));
                    }
                  });
            } else {
              LOGGER.error("Fail: DB request to get provider failed.");
              handler.handle(Future.failedFuture(internalErrorResp));
            }
          });
    } else if (requestData.containsKey("tags") || requestData.containsKey("instance")) {

      String query = "";
      if (requestData.containsKey("tags") && !requestData.containsKey("instance")) {
        query = GET_ALL_DATASETS_BY_DOMAIN.replace("$1", requestData.getString("tags"));
      } else if (!requestData.containsKey("tags") && requestData.containsKey("instance")) {
        query = GET_ALL_DATASETS_BY_INSTANCE.replace("$1", requestData.getString("instance"));
      } else if (requestData.containsKey("tags") && requestData.containsKey("instance")) {
        query =
            GET_ALL_DATASETS_BY_INSTANCE_AND_DOMAINS
                .replace("$1", requestData.getString("tags"))
                .replace("$2", requestData.getString("instance"));
      }
      getMlayerAllDatasets(query, handler);
    } else {
      LOGGER.error("Invalid field present in request body");
      handler.handle(
          Future.failedFuture(
              new RespBuilder()
                  .withType(TYPE_INVALID_PROPERTY_VALUE)
                  .withTitle(TITLE_INVALID_QUERY_PARAM_VALUE)
                  .withDetail("Invalid field present in request body")
                  .getResponse()));
    }

    return this;
  }

  @Override
  public DatabaseService getMlayerPopularDatasets(String instance,
      JsonArray highestCountResource, Handler<AsyncResult<JsonObject>> handler) {
    Promise<JsonObject> instanceResult = Promise.promise();

    Promise<JsonArray> domainResult = Promise.promise();
    Promise<JsonObject> datasetResult = Promise.promise();

    searchSortedMlayerInstances(instanceResult);

    allMlayerDomains(domainResult);
    datasets(instance, datasetResult, highestCountResource);
    CompositeFuture.all(instanceResult.future(), domainResult.future(), datasetResult.future())
        .onComplete(
            ar -> {
              if (ar.succeeded()) {
                JsonObject instanceList = ar.result().resultAt(0);
                JsonArray domainList = ar.result().resultAt(1);
                JsonObject datasetJson = ar.result().resultAt(2);
                for (int i = 0; i < datasetJson.getJsonArray("latestDataset").size(); i++) {
                  if (datasetJson
                      .getJsonArray("latestDataset")
                      .getJsonObject(i)
                      .containsKey("instance")) {
                    LOGGER.debug("given dataset has associated instance");
                    datasetJson
                        .getJsonArray("latestDataset")
                        .getJsonObject(i)
                        .put(
                            "icon",
                            instanceList
                                .getJsonObject("instanceIconPath")
                                .getString(
                                    datasetJson
                                        .getJsonArray("latestDataset")
                                        .getJsonObject(i)
                                        .getString("instance")
                                        .toLowerCase()));
                  } else {
                    LOGGER.debug("given dataset does not have associated instance");
                    datasetJson.getJsonArray("latestDataset").getJsonObject(i).put("icon", null);
                  }
                }
                for (int i = 0; i < datasetJson.getJsonArray("featuredDataset").size(); i++) {
                  if (datasetJson
                      .getJsonArray("featuredDataset")
                      .getJsonObject(i)
                      .containsKey("instance")) {
                    datasetJson
                        .getJsonArray("featuredDataset")
                        .getJsonObject(i)
                        .put(
                            "icon",
                            instanceList
                                .getJsonObject("instanceIconPath")
                                .getString(
                                    datasetJson
                                        .getJsonArray("featuredDataset")
                                        .getJsonObject(i)
                                        .getString("instance")));
                  } else {
                    datasetJson.getJsonArray("featuredDataset").getJsonObject(i).put("icon", null);
                  }
                }
                JsonObject result =
                    new JsonObject()
                        .put("totalInstance", instanceList.getInteger("totalInstance"))
                        .put("totalDomain", domainList.size())
                        .put(
                            "totalPublishers",
                            datasetJson.getJsonObject("typeCount").getInteger("iudx:Provider"))
                        .put(
                            "totalDatasets",
                            datasetJson.getJsonObject("typeCount").getInteger("iudx:ResourceGroup"))
                        .put(
                            "totalResources",
                            datasetJson.getJsonObject("typeCount").getInteger("iudx:Resource"))
                        .put("domains", domainList)
                        .put("instance", instanceList.getJsonArray("instanceList"))
                        .put("featuredDataset", datasetJson.getJsonArray("featuredDataset"))
                        .put("latestDataset", datasetJson.getJsonArray("latestDataset"));
                RespBuilder respBuilder =
                    new RespBuilder().withType(TYPE_SUCCESS).withTitle(SUCCESS).withResult(result);
                handler.handle(Future.succeededFuture(respBuilder.getJsonResponse()));
              } else {
                LOGGER.error("Fail: failed DB request");
                handler.handle(Future.failedFuture(internalErrorResp));
              }
            });
    return this;
  }

  private void searchSortedMlayerInstances(Promise<JsonObject> instanceResult) {
    client.searchAsync(
        GET_SORTED_MLAYER_INSTANCES,
        mlayerInstanceIndex,
        resultHandler -> {
          if (resultHandler.succeeded()) {
            int totalInstance = resultHandler.result().getInteger(TOTAL_HITS);
            Map<String, String> instanceIconPath = new HashMap<>();
            JsonArray instanceList = new JsonArray();
            for (int i = 0; i < resultHandler.result().getJsonArray(RESULTS).size(); i++) {
              JsonObject instance = resultHandler.result().getJsonArray(RESULTS).getJsonObject(i);
              instanceIconPath.put(
                  instance.getString("name").toLowerCase(), instance.getString("icon"));
              if (i < 4) {
                instanceList.add(i, instance);
              }
            }

            JsonObject json =
                new JsonObject()
                    .put("instanceIconPath", instanceIconPath)
                    .put("instanceList", instanceList)
                    .put("totalInstance", totalInstance);

            instanceResult.complete(json);
          } else {
            LOGGER.error("Fail: failed DB request");
            instanceResult.handle(Future.failedFuture(internalErrorResp));
          }
        });
  }

  private void allMlayerDomains(Promise<JsonArray> domainResult) {
    client.searchAsync(
            GET_ALL_MLAYER_DOMAIN_QUERY,
        mlayerDomainIndex,
        getDomainHandler -> {
          if (getDomainHandler.succeeded()) {
            JsonArray domainList = getDomainHandler.result().getJsonArray(RESULTS);
            domainResult.complete(domainList);
          } else {
            LOGGER.error("Fail: failed DB request");
            domainResult.handle(Future.failedFuture(internalErrorResp));
          }
        });
  }

  private Comparator<JsonObject> comapratorForLatestDataset() {
    Comparator<JsonObject> jsonComparator =
        new Comparator<JsonObject>() {

          @Override
          public int compare(JsonObject record1, JsonObject record2) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

            LocalDateTime dateTime1 =
                LocalDateTime.parse(record1.getString("itemCreatedAt"), formatter);
            LocalDateTime dateTime2 =
                LocalDateTime.parse(record2.getString("itemCreatedAt"), formatter);
            return dateTime2.compareTo(dateTime1);
          }
        };
    return jsonComparator;
  }

  private void datasets(String instance, Promise<JsonObject> datasetResult,
                        JsonArray highestCountResource) {
    String providerAndResources = "";
    if (instance.isBlank()) {
      providerAndResources = GET_PROVIDER_AND_RESOURCES;
    } else {
      providerAndResources = GET_DATASET_BY_INSTANCE.replace("$1", instance);
    }
    client.searchAsync(
        providerAndResources,
        docIndex,
        getCatRecords -> {
          if (getCatRecords.succeeded()) {
            ArrayList<JsonObject> resourceGroupArray = new ArrayList<JsonObject>();
            Map<String, Integer> resourceGroupCount = new HashMap<>();
            Map<String, String> providerDescription = new HashMap<>();
            Map<String, Integer> typeCount = new HashMap<>();
            typeCount.put("iudx:Provider", 0);
            typeCount.put("iudx:ResourceGroup", 0);
            typeCount.put("iudx:Resource", 0);

            for (int i = 0; i < getCatRecords.result().getJsonArray(RESULTS).size(); i++) {
              JsonObject record = getCatRecords.result().getJsonArray(RESULTS).getJsonObject(i);
              // getting count of all the resources in a resourceGroup
              if (record.getJsonArray(TYPE).getString(0).equals("iudx:Resource")) {
                String resourceGroup = record.getString("resourceGroup");
                if (resourceGroupCount.containsKey(resourceGroup)) {
                  resourceGroupCount.put(resourceGroup, resourceGroupCount.get(resourceGroup) + 1);
                } else {
                  resourceGroupCount.put(resourceGroup, 1);
                }
              }
              // getting all resource group datasets in an arrayList
              if (record.getJsonArray(TYPE).getString(0).equals("iudx:ResourceGroup")
                  && record.containsKey("itemCreatedAt")) {
                resourceGroupArray.add(record);
              }
              // getting count of resource,resourceGroup and provider
              String type = record.getJsonArray(TYPE).getString(0);
              if (typeCount.containsKey(type)) {
                typeCount.put(type, typeCount.get(type) + 1);
              } else {
                typeCount.put(type, 1);
              }
              // getting provider description of all provider
              if (record.getJsonArray(TYPE).getString(0).equals("iudx:Provider")) {
                String description = record.getString("description");
                String providerId = record.getString("id");
                providerDescription.put(providerId, description);
              }
            }
            // sorting resource group based on the time of creation.
            Collections.sort(resourceGroupArray, comapratorForLatestDataset());

              // getting count of providers of a particular instance
              ArrayList<String> providerList = new ArrayList<String>();
              if (!instance.isBlank()) {
                for (int i = 0; i < getCatRecords.result().getJsonArray(RESULTS).size(); i++) {
                  JsonObject record = getCatRecords.result().getJsonArray(RESULTS).getJsonObject(i);
                  if (record.getJsonArray(TYPE).getString(0).equals("iudx:ResourceGroup")
                          && !providerList.contains(record.getString("provider"))
                          && !(record.getString("provider")).equals(null)) {
                    providerList.add(record.getString("provider"));

                  }

                }
                typeCount.put("iudx:Provider", providerList.size());
              }

            ArrayList<JsonObject> latestResourceGroup = new ArrayList<>();
            int resourceGroupSize = 0;
            if (resourceGroupArray.size() < 6) {
              resourceGroupSize = resourceGroupArray.size();
            } else {
              resourceGroupSize = 6;
            }
            for (int i = 0; i < resourceGroupSize; i++) {
              JsonObject resource = resourceGroupArray.get(i);
              resource
                  .put(
                      "totalResources",
                      resourceGroupCount.get(resourceGroupArray.get(i).getString("id")))
                  .put(
                      "providerDescription",
                      providerDescription.get(resourceGroupArray.get(i).getString("provider")));
              latestResourceGroup.add(resource);
              resource = new JsonObject();
            }

            ArrayList<JsonObject> featuredResourceGroup = new ArrayList<>();
            for (int j = 0; j < highestCountResource.size(); j++) {
              for (int i = 0; i < resourceGroupArray.size(); i++) {
                if (resourceGroupArray
                    .get(i)
                    .getString("id")
                    .equals(highestCountResource.getJsonObject(j)
                            .getString("resourcegroup"))) {
                  JsonObject resource = resourceGroupArray.get(i);
                  resource
                      .put(
                          "totalResources",
                          resourceGroupCount.get(resourceGroupArray.get(i).getString("id")));
                  featuredResourceGroup.add(resource);
                  resource = new JsonObject();
                }
              }
            }
            JsonObject jsonDataset =
                new JsonObject()
                    .put("latestDataset", latestResourceGroup)
                    .put("typeCount", typeCount)
                    .put("featuredDataset", featuredResourceGroup);
            datasetResult.complete(jsonDataset);

          } else {
            LOGGER.error("Fail: failed DB request");
            datasetResult.handle(Future.failedFuture(internalErrorResp));
          }
        });
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
    client.searchAsync(
        checkInstance,
        docIndex,
        checkRes -> {
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
