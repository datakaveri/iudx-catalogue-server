package iudx.catalogue.server.database.elastic;

import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.database.elastic.query.Queries.*;
import static iudx.catalogue.server.util.Constants.*;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.RespBuilder;
import iudx.catalogue.server.database.elastic.model.QueryAndAggregation;
import iudx.catalogue.server.database.elastic.query.QueryDecoder;
import iudx.catalogue.server.database.mlayer.*;
import iudx.catalogue.server.geocoding.GeocodingService;
import iudx.catalogue.server.nlpsearch.NLPSearchService;
import java.util.*;
import java.util.Timer;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The Database Service Implementation.
 *
 * <h1>Database Service Implementation</h1>
 *
 * <p>The Database Service implementation in the IUDX Catalogue Server implements the definitions of
 * the {@link ElasticsearchService}.
 *
 * @version 1.0
 * @since 2020-05-31
 */
public class ElasticsearchServiceImpl implements ElasticsearchService {

  private static final Logger LOGGER = LogManager.getLogger(ElasticsearchServiceImpl.class);
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
   * Constructs a new ElasticsearchServiceImpl instance with the given ElasticClient and index names.
   *
   * @param client the ElasticClient used for accessing Elasticsearch
   * @param docIndex the name of the index used for document storage
   * @param ratingIndex the name of the index used for rating storage
   * @param mlayerInstanceIndex the name of the index used for ML layer instance storage
   * @param mlayerDomainIndex the name of the index used for ML layer domain storage
   */
  public ElasticsearchServiceImpl(
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
  public ElasticsearchServiceImpl(
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
   * Constructs a new instance of ElasticsearchServiceImpl with only the ElasticClient provided. This
   * constructor sets the values of nlpPluggedIn and geoPluggedIn to false.
   *
   * @param client the ElasticClient used to interact with the Elasticsearch instance
   */
  public ElasticsearchServiceImpl(ElasticClient client) {
    this.client = client;
    nlpPluggedIn = false;
    geoPluggedIn = false;
  }

  /**
   * Constructor for initializing ElasticsearchServiceImpl object.
   *
   * @param client the ElasticClient object for connecting to Elasticsearch
   * @param nlpService the NLPSearchService object for natural language processing searches
   */
  public ElasticsearchServiceImpl(
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
  public ElasticsearchService searchQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

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
    Query query = queryDecoder.searchQuery(request);
    if (query == null) {

      LOGGER.error("Fail: Query returned with an error");
      handler.handle(Future.failedFuture("Error: Failed to construct query"));
      return null;
    }

    LOGGER.debug("Info: Query constructed;" + query);
    List<String> source = null;
    int size = FILTER_PAGINATION_SIZE, from = 0;
    String searchType = request.getString(SEARCH_TYPE);
    LOGGER.debug(searchType);
    if (searchType.equalsIgnoreCase("getParentObjectInfo")) {
      source =
          List.of(
              "type",
              "provider",
              "ownerUserId",
              "resourceGroup",
              "resourceServer",
              "resourceServerRegURL",
              "cos",
              "cos_admin");
    }
    if (searchType.matches(RESPONSE_FILTER_REGEX)) {
      /* Construct the filter for response */
      LOGGER.debug("Info: Adding responseFilter");

      if (request.containsKey(ATTRIBUTE)) {
        JsonArray sourceFilter = request.getJsonArray(ATTRIBUTE);
        source = sourceFilter == null ? null : sourceFilter.getList();
      } else if (request.containsKey(FILTER)) {
        JsonArray sourceFilter = request.getJsonArray(FILTER);
        source = sourceFilter == null ? null : sourceFilter.getList();
      }
    }
    /* checking the requests for offset attribute */
    if (request.containsKey(OFFSET)) {
      Integer offsetFilter = request.getInteger(OFFSET);
      from = offsetFilter;
    }
    /* TODO: Pagination for large result set */
    if (request.getBoolean(SEARCH)) {
      Integer limit =
          request.getInteger(LIMIT, FILTER_PAGINATION_SIZE - request.getInteger(OFFSET, 0));
      size = limit;
    }
    if (source == null) source = List.of();
    client.searchAsync(
        query,
        buildSourceConfig(source),
        size,
        from,
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
   * @return the ElasticsearchService instance
   */
  public ElasticsearchService nlpSearchQuery(
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
   * @return the current ElasticsearchService instance
   */
  public ElasticsearchService nlpSearchLocationQuery(
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
  public ElasticsearchService countQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    request.put(SEARCH, false);

    /* Validate the Request */
    if (!request.containsKey(SEARCH_TYPE)) {
      handler.handle(Future.failedFuture(internalErrorResp));
      return null;
    }

    /* Construct the query to be made */
    Query query = queryDecoder.searchQuery(request);
    if (query == null) {

      LOGGER.error("Fail: Query returned with an error");

      handler.handle(Future.failedFuture("Error: Failed to construct query"));
      return null;
    }

    LOGGER.debug("Info: Query constructed;" + query);
    client.countAsync(
        query,
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
   *
   * @param doc Json
   * @return the current ElasticsearchService instance
   */
  @Override
  public ElasticsearchService createItem(JsonObject doc, Handler<AsyncResult<JsonObject>> handler) {

    RespBuilder respBuilder = new RespBuilder();
    String id = doc.getString("id");
    /* check if the id is present */
    if (id != null) {
      final String instanceId = doc.getString(INSTANCE);
      String errorJson =
          respBuilder
              .withType(FAILED)
              .withResult(id, INSERT, FAILED)
              .withDetail("Insertion Failed")
              .getResponse();
      Query checkItem = buildGetDocQuery(id);

      verifyInstance(instanceId)
          .onComplete(
              instanceHandler -> {
                if (instanceHandler.succeeded()) {
                  LOGGER.debug("Info: Instance info;" + instanceHandler.result());

                  client.searchAsync(
                      checkItem,
                      buildSourceConfig(List.of()),
                      FILTER_PAGINATION_SIZE,
                      0,
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
                                        .withDetail("Fail: Doc Exists")
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
                                                              .withDetail("Success: Item created")
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
                                                            .withDetail("Success: Item created")
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
                              .withDetail(instanceHandler.cause().getLocalizedMessage())
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
   *
   * @param doc JsonObject
   * @return the current ElasticsearchService instance
   */
  @Override
  public ElasticsearchService updateItem(JsonObject doc, Handler<AsyncResult<JsonObject>> handler) {

    RespBuilder respBuilder = new RespBuilder();
    String id = doc.getString("id");
    String type = doc.getJsonArray("type").getString(0);
    Query checkQuery = buildDocQueryWithType(id, type);
    SourceConfig source = buildSourceConfig(Collections.singletonList("id"));

    new Timer()
        .schedule(
            new TimerTask() {
              public void run() {
                client.searchGetId(
                    checkQuery,
                    source,
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
                                      .withDetail("Fail: Doc doesn't exist, can't update")
                                      .getResponse()));
                          return;
                        }
                        String docId = checkRes.result().getJsonArray(RESULTS).getString(0);
                        client.docPutAsync(
                            docId,
                            docIndex,
                            doc,
                            putRes -> {
                              if (putRes.succeeded()) {
                                handler.handle(
                                    Future.succeededFuture(
                                        respBuilder
                                            .withType(TYPE_SUCCESS)
                                            .withTitle(TITLE_SUCCESS)
                                            .withResult(doc)
                                            .withDetail("Success: Item updated successfully")
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

  @Override
  public ElasticsearchService deleteItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    LOGGER.debug("Info: Deleting item");

    RespBuilder respBuilder = new RespBuilder();
    String id = request.getString("id");

    new Timer()
        .schedule(
            new TimerTask() {
              public void run() {
                Query checkQuery;

                /* the check query checks if any type item is present more than once.
                If it's present then the item cannot be deleted.  */
                checkQuery = buildResourceGroupQuery(id);

                client.searchGetId(
                    checkQuery,
                    buildSourceConfig(List.of()),
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
                                          .withDetail("Success: Item deleted successfully")
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

  @Override
  public ElasticsearchService getItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    LOGGER.debug("Info: Get item");

    RespBuilder respBuilder = new RespBuilder();
    String itemId = request.getString(ID);
    Query getQuery = buildGetDocQuery(itemId);

    client.searchAsync(
        getQuery,
        buildSourceConfig(List.of()),
        FILTER_PAGINATION_SIZE,
        0,
        docIndex,
        clientHandler -> {
          if (clientHandler.succeeded()) {
            LOGGER.debug("Success: Successful DB request");
            JsonObject responseJson = clientHandler.result();
            responseJson.put(DETAIL, "Success: Item fetched Successfully");
            handler.handle(Future.succeededFuture(responseJson));
          } else {
            LOGGER.error("Fail: Failed getting item;" + clientHandler.cause());
            /* Handle request error */
            handler.handle(
                Future.failedFuture(
                    respBuilder
                        .withType(TYPE_INTERNAL_SERVER_ERROR)
                        .withTitle(TITLE_INTERNAL_SERVER_ERROR)
                        .withDetail(DETAIL_INTERNAL_SERVER_ERROR)
                        .getResponse()));
          }
        });
    return this;
  }

  @Override
  public ElasticsearchService listItems(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    RespBuilder respBuilder = new RespBuilder();
    QueryAndAggregation queryAndAggregation = queryDecoder.listItemQuery(request);
    Query elasticQuery = queryAndAggregation.getQuery();
    Aggregation aggregation = queryAndAggregation.getAggregation();

    LOGGER.debug("Info: Listing items;" + elasticQuery);

    client.listAggregationAsync(
        elasticQuery,
        aggregation,
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

  @Override
  public ElasticsearchService listOwnerOrCos(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    RespBuilder respBuilder = new RespBuilder();
    QueryAndAggregation queryAndAggregation = queryDecoder.listItemQuery(request);
    Query elasticQuery = queryAndAggregation.getQuery();
    Aggregation aggregation = queryAndAggregation.getAggregation();

    LOGGER.debug("Info: Listing items;" + elasticQuery);

    client.searchAsync(
        elasticQuery,
        aggregation,
        docIndex,
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

  @Override
  public ElasticsearchService listRelationship(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    RespBuilder respBuilder = new RespBuilder();
    Query typeQuery = buildGetRSGroupQuery(request.getString(ID));
    LOGGER.debug("typeQuery: " + typeQuery);

    client.searchAsync(
        typeQuery,
        buildSourceConfig(
            List.of("cos", "resourceServer", "type", "provider", "resourceGroup", "id")),
        FILTER_PAGINATION_SIZE,
        0,
        docIndex,
        queryhandler -> {
          if (queryhandler.succeeded()) {
            if (queryhandler.result().getInteger(TOTAL_HITS) == 0) {
              handler.handle(
                  Future.failedFuture(
                      respBuilder
                          .withType(TYPE_ITEM_NOT_FOUND)
                          .withTitle(TITLE_ITEM_NOT_FOUND)
                          .withDetail("Item id given is not present")
                          .getResponse()));
              return;
            }
            JsonObject relType = queryhandler.result().getJsonArray(RESULTS).getJsonObject(0);

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
              Query elasticQuery = queryDecoder.listRelationshipQuery(request);
              LOGGER.debug("Info: Query constructed;" + elasticQuery);
              JsonObject filters = handleResponseFiltering(request);
              int size = filters.getInteger(SIZE_KEY), from = filters.getInteger("from");
              List<String> includes =
                  filters.getJsonArray("includes") == null
                      ? List.of()
                      : filters.getJsonArray("includes").getList();
              if (elasticQuery != null) {
                handleClientSearchAsync(handler, respBuilder, elasticQuery, includes, size, from);
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
            LOGGER.error(queryhandler.cause().getMessage());
          }
        });
    return this;
  }

  private void handleClientSearchAsync(
      Handler<AsyncResult<JsonObject>> handler,
      RespBuilder respBuilder,
      Query elasticQuery,
      List<String> list,
      int size,
      int from) {
    client.searchAsync(
        elasticQuery,
        buildSourceConfig(list),
        size,
        from,
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
    Query typeQuery4RsGroup = buildTypeQuery4RsGroup(relType.getString(ID));
    LOGGER.debug("typeQuery4RsGroup: " + typeQuery4RsGroup);

    client.searchAsync(
        typeQuery4RsGroup,
        buildSourceConfig(List.of("id")),
        10000,
        0,
        docIndex,
        serverSearch -> {
          if (serverSearch.succeeded()) {
            JsonArray serverResult = serverSearch.result().getJsonArray("results");
            LOGGER.debug("serverResult: " + serverResult);
            request.put("providerIds", serverResult);
            request.mergeIn(relType);
            Query elasticQuery = queryDecoder.listRelationshipQuery(request);
            LOGGER.debug("Info: Query constructed;" + elasticQuery);
            JsonObject filters = handleResponseFiltering(request);
            int size = filters.getInteger(SIZE_KEY), from = filters.getInteger("from");
            List<String> includes =
                filters.getJsonArray("includes") == null
                    ? List.of()
                    : filters.getJsonArray("includes").getList();

            handleClientSearchAsync(handler, respBuilder, elasticQuery, includes, size, from);
          }
        });
  }

  private void handleRsFetchForResourceGroup(
      JsonObject request,
      Handler<AsyncResult<JsonObject>> handler,
      RespBuilder respBuilder,
      JsonObject relType) {
    Query typeQuery4Rserver = buildTypeQuery4RsServer(relType.getString(PROVIDER));

    LOGGER.debug("typeQuery4Rserver: " + typeQuery4Rserver);

    client.searchAsync(
        typeQuery4Rserver,
        buildSourceConfig(
            List.of("cos", "resourceServer", "type", "provider", "resourceGroup", "id")),
        FILTER_PAGINATION_SIZE,
        0,
        docIndex,
        serverSearch -> {
          if (serverSearch.succeeded() && serverSearch.result().getInteger(TOTAL_HITS) != 0) {
            JsonObject serverResult =
                serverSearch.result().getJsonArray("results").getJsonObject(0);
            request.mergeIn(serverResult);
            request.mergeIn(relType);
            Query elasticQuery = queryDecoder.listRelationshipQuery(request);
            LOGGER.debug("Info: Query constructed;" + elasticQuery);
            JsonObject filters = handleResponseFiltering(request);
            int size = filters.getInteger(SIZE_KEY), from = filters.getInteger("from");
            List<String> includes =
                filters.getJsonArray("includes") == null
                    ? List.of()
                    : filters.getJsonArray("includes").getList();

            if (elasticQuery != null) {
              handleClientSearchAsync(handler, respBuilder, elasticQuery, includes, size, from);
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

  private JsonObject handleResponseFiltering(JsonObject request) {
    Integer limit =
        request.getInteger(LIMIT, FILTER_PAGINATION_SIZE - request.getInteger(OFFSET, 0));
    JsonObject responseFilters =
        new JsonObject().put(SIZE_KEY, limit).put("from", 0).put("includes", null);
    String relationshipType = request.getString(RELATIONSHIP, "");
    if (TYPE_KEY.equals(relationshipType)) {
      responseFilters.put("includes", new JsonArray().add(TYPE_KEY));
    }

    /* checking the requests for limit attribute */
    if (request.containsKey(LIMIT)) {
      Integer sizeFilter = request.getInteger(LIMIT);
      responseFilters.put(SIZE_KEY, sizeFilter);
    }

    /* checking the requests for offset attribute */
    if (request.containsKey(OFFSET)) {
      Integer offsetFilter = request.getInteger(OFFSET);
      responseFilters.put("from", offsetFilter);
    }
    /* checking the requests for any filters */
    if (request.containsKey(FILTER)) {
      JsonArray sourceFilter = request.getJsonArray(FILTER, new JsonArray());
      responseFilters.put("includes", sourceFilter);
    }
    return responseFilters;
  }

  @Override
  public ElasticsearchService relSearch(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    RespBuilder respBuilder = new RespBuilder();
    Query elasticQuery;
    String errorJson =
        respBuilder.withType(FAILED).withDetail(ERROR_INVALID_PARAMETER).getResponse();

    /* Validating the request */
    if (request.containsKey(RELATIONSHIP) && request.containsKey(VALUE)) {

      /* parsing data parameters from the request */
      String relReq = request.getJsonArray(RELATIONSHIP).getString(0);
      if (relReq.contains(".")) {

        LOGGER.debug("Info: Reached relationship search dbServiceImpl");

        String typeValue;
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
          typeValue = null;
          LOGGER.error("Fail: Incorrect/missing query parameters");
          handler.handle(Future.failedFuture(errorJson));
          return null;
        }

        // Construct the term and match queries
        Query termQuery = QueryBuilders.term(t -> t.field(TYPE_KEYWORD).value(typeValue));
        Query matchQuery = QueryBuilders.match(m -> m.field(relReqsKey).query(relReqsValue));

        // Construct the bool must query
        elasticQuery = QueryBuilders.bool(b -> b.must(termQuery, matchQuery));

      } else {
        LOGGER.error("Fail: Incorrect/missing query parameters");
        handler.handle(Future.failedFuture(errorJson));
        return null;
      }

      /* Initial db query to filter matching attributes */
      client.searchAsync(
          elasticQuery,
          buildSourceConfig(Collections.singletonList("id")),
          FILTER_PAGINATION_SIZE,
          0,
          docIndex,
          searchRes -> {
            if (searchRes.succeeded()) {

              JsonArray resultValues = searchRes.result().getJsonArray(RESULTS);
              List<String> idCollection;

              /* iterating over the filtered response json array */
              if (!resultValues.isEmpty()) {
                idCollection =
                    resultValues.stream()
                        .map(idIndex -> ((JsonObject) idIndex).getString(ID))
                        .collect(Collectors.toList());
              } else {
                idCollection = null;
                handler.handle(Future.succeededFuture(searchRes.result()));
              }
              // Construct the bool should query
              Query boolShouldQuery =
                  QueryBuilders.bool(
                      b ->
                          b.should(
                              idCollection.stream()
                                  .map(
                                      idObj ->
                                          QueryBuilders.wildcard(
                                              w -> w.field(ID_KEYWORD).value(idObj + "*")))
                                  .collect(Collectors.toList())));
              /* checking the requests for limit attribute */
              int size = FILTER_PAGINATION_SIZE, from = 0;
              if (request.containsKey(LIMIT)) {
                Integer sizeFilter = request.getInteger(LIMIT);
                size = sizeFilter;
              }
              /* checking the requests for offset attribute */
              if (request.containsKey(OFFSET)) {
                Integer offsetFilter = request.getInteger(OFFSET);
                from = offsetFilter;
              }

              LOGGER.debug("INFO: Query constructed;" + boolShouldQuery);

              /* db query to find the relationship to the initial query */
              client.searchAsync(
                  boolShouldQuery,
                  buildSourceConfig(List.of()),
                  size,
                  from,
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

  @Override
  public ElasticsearchService createRating(
      JsonObject ratingDoc, Handler<AsyncResult<JsonObject>> handler) {
    RespBuilder respBuilder = new RespBuilder();
    String ratingId = ratingDoc.getString("ratingID");

    Query checkForExistingRecord = buildRDocQuery(ratingId);
    // TODO: should add sourceconfig to the query

    client.searchAsync(
        checkForExistingRecord,
        buildSourceConfig(List.of()),
        FILTER_PAGINATION_SIZE,
        0,
        ratingIndex,
        checkRes -> {
          if (checkRes.failed()) {
            LOGGER.error("Fail: Insertion of rating failed: " + checkRes.cause());
            handler.handle(
                Future.failedFuture(
                    respBuilder
                        .withType(FAILED)
                        .withResult(ratingId)
                        .withDetail("Fail: Insertion of rating failed")
                        .getResponse()));
          } else {
            if (checkRes.result().getInteger(TOTAL_HITS) != 0) {
              handler.handle(
                  Future.failedFuture(
                      respBuilder
                          .withType(TYPE_ALREADY_EXISTS)
                          .withTitle(TITLE_ALREADY_EXISTS)
                          .withResult(ratingId, INSERT, FAILED, " Fail: Doc Already Exists")
                          .withDetail(" Fail: Doc Already Exists")
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
                                .withDetail("Insertion Failed")
                                .getResponse()));
                    LOGGER.error("Fail: Insertion failed" + postRes.cause());
                  }
                });
          }
        });
    return this;
  }

  @Override
  public ElasticsearchService updateRating(
      JsonObject ratingDoc, Handler<AsyncResult<JsonObject>> handler) {
    RespBuilder respBuilder = new RespBuilder();
    String ratingId = ratingDoc.getString("ratingID");

    Query checkForExistingRecord = buildRDocQuery(ratingId);
    SourceConfig source = buildSourceConfig(List.of());

    client.searchGetId(
        checkForExistingRecord,
        source,
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
                          .withDetail("Fail: Doc doesn't exist, can't update")
                          .getResponse()));
              return;
            }

            String docId = checkRes.result().getJsonArray(RESULTS).getString(0);

            client.docPutAsync(
                docId,
                ratingIndex,
                ratingDoc,
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
  public ElasticsearchService deleteRating(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    RespBuilder respBuilder = new RespBuilder();
    String ratingId = request.getString("ratingID");

    Query checkForExistingRecord = buildRDocQuery(ratingId);
    SourceConfig source = buildSourceConfig(List.of());

    client.searchGetId(
        checkForExistingRecord,
        source,
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
                          .withDetail("Fail: Doc doesn't exist, can't delete")
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
  public ElasticsearchService getRatings(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    Query query;
    if (request.containsKey("ratingID")) {
      String ratingId = request.getString("ratingID");
      query = buildGetRatingDocsQuery("ratingID", ratingId);
      LOGGER.debug(query);
    } else {
      String id = request.getString(ID);
      if (request.containsKey(TYPE) && request.getString(TYPE).equalsIgnoreCase("average")) {
        Future<List<String>> getAssociatedIdFuture = getAssociatedIDs(id);
        getAssociatedIdFuture.onComplete(
            ids -> {
              // StringBuilder avgQuery = new StringBuilder(GET_AVG_RATING_PREFIX);
              if (ids.succeeded()) {
                List<Query> matchQueries =
                    ids.result().stream()
                        .map(v -> QueryBuilders.match(m -> m.field("id.keyword").query(v)))
                        .collect(Collectors.toList());

                Query avgQuery =
                    QueryBuilders.bool(
                        b ->
                            b.should(matchQueries)
                                .minimumShouldMatch("1")
                                .must(
                                    QueryBuilders.match(m -> m.field("status").query("approved"))));
                LOGGER.debug(avgQuery);
                client.ratingAggregationAsync(
                    avgQuery,
                    buildAvgRatingAggregation(),
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
        query = buildGetRatingDocsQuery(ID_KEYWORD, id);
        LOGGER.debug(query);
      }
    }

    LOGGER.debug(query);

    client.searchAsync(
        query,
        buildSourceConfig(List.of("rating", "id")),
        FILTER_PAGINATION_SIZE,
        0,
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
    Query query = getAssociatedIdQuery(id, id);
    LOGGER.debug(query);

    client.searchAsync(
        query,
        buildSourceConfig(List.of("id")),
        FILTER_PAGINATION_SIZE,
        0,
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
   * @return the ElasticsearchService instance
   */
  @Override
  public ElasticsearchService createMlayerInstance(
      JsonObject instanceDoc, Handler<AsyncResult<JsonObject>> handler) {
    MlayerInstance getMlayerInstance = new MlayerInstance(client, mlayerInstanceIndex);
    getMlayerInstance.createMlayerInstance(instanceDoc, handler);
    return this;
  }

  @Override
  public ElasticsearchService getMlayerInstance(
      JsonObject requestParams, Handler<AsyncResult<JsonObject>> handler) {
    MlayerInstance getMlayerInstance = new MlayerInstance(client, mlayerInstanceIndex);
    getMlayerInstance.getMlayerInstance(requestParams, handler);
    return this;
  }

  @Override
  public ElasticsearchService deleteMlayerInstance(
      String instanceId, Handler<AsyncResult<JsonObject>> handler) {
    MlayerInstance mlayerInstance = new MlayerInstance(client, mlayerInstanceIndex);
    mlayerInstance.deleteMlayerInstance(instanceId, handler);
    return this;
  }

  @Override
  public ElasticsearchService updateMlayerInstance(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    MlayerInstance mlayerInstance = new MlayerInstance(client, mlayerInstanceIndex);
    mlayerInstance.updateMlayerInstance(request, handler);
    return this;
  }

  @Override
  public ElasticsearchService createMlayerDomain(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    MlayerDomain mlayerDomain = new MlayerDomain(client, mlayerDomainIndex);
    mlayerDomain.createMlayerDomain(request, handler);
    return this;
  }

  @Override
  public ElasticsearchService getMlayerDomain(
      JsonObject requestParams, Handler<AsyncResult<JsonObject>> handler) {
    MlayerDomain mlayerDomain = new MlayerDomain(client, mlayerDomainIndex);
    mlayerDomain.getMlayerDomain(requestParams, handler);
    return this;
  }

  @Override
  public ElasticsearchService updateMlayerDomain(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    MlayerDomain mlayerDomain = new MlayerDomain(client, mlayerDomainIndex);
    mlayerDomain.updateMlayerDomain(request, handler);
    return this;
  }

  @Override
  public ElasticsearchService deleteMlayerDomain(
      String domainId, Handler<AsyncResult<JsonObject>> handler) {
    MlayerDomain mlayerDomain = new MlayerDomain(client, mlayerDomainIndex);
    mlayerDomain.deleteMlayerDomain(domainId, handler);
    return this;
  }

  @Override
  public ElasticsearchService getMlayerProviders(
      JsonObject requestParams, Handler<AsyncResult<JsonObject>> handler) {
    MlayerProvider mlayerProvider = new MlayerProvider(client, docIndex);
    mlayerProvider.getMlayerProviders(requestParams, handler);
    return this;
  }

  @Override
  public ElasticsearchService getMlayerGeoQuery(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    LOGGER.debug("request body" + request);
    MlayerGeoQuery mlayerGeoQuery = new MlayerGeoQuery(client, docIndex);
    mlayerGeoQuery.getMlayerGeoQuery(request, handler);

    return this;
  }

  @Override
  public ElasticsearchService getMlayerAllDatasets(
      JsonObject requestParam, String query, Handler<AsyncResult<JsonObject>> handler) {
    MlayerDataset mlayerDataset = new MlayerDataset(client, docIndex, mlayerInstanceIndex);
    mlayerDataset.getMlayerAllDatasets(requestParam, query, handler);
    return this;
  }

  @Override
  public ElasticsearchService getMlayerDataset(
      JsonObject requestData, Handler<AsyncResult<JsonObject>> handler) {
    MlayerDataset mlayerDataset = new MlayerDataset(client, docIndex, mlayerInstanceIndex);
    mlayerDataset.getMlayerDataset(requestData, handler);
    return this;
  }

  @Override
  public ElasticsearchService getMlayerPopularDatasets(
      String instance,
      JsonArray frequentlyUsedResourceGroup,
      Handler<AsyncResult<JsonObject>> handler) {
    MlayerPopularDatasets mlayerPopularDatasets =
        new MlayerPopularDatasets(client, docIndex, mlayerInstanceIndex, mlayerDomainIndex);
    mlayerPopularDatasets.getMlayerPopularDatasets(instance, frequentlyUsedResourceGroup, handler);
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
    Query checkInstance = buildMatchQuery("id", instanceId);

    client.searchAsync(
        checkInstance,
        buildSourceConfig(List.of()),
        FILTER_PAGINATION_SIZE,
        0,
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
