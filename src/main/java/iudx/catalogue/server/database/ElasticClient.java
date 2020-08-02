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

import java.io.IOException;

import static iudx.catalogue.server.database.Constants.*;

public final class ElasticClient {
  private final RestClient client;

  public ElasticClient(String databaseIP, int databasePort) {
    client = RestClient.builder(new HttpHost(databaseIP, databasePort, "http")).build();
  }

  public ElasticClient performRequestAsync(Request request,
      Handler<AsyncResult<JsonObject>> resultHandler) {

    Future<JsonObject> future = performRequestAsync(request);
    future.onComplete(resultHandler);
    return this;
  }

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

    DBRespMsgBuilder addResult(JsonObject obj) {
      response.getJsonArray(RESULTS).add(obj);
      return this;
    }

    JsonObject getResponse() {
      return response;
    }
  }

  private Future<JsonObject> performRequestAsync(Request request) {
    Promise<JsonObject> promise = Promise.promise();

    DBRespMsgBuilder responseMsg = new DBRespMsgBuilder();

    client.performRequestAsync(request, new ResponseListener() {
      @Override
      public void onSuccess(Response response) {

        try {
          int statusCode = response.getStatusLine().getStatusCode();
          if (statusCode != 200 && statusCode != 204) {
            promise.fail(DATABASE_BAD_QUERY);
          }
          JsonObject responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));
          responseMsg.statusSuccess()
                      .setTotalHits(responseJson.getJsonObject(HITS)
                                                .getJsonObject(TOTAL)
                                                .getInteger(VALUE));
          JsonArray hits = responseJson.getJsonObject(HITS).getJsonArray(HITS);
          for (int i=0; i<hits.size(); i++) {
            responseMsg.addResult(hits.getJsonObject(i).getJsonObject(SOURCE));
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




}
