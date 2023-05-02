package iudx.catalogue.server.mlayer.util;

public class Constants {
  public static final String METHOD = "method";
  public static final String MLAYER_ID = "id";
  public static final String NAME = "name";
  public static final String INSTANCE_ID = "instanceId";
  public static final String DOMAIN_ID = "domainId";
  public static final String GET_HIGH_COUNT_DATASET =
          "with auditing_rs_view as (select resourceid, count(*) as hits, "
                  + "(select count(*) from regexp_matches(resourceid, '/', 'g')) as "
                  + "idtype from $1 group by resourceid) select left(resourceid,length(resourceid) "
                  + "-strpos(reverse(resourceid),'/')) as rgid, sum(hits) as totalhits from "
                  + "auditing_rs_view where idtype=4 group by rgid order by totalhits desc limit 6";
}
