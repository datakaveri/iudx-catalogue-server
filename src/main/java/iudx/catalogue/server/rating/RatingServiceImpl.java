package iudx.catalogue.server.rating;

import static iudx.catalogue.server.auditing.util.Constants.ID;
import static iudx.catalogue.server.geocoding.util.Constants.TYPE;
import static iudx.catalogue.server.rating.util.Constants.*;
import static iudx.catalogue.server.util.Constants.*;

import com.google.common.hash.Hashing;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.apiserver.util.RespBuilder;
import iudx.catalogue.server.database.elastic.ElasticsearchService;
import iudx.catalogue.server.database.postgres.PostgresService;
import iudx.catalogue.server.databroker.DataBrokerService;

import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class RatingServiceImpl implements RatingService {
  private static final Logger LOGGER = LogManager.getLogger(RatingServiceImpl.class);
    ElasticsearchService databaseService;
  DataBrokerService dataBrokerService;
  PostgresService postgresService;
  private String ratingExchangeName;
  private final String rsauditingtable;
  private final int minReadNumber;

  /**
   * Constructor for RatingServiceImpl class. Initializes the object with the given parameters.
   * @param exchangeName the name of the exchange used for rating
   * @param rsauditingtable the name of the table used for auditing the rating system
   * @param minReadNumber the minimum number of reads for a rating to be considered valid
   * @param databaseService the service used for interacting with the database
   * @param dataBrokerService the service used for interacting with the data broker
   * @param postgresService the service used for interacting with the PostgreSQL database
   */
  public RatingServiceImpl(
      String exchangeName,
      String rsauditingtable,
      int minReadNumber,
      ElasticsearchService databaseService,
      DataBrokerService dataBrokerService,
      PostgresService postgresService) {
    this.ratingExchangeName = exchangeName;
    this.rsauditingtable = rsauditingtable;
    this.minReadNumber = minReadNumber;
    this.databaseService = databaseService;
    this.dataBrokerService = dataBrokerService;
    this.postgresService = postgresService;
  }

  @Override
  public RatingService createRating(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String sub = request.getString(USER_ID);
    String id = request.getString(ID);
    StringBuilder query = new StringBuilder(AUDIT_INFO_QUERY
            .replace("$1", rsauditingtable).replace("$2", sub).replace("$3", id));
    Future<JsonObject> getRsAuditingInfo = getAuditingInfo(query);

    getRsAuditingInfo
        .onSuccess(
            successHandler -> {
              int countResourceAccess = successHandler.getInteger("totalHits");
              if (countResourceAccess > minReadNumber) {

                String ratingId =
                    Hashing.sha256().hashString(sub + id, StandardCharsets.UTF_8).toString();

                request.put(RATING_ID, ratingId);

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
                        .withType(TYPE_ACCESS_DENIED)
                        .withTitle(TITLE_REQUIREMENTS_NOT_MET)
                        .withDetail("User has to access resource at least "
                                + minReadNumber + " times to give rating")
                        .getResponse()));
              }
            })
        .onFailure(
            failureHandler -> {
              LOGGER.error(
                  "User has not accessed resource"
                          + " before and hence is not authorised to give rating");
              handler.handle(Future.failedFuture(failureHandler.getMessage()));
            });

    return this;
  }

  @Override
  public RatingService getRating(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String id = request.getString(ID);
    if (!request.containsKey(TYPE)) {
      String sub = request.getString(USER_ID);
      String ratingId = Hashing.sha256().hashString(sub + id, StandardCharsets.UTF_8).toString();

      request.put(RATING_ID, ratingId);
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

    String ratingId = Hashing.sha256().hashString(sub + id, StandardCharsets.UTF_8).toString();

    request.put(RATING_ID, ratingId);

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

    String ratingId = Hashing.sha256().hashString(sub + id, StandardCharsets.UTF_8).toString();

    request.put(RATING_ID, ratingId);

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
    postgresService.executeCountQuery(query.toString(), pgHandler -> {
      if (pgHandler.succeeded()) {
        promise.complete(pgHandler.result());
      } else {
        promise.fail(pgHandler.cause());
      }
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
