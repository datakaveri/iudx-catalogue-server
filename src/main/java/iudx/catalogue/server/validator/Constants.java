package iudx.catalogue.server.validator;

public class Constants {

  /** General definations. */
  public static final String ID_KEYWORD = "id.keyword";
  public static final String VALUE = "value";
  public static final String CAT_DOC = "cat/_doc";

  public static final String ITEM_STATUS = "itemStatus";
  public static final String ACTIVE = "ACTIVE";
  public static final String ITEM_CREATED_AT = "itemCreatedAt";

  static final String FILTER_PATH = "?filter_path=took,hits.total.value,hits.hits._source";

  /** Error messages. */
  public static final String NON_EXISTING_LINK_MSG = "No such cat item exists: ";
  public static final String VALIDATION_FAILURE_MSG = "Validation failed";
  public static final String INVALID_SCHEMA_MSG = "Invalid Schema";
  public static final String RESOURCE_CHECK_QUERY =
      "{\"query\":{\"bool\":{\"should\":[{\"bool\":{\"must\":[{\"term\":"
              + "{\"id.keyword\":\"$1\"}},{\"term\":{\"type.keyword\": "
              + "\"iudx:Provider\"}}]}},{\"bool\":{\"must\":[{\"term\":"
              + "{\"id.keyword\":\"$2\"}},{\"term\":{\"type.keyword\": "
              + "\"iudx:ResourceGroup\"}}]}}]}}}";
  public static final String RESOURCE_GROUP_CHECK_QUERY =
      "{\"query\":{\"bool\":{\"should\":[{\"bool\":{\"must\":[{\"term\":"
              + "{\"id.keyword\":\"$1\"}},{\"term\":{\"type.keyword\": "
              + "\"iudx:Provider\"}}]}},{\"bool\":{\"must\":[{\"term\":"
              + "{\"id.keyword\":\"$2\"}},{\"term\":{\"type.keyword\": "
              + "\"iudx:ResourceServer\"}}]}}]}}}";
}
