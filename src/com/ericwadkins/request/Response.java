package com.ericwadkins.request;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

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
	private final byte[] data;
	private final String text;
	private final JSONObject jsonObj;
	private final JSONArray jsonArr;
	private final Document html;
	private final Map<String, List<String>> headerFields;
	private final int statusCode;
	private final long date;

	private final String urlString;

	/**
	 * Constructs a Response object with the specified data.
	 * 
	 * @param data
	 *            the binary data of the body
	 * @param text
	 *            the text of the body
	 * @param jsonObj
	 *            a JSONObject when applicable, should be null otherwise
	 * @param jsonArr
	 *            a JSONArray when applicable, should be null otherwise
	 * @param html
	 *            a Jsoup Document when applicable, should be null otherwise
	 * @param headerFields
	 *            a map of headers, which maps the header names to values
	 * @param statusCode
	 *            the status/response code
	 * @param date
	 *            the date the response was sent, should be 0 if not known
	 * @param urlString
	 *            the url
	 */
	protected Response(byte[] data, String text, JSONObject jsonObj,
			JSONArray jsonArr, Document html,
			Map<String, List<String>> headerFields, int statusCode, long date,
			String urlString) {
		this.data = data;
		this.text = text;
		this.jsonObj = jsonObj;
		this.jsonArr = jsonArr;
		this.html = html;
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
	protected static Response parse(URLConnection connection) throws IOException {
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
			Object[] parsed = parseBody(in, urlString);
			byte[] data = (byte[]) parsed[0];
			String text = (String) parsed[1];
			JSONObject jsonObj = (JSONObject) parsed[2];
			JSONArray jsonArr = (JSONArray) parsed[3];
			Document html = (Document) parsed[4];
			return new Response(data, text, jsonObj, jsonArr, html,
					headerFields, statusCode, date, urlString);
		} catch (IOException e) {
			throw e;
		}
	}
	
	/**
	 * Parses the body and returns an Object array containing three elements:
	 * 	the byte[] binary data pf the body,
	 * 	the String text pf the body,
	 * 	the JSONObject of the body (if parsed successfully),
	 *  the JSONArray of the body (if parsed successfully),
	 *  and the Jsoup Document of the body (if parsed successfully).
	 * 
	 * @param in
	 * @return the content in different forms, as described above
	 * @throws IOException
	 */
	private static Object[] parseBody(InputStream in, String baseUrl) throws IOException {
		/*BufferedReader br = new BufferedReader(new InputStreamReader(in));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = br.readLine()) != null) {
			sb.append(line + "\n");
		}
		String body = sb.toString();*/
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int read;
		while ((read = in.read()) != -1) {
			buffer.write(read);
		}
		byte[] data = buffer.toByteArray();
		String text = new String(data);
		JSONObject jsonObj = null;
		JSONArray jsonArr = null;
		try {
			jsonObj = new JSONObject(text);
		} catch (JSONException e1) {
			try {
				jsonArr = new JSONArray(text);
			} catch (JSONException e2) {
			}
		}
		Document html = Jsoup.parse(text, baseUrl);
		return new Object[]{ data, text, jsonObj, jsonArr, html };
	}
	
	/**
	 * Writes the binary data read from this response's body to the
	 * specified file object.
	 * 
	 * @param file
	 * @throws IOException if an error occurs
	 */
	public void saveAsFile(File file) throws IOException {
		FileOutputStream output = new FileOutputStream(file);
		output.write(data);
		output.close();
	}

	/**
	 * Writes the binary data read from this response's body to the
	 * specified file path.
	 * 
	 * @param filepath
	 * @throws IOException if an error occurs
	 */
	public void saveAsFile(String filepath) throws IOException {
		File file = new File(filepath);
		saveAsFile(file);
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
	 * Whether the response could be parsed into a Jsoup Document or not.
	 * 
	 * @return true if this response could parse the body into a Document,
	 *         false otherwise
	 */
	public boolean isHtml() {
		return html != null;
	}

	/**
	 * Returns the body as received, as binary data.
	 * 
	 * @return the body's binary data
	 */
	public byte[] getBinaryData() {
		byte[] copy = new byte[data.length];
		System.arraycopy(data, 0, copy, 0, data.length);
		return copy;
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
	 * Returns a clone of the Document if parsing the text into HTML was
	 * successful.
	 * 
	 * @return the Jsoup Document if it could be parsed, null otherwise
	 */
	public Document getHtml() {
		if (html == null) {
			return null;
		}
		return html.clone();
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
