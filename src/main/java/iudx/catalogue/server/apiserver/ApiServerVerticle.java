package iudx.catalogue.server.apiserver;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.EventBusService;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import iudx.catalogue.server.apiserver.util.QueryMapper;
import iudx.catalogue.server.apiserver.util.ResponseHandler;
import iudx.catalogue.server.authenticator.AuthenticationService;
import iudx.catalogue.server.database.DatabaseService;
import iudx.catalogue.server.validator.ValidatorService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import static iudx.catalogue.server.apiserver.util.Constants.*;

/**
 * The Catalogue Server API Verticle.
 *
 * <h1>Catalogue Server API Verticle</h1>
 *
 * <p>
 * The API Server verticle implements the IUDX Catalogue Server APIs. It handles the API requests
 * from the clients and interacts with the associated Service to respond.
 *
 * @see io.vertx.core.Vertx
 * @see io.vertx.core.AbstractVerticle
 * @see io.vertx.core.http.HttpServer
 * @see io.vertx.ext.web.Router
 * @see io.vertx.servicediscovery.ServiceDiscovery
 * @see io.vertx.servicediscovery.types.EventBusService
 * @see io.vertx.spi.cluster.hazelcast.HazelcastClusterManager
 * @version 1.0
 * @since 2020-05-31
 */
public class ApiServerVerticle extends AbstractVerticle {


  private ClusterManager mgr;
  private VertxOptions options;

  private ServiceDiscovery discovery;

  private DatabaseService dbService;
  private ValidatorService validationService;
  private AuthenticationService authService;

  private HttpServer server;
  private CrudApis crudApis;
  private SearchApis searchApis;
  private ListApis listApis;

  @SuppressWarnings("unused")
  private Router router;

  private String keystore;
  private String keystorePassword;

  private Properties properties;
  private InputStream inputstream;

  private static final Logger LOGGER = LogManager.getLogger(ApiServerVerticle.class);

  /**
   * This method is used to start the Verticle and joing a cluster
   *
   * @throws Exception which is a startup exception
   */
  @Override
  public void start() throws Exception {

    mgr = new HazelcastClusterManager();
    options = new VertxOptions().setClusterManager(mgr);

    /* Create or Join a Vert.x Cluster. */
    Vertx.clusteredVertx(options, res -> {
      if (res.succeeded()) {

        Vertx vertx = res.result();
        router = Router.router(vertx);

        properties = new Properties();
        inputstream = null;
        
        /* Read the configuration and set the HTTPs server properties. */
        try {
          inputstream = new FileInputStream(CONFIG_FILE);
          properties.load(inputstream);
          keystore = properties.getProperty(KEYSTORE_FILE_NAME);
          keystorePassword = properties.getProperty(KEYSTORE_FILE_PASSWORD);
        } catch (Exception ex) {
          LOGGER.info(ex.toString());
        }


        /** Instantiate this server */
        server = vertx.createHttpServer(new HttpServerOptions()
                                            .setSsl(true)
                                            .setKeyStoreOptions(new JksOptions()
                                                                .setPath(keystore)
                                                                .setPassword(keystorePassword)));

        /** API Callback managers */
        crudApis = new CrudApis();
        searchApis = new SearchApis();
        listApis = new ListApis();

        /**
         *
         * Get proxies and handlers
         *
        */

        /* Handler for service discovery */
        discovery = ServiceDiscovery.create(vertx);

        /* Handler for DatabaseService from service discovery */
        EventBusService.getProxy(discovery, DatabaseService.class,
            ar -> {
              if (ar.succeeded()) {
                dbService = ar.result();
                crudApis.setDbService(dbService);
                listApis.setDbService(dbService);
                searchApis.setDbService(dbService);
                LOGGER.info("Service Discovery Success. Service name is : "
                        + dbService.getClass().getName());
              } else {
                LOGGER.fatal("DatabaseService Discovery Failed");
              }
            });

        /* Handler for AuthenticationService from service discovery*/
        EventBusService.getProxy(discovery, AuthenticationService.class,
            ar -> {
              if (ar.succeeded()) {
                authService = ar.result();
                crudApis.setAuthService(authService);
                LOGGER.info("Service Discovery Success. Service name is : "
                        + authService.getClass().getName());
              } else {
                LOGGER.fatal("Auth Discovery Failed");
              }
            });

        /* Handler for ValidatorService from service discovery*/
        EventBusService.getProxy(discovery, ValidatorService.class,
            ar -> {
              if (ar.succeeded()) {
                validationService = ar.result();
                crudApis.setValidatorService(validationService);
                LOGGER.info("Service Discovery Success. Service name is : "
                        + validationService.getClass().getName());
              } else {
                LOGGER.fatal("ValidatorService Discovery Failed");
              }
            });


        /**
         *
         * API Routes and Callbacks
         *
         */

        /** 
         * Routes - Defines the routes and callbacks
         */
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.route().handler(CorsHandler.create("*").allowedHeaders(ALLOWED_HEADERS));

        /** Static Resource Handler */
        router.route(ROUTE_STATIC).handler(StaticHandler.create());

        /**
         * Routes for item CRUD
         */
        /* Create Item - Body contains data */
        router.post(ROUTE_ITEMS)
          .consumes(MIME_APPLICATION_JSON)
          .produces(MIME_APPLICATION_JSON)
          .handler( routingContext -> {
          /* checking auhthentication info in requests */
          if (routingContext.request().headers().contains(HEADER_TOKEN)) {
            crudApis.createItemHandler(routingContext);
          } else {
            LOGGER.warn("Fail: Unathorized CRUD operation");
            routingContext.response().setStatusCode(401).end();
          }
        });

        /* Get Item */
        router.get(ROUTE_ITEMS)
          .produces(MIME_APPLICATION_JSON)
          .handler( routingContext -> {
          crudApis.getItemHandler(routingContext);
        });

        /* Update Item - Body contains data */
        router.put(ROUTE_UPDATE_ITEMS)
          .consumes(MIME_APPLICATION_JSON)
          .produces(MIME_APPLICATION_JSON)
          .handler( routingContext -> {
          /* checking auhthentication info in requests */
          if (routingContext.request().headers().contains(HEADER_TOKEN)) {
            /** Update params checked in createItemHandler */
            crudApis.createItemHandler(routingContext);
          } else {
            LOGGER.warn("Unathorized CRUD operation");
            routingContext.response().setStatusCode(401).end();
          }
        });

        /* Delete Item - Query param contains id */
        router.delete(ROUTE_DELETE_ITEMS)
          .produces(MIME_APPLICATION_JSON)
          .handler( routingContext -> {
          /* checking auhthentication info in requests */
          if (routingContext.request().headers().contains(HEADER_TOKEN) &&
              routingContext.queryParams().contains(ID)) {
            /** Update params checked in createItemHandler */
            crudApis.deleteItemHandler(routingContext);
          } else {
            LOGGER.warn("Unathorized CRUD operation");
            routingContext.response().setStatusCode(401).end();
          }
        });


        /**
         * Routes for search and count
         */
        /* Search for an item */
        router.get(ROUTE_SEARCH)
          .produces(MIME_APPLICATION_JSON)
          .handler( routingContext -> {
          searchApis.searchHandler(routingContext);
        });

        /* Count the Cataloque server items */
        router.get(ROUTE_COUNT)
          .produces(MIME_APPLICATION_JSON)
          .handler( routingContext -> {
          searchApis.searchHandler(routingContext);
        });

        
        /**
         * Routes for list
         */
        /* list the item from database using itemId */
        router.get(ROUTE_LIST_ITEMS)
          .produces(MIME_APPLICATION_JSON)
          .handler( routingContext -> { 
            listApis.listItems(routingContext);
          });
        /* Get list types with the database for an item */
        /* list the item from database using itemId */
        router.getWithRegex(ROUTE_DATA_TYPE)
          .produces(MIME_APPLICATION_JSON)
          .handler( routingContext -> { 
            listApis.listTypes(routingContext);
          });

        /**
         * Routes for relationships
         */
        /* Get all resources belonging to a resource group */
        router.getWithRegex(ROUTE_LIST_RESOURCE_REL)
            .handler(this::listResourceRelationship);
        /* Get resource group of an item belonging to a resource */
        router.getWithRegex(ROUTE_LIST_RESOURCE_GROUP_REL)
            .handler(this::listResourceGroupRelationship);
        /* Get provider relationship to an item */
        router.getWithRegex(ROUTE_PROVIDER_REL).handler(this::listProviderRelationship);
        /* Get resource server relationship to an item */
        router.getWithRegex(ROUTE_RESOURCE_SERVER_REL)
            .handler(this::listResourceServerRelationship);
        /* Relationship related search */
        router.get(ROUTE_REL_SEARCH).handler(this::relSearch);



        /**
         * Start server 
         */
        server.requestHandler(router).listen(PORT);


      }
    });
  }


  /**
   * Get all resources belonging to a resourceGroup.
   *
   * @param routingContext handles web requests in Vert.x Web
   */
  public void listResourceRelationship(RoutingContext routingContext) {

    LOGGER.info("Searching for relationship of resource");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(HEADER_HOST);

    /* Parsing id from HTTP request */
    String id = request.getParam(ID);

    /* Checking if id is either not null nor empty */
    if (id != null && !id.isBlank()) {

      /* Populating query mapper */
      requestBody.put(ID, id);
      requestBody.put(INSTANCE_ID_KEY, instanceID);
      requestBody.put(RELATIONSHIP, REL_RESOURCE);

      /*
       * Request database service with requestBody for listing resource relationship
       */
      dbService.listResourceRelationship(requestBody, dbhandler -> {
        if (dbhandler.succeeded()) {
          LOGGER.info("List of resources belonging to resourceGroups "
              .concat(dbhandler.result().toString()));
          response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
              .setStatusCode(200).end(dbhandler.result().toString());
        } else if (dbhandler.failed()) {
          LOGGER.error(
              "Issue in listing resource relationship ".concat(dbhandler.cause().toString()));
          response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
              .setStatusCode(400).end(dbhandler.cause().toString());
        }
      });
    } else {
      LOGGER.error("Issue in path parameter");
      response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
          .setStatusCode(400).end(new ResponseHandler.Builder().withStatus(INVALID_SYNTAX)
              .build().toJsonString());
    }
  }

  /**
   * Get all resourceGroup relationships.
   *
   * @param routingContext handles web requests in Vert.x Web
   */
  public void listResourceGroupRelationship(RoutingContext routingContext) {

    LOGGER.info("Searching for relationship of resource and resourceGroup");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(HEADER_HOST);

    /* Parsing id from HTTP request */
    String id = request.getParam(ID);

    /* Checking if id is either not null nor empty */
    if (id != null && !id.isBlank()) {

      /* Populating query mapper */
      requestBody.put(ID, id);
      requestBody.put(INSTANCE_ID_KEY, instanceID);
      requestBody.put(RELATIONSHIP, REL_RESOURCE_GRP);

      /*
       * Request database service with requestBody for listing resource group relationship
       */
      dbService.listResourceGroupRelationship(requestBody, dbhandler -> {
        if (dbhandler.succeeded()) {
          LOGGER.info(
              "List of resourceGroup belonging to resource ".concat(dbhandler.result().toString()));
          response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
              .setStatusCode(200).end(dbhandler.result().toString());
        } else if (dbhandler.failed()) {
          LOGGER.error(
              "Issue in listing resourceGroup relationship ".concat(dbhandler.cause().toString()));
          response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
              .setStatusCode(400).end(dbhandler.cause().getLocalizedMessage());
        }
      });
    } else {
      LOGGER.error("Issue in path parameter");
      response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
          .setStatusCode(400).end(new ResponseHandler.Builder().withStatus(INVALID_SYNTAX)
              .build().toJsonString());
    }
  }


  /**
   * Queries the database and returns all resource servers belonging to an item.
   *
   * @param routingContext Handles web request in Vert.x web
   */
  public void listResourceServerRelationship(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    JsonObject queryJson = new JsonObject();
    String instanceID = routingContext.request().host();
    String id = routingContext.request().getParam(ID);
    queryJson.put(INSTANCE_ID_KEY, instanceID).put(ID, id)
        .put(RELATIONSHIP, REL_RESOURCE_SVR);
    LOGGER.info("search query : " + queryJson);
    dbService.listResourceServerRelationship(queryJson, handler -> {
      if (handler.succeeded()) {
        JsonObject resultJson = handler.result();
        String status = resultJson.getString(STATUS);
        if (status.equalsIgnoreCase(SUCCESS)) {
          response.setStatusCode(200);
        } else {
          response.setStatusCode(400);
        }
        response.headers().add(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .add(HEADER_CONTENT_LENGTH, String.valueOf(resultJson.toString().length()));
        response.write(resultJson.toString());
        LOGGER.info("response : " + resultJson);
        response.end();
      } else if (handler.failed()) {
        LOGGER.error(handler.cause().getMessage());
        response.headers().add(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);
        response.setStatusCode(400);
        response.end(handler.cause().getLocalizedMessage());
      }
    });
  }

  /**
   * Queries the database and returns provider of an item.
   *
   * @param routingContext Handles web request in Vert.x web
   */
  public void listProviderRelationship(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    JsonObject queryJson = new JsonObject();
    String instanceID = routingContext.request().host();
    String id = routingContext.request().getParam(ID);
    queryJson
        .put(INSTANCE_ID_KEY, instanceID)
        .put(ID, id)
        .put(RELATIONSHIP, REL_PROVIDER);
    LOGGER.info("search query : " + queryJson);
    dbService.listProviderRelationship(queryJson, handler -> {
      if (handler.succeeded()) {
        JsonObject resultJson = handler.result();
        String status = resultJson.getString(STATUS);
        if (status.equalsIgnoreCase(SUCCESS)) {
          response.setStatusCode(200);
        } else {
          response.setStatusCode(400);
        }
        response.headers().add(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .add(HEADER_CONTENT_LENGTH, String.valueOf(resultJson.toString().length()));
        response.write(resultJson.toString());
        LOGGER.info("response : " + resultJson);
        response.end();
      } else if (handler.failed()) {
        LOGGER.error(handler.cause().getMessage());
        response.headers().add(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);
        response.setStatusCode(400);
        response.end(handler.cause().getLocalizedMessage());
      }
    });
  }

  /**
   * Queries the database and returns data model of an item.
   *
   * @param routingContext Handles web request in Vert.x web
   */
  public void listTypes(RoutingContext routingContext) {
    HttpServerResponse response = routingContext.response();
    JsonObject queryJson = new JsonObject();
    String instanceID = routingContext.request().host();
    String id = routingContext.request().getParam(ID);
    queryJson
        .put(INSTANCE_ID_KEY, instanceID)
        .put(ID, id)
        .put(RELATIONSHIP, REL_TYPE);
    LOGGER.info("search query : " + queryJson);
    dbService.listTypes(
        queryJson,
        handler -> {
          if (handler.succeeded()) {
            JsonObject resultJson = handler.result();
            String status = resultJson.getString(STATUS);
            if (status.equalsIgnoreCase(SUCCESS)) {
              response.setStatusCode(200);
            } else {
              response.setStatusCode(400);
            }
            response
                .headers()
                .add(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
                .add(
                    HEADER_CONTENT_LENGTH,
                    String.valueOf(resultJson.toString().length()));
            response.write(resultJson.toString());
            LOGGER.info("response : " + resultJson);
            response.end();
          } else if (handler.failed()) {
            LOGGER.error(handler.cause().getMessage());
            response.headers().add(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON);
            response.setStatusCode(400);
            response.end(handler.cause().getLocalizedMessage());
          }
        });
  }

  /**
   * Relationship search of the cataloque items.
   *
   * @param routingContext Handles web request in Vert.x web
   */
  public void relSearch(RoutingContext routingContext) {

    LOGGER.info("Relationship search");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();
    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(HEADER_HOST);

    /* Collection of query parameters from HTTP request */
    MultiMap queryParameters = routingContext.queryParams();

    /* validating proper actual query parameters from request */
    if (request.getParam(RELATIONSHIP) != null && request.getParam(VALUE) != null) {

      /* converting query parameters in json */
      requestBody = QueryMapper.map2Json(queryParameters);

      if (requestBody != null) {

        /* Populating query mapper */
        requestBody.put(INSTANCE_ID_KEY, instanceID);

        /* Request database service with requestBody for listing domains */
        dbService.relSearch(requestBody, dbhandler -> {
          if (dbhandler.succeeded()) {
            LOGGER.info(
                "Relationship search completed, response: ".concat(dbhandler.result().toString()));
            response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON).setStatusCode(200)
                .end(dbhandler.result().toString());
          } else if (dbhandler.failed()) {
            LOGGER.error("Issue in relationship search ".concat(dbhandler.cause().toString()));
            response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON).setStatusCode(400)
                .end(dbhandler.cause().getLocalizedMessage());
          }
        });
      } else {
        LOGGER.error("Invalid request query parameters");
        response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON).setStatusCode(400)
            .end(new ResponseHandler.Builder().withStatus(INVALID_VALUE).build().toJsonString());
      }
    } else {
      LOGGER.error("Invalid request query parameters");
      response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON).setStatusCode(400)
          .end(new ResponseHandler.Builder().withStatus(INVALID_SYNTAX).build().toJsonString());
    }
  }
}
