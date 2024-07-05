package iudx.catalogue.server.database.elastic.model;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;

public class QueryAndAggregation {
  private final Query query;
  private final Aggregation aggregation;

  public QueryAndAggregation(Query query, Aggregation aggregation) {
    this.query = query;
    this.aggregation = aggregation;
  }

  public Query getQuery() {
    return query;
  }

  public Aggregation getAggregation() {
    return aggregation;
  }
}
