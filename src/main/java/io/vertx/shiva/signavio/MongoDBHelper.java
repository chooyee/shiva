package io.vertx.shiva.signavio;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

// import io.vertx.util.Runner;




public class MongoDBHelper extends AbstractVerticle {

  // private MongoClient mongo;

  // public void StartConnection()
  // {
  //   mongo = MongoClient.createShared(vertx, config());
  // }

  // public void StopConnection()
  // {
  //   mongo.close();
  // }

  
  public static void findOne(MongoClient mongo, String collection, JsonObject json, Handler<AsyncResult<JsonObject>> aHandler) {

    mongo.findOne(collection, json, null, ar -> {
      if (ar.succeeded()) {
        if (ar.result() == null) {
          aHandler.handle(Future.failedFuture("Connection succeeded but no result found!")); 
        }
        else {
          aHandler.handle(Future.succeededFuture(ar.result())); 
        }
      }
      
    });
  }

  public static void insert(MongoClient mongo, String collection, JsonObject json, Handler<AsyncResult<String>> aHandler)
  {
    mongo.insert(collection, json, ar -> {
      if (ar.succeeded()) {
        aHandler.handle(Future.succeededFuture(ar.result())); 
      } else {
        aHandler.handle(Future.failedFuture(Json.encodePrettily(json))); 
      }
    });
  }
}
