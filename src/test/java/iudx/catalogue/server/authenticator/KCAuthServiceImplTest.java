package iudx.catalogue.server.authenticator;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.catalogue.server.Configuration;
import iudx.catalogue.server.authenticator.model.JwtData;
import iudx.catalogue.server.util.Api;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.text.ParseException;

import static org.mockito.Mockito.*;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class KCAuthServiceImplTest {
  private static final Logger LOGGER = LogManager.getLogger(KCAuthenticationServiceImpl.class);
  private static KCAuthenticationServiceImpl kcAuthenticationService, kcAuthenticationServiceSpy;
  private static ConfigurableJWTProcessor<SecurityContext> jwtProcessor;
  private static Api api;
  private static AsyncResult<JsonObject> asyncResult;

  @BeforeAll
  @DisplayName("Initialize vertx and deploy auth verticle")
  static void init(Vertx vertx, VertxTestContext testContext) {

    api = Api.getInstance("/iudx/cat/v1");
    jwtProcessor = mock(DefaultJWTProcessor.class);
    kcAuthenticationService = new KCAuthenticationServiceImpl(jwtProcessor, api);
    kcAuthenticationServiceSpy = spy(kcAuthenticationService);
    asyncResult = mock(AsyncResult.class);
    testContext.completeNow();
  }

  @Test
  @DisplayName("Success: Test token introspect")
  public void TestTokenIntrospect(Vertx vertx,  VertxTestContext testContext){
    JsonObject authInfo = mock(JsonObject.class);
    doAnswer(Answer -> Future.succeededFuture(new JwtData(new JsonObject()))
    ).when(kcAuthenticationServiceSpy).decodeKCToken(anyString());

    when(authInfo.getString(anyString())).thenReturn(api.getRouteItems());

    kcAuthenticationServiceSpy.tokenInterospect(new JsonObject(), authInfo, handler -> {
      if(handler.succeeded()) {
//        verify(authInfo, times(1));
        LOGGER.debug("success");
        testContext.completeNow();
      } else {
        LOGGER.debug("fail");
        testContext.failNow(handler.cause());
      }
    });
  }

  @Test
  @DisplayName("Fail: Test token introspect - invalid endpoint")
  public void FailureTestTokenIntrospect(Vertx vertx, VertxTestContext testContext) {

    JsonObject authInfo = mock(JsonObject.class);
    doAnswer(Answer -> Future.succeededFuture(new JwtData(new JsonObject()))
    ).when(kcAuthenticationServiceSpy).decodeKCToken(anyString());

    when(authInfo.getString(anyString())).thenReturn(api.getRouteRelationship());


    kcAuthenticationServiceSpy.tokenInterospect(new JsonObject(), authInfo, handler -> {
      if(handler.failed()) {
        testContext.completeNow();
      } else {
        testContext.failNow("Failed");
      }
    });
  }

  @Test
  @DisplayName("Fail: Test token introspect - token decode fail")
  public void FailureTestTokenIntrospect(VertxTestContext testContext) {

    JsonObject authInfo = mock(JsonObject.class);
    doAnswer(Answer -> Future.failedFuture("decode failed")
    ).when(kcAuthenticationServiceSpy).decodeKCToken(anyString());

    when(authInfo.getString(anyString())).thenReturn(api.getRouteRelationship());


    kcAuthenticationServiceSpy.tokenInterospect(new JsonObject(), authInfo, handler -> {
      if(handler.failed()) {
        testContext.completeNow();
      } else {
        LOGGER.error("failed");
        testContext.failNow("Failed");
      }
    });
  }

  /*static String getKCToken(Vertx vertx) {
    WebClientOptions webClientOptions = new WebClientOptions().setTrustAll(true).setVerifyHost(false).setSsl(true);

    WebClient webClient = WebClient.create(vertx,)
  }*/

  @Test
  @DisplayName("Success: Test decode token")
  public void TestDecodeTokenFuture(Vertx vertx, VertxTestContext testContext) throws BadJOSEException, ParseException, JOSEException {

    JWTClaimsSet jwtClaimsSet = JWTClaimsSet.parse("{\n" +
        "\t\"exp\": 1687091138,\n" +
        "\t\"iat\": 1687089398,\n" +
        "\t\"jti\": \"f801adad-704e-40cc-b2ea-c2e42408e3bc\",\n" +
        "\t\"iss\": \"https://keycloak.demo.org/auth/realms/demo\",\n" +
        "\t\"aud\": \"account\",\n" +
        "\t\"sub\": \"a9a10917-b51e-4ccb-af59-32af3df9dfbb\",\n" +
        "\t\"typ\": \"Bearer\",\n" +
        "\t\"clientId\": \"cop.iudx.io\"\n" +
        "}");
    when(jwtProcessor.process(anyString(), any())).thenReturn(jwtClaimsSet);
    kcAuthenticationService.decodeKCToken("token")
        .onComplete(handler -> {
          if(handler.succeeded()) {
            testContext.completeNow();
          } else {
            testContext.failNow("decode token test failed");
          }
        });
  }
}
