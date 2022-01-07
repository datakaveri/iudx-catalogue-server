package iudx.catalogue.server.auditing.util;

public class Constants {
  public static final String ID = "id";
  /* Errors */
  public static final String SUCCESS = "success";
  public static final String FAILED = "failed";
  public static final String DETAIL = "detail";
  public static final String ERROR_TYPE = "type";
  public static final String TITLE = "title";
  public static final String RESULTS = "results";
  public static final String STATUS = "status";
  public static final String MESSAGE = "message";
  public static final String EMPTY_RESPONSE = "Empty response";

  /* Database */
  public static final String ERROR = "Error";
  public static final String QUERY_KEY = "query";
  public static final String DATA_NOT_FOUND="Required Data not Found";
  public static final String USERID_NOT_FOUND = "User ID not found" ;
  public static final String START_TIME = "startTime";
  public static final String END_TIME = "endTime";
  public static final String ENDPOINT = "endPoint";
  public static final String TIME = "time";
  public static final String INVALID_DATE_TIME = "Date-Time not in correct format.";
  public static final String MISSING_START_TIME = "Start-Time not found.";
  public static final String MISSING_END_TIME = "End-Time not found.";
  public static final String INVALID_TIME = "End-Time cannot be before Start-Time.";
  public static final String METHOD_COLUMN_NAME = "(defaultdb.auditingtable1.method)";
  public static final String TIME_COLUMN_NAME = "(defaultdb.auditingtable1.time)";
  public static final String USERID_COLUMN_NAME = "(defaultdb.auditingtable1.userid)";
  public static final String API_COLUMN_NAME = "(defaultdb.auditingtable1.api)";
  public static final String IID_COLUMN_NAME = "(defaultdb.auditingtable1.iid)";
  public static final String IUDX_COLUMN_NAME = "(defaultdb.auditingtable1.iudxid)";
  public static final String USERROLE_COLUMN_NAME = "(defaultdb.auditingtable1.userrole)";

  /* Auditing Service Constants*/
  public static final String USER_ROLE = "userRole";
  public static final String USER_ID = "userID";
  public static final String IID = "iid";
  public static final String API = "api";
  public static final String METHOD = "httpMethod";
  public static final String IUDX_ID = "iudxID";
  public static final String WRITE_QUERY =
          "INSERT INTO auditingtable1 (id, userRole, userId, iid, api, method, time, iudxID) VALUES ('$1','$2','$3','$4','$5','$6',$7,'$8')";
  public static final String READ_QUERY =
          "SELECT userRole, userId, iid, api, method, time, iudxID from auditingtable1 where userId='$1'";
  public static final String START_TIME_QUERY = " and time>=$2";
  public static final String END_TIME_QUERY = " and time<=$3";
  public static final String API_QUERY = " and api='$4'";
  public static final String METHOD_QUERY = " and method='$5'";
}
