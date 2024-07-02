package iudx.catalogue.server.database.elastic;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.geocoding.GeocodingService;
import iudx.catalogue.server.nlpsearch.NLPSearchService;

/**
 * The Database Service.
 *
 * <h1>Database Service</h1>
 *
 * <p>The Database Service in the IUDX Catalogue Server defines the operations to be performed with
 * the IUDX Database server.
 *
 * @see io.vertx.codegen.annotations.ProxyGen
 * @see io.vertx.codegen.annotations.VertxGen
 * @version 1.0
 * @since 2020-05-31
 */
@VertxGen
@ProxyGen
public interface ElasticsearchService {

  /* create db service with nlp and geocoding */
  @GenIgnore
  static ElasticsearchService create(
          ElasticClient client, NLPSearchService nlpService, GeocodingService geoService) {
    return new ElasticsearchServiceImpl(client, nlpService, geoService);
  }

  @GenIgnore
  static ElasticsearchService create(ElasticClient client) {
    return new ElasticsearchServiceImpl(client);
  }

  @GenIgnore
  static ElasticsearchService createProxy(Vertx vertx, String address) {
    return new ElasticsearchServiceVertxEBProxy(vertx, address);
  }

  /**
   * The searchQuery implements the search operation with the database.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return ElasticsearchService which is a Service
   */
  @Fluent
  ElasticsearchService searchQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The searchQuery implements the nlp search operation with the database.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return ElasticsearchService which is a Service
   */
  @Fluent
  ElasticsearchService nlpSearchQuery(JsonArray request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The searchQuery implements the nlp search operation with the database.
   *
   * @param request which is a JsonObject
   * @param queryParams which is a JsonObject
   * @param handler which is a Request Handler
   * @return ElasticsearchService which is a Service
   */
  @Fluent
  ElasticsearchService nlpSearchLocationQuery(
      JsonArray request, JsonObject queryParams, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The countQuery implements the count operation with the database.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return ElasticsearchService which is a Service
   */
  @Fluent
  ElasticsearchService countQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The createItem implements the create item operation with the database.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return ElasticsearchService which is a Service
   */
  @Fluent
  ElasticsearchService createItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The updateItem implements the update item operation with the database.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return ElasticsearchService which is a Service
   */
  @Fluent
  ElasticsearchService updateItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The deleteItem implements the delete item operation with the database.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return ElasticsearchService which is a Service
   */
  @Fluent
  ElasticsearchService deleteItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The listItems implements the list items operation with the database.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return ElasticsearchService which is a Service
   */
  @Fluent
  ElasticsearchService listItems(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The listOwnerOrCos implements the fetch of entire owner or cos item from the database.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return ElasticsearchService which is a service
   */
  @Fluent
  ElasticsearchService listOwnerOrCos(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The listRelationship implements the list resource, resourceGroup, provider, resourceServer,
   * type relationships operation with the database.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return ElasticsearchService which is a Service
   */
  @Fluent
  ElasticsearchService listRelationship(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The relSearch implements the Relationship searches with the database.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return ElasticsearchService which is a Service
   */
  @Fluent
  ElasticsearchService relSearch(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  ElasticsearchService getItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The createRating implements the rating creation operation with the database.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return ElasticsearchService which is a Service
   */
  @Fluent
  ElasticsearchService createRating(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The updateRating implements the rating updation operation with the database.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return ElasticsearchService which is a Service
   */
  @Fluent
  ElasticsearchService updateRating(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The deleteRating implements the rating deletion operation with the database.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return ElasticsearchService which is a Service
   */
  @Fluent
  ElasticsearchService deleteRating(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The getRatings implements fetching ratings from the database.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return ElasticsearchService which is a Service
   */
  @Fluent
  ElasticsearchService getRatings(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The createMlayerInstance implements the instance creation operation with the database.
   *
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return ElasticsearchService which is a Service.
   */
  @Fluent
  ElasticsearchService createMlayerInstance(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The getMlayerInstance implements fetching instance from the database.
   *
   * @param handler which is a request handler
   * @return ElasticsearchService which is a Service
   */
  @Fluent
  ElasticsearchService getMlayerInstance(
      JsonObject requestParams, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The deleteMlayerInstance implements deleting instance from the database.
   *
   * @param request which is JsonObject
   * @param handler which is a request handler
   * @return ElasticsearchService which is a Service
   */
  @Fluent
  ElasticsearchService deleteMlayerInstance(String request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The updateMlayerInstance implements updating instance from the database.
   *
   * @param request which is a jsonobject
   * @param handler which is a request handler
   * @return ElasticsearchService which is a Service
   */
  @Fluent
  ElasticsearchService updateMlayerInstance(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The createMlayerDomain implemenets creation of a domain from the database.
   *
   * @param request is a jsonObject
   * @param handler which is a request handler
   * @return ElasticsearchService which is Service
   */
  @Fluent
  ElasticsearchService createMlayerDomain(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The getMlayerDomain implements getting all domain from database.
   *
   * @param handler which is a request handler
   * @return ElasticsearchService which is a Service
   */
  @Fluent
  ElasticsearchService getMlayerDomain(
      JsonObject requestParams, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The updateMlayerDomain implements updating all domain from database.
   *
   * @param request is a JsonObject
   * @param handler which is a request handler
   * @return ElasticsearchService which is a Service
   */
  @Fluent
  ElasticsearchService updateMlayerDomain(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The deleteMlayerDomain deletes a domain from the darabase.
   *
   * @param request is a JsonObject
   * @param handler which is a request handler
   * @return ElasticsearchService which is a Service
   */
  @Fluent
  ElasticsearchService deleteMlayerDomain(String request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The get Mlayer Providers get all the provider's description.
   *
   * @param handler which is a request handler
   * @return ElasticsearchService which is a Service
   */
  @Fluent
  ElasticsearchService getMlayerProviders(
      JsonObject requestParams, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The post Mlayer GeoQuery posts all the dataset_id's location and label.
   *
   * @param request which is a JsonObject
   * @param handler which is a request handler
   * @return ElasticsearchService which is a Service
   */
  @Fluent
  ElasticsearchService getMlayerGeoQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The get Mlayer All Datasets gets all the dataset belonging to IUDX.
   *
   * @param query which is a string
   * @param handler which is a request handler
   * @return ElasticsearchService which is a Service
   */
  @Fluent
  ElasticsearchService getMlayerAllDatasets(
      JsonObject requestPram, String query, Handler<AsyncResult<JsonObject>> handler);

  /* create db service vanilla */

  /**
   * The get Mlayer datasset get details of the dataset.
   *
   * @param requestData which is a Json Object.
   * @param handler which is a request handler.
   * @return ElasticsearchService which is a Service.
   */
  @Fluent
  ElasticsearchService getMlayerDataset(
      JsonObject requestData, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  ElasticsearchService getMlayerPopularDatasets(
      String instance, JsonArray highestCountResource, Handler<AsyncResult<JsonObject>> handler);
}
