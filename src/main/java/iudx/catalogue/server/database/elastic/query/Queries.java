package iudx.catalogue.server.database.elastic.query;

import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.util.Constants.*;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.AverageAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.ValueCountAggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import co.elastic.clients.elasticsearch.core.search.SourceFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Queries {
  public static Query buildMlayerDatasetQuery(String id1, String id2, String id3) {
    // Match queries
    Query matchId1 = MatchQuery.of(m -> m.field(ID_KEYWORD).query(id1))._toQuery();

    Query matchTypeResourceGroup =
        MatchQuery.of(m -> m.field(TYPE_KEYWORD).query("iudx:ResourceGroup"))._toQuery();

    Query matchId2 = MatchQuery.of(m -> m.field(ID_KEYWORD).query(id2))._toQuery();

    Query matchTypeProvider =
        MatchQuery.of(m -> m.field(TYPE_KEYWORD).query("iudx:Provider"))._toQuery();

    Query matchResourceGroup =
        MatchQuery.of(m -> m.field("resourceGroup" + KEYWORD_KEY).query(id1))._toQuery();

    Query matchTypeResource =
        MatchQuery.of(m -> m.field(TYPE_KEYWORD).query("iudx:Resource"))._toQuery();

    Query matchId3 = MatchQuery.of(m -> m.field(ID_KEYWORD).query(id3))._toQuery();

    Query matchTypeCOS = MatchQuery.of(m -> m.field(TYPE_KEYWORD).query("iudx:COS"))._toQuery();

    // Bool queries with must clauses
    Query boolQuery1 = BoolQuery.of(b -> b.must(matchId1, matchTypeResourceGroup))._toQuery();

    Query boolQuery2 = BoolQuery.of(b -> b.must(matchId2, matchTypeProvider))._toQuery();

    Query boolQuery3 = BoolQuery.of(b -> b.must(matchResourceGroup, matchTypeResource))._toQuery();

    Query boolQuery4 = BoolQuery.of(b -> b.must(matchId3, matchTypeCOS))._toQuery();

    return BoolQuery.of(b -> b.should(boolQuery1, boolQuery2, boolQuery3, boolQuery4))._toQuery();
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

  public static Query getMlayerAllDatasetsQuery() {
    return TermsQuery.of(
            f ->
                f.field(TYPE_KEYWORD)
                    .terms(
                        (TermsQueryField)
                            List.of(
                                "iudx:Provider",
                                "iudx:COS",
                                "iudx:ResourceGroup",
                                "iudx:Resource")))
        ._toQuery();
  }

  public static Query buildGetAllDatasetsByFieldsQuery() {
    Query resourceGroupQuery =
        BoolQuery.of(
                b ->
                    b.must(
                        List.of(
                            MatchQuery.of(q -> q.field(TYPE_KEYWORD).query("iudx:ResourceGroup"))
                                ._toQuery())))
            ._toQuery();
    return BoolQuery.of(b -> b.should(List.of(resourceGroupQuery)))._toQuery();
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

  public static Query buildGetRSGroupQuery(String id) {
    FieldValue field = FieldValue.of(id);
    TermsQueryField termQueryField = TermsQueryField.of(e -> e.value(List.of(field)));
    Query termQuery = QueryBuilders.terms(t -> t.field(ID_KEYWORD).terms(termQueryField));
    return QueryBuilders.bool(b -> b.filter((termQuery)));
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
}
