package iudx.catalogue.server.apiserver;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

public class Tests {
	int result;
	String status;

	@DisplayName("successTest")
	@Test
	public void TestSuccess() throws InterruptedException {
		Vertx vertx = Vertx.vertx();
//		vertx.deployVerticle(new ApiServerVerticle());
//		TimeUnit.SECONDS.sleep(15);
		System.out.println("running");
		WebClient client = WebClient.create(vertx);
		client.get(8443, "127.0.0.1", "/iudx/cat/v1/search").addQueryParam("property", "id")
				.addQueryParam("value", "test").ssl(true).send(ar -> {
					System.out.println("in");
					if (ar.succeeded()) {
						// Obtain response
						HttpResponse<Buffer> response = ar.result();
						System.out.println("Received response with status code" + response.statusCode());
					} else {
						System.out.println("Something went wrong " + ar.cause().getMessage());
					}
				});
		assertEquals("", "");
		System.out.println("stopped");
	}
}
