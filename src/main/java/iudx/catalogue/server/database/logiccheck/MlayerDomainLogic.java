package iudx.catalogue.server.database.logiccheck;

import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.mlayer.util.Constants.DOMAIN_ID;
import static iudx.catalogue.server.mlayer.util.Constants.MLAYER_ID;
import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.util.Constants.DETAIL_INTERNAL_SERVER_ERROR;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.ElasticClient;
import iudx.catalogue.server.database.RespBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MlayerDomainLogic {
  private static final Logger LOGGER = LogManager.getLogger(MlayerDomainLogic.class);
  private static String internalErrorResp =
      new RespBuilder()
          .withType(TYPE_INTERNAL_SERVER_ERROR)
          .withTitle(TITLE_INTERNAL_SERVER_ERROR)
          .withDetail(DETAIL_INTERNAL_SERVER_ERROR)
          .getResponse();
  ElasticClient client;
  String mlayerDomainIndex;

  public MlayerDomainLogic(ElasticClient client, String mlayerDomainIndex) {
    this.client = client;
    this.mlayerDomainIndex = mlayerDomainIndex;
  }

  public void createMlayerDomain(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    RespBuilder respBuilder = new RespBuilder();
    String domainId = request.getString(DOMAIN_ID);
    String id = request.getString(MLAYER_ID);
    String checkForExistingDomain = CHECK_MDOC_QUERY.replace("$1", id).replace("$2", "");
    client.searchAsync(
        checkForExistingDomain,
        mlayerDomainIndex,
        res -> {
          if (res.failed()) {
            LOGGER.error("Fail: Insertion of mLayer domain failed: " + res.cause());
            handler.handle(
                Future.failedFuture(
                    respBuilder
                        .withType(FAILED)
                        .withResult(id)
                        .withDetail("Fail: Insertion of mLayer domain failed: ")
                        .getResponse()));

          } else {
            if (res.result().getInteger(TOTAL_HITS) != 0) {
              JsonObject json = new JsonObject(res.result().getJsonArray(RESULTS).getString(0));
              String domainIdExists = json.getString(DOMAIN_ID);
              handler.handle(
                  Future.failedFuture(
                      respBuilder
                          .withType(TYPE_ALREADY_EXISTS)
                          .withTitle(TITLE_ALREADY_EXISTS)
                          .withResult(domainIdExists, "Fail: Domain Already Exists")
                          .withDetail("Fail: Domain Already Exists")
                          .getResponse()));
              return;
            }
            client.docPostAsync(
                mlayerDomainIndex,
                request.toString(),
                result -> {
                  if (result.succeeded()) {
                    handler.handle(
                        Future.succeededFuture(
                            respBuilder
                                .withType(TYPE_SUCCESS)
                                .withTitle(SUCCESS)
                                .withResult(domainId, "domain Created Successfully")
                                .getJsonResponse()));
                  } else {
                    handler.handle(
                        Future.failedFuture(
                            respBuilder
                                .withType(TYPE_FAIL)
                                .withResult(FAILED)
                                .withDetail(DETAIL_INTERNAL_SERVER_ERROR)
                                .getResponse()));

                    LOGGER.error("Fail: Insertion failed" + result.cause());
                  }
                });
          }
        });
  }

  public void getMlayerDomain(String id, Handler<AsyncResult<JsonObject>> handler) {
    String query = "";
    if (id == null || id.isBlank()) {
      query = GET_ALL_MLAYER_DOMAIN_QUERY;
    } else {
      query = GET_MLAYER_DOMAIN_QUERY.replace("$1", id);
    }
    client.searchAsync(
        query,
        mlayerDomainIndex,
        resultHandler -> {
          if (resultHandler.succeeded()) {
            LOGGER.debug("Success: Successful DB Request");
            JsonObject result = resultHandler.result();
            handler.handle(Future.succeededFuture(result));
          } else {
            LOGGER.error("Fail: failed DB request");
            handler.handle(Future.failedFuture(internalErrorResp));
          }
        });
  }

  public void updateMlayerDomain(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    RespBuilder respBuilder = new RespBuilder();
    String domainId = request.getString(DOMAIN_ID);
    String checkForExistingRecord =
        CHECK_MDOC_QUERY_DOMAIN.replace("$1", domainId).replace("$2", "");
    client.searchAsyncGetId(
        checkForExistingRecord,
        mlayerDomainIndex,
        checkRes -> {
          if (checkRes.failed()) {
            LOGGER.error("Fail: Check Query Fail");
            handler.handle(Future.failedFuture(internalErrorResp));
          } else {
            LOGGER.debug(checkRes.result());
            if (checkRes.result().getInteger(TOTAL_HITS) != 1) {
              LOGGER.error("Fail: Domain does not exist, can't update");
              handler.handle(
                  Future.failedFuture(
                      respBuilder
                          .withType(TYPE_ITEM_NOT_FOUND)
                          .withTitle(TITLE_ITEM_NOT_FOUND)
                          .withResult(domainId, "Fail: Domain doesn't exist, can't update")
                          .withDetail("Fail: Domain doesn't exist, can't update")
                          .getResponse()));
              return;
            }

            JsonObject result =
                new JsonObject(checkRes.result().getJsonArray(RESULTS).getString(0));

            String parameterIdName = result.getJsonObject(SOURCE).getString("name").toLowerCase();
            String requestBodyName = request.getString("name").toLowerCase();
            if (parameterIdName.equals(requestBodyName)) {
              String docId = result.getString(DOC_ID);
              client.docPutAsync(
                  docId,
                  mlayerDomainIndex,
                  request.toString(),
                  putRes -> {
                    if (putRes.succeeded()) {
                      handler.handle(
                          Future.succeededFuture(
                              respBuilder
                                  .withType(TYPE_SUCCESS)
                                  .withTitle(SUCCESS)
                                  .withResult(domainId, "Domain Updated Successfully")
                                  .getJsonResponse()));
                    } else {
                      handler.handle(Future.failedFuture(internalErrorResp));
                      LOGGER.error("Fail: Updation failed" + putRes.cause());
                    }
                  });
            } else {
              handler.handle(
                  Future.failedFuture(
                      respBuilder
                          .withType(TYPE_FAIL)
                          .withTitle(TITLE_WRONG_INSTANCE_NAME)
                          .withDetail(WRONG_INSTANCE_NAME)
                          .getResponse()));
              LOGGER.error("Fail: Updation Failed" + checkRes.cause());
            }
          }
        });
  }

  public void deleteMlayerDomain(String domainId, Handler<AsyncResult<JsonObject>> handler) {
    RespBuilder respBuilder = new RespBuilder();
    LOGGER.debug(domainId);

    String checkForExistingRecord =
        CHECK_MDOC_QUERY_DOMAIN.replace("$1", domainId).replace("$2", "");

    client.searchGetId(
        checkForExistingRecord,
        mlayerDomainIndex,
        checkRes -> {
          if (checkRes.failed()) {
            LOGGER.error("Fail: Check query fail;" + checkRes.cause());
            handler.handle(Future.failedFuture(internalErrorResp));
          } else {
            if (checkRes.result().getInteger(TOTAL_HITS) != 1) {
              LOGGER.error("Fail: Domain doesn't exist, can't delete");
              handler.handle(
                  Future.failedFuture(
                      respBuilder
                          .withType(TYPE_ITEM_NOT_FOUND)
                          .withTitle(TITLE_ITEM_NOT_FOUND)
                          .withResult(domainId, "Fail: Domain doesn't exist, can't delete")
                          .withDetail("Fail: Domain doesn't exist, can't delete")
                          .getResponse()));
              return;
            }

            String docId = checkRes.result().getJsonArray(RESULTS).getString(0);

            client.docDelAsync(
                docId,
                mlayerDomainIndex,
                putRes -> {
                  if (putRes.succeeded()) {

                    handler.handle(
                        Future.succeededFuture(
                            respBuilder
                                .withType(TYPE_SUCCESS)
                                .withTitle(SUCCESS)
                                .withResult(domainId, "Domain deleted Successfully")
                                .getJsonResponse()));
                  } else {
                    handler.handle(Future.failedFuture(internalErrorResp));
                    LOGGER.error("Fail: Deletion failed;" + putRes.cause());
                  }
                });
          }
        });
  }
}
