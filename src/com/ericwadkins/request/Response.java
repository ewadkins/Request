package com.ericwadkins.request;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * An immutable response object. This class takes in an HttpURLConnection, reads
 * from the input stream, and parses the body of the response into text, as well
 * as JSON and HTML when applicable. It also stores other relevant information
 * from the header.
 * 
 * @author ericwadkins
 */
public class Response {

	// Data storage
	private final String text;
	private final JSONObject jsonObj;
	private final JSONArray jsonArr;
	private final boolean isHtml;
	private final Map<String, List<String>> headerFields;
	private final int statusCode;
	private final long date;

	private final String urlString;

	/**
	 * Constructs a Response object with the specified data.
	 * 
	 * @param text
	 *            the text of the body
	 * @param jsonObj
	 *            a JSONObject when applicable, should be null otherwise
	 * @param jsonArr
	 *            a JSONArray when applicable, should be null otherwise
	 * @param isHtml
	 *            should be true if and only if text can be parsed into HTML
	 * @param headerFields
	 *            a map of headers, which maps the header names to values
	 * @param statusCode
	 *            the status/response code
	 * @param date
	 *            the date the response was sent, should be 0 if not known
	 * @param urlString
	 *            the url
	 */
	public Response(String text, JSONObject jsonObj, JSONArray jsonArr,
			boolean isHtml, Map<String, List<String>> headerFields,
			int statusCode, long date, String urlString) {
		this.text = text;
		this.jsonObj = jsonObj;
		this.jsonArr = jsonArr;
		this.isHtml = isHtml;
		this.headerFields = new HashMap<>(headerFields);
		this.statusCode = statusCode;
		this.date = date;
		
		this.urlString = urlString;
	}

	/**
	 * Reads from the connection's InputStream and produces a response.
	 * 
	 * @param connection the connection
	 * @return the response
	 * @throws IOException if an error occurs
	 */
	public static Response parse(URLConnection connection) throws IOException {
		try (InputStream in = connection.getInputStream()) {
			String urlString = connection.getURL().toString();
			Map<String, List<String>> headerFields =
					connection.getHeaderFields();
			int statusCode = 0;
			String protocol = connection.getURL().getProtocol();
			switch (protocol) {
			case "http": statusCode = 
					((HttpURLConnection) connection).getResponseCode(); break;
			case "https": statusCode = 
					((HttpsURLConnection) connection).getResponseCode(); break;
			default: throw new UnsupportedOperationException(
					"Request does not support " + protocol + " requests");
			}
			long date = connection.getDate();
			Object[] parsed = parseBody(in);
			String body = (String) parsed[0];
			JSONObject jsonObj = (JSONObject) parsed[1];
			JSONArray jsonArr = (JSONArray) parsed[2];
			boolean isHtml = parseHtml(body) != null;
			return new Response(body, jsonObj, jsonArr, isHtml, headerFields,
					statusCode, date, urlString);
		} catch (IOException e) {
			throw e;
		}
	}
	
	/**
	 * Parses the body and returns an Object array containing three elements:
	 * 	the String pf the body,
	 * 	the JSONObject of the body (if parsed successfully),
	 *  the JSONArray of the body (if parsed successfully),
	 *  and the HTMLDocument of the nody (if parsed successfully).
	 * 
	 * @param in
	 * @return the content in different forms, as described above
	 * @throws IOException
	 */
	private static Object[] parseBody(InputStream in) throws IOException {
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
		return new Object[]{ body, jsonObj, jsonArr };
	}
	
	private static HTMLDocument parseHtml(String body) {
		InputStream stream = new ByteArrayInputStream(body.getBytes());
		HTMLEditorKit kit = new HTMLEditorKit();
		HTMLDocument doc = (HTMLDocument) kit.createDefaultDocument();
		doc.putProperty("IgnoreCharsetDirective", true);
		try {
			kit.read(stream, doc, 0);
		} catch (BadLocationException | IOException e) {
		}
		if (doc.getLength() > 0) {
			return doc;
		}
		return null;
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
	 * Whether the response could be parsed into a HTMLDocument or not.
	 * 
	 * @return true if this response could parse the body into a HTMLDocument,
	 *         false otherwise
	 */
	public boolean isHtml() {
		return isHtml;
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
	 * Returns the HTMLDocument if parsing the text into html was successful.
	 * Since HTMLDocument is a mutable class, each time this method is called
	 * the text is re-parsed and a new HTMLDocument instance is returned.
	 * 
	 * @return the HTMLDocument if it could be parsed, null otherwise
	 */
	public HTMLDocument getHtml() {
		if (!isHtml) {
			return null;
		}
		return parseHtml(text);
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

	/**
	 * Returns the url string of the request that generated this response.
	 * 
	 * @return the url string
	 */
	public String getURL() {
		return urlString;
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
