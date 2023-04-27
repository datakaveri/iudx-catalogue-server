package iudx.catalogue.server.auditing.util;

import static iudx.catalogue.server.auditing.util.Constants.*;

import io.vertx.core.json.JsonObject;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class QueryBuilder {

  private static final Logger LOGGER = LogManager.getLogger(QueryBuilder.class);

  public JsonObject buildMessageForRmq(JsonObject request) {
    String primaryKey = UUID.randomUUID().toString().replace("-", "");
    request.put(PRIMARY_KEY, primaryKey).put(ORIGIN, ORIGIN_SERVER);

    LOGGER.debug("request " + request);
    return request;
  }
}
