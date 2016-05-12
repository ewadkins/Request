package com.ericwadkins.request;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A mutable request object. This class provides methods to send HTTP requests,
 * and is designed to act as a wrapper for the HttpURLConnection class, as well
 * as provide extra data management methods.
 * 
 * @author ericwadkins
 */
public class Request {

	/**
	 * The type of data to be added to the body.
	 * 
	 * @author ericwadkins
	 */
	public static enum BodyType {
		FORM_DATA, X_WWW_FORM_URLENCODED, RAW, JSON, BINARY
	};

	/**
	 * The type of data contained in each part of the multipart form-data.
	 * 
	 * @author ericwadkins
	 */
	public static enum FormDataType {
		FIELD, RAW_FILE, BINARY_FILE
	}

	// Data storage
	private URL url;
	private final Map<String, String> requestProperties = new HashMap<>();
	private BodyType bodyType = BodyType.RAW;
	private final Map<String, List<Map.Entry<FormDataType, Object>>> formData =
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
	private static final String charset = "utf-8";

	/**
	 * Constructs a Request object with the specified URL.
	 * 
	 * @param urlString
	 *            the URL
	 */
	public Request(String urlString) {
		setURL(urlString);
	}

	/**
	 * Constructs an empty Request object. Must later set the URL with
	 * setURL(urlString).
	 */
	public Request() {
	}

	/**
	 * A static convenience method for quick GET requests to the specified URL.
	 * 
	 * @param urlString
	 *            the URL
	 * @return the response object
	 */
	public static Response GET(String urlString) {
		return new Request(urlString).GET();
	}

	/**
	 * Sends a GET request with this request object.
	 * 
	 * @return the response object
	 */
	public Response GET() {
		HttpURLConnection connection = null;
		try {
			connection = connect();
			connection.setUseCaches(false);
			applyRequestProperties(connection);
			Response response = Response.parse(connection);
			return response;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			disconnect(connection);
		}
		return null;
	}

	/**
	 * Sends a POST request with this request object.
	 * 
	 * @return the response object
	 */
	public Response POST() {
		HttpURLConnection connection = null;
		try {
			connection = connect();
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			applyRequestProperties(connection);
			addBody(connection);
			Response response = Response.parse(connection);
			return response;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			disconnect(connection);
		}
		return null;
	}

	/**
	 * Sends a PUT request with this request object.
	 * 
	 * @return the response object
	 */
	public Response PUT() {
		HttpURLConnection connection = null;
		try {
			connection = connect();
			connection.setRequestMethod("PUT");
			connection.setDoOutput(true);
			connection.setUseCaches(false);
			applyRequestProperties(connection);
			addBody(connection);
			Response response = Response.parse(connection);
			return response;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			disconnect(connection);
		}
		return null;
	}

	/**
	 * Sends a DELETE request with this request object.
	 * 
	 * @return the response object
	 */
	public Response DELETE() {
		HttpURLConnection connection = null;
		try {
			connection = connect();
			connection.setRequestMethod("DELETE");
			connection.setUseCaches(false);
			applyRequestProperties(connection);
			Response response = Response.parse(connection);
			return response;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			disconnect(connection);
		}
		return null;
	}

	/**
	 * Sets this request object's URL.
	 * 
	 * @param urlString
	 */
	public void setURL(String urlString) {
		try {
			this.url = new URL(urlString);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
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
			HttpURLConnection connection) {
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
	 * Adds a field with the specified key and value to the form, and updates
	 * the body to use multipart/form-data in the future.
	 * 
	 * @param key
	 * @param value
	 */
	public void addFormField(String key, String value) {
		bodyType = BodyType.FORM_DATA;
		if (!formData.containsKey(key)) {
			formData.put(key,
					new ArrayList<Map.Entry<Request.FormDataType, Object>>());
		}
		formData.get(key).add(new AbstractMap.SimpleEntry<>(
				FormDataType.FIELD, (Object) value));
	}

	/**
	 * Adds a raw file to the form with the specified key, and updates the body
	 * to use multipart/form-data in the future.
	 * 
	 * @param key
	 * @param file
	 */
	public void addFormRawFile(String key, File file) {
		bodyType = BodyType.FORM_DATA;
		if (!formData.containsKey(key)) {
			formData.put(key,
					new ArrayList<Map.Entry<Request.FormDataType, Object>>());
		}
		formData.get(key).add(new AbstractMap.SimpleEntry<>(FormDataType.RAW_FILE, (Object) file));
	}

	/**
	 * Adds a binary file to the form with the specified key, and updates the
	 * body to use multipart/form-data in the future.
	 * 
	 * @param key
	 * @param file
	 */
	public void addFormBinaryFile(String key, File file) {
		bodyType = BodyType.FORM_DATA;
		if (!formData.containsKey(key)) {
			formData.put(key,
					new ArrayList<Map.Entry<Request.FormDataType, Object>>());
		}
		formData.get(key).add(new AbstractMap.SimpleEntry<>(
				FormDataType.BINARY_FILE, (Object) file));
	}

	/**
	 * Removes the value with the specified key from the form, if it exists.
	 * 
	 * @param key
	 * @return the value associated with the key, or null if there was none
	 */
	public List<Map.Entry<Request.FormDataType, Object>> removeFormField(
			String key) {
		return formData.remove(key);
	}

	/**
	 * Removes all the values from the form.
	 * 
	 * @return a copy of the form
	 */
	public Map<String, List<Map.Entry<Request.FormDataType, Object>>> clearForm() {
		Map<String, List<Map.Entry<Request.FormDataType, Object>>> data =
				new HashMap<>(formData);
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
	 * @return the previous value associated with key, or null if there was none
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
	 * @return the value associated with the key, or null if there was none
	 */
	public List<String> removeEncodedField(String key) {
		return encodedFormData.remove(key);
	}

	/**
	 * Removes all the values from the encoded URL form.
	 * 
	 * @return a copy of the encoded URL form
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
	 * @return the complete raw data
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
	 * @return the complete raw data
	 */
	public String addRawData(File file) {
		bodyType = BodyType.RAW;
		try {
			rawData.append(new String(Files.readAllBytes(Paths.get(
					file.getAbsolutePath())), Charset.forName(charset)));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return rawData.toString();
	}

	/**
	 * Removes all the raw data
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
	 *            the JSON object
	 */
	public void addJsonData(JSONObject jsonObj) {
		bodyType = BodyType.JSON;
		this.jsonObjData = jsonObj;
		this.jsonArrData = null;
	}

	/**
	 * Sets the JSON data to the specified JSON array, and updates the body to
	 * use application/json in the future.
	 * 
	 * @param jsonArr
	 *            the JSON array
	 */
	public void addJsonData(JSONArray jsonArr) {
		bodyType = BodyType.JSON;
		this.jsonObjData = null;
		this.jsonArrData = jsonArr;
	}

	/**
	 * Parses the string into JSON data and stores it, and updates the body to
	 * use application/json in the future.
	 * 
	 * @param jsonArr
	 *            the JSON array
	 */
	public void addJsonData(String json) {
		bodyType = BodyType.JSON;
		jsonObjData = null;
		jsonArrData = null;
		try {
			jsonObjData = new JSONObject(json);
		} catch (JSONException e1) {
			try {
				jsonArrData = new JSONArray(json);
			} catch (JSONException e2) {
			}
		}
	}

	/**
	 * Removes the JSON data.
	 * 
	 * @return the JSON data, in the form of an Object (is an instance of a
	 *         JSONObject or a JSONArray)
	 */
	public Object clearJsonData() {
		Object data = null;
		try {
			data = (jsonObjData != null ? new JSONObject(jsonObjData) :
				new JSONArray(jsonArrData));
		} catch (JSONException e) {
			e.printStackTrace();
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
	 */
	public byte[] addBinaryData(byte[] bytes) {
		bodyType = BodyType.BINARY;
		try {
			binaryData.write(bytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return binaryData.toByteArray();
	}

	/**
	 * Appends the binary content of the file to the binary data, and updates
	 * the body to use application/octet-stream in the future.
	 * 
	 * @param bytes
	 * @return the complete binary data
	 */
	public byte[] addBinaryData(File file) {
		bodyType = BodyType.BINARY;
		try {
			binaryData.write(Files.readAllBytes(Paths.get(
					file.getAbsolutePath())));
		} catch (IOException e) {
			e.printStackTrace();
		}
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
	 * Updates the connection's request properties.
	 * 
	 * @param connection
	 */
	private void applyRequestProperties(HttpURLConnection connection) {
		for (String key : requestProperties.keySet()) {
			connection.setRequestProperty(key, requestProperties.get(key));
		}
	}

	/**
	 * Adds the body to the request depending on the request's BodyType
	 * 
	 * @param connection
	 */
	private void addBody(HttpURLConnection connection) {
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
	 */
	private void addFormData(HttpURLConnection connection) {
		try (OutputStream output = connection.getOutputStream();
				PrintWriter writer = new PrintWriter(
						new OutputStreamWriter(output), false)) {
			for (String key : formData.keySet()) {
				for (Map.Entry<FormDataType, Object> dataType :
					formData.get(key)) {
					if (dataType.getKey() == FormDataType.FIELD) {
						String value = (String) dataType.getValue();

						writer.append("--" + boundary).append(CRLF);
						writer.append("Content-Disposition: form-data; name=\""
								+ key + "\"").append(CRLF);
						writer.append("Content-Type: text/plain; charset="
								+ charset).append(CRLF);
						writer.append(CRLF).append(value).append(CRLF).flush();
					} else if (dataType.getKey() == FormDataType.RAW_FILE) {
						File file = (File) dataType.getValue();

						writer.append("--" + boundary).append(CRLF);
						writer.append("Content-Disposition: form-data; name=\""
								+ key + "\"; filename=\""
								+ file.getName() + "\"").append(CRLF);
						writer.append("Content-Type: text/plain; charset="
								+ charset).append(CRLF);
						writer.append(CRLF).flush();
						try {
							Files.copy(file.toPath(), output);
							output.flush();
						} catch (IOException e) {
							e.printStackTrace();
						}
						writer.append(CRLF).flush();
					} else if (dataType.getKey() == FormDataType.BINARY_FILE) {
						File file = (File) dataType.getValue();

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
							e.printStackTrace();
						}
						writer.append(CRLF).flush();
					}
				}
			}
			writer.append("--" + boundary + "--").append(CRLF).flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Writes the encoded URL form data to the connection's output stream
	 * 
	 * @param connection
	 */
	private void addEncodedFormData(HttpURLConnection connection) {
		try (OutputStream output = connection.getOutputStream()) {
			StringBuilder sb = new StringBuilder();
			for (String key : encodedFormData.keySet()) {
				for (String value : encodedFormData.get(key)) {
					String encodedKey = URLEncoder.encode(key, charset);
					String encodedValue = URLEncoder.encode(value, charset);
					sb.append(encodedKey + "=" + encodedValue + "&");
				}
			}
			String encodedData = sb.substring(0, sb.length() - 1);
			output.write(encodedData.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Writes the raw data to the connection's output stream
	 * 
	 * @param connection
	 */
	private void addRawData(HttpURLConnection connection) {
		try (OutputStream output = connection.getOutputStream()) {
			String encodedData = rawData.toString();
			output.write(encodedData.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Writes the JSON data to the connection's output stream
	 * 
	 * @param connection
	 */
	private void addJsonData(HttpURLConnection connection) {
		try (OutputStream output = connection.getOutputStream()) {
			String encodedData = (jsonObjData != null ?
					jsonObjData :jsonArrData).toString();
			output.write(encodedData.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Writes the binary data to the connection's output stream
	 * 
	 * @param connection
	 */
	private void addBinaryData(HttpURLConnection connection) {
		try (OutputStream output = connection.getOutputStream()) {
			binaryData.writeTo(output);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * A helper method that creates and returns a connection
	 * 
	 * @return the connection
	 * @throws IOException
	 */
	private HttpURLConnection connect() throws IOException {
		if (url == null) {
			throw new RuntimeException(
					"Must specify a URL - call setURL(urlString)");
		}
		return (HttpURLConnection) url.openConnection();
	}

	/**
	 * A helper method that disconnects the given connection
	 * 
	 * @param connection
	 */
	private static void disconnect(HttpURLConnection connection) {
		if (connection != null) {
			connection.disconnect();
		}
	}

}
