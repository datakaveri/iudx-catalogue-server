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
  public static final String TIME_QUERY = "where time between '$1' AND '$2'";
  public static final String COUNT_SIZE_QUERY =
      "select count(api) as counts , COALESCE(SUM(size), 0) as size from $a ";

  public static final String EXCLUDED_IDS_QUERY = " and userid NOT IN ($3)";
}
