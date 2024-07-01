package iudx.catalogue.server.database.mlayer;

import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.database.elastic.query.Queries.*;
import static iudx.catalogue.server.mlayer.util.Constants.DOMAIN_ID;
import static iudx.catalogue.server.mlayer.util.Constants.MLAYER_ID;
import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.util.Constants.DETAIL_INTERNAL_SERVER_ERROR;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.elastic.ElasticClient;
import iudx.catalogue.server.database.RespBuilder;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MlayerDomain {
  private static final Logger LOGGER = LogManager.getLogger(MlayerDomain.class);
  private static String internalErrorResp =
      new RespBuilder()
          .withType(TYPE_INTERNAL_SERVER_ERROR)
          .withTitle(TITLE_INTERNAL_SERVER_ERROR)
          .withDetail(DETAIL_INTERNAL_SERVER_ERROR)
          .getResponse();
  ElasticClient client;
  String mlayerDomainIndex;

  public MlayerDomain(ElasticClient client, String mlayerDomainIndex) {
    this.client = client;
    this.mlayerDomainIndex = mlayerDomainIndex;
  }

  public void createMlayerDomain(JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    RespBuilder respBuilder = new RespBuilder();
    String domainId = request.getString(DOMAIN_ID);
    String id = request.getString(MLAYER_ID);
    Query checkForExistingDomain = buildCheckMdocQuery(id);
    client.searchAsync(
        checkForExistingDomain,
        buildSourceConfig(List.of()),
        FILTER_PAGINATION_SIZE,
        0,
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

  public void getMlayerDomain(JsonObject requestParams, Handler<AsyncResult<JsonObject>> handler) {
    // String query = "";
    Query query;
    SourceConfig sourceConfig;
    // Define the fields to include in the source
    List<String> includes = List.of("domainId", "description", "icon", "label", "name");
    sourceConfig = buildSourceConfig(includes);
    int limit =
        requestParams.getString(LIMIT) == null
            ? 10000
            : Integer.parseInt(requestParams.getString(LIMIT));
    int offset =
        requestParams.getString(OFFSET) == null
            ? 0
            : Integer.parseInt(requestParams.getString(OFFSET));
    if (!requestParams.containsKey(ID)) {
      query = buildAllMlayerDomainsQuery();
    } else {
      query = buildMlayerDomainQuery(requestParams.getString(ID));
    }
    client.searchAsync(
        query,
        sourceConfig,
        limit,
        offset,
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
    Query checkForExistingRecord = buildMdocDomainQuery(domainId);
    List<String> includes = List.of("domainId", "description", "icon", "label", "name");
    SourceConfig sourceConfig = buildSourceConfig(includes);
    client.searchAsyncGetId(
        checkForExistingRecord,
        sourceConfig,
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
                  request,
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

    Query checkForExistingRecord = buildMdocDomainQuery(domainId);
    List<String> includes = List.of("domainId", "description", "icon", "label", "name");
    SourceConfig sourceConfig = buildSourceConfig(includes);

    client.searchGetId(
        checkForExistingRecord,
        sourceConfig,
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
