package com.pixplaze.controllers;

import com.google.gson.JsonObject;

@Deprecated(since = "0.1.2-indev", forRemoval = true)
public class ResponseBodyBuilder {

    private String message = null;
    private JsonObject response = null;
    private String error = null;

    public ResponseBodyBuilder setMessage(String message) {
        this.message = message;
        return this;
    }

    public ResponseBodyBuilder setResponse(JsonObject response) {
        this.response = response;
        return this;
    }

    public ResponseBodyBuilder setError(String error) {
        this.error = error;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public JsonObject getResponse() {
        return response;
    }

    public String getError() {
        return error;
    }

    public String getFinal() {
        JsonObject bodyResult = new JsonObject();
        if (message != null) {
            bodyResult.addProperty("message", message);
        }
        if (response != null) {
            bodyResult.add("response", response);
        }
        if (error != null) {
            bodyResult.addProperty("error", error);
        }
        return bodyResult.toString();
    }
}
