package iudx.catalogue.server.database.mlayer;

import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.util.Constants.TOTAL_HITS;

import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
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
  String docIndex;
  String mlayerInstanceIndex;

  public MlayerDataset(ElasticClient client, String docIndex, String mlayerInstanceIndex) {
    this.client = client;
    this.docIndex = docIndex;
    this.mlayerInstanceIndex = mlayerInstanceIndex;
  }

  public void getMlayerDataset(JsonObject requestData, Handler<AsyncResult<JsonObject>> handler) {

    LOGGER.debug("dataset Id" + requestData.getString(ID));
    client.searchAsync(
        GET_PROVIDER_AND_RS_ID.replace("$1", requestData.getString(ID)),
        docIndex,
        handlerRes -> {
          if (handlerRes.succeeded()) {
            if (handlerRes.result().getInteger(TOTAL_HITS) == 0) {
              LOGGER.debug("The dataset is not available.");
              handler.handle(
                  Future.failedFuture(
                      new RespBuilder()
                          .withType(TYPE_ITEM_NOT_FOUND)
                          .withTitle(TITLE_ITEM_NOT_FOUND)
                          .withDetail("dataset belonging to Id requested is not present")
                          .getResponse()));
            }
            String providerId =
                handlerRes.result().getJsonArray(RESULTS).getJsonObject(0).getString("provider");
            String cosId = "";
            if (handlerRes.result().getJsonArray(RESULTS).getJsonObject(0).containsKey("cos")) {
              cosId = handlerRes.result().getJsonArray(RESULTS).getJsonObject(0).getString("cos");
            }

            /*
            query to fetch resource group, provider of the resource group, resource
            items associated with the resource group and cos item.
            */
            String query =
                GET_MLAYER_DATASET
                    .replace("$1", requestData.getString(ID))
                    .replace("$2", providerId)
                    .replace("$3", cosId);
            LOGGER.debug("Query " + query);
            client.searchAsyncDataset(
                query,
                docIndex,
                resultHandler -> {
                  if (resultHandler.succeeded()) {
                    LOGGER.debug("Success: Successful DB Request");
                    JsonObject record =
                        resultHandler.result().getJsonArray(RESULTS).getJsonObject(0);
                    record
                        .getJsonObject("dataset")
                        .put("totalResources", record.getJsonArray("resource").size());
                    String instanceName = "";
                    String instanceCapitalizeName = "";
                    if (record.getJsonObject("dataset").containsKey(INSTANCE)
                        && !(record.getJsonObject("dataset").getString(INSTANCE) == null)
                        && !(record.getJsonObject("dataset").getString(INSTANCE).isBlank())) {

                      instanceName = record.getJsonObject("dataset").getString(INSTANCE);
                      instanceCapitalizeName =
                          instanceName.substring(0, 1).toUpperCase() + instanceName.substring(1);

                      // query to get the icon path of the instance in the  resource group
                      String getIconQuery =
                          GET_MLAYER_INSTANCE_ICON.replace("$1", instanceCapitalizeName);
                      client.searchAsync(
                          getIconQuery,
                          mlayerInstanceIndex,
                          iconResultHandler -> {
                            if (iconResultHandler.succeeded()) {
                              LOGGER.debug("Success: Successful DB Request");
                              JsonObject instances = iconResultHandler.result();
                              if (instances.getInteger(TOTAL_HITS) == 0) {
                                LOGGER.debug("The icon path for the instance is not present.");
                                record.getJsonObject("dataset").put("instance_icon", "");
                              } else {
                                JsonObject resource =
                                    instances.getJsonArray(RESULTS).getJsonObject(0);
                                String instancePath = resource.getString("icon");
                                record.getJsonObject("dataset").put("instance_icon", instancePath);
                              }
                              resultHandler.result().remove(TOTAL_HITS);
                              handler.handle(Future.succeededFuture(resultHandler.result()));
                            } else {
                              LOGGER.error("Fail: failed DB request inner");
                              LOGGER.error(resultHandler.cause());
                              handler.handle(Future.failedFuture(internalErrorResp));
                            }
                          });
                    } else {
                      resultHandler.result().remove(TOTAL_HITS);
                      record.getJsonObject("dataset").put("instance_icon", "");
                      handler.handle(Future.succeededFuture(resultHandler.result()));
                    }
                  } else {
                    LOGGER.error("Fail: failed DB request outer");
                    LOGGER.error(resultHandler.cause());
                    handler.handle(Future.failedFuture(internalErrorResp));
                  }
                });
          } else {
            LOGGER.error("Fail: DB request to get provider failed.");
            handler.handle(Future.failedFuture(internalErrorResp));
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
                DataModel dataModel = new DataModel(client, docIndex);
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
              LOGGER.error("getInstance enexpectedly failed : {}", e.getMessage());
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
