package iudx.catalogue.server.auditing;

import static iudx.catalogue.server.auditing.util.Constants.USER_ROLE;
import static iudx.catalogue.server.auditing.util.Constants.EMAIL_ID;
import static iudx.catalogue.server.auditing.util.Constants.IID;
import static iudx.catalogue.server.auditing.util.Constants.API;
import static iudx.catalogue.server.auditing.util.Constants.METHOD;
import static iudx.catalogue.server.auditing.util.Constants.IUDX_ID;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import iudx.catalogue.server.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith({VertxExtension.class})
public class AuditingServiceTest {

        private static final Logger LOGGER = LogManager.getLogger(AuditingServiceTest.class);
        private static AuditingService auditingService;
        private static Vertx vertxObj;
        private static JsonObject dbConfig;
//        private static String databaseIP;
//        private static int databasePort;
//        private static String databaseName;
//        private static String databaseUserName;
//        private static String databasePassword;
//        private static int databasePoolSize;

        @BeforeAll
        @DisplayName("Deploying Verticle")
        static void startVertex(Vertx vertx, VertxTestContext vertxTestContext) {
                vertxObj = vertx;
                dbConfig = Configuration.getConfiguration("./configs/config-test.json",4);
                System.out.println(dbConfig.toString());
//                databaseIP = dbConfig.getString("meteringDatabaseIP");
//                databasePort = dbConfig.getInteger("meteringDatabasePort");
//                databaseName = dbConfig.getString("meteringDatabaseName");
//                databaseUserName = dbConfig.getString("meteringDatabaseUserName");
//                databasePassword = dbConfig.getString("meteringDatabasePassword");
//                databasePoolSize = dbConfig.getInteger("meteringPoolSize");
                auditingService = new AuditingServiceImpl(dbConfig, vertxObj);
                vertxTestContext.completeNow();
        }

        @Test
        @DisplayName("Testing Write Query")
        void writeData(VertxTestContext vertxTestContext) {
                JsonObject request = new JsonObject();
                request.put(USER_ROLE,"delegate");
                request.put(EMAIL_ID,"test.data@iudx.org");
                request.put(IID,"1234abcd");
                request.put(API,"/iudx/cat/v1/instance");
                request.put(METHOD,"POST");
                request.put(IUDX_ID,"abcd1234");
                auditingService.executeWriteQuery(
                request,
                vertxTestContext.succeeding(
                    response ->
                        vertxTestContext.verify(
                            () -> {
                              LOGGER.info("RESPONSE" + response.getString("title"));
                              assertTrue(response.getString("title").equals("Success"));
                              vertxTestContext.completeNow();
                            })));
        }
}
