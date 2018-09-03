
package io.vertx.shiva.liquor;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Data {

    @SerializedName("formInstance")
    @Expose
    public FormInstance formInstance;

    /**
     * No args constructor for use in serialization
     * 
     */
    public Data() {
    }

    /**
     * 
     * @param formInstance
     */
    public Data(FormInstance formInstance) {
        super();
        this.formInstance = formInstance;
    }

    public FormInstance getFormInstance() {
        return formInstance;
    }

    public void setFormInstance(FormInstance formInstance) {
        this.formInstance = formInstance;
    }

}
