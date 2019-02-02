package io.vertx.shiva;
import io.vertx.shiva.liquor.*;
import io.vertx.shiva.signavio.*;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
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

import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.TCPSSLOptions;

public class ShivaVerticle extends AbstractVerticle {

  private static final int HTTP1_PORT = 8527;
  private static final int HTTP2_PORT = 9443;
  private String host = "localhost";

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
    //System.err.println(Json.encodePrettily(config()));
    //System.out.println("Shiva started with Port:" + config().getInteger("http.port", 8282));
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
    router.route("/api/v1/nonindi/new").handler(BodyHandler.create());
    router.post("/api/v1/nonindi/new").handler(this::InitCaseNonIndi);
    router.get("/api/v1/task/unassigned/:caseid").handler(this::getUnAssignedTask); 
    router.get("/api/v1/task/assign/:caseid").handler(this::setAssignee);  

    router.get("/api/v1/test/mongo").handler(this::mongoTest);

    // Create the HTTP server and pass the "accept" method to the request handler.
    vertx
        .createHttpServer(createOptions(true))
        .requestHandler(router::accept)
        .listen(
            // Retrieve the port from the configuration,
            // default to 8080.
            config().getInteger("https.port", 8443),
            next
        );
  }

  private HttpServerOptions createOptions(boolean http2) {
    HttpServerOptions serverOptions = new HttpServerOptions()
        .setPort(http2 ? HTTP2_PORT : HTTP1_PORT)
        .setHost(host);
    if (http2) {
        serverOptions
        .setSsl(true)
        .setEnabledSecureTransportProtocols(new HashSet<String>(TCPSSLOptions.DEFAULT_ENABLED_SECURE_TRANSPORT_PROTOCOLS))
        .setKeyCertOptions(new PemKeyCertOptions().setCertPath("tls/server-cert.pem").setKeyPath("tls/server-key.pem"));
      
        serverOptions.addEnabledCipherSuite("TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256").addEnabledCipherSuite("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256")
        .addEnabledCipherSuite("TLS_RSA_WITH_AES_128_CBC_SHA256").addEnabledCipherSuite("TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256")
        .addEnabledCipherSuite("TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256").addEnabledCipherSuite("TLS_DHE_DSS_WITH_AES_128_CBC_SHA256")
        .addEnabledCipherSuite("TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA").addEnabledCipherSuite("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA")
        .addEnabledCipherSuite("TLS_RSA_WITH_AES_128_CBC_SHA").addEnabledCipherSuite("TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA")
        .addEnabledCipherSuite("TLS_ECDH_RSA_WITH_AES_128_CBC_SHA").addEnabledCipherSuite("TLS_DHE_DSS_WITH_AES_128_CBC_SHA")
        .addEnabledCipherSuite("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256").addEnabledCipherSuite("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256")
        .addEnabledCipherSuite("TLS_RSA_WITH_AES_128_GCM_SHA256").addEnabledCipherSuite("TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256")
        .addEnabledCipherSuite("TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256").addEnabledCipherSuite("TLS_DHE_DSS_WITH_AES_128_GCM_SHA256")
        .addEnabledCipherSuite("TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA").addEnabledCipherSuite("SSL_RSA_WITH_3DES_EDE_CBC_SHA")
        .addEnabledCipherSuite("TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA").addEnabledCipherSuite("TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA")
        .addEnabledCipherSuite("SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA").addEnabledCipherSuite("TLS_ECDHE_ECDSA_WITH_RC4_128_SHA")
        .addEnabledCipherSuite("SSL_RSA_WITH_RC4_128_SHA").addEnabledCipherSuite("TLS_ECDH_ECDSA_WITH_RC4_128_SHA")
        .addEnabledCipherSuite("TLS_ECDH_RSA_WITH_RC4_128_SHA").addEnabledCipherSuite("SSL_RSA_WITH_RC4_128_MD5")
        .addEnabledCipherSuite("TLS_EMPTY_RENEGOTIATION_INFO_SCSV").addEnabledCipherSuite("TLS_ECDH_anon_WITH_RC4_128_SHA")
        .addEnabledCipherSuite("SSL_DH_anon_WITH_RC4_128_MD5").addEnabledCipherSuite("SSL_RSA_WITH_DES_CBC_SHA")
        .addEnabledCipherSuite("SSL_DHE_RSA_WITH_DES_CBC_SHA").addEnabledCipherSuite("SSL_DHE_DSS_WITH_DES_CBC_SHA")
        .addEnabledCipherSuite("SSL_DH_anon_WITH_DES_CBC_SHA").addEnabledCipherSuite("SSL_RSA_EXPORT_WITH_DES40_CBC_SHA")
        .addEnabledCipherSuite("SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA").addEnabledCipherSuite("SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA")
        .addEnabledCipherSuite("SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA").addEnabledCipherSuite("SSL_RSA_EXPORT_WITH_RC4_40_MD5")
        .addEnabledCipherSuite("SSL_DH_anon_EXPORT_WITH_RC4_40_MD5").addEnabledCipherSuite("TLS_KRB5_WITH_3DES_EDE_CBC_SHA")
        .addEnabledCipherSuite("TLS_KRB5_WITH_3DES_EDE_CBC_MD5").addEnabledCipherSuite("TLS_KRB5_WITH_RC4_128_SHA")
        .addEnabledCipherSuite("TLS_KRB5_WITH_RC4_128_MD5").addEnabledCipherSuite("TLS_KRB5_WITH_DES_CBC_SHA")
        .addEnabledCipherSuite("TLS_KRB5_WITH_DES_CBC_MD5").addEnabledCipherSuite("TLS_KRB5_EXPORT_WITH_DES_CBC_40_SHA")
        .addEnabledCipherSuite("TLS_KRB5_EXPORT_WITH_DES_CBC_40_MD5").addEnabledCipherSuite("TLS_KRB5_EXPORT_WITH_RC4_40_SHA")
        .addEnabledCipherSuite("TLS_KRB5_EXPORT_WITH_RC4_40_MD5");
    }
    return serverOptions;
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

  private void mongoTest(RoutingContext routingContext) {
    JsonObject filter = new JsonObject().put("name", "CP").put("version", "1.0");

    mongo.findOne("abmb_workflow_trigger",filter , null, ar -> {
      routingContext.response()
        .setStatusCode(200)
        .putHeader("content-type", "application/json; charset=utf-8")
        .end(Json.encodePrettily(ar.result()));
    });     
    
    
  }
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
            routingContext.response()
              .setStatusCode(200)
              .putHeader("content-type", "application/json; charset=utf-8")
              .end(Json.encodePrettily(ar.cause()));
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
    //final String id = jsonObj.getString("id");
    // final String id =  "leechooyee@alliancefg.com";
    final String id =  config().getString("signavio.admin");
    // final String newCaseName = jsonObj.getString("newCaseName");

    
    InitCase initObject = new InitCase(mongo);
     //Insert to abmb_init_log
    initObject.auditLog(jsonObj, ah->{});
   
    //Get User token from database 
    UserHelper.getUserTokenByID(mongo,id, ar->{
      if (ar.succeeded()) {
        if (ar.result() == null) {
          routingContext.response().setStatusCode(404).end();
          return;
        }
        
        initObject.InitWorkflow(jsonObj, ar.result());

        initObject.initIndiWfMain(jsonObj,  aHandler->{
          if (aHandler.succeeded()){    
              routingContext.response()
              .setStatusCode(200)
              .putHeader("content-type", "application/json; charset=utf-8")
              .end(aHandler.result());
          }
          else{
             routingContext.response()
              .setStatusCode(200)
              .putHeader("content-type", "application/json; charset=utf-8")
              .end(Json.encodePrettily(aHandler.cause()));
          }
        });
       
      } else {
        routingContext.response().setStatusCode(200).end("Failed to get user token");
      }
    });
  }

  /**
  * This is call by DBOS to initiate a new EDD workflow
  */
  private void InitCaseNonIndi(RoutingContext routingContext)
  {
  
    JsonObject jsonObj = routingContext.getBodyAsJson();
    //final String id = jsonObj.getString("id");
    // final String id =  "leechooyee@alliancefg.com";
    final String id =  config().getString("signavio.admin");
    // final String newCaseName = jsonObj.getString("newCaseName");

    
    InitCaseNonIndi initObject = new InitCaseNonIndi(mongo);
     //Insert to abmb_init_log
    initObject.auditLog(jsonObj, ah->{});

    //Get User token from database 
    UserHelper.getUserTokenByID(mongo,id, ar->{
      if (ar.succeeded()) {
        if (ar.result() == null) {
          routingContext.response().setStatusCode(404).end();
          return;
        }
        
        initObject.init(jsonObj, ar.result(), aHandler->{
          if (aHandler.succeeded()){
            initObject.initNonIndiWfTracker(aHandler.result(), jsonObj.getInteger("id").toString(), wfHandler->{
              routingContext.response()
              .setStatusCode(200)
              .putHeader("content-type", "application/json; charset=utf-8")
              .end(aHandler.result());
            });
          }
          else{
             routingContext.response()
              .setStatusCode(200)
              .putHeader("content-type", "application/json; charset=utf-8")
              .end(Json.encodePrettily(aHandler.cause()));
          }
        });
       
      } else {
        routingContext.response().setStatusCode(200).end("Failed to get user token");
      }
    });
  }
  /**
   * Set task assignee
   * @param routingContext
   */
  private void setAssignee(RoutingContext routingContext)
  {
    final String caseId = routingContext.request().getParam("caseid");
    new Case(mongo).setTaskAssignee(caseId, car->{
      routingContext.response()
      .setStatusCode(200)
      .putHeader("content-type", "application/json; charset=utf-8")
      .end(Json.encodePrettily(car.result()));
    });
    
  }
  private void getUnAssignedTask(RoutingContext routingContext)
  {
    final String caseId = routingContext.request().getParam("caseid");
    new Case(mongo).getUnAssignedTask(caseId, aHandler->{
        routingContext.response()
        .setStatusCode(200)
        .putHeader("content-type", "text/html; charset=utf-8")
        .end(Json.encodePrettily(aHandler.result()));
    });
 
  }
  // public static byte[] decodeBase64(String encodedString)
  // {
  //     byte[] imageByte = Base64.getDecoder().decode(encodedString);
  //     return imageByte;
  // }
}
