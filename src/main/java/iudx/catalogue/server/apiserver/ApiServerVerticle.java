package iudx.catalogue.server.apiserver;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
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

        /* list all the tags */
        router.get(ROUTE_TAGS).handler(this::listTags);
        /* list all the domains */
        router.get(ROUTE_DOMAINS).handler(this::listDomains);
        /* list all the cities associated with the cataloque instance */
        router.get(ROUTE_CITIES).handler(this::listCities);
        /* list all the resource server associated with the cataloque instance */
        router.get(ROUTE_RESOURCE_SERVERS).handler(this::listResourceServers);
        /* list all the providers associated with the cataloque instance */
        router.get(ROUTE_PROVIDERS).handler(this::listProviders);
        /* list all the resource groups associated with the cataloque instance */
        router.get(ROUTE_RESOURCE_GROUPS).handler(this::listResourceGroups);
        /* list the item from database using itemId */
        router.get(ROUTE_LIST_ITEMS).handler(this::listItems);
        /* Get list types with the database for an item */
        router.getWithRegex(ROUTE_DATA_TYPE).handler(this::listTypes);

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


        /**
         * Start server 
         */
        server.requestHandler(router).listen(PORT);


      }
    });
  }

  public void search(RoutingContext routingContext) {

  }

  /**
   * List the items from database using itemId.
   *
   * @param routingContext handles web requests in Vert.x Web
   */
  private void listItems(RoutingContext routingContext) {

    LOGGER.info("Listing items from database");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(HEADER_HOST);

    /* Building complete itemID from HTTP request path parameters */
    String itemId =
        routingContext
            .pathParam(RESOURCE_ITEM)
            .concat("/")
            .concat(
                routingContext
                    .pathParam(RESOURCE_GRP_ITEM)
                    .concat("/")
                    .concat(routingContext.pathParam(RESOURCE_SVR_ITEM))
                    .concat("/")
                    .concat(routingContext.pathParam(PROVIDER_ITEM).concat("/"))
                    .concat(routingContext.pathParam(DATA_DES_ITEM)));

    /* Populating query mapper */
    requestBody.put(ID, itemId);
    requestBody.put(INSTANCE_ID_KEY, instanceID);

    /* Databse service call for listing item */
    dbService.listItem(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        LOGGER.info("List of items ".concat(dbhandler.result().toString()));
        response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .setStatusCode(200).end(dbhandler.result().toString());
      } else if (dbhandler.failed()) {
        LOGGER.error("Issue in listing items ".concat(dbhandler.cause().toString()));
        response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .setStatusCode(400).end(dbhandler.cause().toString());
      }
    });
  }

  /**
   * Get the list of tags for a catalogue instance.
   *
   * @param routingContext handles web requests in Vert.x Web
   */
  private void listTags(RoutingContext routingContext) {

    LOGGER.info("Listing tags of a cataloque instance");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(HEADER_HOST);

    /* Populating query mapper */
    requestBody.put(INSTANCE_ID_KEY, instanceID);

    /* Request database service with requestBody for listing tags */
    dbService.listTags(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        LOGGER.info("List of tags ".concat(dbhandler.result().toString()));
        response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .setStatusCode(200).end(dbhandler.result().toString());
      } else if (dbhandler.failed()) {
        LOGGER.error("Issue in listing tags ".concat(dbhandler.cause().toString()));
        response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .setStatusCode(400).end(dbhandler.cause().toString());
      }
    });
  }

  /**
   * Get a list of domains for a cataloque instance.
   *
   * @param routingContext handles web requests in Vert.x Web
   */
  private void listDomains(RoutingContext routingContext) {

    LOGGER.info("Listing domains of a cataloque instance");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(HEADER_HOST);

    /* Populating query mapper */
    requestBody.put(INSTANCE_ID_KEY, instanceID);

    /* Request database service with requestBody for listing domains */
    dbService.listDomains(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        LOGGER.info("List of domains ".concat(dbhandler.result().toString()));
        response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .setStatusCode(200).end(dbhandler.result().toString());
      } else if (dbhandler.failed()) {
        LOGGER.error("Issue in listing domains ".concat(dbhandler.cause().toString()));
        response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .setStatusCode(400).end(dbhandler.cause().toString());
      }
    });
  }

  /**
   * Get the list of cities and the catalogue instance ID.
   *
   * @param routingContext handles web requests in Vert.x Web
   */
  private void listCities(RoutingContext routingContext) {

    LOGGER.info("Listing cities of a cataloque instance");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(HEADER_HOST);

    /* Populating query mapper */
    requestBody.put(INSTANCE_ID_KEY, instanceID);

    /* Request database service with requestBody for listing cities */
    dbService.listCities(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        LOGGER.info("List of cities ".concat(dbhandler.result().toString()));
        response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .setStatusCode(200).end(dbhandler.result().toString());
      } else if (dbhandler.failed()) {
        LOGGER.error("Issue in listing cities ".concat(dbhandler.cause().toString()));
        response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .setStatusCode(400).end(dbhandler.cause().toString());
      }
    });
  }

  /**
   * Get the list of resourceServers for a catalogue instance.
   *
   * @param routingContext handles web requests in Vert.x Web
   */
  private void listResourceServers(RoutingContext routingContext) {

    LOGGER.info("Listing resource servers of a cataloque instance");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(HEADER_HOST);

    /* Populating query mapper */
    requestBody.put(INSTANCE_ID_KEY, instanceID);

    /* Request database service with requestBody for listing resource servers */
    dbService.listResourceServers(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        LOGGER.info("List of resource servers ".concat(dbhandler.result().toString()));
        response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .setStatusCode(200).end(dbhandler.result().toString());
      } else if (dbhandler.failed()) {
        LOGGER.error("Issue in listing resource servers ".concat(dbhandler.cause().toString()));
        response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .setStatusCode(400).end(dbhandler.cause().toString());
      }
    });
  }

  /**
   * Get the list of providers for a catalogue instance.
   *
   * @param routingContext handles web requests in Vert.x Web
   */
  private void listProviders(RoutingContext routingContext) {

    LOGGER.info("Listing providers of a cataloque instance");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(HEADER_HOST);

    /* Populating query mapper */
    requestBody.put(INSTANCE_ID_KEY, instanceID);

    /* Request database service with requestBody for listing providers */
    dbService.listProviders(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        LOGGER.info("List of providers ".concat(dbhandler.result().toString()));
        response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .setStatusCode(200).end(dbhandler.result().toString());
      } else if (dbhandler.failed()) {
        LOGGER.error("Issue in listing providers ".concat(dbhandler.cause().toString()));
        response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .setStatusCode(400).end(dbhandler.cause().toString());
      }
    });
  }

  /**
   * Get the list of resource groups for a catalogue instance.
   *
   * @param routingContext handles web requests in Vert.x Web
   */
  private void listResourceGroups(RoutingContext routingContext) {

    LOGGER.info("Listing resource groups of a cataloque instance");

    /* Handles HTTP request from client */
    HttpServerRequest request = routingContext.request();

    /* Handles HTTP response from server to client */
    HttpServerResponse response = routingContext.response();

    JsonObject requestBody = new JsonObject();

    /* HTTP request instance/host details */
    String instanceID = request.getHeader(HEADER_HOST);

    /* Populating query mapper */
    requestBody.put(INSTANCE_ID_KEY, instanceID);

    /* Request database service with requestBody for listing resource groups */
    dbService.listResourceGroups(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        LOGGER.info("List of resource groups ".concat(dbhandler.result().toString()));
        response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .setStatusCode(200).end(dbhandler.result().toString());
      } else if (dbhandler.failed()) {
        LOGGER.error("Issue in listing resource groups ".concat(dbhandler.cause().toString()));
        response.putHeader(HEADER_CONTENT_TYPE, MIME_APPLICATION_JSON)
            .setStatusCode(400).end(dbhandler.cause().toString());
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
        response.headers().add(HEADER_CONTENT_TYPE, TEXT);
        response.setStatusCode(500);
        response.end(INTERNAL_SERVER_ERROR);
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
    dbService.listProviderRelationship(
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
            response.headers().add(HEADER_CONTENT_TYPE, TEXT);
            response.setStatusCode(500);
            response.end(INTERNAL_SERVER_ERROR);
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
            response.headers().add(HEADER_CONTENT_TYPE, TEXT);
            response.setStatusCode(500);
            response.end(INTERNAL_SERVER_ERROR);
          }
        });
  }

}
