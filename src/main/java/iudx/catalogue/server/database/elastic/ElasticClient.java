package iudx.catalogue.server.database.elastic;

import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.database.elastic.query.Queries.getScriptLocationSearchQuery;
import static iudx.catalogue.server.database.elastic.query.Queries.getScriptScoreQuery;
import static iudx.catalogue.server.geocoding.util.Constants.RESULTS;
import static iudx.catalogue.server.geocoding.util.Constants.TYPE;
import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.validator.Constants.VALIDATION_FAILURE_MSG;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.WriteResponseBase;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import co.elastic.clients.elasticsearch.core.search.SourceFilter;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.JsonpMapperFeatures;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.Util;
import jakarta.json.stream.JsonGenerator;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.*;

public final class ElasticClient {

  private static final Logger LOGGER = LogManager.getLogger(ElasticClient.class);
  private final RestClient rsClient;
  ElasticsearchAsyncClient client;
  ElasticsearchClient esClient;
  private String index;

  /**
   * ElasticClient - Wrapper around ElasticSearch low level client.
   *
   * @param databaseIp IP of the DB
   * @param databasePort Port @TODO XPack Security
   */
  public ElasticClient(
      String databaseIp,
      int databasePort,
      String index,
      String databaseUser,
      String databasePassword) {
    CredentialsProvider credentials = new BasicCredentialsProvider();
    credentials.setCredentials(
        AuthScope.ANY, new UsernamePasswordCredentials(databaseUser, databasePassword));
    RestClientBuilder restClientBuilder =
        RestClient.builder(new HttpHost(databaseIp, databasePort))
            .setHttpClientConfigCallback(
                httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentials));
    rsClient = restClientBuilder.build();
    ElasticsearchTransport transport = new RestClientTransport(rsClient, new JacksonJsonpMapper());
    // And create the API client
    this.client = new ElasticsearchAsyncClient(transport);
    esClient = new ElasticsearchClient(transport);
    this.index = index;
  }

  /**
   * searchAsync - private function which perform performRequestAsync for search apis.
   *
   * @param searchRequest Elastic Request
   * @param options SOURCE - Source only DOCIDS - DOCIDs only IDS - IDs only @TODO XPack Security
   */
  private Future<JsonObject> searchAsync(SearchRequest searchRequest, String options) {
    Promise<JsonObject> promise = Promise.promise();
    try {
      client
          .search(searchRequest, ObjectNode.class)
          .whenComplete(
              (response, exception) -> {
                if (exception != null) {
                  LOGGER.error("async search query failed : {}", exception);
                  promise.fail(exception);
                } else {
                  processSearchResponse(response, options, promise);
                }
              });
    } catch (Exception e) {
      promise.fail(e);
    }
    return promise.future();
  }

  private Future<JsonObject> processSearchResponse(
      SearchResponse<ObjectNode> response, String options, Promise<JsonObject> promise) {
    try {
      DbResponseMessageBuilder responseMsg = new DbResponseMessageBuilder();
      TotalHits totalHitsObj = response.hits().total();
      int totalHits = (int) (totalHitsObj != null ? totalHitsObj.value() : 0);
      // LOGGER.debug("options: " + options);
      responseMsg.statusSuccess().setTotalHits(totalHits);
      if (totalHits > 0) {
        JsonArray results = new JsonArray();
        if ((options == SOURCE_ONLY
                || options == DOC_IDS_ONLY
                || options == SOURCE_AND_ID
                || options == SOURCE_AND_ID_GEOQUERY
                || options == DATASET
                || options == PROVIDER_AGGREGATION_ONLY)
            && response.hits() != null) {
          // LOGGER.debug("total: " + totalHits);
          List<Hit<ObjectNode>> hits = response.hits().hits();
          for (Hit<ObjectNode> hit : hits) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.put("_id", hit.id());
            jsonObject.put("_index", hit.index());
            jsonObject.put("_source", JsonObject.mapFrom(hit.source()));
            results.add(jsonObject);
          }
        }
        if (options == AGGREGATION_ONLY
            || options == RATING_AGGREGATION_ONLY
            || options == RESOURCE_AGGREGATION_ONLY) {
          JsonpMapper mapper =
              client._jsonpMapper().withAttribute(JsonpMapperFeatures.SERIALIZE_TYPED_KEYS, false);

          StringWriter writer = new StringWriter();
          try (JsonGenerator generator = mapper.jsonProvider().createGenerator(writer)) {
            mapper.serialize(response, generator);
          }
          String result = writer.toString();
          JsonObject aggs = new JsonObject(result);
          results = aggs.getJsonObject("aggregations").getJsonObject(RESULTS).getJsonArray(BUCKETS);
        }

        for (int i = 0; i < results.size(); i++) {
          if (options == SOURCE_ONLY) {
            // Todo: This might slow system down
            JsonObject source = results.getJsonObject(i).getJsonObject(SOURCE);
            source.remove(SUMMARY_KEY);
            source.remove(WORD_VECTOR_KEY);
            responseMsg.addResult(source);
          }
          if (options == DOC_IDS_ONLY) {
            responseMsg.addResult(results.getJsonObject(i).getString(DOC_ID));
          }
          if (options == AGGREGATION_ONLY) {
            responseMsg.addResult(results.getJsonObject(i).getString(KEY));
          }
          if (options == RATING_AGGREGATION_ONLY) {
            JsonObject result =
                new JsonObject()
                    .put(ID, results.getJsonObject(i).getString(KEY))
                    .put(TOTAL_RATINGS, results.getJsonObject(i).getString(DOC_COUNT))
                    .put(
                        AVERAGE_RATING,
                        results.getJsonObject(i).getJsonObject(AVERAGE_RATING).getDouble(VALUE));
            responseMsg.addResult(result);
          }
          if (options == SOURCE_AND_ID) {
            JsonObject source = results.getJsonObject(i).getJsonObject(SOURCE);
            String docId = results.getJsonObject(i).getString(DOC_ID);
            JsonObject result = new JsonObject().put(SOURCE, source).put(DOC_ID, docId);
            responseMsg.addResult(result);
          }
          if (options == SOURCE_AND_ID_GEOQUERY) {
            String docId = results.getJsonObject(i).getString(DOC_ID);
            JsonObject source = results.getJsonObject(i).getJsonObject(SOURCE);
            source.put("doc_id", docId);
            JsonObject result = new JsonObject();
            result.mergeIn(source);
            responseMsg.addResult(result);
          }
          if (options == RESOURCE_AGGREGATION_ONLY) {
            responseMsg.addResult(results.getJsonObject(i));
          }
        }
        if (options == DATASET) {
          JsonArray resource = new JsonArray();
          JsonObject datasetDetail = new JsonObject();
          JsonObject dataset = new JsonObject();
          for (int i = 0; i < results.size(); i++) {
            JsonObject record = results.getJsonObject(i).getJsonObject(SOURCE);
            JsonObject provider = new JsonObject();
            String itemType = Util.getItemType(record);
            if (itemType.equals(VALIDATION_FAILURE_MSG)) {
              promise.fail(VALIDATION_FAILURE_MSG);
            }
            if (itemType.equals("iudx:Provider")) {
              provider
                  .put(ID, record.getString(ID))
                  .put(DESCRIPTION_ATTR, record.getString(DESCRIPTION_ATTR));
              dataset.put("resourceServerRegURL", record.getString("resourceServerRegURL"));
              dataset.put(PROVIDER, provider);
            }
            if (itemType.equals("iudx:Resource")) {
              if (record.getJsonArray(TYPE).size() > 1) {
                String schema =
                    record.getString("@context")
                        + record
                            .getJsonArray(TYPE)
                            .getString(1)
                            .substring(5, record.getJsonArray(TYPE).getString(1).length());
                record.put("schema", schema);
              }
              record.remove("type");
              record.put("resourceId", record.getString("id"));
              record.remove("id");
              resource.add(record);
            }
            if (itemType.equals("iudx:ResourceGroup")) {
              dataset
                  .put(ID, record.getString(ID))
                  .put(DESCRIPTION_ATTR, record.getString(DESCRIPTION_ATTR));
              if (record.getJsonArray(TYPE).size() > 1) {
                String schema =
                    record.getString("@context")
                        + record
                            .getJsonArray(TYPE)
                            .getString(1)
                            .substring(5, record.getJsonArray(TYPE).getString(1).length());
                record.put("schema", schema);
                record.remove("@context");
                dataset.put("schema", schema);
              }
              if (record.containsKey(LABEL)) {
                dataset.put(LABEL, record.getString(LABEL));
              }
              if (record.containsKey(ACCESS_POLICY)) {
                dataset.put(ACCESS_POLICY, record.getString(ACCESS_POLICY));
              }
              if (record.containsKey(INSTANCE)) {
                dataset.put(INSTANCE, record.getString(INSTANCE));
              }
              if (record.containsKey(DATA_SAMPLE)) {
                dataset.put(DATA_SAMPLE, record.getJsonObject(DATA_SAMPLE));
              }
              if (record.containsKey("dataSampleFile")) {
                dataset.put("dataSampleFile", record.getJsonArray("dataSampleFile"));
              }
              if (record.containsKey("dataQualityFile")) {
                dataset.put("dataQualityFile", record.getJsonArray("dataQualityFile"));
              }
              if (record.containsKey(DATA_DESCRIPTOR)) {
                dataset.put(DATA_DESCRIPTOR, record.getJsonObject(DATA_DESCRIPTOR));
              }
              if (record.containsKey("resourceType")) {
                dataset.put("resourceType", record.getString("resourceType"));
              }
              if (record.containsKey("location")) {
                dataset.put("location", record.getJsonObject("location"));
              }
            }

            if (itemType.equals("iudx:COS")) {
              dataset.put("cosURL", record.getString("cosURL"));
            }
          }
          datasetDetail.put("dataset", dataset);
          datasetDetail.put("resource", resource);
          responseMsg.addResult(datasetDetail);
        }
        if (options == PROVIDER_AGGREGATION_ONLY) {
          JsonObject result = new JsonObject();
          JsonArray resourceGroupAndProvider = new JsonArray();
          if (response.aggregations().containsKey("provider_count")) {
            Aggregate providerCount = response.aggregations().get("provider_count");
            result.put("providerCount", providerCount.cardinality().value());
          }
          for (int i = 0; i < results.size(); i++) {
            JsonObject source = results.getJsonObject(i).getJsonObject(SOURCE);
            source.remove(SUMMARY_KEY);
            source.remove(WORD_VECTOR_KEY);
            resourceGroupAndProvider.add(source);
          }
          result.put("resourceGroupAndProvider", resourceGroupAndProvider);
          responseMsg.addResult(result);
        }
      } else {
        responseMsg.addResult();
      }
      promise.complete(responseMsg.getResponse());
    } catch (Exception e) {
      LOGGER.debug(e);
      promise.fail(e);
    }
    return promise.future();
  }

  /**
   * searchAsync - Wrapper around elasticsearch async search requests.
   *
   * @param query Query
   * @param resultHandler JsonObject result {@link AsyncResult} @TODO XPack Security
   */
  public ElasticClient searchAsync(
      String query, String index, Handler<AsyncResult<JsonObject>> resultHandler) {
    LOGGER.debug("searchAsync called");
    LOGGER.debug(query);

    SearchRequest searchRequest =
        new SearchRequest.Builder()
            .index(index)
            .withJson(new StringReader(query)) // using a StringReader to pass the query JSON
            .build();
    Future<JsonObject> future = searchAsync(searchRequest, SOURCE_ONLY);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * searchAsync - Wrapper around elasticsearch async search requests.
   *
   * @param query Query
   * @param source SourceConfig
   * @param size int
   * @param from int
   * @param index String
   * @param resultHandler JsonObject result {@link AsyncResult} @TODO XPack Security
   */
  public ElasticClient searchAsync(
      Query query,
      SourceConfig source,
      int size,
      int from,
      String index,
      Handler<AsyncResult<JsonObject>> resultHandler) {
    LOGGER.debug("searchAsync called");
    LOGGER.debug(query);

    SearchRequest.Builder searchRequest =
        new SearchRequest.Builder().index(index).size(size).from(from);
    if (query != null) searchRequest.query(query);
    if (source != null) searchRequest.source(source);
    Future<JsonObject> future = searchAsync(searchRequest.build(), SOURCE_ONLY);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * searchAsync - Wrapper around elasticsearch async search requests.
   *
   * @param query Query
   * @param source SourceConfig
   * @param sort SortOptions
   * @param index String
   * @param resultHandler JsonObject result {@link AsyncResult} @TODO XPack Security
   */
  public ElasticClient searchAsync(
      Query query,
      SourceConfig source,
      SortOptions sort,
      String index,
      Handler<AsyncResult<JsonObject>> resultHandler) {
    LOGGER.debug("searchAsync called");
    LOGGER.debug(query);

    SearchRequest searchRequest =
        new SearchRequest.Builder().index(index).query(query).source(source).sort(sort).build();
    Future<JsonObject> future = searchAsync(searchRequest, SOURCE_ONLY);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * searchAsync - Wrapper around elasticsearch async search requests.
   *
   * @param query Query
   * @param aggregation Aggregation
   * @param index String
   * @param resultHandler JsonObject result {@link AsyncResult} @TODO XPack Security
   */
  public ElasticClient searchAsync(
      Query query,
      Aggregation aggregation,
      String index,
      Handler<AsyncResult<JsonObject>> resultHandler) {
    LOGGER.debug("searchAsync called");
    LOGGER.debug(query);

    SearchRequest searchRequest =
        new SearchRequest.Builder()
            .index(index)
            .query(query)
            .aggregations("results", aggregation)
            .build();
    Future<JsonObject> future = searchAsync(searchRequest, SOURCE_ONLY);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * Searches for a dataset in the specified Elasticsearch index using the provided query string.
   *
   * @param query the query string to use for the search
   * @param sourceConfig SourceConfig
   * @param size int
   * @param index the name of the Elasticsearch index to search in
   * @param resultHandler the async result handler to receive the search results in JSON format
   * @return the ElasticClient instance for chaining method calls
   */
  public ElasticClient searchAsyncDataset(
      Query query,
      SourceConfig sourceConfig,
      int size,
      String index,
      Handler<AsyncResult<JsonObject>> resultHandler) {
    SearchRequest.Builder searchRequest =
        new SearchRequest.Builder().index(index).query(query).size(size);
    if (sourceConfig != null) searchRequest.source(sourceConfig);
    Future<JsonObject> future = searchAsync(searchRequest.build(), DATASET);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * Executes a resource aggregation request asynchronously for the given query and index, and
   * returns the result through the resultHandler.
   *
   * @param aggregation the aggregation query to execute
   * @param size int
   * @param index the index to search in
   * @param resultHandler the handler for the result of the aggregation query
   * @return the ElasticClient object
   */
  public ElasticClient resourceAggregationAsync(
      Aggregation aggregation,
      int size,
      String index,
      Handler<AsyncResult<JsonObject>> resultHandler) {
    SearchRequest searchRequest =
        new SearchRequest.Builder()
            .index(index)
            .aggregations("results", aggregation)
            .size(size)
            .build();
    LOGGER.debug("searchAsync called for resource group and provider");
    Future<JsonObject> future = searchAsync(searchRequest, RESOURCE_AGGREGATION_ONLY);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * Executes an asynchronous search operation with a geo query in the specified index.
   *
   * @param query the query to execute
   * @param source SourceConfig
   * @param index the name of the index to search
   * @param resultHandler the handler for the search result
   * @return this ElasticClient instance
   */
  public ElasticClient searchAsyncGeoQuery(
      Query query,
      SourceConfig source,
      String index,
      Handler<AsyncResult<JsonObject>> resultHandler) {
    SearchRequest.Builder searchRequest = new SearchRequest.Builder().index(index).query(query);
    if (source != null) searchRequest.source(source);
    Future<JsonObject> future = searchAsync(searchRequest.build(), SOURCE_AND_ID_GEOQUERY);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * Asynchronously search for documents and retrieve only their ids and sources in the given index.
   *
   * @param query the query in JSON format as a string
   * @param source SourceConfig
   * @param index the name of the index to search in
   * @param resultHandler the handler for the asynchronous result of the search
   * @return the ElasticClient instance to enable method chaining
   */
  public ElasticClient searchAsyncGetId(
      Query query,
      SourceConfig source,
      String index,
      Handler<AsyncResult<JsonObject>> resultHandler) {
    SearchRequest.Builder searchRequest = new SearchRequest.Builder().index(index).query(query);
    if (source != null) searchRequest.source(source);
    Future<JsonObject> future = searchAsync(searchRequest.build(), SOURCE_AND_ID);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * Asynchronously searches for resource groups and providers based on the provided query.
   *
   * @param query the query in JSON format as a string
   * @param index the index to search within
   * @param aggregation Aggregation
   * @param sourceConfig SourceConfig
   * @param resultHandler the handler for the asynchronous result of the search
   * @return the ElasticClient instance to allow method chaining
   */
  public ElasticClient searchAsyncResourceGroupAndProvider(
      Query query,
      Aggregation aggregation,
      SourceConfig sourceConfig,
      int size,
      String index,
      Handler<AsyncResult<JsonObject>> resultHandler) {
    SearchRequest.Builder searchRequest =
        new SearchRequest.Builder().index(index).query(query).size(size);
    if (sourceConfig != null) searchRequest.source(sourceConfig);
    if (aggregation != null) searchRequest.aggregations("provider_count", aggregation);
    LOGGER.debug("searchAsync called for resource group and provider");
    Future<JsonObject> future = searchAsync(searchRequest.build(), "PROVIDER_AGGREGATION");
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * Performs a search based on the given query vector using a script score query with cosine
   * similarity between the query vector and the word vectors of the documents in the index.
   *
   * @param queryVector the query vector in a JSON array format
   * @param resultHandler the handler for the result of the search operation
   * @return the ElasticClient instance for chaining calls
   */
  public ElasticClient scriptSearch(
      JsonArray queryVector, Handler<AsyncResult<JsonObject>> resultHandler) {
    // String query = NLP_SEARCH.replace("$1", queryVector.toString());
    // Convert JsonArray to List for params
    List<Double> vectorList = queryVector.getList();

    // Convert List to JSON string
    String jsonString = new JsonArray(vectorList).encode();

    // Create a map for script parameters
    Map<String, JsonData> params = new HashMap<>();
    params.put("query_vector", JsonData.fromJson(jsonString));


    Query query = getScriptScoreQuery(params);
    SearchRequest searchRequest =
        new SearchRequest.Builder()
            .index(index)
            .query(query)
            .source(
                SourceConfig.of(
                    src -> src.filter(SourceFilter.of(f -> f.excludes("_word_vector")))))
            .build();
    Future<JsonObject> future = searchAsync(searchRequest, SOURCE_ONLY);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * Performs a location-based search query using a vector and a set of query parameters.
   *
   * @param queryVector The vector to use for the query.
   * @param queryParams The parameters to filter results based on location.
   * @return A Future of a JsonObject containing the search results.
   */
  public Future<JsonObject> scriptLocationSearch(JsonArray queryVector, JsonObject queryParams) {

    // Convert JsonArray to List for params
    List<Double> vectorList = queryVector.getList();
    // Convert List to JSON string
    String jsonString = new JsonArray(vectorList).encode();
    // Create a map for script parameters
    Map<String, JsonData> params = new HashMap<>();
    params.put("query_vector", JsonData.fromJson(jsonString));
    Query query = getScriptLocationSearchQuery(queryParams, params);

    SearchRequest searchRequest =
        new SearchRequest.Builder()
            .query(query)
            .source(
                SourceConfig.of(
                    src -> src.filter(SourceFilter.of(f -> f.excludes("_word_vector")))))
            .index(index)
            .build();
    Future<JsonObject> future = searchAsync(searchRequest, SOURCE_ONLY);
    Promise<JsonObject> promise = Promise.promise();

    future
        .onSuccess(
            h -> {
              promise.complete(future.result());
            })
        .onFailure(
            h -> {
              promise.fail(future.cause());
            });
    return promise.future();
  }

  /**
   * searchGetIdAsync - Get document IDs matching a query.
   *
   * @param query Query
   * @param source SourceConfig
   * @param index String
   * @param resultHandler JsonObject result {@link AsyncResult} @TODO XPack Security
   */
  public ElasticClient searchGetId(
      Query query,
      SourceConfig source,
      String index,
      Handler<AsyncResult<JsonObject>> resultHandler) {

    SearchRequest.Builder searchRequest = new SearchRequest.Builder().index(index).query(query);
    if (source != null) searchRequest.source(source);
    Future<JsonObject> future = searchAsync(searchRequest.build(), DOC_IDS_ONLY);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * Executes a rating aggregation request asynchronously for the given query and index, and returns
   * the result through the resultHandler.
   *
   * @param query the aggregation query to execute
   * @param aggregation Aggregation
   * @param index the index to search in
   * @param resultHandler the handler for the result of the aggregation query
   * @return the ElasticClient object
   */
  public ElasticClient ratingAggregationAsync(
      Query query,
      Aggregation aggregation,
      String index,
      Handler<AsyncResult<JsonObject>> resultHandler) {
    SearchRequest searchRequest =
        new SearchRequest.Builder()
            .index(index)
            .query(query)
            .aggregations("results", aggregation)
            .build();
    Future<JsonObject> future = searchAsync(searchRequest, RATING_AGGREGATION_ONLY);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * aggregationsAsync - Wrapper around elasticsearch async search requests.
   *
   * @param query Query
   * @param aggregation Aggregation
   * @param resultHandler JsonObject result {@link AsyncResult} @TODO XPack Security
   */
  public ElasticClient listAggregationAsync(
      Query query, Aggregation aggregation, Handler<AsyncResult<JsonObject>> resultHandler) {
    SearchRequest searchRequest =
        new SearchRequest.Builder()
            .index(index)
            .query(query)
            .aggregations("results", aggregation)
            .build();
    Future<JsonObject> future = searchAsync(searchRequest, AGGREGATION_ONLY);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * countAsync - private function which perform performRequestAsync for count apis.
   *
   * @param countRequest Elastic Request @TODO XPack Security @TODO Can combine countAsync and
   *     searchAsync
   */
  private Future<JsonObject> countAsync(CountRequest countRequest) {
    Promise<JsonObject> promise = Promise.promise();

    DbResponseMessageBuilder responseMsg = new DbResponseMessageBuilder();
    client
        .count(countRequest)
        .whenCompleteAsync(
            (response, exception) -> {
              if (exception != null) {
                LOGGER.error("async count query failed: {}", exception);
                promise.fail(exception);
                return;
              }
              try {
                long count = response.count();
                responseMsg.statusSuccess().setTotalHits((int) count);
                promise.complete(responseMsg.getResponse());

              } catch (Exception e) {
                promise.fail(e);
              }
            });
    return promise.future();
  }

  /**
   * countAsync - Wrapper around elasticsearch async count requests.
   *
   * @param query Query
   * @param index Index to search on
   * @param resultHandler JsonObject result {@link AsyncResult} @TODO XPack Security
   */
  public ElasticClient countAsync(
      Query query, String index, Handler<AsyncResult<JsonObject>> resultHandler) {

    CountRequest countRequest = CountRequest.of(e -> e.index(index).query(query));
    Future<JsonObject> future = countAsync(countRequest);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * docPostAsync - Wrapper around elasticsearch async doc post request.
   *
   * @param index Index to search on
   * @param doc Document
   * @param resultHandler JsonObject @TODO XPack Security
   */
  public ElasticClient docPostAsync(
      String index, String doc, Handler<AsyncResult<JsonObject>> resultHandler) {

    // TODO: Validation
    // Parse the document JSON to extract the id
    JsonObject jsonDoc = new JsonObject(doc);
    String id = jsonDoc.getString("id");
    CreateRequest<JsonObject> docRequest =
        CreateRequest.of(e -> e.index(index).id(id).withJson(new StringReader(doc)));
    Future<JsonObject> future = docAsync(REQUEST_POST, docRequest);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * docPutAsync - Wrapper around elasticsearch async doc put request.
   *
   * @param index Index to search on
   * @param docId Document id (elastic id)
   * @param doc Document
   * @param resultHandler JsonObject @TODO XPack Security
   */
  public ElasticClient docPutAsync(
      String docId, String index, JsonObject doc, Handler<AsyncResult<JsonObject>> resultHandler) {

    // TODO: Validation
    JsonObject wrappedDoc = new JsonObject().put("doc", doc);

    UpdateRequest<Object, Object> docRequest =
        UpdateRequest.of(
            e -> e.index(index).id(docId).withJson(new StringReader(wrappedDoc.encode())));
    Future<JsonObject> future = docAsync(REQUEST_PUT, docRequest);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * docDelAsync - Wrapper around elasticsearch async doc delete request.
   *
   * @param index Index to search on
   * @param docId String of the Document
   * @param resultHandler JsonObject @TODO XPack Security
   */
  public ElasticClient docDelAsync(
      String docId, String index, Handler<AsyncResult<JsonObject>> resultHandler) {

    // TODO: Validation
    DeleteRequest docRequest = DeleteRequest.of(e -> e.index(index).id(docId));
    Future<JsonObject> future = docAsync(REQUEST_DELETE, docRequest);
    future.onComplete(resultHandler);
    return this;
  }

  public ElasticClient docPatchAsync(
      String docId, String index, String doc, Handler<AsyncResult<JsonObject>> resultHandler) {

    // TODO: Validation
    UpdateRequest<JsonObject, JsonObject> docRequest =
        UpdateRequest.of(e -> e.index(index).id(docId).withJson(new StringReader(doc)));
    Future<JsonObject> future = docAsync(REQUEST_POST, docRequest);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * docAsync - private function which perform performRequestAsync for doc apis.
   *
   * @param request Elastic Request
   * @param method SOURCE - Source only DOCIDS - DOCIDs only IDS - IDs only @TODO XPack
   *     Security @TODO Can combine countAsync and searchAsync
   */
  public <T> Future<JsonObject> docAsync(String method, T request) {
    Promise<JsonObject> promise = Promise.promise();

    if (request instanceof CreateRequest) {
      client.create((CreateRequest<?>) request).whenComplete(handleResponse(promise));
    } else if (request instanceof DeleteRequest) {
      client.delete((DeleteRequest) request).whenComplete(handleResponse(promise));
    } else if (request instanceof UpdateRequest) {
      client
          .update((UpdateRequest<?, ?>) request, UpdateResponse.class)
          .whenComplete(handleResponse(promise));
    } else {
      promise.fail("Invalid request type");
    }

    return promise.future();
  }

  private <R> BiConsumer<R, Throwable> handleResponse(Promise<JsonObject> promise) {
    return (response, exception) -> {
      if (exception != null) {
        promise.fail(exception);
        return;
      }

      try {
        JsonObject responseJson = extractFields(response);
        promise.complete(responseJson);
      } catch (Exception e) {
        promise.fail(e);
      }
    };
  }

  private JsonObject extractFields(Object response) {
    JsonObject responseJson = new JsonObject();

    if (response instanceof CreateResponse) {
      CreateResponse createResponse = (CreateResponse) response;
      responseJson = populateFields(createResponse);
    } else if (response instanceof UpdateResponse) {
      UpdateResponse updateResponse = (UpdateResponse) response;
      responseJson = populateFields(updateResponse);
    } else if (response instanceof DeleteResponse) {
      DeleteResponse deleteResponse = (DeleteResponse) response;
      responseJson = populateFields(deleteResponse);
    }

    return responseJson;
  }

  private JsonObject populateFields(WriteResponseBase response) {
    return new JsonObject()
        .put("_id", response.id())
        .put("_index", response.index())
        .put("_primary_term", response.primaryTerm())
        .put("result", response.result())
        .put("_seq_no", response.seqNo())
        .put(
            "_shards",
            new JsonObject()
                .put("failed", response.shards().failed())
                .put("successful", response.shards().successful())
                .put("total", response.shards().total()))
        .put("_version", response.version());
  }

  /** DbResponseMessageBuilder} Message builder for search APIs. */
  private class DbResponseMessageBuilder {
    private JsonObject response = new JsonObject();
    private JsonArray results = new JsonArray();

    DbResponseMessageBuilder() {}

    DbResponseMessageBuilder statusSuccess() {
      response.put(TYPE, TYPE_SUCCESS);
      response.put(TITLE, TITLE_SUCCESS);
      return this;
    }

    DbResponseMessageBuilder setTotalHits(int hits) {
      response.put(TOTAL_HITS, hits);
      return this;
    }

    /** Overloaded for source only request. */
    DbResponseMessageBuilder addResult(JsonObject obj) {
      response.put(RESULTS, results.add(obj));
      return this;
    }

    /** Overloaded for doc-ids request. */
    DbResponseMessageBuilder addResult(String value) {
      response.put(RESULTS, results.add(value));
      return this;
    }

    DbResponseMessageBuilder addResult() {
      response.put(RESULTS, results);
      return this;
    }

    JsonObject getResponse() {
      return response;
    }
  }
}
