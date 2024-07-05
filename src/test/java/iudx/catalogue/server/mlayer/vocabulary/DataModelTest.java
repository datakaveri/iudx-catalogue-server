package iudx.catalogue.server.mlayer.vocabulary;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import iudx.catalogue.server.database.elastic.ElasticClient;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DataModelTest {
  private static final Logger LOGGER = LogManager.getLogger(DataModelTest.class);
  MultiMap mockHeaders = mock(MultiMap.class);
  @Mock private ElasticClient mockElasticClient;
  @Mock private WebClient mockWebClient;
  @Mock private HttpResponse<Buffer> mockHttpResponse;
  @Mock private HttpRequest<Buffer> mockHttpRequest;

  @Mock private Buffer mockBuffer;

  @InjectMocks private DataModel dataModel;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    dataModel = new DataModel(mockElasticClient, "test-index");
  }

  @Test
  void testGetDataModelInfo_Success() {
    // Mocking Elasticsearch client's async search
    when(mockElasticClient.searchAsync(
            any(Query.class), any(), anyInt(), anyInt(), anyString(), any()))
        .thenAnswer(
            invocation -> {
              ((Handler<AsyncResult<JsonObject>>) invocation.getArgument(5))
                  .handle(
                      Future.succeededFuture(
                          new JsonObject()
                              .put(
                                  "results",
                                  new JsonArray()
                                      .add(
                                          new JsonObject()
                                              .put("@context", "https://example.com/")
                                              .put(
                                                  "type",
                                                  new JsonArray()
                                                      .add("dummy")
                                                      .add("iudx:Resource"))))));
              return null;
            });
    lenient().when(mockWebClient.getAbs(anyString())).thenReturn(mockHttpRequest);
    lenient().when(mockHttpRequest.send()).thenReturn(Future.succeededFuture(mockHttpResponse));
    lenient().when(mockHttpResponse.body())
        .thenReturn(
            Buffer.buffer(
                "{ \"@graph\": [ { \"@id\": \"iudx:Resource\", \"rdfs:subClassOf\": { \"@id\": \"iudx:SuperClass\" } } ] }"));

    Future<JsonObject> resultFuture = dataModel.getDataModelInfo();

    resultFuture.onComplete(
        ar -> {
          assertTrue(ar.succeeded());
          JsonObject result = ar.result();
          assertNotNull(result);
          assertEquals("SuperClass", result.getString("Resource"));
        });
  }

  @Test
  void testHandleDataModelResponse_Success() {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject classIdToSubClassMap = new JsonObject();
    AtomicInteger pendingRequests = new AtomicInteger(1);

    // Mocking AsyncResult for successful response
    AsyncResult<HttpResponse<Buffer>> dmAr = mock(AsyncResult.class);
    when(dmAr.succeeded()).thenReturn(true);
    when(dmAr.result()).thenReturn(mockHttpResponse);
    when(mockHttpResponse.body()).thenReturn(mockBuffer);
    LOGGER.debug(mockHttpResponse.body().toString());
    when(mockHttpResponse.headers()).thenReturn(mockHeaders);
    when(mockHttpResponse.headers().get(anyString())).thenReturn("application/json");
    when(mockBuffer.toJsonObject())
        .thenReturn(
            new JsonObject()
                .put(
                    "@graph",
                    new JsonArray()
                        .add(
                            new JsonObject()
                                .put("@id", "iudx:Class")
                                .put(
                                    "rdfs:subClassOf",
                                    new JsonObject().put("@id", "iudx:SubClass")))));

    dataModel.handleDataModelResponse(
        dmAr, "id", "Class", classIdToSubClassMap, pendingRequests, promise, "dmUrl");

    // Verify the result
    JsonObject result = promise.future().result();
    assertNotNull(result);
    assertEquals("SubClass", result.getString("id"));
  }

  @Test
  void testHandleDataModelResponse_Failed() {
    Promise<JsonObject> promise = Promise.promise();
    JsonObject classIdToSubClassMap = new JsonObject();
    AtomicInteger pendingRequests = new AtomicInteger(1);

    // Mocking AsyncResult for failed response
    AsyncResult<HttpResponse<Buffer>> dmAr = mock(AsyncResult.class);
    when(dmAr.succeeded()).thenReturn(false);
    when(dmAr.cause()).thenReturn(new Throwable("Failed"));

    dataModel.handleDataModelResponse(
        dmAr, "id", "Class", classIdToSubClassMap, pendingRequests, promise, "dmUrl");

    // Verify the result
    JsonObject result = promise.future().result();
    assertNotNull(result);
    assertTrue(result.isEmpty());
  }
}
