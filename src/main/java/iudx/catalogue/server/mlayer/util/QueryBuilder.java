package iudx.catalogue.server.mlayer.util;

import static iudx.catalogue.server.mlayer.util.Constants.*;

import io.vertx.core.json.JsonArray;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class QueryBuilder {
  private static final Logger LOGGER = LogManager.getLogger(QueryBuilder.class);
  String query;

  public String buildSummaryCountSizeQuery(String catSummaryTable) {
    query = "select * from $a".replace("$a", catSummaryTable);

    return query;
  }

  public String buildCountAndSizeQuery(String databaseTable, JsonArray exlcudedIdsJson) {
    String current = ZonedDateTime.now().toString();
    LOGGER.debug("zone IST =" + ZonedDateTime.now());
    ZonedDateTime zonedDateTimeUtc = ZonedDateTime.parse(current);
    zonedDateTimeUtc = zonedDateTimeUtc.withZoneSameInstant(ZoneId.of("UTC"));
    LOGGER.debug("zonedDateTimeUTC UTC = " + zonedDateTimeUtc);
    LocalDateTime utcTime = zonedDateTimeUtc.toLocalDateTime();
    LOGGER.debug("UTCtime =" + utcTime);

    String timeFrom12Am = utcTime.withHour(0).withMinute(0).withSecond(0).withNano(0).toString();
    LOGGER.debug("timeFrom12Am =" + timeFrom12Am);

    StringBuilder ids = new StringBuilder();

    for (int i = 0; i < exlcudedIdsJson.size(); i++) {
      if (i == exlcudedIdsJson.size()) {
        break;
      } else {
        ids.append("'").append(exlcudedIdsJson.getString(i)).append("'");
        if (i != exlcudedIdsJson.size() - 1) {
          ids.append(",");
        }
      }
    }

    if (exlcudedIdsJson.isEmpty()) {
      query =
          COUNT_SIZE_QUERY
              .concat(TIME_QUERY)
              .replace("$a", databaseTable)
              .replace("$1", timeFrom12Am)
              .replace("$2", utcTime.toString());
    } else {
      query =
          COUNT_SIZE_QUERY
              .concat(TIME_QUERY)
              .concat(EXCLUDED_IDS_QUERY)
              .replace("$a", databaseTable)
              .replace("$1", timeFrom12Am)
              .replace("$2", utcTime.toString())
              .replace("$3", ids.toString());
    }

    return query;
  }
}
