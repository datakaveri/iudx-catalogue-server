package iudx.catalogue.server.mockauthenticator;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import jdk.jfr.Description;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static junit.framework.Assert.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class MockAuthenticationServiceImplTest {
    @Mock
    WebClient client;
    @Mock
    Handler<AsyncResult<JsonObject>> handler;
    MockAuthenticationServiceImpl mockAuthenticationService;
    @Test
    @Description("test tokenIntrospect method ")
    public void testTokenInterospect(VertxTestContext vertxTestContext) {
        String authHost="dummy";
        mockAuthenticationService=new MockAuthenticationServiceImpl(client,authHost);
        JsonObject request=new JsonObject();
        JsonObject authenticationInfo=new JsonObject();
        assertNotNull(mockAuthenticationService.tokenInterospect(request,authenticationInfo,handler));
        vertxTestContext.completeNow();
    }
    @Test
    @Description("test IsPermittedProviderID method ")
    public void testIsPermittedProviderID(VertxTestContext vertxTestContext) {
        String authHost="dummy";
        mockAuthenticationService=new MockAuthenticationServiceImpl(client,authHost);
        String requestID="abcd/abcd/abcd/abcd";
        String providerID="dummy";
        assertFalse(mockAuthenticationService.isPermittedProviderID(requestID,providerID));
        vertxTestContext.completeNow();
    }
    @Test
    @Description("test IsPermittedMethod method ")
    public void testIsPermittedMethod(VertxTestContext vertxTestContext) {
        String authHost="dummy";
        mockAuthenticationService=new MockAuthenticationServiceImpl(client,authHost);
        JsonArray methods =new JsonArray();
        String providerID="dummy";
        assertFalse(mockAuthenticationService.isPermittedMethod(methods,providerID));
        vertxTestContext.completeNow();
    }
}
