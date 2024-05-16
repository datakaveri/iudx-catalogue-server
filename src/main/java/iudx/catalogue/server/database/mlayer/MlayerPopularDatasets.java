package iudx.catalogue.server.database.mlayer;

import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.util.Constants.*;

import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.ElasticClient;
import iudx.catalogue.server.database.RespBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MlayerPopularDatasets {
  private static final Logger LOGGER = LogManager.getLogger(MlayerPopularDatasets.class);
  private static String internalErrorResp =
      new RespBuilder()
          .withType(TYPE_INTERNAL_SERVER_ERROR)
          .withTitle(TITLE_INTERNAL_SERVER_ERROR)
          .withDetail(DETAIL_INTERNAL_SERVER_ERROR)
          .getResponse();
  ElasticClient client;
  String docIndex;
  String mlayerInstanceIndex;
  String mlayerDomainIndex;

  public MlayerPopularDatasets(
      ElasticClient client, String docIndex, String mlayerInstanceIndex, String mlayerDomainIndex) {
    this.client = client;
    this.docIndex = docIndex;
    this.mlayerInstanceIndex = mlayerInstanceIndex;
    this.mlayerDomainIndex = mlayerDomainIndex;
  }

  public void getMlayerPopularDatasets(
      String instance,
      JsonArray frequentlyUsedResourceGroup,
      Handler<AsyncResult<JsonObject>> handler) {

    Promise<JsonObject> instanceResult = Promise.promise();
    Promise<JsonArray> domainResult = Promise.promise();
    Promise<JsonObject> datasetResult = Promise.promise();

    searchSortedMlayerInstances(instanceResult);
    datasets(instance, datasetResult, frequentlyUsedResourceGroup);
    allMlayerDomains(domainResult);
    CompositeFuture.all(instanceResult.future(), domainResult.future(), datasetResult.future())
        .onComplete(
            ar -> {
              if (ar.succeeded()) {
                JsonObject result =
                    new JsonObject()
                        .put("instanceList", ar.result().resultAt(0))
                        .put("domainList", ar.result().resultAt(1))
                        .put("datasetJson", ar.result().resultAt(2));
                handler.handle(Future.succeededFuture(result));
              } else {
                LOGGER.error("Fail: failed DB request");
                if (ar.cause().getMessage().equals("No Content Available")) {
                  handler.handle(Future.failedFuture(ar.cause().getMessage()));
                }
                handler.handle(Future.failedFuture(internalErrorResp));
              }
            });
  }

  private void searchSortedMlayerInstances(Promise<JsonObject> instanceResult) {
    client.searchAsync(
        GET_SORTED_MLAYER_INSTANCES,
        mlayerInstanceIndex,
        resultHandler -> {
          if (resultHandler.succeeded()) {
            instanceResult.complete(resultHandler.result());
          } else {
            LOGGER.error("Fail: failed DB request");
            instanceResult.handle(Future.failedFuture(internalErrorResp));
          }
        });
  }

  private void allMlayerDomains(Promise<JsonArray> domainResult) {
    String getAllDomains = GET_ALL_MLAYER_DOMAIN_QUERY.replace("$0", "10000").replace("$2", "0");
    client.searchAsync(
        getAllDomains,
        mlayerDomainIndex,
        getDomainHandler -> {
          if (getDomainHandler.succeeded()) {
            JsonArray domainList = getDomainHandler.result().getJsonArray(RESULTS);
            domainResult.complete(domainList);
          } else {
            LOGGER.error("Fail: failed DB request");
            domainResult.handle(Future.failedFuture(internalErrorResp));
          }
        });
  }

  private void datasets(
      String instance, Promise<JsonObject> datasetResult, JsonArray frequentlyUsedResourceGroup) {
    String providerAndResources = "";
    if (instance.isBlank()) {
      providerAndResources = GET_PROVIDER_AND_RESOURCEGROUP;
    } else {
      providerAndResources = GET_DATASET_BY_INSTANCE.replace("$1", instance);
    }
    client.searchAsyncResourceGroupAndProvider(
        providerAndResources,
        docIndex,
        getCatRecords -> {
          if (getCatRecords.succeeded()) {
            if (getCatRecords.result().getJsonArray(RESULTS).isEmpty()) {
              datasetResult.handle(Future.failedFuture(NO_CONTENT_AVAILABLE));
            }
            JsonArray results =
                getCatRecords
                    .result()
                    .getJsonArray(RESULTS)
                    .getJsonObject(0)
                    .getJsonArray("resourceGroupAndProvider");
            int resultSize = results.size();

            Promise<JsonObject> resourceCount = Promise.promise();
            MlayerDataset mlayerDataset = new MlayerDataset(client, docIndex, mlayerInstanceIndex);
            // function to get the resource group items count
            mlayerDataset.gettingResourceAccessPolicyCount(resourceCount);
            resourceCount
                .future()
                .onComplete(
                    handler -> {
                      if (handler.succeeded()) {
                        resourceCount
                            .future()
                            .result()
                            .put("cat_results", results)
                            .put("resultSize", resultSize)
                            .put(
                                "totalPublishers",
                                getCatRecords
                                    .result()
                                    .getJsonArray(RESULTS)
                                    .getJsonObject(0)
                                    .getInteger("providerCount"))
                            .put("frequentlyUsedResourceGroup", frequentlyUsedResourceGroup);
                        datasetResult.complete(resourceCount.future().result());

                      } else {
                        LOGGER.error("Fail: failed DB request");
                        datasetResult.handle(Future.failedFuture(internalErrorResp));
                      }
                    });

          } else {
            LOGGER.error("Fail: failed DB request");
            datasetResult.handle(Future.failedFuture(internalErrorResp));
          }
        });
  }
}
