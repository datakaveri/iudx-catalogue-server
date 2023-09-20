package iudx.catalogue.server.authenticator.authorization;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.catalogue.server.authenticator.model.JwtData;
import iudx.catalogue.server.util.Api;
import jdk.jfr.Description;
import static junit.framework.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import junit.framework.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Executable;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class AuthorizationContextFactoryTest {
    @Mock
    AuthorizationRequest authRequest;
    @Mock
    JwtData jwtData;
    DelegateAuthStrategy delegateAuthStrategy;
    ConsumerAuthStrategy consumerAuthStrategy;
    AuthorizationContextFactory authorizationContextFactory;
    private String dxApiBasePath;
    private Api api;

    @BeforeEach
    public void init(VertxTestContext vertxTestContext)
    {
        dxApiBasePath = "/iudx/cat/v1";
        api = Api.getInstance(dxApiBasePath);
        vertxTestContext.completeNow();
    }
    @Test
    @Description("testing the method create when role is consumer")
    public void testCreate(VertxTestContext vertxTestContext) {
        authorizationContextFactory=new AuthorizationContextFactory();
        String role="consumer";
        consumerAuthStrategy= ConsumerAuthStrategy.getInstance(this.api);
        assertFalse(consumerAuthStrategy.isAuthorized(authRequest));
        assertNotNull(AuthorizationContextFactory.create(role,this.api));
        vertxTestContext.completeNow();
    }
    @Test
    @Description("testing the method create when role is delegate")
    public void testCreateDelegate(VertxTestContext vertxTestContext) {
        authorizationContextFactory=new AuthorizationContextFactory();
        String role="delegate";
        delegateAuthStrategy= DelegateAuthStrategy.getInstance(this.api);
        assertFalse(delegateAuthStrategy.isAuthorized(authRequest));
        assertNotNull(AuthorizationContextFactory.create(role,this.api));
        vertxTestContext.completeNow();
    }

    @Test
    @Description("test create for cop admin")
    public void testCreateCopAdmin(VertxTestContext testContext) {
        authorizationContextFactory=new AuthorizationContextFactory();
        String role="cos_admin";
        delegateAuthStrategy= DelegateAuthStrategy.getInstance(this.api);
        assertFalse(delegateAuthStrategy.isAuthorized(authRequest));
        assertNotNull(AuthorizationContextFactory.create(role,this.api));
        testContext.completeNow();
    }

    @Test
    @Description("testing the method create with IllegalArgumentException")
    public void testCreateDefault(VertxTestContext vertxTestContext) {
        assertThrows(IllegalArgumentException.class,()->{
            authorizationContextFactory=new AuthorizationContextFactory();
            String role="dummy";
            AuthorizationContextFactory.create(role,this.api);
        });
        vertxTestContext.completeNow();

    }


}