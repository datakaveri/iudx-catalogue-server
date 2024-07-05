package iudx.catalogue.server.apiserver.stack;

import static iudx.catalogue.server.apiserver.stack.StackConstants.DOC_ID;
import static iudx.catalogue.server.database.elastic.query.Queries.buildSourceConfig;
import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.util.Constants.TITLE_ITEM_NOT_FOUND;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.RespBuilder;
import iudx.catalogue.server.database.elastic.ElasticClient;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StacServiceImpl implements StacSevice {

  private static final Logger LOGGER = LogManager.getLogger(StacServiceImpl.class);

  private final ElasticClient elasticClient;
  private final String index;
  RespBuilder respBuilder;
  Supplier<String> idSuppler = () -> UUID.randomUUID().toString();
  private QueryBuilder queryBuilder = new QueryBuilder();

  public StacServiceImpl(ElasticClient elasticClient, String index) {
    this.elasticClient = elasticClient;
    this.index = index;
  }

  /**
   * @param stackId stack id
   * @return future Json
   */
  @Override
  public Future<JsonObject> get(String stackId) {
    Promise<JsonObject> promise = Promise.promise();
    Query query = queryBuilder.getQuery(stackId);
    elasticClient.searchAsync(
        query,
        buildSourceConfig(List.of()),
        FILTER_PAGINATION_SIZE,
        FILTER_PAGINATION_FROM,
        index,
        clientHandler -> {
          if (clientHandler.succeeded()) {
            LOGGER.debug("Success: Successful DB request");
            JsonObject result = clientHandler.result();
            if (result.getInteger(TOTAL_HITS) > 0) {
              promise.complete(result);
            } else {
              LOGGER.error("Fail: Item not found");
              respBuilder =
                  new RespBuilder()
                      .withType(TYPE_ITEM_NOT_FOUND)
                      .withTitle(TITLE_ITEM_NOT_FOUND)
                      .withDetail("Fail: Stac doesn't exist");
              promise.fail(respBuilder.getResponse());
            }
          } else {
            LOGGER.error("Fail: DB request has failed;" + clientHandler.cause());
            respBuilder =
                new RespBuilder()
                    .withType(FAILED)
                    .withResult(stackId, REQUEST_GET, FAILED)
                    .withDetail(DATABASE_ERROR);
            promise.fail(respBuilder.getResponse());
          }
        });

    return promise.future();
  }

  /**
   * @param request json
   * @return future json
   */
  @Override
  public Future<JsonObject> create(JsonObject request) {
    LOGGER.debug("create () method started");
    Query query = queryBuilder.getQuery4CheckExistence(request);
    String id = idSuppler.get();
    request.put(ID, id);
    LOGGER.info("id :{}", request);
    Promise<JsonObject> promise = Promise.promise();
    elasticClient.searchAsync(
        query,
        buildSourceConfig(List.of()),
        FILTER_PAGINATION_SIZE,
        0,
        index,
        searchHandler -> {
          if (searchHandler.succeeded()) {
            JsonObject searchResult = searchHandler.result();
            if (searchResult.getInteger("totalHits") == 0) {
              elasticClient.docPostAsync(
                  index,
                  request.toString(),
                  postHandler -> {
                    if (postHandler.succeeded()) {
                      LOGGER.info("Success: Stac creation");
                      JsonObject result = postHandler.result();
                      LOGGER.debug("Success : " + result);
                      respBuilder =
                          new RespBuilder()
                              .withType(SUCCESS)
                              .withResult(id, INSERT, SUCCESS)
                              .withDetail(STAC_CREATION_SUCCESS);
                      promise.complete(respBuilder.getJsonResponse());
                    } else {
                      LOGGER.error("Fail: STAC creation : {}", postHandler.cause().getMessage());
                      respBuilder =
                          new RespBuilder()
                              .withType(FAILED)
                              .withResult("stack", INSERT, FAILED)
                              .withDetail(DATABASE_ERROR);
                      promise.fail(respBuilder.getResponse());
                    }
                  });
            } else {
              LOGGER.error("STAC already exists, skipping creation");
              respBuilder =
                  new RespBuilder()
                      .withType(TYPE_CONFLICT)
                      .withTitle(DETAIL_CONFLICT)
                      .withDetail("STAC already exists,creation skipped");
              promise.fail(respBuilder.getResponse());
            }
          } else {
            LOGGER.error("Fail: Search operation : {}", searchHandler.cause().getMessage());
            respBuilder =
                new RespBuilder()
                    .withType(FAILED)
                    .withResult("stac", INSERT, FAILED)
                    .withDetail(DATABASE_ERROR);
            promise.fail(respBuilder.getResponse());
          }
        });

    return promise.future();
  }

  /**
   * @param stack Json object
   * @return future json
   */
  @Override
  public Future<JsonObject> update(JsonObject stack) {
    LOGGER.debug("update () method started");
    Promise<JsonObject> promise = Promise.promise();
    ResultContainer result = new ResultContainer();
    String stacId = stack.getString("id");
    Future<JsonObject> existFuture = isExist(stacId);
    existFuture
        .compose(
            existHandler -> {
              LOGGER.info(existHandler);
              result.links = existHandler.getJsonObject("_source").getJsonArray("links");
              result.docId = existHandler.getString(DOC_ID);
              return isAllowPatch(stack, result.links);
            })
        .compose(
            allowHandler -> {
              return doUpdate(stack, result.docId, allowHandler);
            })
        .onSuccess(
            successHandler -> {
              respBuilder =
                  new RespBuilder()
                      .withType(TYPE_SUCCESS)
                      .withTitle(TITLE_SUCCESS)
                      .withResult(stacId)
                      .withDetail("Success: Item updated successfully");
              promise.complete(respBuilder.getJsonResponse());
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error("error : " + failureHandler.getMessage());
              promise.fail(failureHandler.getMessage());
            });

    return promise.future();
  }

  /**
   * @param stacId String
   * @return future json
   */
  @Override
  public Future<JsonObject> delete(String stacId) {
    LOGGER.debug("delete () method started");
    Promise<JsonObject> promise = Promise.promise();
    LOGGER.debug("stackId for delete :{}", stacId);
    isExist(stacId)
        .onComplete(
            existHandler -> {
              if (existHandler.succeeded()) {
                JsonObject result = existHandler.result();
                String docId = result.getString(DOC_ID);

                elasticClient.docDelAsync(
                    docId,
                    index,
                    deleteHandler -> {
                      if (deleteHandler.succeeded()) {
                        LOGGER.info("Deletion success :{}", deleteHandler.result());
                        respBuilder =
                            new RespBuilder()
                                .withType(SUCCESS)
                                .withResult(stacId)
                                .withDetail(STAC_DELETION_SUCCESS);
                        promise.complete(respBuilder.getJsonResponse());
                      } else {
                        LOGGER.error(
                            "Fail: Delete operation failed : {}",
                            deleteHandler.cause().getMessage());
                        respBuilder =
                            new RespBuilder()
                                .withType(FAILED)
                                .withResult(stacId, REQUEST_DELETE, FAILED)
                                .withDetail(DATABASE_ERROR);
                        promise.fail(respBuilder.getResponse());
                      }
                    });
              } else {
                LOGGER.error(
                    "Fail: Item not found for deletion : {}", existHandler.cause().getMessage());
                respBuilder =
                    new RespBuilder()
                        .withType(TYPE_ITEM_NOT_FOUND)
                        .withResult(stacId, REQUEST_DELETE, FAILED)
                        .withDetail("Item not found, can't delete");
                promise.fail(respBuilder.getResponse());
              }
            });

    return promise.future();
  }

  private Future<JsonObject> isExist(String id) {
    LOGGER.debug("isExist () method started");
    Promise<JsonObject> promise = Promise.promise();
    LOGGER.debug("stacId: {}", id);
    Query query = queryBuilder.getQuery(id);
    LOGGER.error(elasticClient);

    elasticClient.searchAsyncGetId(
        query,
        buildSourceConfig(List.of()),
        index,
        existHandler -> {
          LOGGER.error("existHandler succeeded " + existHandler.succeeded());
          if (existHandler.failed()) {
            LOGGER.error("Fail: Check Query Fail : {}", existHandler.cause().getMessage());
            promise.fail("Fail: Check Query Fail : " + existHandler.cause().getMessage());
            return;
          }
          if (existHandler.result().getInteger(TOTAL_HITS) == 0) {
            LOGGER.debug("success: existHandler " + existHandler.result());
            respBuilder =
                new RespBuilder()
                    .withType(TYPE_ITEM_NOT_FOUND)
                    .withTitle(TITLE_ITEM_NOT_FOUND)
                    .withDetail("Fail: stac doesn't exist");
            promise.fail(respBuilder.getResponse());
          } else {
            try {
              LOGGER.debug(existHandler.result());
              JsonObject result =
                  new JsonObject(existHandler.result().getJsonArray(RESULTS).getString(0));
              promise.complete(result);
            } catch (Exception e) {
              LOGGER.error("Fail: Parsing result : {}", e.getMessage());
              promise.fail("Fail: Parsing result");
            }
          }
        });

    return promise.future();
  }

  private Future<Boolean> isAllowPatch(JsonObject requestBody, JsonArray links) {
    LOGGER.debug("isAllowPatch () method started");
    Promise<Boolean> promise = Promise.promise();
    AtomicBoolean allowPatch = new AtomicBoolean(true);
    links.stream()
        .map(JsonObject.class::cast)
        .forEach(
            child -> {
              if (child.getString("rel").equalsIgnoreCase("child")
                  && child.getString("href").equalsIgnoreCase(requestBody.getString("href"))) {
                allowPatch.set(false);
              }
            });
    LOGGER.info("isAllowPatch : {}", allowPatch.get());
    promise.complete(allowPatch.get());
    return promise.future();
  }

  private Future<JsonObject> doUpdate(JsonObject request, String docId, boolean isAllowed) {
    LOGGER.debug("doUpdate () method started");
    if (!isAllowed) {
      LOGGER.debug("Patch operations not allowed for duplicate child");
      respBuilder =
          new RespBuilder()
              .withType(TYPE_CONFLICT)
              .withTitle(TITLE_ALREADY_EXISTS)
              .withDetail("Patch operations not allowed for duplicate child");
      return Future.failedFuture(respBuilder.getResponse());
    }
    LOGGER.debug("docId: {}", docId);
    request.remove("id");
    String query = queryBuilder.getPatchQuery(request);
    LOGGER.debug("patchQuery:: " + query);
    Promise<JsonObject> promise = Promise.promise();
    elasticClient.docPatchAsync(
        docId,
        index,
        query,
        patchHandler -> {
          if (patchHandler.succeeded()) {

            JsonObject result = patchHandler.result();
            LOGGER.debug("patch result " + result);
            promise.complete(result);
          } else {
            LOGGER.error("failed:: " + patchHandler.cause().getMessage());
            promise.fail(patchHandler.cause().getMessage());
          }
        });
    return promise.future();
  }

  final class ResultContainer {
    JsonArray links;
    String docId;
    boolean allowPatch;
  }
}
