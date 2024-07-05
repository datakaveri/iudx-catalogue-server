package iudx.catalogue.server.database.elastic.query.querydecorator;

import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.database.elastic.ResponseUrn.INVALID_COORDINATE_POLYGON_URN;
import static iudx.catalogue.server.database.elastic.ResponseUrn.INVALID_GEO_PARAMETER_URN;
import static iudx.catalogue.server.util.Constants.*;

import co.elastic.clients.elasticsearch._types.GeoShapeRelation;
import co.elastic.clients.elasticsearch._types.query_dsl.GeoShapeFieldQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.json.JsonData;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import iudx.catalogue.server.database.elastic.exception.EsQueryException;
import java.util.List;
import java.util.Map;

public class GeoQueryFiltersDecorator implements ElasticsearchQueryDecorator {
  private Map<FilterType, List<Query>> queryFilters;
  private JsonObject requestQuery;

  public GeoQueryFiltersDecorator(Map<FilterType, List<Query>> queryFilters, JsonObject requestQuery) {
    this.queryFilters = queryFilters;
    this.requestQuery = requestQuery;
  }

  @Override
  public Map<FilterType, List<Query>> add() {
    Query geoShapeQuery;
    String geometry = requestQuery.getString(GEOMETRY);
    String geoProperty = requestQuery.getString(GEOPROPERTY);
    String relation = requestQuery.getString(GEORELATION);

    /* Construct the search query */
    if (POINT.equalsIgnoreCase(geometry)) {
      // Circle
      JsonArray coordinates = requestQuery.getJsonArray(COORDINATES_KEY);
      int radius = requestQuery.getInteger(MAX_DISTANCE);
      geoShapeQuery = buildCircleGeoShapeQuery(geoProperty, coordinates, relation, radius);

    } else if (POLYGON.equalsIgnoreCase(geometry) || LINESTRING.equalsIgnoreCase(geometry)) {
      // Polygon & LineString
      JsonArray coordinates = requestQuery.getJsonArray(COORDINATES_KEY);
      /*Check if valid polygon*/
      if (POLYGON.equalsIgnoreCase(geometry) && !isValidPolygonCoordinates(coordinates)) {
        throw new EsQueryException(INVALID_COORDINATE_POLYGON_URN, DETAIL_INVALID_COORDINATE_POLYGON);
      }
      geoShapeQuery = buildGeoShapeQuery(geoProperty, geometry, coordinates, relation);

    } else if (BBOX.equalsIgnoreCase(geometry)) {
      /* Construct the query for BBOX */
      JsonArray coordinates = requestQuery.getJsonArray(COORDINATES_KEY);
      geoShapeQuery = buildGeoShapeQuery(geoProperty, GEO_BBOX, coordinates, relation);
    } else {
      throw new EsQueryException(INVALID_GEO_PARAMETER_URN, DETAIL_INVALID_GEO_PARAMETER);
    }

    List<Query> queryList = queryFilters.get(FilterType.FILTER);
    queryList.add(geoShapeQuery);
    return queryFilters;
  }

  private Query buildCircleGeoShapeQuery(String geoProperty, JsonArray coordinates, String relation, int radius) {
    JsonObject geoJson = new JsonObject();
    geoJson.put("type", GEO_CIRCLE);
    geoJson.put("coordinates", coordinates);
    geoJson.put("radius", radius + "m");
    relation = relation.substring(0, 1).toUpperCase() + relation.substring(1).toLowerCase();

    GeoShapeFieldQuery geoShapeFieldQuery = new GeoShapeFieldQuery.Builder()
            .shape(JsonData.fromJson(geoJson.toString()))
            .relation(GeoShapeRelation.valueOf(relation))
            .build();
    Query query = QueryBuilders.geoShape(g->g
            .field(geoProperty+GEO_KEY)
            .shape(geoShapeFieldQuery));

    return query;
  }

  private Query buildGeoShapeQuery(String geoProperty, String geometry, JsonArray coordinates, String relation) {
    JsonObject geoJson = new JsonObject();
    geoJson.put("type", geometry);
    geoJson.put("coordinates", coordinates);
    relation = relation.substring(0, 1).toUpperCase() + relation.substring(1).toLowerCase();

    GeoShapeFieldQuery geoShapeFieldQuery = new GeoShapeFieldQuery.Builder()
            .shape(JsonData.fromJson(geoJson.toString()))
            .relation(GeoShapeRelation.valueOf(relation))
            .build();
    Query query = QueryBuilders.geoShape(g->g
            .field(geoProperty+GEO_KEY)
            .shape(geoShapeFieldQuery));

    return query;
  }

  private boolean isValidPolygonCoordinates(JsonArray coordinates) {
    int length = coordinates.getJsonArray(0).size();
    return coordinates.getJsonArray(0).getJsonArray(0).getDouble(0)
            .equals(coordinates.getJsonArray(0).getJsonArray(length - 1).getDouble(0))
            && coordinates.getJsonArray(0).getJsonArray(0).getDouble(1)
            .equals(coordinates.getJsonArray(0).getJsonArray(length - 1).getDouble(1));
  }

}
