package iudx.catalogue.server.mlayer;

import com.google.common.hash.Hashing;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.DatabaseService;
import iudx.catalogue.server.rating.RatingServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;

import static iudx.catalogue.server.mlayer.util.Constants.MLAYER_INSTANCE_ID;

public class MlayerServiceImpl implements MlayerService {
  private static final Logger LOGGER = LogManager.getLogger(MlayerServiceImpl.class);
  DatabaseService databaseService;

  MlayerServiceImpl(DatabaseService databaseService) {
    this.databaseService = databaseService;
  }

  @Override
  public MlayerService createMlayerInstance(
      JsonObject request, Handler<AsyncResult<JsonObject>> handler) {
    String nameValue = request.getString("name");
    String name = nameValue.toLowerCase();
    String mlayerInstanceID = Hashing.sha256().hashString(name, StandardCharsets.UTF_8).toString();
    request.put(MLAYER_INSTANCE_ID, mlayerInstanceID);
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
  public MlayerService getMlayerInstance( Handler<AsyncResult<JsonObject>> handler) {

    databaseService.getMlayerInstance(getMlayerInstancehandler->{
      if(getMlayerInstancehandler.succeeded()){
        handler.handle(Future.succeededFuture(getMlayerInstancehandler.result()));
      }
      else{
        handler.handle(Future.failedFuture(getMlayerInstancehandler.cause()));
      }
    });
    return this;
  }
}
