package iudx.catalogue.server.database.mlayer;

import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.database.elastic.query.Queries.*;
import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.validator.Constants.VALIDATION_FAILURE_MSG;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.AggregationBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.elastic.ElasticClient;
import iudx.catalogue.server.database.RespBuilder;
import iudx.catalogue.server.database.Util;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MlayerProvider {
  private static final Logger LOGGER = LogManager.getLogger(MlayerProvider.class);
  private static String internalErrorResp =
      new RespBuilder()
          .withType(TYPE_INTERNAL_SERVER_ERROR)
          .withTitle(TITLE_INTERNAL_SERVER_ERROR)
          .withDetail(DETAIL_INTERNAL_SERVER_ERROR)
          .getResponse();
  ElasticClient client;
  String docIndex;

  public MlayerProvider(ElasticClient client, String docIndex) {
    this.client = client;
    this.docIndex = docIndex;
  }

  public void getMlayerProviders(
      JsonObject requestParams, Handler<AsyncResult<JsonObject>> handler) {
    int limit = Integer.parseInt(requestParams.getString(LIMIT));
    int offset = Integer.parseInt(requestParams.getString(OFFSET));
    // Aggregation for provider_count
    Aggregation providerCountAgg = AggregationBuilders.cardinality()
            .field("provider.keyword")
            .build()._toAggregation();

    List<String> includes =
            List.of(
                    "id",
                    "description",
                    "type",
                    "resourceGroup",
                    "accessPolicy",
                    "provider",
                    "itemCreatedAt",
                    "instance",
                    "label");
    SourceConfig source = buildSourceConfig(includes);
    if (requestParams.containsKey(INSTANCE)) {
      Query query = buildgetDatasetByInstanceQuery(requestParams.getString(INSTANCE));
      client.searchAsyncResourceGroupAndProvider(
          query,
          providerCountAgg,
          source,
          10000,
          docIndex,
          resultHandler -> {
            if (resultHandler.succeeded()) {
              LOGGER.debug("Success: Successful DB Request");
              if (resultHandler.result().getJsonArray(RESULTS).isEmpty()) {
                handler.handle(Future.failedFuture(NO_CONTENT_AVAILABLE));
              }

              // providerCount depicts the number of provider associated with the instance
              Integer providerCount =
                  resultHandler
                      .result()
                      .getJsonArray(RESULTS)
                      .getJsonObject(0)
                      .getInteger("providerCount");
              LOGGER.debug("provider Count {} ", providerCount);
              // results consists of all providers and resource groups belonging to instance
              JsonArray results =
                  resultHandler
                      .result()
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
                }
                if (ITEM_TYPE_RESOURCE_GROUP.equals(itemType)
                    && allProviders.containsKey(resourceGroup.getString(PROVIDER))) {
                  providersList.add(allProviders.get(resourceGroup.getString(PROVIDER)));
                  allProviders.remove(resourceGroup.getString(PROVIDER));
                }
              }
              LOGGER.debug("provider belonging to instance are {} ", providersList);
              // Pagination applied to the final response.
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
              LOGGER.error("Fail: failed DB request");
              handler.handle(Future.failedFuture(internalErrorResp));
            }
          });
    } else {
      List<String> includesList = List.of("id", "description");
      SourceConfig sourceConfig = buildSourceConfig(includesList);
      Query query = buildMlayerProvidersQuery();
      client.searchAsync(
          query,
          sourceConfig,
          limit,
          1,
          docIndex,
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
  }
}
