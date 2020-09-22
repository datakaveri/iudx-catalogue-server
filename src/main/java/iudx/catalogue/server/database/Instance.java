package iudx.catalogue.server.database;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.catalogue.server.Constants.*;
import static iudx.catalogue.server.database.Constants.*;

/**
 * Class to check, validate the instance with the db.
 */
public class Instance {

  ElasticClient client;
  private static final Logger LOGGER = LogManager.getLogger(Instance.class);

  Instance(ElasticClient client) {
    this.client = client;
  }

  /**
   * Validates the instanceId from the request.
   * 
   * @param instanceId
   * @return Promise<Boolean>
   */
  Future<Boolean> verify(String instanceId) {

    Promise<Boolean> promise = Promise.promise();

    if (instanceId == null || instanceId.startsWith("\"") || instanceId.isBlank()) {
      LOGGER.debug("Info: InstanceID null. Maybe provider item");
      promise.complete(true);
      return promise.future();
    }

    String checkInstance = TERM_COMPLEX_QUERY.replace("$1", instanceId).replace("$2", "");
    client.searchAsync(CAT_INDEX_NAME, checkInstance, checkRes -> {
      if (checkRes.failed()) {
        LOGGER.error(ERROR_DB_REQUEST + checkRes.cause().getMessage());
        promise.fail(INTERNAL_SERVER_ERROR);
      } else if (checkRes.result().getInteger(TOTAL_HITS) == 0) {
        LOGGER.debug(INSTANCE_NOT_EXISTS);
        promise.fail("Fail: Instance doesn't exist/registered");
      } else {
        promise.complete(true);
      }
      return;
    });

    return promise.future();
  }
}
