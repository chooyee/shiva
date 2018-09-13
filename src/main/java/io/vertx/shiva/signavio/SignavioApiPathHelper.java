
package io.vertx.shiva.signavio;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

public class SignavioApiPathHelper {

    private String apiPath;
   
    private String files = "files";
    private String cases = "cases";
    private JsonObject jsonObject;

    public SignavioApiPathHelper() {
        try(Reader reader = new InputStreamReader(io.vertx.shiva.MainVerticle.class.getClassLoader().getResourceAsStream("signavioapi.json"), "UTF-8")){
            JsonParser jsonParser = new JsonParser();
            this.jsonObject = (JsonObject)jsonParser.parse(reader);
            
        }//end try
        catch (IOException e) {
           System.out.println(e.getMessage());
        }//end catch

      
        apiPath = this.jsonObject.get("sig_uri").getAsString() + ":" + this.jsonObject.get("sig_port").getAsString() + "/" + this.jsonObject.get("sig_apiver").getAsString()+ this.jsonObject.get("sig_org").getAsString();
    }

    public String getFile()
    {
        return apiPath + files;
    }
    public String getCases()
    {
        return apiPath + cases;
    }

    public String getUri()
    {
        return this.jsonObject.get("sig_uri").getAsString();
    }
    public Integer getPort()
    {
        return this.jsonObject.get("sig_port").getAsInt();
    }
    public String getOrg()
    {
        return this.jsonObject.get("sig_org").getAsString();
    }
    public String getApiVer()
    {
        return this.jsonObject.get("sig_apiver").getAsString();
    }

    
}
