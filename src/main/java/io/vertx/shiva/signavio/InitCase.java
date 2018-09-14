package io.vertx.shiva.signavio;

import io.vertx.shiva.liquor.*;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

import java.util.List;
import java.util.Base64;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
// import io.vertx.util.Runner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class InitCase extends Case
{
    /**
     * Inititalize new workflow
     * @param jsonObj
     * @param token
     * @param aHandler
     */
    public static void init(JsonObject jsonObj, String token, Handler<AsyncResult<String>> aHandler) 
    {
        //JsonObject jsonStr = routingContext.getBodyAsJson();
        final String id = jsonObj.getString("id");
        final String newCaseName = jsonObj.getString("newCaseName");
        String fileID = "";
        JsonObject fileJson;
        if (jsonObj.containsKey("eface"));
        {
            String idfront = "";
            JsonArray eface = jsonObj.getJsonArray("eface");
            //System.out.println(eface.size());
            for (int i = 0 ; i < eface.size(); i++) {
                JsonObject obj = eface.getJsonObject(i);
                //System.out.println(obj.encodePrettily());
                idfront = obj.getString("idfront");
            }
            byte[] imageByte = Base64.getDecoder().decode(idfront);
            fileJson = new JsonObject(WebClientHelper.uploadToSignavio(imageByte,"idfront.png", token));
            fileID = fileJson.getString("id");
        }
        Whisky whisky = caseWhisky(newCaseName, fileID);      
        aHandler.handle(Future.succeededFuture(WebClientHelper.postJson(new SignavioApiPathHelper().getCases(), token, whisky))); 
       
    }//end InitCase

    /**
     * To insert record to DB for tracking on Task. Purpose is to dynamically set the user group in the task
     * @param mongo
     * @param resultStr
     * @param userid
     * @param aHandler
     */
    public static void initWfTracker(MongoClient mongo, String resultStr, String userid, Handler<AsyncResult<String>> aHandler) 
    {
        JsonObject caseObj = new JsonObject(resultStr);
        
        mongo.findOne("abmb_user_branch", new JsonObject().put("userid", userid), null, ar -> {
            if (ar.succeeded()) {

                JsonObject document = new JsonObject()
                .put("caseid", caseObj.getString("id"))
                .put("email", userid)
                .put("branch", ar.result().getString("branch"))
                .put("complete", "false");
                
                mongo.insert("abmb_tracker", document, insertar -> {
                    if (ar.succeeded()) {
                        aHandler.handle(Future.succeededFuture(insertar.result())); 
                    } else {
                        aHandler.handle(Future.failedFuture(document.encodePrettily())); 
                    }
                });
                //MongoDBHelper.insert(mongo, "abmb_tracker", document, aHandler);
            }
            
          });
        
    }//end InitCase

    
    private static Whisky caseWhisky(String newCaseName, String idfront)
    {
        try(Reader reader = new InputStreamReader(io.vertx.shiva.MainVerticle.class.getClassLoader().getResourceAsStream("test.json"), "UTF-8")){
            Gson gson = new GsonBuilder().create();
            Whisky whisky = gson.fromJson(reader, Whisky.class);
            List<Field> fields = whisky.triggerInstance.data.formInstance.value.getFields();
            for (int i = 0; i < fields.size(); i++) {
                Field f = fields.get(i);
                // System.out.println(gson.toJson(f));
                // System.out.println(f.getName().equals("Name"));
                // System.out.println(f.getName());
                if (f.getName().equals("name"))
                {
                    f.setValue(newCaseName);
                    fields.set(i, f);
                }
                if (f.getName().equals("idfront"))
                {
                    f.setValue(idfront);
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