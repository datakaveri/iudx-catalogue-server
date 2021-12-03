package iudx.catalogue.server.authenticator;

import static iudx.catalogue.server.authenticator.Constants.*;
import static iudx.catalogue.server.util.Constants.ID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.vertx.ext.web.client.WebClient;
import iudx.catalogue.server.authenticator.authorization.Method;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.catalogue.server.authenticator.model.JwtData;
import iudx.catalogue.server.Configuration;

@ExtendWith(VertxExtension.class)
public class JwtAuthServiceImplTest {

  private static final Logger LOGGER = LogManager.getLogger(JwtAuthServiceImplTest.class);
  private static JsonObject authConfig;
  private static JwtAuthenticationServiceImpl jwtAuthenticationService;
  private static AuthenticationService authenticationService;
  private static Vertx vertxObj;
  /**
   * Initialize and start the auth service for testing.
   *
   * @param vertx       the vertx instance object. Injected by VertxExtension and not started in clustered mode.
   * @param testContext the context object injected by VertxExtension
   */
  @BeforeAll
  @DisplayName("Initialize Vertx and deploy Auth Verticle")
  static void init(Vertx vertx, VertxTestContext testContext) {
    authConfig = Configuration.getConfiguration("./configs/config-test.json",1);
    String cert = authConfig.getString("cert");
    JWTAuthOptions jwtAuthOptions = new JWTAuthOptions();
    jwtAuthOptions.addPubSecKey(
            new PubSecKeyOptions()
                    .setAlgorithm("ES256")
                    .setBuffer(cert));

    // ignore token expiration only for test
    jwtAuthOptions.getJWTOptions().setIgnoreExpiration(true);
    JWTAuth jwtAuth = JWTAuth.create(vertx, jwtAuthOptions);

    jwtAuthenticationService = new JwtAuthenticationServiceImpl(vertx,  jwtAuth, authConfig);

    LOGGER.info("Auth tests setup complete");
    testContext.completeNow();
  }

  @Test
  @DisplayName("Test getting public key from auth server")
  public void testPublicKeySetup(Vertx vertx, VertxTestContext testContext) {
    AuthenticationVerticle.getJwtPublicKey(vertx, authConfig)
            .onComplete(handler -> {
              if(handler.succeeded())
              {
                testContext.completeNow();
              } else {
                LOGGER.info(handler.cause().getMessage());
                testContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Test if webClient has been initialized properly")
  public void testWebClientSetup(Vertx vertx, VertxTestContext testContext) {
    WebClient client = AuthenticationVerticle.createWebClient(vertx, authConfig, true);
    String host = authConfig.getString(Constants.AUTH_SERVER_HOST);
    client.post(443, host, Constants.AUTH_CERTINFO_PATH).send(httpResponseAsyncResult -> {
      if (httpResponseAsyncResult.failed()) {
        LOGGER.error("Cert info call failed");
        testContext.failNow(httpResponseAsyncResult.cause());
        return;
      }
      LOGGER.info("Cert info call to auth server succeeded");
      testContext.completeNow();
    });
  }

  private JsonObject authJson() {
    JsonObject jsonObject = new JsonObject();
    jsonObject
            .put("token", JwtTokenHelper.providerToken)
            .put("id", "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86")
            .put("apiEndpoint", "/iudx/cat/v1/item")
            .put("method", Method.POST);
    return jsonObject;
  }

  private JwtData jwtDataObject() {
    JwtData jwtData = new JwtData();
    jwtData.setIss("authvertx.iudx.io");
    jwtData.setAud("catalogue.iudx.io");
    jwtData.setExp(1627408865L);
    jwtData.setIat(1627408865L);
    jwtData.setIid("ri:iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86/catalogue.iudx.io/catalogue/crud");
    jwtData.setRole("provider");
    jwtData.setCons(new JsonObject());

    return jwtData;
  }

  @Test
  @DisplayName("successful allow access to protected endpoint")
  public void providerTokenInterospectSuccess(VertxTestContext vertxTestContext) {
    JsonObject authInfo = authJson();

    jwtAuthenticationService
            .tokenInterospect(new JsonObject(), authInfo, handler -> {
              if(handler.succeeded()) {
                LOGGER.debug("Successfuly interospected the token");
                vertxTestContext.completeNow();
              }
              else  {
                vertxTestContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("fail: allow access to protected endpoint")
  public void providerTokenInterospectFail(VertxTestContext vertxTestContext) {
    JsonObject authInfo = authJson();
    authInfo.put(TOKEN, "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiI4NDRlMjUxYi01NzRiLTQ2ZTYtOTI0Ny1mNzZmMWY3MGE2NadjMDKqd4NjPjKieYSpenWzeIhvbYW21212pbyIsImF1ZCI6ImNhdGFsb2d1ZS5pdWR4LmlvIiwiZXhwIjoxNjMyMjYxMjkxLCJpYXQiOjE2MzIyMTgwOTEsImlpZCI6InJpOmlpc2MuYWMuaW4vODlhMzYyNzNkNzdkYWM0Y2YzODExNGZjYTFiYmU2NDM5MjU0N2Y4Ni9jYXRhbG9ndWUuaXVkeC5pby9jYXRhbG9ndWUvY3J1ZCIsInJvbGUiOiJwcm92aWRlciIsImNvbnMiOnt9fQ.BTNDXRQ90C9wTWGtcYzIgjZgbhoV_ELX6smaJxjbvceKFHbVaHMaxYMMyyTrQUGe3b7BpGgODu4vR6JAycfmRg");

    jwtAuthenticationService
            .tokenInterospect(new JsonObject(), authInfo, handler -> {
              if(handler.succeeded())
                vertxTestContext.failNow(handler.cause());
              else
                vertxTestContext.completeNow();
            });
  }

  @Test
  @DisplayName("Decode valid provider JWT")
  public void decodeJwtProviderSuccess(VertxTestContext vertxTestContext) {
    jwtAuthenticationService.decodeJwt(JwtTokenHelper.providerToken)
            .onComplete(handler -> {
              if(handler.succeeded()) {
                assertEquals("provider", handler.result().getRole());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Decode valid delegate JWT")
  public void decodeJwtDelegateSuccess(VertxTestContext vertxTestContext) {
    jwtAuthenticationService.decodeJwt(JwtTokenHelper.delegateToken)
            .onComplete(handler -> {
              if(handler.succeeded()) {
                assertEquals("delegate", handler.result().getRole());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Decode valid admin JWT")
  public void decodeJwtAdminSuccess(VertxTestContext vertxTestContext) {
    jwtAuthenticationService.decodeJwt(JwtTokenHelper.adminToken)
            .onComplete(handler -> {
              if(handler.succeeded()) {
                assertEquals("admin", handler.result().getRole());
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("Decode invalid JWT")
  public void decodeInvalidJwt(VertxTestContext vertxTestContext) {
    jwtAuthenticationService.decodeJwt("eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJzdWIiOiI4NDRlMjUxYi01NzRiLTQ2ZTYtOTI0Ny1mNzZmMWY3MGE2MzciLCJpc3MiOiJhdXRodmVydHguaXVkeC5pbyIsImF1ZCI6ImNhdGFsb2d1ZS5pdWR4LmlvIiwiZXhwIjoxNjMyMjYxMjkxLCJpYXQiOjE2MzIyMTgwOTEsImlpZCI6InJpOmlpc2MuYWMuaW4vODlhMzYyNzNkNzdkYWM0Y2YzODExNGZjYTFiYmU2NDM5MjU0N2Y4Ni9jYXRhbG9ndWUuaXVkeC5pby9jYXRhbG9ndWUvY3J1ZCIsInJvbGUiOiJwcm92aWRlciIsImNvbnMiOnt9fQ.BTNDXRQ90C9wTWGtcYzIgjZgbhoV_ELX6smaJxjbvceKFHbVaHMaxYMMyyTrQUGe3b7BpGgODu4vR6JA123456")
            .onComplete(handler -> {
              if(handler.succeeded()) {
                vertxTestContext.failNow(handler.cause());
              } else {
                vertxTestContext.completeNow();
              }
            });
  }

  @Test
  @DisplayName("successful valid endpoint check")
  public void validEndpointCheck(VertxTestContext vertxTestContext) {
    JsonObject authInfo = authJson();

    jwtAuthenticationService.isValidEndpoint(authInfo.getString("apiEndpoint")).onComplete(handler -> {
      if (handler.failed()) {
        vertxTestContext.failNow("fail");
      } else {
        vertxTestContext.completeNow();

      }
    });
  }

  @Test
  @DisplayName("invalid endpoint check")
  public void invalidEndpointCheck(VertxTestContext vertxTestContext) {
    JsonObject authInfo = authJson();
    authInfo.put("apiEndpoint", "/iudx/rs/v1/item");

    jwtAuthenticationService.isValidEndpoint(authInfo.getString("apiEndpoint")).onComplete(handler -> {
      if (handler.failed()) {
        vertxTestContext.completeNow();
      } else {
        vertxTestContext.failNow("fail");

      }
    });
  }

  @Test
  @DisplayName("successful valid id check")
  public void validIdCheckForJwtToken(VertxTestContext vertxTestContext) {
    JwtData jwtData = jwtDataObject();
    String id = "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547f86";
    jwtAuthenticationService.isValidId(jwtData, id).onComplete(handler -> {
      if (handler.failed()) {
        vertxTestContext.failNow("fail");
      } else {
        vertxTestContext.completeNow();

      }
    }
    ) ;
  }

  @Test
  @DisplayName("invalid id check")
  public void invalidIdCheckForJwtToken(VertxTestContext vertxTestContext) {
    JwtData jwtData = jwtDataObject();
    String id = "iisc.ac.in/89a36273d77dac4cf38114fca1bbe64392547fab";

    jwtAuthenticationService.isValidId(jwtData, id).onComplete(handler -> {
      if (handler.failed()) {
        vertxTestContext.completeNow();
      } else {
        vertxTestContext.failNow("fail");

      }
    });
  }

  @Test
  @DisplayName("successful valid audience check")
  public void validAudienceCheck(VertxTestContext vertxTestContext) {
    JwtData jwtData = jwtDataObject();
    jwtAuthenticationService.isValidAudienceValue(jwtData).onComplete(handler -> {
          if (handler.failed()) {
            vertxTestContext.failNow("fail");
          } else {
            vertxTestContext.completeNow();

          }
      });
  }

  @Test
  @DisplayName("invalid audience check")
  public void invalidAudienceCheck(VertxTestContext vertxTestContext) {
    JwtData jwtData = jwtDataObject();
    jwtData.setAud("rs.iudx.io");

    jwtAuthenticationService.isValidAudienceValue(jwtData).onComplete(handler -> {
          if (handler.failed()) {
            vertxTestContext.completeNow();
          } else {
            vertxTestContext.failNow("fail");

          }
      });
  }

  @Test
  @DisplayName("successful validate access test")
  public void validvValidateAccessTest(VertxTestContext vertxTestContext) {
    JwtData jwtData = jwtDataObject();
    JsonObject authInfo = authJson();

    jwtAuthenticationService.validateAccess(jwtData,authInfo)
            .onComplete(handler -> {
              if(handler.succeeded()){
                vertxTestContext.completeNow();
              } else {
                vertxTestContext.failNow(handler.cause());
              }
            });
  }

  @Test
  @DisplayName("fail validate access test")
  public void invalidValidateAccessTest(VertxTestContext vertxTestContext) {
    JwtData jwtData = jwtDataObject();
    JsonObject authInfo = authJson();
    authInfo.put("apiEndpoint","/iudx/cat/v1/itemzzz");

    jwtAuthenticationService.validateAccess(jwtData,authInfo)
            .onComplete(handler -> {
              if(handler.succeeded()){
                vertxTestContext.failNow(handler.cause());
              } else {
                vertxTestContext.completeNow();
              }
            });
  }


  @Test
  @DisplayName("successful allow admin access to protected endpoint")
  public void adminTokenInterospectSuccess(VertxTestContext vertxTestContext) {

    JsonObject authInfo = new JsonObject();
    authInfo
            .put("token", JwtTokenHelper.adminToken)
            .put("id", "catalogue.iudx.io")
            .put("apiEndpoint", "/iudx/cat/v1/instance")
            .put("method", Method.POST);

    jwtAuthenticationService
            .tokenInterospect(new JsonObject(), authInfo, handler -> {
              if(handler.succeeded()) {
                LOGGER.debug("Successfuly interospected the token");
                vertxTestContext.completeNow();
              }
              else  {
                vertxTestContext.failNow(handler.cause());
              }
            });
  }

}
