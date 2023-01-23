package iudx.catalogue.server.mlayer;

import com.google.common.hash.Hashing;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.DatabaseService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static iudx.catalogue.server.mlayer.util.Constants.*;

public class MlayerServiceImpl implements MlayerService {
  private static final Logger LOGGER = LogManager.getLogger(MlayerServiceImpl.class);
  DatabaseService databaseService;

  MlayerServiceImpl(DatabaseService databaseService) {
    this.databaseService = databaseService;
  }

  @Override
  public MlayerService createMlayerInstance(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String name = request.getString(NAME).toLowerCase();
    String ID = Hashing.sha256().hashString(name, StandardCharsets.UTF_8).toString();
    String InstanceID = UUID.randomUUID().toString();
    request.put(MLAYER_INSTANCE_ID, ID).put(INSTANCE_ID, InstanceID);

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
    String ID = Hashing.sha256().hashString(name, StandardCharsets.UTF_8).toString();
    LOGGER.debug(ID);
    request.put("ID", ID);
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
    request.put(MLAYER_DOMAIN_ID, id).put(DOMAIN_ID, domainId);

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
    request.put("ID", id);
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
            LOGGER.info("Success: Getting all domain values");
            handler.handle(Future.succeededFuture(getMlayerDomainHandler.result()));
          } else {
            LOGGER.error("Fail: Getting all domains failed");
            handler.handle(Future.failedFuture(getMlayerDomainHandler.cause()));
          }
        });
    return this;
  }
}
