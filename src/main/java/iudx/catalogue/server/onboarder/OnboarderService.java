package iudx.catalogue.server.onboarder;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * The Onboarder Service.
 * <h1>Onboarder Service</h1>
 * <p>
 * The Onboarder Service in the IUDX Catalogue Server defines the operations to be performed with
 * the IUDX Resource server.
 * </p>
 * 
 * @see io.vertx.codegen.annotations.ProxyGen
 * @see io.vertx.codegen.annotations.VertxGen
 * @version 1.0
 * @since 2020-05-31
 */

@VertxGen
@ProxyGen
public interface OnboarderService {

  /**
   * The registerAdaptor method implements the registration flow with RabbitMQ for an Adaptor. It
   * includes creation of exchanges, access control for the adaptor.
   * 
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return OnboarderService which is a Service
   */

  @Fluent
  OnboarderService registerAdaptor(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The registerAdaptor method implements the update flow with RabbitMQ for an Adaptor. It includes
   * updation of exchanges, access control rules for the adaptor.
   * 
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return OnboarderService which is a Service
   */

  @Fluent
  OnboarderService updateAdaptor(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The registerAdaptor method implements the delete flow with RabbitMQ for an Adaptor. It includes
   * deleting an exchanges or access control rule for the adaptor.
   * 
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return OnboarderService which is a Service
   */

  @Fluent
  OnboarderService deleteAdaptor(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The listAdaptor method implements the get infor flow with RabbitMQ for an Adaptor.
   * 
   * @param request which is a JsonObject
   * @param handler which is a Request Handler
   * @return OnboarderService which is a Service
   */

  @Fluent
  OnboarderService listAdaptor(JsonObject request, Handler<AsyncResult<JsonObject>> handler);

  /**
   * The createProxy helps the code generation blocks to generate proxy code.
   * @param vertx which is the vertx instance
   * @param address which is the proxy address
   * @return OnboarderServiceVertxEBProxy which is a service proxy 
   */

  @GenIgnore
  static OnboarderService createProxy(Vertx vertx, String address) {
    return new OnboarderServiceVertxEBProxy(vertx, address);
  }
}
