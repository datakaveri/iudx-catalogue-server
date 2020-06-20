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
 * <p>The Database Service implementation in the IUDX Catalogue Server implements the definitions of
 * the {@link iudx.catalogue.server.database.DatabaseService}.
 *
 * @version 1.0
 * @since 2020-05-31
 */
public class DatabaseServiceImpl implements DatabaseService {

  private static final Logger logger = LoggerFactory.getLogger(DatabaseServiceImpl.class);

  /** {@inheritDoc} */
  @Override
  public DatabaseService searchQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    handler.handle(Future.succeededFuture(new JsonObject().put("status", "success")));
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public DatabaseService countQuery(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /** {@inheritDoc} */
  @Override
  public DatabaseService createItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    handler.handle(Future.succeededFuture(new JsonObject().put("status", "success")));
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public DatabaseService updateItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    handler.handle(Future.succeededFuture(new JsonObject().put("status", "success")));
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public DatabaseService deleteItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    handler.handle(Future.succeededFuture(new JsonObject().put("status", "success")));
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public DatabaseService listItem(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /** {@inheritDoc} */
  @Override
  public DatabaseService listTags(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /** {@inheritDoc} */
  @Override
  public DatabaseService listDomains(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /** {@inheritDoc} */
  @Override
  public DatabaseService listCities(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /** {@inheritDoc} */
  @Override
  public DatabaseService listResourceServers(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /** {@inheritDoc} */
  @Override
  public DatabaseService listResourceRelationship(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /** {@inheritDoc} */
  @Override
  public DatabaseService listResourceGroupRelationship(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /** {@inheritDoc} */
  @Override
  public DatabaseService listProviderRelationship(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /** {@inheritDoc} */
  @Override
  public DatabaseService listResourceServerRelationship(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    return null;
  }

  /** {@inheritDoc} */
  @Override
  public DatabaseService listTypes(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

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
