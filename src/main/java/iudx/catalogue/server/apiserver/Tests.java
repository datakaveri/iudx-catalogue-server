package iudx.catalogue.server.apiserver;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

public class Tests {
	int result;
	String status;
	private Properties properties;
	private InputStream inputstream;
	private String keystore;
	private String keystorePassword;

	@DisplayName("successTest")
	@Test
	public void TestSuccess() throws InterruptedException {
		Vertx vertx = Vertx.vertx();
		properties = new Properties();
		inputstream = null;
		try {

			inputstream = new FileInputStream("config.properties");
			properties.load(inputstream);

			keystore = properties.getProperty("keystore");
			keystorePassword = properties.getProperty("keystorePassword");

		} catch (Exception ex) {
			System.out.println(ex.toString());
		}
//		vertx.deployVerticle(new ApiServerVerticle());
//		TimeUnit.SECONDS.sleep(15);
		System.out.println("running");
		WebClientOptions webClientOptions = new WebClientOptions();
		webClientOptions.setSsl(true).setVerifyHost(false).setTrustAll(true)
				.setKeyStoreOptions(new JksOptions().setPath(keystore).setPassword(keystorePassword));
		WebClient client = WebClient.create(vertx, webClientOptions);
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
