
package io.vertx.shiva.liquor;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Field {

    @SerializedName("elementType")
    @Expose
    public String elementType;
    @SerializedName("id")
    @Expose
    public String id;
    @SerializedName("name")
    @Expose
    public String name;
    @SerializedName("type")
    @Expose
    public Type type;
    @SerializedName("visible")
    @Expose
    public Boolean visible;
    @SerializedName("readOnly")
    @Expose
    public Boolean readOnly;
    @SerializedName("required")
    @Expose
    public Boolean required;
    @SerializedName("value")
    @Expose
    public String value;

    /**
     * No args constructor for use in serialization
     * 
     */
    public Field() {
    }

    /**
     * 
     * @param id
     * @param readOnly
     * @param visible
     * @param name
     * @param value
     * @param required
     * @param type
     * @param elementType
     */
    public Field(String elementType, String id, String name, Type type, Boolean visible, Boolean readOnly, Boolean required, String value) {
        super();
        this.elementType = elementType;
        this.id = id;
        this.name = name;
        this.type = type;
        this.visible = visible;
        this.readOnly = readOnly;
        this.required = required;
        this.value = value;
    }

    public String getElementType() {
        return elementType;
    }

    public void setElementType(String elementType) {
        this.elementType = elementType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public Boolean getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
