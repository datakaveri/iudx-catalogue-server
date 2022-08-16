package iudx.catalogue.server.database;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.client.WebClient;

import iudx.catalogue.server.database.ElasticClient;
import iudx.catalogue.server.nlpsearch.NLPSearchService;
import iudx.catalogue.server.geocoding.GeocodingService;

/**
 * The Database Service.
 * <h1>Database Service</h1>
 * <p>
 * The Database Service in the IUDX Catalogue Server defines the operations to be performed with the
 * IUDX Database server.
 * </p>
 * 
 * @see io.vertx.codegen.annotations.ProxyGen
 * @see io.vertx.codegen.annotations.VertxGen
 * @version 1.0
 * @since 2020-05-31
 */

@VertxGen
@ProxyGen
public interface DatabaseService {

  /**
   * The searchQuery implements the search operation with the database.
   * 
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService searchQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

   /**
   * The searchQuery implements the nlp search operation with the database.
   * 
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService nlpSearchQuery(JsonArray request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The searchQuery implements the nlp search operation with the database.
   * 
   * @param request which is a JsonObject
   * @param location which is a String
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService nlpSearchLocationQuery(JsonArray request, String location,
                                          Handler<AsyncResult<JsonObject>> handler);

  /**
   * The countQuery implements the count operation with the database.
   * 
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService countQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The createItem implements the create item operation with the database.
   * 
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService createItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler);


  /**
   * The updateItem implements the update item operation with the database.
   * 
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService updateItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The deleteItem implements the delete item operation with the database.
   * 
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService deleteItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The listItems implements the list items operation with the database.
   * 
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService listItems(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The listRelationship implements the list resource, resourceGroup, provider, resourceServer,
   * type relationships operation with the database.
   * 
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService listRelationship(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler);

  /**
   * The relSearch implements the Relationship searches with the database.
   * 
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService relSearch(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  @Fluent
  DatabaseService getItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The createRating implements the rating creation operation with the database.
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService createRating(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The updateRating implements the rating updation operation with the database.
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService updateRating(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The deleteRating implements the rating deletion operation with the database.
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService deleteRating(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The getRatings implements fetching ratings from the database
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */
  @Fluent
  DatabaseService getRatings(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /* create db service with nlp and geocoding */
  @GenIgnore
  static DatabaseService create(ElasticClient client,
                                NLPSearchService nlpService,
                                GeocodingService geoService) {
    return new DatabaseServiceImpl(client, nlpService, geoService);
  }
  /* create db service vanilla */
  @GenIgnore
  static DatabaseService create(ElasticClient client) {
    return new DatabaseServiceImpl(client);
  }

  @GenIgnore
  static DatabaseService createProxy(Vertx vertx, String address) {
    return new DatabaseServiceVertxEBProxy(vertx, address);
  }

}
