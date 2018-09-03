
package io.vertx.shiva.liquor;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class TriggerInstance {

    @SerializedName("sourceWorkflowId")
    @Expose
    public String sourceWorkflowId;
    @SerializedName("data")
    @Expose
    public Data data;

    /**
     * No args constructor for use in serialization
     * 
     */
    public TriggerInstance() {
    }

    /**
     * 
     * @param data
     * @param sourceWorkflowId
     */
    public TriggerInstance(String sourceWorkflowId, Data data) {
        super();
        this.sourceWorkflowId = sourceWorkflowId;
        this.data = data;
    }

    public String getSourceWorkflowId() {
        return sourceWorkflowId;
    }

    public void setSourceWorkflowId(String sourceWorkflowId) {
        this.sourceWorkflowId = sourceWorkflowId;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

}
