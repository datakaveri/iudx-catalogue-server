package iudx.catalogue.server.database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * The Database Service Implementation.
 *
 * <h1>Database Service Implementation</h1>
 *
 * <p> The Database Service implementation in the IUDX Catalogue Server implements
 * the definitions of the
 * {@link iudx.catalogue.server.database.DatabaseService}.
 * </p>
 *
 * @version 1.0
 * @since 2020-05-31
 */
public class DatabaseServiceImpl implements DatabaseService {

  private static final Logger logger = LoggerFactory.getLogger(DatabaseServiceImpl.class);

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService searchQuery(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed
    String result = 
        "{ \"status\": \"success\", \"totalHits\": 100,"
          + "\"limit\": 10, \"offset\": 100,"
          + "\"results\": ["
          + "{ \"id\": \"abc/123\", \"tags\": [ \"a\", \"b\"] } ] }";

    if ("Point".equals(request.getString("geometry"))) {
      handler.handle(Future.succeededFuture(new JsonObject(result)));

    } else if ("Polygon".equals(request.getString("geometry"))) {
      handler.handle(Future.succeededFuture(new JsonObject(result)));
    } else {
      handler.handle(Future.succeededFuture(new JsonObject(result)));
    }

    if (result != null) {
      handler.handle(Future.succeededFuture(new JsonObject(result)));
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService countQuery(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService createItem(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use
    String result = 
        "{ \"status\": \"success\","
          + "\"results\": [ "
          + "{ \"id\": \"123123\","
          + "\"method\": \"insert\", \"status\": \"success\" } ] }";

    handler.handle(Future.succeededFuture(new JsonObject(result)));
    return null;

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService updateItem(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use
    String result = 
        "{ \"status\": \"success\","
          + "\"results\": [ "
          + "{ \"id\": \"123123\","
          + "\"method\": \"update\", \"status\": \"success\" } ] }";

    handler.handle(Future.succeededFuture(new JsonObject(result)));
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService deleteItem(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use
    String result = 
        "{ \"status\": \"success\","
          + "\"results\": [ "
          + "{ \"id\": \"123123\","
          + "\"method\": \"delete\", \"status\": \"success\" } ] }";

    handler.handle(Future.succeededFuture(new JsonObject(result)));
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listItem(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use

    String result = 
        "{ \"status\": \"success\", \"totalHits\": 100,"
          + "\"limit\": 10, \"offset\": 100,"
          + "\"results\": ["
          + "{ \"id\": \"abc/123\", \"tags\": [ \"a\", \"b\"] } ] }";

    String errResult = 
        " { \"status\": \"invalidValue\", \"results\": [] }";

    if (request.getString("id").contains("/")) {
      handler.handle(Future.succeededFuture(new JsonObject(result)));
    } else {
      handler.handle(Future.succeededFuture(new JsonObject(errResult)));
    }

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listTags(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use
    String result = "{ \"status\": \"success\", \"results\": [ \"environment\", \"civic\" ] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listDomains(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use
    String result = "{ \"status\": \"success\", \"results\": [ \"environment\", \"civic\" ] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listCities(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use
    String result = "{ \"status\": \"success\", \"results\": [ \"Pune\", \"Varanasi\" ] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listResourceServers(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use
    String result = "{ \"status\": \"success\", \"results\": [ \"server-1\", \"server-2\" ] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listProviders(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use [was not part of master code]
    String result = "{ \"status\": \"success\", \"results\": [ \"pr-1\", \"pr-2\" ] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listResourceGroups(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use [was not part of master code]
    String result = "{ \"status\": \"success\", \"results\": [ \"rg-1\", \"rg-2\" ] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listResourceRelationship(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use [was not part of master code]
    String result = "{ \"status\": \"success\", \"results\": [ \"rg-1\", \"rg-2\" ] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listResourceGroupRelationship(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {
    // TODO: Stub code, to be removed after use [was not part of master code]
    String result = "{ \"status\": \"success\", \"results\": [ { \"id\": \"abc/123\" }] }";
    handler.handle(Future.succeededFuture(new JsonObject(result)));

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listProviderRelationship(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listResourceServerRelationship(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DatabaseService listTypes(JsonObject request,
      Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /**
   * The queryDecoder implements the query decoder module.
   *
   * @param request which is a JsonObject
   * @return JsonObject which is a JsonObject
   */
  public JsonObject queryDecoder(JsonObject request) {

    return null;
  }
}
