package iudx.catalogue.server.auditing.util;

public class Constants {
    public static final String ID = "id";
    /* Errors */
    public static final String SUCCESS = "Success";
    public static final String FAILED = "Failed";
    public static final String DETAIL = "detail";
    public static final String ERROR_TYPE = "type";
    public static final String TITLE = "title";
    public static final String RESULTS = "results";
    public static final String STATUS = "status";

    /* Database */
    public static final String ERROR = "Error";
    public static final String QUERY_KEY = "query";
    public static final String DATA_NOT_FOUND="Required Data not Found";

    /* Auditing Service Constants*/
    public static final String USER_ROLE = "userRole";
    public static final String USER_ID = "userID";
    public static final String IID = "iid";
    public static final String API = "api";
    public static final String METHOD = "httpMethod";
    public static final String IUDX_ID = "iudxID";
    public static final String WRITE_QUERY =
            "INSERT INTO auditingtable1 (id, userRole, userId, iid, api, method, time, iudxID) VALUES ('$1','$2','$3','$4','$5','$6',$7,'$8')";

    public static final String MESSAGE = "message";
}
