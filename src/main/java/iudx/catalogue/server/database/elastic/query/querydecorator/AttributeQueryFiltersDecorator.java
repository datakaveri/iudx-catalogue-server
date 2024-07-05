package iudx.catalogue.server.database.elastic.query.querydecorator;

import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.util.Constants.*;

import co.elastic.clients.elasticsearch._types.query_dsl.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.elastic.ResponseUrn;
import iudx.catalogue.server.database.elastic.exception.EsQueryException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AttributeQueryFiltersDecorator implements ElasticsearchQueryDecorator {

  private Map<FilterType, List<Query>> queryFilters;
  private JsonObject requestQuery;

  public AttributeQueryFiltersDecorator(
          Map<FilterType, List<Query>> queryFilters, JsonObject requestQuery) {
    this.queryFilters = queryFilters;
    this.requestQuery = requestQuery;
  }

  @Override
  public Map<FilterType, List<Query>> add() {
    // Validate tag search attributes
    if (requestQuery.containsKey(PROPERTY)
            && !requestQuery.getJsonArray(PROPERTY).isEmpty()
            && requestQuery.containsKey(VALUE)
            && !requestQuery.getJsonArray(VALUE).isEmpty()) {
      /* fetching values from request */
      JsonArray propertyAttrs = requestQuery.getJsonArray(PROPERTY);
      JsonArray valueAttrs = requestQuery.getJsonArray(VALUE);

      // Ensure property and value arrays are of the same size
      if (propertyAttrs.size() != valueAttrs.size()) {
        throw new EsQueryException(
                ResponseUrn.INVALID_PROPERTY_VALUE_URN, "Invalid Property Value");
      }

      // Initialize a BoolQuery for combining must clauses
      BoolQuery.Builder mainBoolQueryBuilder = QueryBuilders.bool();

      // Process each property-value pair
      for (int i = 0; i < propertyAttrs.size(); i++) {
        String property = propertyAttrs.getString(i);
        JsonArray values = valueAttrs.getJsonArray(i);
        BoolQuery.Builder propertyBoolQueryBuilder = QueryBuilders.bool();
        processPropertyValues(property, values, propertyBoolQueryBuilder);
        // Add the constructed propertyBoolQuery to mainBoolQueryBuilder as a must clause
        mainBoolQueryBuilder.must(propertyBoolQueryBuilder.build()._toQuery());
      }

      // Add the constructed mainBoolQuery to the appropriate query filter
      List<Query> filterList =queryFilters.computeIfAbsent(FilterType.FILTER, k -> new ArrayList<>());
      filterList.add(mainBoolQueryBuilder.build()._toQuery());
    }

    return queryFilters;
  }

  private void processPropertyValues(
          String property, JsonArray values, BoolQuery.Builder propertyBoolQueryBuilder) {
    // Construct queries based on property and values
    for (int j = 0; j < values.size(); j++) {
      String value = values.getString(j);
      Query query = buildQuery(property, value);
      propertyBoolQueryBuilder.should(query);
    }
  }

  private Query buildQuery(String property, String value) {
    /* Attribute related queries using "match" and without the ".keyword" */
    if (property.equals(TAGS)
            || property.equals(DESCRIPTION_ATTR)
            || property.startsWith(LOCATION)) {
      return MatchQuery.of(query -> query.field(property).query(value))._toQuery();
      /* Attribute related queries using "match" and with the ".keyword" */
    }else{
      /* checking keyword in the query paramters */
      if (property.endsWith(KEYWORD_KEY)) {
        // Use match query without .keyword suffix
        return MatchQuery.of(query -> query.field(property).query(value))._toQuery();
      } else {
        // Add keyword if not available
        return MatchQuery.of(query -> query.field(property + KEYWORD_KEY).query(value))._toQuery();
      }
    }
  }
}
