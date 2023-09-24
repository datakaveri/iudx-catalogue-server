package iudx.catalogue.server.authenticator.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({VertxExtension.class, MockitoExtension.class})
public class JwtDataTest {
  JwtData jwtData;

  @BeforeEach
  public void setUp(VertxTestContext vertxTestContext) {
    jwtData = new JwtData();
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test toJson method")
  public void test_toJson(VertxTestContext vertxTestContext) {
    JsonObject actual = jwtData.toJson();
    assertNotNull(actual);

    JsonObject result = new JsonObject();
    result.put("exp", 0).put("iat", 0);
    assertEquals(result, actual);
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test getAccess_token method")
  public void test_getAccess_token(VertxTestContext vertxTestContext) {
    String actual = jwtData.getAccessToken();
    assertNull(actual);
    vertxTestContext.completeNow();
  }

  @Test
  @DisplayName("Test toString method")
  public void testToString(VertxTestContext vertxTestContext) {
    String access_token = jwtData.getAccessToken();
    String sub = jwtData.getSub();
    String iss = jwtData.getIss();
    String aud = jwtData.getAud();
    String iid = jwtData.getIid();
    String role = jwtData.getRole();
    JsonObject cons = jwtData.getCons();
    String clientid = jwtData.getClientId();
    String did = jwtData.getDid();
    String drl = jwtData.getDrl();
    String result =
        "JwtData [access_token="
            + access_token
            + ", sub="
            + sub
            + ", iss="
            + iss
            + ", aud="
            + aud
            + ", iid="
            + iid
            + ", role="
            + role
            + ", cons="
            + cons
            + ", client_id="
            + clientid
            + ", did="
            + did
            + ", drl="
            + drl
            + "]";

    assertEquals(result, jwtData.toString());
    vertxTestContext.completeNow();
  }
}
