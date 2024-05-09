package iudx.catalogue.server.mlayer;

import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.mlayer.util.Constants.*;
import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.util.Constants.NAME;
import static iudx.catalogue.server.util.Constants.PROVIDERS;
import static iudx.catalogue.server.validator.Constants.VALIDATION_FAILURE_MSG;

import com.google.common.hash.Hashing;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.DatabaseService;
import iudx.catalogue.server.database.RespBuilder;
import iudx.catalogue.server.database.Util;
import iudx.catalogue.server.database.postgres.PostgresService;
import iudx.catalogue.server.mlayer.util.QueryBuilder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MlayerServiceImpl implements MlayerService {
  private static final Logger LOGGER = LogManager.getLogger(MlayerServiceImpl.class);
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
              processProviderData(resultHandlerResult, requestParams, handler);
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

  private void processProviderData(
      JsonObject resultHandlerResult,
      JsonObject requestParams,
      Handler<AsyncResult<JsonObject>> handler) {
    Integer providerCount =
        resultHandlerResult.getJsonArray(RESULTS).getJsonObject(0).getInteger("providerCount");
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

    handler.handle(Future.succeededFuture(paginateProviders(requestParams, providersList)));
  }

  // Pagination applied to the final response.
  private JsonObject paginateProviders(JsonObject requestParams, JsonArray providersList) {
    int limit = requestParams.getInteger(LIMIT);
    int offset = requestParams.getInteger(OFFSET);
    int endIndex = limit + offset;
    int providerCount = providersList.size();

    if (endIndex >= providerCount) {
      if (offset >= providerCount) {
        LOGGER.debug("Offset value has exceeded total hits");
        return new JsonObject()
            .put(TYPE, TYPE_SUCCESS)
            .put(TITLE, SUCCESS)
            .put(TOTAL_HITS, providerCount);
      } else {
        endIndex = providerCount;
      }
    }

    JsonArray pagedProviders = new JsonArray();
    for (int i = offset; i < endIndex; i++) {
      pagedProviders.add(providersList.getJsonObject(i));
    }
    return new JsonObject()
        .put(TYPE, TYPE_SUCCESS)
        .put(TITLE, SUCCESS)
        .put(TOTAL_HITS, providerCount)
        .put(RESULTS, pagedProviders);
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
    LOGGER.debug("databse get mlayer all datasets called");
    databaseService.getMlayerAllDatasets(
        requestParam,
        query,
        getMlayerAllDatasets -> {
          if (getMlayerAllDatasets.succeeded()) {
            LOGGER.info("Success: Getting all datasets");
            handler.handle(Future.succeededFuture(getMlayerAllDatasets.result()));
          } else {
            LOGGER.error("Fail: Getting all datasets failed");
            handler.handle(Future.failedFuture(getMlayerAllDatasets.cause()));
          }
        });
    return this;
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
          query += ",{\"query_string\":{\"default_field\":\"tags\",\"query\":\""
                  + tagQueryString + "\"}}";
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
          getAllDatasetsHandler -> {
            if (getAllDatasetsHandler.succeeded()) {
              LOGGER.info("Success: Getting details of all datasets");
              handler.handle(Future.succeededFuture(getAllDatasetsHandler.result()));
            } else {
              LOGGER.error("Fail: Getting details of all datasets");
              handler.handle(Future.failedFuture(getAllDatasetsHandler.cause()));
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
                    LOGGER.info("Success: Getting data for the landing page.");
                    handler.handle(Future.succeededFuture(getPopularDatasetsHandler.result()));
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