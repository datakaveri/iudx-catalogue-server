package iudx.catalogue.server.database.mlayer;

import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.validator.Constants.VALIDATION_FAILURE_MSG;

import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.ElasticClient;
import iudx.catalogue.server.database.RespBuilder;
import iudx.catalogue.server.database.Util;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
                JsonObject instanceList = ar.result().resultAt(0);
                JsonObject datasetJson = ar.result().resultAt(2);
                for (int i = 0; i < datasetJson.getJsonArray("latestDataset").size(); i++) {
                  if (datasetJson
                      .getJsonArray("latestDataset")
                      .getJsonObject(i)
                      .containsKey(INSTANCE)) {
                    LOGGER.debug("given dataset has associated instance");
                    datasetJson
                        .getJsonArray("latestDataset")
                        .getJsonObject(i)
                        .put(
                            "icon",
                            instanceList
                                .getJsonObject("instanceIconPath")
                                .getString(
                                    datasetJson
                                        .getJsonArray("latestDataset")
                                        .getJsonObject(i)
                                        .getString(INSTANCE)
                                        .toLowerCase()));
                  } else {
                    LOGGER.debug("given dataset does not have associated instance");
                    datasetJson.getJsonArray("latestDataset").getJsonObject(i).put("icon", "");
                  }
                }
                for (int i = 0; i < datasetJson.getJsonArray("featuredDataset").size(); i++) {
                  if (datasetJson
                      .getJsonArray("featuredDataset")
                      .getJsonObject(i)
                      .containsKey(INSTANCE)) {
                    datasetJson
                        .getJsonArray("featuredDataset")
                        .getJsonObject(i)
                        .put(
                            "icon",
                            instanceList
                                .getJsonObject("instanceIconPath")
                                .getString(
                                    datasetJson
                                        .getJsonArray("featuredDataset")
                                        .getJsonObject(i)
                                        .getString(INSTANCE)));
                  } else {
                    datasetJson.getJsonArray("featuredDataset").getJsonObject(i).put("icon", "");
                  }
                }
                JsonArray domainList = ar.result().resultAt(1);
                JsonObject result = new JsonObject();
                result.mergeIn(datasetJson.getJsonObject("typeCount"));
                result
                    .put("totalInstance", instanceList.getInteger("totalInstance"))
                    .put("totalDomain", domainList.size())
                    .put("domains", domainList)
                    .put(INSTANCE, instanceList.getJsonArray("instanceList"))
                    .put("featuredDataset", datasetJson.getJsonArray("featuredDataset"))
                    .put("latestDataset", datasetJson.getJsonArray("latestDataset"));

                RespBuilder respBuilder =
                    new RespBuilder().withType(TYPE_SUCCESS).withTitle(SUCCESS).withResult(result);
                handler.handle(Future.succeededFuture(respBuilder.getJsonResponse()));
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
            int totalInstance = resultHandler.result().getInteger(TOTAL_HITS);
            Map<String, String> instanceIconPath = new HashMap<>();
            JsonArray instanceList = new JsonArray();
            for (int i = 0; i < resultHandler.result().getJsonArray(RESULTS).size(); i++) {
              JsonObject instance = resultHandler.result().getJsonArray(RESULTS).getJsonObject(i);
              instanceIconPath.put(
                  instance.getString("name").toLowerCase(), instance.getString("icon"));
              if (i < 4) {
                instanceList.add(i, instance);
              }
            }

            JsonObject json =
                new JsonObject()
                    .put("instanceIconPath", instanceIconPath)
                    .put("instanceList", instanceList)
                    .put("totalInstance", totalInstance);

            instanceResult.complete(json);
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
            ArrayList<JsonObject> latestDatasetArray = new ArrayList<JsonObject>();
            Map<String, JsonObject> resourceGroupMap = new HashMap<>();
            Map<String, String> providerDescription = new HashMap<>();
            if (getCatRecords
                    .result()
                    .getJsonArray(RESULTS).isEmpty()) {
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
                        JsonObject resourceItemCount =
                            resourceCount.future().result().getJsonObject("resourceItemCount");
                        JsonObject resourceAccessPolicy =
                            resourceCount.future().result().getJsonObject("resourceAccessPolicy");
                        int totalResourceItem = 0;

                        for (int i = 0; i < resultSize; i++) {
                          JsonObject record = results.getJsonObject(i);
                          String itemType = Util.getItemType(record);
                          if (itemType.equals(VALIDATION_FAILURE_MSG)) {
                            datasetResult.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
                          }
                          // making a map of all resource group and provider id and its description
                          if (ITEM_TYPE_RESOURCE_GROUP.equals(itemType)) {
                            String id = record.getString(ID);
                            int resourceItemCountInGroup =
                                resourceItemCount.containsKey(id)
                                    ? resourceItemCount.getInteger(id)
                                    : 0;
                            record.put("totalResources", resourceItemCountInGroup);
                            if (resourceAccessPolicy.containsKey(id)) {
                              record.put("accessPolicy", resourceAccessPolicy.getJsonObject(id));
                            } else {
                              record.put(
                                  "accessPolicy",
                                  new JsonObject().put("PII", 0).put("SECURE", 0).put("OPEN", 0));
                            }

                            // getting total count of resource items
                            totalResourceItem = totalResourceItem + resourceItemCountInGroup;
                            if (record.containsKey("itemCreatedAt")) {
                              latestDatasetArray.add(record);
                            }
                            resourceGroupMap.put(record.getString(ID), record);
                          } else if (ITEM_TYPE_PROVIDER.equals(itemType)) {
                            String description = record.getString(DESCRIPTION_ATTR);
                            String providerId = record.getString(ID);

                            providerDescription.put(providerId, description);
                          }
                        }
                        // sorting resource group based on the time of creation.
                        Collections.sort(latestDatasetArray, comapratorForLatestDataset());

                        JsonObject typeCount =
                            new JsonObject()
                                .put("totalDatasets", resourceGroupMap.size())
                                .put("totalResources", totalResourceItem);

                        if (instance.isBlank()) {
                          typeCount.put("totalPublishers", providerDescription.size());
                        } else {
                          typeCount.put(
                              "totalPublishers",
                              getCatRecords
                                  .result()
                                  .getJsonArray(RESULTS)
                                  .getJsonObject(0)
                                  .getInteger("providerCount"));
                        }

                        // making an arrayList of top six latest resource group
                        ArrayList<JsonObject> latestResourceGroup = new ArrayList<>();
                        int resourceGroupSize = Math.min(latestDatasetArray.size(), 6);
                        for (int i = 0; i < resourceGroupSize; i++) {
                          JsonObject resourceGroup = latestDatasetArray.get(i);
                          resourceGroup.put(
                              "providerDescription",
                              providerDescription.get(
                                  latestDatasetArray.get(i).getString(PROVIDER)));

                          latestResourceGroup.add(resourceGroup);
                        }

                        // making array list of most accessed resource groups
                        ArrayList<JsonObject> featuredResourceGroup = new ArrayList<>();
                        for (int resourceIndex = 0;
                            resourceIndex < frequentlyUsedResourceGroup.size();
                            resourceIndex++) {
                          String id =
                              frequentlyUsedResourceGroup
                                  .getJsonObject(resourceIndex)
                                  .getString("resource_group");
                          if (resourceGroupMap.containsKey(id)) {
                            JsonObject resourceGroup = resourceGroupMap.get(id);
                            resourceGroup.put(
                                "providerDescription",
                                providerDescription.get(resourceGroup.getString(PROVIDER)));
                            featuredResourceGroup.add(resourceGroup);
                            // removing the resourceGroup from resourceGroupMap after
                            // resources added to featuredResourceGroup array
                            resourceGroupMap.remove(id);
                          }
                        }

                        // Determining the number of resource group that can be added if
                        // total featured datasets are not 6. Max value is 6.
                        int remainingResources =
                            Math.min(6 - featuredResourceGroup.size(), resourceGroupMap.size());

                        /* Iterate through the values of 'resourceGroupMap' to add resources
                          to 'featuredResourceGroup' array while ensuring we don't exceed the
                          'remainingResources' limit. For each resource, we update its
                          'providerDescription' before adding it to the group.
                        */
                        for (JsonObject resourceGroup : resourceGroupMap.values()) {
                          if (remainingResources <= 0) {
                            break; // No need to continue if we've added enough resources
                          }
                          resourceGroup.put(
                              "providerDescription",
                              providerDescription.get(resourceGroup.getString("provider")));

                          featuredResourceGroup.add(resourceGroup);
                          remainingResources--;
                        }

                        JsonObject jsonDataset =
                            new JsonObject()
                                .put("latestDataset", latestResourceGroup)
                                .put("typeCount", typeCount)
                                .put("featuredDataset", featuredResourceGroup);
                        datasetResult.complete(jsonDataset);

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

  private Comparator<JsonObject> comapratorForLatestDataset() {
    Comparator<JsonObject> jsonComparator =
        new Comparator<JsonObject>() {

          @Override
          public int compare(JsonObject record1, JsonObject record2) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

            LocalDateTime dateTime1 =
                LocalDateTime.parse(record1.getString("itemCreatedAt"), formatter);
            LocalDateTime dateTime2 =
                LocalDateTime.parse(record2.getString("itemCreatedAt"), formatter);
            return dateTime2.compareTo(dateTime1);
          }
        };
    return jsonComparator;
  }
}
