package iudx.catalogue.server.auditing;

import static iudx.catalogue.server.auditing.util.Constants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
        private static String databaseIP;
        private static Integer databasePort;
        private static String databaseName;
        private static String databaseUserName;
        private static String databasePassword;
        private static Integer databasePoolSize;

        @BeforeAll
        @DisplayName("Deploying Verticle")
        static void startVertex(Vertx vertx, VertxTestContext vertxTestContext) {
                vertxObj = vertx;
                dbConfig = Configuration.getConfiguration("./configs/config-test.json",4);
                databaseIP = dbConfig.getString("meteringDatabaseIP");
                databasePort = dbConfig.getInteger("meteringDatabasePort");
                databaseName = dbConfig.getString("meteringDatabaseName");
                databaseUserName = dbConfig.getString("meteringDatabaseUserName");
                databasePassword = dbConfig.getString("meteringDatabasePassword");
                databasePoolSize = dbConfig.getInteger("meteringPoolSize");
                auditingService = new AuditingServiceImpl(dbConfig, vertxObj);
                vertxTestContext.completeNow();
        }

        private JsonObject request() {
                JsonObject jsonObject = new JsonObject();
                jsonObject.put(USER_ROLE,"delegate");
                jsonObject.put(USER_ID,"test.data@iudx.org");
                jsonObject.put(IID,"/iid");
                jsonObject.put(API,"/iudx/cat/v1/instance");
                jsonObject.put(METHOD,"POST");
                jsonObject.put(IUDX_ID,"/iudxid");
                return jsonObject;
        }

        @Test
        @DisplayName("Testing write query w/o endpoint")
        void writeForMissingEndpoint(VertxTestContext vertxTestContext){
                JsonObject request = request();
                request.remove(API);
                auditingService.executeWriteQuery(
                        request,
                        vertxTestContext.failing(
                                response ->
                                        vertxTestContext.verify(
                                                () -> {
                                                        LOGGER.info("RESPONSE" + new JsonObject(response.getMessage()).getString(DETAIL));
                                                        assertEquals(DATA_NOT_FOUND, new JsonObject(response.getMessage()).getString(DETAIL));
                                                        vertxTestContext.completeNow();
                                                })));
        }

        @Test
        @DisplayName("Testing write query w/o method")
        void writeForMissingMethod(VertxTestContext vertxTestContext){
                JsonObject request = request();
                request.remove(METHOD);
                auditingService.executeWriteQuery(
                        request,
                        vertxTestContext.failing(
                                response ->
                                        vertxTestContext.verify(
                                                () -> {
                                                        LOGGER.info("RESPONSE" + new JsonObject(response.getMessage()).getString(DETAIL));
                                                        assertEquals(DATA_NOT_FOUND, new JsonObject(response.getMessage()).getString(DETAIL));
                                                        vertxTestContext.completeNow();
                                                })));
        }

        @Test
        @DisplayName("Testing write query w/o user role")
        void writeForMissingUserRole(VertxTestContext vertxTestContext){
                JsonObject request = request();
                request.remove(USER_ROLE);
                auditingService.executeWriteQuery(
                        request,
                        vertxTestContext.failing(
                                response ->
                                        vertxTestContext.verify(
                                                () -> {
                                                        LOGGER.info("RESPONSE" + new JsonObject(response.getMessage()).getString(DETAIL));
                                                        assertEquals(DATA_NOT_FOUND, new JsonObject(response.getMessage()).getString(DETAIL));
                                                        vertxTestContext.completeNow();
                                                })));
        }

        @Test
        @DisplayName("Testing write query w/o user ID")
        void writeForMissingUserID(VertxTestContext vertxTestContext){
                JsonObject request = request();
                request.remove(USER_ID);
                auditingService.executeWriteQuery(
                        request,
                        vertxTestContext.failing(
                                response ->
                                        vertxTestContext.verify(
                                                () -> {
                                                        LOGGER.info("RESPONSE" + new JsonObject(response.getMessage()).getString(DETAIL));
                                                        assertEquals(DATA_NOT_FOUND, new JsonObject(response.getMessage()).getString(DETAIL));
                                                        vertxTestContext.completeNow();
                                                })));
        }

        @Test
        @DisplayName("Testing write query w/o IID")
        void writeForMissingIID(VertxTestContext vertxTestContext){
                JsonObject request = request();
                request.remove(IID);
                auditingService.executeWriteQuery(
                        request,
                        vertxTestContext.failing(
                                response ->
                                        vertxTestContext.verify(
                                                () -> {
                                                        LOGGER.info("RESPONSE" + new JsonObject(response.getMessage()).getString(DETAIL));
                                                        assertEquals(DATA_NOT_FOUND, new JsonObject(response.getMessage()).getString(DETAIL));
                                                        vertxTestContext.completeNow();
                                                })));
        }

        @Test
        @DisplayName("Testing write query w/o IUDX ID")
        void writeForMissingIUDXid(VertxTestContext vertxTestContext){
                JsonObject request = request();
                request.remove(IUDX_ID);
                auditingService.executeWriteQuery(
                        request,
                        vertxTestContext.failing(
                                response ->
                                        vertxTestContext.verify(
                                                () -> {
                                                        LOGGER.info("RESPONSE" + new JsonObject(response.getMessage()).getString(DETAIL));
                                                        assertEquals(DATA_NOT_FOUND, new JsonObject(response.getMessage()).getString(DETAIL));
                                                        vertxTestContext.completeNow();
                                                })));
        }

        @Test
        @DisplayName("Testing Write Query")
        void writeData(VertxTestContext vertxTestContext) {
                JsonObject request = request();
                auditingService.executeWriteQuery(
                request,
                vertxTestContext.succeeding(
                    response ->
                        vertxTestContext.verify(
                            () -> {
                              LOGGER.info("RESPONSE" + response.getString("title"));
                                    assertEquals("Success", response.getString("title"));
                              vertxTestContext.completeNow();
                            })));
        }
}
