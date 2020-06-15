package iudx.catalogue.server.apiserver;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.EventBusService;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import iudx.catalogue.server.authenticator.AuthenticationService;
import iudx.catalogue.server.database.DatabaseService;
import iudx.catalogue.server.onboarder.OnboarderService;
import iudx.catalogue.server.validator.ValidatorService;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

/**
 * The Catalogue Server API Verticle.
 * <h1>Catalogue Server API Verticle</h1>
 * <p>
 * The API Server verticle implements the IUDX Catalogue Server APIs. It handles
 * the API requests from the clients and interacts with the associated Service
 * to respond.
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
	private Router router;
	private Properties properties;
	private InputStream inputstream;
	private final int port = 8443;
	private String keystore;
	private String keystorePassword;

	/**
	 * This method is used to start the Verticle. It deploys a verticle in a
	 * cluster, reads the configuration, obtains a proxy for the Event bus services
	 * exposed through service discovery, start an HTTPs server at port 8443.
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

				router = Router.router(vertx);
				router.route("/apis/*").handler(StaticHandler.create());
				router.route().handler(BodyHandler.create());
				router.get("/iudx/cat/v1/search").handler(this::search);
				router.get("/iudx/cat/v1/ui/cities").handler(this::getCities);
				router.post("/iudx/cat/v1/ui/cities").handler(this::setCities);
				router.put("/iudx/cat/v1/ui/cities").handler(this::updateCities);
				router.get("/iudx/cat/v1/ui/config").handler(this::getConfig);
				router.post("/iudx/cat/v1/ui/config").handler(this::setConfig);
				router.delete("/iudx/cat/v1/ui/config").handler(this::deleteConfig);
				router.put("/iudx/cat/v1/ui/config").handler(this::updateConfig);
				router.patch("/iudx/cat/v1/ui/config").handler(this::appendConfig);

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

				EventBusService.getProxy(discovery, DatabaseService.class, databaseServiceDiscoveryHandler -> {
					if (databaseServiceDiscoveryHandler.succeeded()) {
						database = databaseServiceDiscoveryHandler.result();
						logger.info("\n +++++++ Service Discovery  Success. +++++++ \n +++++++ Service name is : "
								+ database.getClass().getName() + " +++++++ ");
					} else {
						logger.info("\n +++++++ Service Discovery Failed. +++++++ ");
					}
				});
				/* Get a handler for the OnboarderService from Service Discovery interface. */

				EventBusService.getProxy(discovery, OnboarderService.class, onboarderServiceDiscoveryHandler -> {
					if (onboarderServiceDiscoveryHandler.succeeded()) {
						onboarder = onboarderServiceDiscoveryHandler.result();
						logger.info("\n +++++++ Service Discovery  Success. +++++++ \n +++++++ Service name is : "
								+ onboarder.getClass().getName() + " +++++++ ");
					} else {
						logger.info("\n +++++++ Service Discovery Failed. +++++++ ");
					}
				});
				/* Get a handler for the ValidatorService from Service Discovery interface. */

				EventBusService.getProxy(discovery, ValidatorService.class, validatorServiceDiscoveryHandler -> {
					if (validatorServiceDiscoveryHandler.succeeded()) {
						validator = validatorServiceDiscoveryHandler.result();
						logger.info("\n +++++++ Service Discovery  Success. +++++++ \n +++++++ Service name is : "
								+ validator.getClass().getName() + " +++++++ ");
					} else {
						logger.info("\n +++++++ Service Discovery Failed. +++++++ ");
					}
				});
				/*
				 * Get a handler for the AuthenticationService from Service Discovery interface.
				 */

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

	public void search(RoutingContext routingContext) {
		HttpServerRequest request = routingContext.request();
		HttpServerResponse response = routingContext.response();
		System.out.println("routed to search");
		if ((request.getParam("property") == null || request.getParam("value") == null)
				&& (request.getParam("geoproperty") == null || request.getParam("georel") == null
						|| request.getParam("geometry") == null || request.getParam("coordinates") == null)
				&& (request.getParam("q") == null || request.getParam("limit") == null
						|| request.getParam("offset") == null)) {
			JsonObject json = new JsonObject();
			json.put("status", "invalidSyntax").put("results", new JsonArray());
			response.headers().add("content-type", "application/json").add("content-length",
					String.valueOf(json.toString().length()));
			response.setStatusCode(400);
			response.write(json.toString());
			response.end();
			return;
		}
		MultiMap params = request.params();
//		System.out.println(params);
		JsonObject queryJson = new JsonObject();
		String host = request.host().replaceAll("[:8443]+$", "");
		if (request.getParam("property") != null
				&& request.getParam("property").toLowerCase().contains("provider.name")) {
			queryJson.put("instanceID", host);
		} else if (request.getParam("geometry") != null && (!request.getParam("geometry").equals("bbox")
				&& !request.getParam("geometry").equals("LineString"))) {
			System.out.println("invalid geometry value");
			JsonObject json = new JsonObject();
			json.put("status", "invalidValue").put("results", new JsonArray());
			response.headers().add("content-type", "application/json").add("content-length",
					String.valueOf(json.toString().length()));
			response.setStatusCode(400);
			response.write(json.toString());
			response.end();
			return;
		}
		outerloop: for (String str : params.names()) {
			if (params.get(str).contains("[")) {
				JsonArray value = new JsonArray();
				String[] split = params.get(str).split("\\],");
				for (String s : split) {
					JsonArray json = new JsonArray();
					String[] paramValues = s.split(",");
					for (String val : paramValues) {
						if (str.equalsIgnoreCase("coordinates")) {
							try {
								double number = Double.parseDouble(
										val.strip().replaceAll("\"", "").replaceAll("\\[", "").replaceAll("\\]", ""));
								json.add(number);
							} catch (NumberFormatException e) {
								System.out.println("invalid coordinate value");
								JsonObject invalidValue = new JsonObject();
								invalidValue.put("status", "invalidValue").put("results", new JsonArray());
								response.headers().add("content-type", "application/json").add("content-length",
										String.valueOf(invalidValue.toString().length()));
								response.setStatusCode(400);
								response.write(invalidValue.toString());
								response.end();
								return;
							}
						} else {
							json.add(val.strip().replaceAll("\"", "").replaceAll("\\[", "").replaceAll("\\]", ""));
						}
					}
					if (split.length > 1 || str.equalsIgnoreCase("value")) {
						value.add(json);
					} else {
						queryJson.put(str, json);
						continue outerloop;
					}
				}
				queryJson.put(str, value);
			} else if (str.equalsIgnoreCase("limit") || str.equalsIgnoreCase("offset")) {
				int number = Integer.parseInt(params.get(str));
				queryJson.put(str, number);
			}

			else {
				queryJson.put(str,
						params.get(str).strip().replaceAll("\"", "").replaceAll("\\[", "").replaceAll("\\]", ""));
			}
		}
		System.out.println(queryJson);
		// Query queryJson to Database
		database.searchQuery(queryJson, handler -> {
			if (handler.succeeded()) {
				// store response from DB to resultJson
				JsonObject resultJson = handler.result();
//				String status = resultJson.getString("status");
				String status = "success";
				if (status.equalsIgnoreCase("success")) {
					response.setStatusCode(200);
				} else if (status.equalsIgnoreCase("partial-content")) {
					response.setStatusCode(206);
				} else {
					response.setStatusCode(400);
				}
				response.headers().add("content-type", "application/json").add("content-length",
						String.valueOf(resultJson.toString().length()));
				response.write(resultJson.toString());
				System.out.println(resultJson);
				response.end();
			} else if (handler.failed()) {
				handler.cause().getMessage();
				response.headers().add("content-type", "text");
				response.setStatusCode(500);
				response.end("Internal server error");
			}
		});
	}

	public void getCities(RoutingContext routingContext) {
		HttpServerRequest request = routingContext.request();
		HttpServerResponse response = routingContext.response();
		String domainName = request.host().replaceAll("[:8443]+$", "");
		JsonObject queryJson = new JsonObject();
		queryJson.put("instanceID", domainName).put("operation", "getCities");
		System.out.println(queryJson);
		// Query database for all cities
		database.searchQuery(queryJson, handler -> {
			if (handler.succeeded()) {
				// store response from DB to resultJson
//				JsonObject resultJson = handler.result();
//				String status = resultJson.getString("status");
				String status = "success";
				JsonObject resultJson = new JsonObject();
				if (status.equalsIgnoreCase("success")) {
					response.setStatusCode(200);
				} else {
					response.setStatusCode(400);
				}
				response.headers().add("content-type", "application/json").add("content-length",
						String.valueOf(resultJson.toString().length()));
				response.write(resultJson.toString());
				System.out.println(resultJson);
				response.end();
			} else if (handler.failed()) {
				handler.cause().getMessage();
				response.headers().add("content-type", "text");
				response.setStatusCode(500);
				response.end("Internal server error");
			}
		});
	}

	public void setCities(RoutingContext routingContext) {
		HttpServerRequest request = routingContext.request();
		HttpServerResponse response = routingContext.response();
		JsonObject queryJson = routingContext.getBodyAsJson();
		String domainName = request.host().replaceAll("[:8443]+$", "");
		queryJson.put("instanceID", domainName);
		System.out.println(queryJson);
		// Query database for setting config
		database.searchQuery(queryJson, handler -> {
			if (handler.succeeded()) {
				// store response from DB to resultJson
//				JsonObject resultJson = handler.result();
//				String status = resultJson.getString("status");
				String status = "success";
				JsonObject resultJson = new JsonObject();
				if (status.equalsIgnoreCase("success")) {
					response.setStatusCode(201);
				} else {
					response.setStatusCode(400);
				}
				response.headers().add("content-type", "application/json").add("content-length",
						String.valueOf(resultJson.toString().length()));
				response.write(resultJson.toString());
				System.out.println(resultJson);
				response.end();
			} else if (handler.failed()) {
				handler.cause().getMessage();
				response.headers().add("content-type", "text");
				response.setStatusCode(500);
				response.end("Internal server error");
			}
		});
	}

	public void updateCities(RoutingContext routingContext) {
		HttpServerRequest request = routingContext.request();
		HttpServerResponse response = routingContext.response();
		JsonObject queryJson = routingContext.getBodyAsJson();
		String domainName = request.host().replaceAll("[:8443]+$", "");
		queryJson.put("instanceID", domainName);
		System.out.println(queryJson);
		// Query database for setting config
		database.searchQuery(queryJson, handler -> {
			if (handler.succeeded()) {
				// store response from DB to resultJson
//				JsonObject resultJson = handler.result();
//				String status = resultJson.getString("status");
				String status = "success";
				JsonObject resultJson = new JsonObject();
				if (status.equalsIgnoreCase("success")) {
					response.setStatusCode(201);
				} else {
					response.setStatusCode(400);
				}
				response.headers().add("content-type", "application/json").add("content-length",
						String.valueOf(resultJson.toString().length()));
				response.write(resultJson.toString());
				System.out.println(resultJson);
				response.end();
			} else if (handler.failed()) {
				handler.cause().getMessage();
				response.headers().add("content-type", "text");
				response.setStatusCode(500);
				response.end("Internal server error");
			}
		});
	}

	public void getConfig(RoutingContext routingContext) {
		HttpServerRequest request = routingContext.request();
		HttpServerResponse response = routingContext.response();
		String domainName = request.host().replaceAll("[:8443]+$", "");
		JsonObject queryJson = new JsonObject();
		queryJson.put("instanceID", domainName).put("operation", "getConfig");
		System.out.println(queryJson);
		// Query database for config
		database.searchQuery(queryJson, handler -> {
			if (handler.succeeded()) {
				// store response from DB to resultJson
//				JsonObject resultJson = handler.result();
//				String status = resultJson.getString("status");
				String status = "success";
				JsonObject resultJson = new JsonObject();
				if (status.equalsIgnoreCase("success")) {
					response.setStatusCode(200);
				} else {
					response.setStatusCode(400);
				}
				response.headers().add("content-type", "application/json").add("content-length",
						String.valueOf(resultJson.toString().length()));
				response.write(resultJson.toString());
				System.out.println(resultJson);
				response.end();
			} else if (handler.failed()) {
				handler.cause().getMessage();
				response.headers().add("content-type", "text");
				response.setStatusCode(500);
				response.end("Internal server error");
			}
		});
	}

	public void setConfig(RoutingContext routingContext) {
		HttpServerRequest request = routingContext.request();
		HttpServerResponse response = routingContext.response();
		JsonObject queryJson = routingContext.getBodyAsJson();
		String domainName = request.host().replaceAll("[:8443]+$", "");
		queryJson.put("instanceID", domainName);
		System.out.println(queryJson);
		// Query database for setting config
		database.searchQuery(queryJson, handler -> {
			if (handler.succeeded()) {
				// store response from DB to resultJson
//				JsonObject resultJson = handler.result();
//				String status = resultJson.getString("status");
				String status = "success";
				JsonObject resultJson = new JsonObject();
				if (status.equalsIgnoreCase("success")) {
					response.setStatusCode(201);
				} else {
					response.setStatusCode(400);
				}
				response.headers().add("content-type", "application/json").add("content-length",
						String.valueOf(resultJson.toString().length()));
				response.write(resultJson.toString());
				System.out.println(resultJson);
				response.end();
			} else if (handler.failed()) {
				handler.cause().getMessage();
				response.headers().add("content-type", "text");
				response.setStatusCode(500);
				response.end("Internal server error");
			}
		});
	}

	public void deleteConfig(RoutingContext routingContext) {
		HttpServerRequest request = routingContext.request();
		HttpServerResponse response = routingContext.response();
		String domainName = request.host().replaceAll("[:8443]+$", "");
		JsonObject queryJson = new JsonObject();
		queryJson.put("instanceID", domainName);
		System.out.println(queryJson);
		// Query database for config
		database.searchQuery(queryJson, handler -> {
			if (handler.succeeded()) {
				// store response from DB to resultJson
//				JsonObject resultJson = handler.result();
//				String status = resultJson.getString("status");
				String status = "success";
				JsonObject resultJson = new JsonObject();
				if (status.equalsIgnoreCase("success")) {
					response.setStatusCode(200);
				} else {
					response.setStatusCode(400);
				}
				response.headers().add("content-type", "application/json").add("content-length",
						String.valueOf(resultJson.toString().length()));
				response.write(resultJson.toString());
				System.out.println(resultJson);
				response.end();
			} else if (handler.failed()) {
				handler.cause().getMessage();
				response.headers().add("content-type", "text");
				response.setStatusCode(500);
				response.end("Internal server error");
			}
		});
	}

	public void updateConfig(RoutingContext routingContext) {
		HttpServerRequest request = routingContext.request();
		HttpServerResponse response = routingContext.response();
		JsonObject queryJson = routingContext.getBodyAsJson();
		String domainName = request.host().replaceAll("[:8443]+$", "");
		queryJson.put("instanceID", domainName);
		System.out.println(queryJson);
		// Query database for setting config
		database.searchQuery(queryJson, handler -> {
			if (handler.succeeded()) {
				// store response from DB to resultJson
//				JsonObject resultJson = handler.result();
//				String status = resultJson.getString("status");
				String status = "success";
				JsonObject resultJson = new JsonObject();
				if (status.equalsIgnoreCase("success")) {
					response.setStatusCode(201);
				} else {
					response.setStatusCode(400);
				}
				response.headers().add("content-type", "application/json").add("content-length",
						String.valueOf(resultJson.toString().length()));
				response.write(resultJson.toString());
				System.out.println(resultJson);
				response.end();
			} else if (handler.failed()) {
				handler.cause().getMessage();
				response.headers().add("content-type", "text");
				response.setStatusCode(500);
				response.end("Internal server error");
			}
		});
	}

	public void appendConfig(RoutingContext routingContext) {
		HttpServerResponse response = routingContext.response();
		JsonObject queryJson = routingContext.getBodyAsJson();
		queryJson.put("operation", "append-config");
		System.out.println(queryJson);
		// Query database for setting config
		database.searchQuery(queryJson, handler -> {
			if (handler.succeeded()) {
				// store response from DB to resultJson
//				JsonObject resultJson = handler.result();
//				String status = resultJson.getString("status");
				String status = "success";
				JsonObject resultJson = new JsonObject();
				if (status.equalsIgnoreCase("success")) {
					response.setStatusCode(200);
				} else {
					response.setStatusCode(400);
				}
				response.headers().add("content-type", "application/json").add("content-length",
						String.valueOf(resultJson.toString().length()));
				response.write(resultJson.toString());
				System.out.println(resultJson);
				response.end();
			} else if (handler.failed()) {
				handler.cause().getMessage();
				response.headers().add("content-type", "text");
				response.setStatusCode(500);
				response.end("Internal server error");
			}
		});
	}
}
