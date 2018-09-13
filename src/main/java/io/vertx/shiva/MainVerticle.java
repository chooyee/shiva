package io.vertx.shiva;
import io.vertx.shiva.liquor.*;
import io.vertx.shiva.signavio.*;
import io.vertx.shiva.util.*;

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
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.handler.CorsHandler;

import java.util.HashSet;
import java.util.Set;
import java.util.Base64;
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

    router.get("/api/v1/users/token/:id").handler(this::getUserTokenByID);   
    router.route("/api/v1/case/new").handler(BodyHandler.create());
    router.post("/api/v1/case/new").handler(this::initCase);

    router.get("/api/v1/test/:id/:newCaseName").handler(this::test_post_trigger);
    router.post("/api/v1/test/posttrigger").handler(this::test_post_trigger);
    router.get("/api/v1/test/upload").handler(this::test_signavio_upload);
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

  //#region Test Method
  private void test_signavio_upload(RoutingContext routingContext)
  {
    UserHelper.getUserTokenByID(mongo, config().getString("test_user") , token -> {
      String result = WebClientHelper.uploadToSignavioTest(token.result());
      routingContext.response()
      .setStatusCode(200)
      .putHeader("content-type", "application/json; charset=utf-8")
      .end(result);
    });
   
  }

  private void test_post_trigger(RoutingContext routingContext) 
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
    UserHelper.getUserTokenByID(mongo, id, token -> {
      if (token.succeeded()){
        new WebClientPost().postJson("localhost", "/api/v1/alliancebankofmalaysia/cases", 8080, token.result(), whisky, ar -> {
          if (ar.succeeded()) {
            routingContext.response()
            .setStatusCode(200)
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(ar.result());
          }     
        });
      
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
  //#endregion

  private void getUserTokenByID(RoutingContext routingContext) {
    final String id = routingContext.request().getParam("id");
    
    if (id == null) {
      routingContext.response().setStatusCode(400).end();
    } else {
      UserHelper.getUserTokenByID(mongo, id, ar->{
        if (ar.succeeded()) {
          if (ar.result() == null) {
            routingContext.response().setStatusCode(404).end();
            return;
          }
         
          routingContext.response()
              .setStatusCode(200)
              .putHeader("content-type", "application/json; charset=utf-8")
              .end(ar.result());
        } else {
          routingContext.response().setStatusCode(404).end();
        }
      });
      
    }
  }

  /**
  * This is call by DBOS to initiate a new EDD workflow
  */
  private void initCase(RoutingContext routingContext)
  {
  
    JsonObject jsonObj = routingContext.getBodyAsJson();
    final String id = jsonObj.getString("id");
    final String newCaseName = jsonObj.getString("newCaseName");

    UserHelper.getUserTokenByID(mongo, id, ar->{
      if (ar.succeeded()) {
        if (ar.result() == null) {
          routingContext.response().setStatusCode(404).end();
          return;
        }
        InitCase.init(mongo, jsonObj, ar.result(), aHandler->{
          if (aHandler.succeeded()){
            InitCase.initWfTracker(mongo, aHandler.result(), id, wfHandler->{
              routingContext.response()
              .setStatusCode(200)
              .putHeader("content-type", "application/json; charset=utf-8")
              .end(aHandler.result());
            });
          }
         
        });
       
      } else {
        routingContext.response().setStatusCode(404).end();
      }
    });
  }

  public static byte[] decodeBase64(String encodedString)
  {
      byte[] imageByte = Base64.getDecoder().decode(encodedString);
      return imageByte;
  }
}
