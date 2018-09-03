
package io.vertx.shiva.liquor;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Whisky {

    @SerializedName("triggerInstance")
    @Expose
    public TriggerInstance triggerInstance;

    /**
     * No args constructor for use in serialization
     * 
     */
    public Whisky() {
    }

    /**
     * 
     * @param triggerInstance
     */
    public Whisky(TriggerInstance triggerInstance) {
        super();
        this.triggerInstance = triggerInstance;
    }

    public TriggerInstance getTriggerInstance() {
        return triggerInstance;
    }

    public void setTriggerInstance(TriggerInstance triggerInstance) {
        this.triggerInstance = triggerInstance;
    }

}
