
package io.vertx.shiva.signavio;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

// import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class UserHelper extends MongoDBHelper{

    public UserHelper() {
    }

    public static void getUserToken(MongoClient mongo, String id, Handler<AsyncResult<String>> aHandler) {
    
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
  
    public static void getUserTokenByID(MongoClient mongo, String id, Handler<AsyncResult<String>> aHandler) {
    
        MongoDBHelper.findOne(mongo, "users", new JsonObject().put("emailAddressLower", id), ar->{
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

    public static void getUserInfoByObjID(MongoClient mongo, String userObjectID, Handler<AsyncResult<JsonObject>> aHandler) {
    
      MongoDBHelper.findOne(mongo, "users", new JsonObject().put("_id", userObjectID), ar->{
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
}
