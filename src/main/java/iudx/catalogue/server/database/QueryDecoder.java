package iudx.catalogue.server.database;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.util.Constants.*;

public final class QueryDecoder {

  private static final Logger LOGGER = LogManager.getLogger(QueryDecoder.class);

  /**
   * Decodes and constructs ElasticSearch Search/Count query based on the parameters passed in the
   * request.
   *
   * @param request Json object containing various fields related to query-type.
   * @return JsonObject which contains fully formed ElasticSearch query.
   */
  public JsonObject searchQuery(JsonObject request) {

    String searchType = request.getString(SEARCH_TYPE);
    JsonObject elasticQuery = new JsonObject();
    String queryGeoShape = null;
    JsonArray mustQuery = new JsonArray();
    Boolean match = false;

    /* Will be used for multi-tenancy */
    String instanceId = request.getString(INSTANCE);

    /* TODO: Pagination for large result set */
    if (request.getBoolean(SEARCH)) {
      elasticQuery.put(SIZE_KEY, 10);
    }

    /* Handle the search type */
    if (searchType.matches(GEOSEARCH_REGEX)) {
      LOGGER.debug("Info: Geo search block");

      match = true;
      String relation;
      JsonArray coordinates;
      String geometry = request.getString(GEOMETRY);
      /* Construct the search query */
      if (POINT.equalsIgnoreCase(geometry)) {
        /* Construct the query for Circle */
        coordinates = request.getJsonArray(COORDINATES_KEY);
        relation = request.getString(GEORELATION);
        int radius = request.getInteger(MAX_DISTANCE);
        String radiusStr = ",\"radius\": \"$1m\"".replace("$1", Integer.toString(radius));
        queryGeoShape = GEO_SHAPE_QUERY.replace("$1", GEO_CIRCLE)
            .replace("$2", coordinates.toString() + radiusStr).replace("$3", relation)
            .replace("$4", GEO_KEY);
      } else if (POLYGON.equalsIgnoreCase(geometry) || LINESTRING.equalsIgnoreCase(geometry)) {
        relation = request.getString(GEORELATION);
        coordinates = request.getJsonArray(COORDINATES_KEY);
        int length = coordinates.getJsonArray(0).size();
        /* Check if valid polygon */
        if (geometry.equalsIgnoreCase(POLYGON)
            && !coordinates.getJsonArray(0).getJsonArray(0).getDouble(0)
                .equals(coordinates.getJsonArray(0).getJsonArray(length - 1).getDouble(0))
            && !coordinates.getJsonArray(0).getJsonArray(0).getDouble(1)
                .equals(coordinates.getJsonArray(0).getJsonArray(length - 1).getDouble(1))) {
          return new JsonObject().put(ERROR, ERROR_INVALID_COORDINATE_POLYGON);
        }
        queryGeoShape = GEO_SHAPE_QUERY.replace("$1", geometry).replace("$2", coordinates.toString())
            .replace("$3", relation).replace("$4", GEO_KEY);

      } else if (BBOX.equalsIgnoreCase(geometry)) {
        /* Construct the query for BBOX */
        relation = request.getString(GEORELATION);
        coordinates = request.getJsonArray(COORDINATES_KEY);
        queryGeoShape = GEO_SHAPE_QUERY.replace("$1", GEO_BBOX).replace("$2", coordinates.toString())
            .replace("$3", relation).replace("$4", GEO_KEY);
      } else {
        return new JsonObject().put(ERROR, ERROR_INVALID_GEO_PARAMETER);
      }
    }

    /* Construct the query for text based search */
    if (searchType.matches(TEXTSEARCH_REGEX)) {
      LOGGER.debug("Info: Text search block");

      match = true;
      /* validating tag search attributes */
      if (request.containsKey(Q_VALUE) && !request.getString(Q_VALUE).isBlank()) {
        /* constructing db queries */
        String textAttr = request.getString(Q_VALUE);
        String textQuery = TEXT_QUERY.replace("$1", textAttr);
        mustQuery.add(new JsonObject(textQuery));
      }
    }

    /* Construct the query for attribute based search */
    if (searchType.matches(ATTRIBUTE_SEARCH_REGEX)) {
      LOGGER.debug("Info: Attribute search block");

      match = true;
      /* validating tag search attributes */
      if (request.containsKey(PROPERTY) && !request.getJsonArray(PROPERTY).isEmpty()
          && request.containsKey(VALUE) && !request.getJsonArray(VALUE).isEmpty()) {
        /* fetching values from request */
        JsonArray propertyAttrs = request.getJsonArray(PROPERTY);
        JsonArray valueAttrs = request.getJsonArray(VALUE);
        /* For attribute property and values search */
        if (propertyAttrs.size() == valueAttrs.size()) {
          /* Mapping and constructing the value attributes with the property attributes for query */
          for (int i = 0; i < valueAttrs.size(); i++) {
            JsonArray shouldQuery = new JsonArray();
            JsonArray valueArray = valueAttrs.getJsonArray(i);
            for (int j = 0; j < valueArray.size(); j++) {
              String matchQuery;
              /* Attribute related queries using "match" and without the ".keyword" */
              if (propertyAttrs.getString(i).equals(TAGS)
                  || propertyAttrs.getString(i).equals(DESCRIPTION_ATTR)
                  || propertyAttrs.getString(i).startsWith(LOCATION)) {

                matchQuery = MATCH_QUERY.replace("$1", propertyAttrs.getString(i))
                                        .replace("$2", valueArray.getString(j));
                shouldQuery.add(new JsonObject(matchQuery));
                /* Attribute related queries using "match" and with the ".keyword" */
              } else {
                /* checking keyword in the query paramters */
                if (propertyAttrs.getString(i).endsWith(KEYWORD_KEY)) {
                  matchQuery = MATCH_QUERY.replace("$1", propertyAttrs.getString(i))
                                          .replace("$2", valueArray.getString(j));
                } else {

                  /* add keyword if not avaialble */
                  matchQuery = MATCH_QUERY.replace("$1", propertyAttrs.getString(i) + KEYWORD_KEY)
                                          .replace("$2", valueArray.getString(j));
                }
                shouldQuery.add(new JsonObject(matchQuery));
              }
            }
            mustQuery.add(new JsonObject(SHOULD_QUERY.replace("$1", shouldQuery.toString())));
          }
        } else {
          return new JsonObject().put(ERROR, ERROR_INVALID_PARAMETER);
        }
      }
    }

    if (instanceId != null) {
      String instanceFilter = INSTANCE_FILTER.replace("$1", instanceId);
      LOGGER.debug("Info: Instance found in query;" + instanceFilter);
      mustQuery.add(new JsonObject(instanceFilter));
    }

    /* checking the requests for limit attribute */
    if (request.containsKey(LIMIT)) {
      Integer sizeFilter = request.getInteger(LIMIT);
      elasticQuery.put(SIZE_KEY, sizeFilter);
    }

    /* checking the requests for offset attribute */
    if (request.containsKey(OFFSET)) {
      Integer offsetFilter = request.getInteger(OFFSET);
      elasticQuery.put(FROM, offsetFilter);
    }

    if (searchType.matches(RESPONSE_FILTER_REGEX)) {
     
      /* Construct the filter for response */
      LOGGER.debug("Info: Adding responseFilter");
      match = true;
      
      if (!request.getBoolean(SEARCH)) {
        return new JsonObject().put(ERROR, COUNT_UNSUPPORTED);
      }
      
      if (request.containsKey(ATTRIBUTE)) {
        JsonArray sourceFilter = request.getJsonArray(ATTRIBUTE);
        elasticQuery.put(SOURCE, sourceFilter);
      } else if (request.containsKey(FILTER)) {
        JsonArray sourceFilter = request.getJsonArray(FILTER);
        elasticQuery.put(SOURCE, sourceFilter);
      } else {
        return new JsonObject().put(ERROR, ERROR_INVALID_RESPONSE_FILTER);
      }
    }

    if (!match) {
      return new JsonObject().put(ERROR, INVALID_SEARCH);
    } else {

      JsonObject boolQuery = new JsonObject(MUST_QUERY.replace("$1", mustQuery.toString()));
      /* return fully formed elastic query */
      if (queryGeoShape != null) {
        boolQuery.getJsonObject("bool").put(FILTER,
            new JsonArray().add(new JsonObject(queryGeoShape)));
      }
      return elasticQuery.put(QUERY_KEY, boolQuery);
    }
  }

  /**
   * Decodes and constructs ElasticSearch Relationship queries based on the parameters passed in the
   * request.
   *
   * @param request Json object containing various fields related to query-type.
   * @return JsonObject which contains fully formed ElasticSearch query.
   */
  public String listRelationshipQuery(JsonObject request) {

    String relationshipType = request.getString(RELATIONSHIP);
    String subQuery = "";

    /* Validating the request */
    if (request.containsKey(ID) && RESOURCE.equals(relationshipType)) {

      /* parsing resourceGroupId from the request */
      String resourceGroupId = request.getString(ID);
      
      subQuery = TERM_QUERY.replace("$1", RESOURCE_GRP + KEYWORD_KEY)
                           .replace("$2", resourceGroupId) 
                            + "," + 
                 TERM_QUERY.replace("$1", TYPE_KEYWORD)
                           .replace("$2", ITEM_TYPE_RESOURCE);

    } else if (request.containsKey(ID) && RESOURCE_GRP.equals(relationshipType)) {

      String resourceGroupId =
          StringUtils.substringBeforeLast(request.getString(ID), FORWARD_SLASH);
      
      subQuery = TERM_QUERY.replace("$1", ID_KEYWORD)
                           .replace("$2", resourceGroupId) 
                           + "," + 
                 TERM_QUERY.replace("$1", TYPE_KEYWORD)
                           .replace("$2", ITEM_TYPE_RESOURCE_GROUP);

    } else if (request.containsKey(ID) && PROVIDER.equals(relationshipType)) {

      /* parsing id/providerId from the request */
      String id = request.getString(ID);
      String providerId = StringUtils.substring(id, 0, id.indexOf("/", id.indexOf("/") + 1));

      subQuery = TERM_QUERY.replace("$1", ID_KEYWORD)
                           .replace("$2", providerId) 
                           + "," + 
                 TERM_QUERY.replace("$1", TYPE_KEYWORD)
                           .replace("$2", ITEM_TYPE_PROVIDER);

    } else if (request.containsKey(ID) && RESOURCE_SVR.equals(relationshipType)) {
            
      /* parsing id from the request */
      String[] id = request.getString(ID).split(FORWARD_SLASH);

      subQuery = MATCH_QUERY.replace("$1", ID)
                            .replace("$2", id[0])
                            + "," + 
                 MATCH_QUERY.replace("$1", ID)
                            .replace("$2", id[2])
                            + "," + 
                 TERM_QUERY.replace("$1", TYPE_KEYWORD)
                           .replace("$2", ITEM_TYPE_RESOURCE_SERVER);

    } else if (request.containsKey(ID) && TYPE_KEY.equals(relationshipType)) {

      /* parsing id from the request */
      String itemId = request.getString(ID);

      subQuery = TERM_QUERY.replace("$1", ID_KEYWORD)
                           .replace("$2", itemId);
    } else {
      return null;
    }

    String elasticQuery = BOOL_MUST_QUERY.replace("$1", subQuery);

    if (TYPE_KEY.equals(relationshipType)) {
      elasticQuery = new JsonObject(elasticQuery).put(SOURCE, TYPE_KEY).toString();
    }

    return elasticQuery;
  }

  /**
   * Decodes and constructs Elastic query for listing items based on the parameters passed in the
   * request.
   *
   * @param request Json object containing various fields related to query-type.
   * @return JsonObject which contains fully formed ElasticSearch query.
   */
  public String listItemQuery(JsonObject request) {

    LOGGER.debug("Info: Reached list items;" + request.toString());
    String itemType = request.getString(ITEM_TYPE);
    String type = request.getString(TYPE_KEY);
    String instanceID = request.getString(INSTANCE);
    String elasticQuery = "";

    if (itemType.equalsIgnoreCase(TAGS)) {
      if (instanceID == null || instanceID == "") {
        elasticQuery = LIST_TAGS_QUERY;
      } else {
        elasticQuery = LIST_INSTANCE_TAGS_QUERY.replace("$1", instanceID);
      }
    } else {
      if (instanceID == null || instanceID == "") {
        elasticQuery = LIST_TYPES_QUERY.replace("$1", type);
      } else {
        elasticQuery = LIST_INSTANCE_TYPES_QUERY.replace("$1", type).replace("$2", instanceID);
      }
    }
    return elasticQuery;
  }
}
