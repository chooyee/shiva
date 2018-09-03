
package io.vertx.shiva.liquor;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class FormInstance {

    @SerializedName("value")
    @Expose
    public Value value;

    /**
     * No args constructor for use in serialization
     * 
     */
    public FormInstance() {
    }

    /**
     * 
     * @param value
     */
    public FormInstance(Value value) {
        super();
        this.value = value;
    }

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }

}
