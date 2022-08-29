package iudx.catalogue.server.authenticator.authorization;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({VertxExtension.class, MockitoExtension.class})

public class AuthorizationRequestTest {
    AuthorizationRequest authorizationRequest;
    IudxRole iudxRole;
    @Mock
    Method method;
    @Mock
    Api api;
    @Test
    @DisplayName("Test getMethod method")
    public void test_toJson(VertxTestContext vertxTestContext)
    {
        authorizationRequest=new AuthorizationRequest(method,api);
        String actual= String.valueOf(authorizationRequest.getMethod());
        assertNotNull(actual);
        vertxTestContext.completeNow();
    }
    @Test
    @DisplayName("Test getApi method")
    public void testGetApi(VertxTestContext vertxTestContext)
    {
        authorizationRequest=new AuthorizationRequest(method,api);
        assertNotNull(authorizationRequest.getApi());
        vertxTestContext.completeNow();
    }
    @Test
    @DisplayName("Test hashCode() method")
    public void testHashCode(VertxTestContext vertxTestContext){
        final int prime=31;
        int result = 1;
        result = prime * result + ((api == null) ? 0 : api.hashCode());
        result = prime * result + ((method == null) ? 0 : method.hashCode());

        authorizationRequest=new AuthorizationRequest(method,api);
        assertEquals(result,authorizationRequest.hashCode());
        vertxTestContext.completeNow();
    }
    @Test
    @DisplayName("Test equals method")
    public void testEquals(VertxTestContext vertxTestContext){
        authorizationRequest=new AuthorizationRequest(method,api);
        Object obj = new Object();
        authorizationRequest.equals(obj);
        obj=null;
        assertFalse(authorizationRequest.equals(obj));


        vertxTestContext.completeNow();
    }

}