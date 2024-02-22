package iudx.catalogue.server.database.relationship;

import static iudx.catalogue.server.database.Constants.GET_RSGROUP;
import static iudx.catalogue.server.database.Constants.GET_TYPE_SEARCH;
import static iudx.catalogue.server.database.DatabaseServiceImpl.isInvalidRelForGivenItem;
import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.util.Constants.TITLE_INVALID_SEARCH_ERROR;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.ElasticClient;
import iudx.catalogue.server.database.QueryDecoder;
import iudx.catalogue.server.database.RespBuilder;
import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ListRelationship {
  private static final Logger LOGGER = LogManager.getLogger(ListRelationship.class);
  private final QueryDecoder queryDecoder = new QueryDecoder();
  ElasticClient client;
  String docIndex;

  public ListRelationship(ElasticClient client, String docIndex) {
    this.client = client;
    this.docIndex = docIndex;
  }

  public void listRelationship(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {

    RespBuilder respBuilder = new RespBuilder();

    StringBuilder typeQuery =
        new StringBuilder(GET_TYPE_SEARCH.replace("$1", request.getString(ID)));
    LOGGER.debug("typeQuery: " + typeQuery);

    client.searchAsync(
        typeQuery.toString(),
        docIndex,
        qeryhandler -> {
          if (qeryhandler.succeeded()) {
            if (qeryhandler.result().getInteger(TOTAL_HITS) == 0) {
              handler.handle(
                  Future.failedFuture(
                      respBuilder
                          .withType(TYPE_ITEM_NOT_FOUND)
                          .withTitle(TITLE_ITEM_NOT_FOUND)
                          .withDetail("Item id given is not present")
                          .getResponse()));
              return;
            }
            JsonObject relType = qeryhandler.result().getJsonArray(RESULTS).getJsonObject(0);

            Set<String> type = new HashSet<String>(relType.getJsonArray(TYPE).getList());
            type.retainAll(ITEM_TYPES);
            String itemType = type.toString().replaceAll("\\[", "").replaceAll("\\]", "");
            LOGGER.debug("Info: itemType: " + itemType);
            relType.put("itemType", itemType);

            if (isInvalidRelForGivenItem(request, itemType)) {
              handler.handle(
                  Future.failedFuture(
                      respBuilder
                          .withType(TYPE_INVALID_SEARCH_ERROR)
                          .withTitle(TITLE_INVALID_SEARCH_ERROR)
                          .withDetail(TITLE_INVALID_SEARCH_ERROR)
                          .getResponse()));
              return;
            }

            if ((request.getString(RELATIONSHIP).equalsIgnoreCase(RESOURCE_SVR)
                    || request.getString(RELATIONSHIP).equalsIgnoreCase(ALL))
                && itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_GROUP)) {
              LOGGER.debug(relType);
              handleRsFetchForResourceGroup(request, handler, respBuilder, relType);
            } else if (request.getString(RELATIONSHIP).equalsIgnoreCase(RESOURCE_GRP)
                && itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_SERVER)) {
              handleResourceGroupFetchForRs(request, handler, respBuilder, relType);
            } else {
              request.mergeIn(relType);
              String elasticQuery = queryDecoder.listRelationshipQuery(request);
              LOGGER.debug("Info: Query constructed;" + elasticQuery);
              if (elasticQuery != null) {
                handleClientSearchAsync(handler, respBuilder, elasticQuery);
              } else {
                handler.handle(
                    Future.failedFuture(
                        respBuilder
                            .withType(TYPE_INVALID_SEARCH_ERROR)
                            .withTitle(TITLE_INVALID_SEARCH_ERROR)
                            .withDetail(TITLE_INVALID_SEARCH_ERROR)
                            .getResponse()));
              }
            }
          } else {
            LOGGER.error(qeryhandler.cause().getMessage());
          }
        });
  }

  private void handleClientSearchAsync(
      Handler<AsyncResult<JsonObject>> handler, RespBuilder respBuilder, String elasticQuery) {
    client.searchAsync(
        elasticQuery,
        docIndex,
        searchRes -> {
          if (searchRes.succeeded()) {
            LOGGER.debug("Success: Successful DB request");
            handler.handle(Future.succeededFuture(searchRes.result()));
          } else {
            LOGGER.error("Fail: DB request has failed;" + searchRes.cause());
            /* Handle request error */
            handler.handle(
                Future.failedFuture(
                    respBuilder
                        .withType(TYPE_INTERNAL_SERVER_ERROR)
                        .withDetail(TITLE_INTERNAL_SERVER_ERROR)
                        .getResponse()));
          }
        });
  }

  private void handleResourceGroupFetchForRs(
      JsonObject request,
      Handler<AsyncResult<JsonObject>> handler,
      RespBuilder respBuilder,
      JsonObject relType) {
    StringBuilder typeQuery4RsGroup =
        new StringBuilder(GET_RSGROUP.replace("$1", relType.getString(ID)));
    LOGGER.debug("typeQuery4RsGroup: " + typeQuery4RsGroup);

    client.searchAsync(
        typeQuery4RsGroup.toString(),
        docIndex,
        serverSearch -> {
          if (serverSearch.succeeded()) {
            JsonArray serverResult = serverSearch.result().getJsonArray("results");
            LOGGER.debug("serverResult: " + serverResult);
            request.put("providerIds", serverResult);
            request.mergeIn(relType);
            String elasticQuery = queryDecoder.listRelationshipQuery(request);

            LOGGER.debug("Info: Query constructed;" + elasticQuery);

            handleClientSearchAsync(handler, respBuilder, elasticQuery);
          }
        });
  }

  private void handleRsFetchForResourceGroup(
      JsonObject request,
      Handler<AsyncResult<JsonObject>> handler,
      RespBuilder respBuilder,
      JsonObject relType) {
    StringBuilder typeQuery4Rserver =
        new StringBuilder(GET_TYPE_SEARCH.replace("$1", relType.getString(PROVIDER)));
    LOGGER.debug("typeQuery4Rserver: " + typeQuery4Rserver);

    client.searchAsync(
        typeQuery4Rserver.toString(),
        docIndex,
        serverSearch -> {
          if (serverSearch.succeeded() && serverSearch.result().getInteger(TOTAL_HITS) != 0) {
            JsonObject serverResult =
                serverSearch.result().getJsonArray("results").getJsonObject(0);
            request.mergeIn(serverResult);
            request.mergeIn(relType);
            String elasticQuery = queryDecoder.listRelationshipQuery(request);

            LOGGER.debug("Info: Query constructed;" + elasticQuery);

            if (elasticQuery != null) {
              handleClientSearchAsync(handler, respBuilder, elasticQuery);
            } else {
              handler.handle(
                  Future.failedFuture(
                      respBuilder
                          .withType(TYPE_INVALID_SEARCH_ERROR)
                          .withTitle(TITLE_INVALID_SEARCH_ERROR)
                          .withDetail(TITLE_INVALID_SEARCH_ERROR)
                          .getResponse()));
            }
          } else {
            respBuilder
                .withType(TYPE_ITEM_NOT_FOUND)
                .withTitle(TITLE_ITEM_NOT_FOUND)
                .withDetail("Resource Group for given item not found");
            handler.handle(Future.failedFuture(respBuilder.getResponse()));
          }
        });
  }
}
