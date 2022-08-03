package iudx.catalogue.server.rating;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import iudx.catalogue.server.apiserver.util.RespBuilder;
import iudx.catalogue.server.database.DatabaseService;
import iudx.catalogue.server.databroker.DataBrokerService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;

import static iudx.catalogue.server.rating.util.Constants.*;
import static iudx.catalogue.server.util.Constants.TITLE_OPERATION_NOT_ALLOWED;
import static iudx.catalogue.server.util.Constants.TYPE_FAIL;

public class RatingServiceImpl implements RatingService {
  private static final Logger LOGGER = LogManager.getLogger(RatingServiceImpl.class);
  PgPool pool;
  DatabaseService databaseService;
  DataBrokerService dataBrokerService;
  private String ratingExchangeName;
  private final String rsauditingtable;
  private final int minReadNumber;

  public RatingServiceImpl(
      String exchangeName, String rsauditingtable, int minReadNumber,
      PgPool pool,
      DatabaseService databaseService,
      DataBrokerService dataBrokerService) {
    this.ratingExchangeName = exchangeName;
    this.rsauditingtable = rsauditingtable;
    this.minReadNumber = minReadNumber;
    this.pool = pool;
    this.databaseService = databaseService;
    this.dataBrokerService = dataBrokerService;
  }

  @Override
  public RatingService createRating(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String sub = request.getString(USER_ID);
    String id = request.getString(ID);
    StringBuilder query = new StringBuilder(AUDIT_INFO_QUERY.replace("$1", rsauditingtable).replace("$2", sub).replace("$3", id));
    Future<JsonObject> getRSAuditingInfo = getAuditingInfo(query);

    getRSAuditingInfo
        .onSuccess(
            successHandler -> {
              int countResourceAccess = successHandler.getInteger("totalHits");
              if (countResourceAccess > minReadNumber) {

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
              } else {
                LOGGER.error("Fail: Rating creation failed");
                handler.handle(Future.failedFuture(
                    new RespBuilder()
                        .withType(TYPE_FAIL)
                        .withTitle(TITLE_OPERATION_NOT_ALLOWED)
                        .withDetail("User has to access resource at least " + minReadNumber + " times to give rating")
                        .getResponse()));
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

    if (!request.containsKey(TYPE)) {
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
        "#",
        handler -> {
          if (handler.succeeded()) {
            LOGGER.info("Rating info publish to RabbitMQ");
          } else {
            LOGGER.error("Failed to publish Rating info");
          }
        });
  }
}
