package iudx.catalogue.server.mlayer;

import static iudx.catalogue.server.mlayer.util.Constants.*;

import com.google.common.hash.Hashing;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.DatabaseService;
import iudx.catalogue.server.database.postgres.PostgresService;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MlayerServiceImpl implements MlayerService {
  private static final Logger LOGGER = LogManager.getLogger(MlayerServiceImpl.class);
  DatabaseService databaseService;
  PostgresService postgresService;
  private String databaseTable;

  MlayerServiceImpl(
      DatabaseService databaseService, PostgresService postgresService, String databaseTable) {
    this.databaseService = databaseService;
    this.postgresService = postgresService;
    this.databaseTable = databaseTable;
  }

  @Override
  public MlayerService createMlayerInstance(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String name = request.getString(NAME).toLowerCase();
    String id = Hashing.sha256().hashString(name, StandardCharsets.UTF_8).toString();
    String instanceId = UUID.randomUUID().toString();
    request.put(MLAYER_ID, id).put(INSTANCE_ID, instanceId);

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
  public MlayerService getMlayerInstance(Handler<AsyncResult<JsonObject>> handler) {

    databaseService.getMlayerInstance(
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
    request.put(MLAYER_ID, id).put(DOMAIN_ID, domainId);

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
  public MlayerService getMlayerDomain(Handler<AsyncResult<JsonObject>> handler) {
    databaseService.getMlayerDomain(
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
  public MlayerService getMlayerProviders(Handler<AsyncResult<JsonObject>> handler) {
    databaseService.getMlayerProviders(
        getMlayerDomainHandler -> {
          if (getMlayerDomainHandler.succeeded()) {
            LOGGER.info("Success: Getting all  providers");
            handler.handle(Future.succeededFuture(getMlayerDomainHandler.result()));
          } else {
            LOGGER.error("Fail: Getting all providers failed");
            handler.handle(Future.failedFuture(getMlayerDomainHandler.cause()));
          }
        });
    return this;
  }

  @Override
  public MlayerService getMlayerGeoQuery(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    databaseService.getMlayerGeoQuery(
        request,
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
  public MlayerService getMlayerAllDatasets(Handler<AsyncResult<JsonObject>> handler) {
    databaseService.getMlayerAllDatasets(
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
      String datasetId, Handler<AsyncResult<JsonObject>> handler) {
    databaseService.getMlayerDataset(
        datasetId,
        getMlayerDatasetHandler -> {
          if (getMlayerDatasetHandler.succeeded()) {
            LOGGER.info("Success: Getting details of dataset");
            handler.handle(Future.succeededFuture(getMlayerDatasetHandler.result()));
          } else {
            LOGGER.error("Fail: Getting details of dataset");
            handler.handle(Future.failedFuture(getMlayerDatasetHandler.cause()));
          }
        });
    return this;
  }

  @Override
  public MlayerService getMlayerPopularDatasets(Handler<AsyncResult<JsonObject>> handler) {

    String query = GET_HIGH_COUNT_DATASET.replace("$1", databaseTable);
    LOGGER.debug("postgres query" + query);
    postgresService.executeQuery(
        query,
        dbHandler -> {
          if (dbHandler.succeeded()) {
            JsonArray popularDataset = dbHandler.result().getJsonArray("results");
            databaseService.getMlayerPopularDatasets(
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
}
