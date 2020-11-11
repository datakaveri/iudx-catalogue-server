package iudx.catalogue.server.database;

import org.apache.http.HttpHost;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Handler;
import io.vertx.core.AsyncResult;
import org.apache.http.util.EntityUtils;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.auth.AuthScope;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.IOException;

import static iudx.catalogue.server.database.Constants.*;
import static iudx.catalogue.server.util.Constants.*;

public final class ElasticClient {
  private final RestClient client;
  private static final Logger LOGGER = LogManager.getLogger(ElasticClient.class);

  /**
   * ElasticClient - Wrapper around ElasticSearch low level client
   * 
   * @param databaseIP IP of the DB
   * @param databasePort Port
   * @TODO XPack Security
   */
  public ElasticClient(String databaseIP, int databasePort,
                        String databaseUser, String databasePassword) {
    CredentialsProvider credentials = new BasicCredentialsProvider();
    credentials.setCredentials(AuthScope.ANY,
                                new UsernamePasswordCredentials(databaseUser, databasePassword));
    client = RestClient.builder(new HttpHost(databaseIP, databasePort)).setHttpClientConfigCallback(
        httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentials)).build();
  }

  /**
   * searchAsync - Wrapper around elasticsearch async search requests
   * 
   * @param index Index to search on
   * @param query Query
   * @param resultHandler JsonObject result {@link AsyncResult}
   * @TODO XPack Security
   */
  public ElasticClient searchAsync(String index, String query,
      Handler<AsyncResult<JsonObject>> resultHandler) {

    Request queryRequest = new Request(REQUEST_GET, index + "/_search" + FILTER_PATH);
    queryRequest.setJsonEntity(query);
    Future<JsonObject> future = searchAsync(queryRequest, SOURCE_ONLY);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * searchGetIdAsync - Get document IDs matching a query
   * 
   * @param index Index to search on
   * @param query Query
   * @param resultHandler JsonObject result {@link AsyncResult}
   * @TODO XPack Security
   */
  public ElasticClient searchGetId(String index, String query,
      Handler<AsyncResult<JsonObject>> resultHandler) {

    Request queryRequest = new Request(REQUEST_GET, index + "/_search" + FILTER_ID_ONLY_PATH);
    queryRequest.setJsonEntity(query);
    Future<JsonObject> future = searchAsync(queryRequest, DOC_IDS_ONLY);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * aggregationsAsync - Wrapper around elasticsearch async search requests
   * 
   * @param index Index to search on
   * @param query Query
   * @param resultHandler JsonObject result {@link AsyncResult}
   * @TODO XPack Security
   */
  public ElasticClient listAggregationAsync(String index, String query,
      Handler<AsyncResult<JsonObject>> resultHandler) {

    Request queryRequest = new Request(REQUEST_GET, index 
                              + "/_search"
                              + FILTER_PATH_AGGREGATION);
    queryRequest.setJsonEntity(query);
    Future<JsonObject> future = searchAsync(queryRequest, AGGREGATION_ONLY);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * countAsync - Wrapper around elasticsearch async count requests
   * 
   * @param index Index to search on
   * @param query Query
   * @param resultHandler JsonObject result {@link AsyncResult}
   * @TODO XPack Security
   */
  public ElasticClient countAsync(String index, String query,
      Handler<AsyncResult<JsonObject>> resultHandler) {

    Request queryRequest = new Request(REQUEST_GET, index + "/_count");
    queryRequest.setJsonEntity(query);
    Future<JsonObject> future = countAsync(queryRequest);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * docPostAsync - Wrapper around elasticsearch async doc post request
   * 
   * @param index Index to search on
   * @param doc Document
   * @param resultHandler JsonObject
   * @TODO XPack Security
   */
  public ElasticClient docPostAsync(String index, String doc,
      Handler<AsyncResult<JsonObject>> resultHandler) {

    /** TODO: Validation */
    Request docRequest = new Request(REQUEST_POST, index + "/_doc");
    docRequest.setJsonEntity(doc.toString());

    Future<JsonObject> future = docAsync(REQUEST_POST, docRequest);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * docPutAsync - Wrapper around elasticsearch async doc put request
   * 
   * @param index Index to search on
   * @param docId Document id (elastic id)
   * @param doc Document
   * @param resultHandler JsonObject
   * @TODO XPack Security
   */
  public ElasticClient docPutAsync(String index, String docId, String doc,
      Handler<AsyncResult<JsonObject>> resultHandler) {

    /** TODO: Validation */
    Request docRequest = new Request(REQUEST_PUT, index + "/_doc/" + docId);
    docRequest.setJsonEntity(doc.toString());

    Future<JsonObject> future = docAsync(REQUEST_PUT, docRequest);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * docDelAsync - Wrapper around elasticsearch async doc delete request
   * 
   * @param index Index to search on
   * @param doc Document
   * @param resultHandler JsonObject
   * @TODO XPack Security
   */
  public ElasticClient docDelAsync(String index, String docId, 
      Handler<AsyncResult<JsonObject>> resultHandler) {

    /** TODO: Validation */
    Request docRequest = new Request(REQUEST_DELETE, index + "/_doc/" + docId);

    Future<JsonObject> future = docAsync(REQUEST_DELETE, docRequest);
    future.onComplete(resultHandler);
    return this;
  }

  /**
   * DBRespMsgBuilder} Message builder for search APIs
   */
  private class DBRespMsgBuilder {
    private JsonObject response = new JsonObject(); 
    private JsonArray results = new JsonArray();

    DBRespMsgBuilder() {
      response.put(RESULTS, results);
    }

    DBRespMsgBuilder statusSuccess() {
      response.put(STATUS, SUCCESS);
      return this;
    }

    DBRespMsgBuilder setTotalHits(int hits) {
      response.put(TOTAL_HITS, hits);
      return this;
    }

    /** Overloaded for source only request */
    DBRespMsgBuilder addResult(JsonObject obj) {
      response.getJsonArray(RESULTS).add(obj);
      return this;
    }

    /** Overloaded for doc-ids request */
    DBRespMsgBuilder addResult(String value) {
      response.getJsonArray(RESULTS).add(value);
      return this;
    }

    JsonObject getResponse() {
      return response;
    }
  }

  /**
   * searchAsync - private function which perform performRequestAsync for search apis
   * 
   * @param request Elastic Request
   * @param options SOURCE - Source only
   *                DOCIDS - DOCIDs only
   *                IDS - IDs only
   * @TODO XPack Security
   */
  private Future<JsonObject> searchAsync(Request request, String options) {
    Promise<JsonObject> promise = Promise.promise();

    DBRespMsgBuilder responseMsg = new DBRespMsgBuilder();

    client.performRequestAsync(request, new ResponseListener() {
      @Override
      public void onSuccess(Response response) {

        try {
          int statusCode = response.getStatusLine().getStatusCode();
          if (statusCode != 200 && statusCode != 204) {
            promise.fail(DATABASE_BAD_QUERY);
            return;
          }
          JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));
          int totalHits = responseJson.getJsonObject(HITS)
                                                .getJsonObject(TOTAL)
                                                .getInteger(VALUE);
          responseMsg.statusSuccess()
                      .setTotalHits(totalHits);
          if (totalHits > 0 ) {
            JsonArray results = new JsonArray();

            if ((options == SOURCE_ONLY) || (options == DOC_IDS_ONLY)) {
              results = responseJson.getJsonObject(HITS).getJsonArray(HITS);
            }
            if (options == AGGREGATION_ONLY) {
              results = responseJson.getJsonObject(AGGREGATIONS)
                                  .getJsonObject(RESULTS)
                                  .getJsonArray(BUCKETS);
            }
            for (int i=0; i<results.size(); i++) {
              if (options == SOURCE_ONLY) {
                results.getJsonObject(i).getJsonObject(SOURCE).remove(SUMMARY_KEY);
                responseMsg.addResult(results.getJsonObject(i).getJsonObject(SOURCE));
              }
              if (options == DOC_IDS_ONLY) {
                responseMsg.addResult(results.getJsonObject(i).getString(DOC_ID));
              }
              if (options == AGGREGATION_ONLY) {
                responseMsg.addResult(results.getJsonObject(i).getString(KEY));
              }
            }
          }
          promise.complete(responseMsg.getResponse());

        } catch (IOException e) {
            promise.fail(e);
        } finally {
        }
      }
      @Override
      public void onFailure(Exception e) {
        promise.fail(e);
      }
    });
    return promise.future();
  }

  /**
   * countAsync - private function which perform performRequestAsync for count apis
   * 
   * @param request Elastic Request
   * @param options SOURCE - Source only
   *                DOCIDS - DOCIDs only
   *                IDS - IDs only
   * @TODO XPack Security
   * @TODO Can combine countAsync and searchAsync
   */
  private Future<JsonObject> countAsync(Request request) {
    Promise<JsonObject> promise = Promise.promise();

    DBRespMsgBuilder responseMsg = new DBRespMsgBuilder();

    client.performRequestAsync(request, new ResponseListener() {
      @Override
      public void onSuccess(Response response) {

        try {
          int statusCode = response.getStatusLine().getStatusCode();
          if (statusCode != 200 && statusCode != 204) {
            promise.fail(DATABASE_BAD_QUERY);
            return;
          }
          JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));
          responseMsg.statusSuccess()
                      .setTotalHits(responseJson.getInteger(COUNT));
          promise.complete(responseMsg.getResponse());

        } catch (IOException e) {
            promise.fail(e);
        } finally {
        }
      }
      @Override
      public void onFailure(Exception e) {
        promise.fail(e);
      }
    });
    return promise.future();
  }


  /**
   * docAsync - private function which perform performRequestAsync for doc apis
   * 
   * @param request Elastic Request
   * @param options SOURCE - Source only
   *                DOCIDS - DOCIDs only
   *                IDS - IDs only
   * @TODO XPack Security
   * @TODO Can combine countAsync and searchAsync
   */
  private Future<JsonObject> docAsync(String method, Request request) {
    Promise<JsonObject> promise = Promise.promise();

    client.performRequestAsync(request, new ResponseListener() {
      @Override
      public void onSuccess(Response response) {
        try {
          JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));
          int statusCode = response.getStatusLine().getStatusCode();
          switch (method) {
            case REQUEST_POST:
              if (statusCode == 201) {
                promise.complete(responseJson);
                return;
              }
            case REQUEST_DELETE:
              if (statusCode == 200) {
                promise.complete(responseJson);
                return;
              }
            case REQUEST_PUT:
              if (statusCode == 200) {
                promise.complete(responseJson);
                return;
              }
            default:
              promise.fail(DATABASE_BAD_QUERY);
          }
          promise.fail("Failed request");
        } catch (IOException e) {
            promise.fail(e);
        } finally {
        }
      }
      @Override
      public void onFailure(Exception e) {
        promise.fail(e);
      }
    });
    return promise.future();
  }


}
