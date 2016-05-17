package com.ericwadkins.request;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ericwadkins.request.FormData.FormDataType;

/**
 * A mutable request object. This class provides methods to send HTTP and HTTPS
 * requests, and is designed to act as a wrapper for the URLConnection class,
 * as well as provide extra data management methods.
 * 
 * @author ericwadkins
 */
public class Request {

	/**
	 * The type of request method to send.
	 * 
	 * @author ericwadkins
	 */
	public static enum RequestMethod {
		GET, POST, PUT, DELETE
	};

	/**
	 * The type of data to be added to the body.
	 * 
	 * @author ericwadkins
	 */
	public static enum BodyType {
		FORM_DATA, X_WWW_FORM_URLENCODED, RAW, JSON, BINARY
	};

	// Data storage
	private URL url;
	private RequestMethod requestMethod = RequestMethod.GET;
	private final Map<String, String> requestProperties = new HashMap<>();
	private BodyType bodyType = BodyType.RAW;
	private final Map<String, List<FormData>> formData =
			new HashMap<>();
	private final Map<String, List<String>> encodedFormData = new HashMap<>();
	private StringBuilder rawData = new StringBuilder();
	private JSONObject jsonObjData = new JSONObject();
	private JSONArray jsonArrData = new JSONArray();
	private final ByteArrayOutputStream binaryData =
			new ByteArrayOutputStream();

	// Accessory values used in the writing of the body
	private final String boundary =
			Long.toHexString(System.currentTimeMillis());
	private static final String CRLF = "\r\n";
	private static final Charset defaultCharset = Charset.forName("utf-8");

	/**
	 * Constructs a Request object with the specified URL string. If a protocol
	 * is not specified, HTTP will be used. Valid protocols include HTTP and
	 * HTTPS.
	 * 
	 * @param urlString the URL
	 */
	public Request(String urlString) {
		setURL(urlString);
	}
	
	/**
	 * Constructs a Request object with the specified URL. Valid protocols
	 * include HTTP and HTTPS.
	 * 
	 * @param urlString the URL
	 */
	public Request(URL url) {
		setURL(url);
	}

	/**
	 * Constructs an empty Request object. Must later set the URL with
	 * setURL().
	 */
	public Request() {
	}

	/**
	 * Sends a request of the set method with this request object. This method
	 * is useful when calls must be made programmatically in conjuction with
	 * setMethod() instead of by calling the specific GET, POST, etc. methods.
	 * 
	 * @return the response object
	 * @throws IOException if an error occurs
	 */
	public Response send() throws IOException {
		Response response = null;
		/*Method method;
		try {
			method = getClass().getMethod(requestMethod.name());
			response = (Response) method.invoke(this);
		} catch (NoSuchMethodException | SecurityException
				| IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw e;
		}*/
		switch (requestMethod) {
		case GET: return GET();
		case POST: return POST();
		case PUT: return PUT();
		case DELETE: return DELETE();
		}
		return response;
	}

	/**
	 * A static convenience method for quick GET requests to the specified URL.
	 * 
	 * @param urlString the URL
	 * @return the response object
	 * @throws IOException if an error occurs
	 */
	public static Response GET(String urlString) throws IOException {
		return new Request(urlString).GET();
	}

	/**
	 * Sends a GET request with this request object.
	 * 
	 * @return the response object
	 * @throws IOException if an error occurs
	 */
	public Response GET() throws IOException {
		URLConnection connection = null;
		try {
			connection = connect();
			setRequestMethod(connection, "GET");
			connection.setUseCaches(false);
			applyRequestProperties(connection);
			Response response = Response.parse(connection);
			return response;
		} catch (IOException e) {
			throw e;
		} finally {
			disconnect(connection);
		}
	}

	/**
	 * Sends a POST request with this request object.
	 * 
	 * @return the response object
	 * @throws IOException if an error occurs
	 */
	public Response POST() throws IOException {
		URLConnection connection = null;
		try {
			connection = connect();
			setRequestMethod(connection, "POST");
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			applyRequestProperties(connection);
			addBody(connection);
			Response response = Response.parse(connection);
			return response;
		} catch (IOException e) {
			throw e;
		} finally {
			disconnect(connection);
		}
	}

	/**
	 * Sends a PUT request with this request object.
	 * 
	 * @return the response object
	 * @throws IOException if an error occurs
	 */
	public Response PUT() throws IOException {
		URLConnection connection = null;
		try {
			connection = connect();
			setRequestMethod(connection, "PUT");
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			applyRequestProperties(connection);
			addBody(connection);
			Response response = Response.parse(connection);
			return response;
		} catch (IOException e) {
			throw e;
		} finally {
			disconnect(connection);
		}
	}

	/**
	 * Sends a DELETE request with this request object.
	 * 
	 * @return the response object
	 * @throws IOException if an error occurs
	 */
	public Response DELETE() throws IOException {
		URLConnection connection = null;
		try {
			connection = connect();
			setRequestMethod(connection, "DELETE");
			connection.setUseCaches(false);
			applyRequestProperties(connection);
			Response response = Response.parse(connection);
			return response;
		} catch (IOException e) {
			throw e;
		} finally {
			disconnect(connection);
		}
	}

	/**
	 * Sets this request object's URL. If a protocol is not specified, HTTP
	 * will be used. Valid protocols include HTTP and HTTPS.
	 * 
	 * @param urlString
	 */
	public void setURL(String urlString) {
		if (!urlString.startsWith("http://") &&
				!urlString.startsWith("https://")) {
			urlString = "http://" + urlString; // HTTP is default protocol
		}
		try {
			this.url = new URL(urlString);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}

	/**
	 * Sets this request object's URL. Valid protocols include HTTP and HTTPS.
	 * 
	 * @param url the url
	 */
	public void setURL(URL url) {
		this.url = url;
	}

	/**
	 * Returns this request object's URL.
	 * 
	 * @return the URL
	 */
	public String getURL() {
		return url.toString();
	}
	
	/**
	 * Returns the protocol of this request object (either http or https). The
	 * protocol of a request is automatically determined upon setting the URL.
	 * 
	 * @return the protocol
	 */
	public String getProtocol() {
		return url.getProtocol();
	}
	
	/**
	 * Sets this request object's method to the specified requestMethod. This
	 * method is useful when calls must be made programmatically in conjuction
	 * with send() instead of by calling the specific GET, POST, etc. methods.
	 * 
	 * @param requestMethod the method
	 */
	public void setMethod(RequestMethod requestMethod) {
		this.requestMethod = requestMethod;
	}

	/**
	 * Sets this request object's method to the specified requestMethod. This
	 * method is useful when calls must be made programmatically in conjuction
	 * with send() instead of by calling the specific GET, POST, etc. methods.
	 * 
	 * @param requestMethod the method represented as a string
	 */
	public void setMethod(String method) {
		this.requestMethod = RequestMethod.valueOf(method.toUpperCase());
	}
	
	/**
	 * Returns this request object's method. This method is useful when calls
	 * must be made programmatically in conjuction with send() and setMethod()
	 * instead of by calling the specific GET, POST, etc. methods.
	 * 
	 * @return the method
	 */
	public RequestMethod getMethod() {
		return requestMethod;
	}

	/**
	 * Sets a request property (or header) with the specified key and value.
	 * 
	 * @param key
	 * @param value
	 * @return the previous value associated with key, or null if there was none
	 */
	public String setRequestProperty(String key, String value) {
		return requestProperties.put(key, value);
	}

	/**
	 * Sets a request property (or header) with the specified key and value and
	 * immediately updates the connection. This method is used to assigned a
	 * Content-Type when adding the body if one has not already been specified.
	 * 
	 * @param key
	 * @param value
	 * @param connection
	 * @return the previous value associated with key, or null if there was none
	 */
	private String setRequestPropertyImmediate(String key, String value,
			URLConnection connection) {
		connection.setRequestProperty(key, value);
		return requestProperties.put(key, value);
	}

	/**
	 * Returns the request properties, in the form of a map from keys to values.
	 * 
	 * @return the request properties
	 */
	public Map<String, String> getRequestProperties() {
		return new HashMap<>(requestProperties);
	}

	/**
	 * Returns the request property with the specified key.
	 * 
	 * @param key
	 * @return the request property, or null if it doesn't exist
	 */
	public String getRequestProperty(String key) {
		return requestProperties.get(key);
	}

	/**
	 * Adds a field with the specified key, value and charset to the form, and
	 * updates the body to use multipart/form-data in the future.
	 * 
	 * @param key
	 * @param value
	 * @return the form data associated with the key
	 */
	public List<FormData> addFormField(String key, String value,
			Charset charset) {
		bodyType = BodyType.FORM_DATA;
		if (!formData.containsKey(key)) {
			formData.put(key, new ArrayList<FormData>());
		}
		formData.get(key).add(new FormData(value, charset));
		return new ArrayList<>(formData.get(key));
	}

	/**
	 * Adds a field with the specified key and value to the form, and updates
	 * the body to use multipart/form-data in the future.
	 * 
	 * @param key
	 * @param value
	 * @return the form data associated with the key
	 */
	public List<FormData> addFormField(String key, String value) {
		return addFormField(key, value, defaultCharset);
	}

	/**
	 * Adds a raw file to the form with the specified key and charset, and
	 * updates the body to use multipart/form-data in the future.
	 * 
	 * @param key
	 * @param file
	 * @param charset
	 * @return the form data associated with the key
	 */
	public List<FormData> addFormRawFile(String key, File file,
			Charset charset) {
		bodyType = BodyType.FORM_DATA;
		if (!formData.containsKey(key)) {
			formData.put(key, new ArrayList<FormData>());
		}
		formData.get(key).add(
				new FormData(file, FormDataType.RAW_FILE, charset));
		return new ArrayList<>(formData.get(key));
	}

	/**
	 * Adds a raw file to the form with the specified key, and updates the body
	 * to use multipart/form-data in the future.
	 * 
	 * @param key
	 * @param file
	 * @return the form data associated with the key
	 */
	public List<FormData> addFormRawFile(String key, File file) {
		return addFormRawFile(key, file, defaultCharset);
	}

	/**
	 * Adds a binary file to the form with the specified key and charset, and
	 * updates the body to use multipart/form-data in the future.
	 * 
	 * @param key
	 * @param file
	 * @param defaultCharset
	 * @return the form data associated with the key
	 */
	public List<FormData> addFormBinaryFile(String key, File file) {
		bodyType = BodyType.FORM_DATA;
		if (!formData.containsKey(key)) {
			formData.put(key, new ArrayList<FormData>());
		}
		formData.get(key).add(
				new FormData(file, FormDataType.BINARY_FILE, defaultCharset));
		return new ArrayList<>(formData.get(key));
	}

	/**
	 * Removes the value with the specified key from the form, if it exists.
	 * 
	 * @param key
	 * @return the form data associated with the key, or null if there was none
	 */
	public List<FormData> removeFormField(
			String key) {
		return formData.remove(key);
	}

	/**
	 * Removes all the values from the form.
	 * 
	 * @return the form data
	 */
	public Map<String, List<FormData>> clearForm() {
		Map<String, List<FormData>> data = new HashMap<>(formData);
		formData.clear();
		return data;
	}

	/**
	 * Adds a field with the specified key and value to the encoded URL form,
	 * and updates the body to use application/x-www-form-urlencoded in the
	 * future.
	 * 
	 * @param key
	 * @param value
	 * @return the encoded URL form data associated with the key
	 */
	public List<String> addEncodedField(String key, String value) {
		bodyType = BodyType.X_WWW_FORM_URLENCODED;
		if (!encodedFormData.containsKey(key)) {
			encodedFormData.put(key, new ArrayList<String>());
		}
		encodedFormData.get(key).add(value);
		return new ArrayList<>(encodedFormData.get(key));
	}

	/**
	 * Removes the value with the specified key from the encoded URL form, if it
	 * exists.
	 * 
	 * @param key
	 * @return the encoded URL form data associated with the key, or null if
	 * there was none
	 */
	public List<String> removeEncodedField(String key) {
		return encodedFormData.remove(key);
	}

	/**
	 * Removes all the values from the encoded URL form.
	 * 
	 * @return the encoded URL form data
	 */
	public Map<String, List<String>> clearEncodedFields() {
		Map<String, List<String>> data = new HashMap<>(encodedFormData);
		encodedFormData.clear();
		return data;
	}

	/**
	 * Appends the string to the raw data, and updates the body to use
	 * text/plain in the future.
	 * 
	 * @param string
	 * @return the raw data
	 */
	public String addRawData(String string) {
		bodyType = BodyType.RAW;
		return rawData.append(string).toString();
	}

	/**
	 * Appends the file's contents to the raw data, and updates the body to use
	 * text/plain in the future.
	 * 
	 * @param file
	 * @return the raw data
	 * @throws IOException if an error occurs
	 */
	public String addRawData(File file) throws IOException {
		bodyType = BodyType.RAW;
		rawData.append(new String(Files.readAllBytes(Paths.get(
				file.getAbsolutePath())), defaultCharset));
		return rawData.toString();
	}

	/**
	 * Removes all the raw data.
	 * 
	 * @return the raw data
	 */
	public String clearRawData() {
		String string = rawData.toString();
		rawData = new StringBuilder();
		return string;
	}

	/**
	 * Sets the JSON data to the specified JSON object, and updates the body to
	 * use application/json in the future.
	 * 
	 * @param jsonObj
	 * @return the JSON data, in the form of an Object (is an instance of a
	 *         JSONObject or a JSONArray)
	 */
	public Object addJsonData(JSONObject jsonObj) {
		Object data = null;
		try {
			data = (jsonObjData != null ? new JSONObject(jsonObjData) :
				new JSONArray(jsonArrData));
		} catch (JSONException e) {}
		bodyType = BodyType.JSON;
		this.jsonObjData = jsonObj;
		this.jsonArrData = null;
		return data;
	}

	/**
	 * Sets the JSON data to the specified JSON array, and updates the body to
	 * use application/json in the future.
	 * 
	 * @param jsonArr
	 * @return the JSON data, in the form of an Object (is an instance of a
	 *         JSONObject or a JSONArray)
	 */
	public Object addJsonData(JSONArray jsonArr) {
		Object data = null;
		try {
			data = (jsonObjData != null ? new JSONObject(jsonObjData) :
				new JSONArray(jsonArrData));
		} catch (JSONException e) {}
		bodyType = BodyType.JSON;
		this.jsonObjData = null;
		this.jsonArrData = jsonArr;
		return data;
	}

	/**
	 * Parses the string into JSON data and stores it, and updates the body to
	 * use application/json in the future.
	 * 
	 * @param jsonArr
	 * @return the JSON data, in the form of an Object (is an instance of a
	 *         JSONObject or a JSONArray)
	 * @throws JSONException if the string could not be parsed into valid JSON
	 */
	public Object addJsonData(String json) throws JSONException {
		Object data = null;
		try {
			data = (jsonObjData != null ? new JSONObject(jsonObjData) :
				new JSONArray(jsonArrData));
		} catch (JSONException e) {}
		boolean added = false;
		try {
			jsonObjData = new JSONObject(json);
			added = true;
		} catch (JSONException e1) {
			try {
				jsonArrData = new JSONArray(json);
				added = true;
			} catch (JSONException e2) {
			}
		}
		if (added) {
			bodyType = BodyType.JSON;
		}
		else {
			throw new JSONException(
					"Could not be parsed into a JSONObject nor a JSONArray");
		}
		return data;
	}

	/**
	 * Removes the JSON data.
	 * 
	 * @return the JSON data, in the form of an Object (is an instance of a
	 *         JSONObject or a JSONArray)
	 */
	public Object clearJsonData() throws JSONException {
		Object data = null;
		try {
			data = (jsonObjData != null ? new JSONObject(jsonObjData) :
				new JSONArray(jsonArrData));
		} finally {
			jsonObjData = null;
			jsonArrData = null;
		}
		return data;
	}

	/**
	 * Appends the byte array to the binary data, and updates the body to use
	 * application/octet-stream in the future.
	 * 
	 * @param bytes
	 * @return the complete binary data
	 * @throws IOException if an error occurs
	 */
	public byte[] addBinaryData(byte[] bytes) throws IOException {
		bodyType = BodyType.BINARY;
		binaryData.write(bytes);
		return binaryData.toByteArray();
	}

	/**
	 * Appends the binary content of the file to the binary data, and updates
	 * the body to use application/octet-stream in the future.
	 * 
	 * @param bytes
	 * @return the complete binary data
	 * @throws IOException if an error occurs
	 */
	public byte[] addBinaryData(File file) throws IOException {
		bodyType = BodyType.BINARY;
		binaryData.write(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
		return binaryData.toByteArray();
	}

	/**
	 * Removes the binary data.
	 * 
	 * @return the binary data
	 */
	public byte[] clearBinaryData() {
		byte[] data = binaryData.toByteArray();
		binaryData.reset();
		return data;
	}

	/**
	 * Updates the body to use multipart/form-data in the future.
	 */
	public void useForm() {
		bodyType = BodyType.FORM_DATA;
	}

	/**
	 * Updates the body to use application/x-www-form-urlencoded in the future.
	 */
	public void useEncodedForm() {
		bodyType = BodyType.X_WWW_FORM_URLENCODED;
	}

	/**
	 * Updates the body to use text/plain in the future.
	 */
	public void useRawData() {
		bodyType = BodyType.RAW;
	}

	/**
	 * Updates the body to use application/json in the future.
	 */
	public void useJson() {
		bodyType = BodyType.JSON;
	}

	/**
	 * Updates the body to use application/octet-stream in the future.
	 */
	public void useBinaryData() {
		bodyType = BodyType.BINARY;
	}

	/**
	 * Returns the type of data the body will use.
	 * 
	 * @return the BodyType of this request object
	 */
	public BodyType getBodyType() {
		return bodyType;
	}
	
	/**
	 * A helper method that sets the connection's method, regardless of the
	 * protocol being used.
	 * 
	 * @param connection
	 * @param method
	 */
	private static void setRequestMethod(URLConnection connection,
			String method) {
		try {
			switch (connection.getURL().getProtocol()) {
			case "http": 
				((HttpURLConnection) connection).setRequestMethod(method);
				break;
			case "https": 
				((HttpsURLConnection) connection).setRequestMethod(method);
				break;
			default: throw new RuntimeException("Unsupported request method");
			}
		} catch (ProtocolException e) {
			throw new RuntimeException("Attempt to set request method failed");
		}
	}

	/**
	 * Updates the connection's request properties.
	 * 
	 * @param connection
	 */
	private void applyRequestProperties(URLConnection connection) {
		for (String key : requestProperties.keySet()) {
			connection.setRequestProperty(key, requestProperties.get(key));
		}
	}

	/**
	 * Adds the body to the request depending on the request's BodyType
	 * 
	 * @param connection
	 * @throws IOException if an error occurs
	 */
	private void addBody(URLConnection connection) throws IOException {
		boolean contentTypeSet = false;
		for (String key : requestProperties.keySet()) {
			if (key.equalsIgnoreCase("Content-Type")) {
				contentTypeSet = true;
			}
		}
		switch (bodyType) {
		case FORM_DATA:
			if (!contentTypeSet)
				setRequestPropertyImmediate("Content-Type",
						"multipart/form-data; boundary=" + boundary,connection);
			addFormData(connection);
			break;
		case X_WWW_FORM_URLENCODED:
			if (!contentTypeSet)
				setRequestPropertyImmediate("Content-Type",
						"application/x-www-form-urlencoded", connection);
			addEncodedFormData(connection);
			break;
		case RAW:
			if (!contentTypeSet)
				setRequestPropertyImmediate("Content-Type",
						"text/plain", connection);
			addRawData(connection);
			break;
		case JSON:
			if (!contentTypeSet)
				setRequestPropertyImmediate("Content-Type",
						"application/json", connection);
			addJsonData(connection);
			break;
		case BINARY:
			if (!contentTypeSet)
				setRequestPropertyImmediate("Content-Type",
						"application/octet-stream", connection);
			addBinaryData(connection);
			break;
		default:
			throw new UnsupportedOperationException(bodyType.name() 
					+ " not supported");
		}
	}

	/**
	 * Writes the form data to the connection's output stream
	 * 
	 * @param connection
	 * @throws IOException if an error occurs
	 */
	private void addFormData(URLConnection connection) throws IOException {
		try (OutputStream output = connection.getOutputStream();
				PrintWriter writer = new PrintWriter(
						new OutputStreamWriter(output), false)) {
			for (String key : formData.keySet()) {
				for (FormData formData :
					formData.get(key)) {
					if (formData.getType() == FormDataType.FIELD) {
						String value = formData.getField();

						writer.append("--" + boundary).append(CRLF);
						writer.append("Content-Disposition: form-data; name=\""
								+ key + "\"").append(CRLF);
						writer.append("Content-Type: text/plain; charset="
								+ formData.getCharset()).append(CRLF);
						writer.append(CRLF).append(value).append(CRLF).flush();
					} else if (formData.getType() == FormDataType.RAW_FILE) {
						File file = formData.getFile();

						writer.append("--" + boundary).append(CRLF);
						writer.append("Content-Disposition: form-data; name=\""
								+ key + "\"; filename=\""
								+ file.getName() + "\"").append(CRLF);
						writer.append("Content-Type: text/plain; charset="
								+ formData.getCharset()).append(CRLF);
						writer.append(CRLF).flush();
						try {
							Files.copy(file.toPath(), output);
							output.flush();
						} catch (IOException e) {
							throw e;
						}
						writer.append(CRLF).flush();
					} else if (formData.getType() == FormDataType.BINARY_FILE) {
						File file = formData.getFile();

						writer.append("--" + boundary).append(CRLF);
						writer.append("Content-Disposition: form-data; name=\""
								+ key + "\"; filename=\""
								+ file.getName() + "\"").append(CRLF);
						writer.append("Content-Type: "
								+ URLConnection.guessContentTypeFromName(
										file.getName()))
								.append(CRLF);
						writer.append("Content-Transfer-Encoding: binary")
						.append(CRLF);
						writer.append(CRLF).flush();
						try {
							Files.copy(file.toPath(), output);
							output.flush();
						} catch (IOException e) {
							throw e;
						}
						writer.append(CRLF).flush();
					}
				}
			}
			writer.append("--" + boundary + "--").append(CRLF).flush();
		} catch (IOException e) {
			throw e;
		}
	}

	/**
	 * Writes the encoded URL form data to the connection's output stream
	 * 
	 * @param connection
	 * @throws IOException if an error occurs
	 */
	private void addEncodedFormData(URLConnection connection) throws IOException {
		try (OutputStream output = connection.getOutputStream()) {
			StringBuilder sb = new StringBuilder();
			for (String key : encodedFormData.keySet()) {
				for (String value : encodedFormData.get(key)) {
					String encodedKey = URLEncoder.encode(key, defaultCharset.name());
					String encodedValue = URLEncoder.encode(value, defaultCharset.name());
					sb.append(encodedKey + "=" + encodedValue + "&");
				}
			}
			String encodedData = sb.substring(0, sb.length() - 1);
			output.write(encodedData.getBytes());
		} catch (IOException e) {
			throw e;
		}
	}

	/**
	 * Writes the raw data to the connection's output stream
	 * 
	 * @param connection
	 * @throws IOException if an error occurs
	 */
	private void addRawData(URLConnection connection) throws IOException {
		try (OutputStream output = connection.getOutputStream()) {
			String encodedData = rawData.toString();
			output.write(encodedData.getBytes());
		} catch (IOException e) {
			throw e;
		}
	}

	/**
	 * Writes the JSON data to the connection's output stream
	 * 
	 * @param connection
	 * @throws IOException if an error occurs
	 */
	private void addJsonData(URLConnection connection) throws IOException {
		try (OutputStream output = connection.getOutputStream()) {
			String encodedData = (jsonObjData != null ?
					jsonObjData :jsonArrData).toString();
			output.write(encodedData.getBytes());
		} catch (IOException e) {
			throw e;
		}
	}

	/**
	 * Writes the binary data to the connection's output stream
	 * 
	 * @param connection
	 * @throws IOException if an error occurs
	 */
	private void addBinaryData(URLConnection connection) throws IOException {
		try (OutputStream output = connection.getOutputStream()) {
			binaryData.writeTo(output);
		} catch (IOException e) {
			throw e;
		}
	}

	/**
	 * A helper method that creates and returns a connection
	 * 
	 * @return the connection
	 * @throws IOException if an error occurs
	 */
	private URLConnection connect() throws IOException {
		if (url == null) {
			throw new RuntimeException(
					"Must specify a URL - call setURL(urlString)");
		}
		return url.openConnection();
	}

	/**
	 * A helper method that disconnects the given connection, regardless of the
	 * protocol being used.
	 * 
	 * @param connection the connection
	 */
	private static void disconnect(URLConnection connection) {
		if (connection != null) {
			String protocol = connection.getURL().getProtocol();
			switch (protocol) {
			case "http": ((HttpURLConnection) connection).disconnect(); break;
			case "https": ((HttpsURLConnection) connection).disconnect(); break;
			default: throw new UnsupportedOperationException(
					"Request does not support " + protocol + " requests");
			}
		}
	}

}
