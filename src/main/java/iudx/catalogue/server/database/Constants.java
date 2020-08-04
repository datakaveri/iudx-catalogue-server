package iudx.catalogue.server.database;

import io.vertx.core.json.JsonObject;

public class Constants {

  /* General purpose */
  static final String DATABASE_IP = "databaseIP";
  static final String DATABASE_PORT = "databasePort";
  static final String SEARCH = "search";
  static final String COUNT = "count";
  static final String DESCRIPTION = "desc";
  static final String FAILED = "failed";
  static final String GEOSEARCH_REGEX = "(.*)geoSearch(.*)";
  static final String HTTP = "http";
  static final String ID = "id";
  static final String RESPONSE_FILTER = "responseFilter_";
  static final String RESPONSE_FILTER_GEO = "responseFilter_geoSearch_";
  static final String RESPONSE_FILTER_REGEX = "(.*)responseFilter(.*)";
  static final String RESULT = "results";
  static final String SEARCH_TYPE = "searchType";
  static final String SEARCH_TYPE_GEO = "geoSearch_";
  static final String SHAPE_KEY = "shape";
  static final String SIZE = "size";
  static final String SOURCE = "_source";
  static final String STATUS = "status";
  static final String SUCCESS = "success";
  static final String INSTANCE_ID_KEY = "instanceId";
  static final String CONFIG_FILE = "config.properties";
  static final String RESOURCE = "resource";
  static final String ITEM_TYPE = "itemType";

  static final JsonObject ERROR_JSON_RESP = new JsonObject();

  /* Database */
  static final String CAT_INDEX_NAME = "testindex";
  static final String CAT_COUNT_INDEX = CAT_INDEX_NAME + "/_count";
  static final String CAT_SEARCH_INDEX = CAT_INDEX_NAME + "/_search";
  static final String CAT_DOC = CAT_INDEX_NAME + "/_doc";
  static final String BOOL_KEY = "bool";
  static final String FILTER_KEY = "filter";
  static final String FILTER_PATH = "?filter_path=took,hits.total.value,hits.hits._source&size=10000";
  static final String FILTER_PATH_AGGREGATION = "?filter_path=hits.total.value,aggregations.results.buckets&size=10000";
  static final String FILTER_ID_ONLY_PATH = "?filter_path=hits.total.value,hits.hits._id&size=10000";
  static final String HITS = "hits";
  static final String AGGREGATIONS = "aggregations";
  static final String QUERY_KEY = "query";
  static final String REQUEST_GET = "GET";
  static final String REQUEST_POST = "POST";
  static final String REQUEST_PUT = "PUT";
  static final String REQUEST_DELETE = "DELETE";
  static final String TAGS = "tags";
  static final String TOTAL = "total";
  static final String TOTAL_HITS = "totalHits";
  static final String TYPE_KEY = "type";
  static final String VALUE = "value";
  static final String WITHIN = "within";
  static final String SIZE_KEY = "size";
  static final String INSERT = "insert";
  static final String UPDATE = "update";
  static final String DELETE = "delete";
  static final String ID_KEYWORD = "id.keyword";
  static final String DOC_ID = "_id";
  static final String METHOD = "method";
  static final String RESULTS = "results";
  static final String BUCKETS = "buckets";
  static final String KEY = "key";
  static final String TERM = "term";
  /* Geo-Spatial */
  static final String BBOX = "bbox";
  static final String COORDINATES_KEY = "coordinates";
  static final String DISTANCE_IN_METERS = "m";
  static final String GEO_BBOX = "envelope";
  static final String GEO_CIRCLE = "circle";
  static final String GEO_KEY = "location.geometry";
  static final String GEO_RADIUS = "radius";
  static final String GEO_RELATION_KEY = "relation";
  static final String GEO_SHAPE_KEY = "geo_shape";
  static final String GEOMETRY = "geometry";
  static final String GEOPROPERTY = "geoproperty";
  static final String GEORELATION = "georel";
  static final String INTERSECTS = "intersects";
  static final String LINESTRING = "linestring";
  static final String LOCATION = "location";
  static final String MAX_DISTANCE = "maxDistance";
  static final String POINT = "point";
  static final String POLYGON = "polygon";
  /* Response Filter */
  static final String ATTRIBUTE = "attrs";
  /* Error */
  static final String DATABASE_ERROR = "DB Error. Check logs for more information";
  static final String DATABASE_BAD_QUERY = "Query Failed with status != 20x";
  static final String EMPTY_RESPONSE = "Empty response";
  static final String ERROR = "Error";
  static final String ERROR_INVALID_COORDINATE_POLYGON = "Coordinate mismatch (Polygon)";
  static final String ERROR_INVALID_GEO_PARAMETER = "Missing/Invalid geo parameters";
  static final String ERROR_INVALID_RESPONSE_FILTER = "Missing/Invalid responseFilter parameters";
  static final String NO_SEARCH_TYPE_FOUND = "No searchType found";
  static final String COUNT_UNSUPPORTED = "Count is not supported with filtering";
  static final String INVALID_SEARCH = "Invalid search request";

  public static final String TAGSEARCH_REGEX = "(.*)tagsSearch(.*)";
  public static final String TEXTSEARCH_REGEX = "(.*)textSearch(.*)";
  public static final String PROPERTY = "property";
  public static final String MATCH_KEY = "match";
  public static final String TERMS_KEY = "terms";
  public static final String STRING_QUERY_KEY = "query_string";
  public static final String LIMIT = "limit";
  public static final String OFFSET = "offset";
  public static final String FROM = "from";
  public static final String ERROR_INVALID_PARAMETER = "Incorrect/missing query parameters";
  public static final String Q_KEY = "q";

  public static final String ATTRIBUTE_SEARCH_REGEX = "(.*)attributeSearch(.*)";
  public static final String KEYWORD_KEY = ".keyword";
  public static final String SHOULD_KEY = "should";
  public static final String MUST_KEY = "must";

  public static final String TEXT_SEARCH = "textSearch_";
  public static final String ATTRIBUTE_SEARCH = "attributeSearch_";
  public static final String DEVICEID_KEY = "deviceId";
  public static final String TAG_AQM = "aqm";
  public static final String GEO_SEARCH = "geoSearch_";
  public static final String DESCRIPTION_ATTR = "description";

  /** ElasticClient search types */
  public static final String DOC_IDS_ONLY = "DOCIDS";
  public static final String SOURCE_ONLY = "SOURCE";
  public static final String AGGREGATION_ONLY = "AGGREGATION";


}
