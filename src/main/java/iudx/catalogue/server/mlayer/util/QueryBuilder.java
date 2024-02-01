package iudx.catalogue.server.mlayer.util;

import static iudx.catalogue.server.mlayer.util.Constants.*;
import static iudx.catalogue.server.mlayer.util.Constants.GROUPBY;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class QueryBuilder {
  private static final Logger LOGGER = LogManager.getLogger(QueryBuilder.class);
  String query;

  public String buildTotalCountQuery(String databaseTable) {

    String current = ZonedDateTime.now().toString();
    LOGGER.debug("zone IST =" + ZonedDateTime.now());
    ZonedDateTime zonedDateTimeUtc = ZonedDateTime.parse(current);
    zonedDateTimeUtc = zonedDateTimeUtc.withZoneSameInstant(ZoneId.of("UTC"));
    LOGGER.debug("zonedDateTimeUTC UTC = " + zonedDateTimeUtc);
    LocalDateTime utcTime = zonedDateTimeUtc.toLocalDateTime();
    LOGGER.debug("UTCtime =" + utcTime);

    String timeYearBack = utcTime.minusYears(1).toString();
    LOGGER.debug("time year back = " + timeYearBack);

    String timeSixMonthBack = utcTime.minusMonths(6).toString();
    LOGGER.debug("time six month back = " + timeSixMonthBack);

    String timeOneMonthBack = utcTime.minusMonths(1).toString();
    LOGGER.debug("time one month back = " + timeOneMonthBack);

    String timeOneWeekBack = utcTime.minusWeeks(1).toString();
    LOGGER.debug("time one week back = " + timeOneWeekBack);

    String timeOneDayBack = utcTime.minusDays(1).toString();
    LOGGER.debug("time one day back = " + timeOneDayBack);

    query =
        "select ( "
            + TOTAL_COUNT_QUERY.replace("$a", databaseTable)
            + " ) as total_count , ( "
            + TOTAL_COUNT_QUERY
                .concat(TIME_QUERY)
                .replace("$a", databaseTable)
                .replace("$1", timeYearBack)
                .replace("$2", utcTime.toString())
            + " ) as year_count , ( "
            + TOTAL_COUNT_QUERY
                .concat(TIME_QUERY)
                .replace("$a", databaseTable)
                .replace("$1", timeSixMonthBack)
                .replace("$2", utcTime.toString())
            + " ) as six_month_count , ( "
            + TOTAL_COUNT_QUERY
                .concat(TIME_QUERY)
                .replace("$a", databaseTable)
                .replace("$1", timeOneMonthBack)
                .replace("$2", utcTime.toString())
            + " ) as one_month_count , ( "
            + TOTAL_COUNT_QUERY
                .concat(TIME_QUERY)
                .replace("$a", databaseTable)
                .replace("$1", timeOneWeekBack)
                .replace("$2", utcTime.toString())
            + " ) as one_week_count , ( "
            + TOTAL_COUNT_QUERY
                .concat(TIME_QUERY)
                .replace("$a", databaseTable)
                .replace("$1", timeOneDayBack)
                .replace("$2", utcTime.toString())
            + " ) as one_day_count ";

    return query;
  }

  public String buildMonthlyHitAndSizeQuery(String databaseTable) {

    String current = ZonedDateTime.now().toString();
    LOGGER.debug("zone IST =" + ZonedDateTime.now());
    ZonedDateTime zonedDateTimeUtc = ZonedDateTime.parse(current);
    zonedDateTimeUtc = zonedDateTimeUtc.withZoneSameInstant(ZoneId.of("UTC"));
    LOGGER.debug("zonedDateTimeUTC UTC = " + zonedDateTimeUtc);
    LocalDateTime utcTime = zonedDateTimeUtc.toLocalDateTime();
    LOGGER.debug("UTCtime =" + utcTime);
    long today = zonedDateTimeUtc.getDayOfMonth();
    String seriesGenerator =
        utcTime
            .minusYears(1)
            .minusDays(today)
            .plusDays(1)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .toString();
    LOGGER.debug("Generator Year back =" + seriesGenerator);

    String timeYearBack = utcTime.minusYears(1).toString();
    LOGGER.debug("time year back = " + timeYearBack);

    query =
        new StringBuilder(
                MONTHLY_HIT_SIZE_QUERY
                    .concat(GROUPBY)
                    .replace("$0", seriesGenerator)
                    .replace("$1", utcTime.toString())
                    .replace("$2", timeYearBack)
                    .replace("$3", utcTime.toString())
                    .replace("$a", databaseTable))
            .toString();
    return query;
  }
}
