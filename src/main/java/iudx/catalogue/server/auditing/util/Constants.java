package iudx.catalogue.server.auditing.util;

public class Constants {
    public static final String ID = "id";
    /* Temporal */
    public static final String START_TIME = "startTime";
    public static final String END_TIME = "endtime";

    /* Errors */
    public static final String SUCCESS = "Success";
    public static final String FAILED = "Failed";
    public static final String EMPTY_RESPONSE = "Empty response";
    public static final String DETAIL = "detail";
    public static final String ERROR_TYPE = "type";
    public static final String TITLE = "title";
    public static final String RESULTS = "results";
    public static final String STATUS = "status";
    public static final String INDEX_NOT_FOUND = "index_not_found_exception";
    public static final String INVALID_RESOURCE_ID = "Invalid resource id";
    public static final String ROOT_CAUSE = "root_cause";
    public static final String REASON = "reason";
    public static final String TIME_NOT_FOUND = "Time interval not found";
    public static final String INVALID_DATE_TIME = "invalid date-time";

    /* Database */
    public static final String ERROR = "Error";
    public static final String QUERY_KEY = "query";
    public static final String TYPE_KEY = "type";
    public static final String FROM_KEY = "from";
    public static final String SIZE_KEY = "size";

    /* Auditing Service Constants*/
    public static final String USER_ROLE = "userRole";
    public static final String EMAIL_ID = "emailID";
    public static final String IID = "iid";
    public static final String API = "api";
    public static final String METHOD = "httpMethod";
    public static final String IUDX_ID = "iudxID";
    public static final String WRITE_QUERY =
            "INSERT INTO auditingtable (id, userRole, emailId, iid, api, method, time, iudxID) VALUES ('$1',$2,'$3','$4','$5','$6','$7','$8')";
    public static final String COLUMN_NAME = "(auditing.auditingtable.col0)";

    public static final String MESSAGE = "message";
}
