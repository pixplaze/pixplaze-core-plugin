package com.pixplaze.http.server;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.pixplaze.http.HttpStatus;
import com.pixplaze.http.exceptions.HttpException;
import com.pixplaze.plugin.PixplazeCorePlugin;
import org.checkerframework.checker.units.qual.A;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;


/**
 * @since 0.1.2-indev
 */
public class ResponseBuilder {
	private final Gson gson;
	private final Charset charset;

	private boolean hasChanges = true;
	private HttpStatus status;
	private HttpException error;

	private final Logger logger = PixplazeCorePlugin.getInstance().getLogger();

	private Object body;
	private byte[] bytes;

	private record BadResponse(Integer code, String error, String message, String details) {}

	public ResponseBuilder() {
		this.gson = new GsonBuilder()
				.setFieldNamingStrategy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
				.create();
		this.charset = StandardCharsets.UTF_8;
	}

	public ResponseBuilder append(Throwable error) {
		if (error instanceof HttpException httpException) {
			return this.setError(httpException);
		}
		return this.setError(error);
	}

	public ResponseBuilder append(HttpException error) {
		return this.setError(error);
	}

	public ResponseBuilder append(byte[] body) {
		return this.append(body, HttpStatus.OK);
	}

	public ResponseBuilder append(byte[] body, HttpStatus status) {
		return this.setBody(body, status);
	}

	public ResponseBuilder append(Object body) {
		return this.append(body, HttpStatus.OK);
	}

	public ResponseBuilder append(Object body, HttpStatus status) {
		return this.setBody(body, status);
	}

	public ResponseBuilder flush() {
		this.error = null;
		this.status = null;
		this.bytes = null;
		this.body = null;
		return this;
	}

	public ResponseBuilder commit() {
		if (this.hasChanges) {
			this.hasChanges = false;

			if (this.status == null) this.status = HttpStatus.INTERNAL_ERROR;

			if (this.status.isSuccessful()) {
				if (this.bytes != null && this.bytes.length > 0) {
					return this;
				} else if (this.body != null) {
					this.bytes = gson.toJson(this.body).getBytes(charset);
					return this;
				} else {
					this.status = HttpStatus.INTERNAL_ERROR;
					this.body = new BadResponse(this.status.getCode(), null, "No response data", null);
					this.bytes = gson.toJson(this.body).getBytes(charset);
				}
			}

			if (this.status.isServerError() ||
				this.status.isClientError() ||
				this.status.isRedirected())
			{
				var code = this.error.getStatus().getCode();

				var error = new AtomicReference<String>();
				var message = new AtomicReference<String>();
				var details = new AtomicReference<String>();

				Optional.ofNullable(this.error.getCause()).ifPresentOrElse(
						err -> {
							var trace = new StringBuilder();
							Arrays.stream(err.getStackTrace()).forEach(e -> {
								if (!e.isNativeMethod()) {
									trace.append(
											"%s#%s(...) in line %d;%n"
											.formatted(e.getClassName(), e.getMethodName(), e.getLineNumber())
									);
								}
							});
							error.set(err.getClass().getSimpleName());
							message.set(err.getMessage());
							details.set(trace.toString());

						},
						() -> {
							error.set(this.error.getClass().getSimpleName());
							message.set(this.error.getMessage());
						}
				);



				if (message.get() == null || message.get().isBlank())
					message.set(this.error.getStatus().getMessage());

				this.body = new BadResponse(code, error.get(), message.get(), details.get());
				this.bytes = gson.toJson(this.body).getBytes(charset);

				return this;
			}
		}

		return this;
	}

	public ResponseBuilder setStatus(HttpStatus status) {
		this.status = status;
		this.hasChanges = true;
		return this;
	}

	public ResponseBuilder setStatus(int code) {
		this.status = HttpStatus.forCode(code);
		this.hasChanges = true;
		return this;
	}

	public ResponseBuilder setError(HttpException error) {
		this.status = error.getStatus();
		this.error = error;

		this.hasChanges = true;

		return this;
	}

	public ResponseBuilder setError(Throwable error) {
		this.status = HttpStatus.INTERNAL_ERROR;
		this.error = new HttpException(this.status, error, error.getMessage());

		this.hasChanges = true;

		return this;
	}

	public ResponseBuilder setBody(Object bytes) {
		return this.setBody(body, HttpStatus.OK);
	}

	public ResponseBuilder setBody(Object body, HttpStatus status) {
		this.hasChanges = true;

		this.status = status;
		this.body = body;
		this.bytes = null;
		this.error = null;

		return this;
	}

	public ResponseBuilder setBody(byte[] bytes) {
		return this.setBody(bytes, HttpStatus.OK);
	}

	public ResponseBuilder setBody(byte[] bytes, HttpStatus status) {
		this.hasChanges = true;

		this.status = status;
		this.bytes = bytes;
		this.body = null;
		this.error = null;

		return this;
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

	public byte[] toBytes() {
		return this.commit().bytes;
	}

	@Override
	public String toString() {
		return gson.toJson(this.body);
	}

}
