package iudx.catalogue.server.mlayer;

import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.mlayer.util.Constants.*;
import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.util.Constants.NAME;
import static iudx.catalogue.server.util.Constants.PROVIDERS;
import static iudx.catalogue.server.validator.Constants.VALIDATION_FAILURE_MSG;

import com.google.common.hash.Hashing;
import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.DatabaseService;
import iudx.catalogue.server.database.RespBuilder;
import iudx.catalogue.server.database.Util;
import iudx.catalogue.server.database.postgres.PostgresService;
import iudx.catalogue.server.mlayer.util.QueryBuilder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MlayerServiceImpl implements MlayerService {
  private static final Logger LOGGER = LogManager.getLogger(MlayerServiceImpl.class);
  private static String internalErrorResp =
      new RespBuilder()
          .withType(TYPE_INTERNAL_SERVER_ERROR)
          .withTitle(TITLE_INTERNAL_SERVER_ERROR)
          .withDetail(DETAIL_INTERNAL_SERVER_ERROR)
          .getResponse();
  DatabaseService databaseService;
  PostgresService postgresService;
  QueryBuilder queryBuilder = new QueryBuilder();
  private String databaseTable;
  private String catSummaryTable;
  private JsonObject configJson;
  private JsonArray excludedIdsJson;

  MlayerServiceImpl(
      DatabaseService databaseService, PostgresService postgresService, JsonObject config) {
    this.databaseService = databaseService;
    this.postgresService = postgresService;
    this.configJson = config;
    databaseTable = configJson.getString("databaseTable");
    catSummaryTable = configJson.getString("catSummaryTable");
    excludedIdsJson = configJson.getJsonArray("excluded_ids");
  }

  @Override
  public MlayerService createMlayerInstance(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String name = request.getString(NAME).toLowerCase();
    String id = Hashing.sha256().hashString(name, StandardCharsets.UTF_8).toString();
    String instanceId = UUID.randomUUID().toString();
    if (!request.containsKey("instanceId")) {
      request.put(INSTANCE_ID, instanceId);
    }
    request.put(MLAYER_ID, id);

    databaseService.createMlayerInstance(
        request,
        createMlayerInstanceHandler -> {
          if (createMlayerInstanceHandler.succeeded()) {
            LOGGER.info("Success: Mlayer Instance Recorded");
            handler.handle(Future.succeededFuture(createMlayerInstanceHandler.result()));
          } else {
            LOGGER.error("Fail: Mlayer Instance creation failed");
            handler.handle(Future.failedFuture(createMlayerInstanceHandler.cause()));
          }
        });
    return this;
  }

  @Override
  public MlayerService getMlayerInstance(
      JsonObject requestParams, Handler<AsyncResult<JsonObject>> handler) {

    databaseService.getMlayerInstance(
        requestParams,
        getMlayerInstancehandler -> {
          if (getMlayerInstancehandler.succeeded()) {
            LOGGER.info("Success: Getting all Instance Values");
            handler.handle(Future.succeededFuture(getMlayerInstancehandler.result()));
          } else {
            LOGGER.error("Fail: Getting all instances failed");
            handler.handle(Future.failedFuture(getMlayerInstancehandler.cause()));
          }
        });
    return this;
  }

  @Override
  public MlayerService deleteMlayerInstance(
      String request, Handler<AsyncResult<JsonObject>> handler) {
    databaseService.deleteMlayerInstance(
        request,
        deleteMlayerInstanceHandler -> {
          if (deleteMlayerInstanceHandler.succeeded()) {
            LOGGER.info("Success: Mlayer Instance Deleted");
            handler.handle(Future.succeededFuture(deleteMlayerInstanceHandler.result()));
          } else {
            LOGGER.error("Fail: Mlayer Instance deletion failed");
            handler.handle(Future.failedFuture(deleteMlayerInstanceHandler.cause()));
          }
        });
    return this;
  }

  @Override
  public MlayerService updateMlayerInstance(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String name = request.getString(NAME).toLowerCase();
    String id = Hashing.sha256().hashString(name, StandardCharsets.UTF_8).toString();
    LOGGER.debug(id);
    request.put("id", id);
    databaseService.updateMlayerInstance(
        request,
        updateMlayerHandler -> {
          if (updateMlayerHandler.succeeded()) {
            LOGGER.info("Success: mlayer instance Updated");
            handler.handle(Future.succeededFuture(updateMlayerHandler.result()));
          } else {
            LOGGER.error("Fail: Mlayer Instance updation failed");
            handler.handle(Future.failedFuture(updateMlayerHandler.cause()));
          }
        });

    return this;
  }

  @Override
  public MlayerService createMlayerDomain(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String name = request.getString(NAME).toLowerCase();
    String id = Hashing.sha256().hashString(name, StandardCharsets.UTF_8).toString();
    String domainId = UUID.randomUUID().toString();
    if (!request.containsKey("domainId")) {
      request.put(DOMAIN_ID, domainId);
    }
    request.put(MLAYER_ID, id);

    databaseService.createMlayerDomain(
        request,
        createMlayerDomainHandler -> {
          if (createMlayerDomainHandler.succeeded()) {
            LOGGER.info("Success: Mlayer Domain Recorded");
            handler.handle(Future.succeededFuture(createMlayerDomainHandler.result()));
          } else {
            LOGGER.error("Fail: Mlayer Domain creation failed");
            handler.handle(Future.failedFuture(createMlayerDomainHandler.cause()));
          }
        });
    return this;
  }

  @Override
  public MlayerService getMlayerDomain(
      JsonObject requestParams, Handler<AsyncResult<JsonObject>> handler) {
    databaseService.getMlayerDomain(
        requestParams,
        getMlayerDomainHandler -> {
          if (getMlayerDomainHandler.succeeded()) {
            LOGGER.info("Success: Getting all domain values");
            handler.handle(Future.succeededFuture(getMlayerDomainHandler.result()));
          } else {
            LOGGER.error("Fail: Getting all domains failed");
            handler.handle(Future.failedFuture(getMlayerDomainHandler.cause()));
          }
        });
    return this;
  }

  @Override
  public MlayerService deleteMlayerDomain(
      String request, Handler<AsyncResult<JsonObject>> handler) {
    databaseService.deleteMlayerDomain(
        request,
        deleteMlayerDomainHandler -> {
          if (deleteMlayerDomainHandler.succeeded()) {
            LOGGER.info("Success: Mlayer Doamin Deleted");
            handler.handle(Future.succeededFuture(deleteMlayerDomainHandler.result()));
          } else {
            LOGGER.error("Fail: Mlayer Domain deletion failed");
            handler.handle(Future.failedFuture(deleteMlayerDomainHandler.cause()));
          }
        });
    return this;
  }

  @Override
  public MlayerService updateMlayerDomain(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String name = request.getString(NAME).toLowerCase();
    LOGGER.debug(name);
    String id = Hashing.sha256().hashString(name, StandardCharsets.UTF_8).toString();
    LOGGER.debug(id);
    request.put(MLAYER_ID, id);
    databaseService.updateMlayerDomain(
        request,
        updateMlayerHandler -> {
          if (updateMlayerHandler.succeeded()) {
            LOGGER.info("Success: mlayer domain updated");
            handler.handle(Future.succeededFuture(updateMlayerHandler.result()));
          } else {
            LOGGER.error("Fail: Mlayer Domain updation Failed");
            handler.handle(Future.failedFuture(updateMlayerHandler.cause()));
          }
        });

    return this;
  }

  @Override
  public MlayerService getMlayerProviders(
      JsonObject requestParams, Handler<AsyncResult<JsonObject>> handler) {
    databaseService.getMlayerProviders(
        requestParams,
        resultHandler -> {
          if (resultHandler.succeeded()) {
            JsonObject resultHandlerResult = resultHandler.result();
            LOGGER.info("Success: Getting all  providers");
            if (requestParams.containsKey(INSTANCE)) {
              Integer providerCount =
                  resultHandlerResult
                      .getJsonArray(RESULTS)
                      .getJsonObject(0)
                      .getInteger("providerCount");
              LOGGER.debug("provider Count {} ", providerCount);
              JsonArray results =
                  resultHandlerResult
                      .getJsonArray(RESULTS)
                      .getJsonObject(0)
                      .getJsonArray("resourceGroupAndProvider");
              int resultSize = results.size();
              // 'allProviders' is a mapping of provider IDs to their corresponding JSON objects
              Map<String, JsonObject> allProviders = new HashMap<>();
              JsonArray providersList = new JsonArray();
              // creating mapping of all provider IDs to their corresponding JSON objects
              for (int i = 0; i < resultSize; i++) {
                JsonObject provider = results.getJsonObject(i);
                String itemType = Util.getItemType(provider);
                if (itemType.equals(VALIDATION_FAILURE_MSG)) {
                  handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
                  return;
                }
                if (ITEM_TYPE_PROVIDER.equals(itemType)) {
                  allProviders.put(
                      provider.getString(ID),
                      new JsonObject()
                          .put(ID, provider.getString(ID))
                          .put(DESCRIPTION_ATTR, provider.getString(DESCRIPTION_ATTR)));
                }
              }
              // filtering out providers which belong to the instance from all providers map.
              for (int i = 0; i < resultSize; i++) {
                JsonObject resourceGroup = results.getJsonObject(i);
                String itemType = Util.getItemType(resourceGroup);
                if (itemType.equals(VALIDATION_FAILURE_MSG)) {
                  handler.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
                  return;
                }
                if (ITEM_TYPE_RESOURCE_GROUP.equals(itemType)
                    && allProviders.containsKey(resourceGroup.getString(PROVIDER))) {
                  providersList.add(allProviders.get(resourceGroup.getString(PROVIDER)));
                  allProviders.remove(resourceGroup.getString(PROVIDER));
                }
              }
              LOGGER.debug("provider belonging to instance are {} ", providersList);
              // Pagination applied to the final response
              int endIndex = requestParams.getInteger(LIMIT) + requestParams.getInteger(OFFSET);
              if (endIndex >= providerCount) {
                if (requestParams.getInteger(OFFSET) >= providerCount) {
                  LOGGER.debug("Offset value has exceeded total hits");
                  JsonObject response =
                      new JsonObject()
                          .put(TYPE, TYPE_SUCCESS)
                          .put(TITLE, SUCCESS)
                          .put(TOTAL_HITS, providerCount);
                  handler.handle(Future.succeededFuture(response));
                } else {
                  endIndex = providerCount;
                }
              }
              JsonArray pagedProviders = new JsonArray();
              for (int i = requestParams.getInteger(OFFSET); i < endIndex; i++) {
                pagedProviders.add(providersList.getJsonObject(i));
              }
              JsonObject response =
                  new JsonObject()
                      .put(TYPE, TYPE_SUCCESS)
                      .put(TITLE, SUCCESS)
                      .put(TOTAL_HITS, providerCount)
                      .put(RESULTS, pagedProviders);
              handler.handle(Future.succeededFuture(response));
            } else {
              handler.handle(Future.succeededFuture(resultHandler.result()));
            }
          } else {
            LOGGER.error("Fail: Getting all providers failed");
            handler.handle(Future.failedFuture(resultHandler.cause()));
          }
        });
    return this;
  }

  @Override
  public MlayerService getMlayerGeoQuery(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    LOGGER.debug("request body" + request);
    String instance = request.getString(INSTANCE);
    JsonArray id = request.getJsonArray("id");
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < id.size(); i++) {
      String datasetId = id.getString(i);
      String combinedQuery =
          GET_MLAYER_BOOL_GEOQUERY.replace("$2", instance).replace("$3", datasetId);
      sb.append(combinedQuery).append(",");
    }
    sb.deleteCharAt(sb.length() - 1);
    String query = GET_MLAYER_GEOQUERY.replace("$1", sb);
    databaseService.getMlayerGeoQuery(
        query,
        postMlayerGeoQueryHandler -> {
          if (postMlayerGeoQueryHandler.succeeded()) {
            LOGGER.info("Success: Getting locations of datasets");
            handler.handle(Future.succeededFuture(postMlayerGeoQueryHandler.result()));
          } else {
            LOGGER.error("Fail: Getting locations of datasets failed");
            handler.handle(Future.failedFuture(postMlayerGeoQueryHandler.cause()));
          }
        });
    return this;
  }

  @Override
  public MlayerService getMlayerAllDatasets(
      JsonObject requestParam, Handler<AsyncResult<JsonObject>> handler) {
    String query = GET_MLAYER_ALL_DATASETS;
    LOGGER.debug("database get mlayer all datasets called");
    databaseService.getMlayerAllDatasets(
        requestParam,
        query,
        getMlayerAllDatasets -> {
          if (getMlayerAllDatasets.succeeded()) {
            processMlayerAllDatasets(getMlayerAllDatasets.result(), requestParam, handler);
          } else {
            LOGGER.error("Fail: Getting all datasets failed");
            handler.handle(Future.failedFuture(getMlayerAllDatasets.cause()));
          }
        });
    return this;
  }

  private void processMlayerAllDatasets(
      JsonObject result, JsonObject requestParam, Handler<AsyncResult<JsonObject>> handler) {

    JsonObject resourceGroupResult = result.getJsonObject("resourceGroupList");
    JsonObject instancesList = result.getJsonObject("instanceResult");
    final JsonObject resourceAndPolicyCnt = result.getJsonObject("resourceAndPolicyCount");
    final JsonObject domains = result.getJsonObject("idAndDomainList");

    LOGGER.debug("Getting all the resource group items");
    Promise<JsonObject> datasetResult = Promise.promise();
    Promise<JsonObject> instanceResult = Promise.promise();
    Promise<JsonObject> resourceCount = Promise.promise();

    gettingAllDatasets(resourceGroupResult, datasetResult);
    allMlayerInstance(instancesList, instanceResult);
    gettingResourceAccessPolicyCount(resourceAndPolicyCnt, resourceCount);

    CompositeFuture.all(instanceResult.future(), datasetResult.future(), resourceCount.future())
        .onComplete(
            ar -> {
              if (ar.succeeded()) {
                JsonObject instanceList = ar.result().resultAt(0);
                JsonObject resourceGroupList = ar.result().resultAt(1);
                JsonObject resourceAndPolicyCount = ar.result().resultAt(2);
                JsonArray resourceGroupArray = new JsonArray();
                LOGGER.debug("getMlayerDatasets resourceGroupList iteration started");
                for (int i = 0; i < resourceGroupList.getInteger("resourceGroupCount"); i++) {
                  JsonObject record =
                      resourceGroupList.getJsonArray("resourceGroup").getJsonObject(i);
                  record.put(
                      "icon",
                      record.containsKey(INSTANCE)
                          ? instanceList.getString(record.getString(INSTANCE))
                          : "");
                  record.put(
                      "totalResources",
                      resourceAndPolicyCount
                              .getJsonObject("resourceItemCount")
                              .containsKey(record.getString(ID))
                          ? resourceAndPolicyCount
                              .getJsonObject("resourceItemCount")
                              .getInteger(record.getString(ID))
                          : 0);
                  if (resourceAndPolicyCount
                      .getJsonObject("resourceAccessPolicy")
                      .containsKey(record.getString(ID))) {
                    record.put(
                        ACCESS_POLICY,
                        resourceAndPolicyCount
                            .getJsonObject("resourceAccessPolicy")
                            .getJsonObject(record.getString(ID)));
                  } else {
                    record.put(
                        ACCESS_POLICY,
                        new JsonObject().put("PII", 0).put("SECURE", 0).put("OPEN", 0));
                  }
                  if (domains.getString(record.getString("id")) != null) {
                    record.put("domain", domains.getString(record.getString("id")));
                  }
                  record.remove(TYPE);
                  resourceGroupArray.add(record);
                }
                JsonArray pagedResourceGroups = new JsonArray();
                int endIndex = requestParam.getInteger(LIMIT) + requestParam.getInteger(OFFSET);
                if (endIndex >= resourceGroupArray.size()) {
                  if (requestParam.getInteger(OFFSET) >= resourceGroupArray.size()) {
                    LOGGER.debug("Offset value has exceeded total hits");
                    RespBuilder respBuilder =
                        new RespBuilder()
                            .withType(TYPE_SUCCESS)
                            .withTitle(SUCCESS)
                            .withTotalHits(resourceGroupList.getInteger("resourceGroupCount"));
                    handler.handle(Future.succeededFuture(respBuilder.getJsonResponse()));
                  } else {
                    endIndex = resourceGroupArray.size();
                  }
                }
                for (int i = requestParam.getInteger(OFFSET); i < endIndex; i++) {
                  pagedResourceGroups.add(resourceGroupArray.getJsonObject(i));
                }

                LOGGER.debug("getMlayerDatasets resourceGroupList interation succeeded");
                RespBuilder respBuilder =
                    new RespBuilder()
                        .withType(TYPE_SUCCESS)
                        .withTitle(SUCCESS)
                        .withTotalHits(resourceGroupList.getInteger("resourceGroupCount"))
                        .withResult(pagedResourceGroups);
                LOGGER.debug("getMlayerDatasets succeeded");
                handler.handle(Future.succeededFuture(respBuilder.getJsonResponse()));

              } else {
                LOGGER.error("Fail: failed DB request");
                handler.handle(Future.failedFuture(internalErrorResp));
              }
            });
  }

  private void gettingAllDatasets(JsonObject datasets, Promise<JsonObject> datasetResult) {
    // Getting all resourceGroup along with provider description resource server url and cosUrl
    LOGGER.debug("getRGs started");
    int size = datasets.getJsonArray(RESULTS).size();
    if (size == 0) {
      LOGGER.debug("getRGs is zero");
      datasetResult.handle(Future.failedFuture(NO_CONTENT_AVAILABLE));
      return;
    }
    JsonObject rsUrl = new JsonObject();
    JsonObject providerDescription = new JsonObject();
    JsonObject cosUrl = new JsonObject();
    LOGGER.debug("getRGs for each provider type result started");
    for (int i = 0; i < size; i++) {
      JsonObject record = datasets.getJsonArray(RESULTS).getJsonObject(i);
      String itemType = Util.getItemType(record);
      if (itemType.equals(VALIDATION_FAILURE_MSG)) {
        datasetResult.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
      }
      if (itemType.equals(ITEM_TYPE_PROVIDER)) {
        providerDescription.put(record.getString(ID), record.getString(DESCRIPTION_ATTR));
        rsUrl.put(
            record.getString(ID),
            record.containsKey("resourceServerRegURL")
                ? record.getString("resourceServerRegURL")
                : "");
      } else if (itemType.equals(ITEM_TYPE_COS)) {
        cosUrl.put(record.getString(ID), record.getString("cosURL"));
      }
    }
    LOGGER.debug("getRGs for each provider type result succeeded");
    int resourceGroupHits = 0;
    JsonArray resourceGroup = new JsonArray();
    LOGGER.debug("getRGs for each resource group result started");
    for (int i = 0; i < size; i++) {
      JsonObject record = datasets.getJsonArray(RESULTS).getJsonObject(i);
      String itemType = Util.getItemType(record);
      if (itemType.equals(ITEM_TYPE_RESOURCE_GROUP)) {
        resourceGroupHits++;
        record.put(
            "providerDescription", providerDescription.getString(record.getString(PROVIDER)));
        record.put("resourceServerRegURL", rsUrl.getString(record.getString(PROVIDER)));
        record.put(
            "cosURL", record.containsKey("cos") ? cosUrl.getString(record.getString("cos")) : "");

        record.remove("cos");
        resourceGroup.add(record);
      }
    }
    LOGGER.debug("getRGs for each resource group result succeeded");
    JsonObject resourceGroupResult =
        new JsonObject()
            .put("resourceGroupCount", resourceGroupHits)
            .put("resourceGroup", resourceGroup);
    LOGGER.debug("getRGs succeeded");
    datasetResult.complete(resourceGroupResult);
  }

  private void allMlayerInstance(JsonObject instances, Promise<JsonObject> instanceResult) {
    // Getting all instance names and icons
    LOGGER.debug("getInstance started");
    int instanceSize = instances.getJsonArray(RESULTS).size();
    JsonObject instanceIcon = new JsonObject();
    LOGGER.debug("getInstance for each instance started");
    for (int i = 0; i < instanceSize; i++) {
      JsonObject instanceObject = instances.getJsonArray(RESULTS).getJsonObject(i);
      instanceIcon.put(
          instanceObject.getString("name").toLowerCase(), instanceObject.getString("icon"));
    }
    LOGGER.debug("getInstance succeeded");
    instanceResult.complete(instanceIcon);
  }

  private void gettingResourceAccessPolicyCount(
      JsonObject resourceCountRes, Promise<JsonObject> resourceCountResult) {
    // Getting resource item count
    LOGGER.debug("resourceAP started");
    JsonObject resourceItemCount = new JsonObject();
    JsonObject resourceAccessPolicy = new JsonObject();
    JsonArray resultsArray = resourceCountRes.getJsonArray(RESULTS);
    LOGGER.debug("resourceAP for each resultsArray started");
    resultsArray.forEach(
        record -> {
          JsonObject recordObject = (JsonObject) record;
          String resourceGrp = recordObject.getString(KEY);
          int docCount = recordObject.getInteger("doc_count");
          resourceItemCount.put(resourceGrp, docCount);
          Map<String, Integer> accessPolicy = new HashMap<>();
          accessPolicy.put("PII", 0);
          accessPolicy.put("SECURE", 0);
          accessPolicy.put("OPEN", 0);

          JsonArray accessPoliciesArray =
              recordObject.getJsonObject("access_policies").getJsonArray("buckets");

          accessPoliciesArray.forEach(
              accessPolicyRecord -> {
                JsonObject accessPolicyRecordObject = (JsonObject) accessPolicyRecord;
                String accessPolicyKey = accessPolicyRecordObject.getString(KEY);
                int accessPolicyDocCount = accessPolicyRecordObject.getInteger("doc_count");
                accessPolicy.put(accessPolicyKey, accessPolicyDocCount);
              });
          resourceAccessPolicy.put(resourceGrp, accessPolicy);
        });

    LOGGER.debug("resourceAP for each resultsArray succeeded");

    JsonObject results =
        new JsonObject()
            .put("resourceItemCount", resourceItemCount)
            .put("resourceAccessPolicy", resourceAccessPolicy);
    LOGGER.debug("resourceAP Succeeded : {}", results.containsKey("resourceItemCount"));
    resourceCountResult.complete(results);
  }

  @Override
  public MlayerService getMlayerDataset(
      JsonObject requestData, Handler<AsyncResult<JsonObject>> handler) {
    if (requestData.containsKey(ID) && !requestData.getString(ID).isBlank()) {
      databaseService.getMlayerDataset(
          requestData,
          getMlayerDatasetHandler -> {
            if (getMlayerDatasetHandler.succeeded()) {
              LOGGER.info("Success: Getting details of dataset");
              handler.handle(Future.succeededFuture(getMlayerDatasetHandler.result()));
            } else {
              LOGGER.error("Fail: Getting details of dataset");
              handler.handle(Future.failedFuture(getMlayerDatasetHandler.cause()));
            }
          });
    } else if ((requestData.containsKey("tags")
            || requestData.containsKey("instance")
            || requestData.containsKey("providers")
            || requestData.containsKey("domains"))
        && (!requestData.containsKey(ID) || requestData.getString(ID).isBlank())) {
      if (requestData.containsKey("domains") && !requestData.getJsonArray("domains").isEmpty()) {
        JsonArray domainsArray = requestData.getJsonArray("domains");
        JsonArray tagsArray =
            requestData.containsKey("tags") ? requestData.getJsonArray("tags") : new JsonArray();

        tagsArray.addAll(domainsArray);
        requestData.put("tags", tagsArray);
      }
      String query = GET_ALL_DATASETS_BY_FIELDS;

      if (requestData.containsKey(TAGS) && !requestData.getJsonArray(TAGS).isEmpty()) {
        JsonArray tagsArray = requestData.getJsonArray(TAGS);
        String tagQueryString = "";

        for (Object tagValue : tagsArray) {
          if (tagValue instanceof String) {
            tagQueryString = tagQueryString.concat(tagValue + " OR ");
          }
        }

        if (!tagQueryString.isEmpty()) {
          tagQueryString = "(" + tagQueryString.substring(0, tagQueryString.length() - 4) + ")";
          query +=
              ",{\"query_string\":{\"default_field\":\"tags\",\"query\":\""
                  + tagQueryString
                  + "\"}}";
        }
      }
      if (requestData.containsKey(INSTANCE) && !requestData.getString(INSTANCE).isBlank()) {
        query =
            query.concat(
                ",{\"match\":{\"instance.keyword\":\"$1\"}}"
                    .replace("$1", requestData.getString(INSTANCE).toLowerCase()));
      }
      if (requestData.containsKey(PROVIDERS) && !requestData.getJsonArray(PROVIDERS).isEmpty()) {
        query =
            query.concat(
                ",{\"terms\":{\"provider.keyword\":$1}}"
                    .replace("$1", requestData.getJsonArray(PROVIDERS).toString()));
      }
      query = query.concat(GET_ALL_DATASETS_BY_FIELD_SOURCE);
      LOGGER.debug("databse get mlayer all datasets called");
      databaseService.getMlayerAllDatasets(
          requestData,
          query,
          getMlayerAllDatasets -> {
            if (getMlayerAllDatasets.succeeded()) {
              processMlayerAllDatasets(getMlayerAllDatasets.result(), requestData, handler);
            } else {
              LOGGER.error("Fail: Getting all datasets failed");
              handler.handle(Future.failedFuture(getMlayerAllDatasets.cause()));
            }
          });
    } else {
      LOGGER.error("Invalid field present in request body");
      handler.handle(
          Future.failedFuture(
              new RespBuilder()
                  .withType(TYPE_INVALID_PROPERTY_VALUE)
                  .withTitle(TITLE_INVALID_QUERY_PARAM_VALUE)
                  .withDetail("The schema is Invalid")
                  .getResponse()));
    }

    return this;
  }

  @Override
  public MlayerService getMlayerPopularDatasets(
      String instance, Handler<AsyncResult<JsonObject>> handler) {
    String query = GET_HIGH_COUNT_DATASET.replace("$1", databaseTable);
    LOGGER.debug("postgres query" + query);
    postgresService.executeQuery(
        query,
        dbHandler -> {
          if (dbHandler.succeeded()) {
            JsonArray popularDataset = dbHandler.result().getJsonArray("results");
            LOGGER.debug("popular datasets are {}", popularDataset);
            databaseService.getMlayerPopularDatasets(
                instance,
                popularDataset,
                getPopularDatasetsHandler -> {
                  if (getPopularDatasetsHandler.succeeded()) {
                    Promise<JsonObject> instanceResult = Promise.promise();
                    Promise<JsonObject> datasetResult = Promise.promise();
                    JsonObject results = getPopularDatasetsHandler.result();
                    searchSortedMlayerInstances(
                        results.getJsonObject("instanceList"), instanceResult);
                    datasets(
                        instance,
                        results.getJsonObject("datasetJson"),
                        datasetResult,
                        popularDataset);
                    CompositeFuture.all(instanceResult.future(), datasetResult.future())
                        .onComplete(
                            ar -> {
                              if (ar.succeeded()) {
                                JsonObject instanceList = ar.result().resultAt(0);
                                JsonObject datasetJson = ar.result().resultAt(1);
                                for (int i = 0;
                                    i < datasetJson.getJsonArray("latestDataset").size();
                                    i++) {
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
                                    datasetJson
                                        .getJsonArray("latestDataset")
                                        .getJsonObject(i)
                                        .put("icon", "");
                                  }
                                }
                                for (int i = 0;
                                    i < datasetJson.getJsonArray("featuredDataset").size();
                                    i++) {
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
                                    datasetJson
                                        .getJsonArray("featuredDataset")
                                        .getJsonObject(i)
                                        .put("icon", "");
                                  }
                                }
                                JsonArray domainList = results.getJsonArray("domainList");
                                JsonObject result = new JsonObject();
                                result.mergeIn(datasetJson.getJsonObject("typeCount"));
                                result
                                    .put("totalInstance", instanceList.getInteger("totalInstance"))
                                    .put("totalDomain", domainList.size())
                                    .put("domains", domainList)
                                    .put(INSTANCE, instanceList.getJsonArray("instanceList"))
                                    .put(
                                        "featuredDataset",
                                        datasetJson.getJsonArray("featuredDataset"))
                                    .put(
                                        "latestDataset", datasetJson.getJsonArray("latestDataset"));

                                RespBuilder respBuilder =
                                    new RespBuilder()
                                        .withType(TYPE_SUCCESS)
                                        .withTitle(SUCCESS)
                                        .withResult(result);
                                handler.handle(
                                    Future.succeededFuture(respBuilder.getJsonResponse()));
                              } else {
                                LOGGER.error("Fail: failed DB request");
                                handler.handle(Future.failedFuture(internalErrorResp));
                              }
                            });
                  } else {
                    LOGGER.error("Fail: Getting data for the landing page.");
                    handler.handle(Future.failedFuture(getPopularDatasetsHandler.cause()));
                  }
                });

          } else {
            LOGGER.debug("postgres query failed");
            handler.handle(Future.failedFuture(dbHandler.cause()));
          }
        });

    return this;
  }

  private void searchSortedMlayerInstances(JsonObject result, Promise<JsonObject> instanceResult) {
    int totalInstance = result.getInteger(TOTAL_HITS);
    Map<String, String> instanceIconPath = new HashMap<>();
    JsonArray instanceList = new JsonArray();
    for (int i = 0; i < result.getJsonArray(RESULTS).size(); i++) {
      JsonObject instance = result.getJsonArray(RESULTS).getJsonObject(i);
      instanceIconPath.put(instance.getString("name").toLowerCase(), instance.getString("icon"));
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
  }

  private void datasets(
      String instance,
      JsonObject resourceCountRes,
      Promise<JsonObject> datasetResult,
      JsonArray frequentlyUsedResourceGroup) {
    Promise<JsonObject> resourceCount = Promise.promise();
    gettingResourceAccessPolicyCount(resourceCountRes, resourceCount);
    JsonObject resourceItemCount =
        resourceCount.future().result().getJsonObject("resourceItemCount");
    JsonObject resourceAccessPolicy =
        resourceCount.future().result().getJsonObject("resourceAccessPolicy");
    int totalResourceItem = 0;

    ArrayList<JsonObject> latestDatasetArray = new ArrayList<JsonObject>();
    Map<String, JsonObject> resourceGroupMap = new HashMap<>();
    Map<String, String> providerDescription = new HashMap<>();

    for (int i = 0; i < resourceCountRes.getInteger("resultSize"); i++) {
      JsonObject record = resourceCountRes.getJsonArray("cat_results").getJsonObject(i);
      String itemType = Util.getItemType(record);
      if (itemType.equals(VALIDATION_FAILURE_MSG)) {
        datasetResult.handle(Future.failedFuture(VALIDATION_FAILURE_MSG));
      }
      // making a map of all resource group and provider id and its description
      if (ITEM_TYPE_RESOURCE_GROUP.equals(itemType)) {
        String id = record.getString(ID);
        int resourceItemCountInGroup =
            resourceItemCount.containsKey(id) ? resourceItemCount.getInteger(id) : 0;
        record.put("totalResources", resourceItemCountInGroup);
        if (resourceAccessPolicy.containsKey(id)) {
          record.put("accessPolicy", resourceAccessPolicy.getJsonObject(id));
        } else {
          record.put(
              "accessPolicy", new JsonObject().put("PII", 0).put("SECURE", 0).put("OPEN", 0));
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
      typeCount.put("totalPublishers", resourceCountRes.getInteger("totalPublishers"));
    }

    // making an arrayList of top six latest resource group
    ArrayList<JsonObject> latestResourceGroup = new ArrayList<>();
    int resourceGroupSize = Math.min(latestDatasetArray.size(), 6);
    for (int i = 0; i < resourceGroupSize; i++) {
      JsonObject resourceGroup = latestDatasetArray.get(i);
      resourceGroup.put(
          "providerDescription",
          providerDescription.get(latestDatasetArray.get(i).getString(PROVIDER)));

      latestResourceGroup.add(resourceGroup);
    }
    // making array list of most accessed resource groups
    ArrayList<JsonObject> featuredResourceGroup = new ArrayList<>();
    for (int resourceIndex = 0;
        resourceIndex < frequentlyUsedResourceGroup.size();
        resourceIndex++) {
      String id =
          frequentlyUsedResourceGroup.getJsonObject(resourceIndex).getString("resource_group");
      if (resourceGroupMap.containsKey(id)) {
        JsonObject resourceGroup = resourceGroupMap.get(id);
        resourceGroup.put(
            "providerDescription", providerDescription.get(resourceGroup.getString(PROVIDER)));
        featuredResourceGroup.add(resourceGroup);
        // removing the resourceGroup from resourceGroupMap after
        // resources added to featuredResourceGroup array
        resourceGroupMap.remove(id);
      }
    }

    // Determining the number of resource group that can be added if
    // total featured datasets are not 6. Max value is 6.
    int remainingResources = Math.min(6 - featuredResourceGroup.size(), resourceGroupMap.size());

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
          "providerDescription", providerDescription.get(resourceGroup.getString("provider")));

      featuredResourceGroup.add(resourceGroup);
      remainingResources--;
    }
    JsonObject datasetJson =
        new JsonObject()
            .put("latestDataset", latestResourceGroup)
            .put("typeCount", typeCount)
            .put("featuredDataset", featuredResourceGroup);
    datasetResult.complete(datasetJson);
  }

  @Override
  public MlayerService getSummaryCountSizeApi(Handler<AsyncResult<JsonObject>> handler) {
    LOGGER.info(" into get summary count api");
    String query = queryBuilder.buildSummaryCountSizeQuery(catSummaryTable);

    LOGGER.debug(" Query: {} ", query);
    postgresService.executeQuery(
        query,
        allQueryHandler -> {
          if (allQueryHandler.succeeded()) {
            handler.handle(Future.succeededFuture(allQueryHandler.result()));
          } else {
            handler.handle(Future.failedFuture(allQueryHandler.cause()));
          }
        });
    return this;
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

  @Override
  public MlayerService getRealTimeDataSetApi(Handler<AsyncResult<JsonObject>> handler) {
    LOGGER.info(" into get real time dataset api");
    String query = queryBuilder.buildCountAndSizeQuery(databaseTable, excludedIdsJson);
    LOGGER.debug("Query =  {}", query);

    postgresService.executeQuery(
        query,
        dbHandler -> {
          if (dbHandler.succeeded()) {
            JsonObject results = dbHandler.result();
            handler.handle(Future.succeededFuture(results));
          } else {
            LOGGER.debug("postgres query failed");
            handler.handle(Future.failedFuture(dbHandler.cause()));
          }
        });
    return this;
  }
}
