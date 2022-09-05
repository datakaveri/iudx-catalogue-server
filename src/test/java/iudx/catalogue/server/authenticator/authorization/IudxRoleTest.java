package iudx.catalogue.server.authenticator.authorization;

import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import jdk.jfr.Description;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

@ExtendWith(MockitoExtension.class)
@ExtendWith(VertxExtension.class)
public class IudxRoleTest {

    @Test
    @Description("testing the method create")
    public void testCreate(VertxTestContext vertxTestContext) {
    String role="dummy";
    assertNull(IudxRole.fromRole(role));
    vertxTestContext.completeNow();
    }

}
