package io.vertx.shiva;
import io.vertx.shiva.liquor.*;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
// import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.client.WebClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.handler.CorsHandler;

import java.util.HashSet;
import java.util.Set;

import java.util.List;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
// import io.vertx.util.Runner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MainVerticle extends AbstractVerticle {

  public static final String COLLECTION = "signavio";
  private MongoClient mongo;

  // Convenience method so you can run it in your IDE
  // public static void main(String[] args) {
  //   Runner.runExample(MainVerticle.class);
  // }

  /**
   * This method is called when the verticle is deployed. It creates a HTTP server and registers a simple request
   * handler.
   * <p/>
   * Notice the `listen` method. It passes a lambda checking the port binding result. When the HTTP server has been
   * bound on the port, it call the `complete` method to inform that the starting has completed. Else it reports the
   * error.
   *
   * @param fut the future
   */
  @Override
  public void start(Future<Void> fut) {

    // Create a Mongo client
    mongo = MongoClient.createShared(vertx, config());

    startWebApp((http) -> completeStartup(http, fut));
  }

  private void startWebApp(Handler<AsyncResult<HttpServer>> next) {
    // Create a router object.
    Router router = Router.router(vertx);
    Set<String> allowedHeaders = new HashSet<>();
    allowedHeaders.add("x-requested-with");
    allowedHeaders.add("Access-Control-Allow-Origin");
    allowedHeaders.add("origin");
    allowedHeaders.add("Content-Type");
    allowedHeaders.add("accept");
    allowedHeaders.add("X-PINGARUNER");

    Set<HttpMethod> allowedMethods = new HashSet<>();
    allowedMethods.add(HttpMethod.GET);
    allowedMethods.add(HttpMethod.POST);
    allowedMethods.add(HttpMethod.OPTIONS);
    /*
     * these methods aren't necessary for this sample, 
     * but you may need them for your projects
     */
    allowedMethods.add(HttpMethod.DELETE);
    allowedMethods.add(HttpMethod.PATCH);
    allowedMethods.add(HttpMethod.PUT);

    router.route().handler(CorsHandler.create("*").allowedHeaders(allowedHeaders).allowedMethods(allowedMethods));

    // Bind "/" to our hello message.
    router.route("/").handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response
          .putHeader("content-type", "text/html")
          .end("<h1>Hello from Vert.x 3!</h1>");
    });

    router.route("/assets/*").handler(StaticHandler.create("assets"));

    router.get("/api/v1/users/token/:id").handler(this::getOne);
    router.get("/api/v1/test/:id/:newCaseName").handler(this::postTrigger);
    router.post("/api/v1/test").handler(this::postTrigger);
    // router.post("/api/v1/j").handler(this::postJson);
    router.route("/api/v1/j").handler(BodyHandler.create());
    router.post("/api/v1/j").handler(this::postJson);
    // router.post("/api/whiskies").handler(this::addOne);
    // router.get("/api/whiskies/:id").handler(this::getOne);
    // router.put("/api/whiskies/:id").handler(this::updateOne);
    // router.delete("/api/whiskies/:id").handler(this::deleteOne);


    // Create the HTTP server and pass the "accept" method to the request handler.
    vertx
        .createHttpServer()
        .requestHandler(router::accept)
        .listen(
            // Retrieve the port from the configuration,
            // default to 8080.
            config().getInteger("http.port", 8484),
            next
        );
  }

  private void completeStartup(AsyncResult<HttpServer> http, Future<Void> fut) {
    if (http.succeeded()) {
      fut.complete();
    } else {
      fut.fail(http.cause());
    }
  }


  @Override
  public void stop() {
    mongo.close();
  }

  private void getOne(RoutingContext routingContext) {
    final String id = routingContext.request().getParam("id");
    
    if (id == null) {
      routingContext.response().setStatusCode(400).end();
    } else {
      mongo.findOne("users", new JsonObject().put("emailAddressLower", id), null, ar -> {
        if (ar.succeeded()) {
          if (ar.result() == null) {
            routingContext.response().setStatusCode(404).end();
            return;
          }
         
          routingContext.response()
              .setStatusCode(200)
              .putHeader("content-type", "application/json; charset=utf-8")
              .end(ar.result().getString("token"));
        } else {
          routingContext.response().setStatusCode(404).end();
        }
      });
    }
  }

  private void getUserToken(String id, Handler<AsyncResult<String>> aHandler) {
    
      mongo.findOne("users", new JsonObject().put("emailAddressLower", id), null, ar -> {
        if (ar.succeeded()) {
          if (ar.result() == null) {
            aHandler.handle(Future.failedFuture("Connection succeeded but no result found!")); 
          }
          else {
            aHandler.handle(Future.succeededFuture(ar.result().getString("token"))); 
          }
        }
        
      });
    
  }
  // private void getTest(RoutingContext routingContext) {
    
    
  //   Whisky w = new Whisky();
  
  //   routingContext.response()
  //       .setStatusCode(200)
  //       .putHeader("content-type", "application/json; charset=utf-8")
  //       .end(Json.encodePrettily(w));
        
  // }

  private void postTrigger(RoutingContext routingContext) 
  {
    final String id = routingContext.request().getParam("id");
    final String newCaseName = routingContext.request().getParam("newCaseName");

    newCase(routingContext, id, newCaseName);
  
  }//end post trigger

  private void postJson(RoutingContext routingContext) 
  {
    JsonObject jsonStr = routingContext.getBodyAsJson();
    final String id = jsonStr.getString("id");
    final String newCaseName = jsonStr.getString("newCaseName");

    newCase(routingContext, id, newCaseName);
  
  }//end postJson

  private void newCase(RoutingContext routingContext, String id, String newCaseName)
  {
    Whisky whisky = caseWhisky(newCaseName);
    getUserToken(id, token -> {
      if (token.succeeded()){
        WebClient client = WebClient.create(vertx);
        client       
          .post(8080, "localhost", "/api/v1/alliancebankofmalaysia/cases")
          .putHeader("Authorization", token.result())
          .putHeader("Content-Type", "application/json")
          .sendJson(whisky, ar -> {
          if (ar.succeeded()) {
            routingContext.response()
            .setStatusCode(200)
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(ar.result().bodyAsString());
          }     
        });//end client
      }
      else{
        routingContext.response()
        .setStatusCode(200)
        .putHeader("content-type", "application/json; charset=utf-8")
        .end(token.cause().getMessage());
      }
    });
  }

  private Whisky caseWhisky(String newCaseName)
  {
    try(Reader reader = new InputStreamReader(MainVerticle.class.getClassLoader().getResourceAsStream("test.json"), "UTF-8")){
      Gson gson = new GsonBuilder().create();
      Whisky whisky = gson.fromJson(reader, Whisky.class);
      List<Field> fields = whisky.triggerInstance.data.formInstance.value.getFields();
      for (int i = 0; i < fields.size(); i++) {
        Field f = fields.get(i);
        // System.out.println(gson.toJson(f));
        // System.out.println(f.getName().equals("Name"));
        // System.out.println(f.getName());
        if (f.getName().equals("Name"))
        {
          f.setValue(newCaseName);
          fields.set(i, f);
        }
      }//end for
      whisky.triggerInstance.data.formInstance.value.setFields(fields);
      return whisky;
      
    }//end try
    catch (IOException e) {
     return null;
    }//end catch
  }

}
