package io.vertx.shiva;

import io.vertx.shiva.signavio.*;
import co.paralleluniverse.fibers.Suspendable;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.sync.SyncVerticle;
import io.vertx.ext.sync.Sync;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import static io.vertx.ext.sync.Sync.awaitResult;

public class MongoSyncVerticle extends SyncVerticle {
 

  
    private MongoClient mongo;

    @Override
    @Suspendable
    public void start(Future<Void> startFuture) throws Exception {
        super.start(startFuture);
       
        mongo = MongoClient.createShared(vertx, config());
        Sync.fiberHandler(req->{
            JsonObject response = awaitResult
            (h ->  mongo.findOne("users",new JsonObject(), null,h));
            System.err.println(response); 
            
        });
              

        Router router = Router.router(vertx);
        
        router.route("/api/v1/sync/task").handler(Sync.fiberHandler(BodyHandler.create()));
        router.post("/api/v1/sync/task").handler(Sync.fiberHandler(this::getTask));
        router.get("/test").handler(Sync.fiberHandler(this::saveNewEntity));
            
     
        // Create the HTTP server and pass the "accept" method to the request handler.
        vertx
        .createHttpServer()
        .requestHandler(router::accept)
        .listen(
            // Retrieve the port from the configuration,
            // default to 8080.
             8089           
        );
    }

    

    @Override
    public void stop() {
        mongo.close();
    }

    
    @Suspendable
    private void saveNewEntity(RoutingContext routingContext){
        final JsonObject response = awaitResult
        (h ->  mongo.findOne("users",new JsonObject(), null,h));
        routingContext.response()
        .setStatusCode(200)
        .putHeader("content-type", "application/json; charset=utf-8")
        .end(Json.encodePrettily(response));
    }

    @Suspendable
    private void getTask(RoutingContext routingContext)
    {
        JsonObject event = routingContext.getBodyAsJson();
        //System.err.println(event);
        String taskId = new JsonObject(event.getValue("taskId").toString()).getString("$oid");
                            
        JsonObject query = new JsonObject()
        .put("_id", new JsonObject().put("$oid", taskId))
        .put("form", new JsonObject().put("$exists",true));

        final JsonObject response = awaitResult
        (h ->  mongo.findOne("tasks", query, null,h));
        if (response !=null)
        {
            event.put("variables", response.getJsonObject("form").getJsonArray("fields")); 
        }
        routingContext.response()
        .setStatusCode(200)
        .putHeader("content-type", "application/json; charset=utf-8")
        .end(Json.encodePrettily(event));
       //return event;
       
    }
}
