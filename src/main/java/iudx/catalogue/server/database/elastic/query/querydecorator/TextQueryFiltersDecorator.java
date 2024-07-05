package iudx.catalogue.server.database.elastic.query.querydecorator;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.elastic.exception.EsQueryException;
import iudx.catalogue.server.database.elastic.ResponseUrn;
import java.util.List;
import java.util.Map;

import static iudx.catalogue.server.util.Constants.Q_VALUE;

public class TextQueryFiltersDecorator implements ElasticsearchQueryDecorator {

  private Map<FilterType, List<Query>> queryFilters;
  private JsonObject requestQuery;

  public TextQueryFiltersDecorator(
      Map<FilterType, List<Query>> queryFilters, JsonObject requestQuery) {
    this.queryFilters = queryFilters;
    this.requestQuery = requestQuery;
  }

  @Override
  public Map<FilterType, List<Query>> add() {
    if (requestQuery.containsKey(Q_VALUE) && !requestQuery.getString(Q_VALUE).isBlank()) {
      String textAttr = requestQuery.getString(Q_VALUE);

      Query queryStringQuery = QueryBuilders.queryString(query -> query.query(textAttr));

      List<Query> queryList = queryFilters.get(FilterType.FILTER);
      queryList.add(queryStringQuery);
    } else {
      throw new EsQueryException(ResponseUrn.BAD_TEXT_QUERY_URN, "bad text query values");
    }
    return queryFilters;
  }
}
