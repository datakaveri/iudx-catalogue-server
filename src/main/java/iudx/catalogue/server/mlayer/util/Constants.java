package iudx.catalogue.server.mlayer.util;

public class Constants {
  public static final String METHOD = "method";
  public static final String MLAYER_ID = "id";
  public static final String NAME = "name";
  public static final String INSTANCE_ID = "instanceId";
  public static final String DOMAIN_ID = "domainId";
  public static final String GET_HIGH_COUNT_DATASET =
      "select resource_group, count(id) as totalhits from $1 "
          + "group by resource_group order by totalhits "
          + "desc limit 6";

  public static final String MONTHLY_HIT_SIZE_QUERY =
      "SELECT month,year,COALESCE(counts, 0) as counts , COALESCE(total_size,0) as total_size\n"
          + "FROM  (\n"
          + "   SELECT day::date ,to_char(date_trunc('month', day),'FMmonth') as month"
          + ",extract('year' from day) as year\n"
          + "   FROM   generate_series(timestamp '$0'\n"
          + "                        , timestamp '$1'\n"
          + "                        , interval  '1 month') day\n"
          + "   ) d\n"
          + "LEFT  JOIN (\n"
          + "   SELECT date_trunc('month', time)::date AS day\n"
          + "        , count(api) as counts , SUM(size) as total_size\n"
          + "   FROM   $a\n"
          + "   WHERE  time between '$2'\n"
          + "   AND '$3'\n";
  public static final String GROUPBY =
      "\n" + "   GROUP  BY 1\n" + "   ) t USING (day)\n" + "ORDER  BY day";

  public static final String TOTAL_HIT_AND_SIZE_QUERY =
      "SELECT count(api) as counts,SUM(size) AS total_size\n"
          + "FROM $a --where isotime like '%2024-01-26%';";
}
