package io.vertx.shiva;

import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.DeploymentOptions;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import io.vertx.core.Future;
import io.vertx.core.CompositeFuture;

@ExtendWith(VertxExtension.class)
public class TestMainVerticle {

  @BeforeEach
  void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
    final JsonObject config = fromFile("src/main/conf/my-application-conf.json");
   
    //System.err.println(Json.encodePrettily(config));
    vertx.deployVerticle(new ShivaVerticle(),new DeploymentOptions().setConfig(config), testContext.succeeding(id -> testContext.completeNow()));
   
  }


  // @Test
  // @DisplayName("Should start a Web Server on port 8484")
  // @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  // void start_http_server(Vertx vertx, VertxTestContext testContext) throws Throwable {
  //   vertx.createHttpClient().getNow(7443, "localhost", "/", response -> testContext.verify(() -> {
  //     assertTrue(response.statusCode() == 200);
  //     response.handler(body -> {
  //       assertTrue(body.toString().contains("Hello"));
  //       testContext.completeNow();
  //     });
  //   }));

  // }

  // @Test
  // @DisplayName("Should start a Web Server on port 8484")
  // @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  // void start_http_server2(Vertx vertx, VertxTestContext testContext) throws Throwable {
  //   vertx.createHttpClient().getNow(8484, "localhost", "/api/v1/test/json/5b9b95bf4581670d90cad0a2", response -> testContext.verify(() -> {
  //     assertTrue(response.statusCode() == 200);
      
  //     response.handler(body -> {
  //       System.out.println(body.toString());
  //       testContext.completeNow();
  //     });
  //   }));
  // }

  // @Test
  // @DisplayName("Should start a Web Server on port 8484")
  // @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  // void test_get_case(Vertx vertx, VertxTestContext testContext) throws Throwable {
  //   vertx.createHttpClient().getNow(8484, "localhost", "/api/v1/test/json/5b9b95bf4581670d90cad0a2", response -> testContext.verify(() -> {
  //     assertTrue(response.statusCode() == 200);
      
  //     response.handler(body -> {
  //       System.out.println(body.toString());
  //       testContext.completeNow();
  //     });
  //   }));

  // }

 
  public static JsonObject fromFile(String file){

    try {
      System.out.println(Paths.get(file));
        return new JsonObject(new String(Files.readAllBytes(Paths.get(file))));
    } catch (IOException e) {
        throw new RuntimeException("Could not read file " + file, e);
    }
}
}
