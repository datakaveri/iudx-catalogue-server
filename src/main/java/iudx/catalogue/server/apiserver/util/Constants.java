package iudx.catalogue.server.apiserver.util;

public class Constants {

  private static String basePath = "/iudx/cat/v1";

  public static final String HEADER_ACCEPT = "Accept";
  public static final String HEADER_TOKEN = "token";
  public static final String HEADER_CONTENT_LENGTH = "Content-Length";
  public static final String HEADER_CONTENT_TYPE = "Content-Type";
  public static final String HEADER_HOST = "Host";
  public static final String HEADER_ORIGIN = "Origin";
  public static final String HEADER_REFERER = "Referer";
  public static final String HEADER_CORS = "Access-Control-Allow-Origin";
  public static final String MIME_APPLICATION_JSON = "application/json";

  public static final String ROUTE_APIS = "/apis/*";
  public static final String ROUTE_ITEMS = basePath.concat("/item");
  public static final String ROUTE_SEARCH = basePath.concat("/search");
  public static final String ROUTE_TAGS = basePath.concat("/tags");
  public static final String ROUTE_DOMAINS = basePath.concat("/domains");
  public static final String ROUTE_CITIES = basePath.concat("/cities");
  public static final String ROUTE_RESOURCE_SERVERS = basePath.concat("/resourceservers");
  public static final String ROUTE_PROVIDERS = basePath.concat("/providers");
  public static final String ROUTE_RESOURCE_GROUPS = basePath.concat("/resourcegroups");


  public static final String ID = "id";
  public static final String ITEM_TYPE = "itemType";
  public static final String RESOURCE_ITEM = "resItem";
  public static final String RESOURCE_GRP_ITEM = "resGrpItem";
  public static final String RESOURCE_SVR_ITEM = "resSvrItem";
  public static final String PROVIDER_ORG = "prvdrOrg";
  public static final String PROVIDER_ITEM = "pvdrItem";
  public static final String DATA_DES_ITEM = "dataDesItem";


  public static final String ROUTE_UPDATE_ITEMS =
      basePath.concat("/item/:resItem/:resGrpItem/:resSvrItem/:pvdrItem/:dataDesItem");
  public static final String ROUTE_DELETE_ITEMS =
      basePath.concat("/item/:" + PROVIDER_ORG + "/:" + PROVIDER_ITEM 
          + "/:" + RESOURCE_SVR_ITEM + "/:" + RESOURCE_GRP_ITEM + "/:" + RESOURCE_ITEM);
  public static final String ROUTE_LIST_ITEMS =
      basePath.concat("/items/:resItem/:resGrpItem/:resSvrItem/:pvdrItem/:dataDesItem");
  public static final String ROUTE_LIST_RESOURCE_REL = basePath.concat("\\/(?<id>.*)\\/resource");
  public static final String ROUTE_LIST_RESOURCE_GROUP_REL =
      basePath.concat("\\/(?<id>.*)\\/resourceGroup");

  public static final String ROUTE_UI_CITIES = basePath.concat("/ui/cities");
  public static final String ROUTE_UI_CONFIG = basePath.concat("/ui/config");
  public static final String ROUTE_PROVIDER_REL = basePath.concat("\\/(?<id>.*)\\/provider");
  public static final String ROUTE_RESOURCE_SERVER_REL =
      basePath.concat("\\/(?<id>.*)\\/resourceServer");
  public static final String ROUTE_DATA_TYPE = basePath.concat("\\/(?<id>.*)\\/type");
  public static final String ROUTE_COUNT = basePath.concat("/count");

  public static final String ITEM_TYPE_RESOURCE = "iudx:Resource";
  public static final String ITEM_TYPE_RESOURCE_GROUP = "iudx:ResourceGroup";
  public static final String ITEM_TYPE_RESOURCE_SERVER = "iudx:ResourceServer";
  public static final String ITEM_TYPE_PROVIDER = "iudx:Provider";
  public static final String RELATIONSHIP = "relationship";
  public static final String REL_RESOURCE = "resource";
  public static final String REL_RESOURCE_GRP = "resourceGroup";
  public static final String REL_RESOURCE_SVR = "resourceServer";
  public static final String REL_PROVIDER = "provider";
  public static final String REL_TYPE = "type";

  public static final String GEOREL_WITHIN = "within";
  public static final String GEOREL_NEAR = "near";
  public static final String GEOREL_COVERED_BY = "coveredBy";
  public static final String GEOREL_INTERSECTS = "intersects";
  public static final String GEOREL_EQUALS = "equals";
  public static final String GEOREL_DISJOINT = "disjoint";

  public static final String CONFIG_FILE = "config.properties";
  public static final String KEYSTORE_FILE_NAME = "keystore";
  public static final String KEYSTORE_FILE_PASSWORD = "keystorePassword";
  public static final int PORT = 8443;

  public static final String PROPERTY = "property";
  public static final String VALUE = "value";
  public static final String INSTANCE_ID_KEY = "instanceID";

  public static final String GEOPROPERTY = "geoproperty";
  public static final String GEOREL = "georel";
  public static final String GEOMETRY = "geometry";
  public static final String COORDINATES = "coordinates";
  public static final String Q_VALUE = "q";
  public static final String LIMIT = "limit";
  public static final String OFFSET = "offset";
  public static final String PROVIDER_NAME = "provider.name";
  public static final String LOCATION = "location";
  public static final String BBOX = "bbox";
  public static final String POLYGON = "Polygon";
  public static final String POINT = "Point";
  public static final String LINE_STRING = "LineString";
  public static final String PARTIAL_CONTENT = "partial-content";
  public static final String TEXT = "text";
  public static final String MAX_DISTANCE = "maxDistance";

  public static final String SEARCH_TYPE = "searchType";
  public static final String GEO_SEARCH = "geoSearch_";
  public static final String TEXT_SEARCH = "textSearch_";
  public static final String TAGS_SEARCH = "tagsSearch_";
  public static final String ATTRIBUTE_SEARCH = "attributeSearch_";
  public static final String RESPONSE_FILTER = "responseFilter_";
  public static final String FILTER = "filter";
  public static final String TAGS = "tags";


  public static final String OPERATION = "operation";
  public static final String GET_CITIES = "getCities";
  public static final String ATTRIBUTE_FILTER = "attribute-filter";

  public static final String STATUS = "status";
  public static final String INVALID_SYNTAX = "invalidSyntax";
  public static final String INVALID_VALUE = "invalidValue";
  public static final String INVALID_HEADER = "invalidHeader";
  public static final String RESULTS = "results";
  public static final String SUCCESS = "success";
  public static final String BAD_REQUEST = "Bad Request";
  public static final String INTERNAL_SERVER_ERROR = "Internal server error";

  public static final String PATTERN_TEXT = "^[\\*]{0,1}[A-Za-z ]+[\\*]{0,1}";
  public static final String PATTERN_ARRAY = "^\\[.*\\]$";


  public static final String POST = "POST";
  public static final String PUT = "PUT";
  public static final String DELETE = "DELETE";
}
