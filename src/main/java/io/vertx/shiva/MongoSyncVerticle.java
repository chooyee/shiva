package io.vertx.shiva;

public class MongoSyncVerticle{
    
}
/*
import co.paralleluniverse.fibers.Suspendable;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.sync.SyncVerticle;
import io.vertx.ext.sync.Sync;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
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
        
        // router.route("/api/v1/sync/task").handler(Sync.fiberHandler(BodyHandler.create()));
        // router.post("/api/v1/sync/task").handler(Sync.fiberHandler(this::dataMassage));
        router.post("/api/v1/sync/task/:caseid").handler(Sync.fiberHandler(this::finale));
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

    @Suspendable
    private void finale(RoutingContext routingContext)
    {
        final String caseID = routingContext.request().getParam("caseid");
        System.err.println(caseID);
        final JsonObject caseObj = awaitResult
        (h ->  mongo.findOne("cases", new JsonObject().put("_id", new JsonObject().put("$oid",caseID)), null,h));
       
        System.err.println(caseObj);
        JsonObject result = new JsonObject();
        JsonArray newEvents =  new JsonArray();
        //JsonArray participants = routingContext.getBodyAsJsonArray();
        caseObj.getJsonArray("participants").forEach(p ->{
            JsonObject participant = (JsonObject)p;
            participant.getJsonArray("events").forEach(e->{  
                JsonObject event = (JsonObject)e;
                String userId = new JsonObject(event.getValue("userId").toString()).getString("$oid");
                        
                final JsonObject response = awaitResult
                (h ->  mongo.findOne("users", new JsonObject().put("emailAddressLower", userId), null,h));
                if (response !=null)
                {
                    event.put("email", response.getString("emailAddressLower"));
                }

                if (event.containsKey("taskId"))
                {
                    String taskId = new JsonObject(event.getValue("taskId").toString()).getString("$oid");
                            
                    JsonObject query = new JsonObject()
                    .put("_id", new JsonObject().put("$oid", taskId))
                    .put("form", new JsonObject().put("$exists",true));
            
                    final JsonObject tresponse = awaitResult
                    (h ->  mongo.findOne("tasks", query, null,h));
                    if (tresponse !=null)
                    {
                        event.put("variables", tresponse.getJsonObject("form").getJsonArray("fields")); 
                    }
                }
                newEvents.add(event);
            });
        
            int lastEventInt = newEvents.size()-1;
            JsonObject lastEvent = newEvents.getJsonObject(lastEventInt);
            String lastStatus = "";
            //System.err.println(Json.encodePrettily(lastEvent) );
            if (lastEvent.containsKey("variables"))
            {
                JsonArray newVariables = addValueName(lastEvent);
                //System.err.println(Json.encodePrettily(newVariables));
                //=================================================================================
                //Get last approval status
                //=================================================================================
                JsonObject lastVariable = newVariables.getJsonObject(newVariables.size()-1);
                if (lastVariable.containsKey("value_name"))
                    lastStatus = lastVariable.getString("value_name");
                //=================================================================================
                //End Get last approval status
                //=================================================================================

                lastEvent.put("variables", newVariables);
                newEvents.remove(lastEventInt);
                newEvents.add(lastEvent);
            }
            
            
            result.put("status", lastStatus).put("events", newEvents);
        });
        routingContext.response()
        .setStatusCode(200)
        .putHeader("content-type", "application/json; charset=utf-8")
        .end(Json.encodePrettily(caseObj));
    }

    @Suspendable
    private JsonObject dataMassage(JsonArray participants, Handler<AsyncResult<JsonObject>> finalHandler)
    {
        JsonObject result = new JsonObject();
        JsonArray newEvents =  new JsonArray();
        //JsonArray participants = routingContext.getBodyAsJsonArray();
        participants.forEach(p ->{
            JsonObject participant = (JsonObject)p;
            participant.getJsonArray("events").forEach(e->{  
                JsonObject event = (JsonObject)e;
                String userId = new JsonObject(event.getValue("userId").toString()).getString("$oid");
                
                final JsonObject response = awaitResult
                (h ->  mongo.findOne("users", new JsonObject().put("emailAddressLower", userId), null,h));
                if (response !=null)
                {
                    event.put("email", response.getString("emailAddressLower"));
                }

                if (event.containsKey("taskId"))
                {
                    String taskId = new JsonObject(event.getValue("taskId").toString()).getString("$oid");
                            
                    JsonObject query = new JsonObject()
                    .put("_id", new JsonObject().put("$oid", taskId))
                    .put("form", new JsonObject().put("$exists",true));
            
                    final JsonObject tresponse = awaitResult
                    (h ->  mongo.findOne("tasks", query, null,h));
                    if (tresponse !=null)
                    {
                        event.put("variables", tresponse.getJsonObject("form").getJsonArray("fields")); 
                    }
                }
                newEvents.add(event);
            });

            int lastEventInt = newEvents.size()-1;
            JsonObject lastEvent = newEvents.getJsonObject(lastEventInt);
            String lastStatus = "";
            //System.err.println(Json.encodePrettily(lastEvent) );
            if (lastEvent.containsKey("variables"))
            {
                JsonArray newVariables = addValueName(lastEvent);
                //System.err.println(Json.encodePrettily(newVariables));
                //=================================================================================
                //Get last approval status
                //=================================================================================
                JsonObject lastVariable = newVariables.getJsonObject(newVariables.size()-1);
                if (lastVariable.containsKey("value_name"))
                    lastStatus = lastVariable.getString("value_name");
                //=================================================================================
                //End Get last approval status
                //=================================================================================

                lastEvent.put("variables", newVariables);
                newEvents.remove(lastEventInt);
                newEvents.add(lastEvent);
            }
            
          
            result.put("status", lastStatus).put("events", newEvents);
        });

       return result;
     
    }

    private JsonArray addValueName(JsonObject lastEvent)
    {
        JsonArray newVariables = new JsonArray();
        lastEvent.getJsonArray("variables").forEach(v->{
            JsonObject variable = (JsonObject)v;
            if (variable.getJsonObject("type").getString("name").equals("choice"))
            {
                String variableValue = variable.getString("value");
                variable.getJsonObject("type").getJsonArray("options").forEach(opt->{
                    JsonObject option = (JsonObject)opt;
                    if (option.getString("id").equals(variableValue))
                    {
                        variable.put("value_name", option.getString("name"));
                    }
                });
            }
            newVariables.add(variable);
        });

        return newVariables;

    }
}
*/