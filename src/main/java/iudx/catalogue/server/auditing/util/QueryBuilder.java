package iudx.catalogue.server.auditing.util;

import static iudx.catalogue.server.auditing.util.Constants.*;

import io.vertx.core.json.JsonObject;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class QueryBuilder {

    private static final Logger LOGGER = LogManager.getLogger(QueryBuilder.class);

    public JsonObject buildWriteQuery(JsonObject request) {
        String primaryKey = UUID.randomUUID().toString().replace("-","");
        String userRole = request.getString(USER_ROLE);
        String emailId = request.getString(EMAIL_ID);
        String iid = request.getString(IID);
        String api = request.getString(API);
        String method = request.getString(METHOD);
        String iudxID = request.getString(IUDX_ID);
        ZonedDateTime zst = ZonedDateTime.now();
        LOGGER.info("TIME ZST: " + zst);
        long time = getEpochTime(zst);

        StringBuilder query =
                new StringBuilder(
                        WRITE_QUERY
                                .replace("$1", primaryKey)
                                .replace("$2", userRole)
                                .replace("$3", emailId)
                                .replace("$4", iid)
                                .replace("$5", api)
                                .replace("$6", method)
                                .replace("$7", Long.toString(time))
                                .replace("$8", iudxID));

        LOGGER.info("Info: Query " + query);
        return new JsonObject().put(QUERY_KEY, query);
    }

    private long getEpochTime(ZonedDateTime zst) {
        return zst.toInstant().toEpochMilli();
    }
}
