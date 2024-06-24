package iudx.catalogue.server.database.mlayer;

import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import iudx.catalogue.server.database.ElasticClient;
import iudx.catalogue.server.database.RespBuilder;
import iudx.catalogue.server.mlayer.vocabulary.DataModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MlayerDataset {
  private static final Logger LOGGER = LogManager.getLogger(MlayerDataset.class);
  private static String internalErrorResp =
      new RespBuilder()
          .withType(TYPE_INTERNAL_SERVER_ERROR)
          .withTitle(TITLE_INTERNAL_SERVER_ERROR)
          .withDetail(DETAIL_INTERNAL_SERVER_ERROR)
          .getResponse();
  ElasticClient client;
  WebClient webClient;
  String docIndex;
  String mlayerInstanceIndex;

  public MlayerDataset(ElasticClient client, String docIndex, String mlayerInstanceIndex) {
    this.client = client;
    this.docIndex = docIndex;
    this.mlayerInstanceIndex = mlayerInstanceIndex;
    this.webClient = WebClient.create(Vertx.vertx());
  }

  public void getProviderAndResourceServerId(
      String query, Handler<AsyncResult<JsonObject>> handler) {
    client.searchAsync(
        query,
        docIndex,
        handlerRes -> {
          if (handlerRes.succeeded()) {
            if (handlerRes.result().getInteger(TOTAL_HITS) == 0) {
              handler.handle(
                  Future.failedFuture(
                      new RespBuilder()
                          .withType(TYPE_ITEM_NOT_FOUND)
                          .withTitle(TITLE_ITEM_NOT_FOUND)
                          .withDetail("Dataset belonging to requested ID is not present")
                          .getResponse()));
              return;
            }
            handler.handle(Future.succeededFuture(handlerRes.result()));
          } else {
            LOGGER.error("Fail: DB request to get provider failed.");
            handler.handle(Future.failedFuture(internalErrorResp));
          }
        });
  }

  public void getDataset(String query, Handler<AsyncResult<JsonObject>> handler) {
    client.searchAsyncDataset(
        query,
        docIndex,
        result -> {
          if (result.succeeded()) {
            LOGGER.debug("Success: Successful DB Request");
            handler.handle(Future.succeededFuture(result.result()));
          } else {
            LOGGER.error("Fail: failed DB request outer", result.cause());
            handler.handle(Future.failedFuture(result.cause()));
          }
        });
  }

  public void getInstanceIcon(String getIconQuery, Handler<AsyncResult<JsonObject>> handler) {
    client.searchAsync(
        getIconQuery,
        mlayerInstanceIndex,
        result -> {
          if (result.succeeded()) {
            LOGGER.debug("Success: Successful DB Request");
            handler.handle(Future.succeededFuture(result.result()));
          } else {
            LOGGER.error("Fail: failed DB request inner", result.cause());
            handler.handle(Future.failedFuture(result.cause()));
          }
        });
  }

  public void getMlayerAllDatasets(
      JsonObject requestParam, String query, Handler<AsyncResult<JsonObject>> handler) {

    LOGGER.debug("Getting all the resource group items");
    Promise<JsonObject> datasetResult = Promise.promise();
    Promise<JsonObject> instanceResult = Promise.promise();
    Promise<JsonObject> resourceCount = Promise.promise();

    gettingAllDatasets(query, datasetResult);
    allMlayerInstance(instanceResult);
    gettingResourceAccessPolicyCount(resourceCount);

    CompositeFuture.all(instanceResult.future(), datasetResult.future(), resourceCount.future())
        .onComplete(
            ar -> {
              if (ar.succeeded()) {
                DataModel dataModel = new DataModel(client, docIndex, webClient);
                dataModel
                    .getDataModelInfo()
                    .onComplete(
                        domainInfoResult -> {
                          if (domainInfoResult.succeeded()) {
                            JsonObject domains = domainInfoResult.result();
                            JsonObject result =
                                new JsonObject()
                                    .put("instanceResult", ar.result().resultAt(0))
                                    .put("resourceGroupList", ar.result().resultAt(1))
                                    .put("resourceAndPolicyCount", ar.result().resultAt(2))
                                    .put("idAndDomainList", domains);
                            LOGGER.debug("getMlayerDatasets succeeded");
                            handler.handle(Future.succeededFuture(result));
                          } else {
                            LOGGER.error("Fail: failed DataModel request");
                            handler.handle(Future.failedFuture(internalErrorResp));
                          }
                        });
              } else {
                LOGGER.error("Fail: failed DB request");
                handler.handle(Future.failedFuture(ar.cause().getMessage()));
              }
            });
  }

  private void gettingAllDatasets(String query, Promise<JsonObject> datasetResult) {

    LOGGER.debug(
        "Getting all resourceGroup along with provider description, "
            + "resource server url and cosUrl");
    client.searchAsync(
        query,
        docIndex,
        resultHandler -> {
          if (resultHandler.succeeded()) {
            try {
              datasetResult.complete(resultHandler.result());
            } catch (Exception e) {
              LOGGER.error("getRGs unexpectedly failed : {}", e.getMessage());
              datasetResult.fail(e.getMessage());
            }
          } else {
            LOGGER.error("Fail: failed DB request");
            datasetResult.handle(Future.failedFuture(internalErrorResp));
          }
        });
  }

  private void allMlayerInstance(Promise<JsonObject> instanceResult) {
    LOGGER.debug("Getting all instance name and icons");
    client.searchAsync(
        GET_ALL_MLAYER_INSTANCES,
        mlayerInstanceIndex,
        instanceRes -> {
          if (instanceRes.succeeded()) {
            try {
              instanceResult.complete(instanceRes.result());
            } catch (Exception e) {
              LOGGER.error("getInstance unexpectedly failed : {}", e.getMessage());
              instanceResult.fail(e.getMessage());
            }
          } else {

            LOGGER.error("Fail: query fail;" + instanceRes.cause());
            instanceResult.handle(Future.failedFuture(internalErrorResp));
          }
        });
  }

  public void gettingResourceAccessPolicyCount(Promise<JsonObject> resourceCountResult) {
    LOGGER.debug("Getting resource item count");
    String query = RESOURCE_ACCESSPOLICY_COUNT;
    client.resourceAggregationAsync(
        query,
        docIndex,
        resourceCountRes -> {
          if (resourceCountRes.succeeded()) {
            try {
              resourceCountResult.complete(resourceCountRes.result());
            } catch (Exception e) {
              LOGGER.error("resourceAP unexpectedly failed : {}", e.getMessage());
              resourceCountResult.fail(e.getMessage());
            }
          } else {
            LOGGER.error("Fail: query fail;" + resourceCountRes.cause());
            resourceCountResult.handle(Future.failedFuture(internalErrorResp));
          }
        });
  }
}
