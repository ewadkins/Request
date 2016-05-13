package com.ericwadkins.request;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * An immutable response object. This class takes in an HttpURLConnection, reads
 * from the input stream, and parses the body of the response into text, as well
 * as JSON data when applicable. It also stores other relevant information from
 * the header.
 * 
 * @author ericwadkins
 */
public class Response {

	// Data storage
	private final String text;
	private final JSONObject jsonObj;
	private final JSONArray jsonArr;
	private final String urlString;
	private final Map<String, List<String>> headerFields;
	private final int statusCode;
	private final long date;

	/**
	 * Constructs a Response object with the specified data.
	 * 
	 * @param text
	 *            the text of the body
	 * @param jsonObj
	 *            a JSONObject when applicable, should be null otherwise
	 * @param jsonArr
	 *            a JSONArray when applicable, should be null otherwise
	 * @param headerFields
	 *            a map of headers, which maps the header names to values
	 * @param statusCode
	 *            the status/response code
	 * @param date
	 *            the date the response was sent, should be 0 if not known
	 */
	public Response(String text, JSONObject jsonObj, JSONArray jsonArr,
			String urlString, Map<String, List<String>> headerFields,
			int statusCode, long date) {
		this.text = text;
		this.jsonObj = jsonObj;
		this.jsonArr = jsonArr;
		this.urlString = urlString;
		this.headerFields = new HashMap<>(headerFields);
		this.statusCode = statusCode;
		this.date = date;
	}

	/**
	 * Reads from the connection's InputStream and produces a response.
	 * 
	 * @param connection
	 *            the connection
	 * @return the response
	 * @throws IOException if an error occurs
	 */
	public static Response parse(HttpURLConnection connection)
			throws IOException {
		try (InputStream in = connection.getInputStream()) {
			String urlString = connection.getURL().toString();
			Map<String, List<String>> headerFields =
					connection.getHeaderFields();
			int statusCode = connection.getResponseCode();
			long date = connection.getDate();
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line + "\n");
			}
			String body = sb.toString();
			JSONObject jsonObj = null;
			JSONArray jsonArr = null;
			try {
				jsonObj = new JSONObject(body);
			} catch (JSONException e1) {
				try {
					jsonArr = new JSONArray(body);
				} catch (JSONException e2) {
				}
			}
			return new Response(body, jsonObj, jsonArr, urlString,
					headerFields, statusCode, date);
		} catch (IOException e) {
			throw e;
		}
	}

	/**
	 * Whether the response could be parsed into a JSONObject or not.
	 * 
	 * @return true if this response could parse the body into a JSONObject,
	 *         false otherwise
	 */
	public boolean isJsonObject() {
		return jsonObj != null;
	}

	/**
	 * Whether the response could be parsed into a JSONArray or not.
	 * 
	 * @return true if this response could parse the body into a JSONArray,
	 *         false otherwise
	 */
	public boolean isJsonArray() {
		return jsonArr != null;
	}

	/**
	 * Returns the body as received, in its text format.
	 * 
	 * @return the body's text
	 */
	public String getText() {
		return text;
	}

	/**
	 * Returns the JSONObject if parsing the text into JSON data was successful.
	 * 
	 * @return the JSONObject if it could be parsed, null otherwise
	 */
	public JSONObject getJsonObject() {
		return jsonObj;
	}

	/**
	 * Returns the JSONArray if parsing the text into JSON data was successful.
	 * 
	 * @return the JSONArray if it could be parsed, null otherwise
	 */
	public JSONArray getJsonArray() {
		return jsonArr;
	}

	/**
	 * Returns the url string of the response.
	 * 
	 * @return the url string
	 */
	public String getURL() {
		return urlString;
	}

	/**
	 * Returns the header fields in the response's header.
	 * 
	 * @return a map of the header fields, mapping keys to values
	 */
	public Map<String, List<String>> getHeaderFields() {
		return new HashMap<>(headerFields);
	}

	/**
	 * Returns the header values associated with the specified field.
	 * 
	 * @return the header values, or null if there are none
	 */
	public List<String> getHeaderField(String field) {
		return headerFields.get(field);
	}

	/**
	 * Returns the status line of the response.
	 * 
	 * @return the status
	 */
	public String getStatusLine() {
		return headerFields.get(null).get(0);
	}

	/**
	 * Returns the status code of the response.
	 * 
	 * @return the status code
	 */
	public int getStatusCode() {
		return statusCode;
	}

	/**
	 * Returns the date the response was sent.
	 * 
	 * @return the date the response was sent, 0 if it is unknown
	 */
	public long getDate() {
		return date;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (String field : headerFields.keySet()) {
			List<String> values = headerFields.get(field);
			sb.append((field != null ? field + ": " : "")
					+ (values.size() == 1 ? values.get(0) : values) + "\n");
		}
		sb.append(text);
		return sb.toString();
	}

}
