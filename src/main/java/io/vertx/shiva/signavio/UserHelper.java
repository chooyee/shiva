package io.vertx.shiva.signavio;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class UserHelper{

    public UserHelper() {
    }

    /**
     * 
     * @param mongo
     * @param id
     * @param aHandler
     */
    public static void getUserToken(MongoClient mongo, String id, Handler<AsyncResult<String>> aHandler) {
    
        mongo.findOne(Base.CollectionHelper.USERS.collection(), new JsonObject().put("emailAddressLower", id), null, ar -> {
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
    public static void getAdminToken(MongoClient mongo, Handler<AsyncResult<String>> aHandler) {
    
      JsonObject config = Base.getConfig();
      mongo.findOne(Base.CollectionHelper.USERS.collection(), new JsonObject().put("emailAddressLower", config.getString("signavio.admin")), null, ar -> {
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
    /**
     * 
     * @param mongo
     * @param id
     * @param aHandler
     */
    public static void getUserTokenByID(MongoClient mongo, String id, Handler<AsyncResult<String>> aHandler) {
    
      mongo.findOne(Base.CollectionHelper.USERS.collection(), new JsonObject().put("emailAddressLower", id), null, ar->{
        if (ar.succeeded()) {
            if (ar.result() == null) {
              aHandler.handle(Future.failedFuture("Connection succeeded but no result found!")); 
            }
            else {
              //System.out.println(ar.result().getString("token"));
              aHandler.handle(Future.succeededFuture(ar.result().getString("token"))); 
            }
        }
        else
        {
          aHandler.handle(Future.failedFuture(Json.encodePrettily(ar.result()))); 
        }
      });
      
    }

     /**
     * 
     * @param mongo
     * @param email
     * @param aHandler
     */
    public static void getUserInfoByEmail(MongoClient mongo, String email, Handler<AsyncResult<JsonObject>> aHandler) {
    
      //.put("_id", new JsonObject().put("$oid","5b9a3ff14581670dace6e4f1"))//
      mongo.findOne(Base.CollectionHelper.USERS.collection(), new JsonObject().put("emailAddressLower", email),null, ar->{
          if (ar.succeeded()) {
              if (ar.result() == null) {
                aHandler.handle(Future.failedFuture("Connection succeeded but no result found!")); 
              }
              else {
                aHandler.handle(Future.succeededFuture(ar.result()));
                // JsonObject userObject = ar.result();
                // getUserBranchByEmail(mongo, userObject.getString("emailAddressLower"), uar->{
                //   if (uar.succeeded())
                //   {
                //     userObject.put("branch", uar.result());
                //     aHandler.handle(Future.succeededFuture(userObject)); 
                //   }
                //   else
                //   {
                //     aHandler.handle(Future.failedFuture(Json.encodePrettily(uar.result()))); 
                //   }
                // });
               
              }
          }
          else
          {
            aHandler.handle(Future.failedFuture(ar.cause())); 
          }
        });
    
    }

    /**
     * 
     * @param mongo
     * @param userObjectID
     * @param aHandler
     */
    public static void getUserInfoByObjID(MongoClient mongo, String userObjectID, Handler<AsyncResult<JsonObject>> aHandler) {
    
      //.put("_id", new JsonObject().put("$oid","5b9a3ff14581670dace6e4f1"))//
      mongo.findOne(Base.CollectionHelper.USERS.collection(), new JsonObject().put("_id",  new JsonObject().put("$oid",userObjectID)),null, ar->{
          if (ar.succeeded()) {
              if (ar.result() == null) {
                aHandler.handle(Future.failedFuture("Connection succeeded but no result found!")); 
              }
              else {
                aHandler.handle(Future.succeededFuture(ar.result()));
                // JsonObject userObject = ar.result();
                // getUserBranchByEmail(mongo, userObject.getString("emailAddressLower"), uar->{
                //   if (uar.succeeded())
                //   {
                //     userObject.put("branch", uar.result());
                //     aHandler.handle(Future.succeededFuture(userObject)); 
                //   }
                //   else
                //   {
                //     aHandler.handle(Future.failedFuture(Json.encodePrettily(uar.result()))); 
                //   }
                // });
               
              }
          }
          else
          {
            aHandler.handle(Future.failedFuture(ar.cause())); 
          }
        });
    
    }

    public static void getUserBranchByEmail(MongoClient mongo, String email, Handler<AsyncResult<String>> aHandler) {
    
      //.put("_id", new JsonObject().put("$oid","5b9a3ff14581670dace6e4f1"))//
      //System.err.println("getUserBranchByEmail : " + email);
      mongo.findOne(Base.CollectionHelper.USER_BRANCH.collection(), new JsonObject().put("email", email),null, ar->{
          if (ar.succeeded()) {
              if (ar.result() == null) {
                aHandler.handle(Future.failedFuture("Connection succeeded but no result found!")); 
              }
              else {
                //System.err.println("getUserBranchByEmail : " + ar.result().getString("branch"));
                aHandler.handle(Future.succeededFuture(ar.result().getString("branch"))); 
              }
          }
          else
          {
            aHandler.handle(Future.failedFuture(Json.encodePrettily(ar.result()))); 
          }
        });
    
    }

    public static void getGroupInfo(MongoClient mongo, String groupName, Handler<AsyncResult<JsonObject>> aHandler)
    {
      mongo.findOne(Base.CollectionHelper.GROUPS.collection(), new JsonObject().put("name", groupName ),null, ar->{
        if (ar.succeeded()) {
          aHandler.handle(Future.succeededFuture(ar.result())); 
        }
        else
        {
          aHandler.handle(Future.failedFuture(Json.encodePrettily(ar.result()))); 
        }
      });
    }
}
