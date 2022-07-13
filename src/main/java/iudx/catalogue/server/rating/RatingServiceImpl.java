package iudx.catalogue.server.rating;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import iudx.catalogue.server.database.DatabaseService;
import iudx.catalogue.server.databroker.DataBrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;

import static iudx.catalogue.server.rating.util.Constants.*;

public class RatingServiceImpl implements RatingService {
  private static final Logger LOGGER = LogManager.getLogger(RatingServiceImpl.class);
  PgPool pool;
  DatabaseService databaseService;
  DataBrokerService dataBrokerService;
  private String ratingExchangeName;

  public RatingServiceImpl(
      String exchangeName,
      PgPool pool,
      DatabaseService databaseService,
      DataBrokerService dataBrokerService) {
    this.ratingExchangeName = exchangeName;
    this.pool = pool;
    this.databaseService = databaseService;
    this.dataBrokerService = dataBrokerService;
  }

  @Override
  public RatingService createRating(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String sub = request.getString(USER_ID);
    String id = request.getString(ID);
    StringBuilder query = new StringBuilder(AUDIT_INFO_QUERY.replace("$1", sub).replace("$2", id));
    Future<JsonObject> getRSAuditingInfo = getAuditingInfo(query);

    getRSAuditingInfo
        .onSuccess(
            successHandler -> {
              int countResourceAccess = successHandler.getInteger("totalHits");
              if (countResourceAccess > 0) {

                String ratingID =
                    Hashing.sha256().hashString(sub + id, StandardCharsets.UTF_8).toString();

                request.put(RATING_ID, ratingID);

                databaseService.createRating(
                    request,
                    createRatingHandler -> {
                      if (createRatingHandler.succeeded()) {
                        LOGGER.info("Success: Rating Recorded");
                        Future.future(fu -> publishMessage(request));
                        handler.handle(Future.succeededFuture(createRatingHandler.result()));
                      } else {
                        LOGGER.error("Fail: Rating creation failed");
                        handler.handle(Future.failedFuture(createRatingHandler.cause()));
                      }
                    });
              }
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error(
                  "User has not accessed resource before and hence is not authorised to give rating");
              handler.handle(Future.failedFuture(failureHandler.getCause().getMessage()));
            });

    return this;
  }

  @Override
  public RatingService getRating(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String id = request.getString(ID);

    if (request.containsKey(USER_ID)) {
      String sub = request.getString(USER_ID);
      String ratingID = Hashing.sha256().hashString(sub + id, StandardCharsets.UTF_8).toString();

      request.put(RATING_ID, ratingID);
    }

    databaseService.getRatings(
        request,
        getRatingHandler -> {
          if (getRatingHandler.succeeded()) {
            handler.handle(Future.succeededFuture(getRatingHandler.result()));
          } else {
            handler.handle(Future.failedFuture(getRatingHandler.cause()));
          }
        });

    return this;
  }

  @Override
  public RatingService updateRating(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String sub = request.getString(USER_ID);
    String id = request.getString(ID);

    String ratingID = Hashing.sha256().hashString(sub + id, StandardCharsets.UTF_8).toString();

    request.put(RATING_ID, ratingID);

    databaseService.updateRating(
        request,
        updateRatingHandler -> {
          if (updateRatingHandler.succeeded()) {
            LOGGER.info("Success: Rating Recorded");
            Future.future(fu -> publishMessage(request));
            handler.handle(Future.succeededFuture(updateRatingHandler.result()));
          } else {
            LOGGER.error("Fail: Rating updation failed");
            handler.handle(Future.failedFuture(updateRatingHandler.cause()));
          }
        });

    return this;
  }

  @Override
  public RatingService deleteRating(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String sub = request.getString(USER_ID);
    String id = request.getString(ID);

    String ratingID = Hashing.sha256().hashString(sub + id, StandardCharsets.UTF_8).toString();

    request.put(RATING_ID, ratingID);

    databaseService.deleteRating(
        request,
        deleteRatingHandler -> {
          if (deleteRatingHandler.succeeded()) {
            LOGGER.info("Success: Rating deleted");
            handler.handle(Future.succeededFuture(deleteRatingHandler.result()));
          } else {
            LOGGER.error("Fail: Rating deletion failed");
            handler.handle(Future.failedFuture(deleteRatingHandler.cause()));
          }
        });

    return this;
  }

  Future<JsonObject> getAuditingInfo(StringBuilder query) {
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

  void publishMessage(JsonObject request) {
    dataBrokerService.publishMessage(
        request,
        ratingExchangeName,
        request.getString(ID),
        handler -> {
          if (handler.succeeded()) {
            LOGGER.info("Rating info publish to RabbitMQ");
          } else {
            LOGGER.error("Failed to publish Rating info");
          }
        });
  }
}
