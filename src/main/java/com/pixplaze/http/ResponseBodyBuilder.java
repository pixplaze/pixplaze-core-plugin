package com.pixplaze.http;

import org.json.JSONObject;

public class ResponseBodyBuilder {

    private String message = null;
    private JSONObject response = null;
    private String error = null;

    public ResponseBodyBuilder setMessage(String message) {
        this.message = message;
        return this;
    }

    public ResponseBodyBuilder setResponse(JSONObject response) {
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

    public JSONObject getResponse() {
        return response;
    }

    public String getError() {
        return error;
    }

    public String getFinal() {
        JSONObject bodyResult = new JSONObject();
        if (message != null) {
            bodyResult.put("message", message);
        }
        if (response != null) {
            bodyResult.put("response", response);
        }
        if (error != null) {
            bodyResult.put("error", error);
        }
        return bodyResult.toString(-1);
    }
}
