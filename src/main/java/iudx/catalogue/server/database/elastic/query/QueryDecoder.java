package iudx.catalogue.server.database.elastic.query;

import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.database.elastic.query.Queries.*;
import static iudx.catalogue.server.database.elastic.query.Queries.buildNestedMustQuery;
import static iudx.catalogue.server.util.Constants.*;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.elastic.ResponseUrn;
import iudx.catalogue.server.database.elastic.exception.EsQueryException;
import iudx.catalogue.server.database.elastic.model.QueryAndAggregation;
import iudx.catalogue.server.database.elastic.query.querydecorator.*;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class QueryDecoder {

  private static final Logger LOGGER = LogManager.getLogger(QueryDecoder.class);

  /**
   * Decodes and constructs ElasticSearch Search/Count query based on the parameters passed in the
   * request.
   *
   * @param request Json object containing various fields related to query-type.
   * @return Query which contains an ElasticSearch query.
   */
  public JsonObject searchQuery(JsonObject request) {

    String searchType = request.getString(SEARCH_TYPE);

    boolean match = false;
    Map<FilterType, List<Query>> queryLists = new HashMap<>();
    for (FilterType filterType : FilterType.values()) {
      queryLists.put(filterType, new ArrayList<Query>());
    }

    if (searchType.equalsIgnoreCase("getParentObjectInfo")) {
      return new JsonObject().put("query", buildGetDocQuery(request.getString(ID)));
    }
    ElasticsearchQueryDecorator queryDecorator = null;

    /* Handle the search type */
    try {
      if (searchType.matches(GEOSEARCH_REGEX)) {
        LOGGER.debug("Info: Geo search block");
        queryDecorator = new GeoQueryFiltersDecorator(queryLists, request);
        queryDecorator.add();

        match = true;
      }

      /* Construct the query for text based search */
      if (searchType.matches(TEXTSEARCH_REGEX)) {
        LOGGER.debug("Info: Text search block");
        queryDecorator = new TextQueryFiltersDecorator(queryLists, request);
        queryDecorator.add();

        match = true;
      }

      /* Construct the query for attribute based search */
      if (searchType.matches(ATTRIBUTE_SEARCH_REGEX)) {
        LOGGER.debug("Info: Attribute search block");
        queryDecorator = new AttributeQueryFiltersDecorator(queryLists, request);
        queryDecorator.add();

        match = true;
      }

      /* Will be used for multi-tenancy */
      String instanceId = request.getString(INSTANCE);

      if (instanceId != null) {
        Query instanceFilter = buildInstanceFilter(instanceId);
        LOGGER.debug("Info: Instance found in query;" + instanceFilter);
        queryLists.get(FilterType.MUST).add(instanceFilter);
      }

      if (searchType.matches(RESPONSE_FILTER_REGEX)) {
        match = true;
        if (!request.getBoolean(SEARCH)) {
          throw new EsQueryException(
              ResponseUrn.OPERATION_NOT_ALLOWED_URN, "operation not allowed");
        }
        if (!request.containsKey(ATTRIBUTE) && !request.containsKey(FILTER)) {
          throw new EsQueryException(ResponseUrn.BAD_FILTER_URN, "bad filters applied");
        }
      }

      if (!match) {
        throw new EsQueryException(ResponseUrn.INVALID_SYNTAX_URN, "Invalid Syntax");
      } else {
        Query q = getBoolQuery(queryLists);
        LOGGER.info("query : {}", q.toString());
        return new JsonObject().put("query", q);
      }
    } catch (EsQueryException e) {
      LOGGER.error("Error constructing search query: {}", e.getMessage());
      return new JsonObject().put(ERROR, e.toJson());
    }
  }

  /**
   * Decodes and constructs ElasticSearch Relationship queries based on the parameters passed in the
   * request.
   *
   * @param request Json object containing various fields related to query-type.
   * @return JsonObject which contains fully formed ElasticSearch query.
   */
  public Query listRelationshipQuery(JsonObject request) {
    LOGGER.debug("request: " + request);

    String relationshipType = request.getString(RELATIONSHIP, "");
    String itemType = request.getString(ITEM_TYPE, "");
    LOGGER.debug("relationshipType: {}", relationshipType);
    LOGGER.debug("itemType: {}", itemType);
    Query subQuery;
    Query termQuery;

    /* Validating the request */
    if (request.containsKey(ID) && relationshipType.equalsIgnoreCase("cos")) {
      String cosId = request.getString(COS_ITEM);
      subQuery = buildTermQuery(ID + KEYWORD_KEY, cosId);
      LOGGER.debug("subQ--> cos: {}", subQuery);
    } else if (request.containsKey(ID) && itemType.equalsIgnoreCase(ITEM_TYPE_COS)) {
      String cosId = request.getString(ID);
      termQuery = buildTermQuery(COS_ITEM + KEYWORD_KEY, cosId);
      switch (relationshipType) {
        case RESOURCE:
          subQuery = buildRelSubQuery(termQuery, ITEM_TYPE_RESOURCE);
          break;
        case RESOURCE_GRP:
          subQuery = buildRelSubQuery(termQuery, ITEM_TYPE_RESOURCE_GROUP);
          break;
        case RESOURCE_SVR:
          subQuery = buildRelSubQuery(termQuery, ITEM_TYPE_RESOURCE_SERVER);
          break;
        case PROVIDER:
          subQuery = buildRelSubQuery(termQuery, ITEM_TYPE_PROVIDER);
          break;
        default:
          return null;
      }
    } else if (request.containsKey(ID)
        && RESOURCE.equals(relationshipType)
        && itemType.equalsIgnoreCase(ITEM_TYPE_PROVIDER)) {
      String providerId = request.getString(ID);
      subQuery = buildNestedMustQuery(PROVIDER, providerId, ITEM_TYPE_RESOURCE);
    } else if (request.containsKey(ID)
        && RESOURCE.equals(relationshipType)
        && itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_GROUP)) {
      String resourceGroupId = request.getString(ID);

      subQuery = buildNestedMustQuery(RESOURCE_GRP, resourceGroupId, ITEM_TYPE_RESOURCE);
    } else if (request.containsKey(ID)
        && RESOURCE.equals(relationshipType)
        && itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_SERVER)) {
      String resourceServerId = request.getString(ID);

      subQuery = buildNestedMustQuery(RESOURCE_SVR, resourceServerId, ITEM_TYPE_RESOURCE);
    } else if (request.containsKey(ID)
        && RESOURCE_GRP.equals(relationshipType)
        && itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE)) {
      String resourceGroupId = request.getString("resourceGroup");
      subQuery = buildNestedMustQuery(ID, resourceGroupId, ITEM_TYPE_RESOURCE_GROUP);

    } else if (request.containsKey(ID)
        && RESOURCE_GRP.equals(relationshipType)
        && itemType.equalsIgnoreCase(ITEM_TYPE_PROVIDER)) {
      String providerId = request.getString(ID);
      subQuery = buildNestedMustQuery(PROVIDER, providerId, ITEM_TYPE_RESOURCE_GROUP);

    } else if (request.containsKey(ID)
        && RESOURCE_GRP.equals(relationshipType)
        && itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_SERVER)) {
      JsonArray providerIds = request.getJsonArray("providerIds");
      // Extract provider IDs from JSON array
      List<String> ids =
          providerIds.stream()
              .map(JsonObject.class::cast)
              .map(providerId -> providerId.getString(ID))
              .collect(Collectors.toList());
      // Build the should clauses for each provider ID
      BoolQuery.Builder boolQueryBuilder = QueryBuilders.bool();
      for (String id : ids) {
        boolQueryBuilder.should(buildMatchQuery("provider.keyword", id));
      }
      // Minimum should match clause
      boolQueryBuilder.minimumShouldMatch(String.valueOf(1));
      // Build the final Elasticsearch Query
      Query query = boolQueryBuilder.build()._toQuery();
      return query;
    } else if (request.containsKey(ID)
        && PROVIDER.equals(relationshipType)
        && itemType.equalsIgnoreCase(ITEM_TYPE_RESOURCE_SERVER)) {
      String resourceServerId = request.getString(ID);

      subQuery = buildNestedMustQuery(RESOURCE_SVR, resourceServerId, ITEM_TYPE_PROVIDER);
    } else if (request.containsKey(ID) && PROVIDER.equals(relationshipType)) {
      // String id = request.getString(ID);
      String providerId = request.getString(PROVIDER);

      subQuery = buildNestedMustQuery(ID, providerId, ITEM_TYPE_PROVIDER);

    } else if (request.containsKey(ID) && RESOURCE_SVR.equals(relationshipType)) {
      String resourceServer = request.getString(RESOURCE_SVR);

      subQuery =
          buildRelSubQuery(buildMatchQuery(ID_KEYWORD, resourceServer), ITEM_TYPE_RESOURCE_SERVER);

    } else if (request.containsKey(ID) && TYPE_KEY.equals(relationshipType)) {
      /* parsing id from the request */
      String itemId = request.getString(ID);

      subQuery = buildTermQuery(ID_KEYWORD, itemId);

    } else if (request.containsKey(ID) && ALL.equalsIgnoreCase(relationshipType)) {
      String id = request.getString(ID);
      BoolQuery.Builder boolQuery = QueryBuilders.bool();
      boolQuery.should(buildMatchQuery(ID_KEYWORD, id));
      if (request.containsKey(RESOURCE_GRP)) {
        boolQuery.should(buildMatchQuery(ID_KEYWORD, request.getString(RESOURCE_GRP)));
      }
      if (request.containsKey(PROVIDER)) {
        boolQuery.should(buildMatchQuery(ID_KEYWORD, request.getString(PROVIDER)));
      }
      if (request.containsKey(RESOURCE_SVR)) {

        boolQuery.should(buildMatchQuery(ID_KEYWORD, request.getString(RESOURCE_SVR)));
      }
      if (request.containsKey(COS_ITEM)) {

        boolQuery.should(buildMatchQuery(ID_KEYWORD, request.getString(COS_ITEM)));
      }
      Query query = boolQuery.build()._toQuery();
      LOGGER.debug("Query: " + query);
      return query;
    } else {
      return null;
    }
    LOGGER.debug("subQuery: " + subQuery);
    return subQuery;
  }

  /**
   * Decodes and constructs Elastic query for listing items based on the parameters passed in the
   * request.
   *
   * @param request Json object containing various fields related to query-type.
   * @return JsonObject which contains fully formed ElasticSearch query.
   */
  public QueryAndAggregation listItemQuery(JsonObject request) {

    LOGGER.debug("Info: Reached list items;" + request.toString());
    String itemType = request.getString(ITEM_TYPE);
    String type = request.getString(TYPE_KEY);
    String instanceId = request.getString(INSTANCE);
    Integer limit =
        request.getInteger(
            LIMIT, FILTER_PAGINATION_SIZE - request.getInteger(OFFSET, FILTER_PAGINATION_FROM));
    Query query;
    Aggregation aggregation;

    if (itemType.equalsIgnoreCase(TAGS)) {
      aggregation = buildTermsAggs(TAGS + KEYWORD_KEY, limit);
      if (instanceId == null || instanceId.isEmpty()) {
        // Match all documents and aggregate tags
        query = QueryBuilders.matchAll().build()._toQuery();
      } else {
        query = buildTermQuery("instance.keyword", instanceId);
      }
    } else {
      aggregation = buildTermsAggs(ID_KEYWORD, limit);
      if (instanceId == null || instanceId.isEmpty()) {
        query = QueryBuilders.bool(b -> b.filter(buildMatchQuery("type", type)));
      } else {
        // Filter by type and instance, then aggregate IDs
        query =
            QueryBuilders.bool(
                b ->
                    b.filter(buildMatchQuery("type", type))
                        .filter(buildTermQuery("instance.keyword", instanceId)));
      }
    }
    return new QueryAndAggregation(query, aggregation);
  }

  private Query getBoolQuery(Map<FilterType, List<Query>> filterQueries) {

    BoolQuery.Builder boolQuery = new BoolQuery.Builder();

    for (Map.Entry<FilterType, List<Query>> entry : filterQueries.entrySet()) {
      if (FilterType.FILTER.equals(entry.getKey())
          && !filterQueries.get(FilterType.FILTER).isEmpty()) {
        boolQuery.filter(filterQueries.get(FilterType.FILTER));
      }

      if (FilterType.MUST_NOT.equals(entry.getKey())
          && !filterQueries.get(FilterType.MUST_NOT).isEmpty()) {
        boolQuery.mustNot(filterQueries.get(FilterType.MUST_NOT));
      }

      if (FilterType.MUST.equals(entry.getKey()) && !filterQueries.get(FilterType.MUST).isEmpty()) {
        boolQuery.must(filterQueries.get(FilterType.MUST));
      }

      if (FilterType.SHOULD.equals(entry.getKey())
          && !filterQueries.get(FilterType.SHOULD).isEmpty()) {
        boolQuery.should(filterQueries.get(FilterType.SHOULD));
      }
    }

    return boolQuery.build()._toQuery();
  }
}
