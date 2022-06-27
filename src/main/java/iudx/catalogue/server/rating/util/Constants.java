package iudx.catalogue.server.rating.util;

public class Constants {

  public static final String USER_ID = "userID";
  public static final String ID = "id";
  public static final String PENDING = "pending";
  public static final String APPROVED = "approved";
  public static final String RATING_ID = "ratingID";
  public static final String AUDIT_INFO_QUERY =
      "SELECT count() from rsauditingtable where userId='$1' and resourceid='$2'";
}
