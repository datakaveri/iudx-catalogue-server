package iudx.catalogue.server.database;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

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
   * The listItem implements the list item operation with the database.
   * 
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */

  @Fluent
  DatabaseService listItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The listTags implements the list tags operation with the database.
   * 
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */

  @Fluent
  DatabaseService listTags(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The listDomains implements the list domains operation with the database.
   * 
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */

  @Fluent
  DatabaseService listDomains(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The listCities implements the list cities operation with the database.
   * 
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */

  @Fluent
  DatabaseService listCities(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The listResourceServers implements the list resource servers operation with the database.
   * 
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */

  @Fluent
  DatabaseService listResourceServers(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The listProviders implements the list providers operation with the database.
   * 
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */

  @Fluent
  DatabaseService listProviders(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The listResourceGroups implements the list resource groups operation with the database.
   * 
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */

  @Fluent
  DatabaseService listResourceGroups(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The listResourceRelationship implements the list relationships operation with the database.
   * 
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */

  @Fluent
  DatabaseService listResourceRelationship(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler);

  /**
   * The listResourceGroupRelationship implements the list group relationships operation with the
   * database.
   * 
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */

  @Fluent
  DatabaseService listResourceGroupRelationship(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler);

  /**
   * The listProviderRelationship implements the list provider relationships with the database.
   * 
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */

  @Fluent
  DatabaseService listProviderRelationship(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler);

  /**
   * The listResourceServerRelationship implements the list resource server relationship with the
   * database.
   * 
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */

  @Fluent
  DatabaseService listResourceServerRelationship(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler);

  /**
   * The listTypes implements the list types with the database.
   * 
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return DatabaseService which is a Service
   */

  @Fluent
  DatabaseService listTypes(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The createProxy helps the code generation blocks to generate proxy code.
   * 
   * @param vertx which is the vertx instance
   * @param address which is the proxy address
   * @return DatabaseServiceVertxEBProxy which is a service proxy
   */

  @GenIgnore
  static DatabaseService createProxy(Vertx vertx, String address) {
    return new DatabaseServiceVertxEBProxy(vertx, address);
  }
}
