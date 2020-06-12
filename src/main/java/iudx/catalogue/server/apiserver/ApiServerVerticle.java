package iudx.catalogue.server.apiserver;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map.Entry;
import java.util.Properties;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.EventBusService;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import iudx.catalogue.server.authenticator.AuthenticationService;
import iudx.catalogue.server.database.DatabaseService;
import iudx.catalogue.server.onboarder.OnboarderService;
import iudx.catalogue.server.validator.ValidatorService;

/**
 * The Catalogue Server API Verticle.
 * <h1>Catalogue Server API Verticle</h1>
 * <p>
 * The API Server verticle implements the IUDX Catalogue Server APIs. It handles the API requests
 * from the clients and interacts with the associated Service to respond.
 * </p>
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

  private static final Logger logger = LoggerFactory.getLogger(ApiServerVerticle.class);
  private Vertx vertx;
  private ClusterManager mgr;
  private VertxOptions options;
  private ServiceDiscovery discovery;
  private DatabaseService database;
  private OnboarderService onboarder;
  private ValidatorService validator;
  private AuthenticationService authenticator;
  private HttpServer server;
  @SuppressWarnings("unused")
  private Router router;
  private Properties properties;
  private InputStream inputstream;
  private final int port = 8443;
  private String keystore;
  private String keystorePassword;
  private String basePath = "/iudx/cat/v1";

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, reads the
   * configuration, obtains a proxy for the Event bus services exposed through service discovery,
   * start an HTTPs server at port 8443.
   * 
   * @throws Exception which is a startup exception
   */

  @Override
  public void start() throws Exception {

    /* Create a reference to HazelcastClusterManager. */

    mgr = new HazelcastClusterManager();
    options = new VertxOptions().setClusterManager(mgr);

    /* Create or Join a Vert.x Cluster. */

    Vertx.clusteredVertx(options, res -> {
      if (res.succeeded()) {

        vertx = res.result();
        router = Router.router(vertx);
        properties = new Properties();
        inputstream = null;

        /* Define the APIs, methods, endpoints and associated methods. */

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        router.route("/apis/*").handler(StaticHandler.create());

        /* New item create */
        router.post(basePath.concat("/item")).handler(this::createItem);

        /* Search for an item */
        router.get(basePath.concat("/search")).handler(this::searchItem);

        /* list all the tags */
        router.get(basePath.concat("/tags")).handler(this::listTags);

        /* list all the domains */
        router.get(basePath.concat("/domains")).handler(this::listDomains);

        /* list all the cities associated with the cataloque instance */
        router.get(basePath.concat("/cities")).handler(this::listCities);

        /* list all the resource server associated with the cataloque instance */
        router.get(basePath.concat("/resourceservers")).handler(this::listResourceServers);

        /* list all the providers associated with the cataloque instance */
        router.get(basePath.concat("/providers")).handler(this::listProviders);

        /* list all the resource groups associated with the cataloque instance */
        router.get(basePath.concat("/resourcegroups")).handler(this::listResourceGroups);

        /*
         * Update an item in the database using itemId [itemId=ResourceItem, ResourceGroupItem,
         * ResourceServerItem, ProviderItem, DataDescriptorItem]
         */
        router
            .patch(basePath.concat("/item/:resItem/:resGrpItem/:resSvrItem/:pvdrItem/:dataDesItem"))
            .handler(this::updateItem);

        /* Delete an item from database using itemId */
        router
            .delete(
                basePath.concat("/item/:resItem/:resGrpItem/:resSvrItem/:pvdrItem/:dataDesItem"))
            .handler(this::deleteItem);

        /* list the item from database using itemId */
        router
            .get(basePath.concat("/items/:resItem/:resGrpItem/:resSvrItem/:pvdrItem/:dataDesItem"))
            .handler(this::listItems);

        /* Read the configuration and set the HTTPs server properties. */

        try {

          inputstream = new FileInputStream("config.properties");
          properties.load(inputstream);

          keystore = properties.getProperty("keystore");
          keystorePassword = properties.getProperty("keystorePassword");

        } catch (Exception ex) {

          logger.info(ex.toString());

        }

        /* Setup the HTTPs server properties, APIs and port. */

        server = vertx.createHttpServer(new HttpServerOptions().setSsl(true)
            .setKeyStoreOptions(new JksOptions().setPath(keystore).setPassword(keystorePassword)));

        server.requestHandler(router).listen(port);

        /* Get a handler for the Service Discovery interface. */

        discovery = ServiceDiscovery.create(vertx);

        /* Get a handler for the DatabaseService from Service Discovery interface. */

        EventBusService.getProxy(discovery, DatabaseService.class,
            databaseServiceDiscoveryHandler -> {
              if (databaseServiceDiscoveryHandler.succeeded()) {
                database = databaseServiceDiscoveryHandler.result();
                logger.info(
                    "\n +++++++ Service Discovery  Success. +++++++ \n +++++++ Service name is : "
                        + database.getClass().getName() + " +++++++ ");
              } else {
                logger.info("\n +++++++ Service Discovery Failed. +++++++ ");
              }
            });

        /* Get a handler for the OnboarderService from Service Discovery interface. */

        EventBusService.getProxy(discovery, OnboarderService.class,
            onboarderServiceDiscoveryHandler -> {
              if (onboarderServiceDiscoveryHandler.succeeded()) {
                onboarder = onboarderServiceDiscoveryHandler.result();
                logger.info(
                    "\n +++++++ Service Discovery  Success. +++++++ \n +++++++ Service name is : "
                        + onboarder.getClass().getName() + " +++++++ ");
              } else {
                logger.info("\n +++++++ Service Discovery Failed. +++++++ ");
              }
            });

        /* Get a handler for the ValidatorService from Service Discovery interface. */

        EventBusService.getProxy(discovery, ValidatorService.class,
            validatorServiceDiscoveryHandler -> {
              if (validatorServiceDiscoveryHandler.succeeded()) {
                validator = validatorServiceDiscoveryHandler.result();
                logger.info(
                    "\n +++++++ Service Discovery  Success. +++++++ \n +++++++ Service name is : "
                        + validator.getClass().getName() + " +++++++ ");
              } else {
                logger.info("\n +++++++ Service Discovery Failed. +++++++ ");
              }
            });

        /* Get a handler for the AuthenticationService from Service Discovery interface. */

        EventBusService.getProxy(discovery, AuthenticationService.class,
            authenticatorServiceDiscoveryHandler -> {
              if (authenticatorServiceDiscoveryHandler.succeeded()) {
                authenticator = authenticatorServiceDiscoveryHandler.result();
                logger.info(
                    "\n +++++++ Service Discovery  Success. +++++++ \n +++++++ Service name is : "
                        + authenticator.getClass().getName() + " +++++++ ");
              } else {
                logger.info("\n +++++++ Service Discovery Failed. +++++++ ");
              }
            });

      }
    });
  }

  /**
   * Creates a new item in database.
   * 
   * @param routingContext handles web requests in Vert.x Web
   */
  private void createItem(RoutingContext routingContext) {

    logger.info("Creating an item");

    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    JsonObject authenticationInfo = new JsonObject();
    JsonObject requestBody = routingContext.getBodyAsJson();

    if (request.headers().contains("token")) {
      authenticationInfo.put("token", request.getHeader("token"));

      /* Authenticating the request */
      authenticator.tokenInterospect(requestBody, authenticationInfo, authhandler -> {
        if (authhandler.succeeded()) {
          logger.info(
              "Authenticating item creation request ".concat(authhandler.result().toString()));
          /* Validating the request */
          validator.validateItem(requestBody, valhandler -> {
            if (valhandler.succeeded()) {
              logger.info("Item creation validated".concat(authhandler.result().toString()));
              /* Requesting database service, creating a item */
              database.createItem(requestBody, dbhandler -> {
                if (dbhandler.succeeded()) {
                  logger.info("Item created".concat(dbhandler.result().toString()));
                  response.putHeader("content-type", "application/json").setStatusCode(201)
                      .end(dbhandler.result().toString());
                } else if (dbhandler.failed()) {
                  logger.error("Item creation failed".concat(dbhandler.cause().toString()));
                  response.putHeader("content-type", "application-json").setStatusCode(500)
                      .end(dbhandler.cause().toString());
                }
              });
            } else if (valhandler.failed()) {
              logger.error("Item validation failed".concat(valhandler.cause().toString()));
              response.putHeader("content-type", "application/json").setStatusCode(500)
                  .end(valhandler.cause().toString());
            }
          });
        } else if (authhandler.failed()) {
          logger.error("Unathorized request".concat(authhandler.cause().toString()));
          response.putHeader("content-type", "application/json").setStatusCode(401)
              .end(authhandler.cause().toString());
        }
      });
    } else {
      logger.error("Invalid 'token' header");
      response.putHeader("content-type", "application-json").setStatusCode(400)
          .end(new ResponseHandler.Builder().withStatus("invalidHeader").build().toJsonString());
    }
  }

  /**
   * Updates a already created item in the database. Endpoint: PATCH /iudx/cat/v1/update/itemId
   * itemId=ResourceItem/ResourceGroupItem/ResourceServerItem/ProviderItem/DataDescriptorItem
   * 
   * @param routingContext handles web requests in Vert.x Web
   */
  private void updateItem(RoutingContext routingContext) {

    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    JsonObject authenticationInfo = new JsonObject();
    JsonObject requestBody = routingContext.getBodyAsJson();


    String itemId = routingContext.pathParam("resItem").concat("/")
        .concat(routingContext.pathParam("resGrpItem").concat("/")
            .concat(routingContext.pathParam("resSvrItem")).concat("/")
            .concat(routingContext.pathParam("pvdrItem").concat("/"))
            .concat(routingContext.pathParam("dataDesItem")));

    logger.info("Updating an item, Id: ".concat(itemId));

    if (itemId.equals(requestBody.getString("id").strip())) {
      if (request.headers().contains("token")) {
        authenticationInfo.put("token", request.getHeader("token"));

        /* Authenticating the request */
        authenticator.tokenInterospect(requestBody, authenticationInfo, authhandler -> {
          if (authhandler.succeeded()) {
            logger.info(
                "Authenticating item update request ".concat(authhandler.result().toString()));
            /* Validating the request */
            validator.validateItem(requestBody, valhandler -> {
              if (valhandler.succeeded()) {
                logger.info("Item update validated ".concat(authhandler.result().toString()));
                /* Requesting database service, creating a item */
                database.updateItem(requestBody, dbhandler -> {
                  if (dbhandler.succeeded()) {
                    logger.info("Item updated ".concat(dbhandler.result().toString()));
                    response.putHeader("content-type", "application/json").setStatusCode(200)
                        .end(dbhandler.result().toString());
                  } else if (dbhandler.failed()) {
                    logger.error("Item update failed ".concat(dbhandler.cause().toString()));
                    response.putHeader("content-type", "application-json").setStatusCode(500)
                        .end(dbhandler.cause().toString());
                  }
                });
              } else if (valhandler.failed()) {
                logger.error("Item validation failed ".concat(valhandler.cause().toString()));
                response.putHeader("content-type", "application/json").setStatusCode(500)
                    .end(valhandler.cause().toString());
              }
            });
          } else if (authhandler.failed()) {
            logger.error("Unathorized request ".concat(authhandler.cause().toString()));
            response.putHeader("content-type", "application/json").setStatusCode(401)
                .end(authhandler.cause().toString());
          }
        });
      } else {
        logger.error("Invalid 'token' header");
        response.putHeader("content-type", "application-json").setStatusCode(400)
            .end(new ResponseHandler.Builder().withStatus("invalidHeader").build().toJsonString());
      }
    } else {
      logger.error("Mismatch 'id' in query parameter and request body");
      response.putHeader("content-type", "application-json").setStatusCode(400).end(
          new ResponseHandler.Builder().withStatus("invalidQueryParameter").build().toJsonString());
    }
  }

  /**
   * Deletes a created item in the database. Endpoint: DELETE /iudx/cat/v1/delete/itemId
   * itemId=ResourceItem/ResourceGroupItem/ResourceServerItem/ProviderItem/DataDescriptorItem
   * 
   * @param routingContext handles web requests in Vert.x Web
   */
  private void deleteItem(RoutingContext routingContext) {
    HttpServerRequest request = routingContext.request();
    HttpServerResponse response = routingContext.response();
    JsonObject authenticationInfo = new JsonObject();
    JsonObject requestBody = new JsonObject();

    String itemId = routingContext.pathParam("resItem").concat("/")
        .concat(routingContext.pathParam("resGrpItem").concat("/")
            .concat(routingContext.pathParam("resSvrItem")).concat("/")
            .concat(routingContext.pathParam("pvdrItem").concat("/"))
            .concat(routingContext.pathParam("dataDesItem")));
    requestBody.put("id", itemId);

    logger.info("Deleting an item, Id: ".concat(itemId));

    if (request.headers().contains("token")) {
      authenticationInfo.put("token", request.getHeader("token"));

      /* Authenticating the request */
      authenticator.tokenInterospect(null, authenticationInfo, authhandler -> {
        if (authhandler.succeeded()) {
          logger.info("Authenticating item delete request".concat(authhandler.result().toString()));
          /* Requesting database service, creating a item */
          database.deleteItem(requestBody, dbhandler -> {
            if (dbhandler.succeeded()) {
              logger.info("Item deleted".concat(dbhandler.result().toString()));
              response.putHeader("content-type", "application/json").setStatusCode(200)
                  .end(dbhandler.result().toString());
            } else if (dbhandler.failed()) {
              logger.error("Item deletion failed".concat(dbhandler.cause().toString()));
              response.putHeader("content-type", "application-json").setStatusCode(400)
                  .end(dbhandler.cause().toString());
            }
          });
        } else if (authhandler.failed()) {
          logger.error("Unathorized request".concat(authhandler.cause().toString()));
          response.putHeader("content-type", "application/json").setStatusCode(401)
              .end(authhandler.cause().toString());
        }
      });
    } else {
      logger.error("Invalid 'token' header");
      response.putHeader("content-type", "application-json").setStatusCode(400)
          .end(new ResponseHandler.Builder().withStatus("invalidHeader").build().toJsonString());
    }
  }

  /**
   * Geo Spatial property (Circle,Polygon) based database search. Validates the request query
   * params.
   * 
   * @param routingContext handles web requests in Vert.x Web
   */
  private void searchItem(RoutingContext routingContext) {

    logger.info("Searching the database for Item");

    HttpServerResponse response = routingContext.response();
    MultiMap queryParameters = routingContext.queryParams();
    JsonObject requestBody = new JsonObject();

    /* Circle and Polygon based item search */
    if (queryParameters.contains("geoproperty") && !queryParameters.get("geoproperty").isBlank()) {

      if ("Point".equals(queryParameters.get("geometry"))
          || "Polygon".equals(queryParameters.get("geometry"))) {

        requestBody = map2Json(queryParameters);
        if (requestBody != null) {
          database.searchQuery(requestBody, dbhandler -> {
            if (dbhandler.succeeded()) {
              logger.info("Search completed ".concat(dbhandler.result().toString()));
              response.putHeader("content-type", "application/json").setStatusCode(200)
                  .end(dbhandler.result().toString());
            } else if (dbhandler.failed()) {
              logger.error("Issue in Item search ".concat(dbhandler.cause().toString()));
              response.putHeader("content-type", "application-json").setStatusCode(400)
                  .end(dbhandler.cause().toString());
            }
          });
        } else {
          response.putHeader("content-type", "application/json").setStatusCode(400)
              .end(new ResponseHandler.Builder().withStatus("invalidValue").build().toJsonString());
        }
      } else {
        logger.error(
            "Invalid Query parameter Values, Expected: 'geometry = Point|Polygon|linestring|bbox'");
        response.putHeader("content-type", "application/json").setStatusCode(400)
            .end(new ResponseHandler.Builder().withStatus("invalidValue").build().toJsonString());
      }
    } else {
      logger.error("Invalid Query parameter values, Expected: 'geopropery'");
      response.putHeader("content-type", "application/json").setStatusCode(400)
          .end(new ResponseHandler.Builder().withStatus("invalidSyntax").build().toJsonString());
    }
  }

  /**
   * List the items from database using itemId.
   * 
   * @param routingContext handles web requests in Vert.x Web
   */
  private void listItems(RoutingContext routingContext) {
    // TODO: Incomplete
    logger.info("Listing items from database");

    // HttpServerResponse response = routingContext.response();
    JsonObject requestBody = new JsonObject();

    // Map<String, String> map = routingContext.pathParams();

    String itemId = routingContext.pathParam("resItem").concat("/")
        .concat(routingContext.pathParam("resGrpItem").concat("/")
            .concat(routingContext.pathParam("resSvrItem")).concat("/")
            .concat(routingContext.pathParam("pvdrItem").concat("/"))
            .concat(routingContext.pathParam("dataDesItem")));
    requestBody.put("id", itemId);

  }

  /**
   * Get the list of tags for a catalogue instance.
   * 
   * @param routingContext handles web requests in Vert.x Web
   */
  private void listTags(RoutingContext routingContext) {

    logger.info("Listing tags of a cataloque instance");

    HttpServerResponse response = routingContext.response();
    JsonObject requestBody = new JsonObject();

    database.listTags(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        logger.info("List of tags ".concat(dbhandler.result().toString()));
        response.putHeader("content-type", "application/json").setStatusCode(200)
            .end(dbhandler.result().toString());
      } else if (dbhandler.failed()) {
        logger.error("Issue in listing tags ".concat(dbhandler.cause().toString()));
        response.putHeader("content-type", "application-json").setStatusCode(400)
            .end(dbhandler.cause().toString());
      }
    });
  }

  /**
   * Get a list of domains for a cataloque instance.
   * 
   * @param routingContext handles web requests in Vert.x Web
   */
  private void listDomains(RoutingContext routingContext) {

    logger.info("Listing domains of a cataloque instance");

    HttpServerResponse response = routingContext.response();
    JsonObject requestBody = new JsonObject();

    database.listDomains(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        logger.info("List of domains ".concat(dbhandler.result().toString()));
        response.putHeader("content-type", "application/json").setStatusCode(200)
            .end(dbhandler.result().toString());
      } else if (dbhandler.failed()) {
        logger.error("Issue in listing domains ".concat(dbhandler.cause().toString()));
        response.putHeader("content-type", "application-json").setStatusCode(400)
            .end(dbhandler.cause().toString());
      }
    });
  }

  /**
   * Get the list of cities and the catalogue instance ID.
   * 
   * @param routingContext handles web requests in Vert.x Web
   */
  private void listCities(RoutingContext routingContext) {

    logger.info("Listing cities of a cataloque instance");

    HttpServerResponse response = routingContext.response();
    JsonObject requestBody = new JsonObject();

    database.listCities(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        logger.info("List of cities ".concat(dbhandler.result().toString()));
        response.putHeader("content-type", "application/json").setStatusCode(200)
            .end(dbhandler.result().toString());
      } else if (dbhandler.failed()) {
        logger.error("Issue in listing cities ".concat(dbhandler.cause().toString()));
        response.putHeader("content-type", "application-json").setStatusCode(400)
            .end(dbhandler.cause().toString());
      }
    });
  }

  /**
   * Get the list of resourceServers for a catalogue instance.
   * 
   * @param routingContext handles web requests in Vert.x Web
   */
  private void listResourceServers(RoutingContext routingContext) {

    logger.info("Listing resource servers of a cataloque instance");

    HttpServerResponse response = routingContext.response();
    JsonObject requestBody = new JsonObject();

    database.listResourceServers(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        logger.info("List of resource servers ".concat(dbhandler.result().toString()));
        response.putHeader("content-type", "application/json").setStatusCode(200)
            .end(dbhandler.result().toString());
      } else if (dbhandler.failed()) {
        logger.error("Issue in listing resource servers ".concat(dbhandler.cause().toString()));
        response.putHeader("content-type", "application-json").setStatusCode(400)
            .end(dbhandler.cause().toString());
      }
    });
  }

  /**
   * Get the list of providers for a catalogue instance.
   * 
   * @param routingContext handles web requests in Vert.x Web
   */
  private void listProviders(RoutingContext routingContext) {
    // TODO: database handler listProviders not available, [Important talk to team]
    logger.info("Listing providers of a cataloque instance");

    HttpServerResponse response = routingContext.response();
    JsonObject requestBody = new JsonObject();


    database.listProviders(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        logger.info("List of providers ".concat(dbhandler.result().toString()));
        response.putHeader("content-type", "application/json").setStatusCode(200)
            .end(dbhandler.result().toString());
      } else if (dbhandler.failed()) {
        logger.error("Issue in listing providers ".concat(dbhandler.cause().toString()));
        response.putHeader("content-type", "application-json").setStatusCode(400)
            .end(dbhandler.cause().toString());
      }
    });
  }

  /**
   * Get the list of resource groups for a catalogue instance.
   * 
   * @param routingContext handles web requests in Vert.x Web
   */
  private void listResourceGroups(RoutingContext routingContext) {
    // TODO: database handler listResourceGroups not available, [Important talk to team]
    logger.info("Listing resource groups of a cataloque instance");

    HttpServerResponse response = routingContext.response();
    JsonObject requestBody = new JsonObject();


    database.listResourceGroups(requestBody, dbhandler -> {
      if (dbhandler.succeeded()) {
        logger.info("List of resource groups ".concat(dbhandler.result().toString()));
        response.putHeader("content-type", "application/json").setStatusCode(200)
            .end(dbhandler.result().toString());
      } else if (dbhandler.failed()) {
        logger.error("Issue in listing resource groups ".concat(dbhandler.cause().toString()));
        response.putHeader("content-type", "application-json").setStatusCode(400)
            .end(dbhandler.cause().toString());
      }
    });
  }

  /**
   * Converts the MultiMap to JsonObject. Checks/validates the value of JsonArray.
   * 
   * @param queryParameters is a MultiMap of request query parameters
   * @return jsonObject of MultiMap query parameters
   */
  private JsonObject map2Json(MultiMap queryParameters) {
    JsonObject jsonBody = new JsonObject();

    for (Entry<String, String> entry : queryParameters.entries()) {
      if (!entry.getValue().startsWith("[") && !entry.getValue().endsWith("]")) {
        jsonBody.put(entry.getKey(), entry.getValue());
      } else {
        try {
          jsonBody.put(entry.getKey(), new JsonArray(entry.getValue()));
        } catch (DecodeException decodeException) {
          logger.error("Invalid Json value ".concat(decodeException.toString()));
          return null;
        }
      }
    }
    return jsonBody;
  }
}
