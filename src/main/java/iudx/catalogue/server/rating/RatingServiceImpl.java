package iudx.catalogue.server.rating;

import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.Handler;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import iudx.catalogue.server.database.DatabaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;

import static iudx.catalogue.server.rating.util.Constants.*;

public class RatingServiceImpl implements RatingService {
  private static final Logger LOGGER = LogManager.getLogger(RatingServiceImpl.class);
  PgPool pool;
  DatabaseService databaseService;

  public RatingServiceImpl(PgPool pool, DatabaseService databaseService, Vertx vertx) {
    this.pool = pool;
    this.databaseService = databaseService;
  }

  @Override
  public RatingService createRating(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String sub = request.getString(USER_ID);
    String resourceID = request.getString(RESOURCE_ID);
    StringBuilder query =
        new StringBuilder(AUDIT_INFO_QUERY.replace("$1", sub).replace("$2", resourceID));
    Future<JsonObject> getRSAuditingInfo = getAuditingInfo(query);
//    getRSAuditingInfo
//        .onSuccess(
//            successHandler -> {
//              int countResourceAccess = successHandler.getInteger("totalHits");
//              if (countResourceAccess > 0) {
//
                String ratingID =
                    Hashing.sha256()
                        .hashString(sub + resourceID, StandardCharsets.UTF_8)
                        .toString();

                request.put(ID, ratingID);

                databaseService.createRating(
                    request,
                    createItemHandler -> {
                      if (createItemHandler.succeeded()) {
                        LOGGER.info("Success: Rating Recorded");
                        handler.handle(Future.succeededFuture(createItemHandler.result()));
                      } else {
                        LOGGER.error("Fail: Rating creation failed");
                        handler.handle(Future.failedFuture(createItemHandler.cause()));
                      }
                    });
//              }
//            })
//        .onFailure(
//            failureHandler -> {
//              LOGGER.error(
//                  "User has not accessed resource before and hence is not authorised to give rating");
//              handler.handle(Future.failedFuture(failureHandler.getCause().getMessage()));
//            });

    return this;
  }

  @Override
  public RatingService getRating(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String resourceID = request.getString(RESOURCE_ID);

    return this;
  }

  @Override
  public RatingService updateRating(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String sub = request.getString(USER_ID);
    String resourceID = request.getString(RESOURCE_ID);

    String ratingID =
        Hashing.sha256().hashString(sub + resourceID, StandardCharsets.UTF_8).toString();

    request.put(ID, ratingID);

    databaseService.updateRating(
        request,
        updateItemHandler -> {
          if (updateItemHandler.succeeded()) {
            LOGGER.info("Success: Rating Recorded");
            handler.handle(Future.succeededFuture(updateItemHandler.result()));
          } else {
            LOGGER.error("Fail: Rating updation failed");
            handler.handle(Future.failedFuture(updateItemHandler.cause()));
          }
        });

    return this;
  }

  @Override
  public RatingService deleteRating(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String sub = request.getString(USER_ID);
    String resourceID = request.getString(RESOURCE_ID);

    String ratingID =
        Hashing.sha256().hashString(sub + resourceID, StandardCharsets.UTF_8).toString();

    request.put(ID, ratingID);

    databaseService.deleteRating(
        request,
        updateItemHandler -> {
          if (updateItemHandler.succeeded()) {
            LOGGER.info("Success: Rating deleted");
            handler.handle(Future.succeededFuture(updateItemHandler.result()));
          } else {
            LOGGER.error("Fail: Rating deletion failed");
            handler.handle(Future.failedFuture(updateItemHandler.cause()));
          }
        });

    return this;
  }

  private Future<JsonObject> getAuditingInfo(StringBuilder query) {
    Promise<JsonObject> promise = Promise.promise();
    pool.withConnection(
            connection ->
                connection
                    .query(query.toString())
                    .execute()
                    .map(rows -> rows.iterator().next().getInteger(0)))
        .onSuccess(
            count -> {
              promise.complete(new JsonObject().put("totalHits", count));
            })
        .onFailure(
            failureHandler -> {
              promise.fail("Empty Message");
            });

    return promise.future();
  }
}
