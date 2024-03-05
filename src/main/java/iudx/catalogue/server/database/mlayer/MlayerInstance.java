package iudx.catalogue.server.database.mlayer;

import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.mlayer.util.Constants.INSTANCE_ID;
import static iudx.catalogue.server.mlayer.util.Constants.MLAYER_ID;
import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.ElasticClient;
import iudx.catalogue.server.database.RespBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MlayerInstance {
  private static final Logger LOGGER = LogManager.getLogger(MlayerInstance.class);
  private static String internalErrorResp =
      new RespBuilder()
          .withType(TYPE_INTERNAL_SERVER_ERROR)
          .withTitle(TITLE_INTERNAL_SERVER_ERROR)
          .withDetail(DETAIL_INTERNAL_SERVER_ERROR)
          .getResponse();
  ElasticClient client;
  String mlayerInstanceIndex;

  public MlayerInstance(ElasticClient client, String mlayerInstanceIndex) {
    this.client = client;
    this.mlayerInstanceIndex = mlayerInstanceIndex;
  }

  public void getMlayerInstance(String id, Handler<AsyncResult<JsonObject>> handler) {
    String query = "";
    if (id == null || id.isBlank()) {
      query = GET_ALL_MLAYER_INSTANCE_QUERY;
    } else {
      query = GET_MLAYER_INSTANCE_QUERY.replace("$1", id);
    }
    client.searchAsync(
        query,
        mlayerInstanceIndex,
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

  public void deleteMlayerInstance(String instanceId, Handler<AsyncResult<JsonObject>> handler) {
    RespBuilder respBuilder = new RespBuilder();

    String checkForExistingRecord =
        CHECK_MDOC_QUERY_INSTANCE.replace("$1", instanceId).replace("$2", "");

    client.searchGetId(
        checkForExistingRecord,
        mlayerInstanceIndex,
        checkRes -> {
          if (checkRes.failed()) {
            LOGGER.error("Fail: Check query fail;" + checkRes.cause());
            handler.handle(Future.failedFuture(internalErrorResp));
          } else {
            if (checkRes.result().getInteger(TOTAL_HITS) != 1) {
              LOGGER.error("Fail: Instance doesn't exist, can't delete");
              handler.handle(
                  Future.failedFuture(
                      respBuilder
                          .withType(TYPE_ITEM_NOT_FOUND)
                          .withTitle(TITLE_ITEM_NOT_FOUND)
                          .withResult(instanceId, "Fail: Instance doesn't exist, can't delete")
                          .getResponse()));
              return;
            }
            String docId = checkRes.result().getJsonArray(RESULTS).getString(0);

            client.docDelAsync(
                docId,
                mlayerInstanceIndex,
                delRes -> {
                  if (delRes.succeeded()) {
                    handler.handle(
                        Future.succeededFuture(
                            respBuilder
                                .withType(TYPE_SUCCESS)
                                .withTitle(SUCCESS)
                                .withResult(instanceId, "Instance deleted Successfully")
                                .getJsonResponse()));
                  } else {
                    handler.handle(Future.failedFuture(internalErrorResp));
                    LOGGER.error("Fail: Deletion failed;" + delRes.cause());
                  }
                });
          }
        });
  }

  public void createMlayerInstance(
      JsonObject instanceDoc, Handler<AsyncResult<JsonObject>> handler) {

    RespBuilder respBuilder = new RespBuilder();
    String instanceId = instanceDoc.getString(INSTANCE_ID);
    String id = instanceDoc.getString(MLAYER_ID);
    String checkForExistingRecord = CHECK_MDOC_QUERY.replace("$1", id).replace("$2", "");
    client.searchAsync(
        checkForExistingRecord,
        mlayerInstanceIndex,
        res -> {
          if (res.failed()) {
            LOGGER.error("Fail: Insertion of mlayer Instance failed: " + res.cause());
            handler.handle(
                Future.failedFuture(
                    respBuilder
                        .withType(FAILED)
                        .withResult(MLAYER_ID)
                        .withDetail("Fail: Insertion of Instance failed")
                        .getResponse()));

          } else {
            if (res.result().getInteger(TOTAL_HITS) != 0) {
              JsonObject json = new JsonObject(res.result().getJsonArray(RESULTS).getString(0));
              String instanceIdExists = json.getString(INSTANCE_ID);

              handler.handle(
                  Future.failedFuture(
                      respBuilder
                          .withType(TYPE_ALREADY_EXISTS)
                          .withTitle(TITLE_ALREADY_EXISTS)
                          .withResult(instanceIdExists, " Fail: Instance Already Exists")
                          .withDetail(" Fail: Instance Already Exists")
                          .getResponse()));
              return;
            }
            client.docPostAsync(
                mlayerInstanceIndex,
                instanceDoc.toString(),
                result -> {
                  if (result.succeeded()) {
                    handler.handle(
                        Future.succeededFuture(
                            respBuilder
                                .withType(TYPE_SUCCESS)
                                .withTitle(SUCCESS)
                                .withResult(instanceId, "Instance created Sucesssfully")
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

  public void updateMlayerInstance(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    RespBuilder respBuilder = new RespBuilder();
    String instanceId = request.getString(INSTANCE_ID);
    String checkForExistingRecord =
        CHECK_MDOC_QUERY_INSTANCE.replace("$1", instanceId).replace("$2", "");
    client.searchAsyncGetId(
        checkForExistingRecord,
        mlayerInstanceIndex,
        checkRes -> {
          if (checkRes.failed()) {
            LOGGER.error("Fail: Check query fail;" + checkRes.cause());
            handler.handle(Future.failedFuture(internalErrorResp));
          } else {
            // LOGGER.debug(checkRes.result());
            if (checkRes.result().getInteger(TOTAL_HITS) != 1) {
              LOGGER.error("Fail: Instance doesn't exist, can't update");
              handler.handle(
                  Future.failedFuture(
                      respBuilder
                          .withType(TYPE_ITEM_NOT_FOUND)
                          .withTitle(TITLE_ITEM_NOT_FOUND)
                          .withResult(instanceId, "Fail : Instance doesn't exist, can't update")
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
                  mlayerInstanceIndex,
                  request.toString(),
                  putRes -> {
                    if (putRes.succeeded()) {
                      handler.handle(
                          Future.succeededFuture(
                              respBuilder
                                  .withType(TYPE_SUCCESS)
                                  .withTitle(SUCCESS)
                                  .withResult(instanceId, "Instance Updated Successfully")
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
}
