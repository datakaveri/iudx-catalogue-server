package iudx.catalogue.server.database.elastic.query;

import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.geocoding.util.Constants.*;
import static iudx.catalogue.server.geocoding.util.Constants.COUNTRY;
import static iudx.catalogue.server.util.Constants.*;
import static iudx.catalogue.server.util.Constants.BBOX;

import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import co.elastic.clients.elasticsearch.core.search.SourceFilter;
import co.elastic.clients.json.JsonData;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.*;

public class Queries {
  public static Query buildMlayerDatasetQuery(String id1, String id2, String id3) {
    // Match queries
    Query matchId1 = buildMatchQuery(ID_KEYWORD, id1);

    Query matchTypeResourceGroup = buildMatchQuery(TYPE_KEYWORD, ITEM_TYPE_RESOURCE_GROUP);

    Query matchId2 = buildMatchQuery(ID_KEYWORD, id2);

    Query matchTypeProvider = buildMatchQuery(TYPE_KEYWORD, ITEM_TYPE_PROVIDER);

    Query matchResourceGroup = buildMatchQuery("resourceGroup" + KEYWORD_KEY, id1);

    Query matchTypeResource = buildMatchQuery(TYPE_KEYWORD, ITEM_TYPE_RESOURCE);

    Query matchId3 = buildMatchQuery(ID_KEYWORD, id3);

    Query matchTypeCos = buildMatchQuery(TYPE_KEYWORD, ITEM_TYPE_COS);

    // Bool queries with must clauses
    Query boolQuery1 = getBoolQuery(matchId1, matchTypeResourceGroup);

    Query boolQuery2 = getBoolQuery(matchId2, matchTypeProvider);

    Query boolQuery3 = getBoolQuery(matchResourceGroup, matchTypeResource);

    Query boolQuery4 = getBoolQuery(matchId3, matchTypeCos);

    return BoolQuery.of(b -> b.should(boolQuery1, boolQuery2, boolQuery3, boolQuery4))._toQuery();
  }

  public static Query getBoolQuery(Query key, Query value) {
    return BoolQuery.of(b -> b.must(key, value))._toQuery();
  }

  public static Query buildMlayerInstanceIconQuery(String name) {
    // Match query for the given name
    return MatchQuery.of(m -> m.field("name").query(name))._toQuery();
  }

  public static Aggregation buildResourceAccessPolicyCountQuery() {
    // Build the accessPolicy_count aggregation
    Aggregation accessPolicyCountAgg =
        Aggregation.of(
            a -> a.valueCount(ValueCountAggregation.of(v -> v.field("accessPolicy.keyword"))));

    // Build the access_policies aggregation
    Aggregation accessPoliciesAgg =
        Aggregation.of(
            a ->
                a.terms(TermsAggregation.of(t -> t.field("accessPolicy.keyword").size(10000)))
                    .aggregations("accessPolicy_count", accessPolicyCountAgg));

    Aggregation resourceCountAgg =
        Aggregation.of(a -> a.valueCount(ValueCountAggregation.of(v -> v.field("id.keyword"))));

    // Build the results aggregation
    Aggregation resultsAgg =
        Aggregation.of(
            a ->
                a.terms(TermsAggregation.of(t -> t.field("resourceGroup.keyword").size(10000)))
                    .aggregations(
                        Map.of(
                            "access_policies", accessPoliciesAgg,
                            "resource_count", resourceCountAgg)));
    return resultsAgg;
  }

  public static Aggregation buildAvgRatingAggregation() {
    // Constructing the average rating aggregation
    Aggregation avgAggregation =
        Aggregation.of(a -> a.avg(AverageAggregation.of(avg -> avg.field("rating"))));

    // Constructing the terms aggregation
    Aggregation termsAggregation =
        Aggregation.of(
            t ->
                t.terms(TermsAggregation.of(term -> term.field(ID_KEYWORD)))
                    .aggregations("average_rating", avgAggregation));

    return termsAggregation;
  }

  public static Query buildCheckMdocQuery(String id) {
    MatchQuery matchQuery = MatchQuery.of(m -> m.field("id.keyword").query(id));

    return BoolQuery.of(b -> b.must(Query.of(q -> q.match(matchQuery))))._toQuery();
  }

  public static Query buildAllMlayerDomainsQuery() {
    return MatchAllQuery.of(m -> m)._toQuery();
  }

  public static Query buildMlayerDomainQuery(String domainId) {
    return MatchQuery.of(m -> m.field("domainId" + KEYWORD_KEY).query(domainId))._toQuery();
  }

  public static Query buildMdocDomainQuery(String domainId) {
    Query matchQuery =
        MatchQuery.of(m -> m.field("domainId" + KEYWORD_KEY).query(domainId))._toQuery();
    return BoolQuery.of(b -> b.must(matchQuery))._toQuery();
  }

  public static Query buildMlayerBoolGeoQuery(String instance, String datasetId) {
    Query shouldQuery1 =
        MatchQuery.of(m -> m.field(TYPE_KEYWORD).query("iudx:Resource"))._toQuery();
    Query shouldQuery2 =
        MatchQuery.of(m -> m.field(TYPE_KEYWORD).query("iudx:ResourceGroup"))._toQuery();
    Query mustQuery1 =
        MatchQuery.of(m -> m.field("instance" + KEYWORD_KEY).query(instance))._toQuery();

    Query mustQuery2 = MatchQuery.of(m -> m.field(ID_KEYWORD).query(datasetId))._toQuery();

    return BoolQuery.of(b -> b.should(shouldQuery1, shouldQuery2).must(mustQuery1, mustQuery2))
        ._toQuery();
  }

  public static Query buildMlayerGeoQuery(List<Query> boolGeoQueries) {
    return BoolQuery.of(b -> b.minimumShouldMatch("1").should(boolGeoQueries))._toQuery();
  }

  public static Query buildAllMlayerInstancesQuery() {
    return MatchAllQuery.of(m -> m)._toQuery();
  }

  public static Query buildMlayerInstanceQuery(String instanceId) {
    return MatchQuery.of(m -> m.field("instanceId" + KEYWORD_KEY).query(instanceId))._toQuery();
  }

  public static Query buildMdocInstanceQuery(String instanceId) {
    Query matchQuery =
        MatchQuery.of(m -> m.field("instanceId" + KEYWORD_KEY).query(instanceId))._toQuery();

    return BoolQuery.of(b -> b.must(matchQuery))._toQuery();
  }

  public static Query buildMatchAllQuery() {
    return MatchAllQuery.of(m -> m)._toQuery();
  }

  public static Query buildGetProviderNdResourceGroupQuery() {
    Query resourceGroupQuery =
        BoolQuery.of(
                b ->
                    b.must(
                        List.of(
                            TermQuery.of(q -> q.field(TYPE_KEYWORD).value("iudx:ResourceGroup"))
                                ._toQuery())))
            ._toQuery();
    Query providerQuery =
        BoolQuery.of(
                b ->
                    b.must(
                        List.of(
                            TermQuery.of(q -> q.field(TYPE_KEYWORD).value("iudx:Provider"))
                                ._toQuery())))
            ._toQuery();

    return BoolQuery.of(b -> b.should(List.of(resourceGroupQuery, providerQuery)))._toQuery();
  }

  public static Query buildgetDatasetByInstanceQuery(String instanceId) {
    Query resourceGroupQuery =
        BoolQuery.of(
                b ->
                    b.must(
                        List.of(
                            MatchQuery.of(q -> q.field(TYPE_KEYWORD).query("iudx:ResourceGroup"))
                                ._toQuery(),
                            MatchQuery.of(q -> q.field("instance" + KEYWORD_KEY).query(instanceId))
                                ._toQuery())))
            ._toQuery();
    Query providerQuery =
        BoolQuery.of(
                b ->
                    b.must(
                        List.of(
                            MatchQuery.of(q -> q.field(TYPE_KEYWORD).query("iudx:Provider"))
                                ._toQuery())))
            ._toQuery();

    return BoolQuery.of(b -> b.should(List.of(resourceGroupQuery, providerQuery)))._toQuery();
  }

  public static Query buildMlayerProvidersQuery() {
    return MatchQuery.of(q -> q.field(TYPE_KEYWORD).query("iudx:Provider"))._toQuery();
  }

  public static Query buildRDocQuery(String ratingId) {
    MatchQuery ratingIdMatch = MatchQuery.of(m -> m.field("ratingID.keyword").query(ratingId));
    MatchQuery statusMatchNot = MatchQuery.of(m -> m.field("status").query("denied"));

    BoolQuery boolQuery =
        BoolQuery.of(b -> b.must(ratingIdMatch._toQuery()).mustNot(statusMatchNot._toQuery()));

    return Query.of(q -> q.bool(boolQuery));
  }

  public static Query buildAllDatasetsByRsGrpQuery() {
    MatchQuery typeMatch = MatchQuery.of(m -> m.field("type.keyword").query("iudx:ResourceGroup"));
    BoolQuery innerBoolQuery = BoolQuery.of(b -> b.should(typeMatch._toQuery()));
    BoolQuery outerBoolQuery = BoolQuery.of(b -> b.must(innerBoolQuery._toQuery()));

    return Query.of(q -> q.bool(outerBoolQuery));
  }

  public static Query buildDocQueryWithType(String id, String type) {
    TermQuery idTermQuery = TermQuery.of(t -> t.field(ID_KEYWORD).value(id));
    MatchQuery typeMatchQuery = MatchQuery.of(m -> m.field(TYPE_KEYWORD).query(type));

    BoolQuery boolQuery =
        BoolQuery.of(b -> b.must(List.of(idTermQuery._toQuery(), typeMatchQuery._toQuery())));
    return Query.of(q -> q.bool(boolQuery));
  }

  public static Query buildGetDocQuery(String id) {
    return TermQuery.of(t -> t.field(ID_KEYWORD).value(id))._toQuery();
  }

  public static Query buildInstanceFilter(String instanceId) {
    return TermQuery.of(t -> t.field(INSTANCE).value(instanceId))._toQuery();
  }

  public static Query buildTermQuery(String type, String value) {
    return QueryBuilders.term(t -> t.field(type).value(value));
  }

  public static Query buildMatchQuery(String field, String query) {
    return QueryBuilders.match(m -> m.field(field).query(query));
  }

  public static Query buildResourceGroupQuery(String id) {
    TermQuery idTermQuery = TermQuery.of(t -> t.field("id.keyword").value(id));

    TermQuery resourceGroupTermQuery =
        TermQuery.of(t -> t.field("resourceGroup.keyword").value(id));

    TermQuery providerTermQuery = TermQuery.of(t -> t.field("provider.keyword").value(id));

    TermQuery resourceServerTermQuery =
        TermQuery.of(t -> t.field("resourceServer.keyword").value(id));

    TermQuery cosTermQuery = TermQuery.of(t -> t.field("cos.keyword").value(id));

    BoolQuery boolQuery =
        BoolQuery.of(
            b ->
                b.should(
                    List.of(
                        idTermQuery._toQuery(),
                        resourceGroupTermQuery._toQuery(),
                        providerTermQuery._toQuery(),
                        resourceServerTermQuery._toQuery(),
                        cosTermQuery._toQuery())));

    return Query.of(q -> q.bool(boolQuery));
  }

  public static Query buildGetRatingDocsQuery(String key, String id) {
    return QueryBuilders.bool(
        b ->
            b.must(
                List.of(
                    QueryBuilders.match(m -> m.field(key).query(id)),
                    QueryBuilders.match(m -> m.field("status").query("approved")))));
  }

  public static Query buildItemExistsQuery(String id, String type, String field, String value) {
    // Construct must query for id.keyword
    Query idMatchQuery = MatchQuery.of(m -> m.field(ID_KEYWORD).query(id))._toQuery();

    // Construct must query for type.keyword
    Query typeMatchQuery = MatchQuery.of(m -> m.field(TYPE_KEYWORD).query(type))._toQuery();

    // Construct must query for the field.keyword
    Query fieldMatchQuery =
        MatchQuery.of(m -> m.field(field + KEYWORD_KEY).query(value))._toQuery();

    // Combine the idMatchQuery in a must clause within a bool query
    BoolQuery boolIdMatchBuilder = BoolQuery.of(b -> b.must(idMatchQuery));

    // Combine the typeMatchQuery and fieldMatchQuery in a must clause within a bool query
    BoolQuery boolTypeFieldMatchBuilder =
        BoolQuery.of(b -> b.must(typeMatchQuery).must(fieldMatchQuery));

    // Combine the two bool queries in a should clause within the main bool query
    BoolQuery mainBoolQueryBuilder =
        BoolQuery.of(
            b ->
                b.should(boolIdMatchBuilder._toQuery())
                    .should(boolTypeFieldMatchBuilder._toQuery()));

    return Query.of(q -> q.bool(mainBoolQueryBuilder));
  }

  public static Query buildProviderItemExistsQuery(
      String id, String ownerId, String resourceServerUrl) {
    // Construct must query for id.keyword
    Query idMatchQuery = MatchQuery.of(m -> m.field(ID_KEYWORD).query(id))._toQuery();

    // Construct must query for type.keyword
    Query typeMatchQuery =
        MatchQuery.of(m -> m.field("ownerUserId" + KEYWORD_KEY).query(ownerId))._toQuery();

    // Construct must query for the field.keyword
    Query fieldMatchQuery =
        MatchQuery.of(m -> m.field("resourceServerRegURL" + KEYWORD_KEY).query(resourceServerUrl))
            ._toQuery();

    // Combine idMatchQuery in a must clause within a bool query
    BoolQuery boolIdMatchBuilder = BoolQuery.of(b -> b.must(idMatchQuery));

    // Combine typeMatchQuery and fieldMatchQuery in a must clause within a bool query
    BoolQuery boolTypeFieldMatchBuilder =
        BoolQuery.of(b -> b.must(typeMatchQuery).must(fieldMatchQuery));

    // Combine the two bool queries in a should clause within the main bool query
    BoolQuery mainBoolQueryBuilder =
        BoolQuery.of(
            b ->
                b.should(boolIdMatchBuilder._toQuery())
                    .should(boolTypeFieldMatchBuilder._toQuery()));

    return Query.of(q -> q.bool(mainBoolQueryBuilder));
  }

  public static Query buildResourceItemExistsQuery(
      String id1, String id2, String id3, String name) {
    // Construct the match queries for id.keyword
    Query idMatchQuery1 = MatchQuery.of(m -> m.field(ID_KEYWORD).query(id1))._toQuery();

    Query idMatchQuery2 = MatchQuery.of(m -> m.field(ID_KEYWORD).query(id2))._toQuery();

    Query idMatchQuery3 = MatchQuery.of(m -> m.field(ID_KEYWORD).query(id3))._toQuery();

    // Construct the must queries for type.keyword and name.keyword
    Query typeMatchQuery =
        MatchQuery.of(m -> m.field(TYPE_KEYWORD).query("iudx:Resource"))._toQuery();

    Query nameMatchQuery = MatchQuery.of(m -> m.field("name" + KEYWORD_KEY).query(name))._toQuery();

    // Combine typeMatchQuery and nameMatchQuery in a must clause within a bool query
    BoolQuery typeNameBoolQueryBuilder =
        BoolQuery.of(b -> b.must(typeMatchQuery).must(nameMatchQuery));

    // Combine all match queries and the typeNameBoolQuery in a should clause within the main bool
    // query
    BoolQuery mainBoolQueryBuilder =
        BoolQuery.of(
            b ->
                b.should(idMatchQuery1)
                    .should(idMatchQuery2)
                    .should(idMatchQuery3)
                    .should(typeNameBoolQueryBuilder._toQuery()));

    return Query.of(q -> q.bool(mainBoolQueryBuilder));
  }

  public static Query buildOwnerItemExistsQuery(String name) {
    Query typeMatchQuery = MatchQuery.of(m -> m.field("type").query("iudx:Owner"))._toQuery();

    Query nameMatchQuery = MatchQuery.of(m -> m.field("name" + KEYWORD_KEY).query(name))._toQuery();

    // Combine typeMatchQuery and nameMatchQuery in a must clause within a bool query
    BoolQuery boolQueryBuilder = BoolQuery.of(b -> b.must(typeMatchQuery).must(nameMatchQuery));
    return Query.of(q -> q.bool(boolQueryBuilder));
  }

  public static Query buildGetStackQuery(String id) {
    Query idMatchQuery = MatchQuery.of(m -> m.field(ID_KEYWORD).query(id))._toQuery();

    // Combine idMatchQuery in a must clause within a bool query
    BoolQuery boolQueryBuilder = BoolQuery.of(b -> b.must(idMatchQuery));
    return Query.of(q -> q.bool(boolQueryBuilder));
  }

  public static Query buildCheckStackExistenceQuery(String href1, String href2) {
    // First bool query (rel: "self", href: href1)
    BoolQuery firstBoolQuery =
        BoolQuery.of(
            b ->
                b.must(
                    Arrays.asList(
                        MatchQuery.of(m -> m.field("links.rel.keyword").query("self"))._toQuery(),
                        MatchQuery.of(m -> m.field("links.href.keyword").query(href1))
                            ._toQuery())));

    // Second bool query (rel: "root", href: href2)
    BoolQuery secondBoolQuery =
        BoolQuery.of(
            b ->
                b.must(
                    Arrays.asList(
                        MatchQuery.of(m -> m.field("links.rel.keyword").query("root"))._toQuery(),
                        MatchQuery.of(m -> m.field("links.href.keyword").query(href2))
                            ._toQuery())));

    // Main bool query with should clauses
    BoolQuery mainBoolQuery =
        BoolQuery.of(
            b -> b.should(Arrays.asList(firstBoolQuery._toQuery(), secondBoolQuery._toQuery())));

    return mainBoolQuery._toQuery();
  }

  public static Query buildGetRsGroupQuery(String id) {
    FieldValue field = FieldValue.of(id);
    TermsQueryField termQueryField = TermsQueryField.of(e -> e.value(List.of(field)));
    Query termQuery = QueryBuilders.terms(t -> t.field(ID_KEYWORD).terms(termQueryField));
    return QueryBuilders.bool(b -> b.filter(termQuery));
  }

  public static Query buildTypeQuery4RsGroup(String id) {
    Query matchQuery = buildMatchQuery("resourceServer" + KEYWORD_KEY, id);
    Query termQuery = buildTermQuery(TYPE_KEYWORD, "iudx:Provider");
    return QueryBuilders.bool(b -> b.must(matchQuery).must(termQuery));
  }

  public static Query buildTypeQuery4RsServer(String id) {
    Query termsQuery =
        QueryBuilders.terms(
            t ->
                t.field(ID_KEYWORD)
                    .terms(
                        TermsQueryField.of(
                            tf -> tf.value(Collections.singletonList(FieldValue.of(id)))))
                    .boost(1.0F));
    Query boolQuery = QueryBuilders.bool(b -> b.filter(termsQuery));

    return QueryBuilders.bool(b -> b.filter(boolQuery));
  }

  public static Query getAssociatedIdQuery(String id, String rsGrp) {
    Query matchQueryId = QueryBuilders.match(m -> m.field(ID_KEYWORD).query(id));

    Query matchQueryResourceGroup =
        QueryBuilders.match(m -> m.field("resourceGroup" + KEYWORD_KEY).query(rsGrp));

    // Construct the bool query with should clause
    return QueryBuilders.bool(
        b ->
            b.should(List.of(matchQueryId, matchQueryResourceGroup))
                .minimumShouldMatch(String.valueOf(1)));
  }

  public static Query buildGetProviderAndRsIdQuery(String id) {
    /* Building Provider and RS id Query */
    Query typeQuery =
        MatchQuery.of(m -> m.field(TYPE_KEYWORD).query("iudx:ResourceGroup"))._toQuery();

    Query idQuery = MatchQuery.of(m -> m.field(ID_KEYWORD).query(id))._toQuery();

    // Combine the match queries in a BoolQuery
    BoolQuery boolQuery = BoolQuery.of(b -> b.must(typeQuery, idQuery));

    return Query.of(q -> q.bool(boolQuery));
  }

  public static SourceConfig buildSourceConfig(List<String> includes) {
    SourceFilter sourceFilter = SourceFilter.of(f -> f.includes(includes));
    return SourceConfig.of(s -> s.filter(sourceFilter));
  }

  public static SortOptions getSortOptions(String field) {
    return SortOptions.of(s -> s.field(f -> f.field(field).order(SortOrder.Asc)));
  }

  public static Aggregation providerCountAgg(String field) {
    return AggregationBuilders.cardinality().field(field).build()._toAggregation();
  }

  public static Aggregation buildTermsAggs(String field, int size) {
    return Aggregation.of(a -> a.terms(TermsAggregation.of(t -> t.field(field).size(size))));
  }

  public static Query buildRelSubQuery(Query termQuery, String itemType) {
    return QueryBuilders.bool(b -> b.must(termQuery).must(buildTermQuery(TYPE_KEYWORD, itemType)));
  }

  public static Query buildNestedMustQuery(String type, String id, String itemType) {
    return QueryBuilders.bool(
        b ->
            b.must(buildTermQuery(type + KEYWORD_KEY, id))
                .must(buildTermQuery(TYPE_KEYWORD, itemType)));
  }

  public static Query getScriptScoreQuery(Map<String, JsonData> params) {
    Script script =
        Script.of(
            s ->
                s.inline(
                    inline ->
                        inline
                            .source(
                                "doc['_word_vector'].size() == 0 ? 0 : cosineSimilarity(params.query_vector, '_word_vector') + 1.0")
                            .lang("painless")
                            .params(params)));
    ScriptScoreQuery scriptScoreQuery =
        new ScriptScoreQuery.Builder()
            .query(MatchAllQuery.of(m -> m)._toQuery())
            .script(script)
            .build();
    return Query.of(q -> q.scriptScore(scriptScoreQuery));
  }

  public static Query getScriptLocationSearchQuery(
      JsonObject queryParams, Map<String, JsonData> params) {
    List<Query> shouldQueries = new ArrayList<>();

    if (queryParams.containsKey(BOROUGH)) {
      shouldQueries.add(
          MatchQuery.of(
                  m ->
                      m.field("_geosummary._geocoded.results.borough")
                          .query(queryParams.getString(BOROUGH)))
              ._toQuery());
    }
    if (queryParams.containsKey(LOCALITY)) {
      shouldQueries.add(
          MatchQuery.of(
                  m ->
                      m.field("_geosummary._geocoded.results.locality")
                          .query(queryParams.getString(LOCALITY)))
              ._toQuery());
    }
    if (queryParams.containsKey(COUNTY)) {
      shouldQueries.add(
          MatchQuery.of(
                  m ->
                      m.field("_geosummary._geocoded.results.county")
                          .query(queryParams.getString(COUNTY)))
              ._toQuery());
    }
    if (queryParams.containsKey(REGION)) {
      shouldQueries.add(
          MatchQuery.of(
                  m ->
                      m.field("_geosummary._geocoded.results.region")
                          .query(queryParams.getString(REGION)))
              ._toQuery());
    }
    if (queryParams.containsKey(COUNTRY)) {
      shouldQueries.add(
          MatchQuery.of(
                  m ->
                      m.field("_geosummary._geocoded.results.country")
                          .query(queryParams.getString(COUNTRY)))
              ._toQuery());
    }
    JsonArray bboxCoords = queryParams.getJsonArray(BBOX);
    JsonObject geoJson = new JsonObject();
    geoJson.put("type", "envelope");
    geoJson.put(
        "coordinates",
        List.of(
            List.of(bboxCoords.getFloat(0), bboxCoords.getFloat(3)),
            List.of(bboxCoords.getFloat(2), bboxCoords.getFloat(1))));

    GeoShapeFieldQuery geoShapeFieldQuery =
        new GeoShapeFieldQuery.Builder()
            .shape(JsonData.fromJson(geoJson.toString()))
            .relation(GeoShapeRelation.Intersects)
            .build();
    Query geoShapeQuery =
        QueryBuilders.geoShape(g -> g.field("location" + GEO_KEY).shape(geoShapeFieldQuery));
    BoolQuery boolQuery =
        BoolQuery.of(b -> b.should(shouldQueries).minimumShouldMatch("1").filter(geoShapeQuery));
    // Construct the script
    Script script =
        new Script.Builder()
            .inline(
                inline ->
                    inline
                        .source(
                            "doc['_word_vector'].size() == 0 ? 0 : cosineSimilarity(params.query_vector, '_word_vector') + 1.0")
                        .params(params))
            .build();
    // Construct the script score query
    ScriptScoreQuery scriptScoreQuery =
        new ScriptScoreQuery.Builder().query(boolQuery._toQuery()).script(script).build();

    // Wrap the script score query
    return new Query.Builder().scriptScore(scriptScoreQuery).build();
  }
}
