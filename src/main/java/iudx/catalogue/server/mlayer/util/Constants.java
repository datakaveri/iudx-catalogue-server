package iudx.catalogue.server.mlayer.util;

public class Constants {
  public static final String METHOD = "method";
  public static final String MLAYER_ID = "id";
  public static final String NAME = "name";
  public static final String INSTANCE_ID = "instanceId";
  public static final String DOMAIN_ID = "domainId";
  public static final String GET_HIGH_COUNT_DATASET =
          "select resourcegroup, count(id) as totalhits from $1 "
                  + "group by resourcegroup order by totalhits "
                  + "desc limit 6";
}
