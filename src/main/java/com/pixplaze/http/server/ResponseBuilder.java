package com.pixplaze.http.server;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.pixplaze.http.HttpStatus;
import com.pixplaze.http.exceptions.HttpException;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ResponseBuilder {
	private final Gson gson;
	private final Charset charset;

	private boolean hasChanges = true;

	private HttpStatus status;
	private HttpException error;

	private Object body;
	private byte[] bytes;

	public ResponseBuilder() {
		this.gson = new GsonBuilder()
				.setFieldNamingStrategy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
				.create();
		this.charset = StandardCharsets.UTF_8;
	}

	private record BadResponse(Integer code, String message, String error) {}

	public ResponseBuilder append(Throwable error) {
		this.error = new HttpException(HttpStatus.INTERNAL_ERROR, error, error.getMessage());
		this.status = this.error.getStatus();
		this.hasChanges = true;
		return this;
	}

	public ResponseBuilder append(HttpException error) {
		this.error = error;
		this.status = this.error.getStatus();
		this.hasChanges = true;
		return this;
	}

	public ResponseBuilder append(Object body) {
		return this.append(body, HttpStatus.OK);
	}

	public ResponseBuilder append(Object body, HttpStatus status) {
		this.body = body;
		this.status = status;
		this.hasChanges = true;
		return this;
	}

	public ResponseBuilder setStatus(HttpStatus status) {
		this.status = status;
		return this;
	}

	public ResponseBuilder setStatus(int code) {
		this.status = HttpStatus.forCode(code);
		return this;
	}

	public ResponseBuilder commit() {
		if (this.hasChanges) {
			this.hasChanges = false;

			if (this.status.isSuccessful()) {
				if (this.bytes != null && this.bytes.length > 0) {
					return this;
				} else if (this.body != null) {
					this.bytes = gson.toJson(this.body).getBytes(charset);
					return this;
				}
			}

			if (this.error != null) {
				this.body = new BadResponse(
						this.error.getStatus().getCode(),
						this.error.getCause().getClass().getSimpleName(),
						this.error.getStatus().getMessage()
				);
				this.bytes = gson.toJson(this.body).getBytes(charset);
				return this;
			}

			this.status = HttpStatus.INTERNAL_ERROR;
			this.body = new BadResponse(this.status.getCode(), "No response data", null);
			this.bytes = gson.toJson(this.body).getBytes(charset);
		}
		return this;
	}

	public byte[] toBytes() {
		return this.commit().bytes;
	}

	public int getCode() {
		return this.commit().status.getCode();
	}

	public String getMessage() {
		return this.commit().status.getMessage();
	}

	public HttpStatus getStatus() {
		return this.commit().status;
	}

	public int getLength() {
		return this.commit().bytes.length;
	}
}
