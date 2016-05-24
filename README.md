# Request
A simple library used for sending HTTP and HTTPS requests, with many data management functions designed to make sending requests and parsing the response as easy as possible.



### Table of contents

* [The Request Object](#)
  * [Constructors](#)
  * [Headers/Request Properties](#)
  * [Query Parameters](#)
  * [Adding the Body](#)
    * [Form Data](#)
    * [URL-Encoded Form Data](#)
    * [Raw Data](#)
    * [JSON Data](#)
    * [Binary Data](#)
  * [Sending the Request](#)
* [The Response Object](#)
  * [The Head](#)
  * [The Body](#)
    * [Getting the Response](#)
    * [Saving as a File](#)

-
### The Request Object

This class provides methods to send HTTP and HTTPS requests.

#### Constructors

You can construct a Request object in a couple different ways. The first is to pass it the URL string:

```java
Request request = new Request("http://www.google.com");
```
Request supports HTTP and HTTPS requests. Using this method you can omit the protocol from the URL, and HTTP will be used by default. 

You can also pass it the `URL` object directly:

```java
URL url = new URL("http://www.google.com");
Request request = new Request(url);
```

If you want to create the Request object, but set the URL later, you can use the empty constructor, in conjunction with the `setURL` method, which acts similarly to the above constructors.

```java
Request request = new Request();
request.setURL("http://www.google.com");
```

The URL string can be retrieved later with the method `getURL`, as well as the protocol with `getProtocol`.

#### Headers/Request Properties
To set headers (a.k.a. request properties), simply use the `setRequestProperty` method:

```java
request.setRequestProperty("Authorization", "Basic bXl1c2VyOm15cGFzcw==");
```

To unset, or remove, a certain request property, you can use `removeRequestProperty`:

```java
request.removeRequestProperty("Authorization");
```

and to remove all previously set request properties, use `clearRequestProperties`:

```java
request.clearRequestProperties();
```

To retrieve the value of a specific previously set request property, you can use the method `getRequestProperty`:

```java
String value = request.getRequestProperty("Authorization");
```

Also, to get a map of all the request properties to their values, use `getRequestProperties`:

```java
Map<String, String> propertyMap = request.getRequestProperties();
```

#### Query Parameters

Request obviously allows URLs with query parameters included to be passed into the constructor. However, you can also add them programmatically using the `addQueryParameter` method:

```java
request.addQueryParameter("key", "value")
```

To remove a certain parameter, use the `removeQueryParamater` method:

```java
request.removeQueryParameter("key")
```

Additionally, to remove all query parameters, use the method `clearQueryParameters`:

```java
request.clearQueryParameters();
```

To retrieve the value of a certain paremeter, use `getQueryParameter`. Note this method returns a list of values, since query parameter keys do not have to be distinct:

```java
List<String> values = request.getQueryParameter("key");
```

Finally, if you want to retrieve all parameters in the query, use the method `getQueryParameters`:

```java
Map<String, List<String>> queryMap = request.getQueryParameters();
```

#### Adding the Body

While a body is not necessary for some requests, it is for many. Multiple body types are supported by Request. If several types are added to the same request object, the last type to be added is used, unless you explicitly declare which body type should be used.

*Note:* Many of the methods presented below will throw a `IOException` if an error occurs, and this is not made explicit.

* **Form Data (mutlipart/form-data)**
  
  Request supports adding fields, raw files, and binary files as form data using `addFormField`, `addFormRawFile`, and `addFormBinaryFile`. For raw or binary files, you can pass in a file path or a `File` object. Additionally, for fields or raw files you can include an optional third argument which is the charset to be used:
  
  ```java
  request.addFormField("key", "value"); // field
  request.addFormField("key", "value", Charset.forName("utf-8")); // field with specified charset
  
  request.addFormRawFile("someTextFile", "example.txt"); // raw file
  request.addFormRawFile("someTextFile", new File("example.txt")); // raw file using File object
  request.addFormRawFile("someTextFile", "example.txt", Charset.forName("utf-8")); // raw file with specified charset
  
  request.addFormBinaryFile("someBinaryFile", "image.jpg"); // binary file
  request.addFormBinaryFile("someBinaryFile", new File("image.jpg")); // binary file using File object
  ```
  
  To remove a certain entry, whether it be a field, raw file, or binary file, use the `removeFormEntry` method:
  
  ```java
  request.removeFormEntry("key");
  ```
  
  To remove all of them, use `clearFormEntries`:
  
  ```java
  request.clearFormEntries();
  ```
  
  To retrieve the value of a certain entry, use the method `getFormEntry`. Note this method also returns a list of values, since form entry keys do not have to be distinct:
  
  ```java
  List<FormData> values = request.getFormEntry("key");
  ```
  
  and to retrieve all entries, use `getFormEntries`:
  
  ```java
  Map<String, List<FormData>> formMap = request.getFormEntries();
  ```
  
  If the last type of body data you added was not form data, but you want the form data to be written to the body instead, you must tell Request that you want to use the form data with `useFormData`:
  
  ```java
  request.useFormData();
  ```
  
  Upon sending the request, if the header `Content-Type` is not already set and form data is being used, Request will set it to `mutlipart/form-data`.

* **URL-Encoded Form Data (application/x-www-form-urlencoded)**
  
  To add a field to the URL-encoded form data, use the method `addEncodedField`:
  
  ```java
  request.addEncodedField("key", "value");
  ```
  
  The methods `removeEncodedField`, `clearEncodedFields`, `getEncodedField`, `getEncodedFields`, and `useEncodedFormData` can be used to perform similar functions as above. Methods `getEncodedField` and `getEncodedFields` have a return type of `List<String>` and `Map<String, List<String>>`, respectively.
  
  Upon sending the request, if the header `Content-Type` is not already set and URL-encoded form data is being used, Request will set it to `application/x-www-form-urlencoded`.

* **Raw Data (text/plain)**
  
  To append raw data to the request body, use `addRawData`, which takes as a parameter a `String` or a `File` object. When using the File object as parameter, you can include an optional second argument which is the charset to be used:
  
  ```java
  request.addRawData("Hello world!"); // raw data from String
  request.addRawData(new File("helloWorld.txt")); // raw data from File object
  request.addRawData(new File("helloWorld.txt"), Charset.forName("utf-8"); // raw data from File object with specified charset
  ```
  
  The methods `clearRawData` and `getRawData` can be used to remove all raw data and retrieve the raw data, respectively. Also, use `useRawData` to set the body type to raw data.
  
  Upon sending the request, if the header `Content-Type` is not already set and raw data is being used, Request will set it to `text/plain`.

* **JSON Data (application/json)**
  
  To add JSON data to the request body, use `addJsonData`, which takes as a parameter a `JSONObject` or `JSONArray` from the **JSON-java** library:
  
  ```java
  JSONObject obj = new JSONObject();
  try {
	  obj.put("key", "value"); // May throw JSONException, so must handle
  } catch (JSONException e) { ... }
  request.addJsonData(obj); // JSON data from JSONObject
  ```
  
  ```java
  JSONArray arr = new JSONArray();
  arr.put("value");
  request.addJsonData(arr); // JSON data from JSONArray
  ```
  
  You can also pass in a `String` representing that JSON data to add JSON data instead. If the `String` cannot be parsed into either a `JSONObject` or `JSONArray`, a `JSONException` will be thrown:
  
  ```java
  try {
	  request.addJsonData("{'key':'value'}"); // JSON data from String. May throw JSONException, so must handle
  } catch (JSONException e) { ... }
  ```
  
  **!!! Important:** Adding JSON data will replace any other JSON data previously added, as a body with Content-Type `application/json` is sent as a string, which servers expect to represent a single JSON data structure.
  
  The methods `clearJsonData` and `getJsonData` can be used to remove the JSON data and retrieve the JSON data, respectively. Also, use `useJsonData` to set the body type to JSON data.
  
  Upon sending the request, if the header `Content-Type` is not already set and JSON data is being used, Request will set it to `application/json`.

* **Binary Data (application/octet-stream)**
  
  To append binary data to the request body, use `addBinaryData`, which takes as a parameter a `byte[]` or a `File` object:
  
  ```java
  byte[] bytes = "Hello World!".getBytes(); // Get the bytes from somewhere
  request.addBinaryData(bytes); // binary data from byte array
  
  request.addBinaryData(new File("helloWorld.txt")); // binary data from File object
  ```
  
  The methods `clearBinaryData` and `getBinaryData` can be used to remove the binary data and retrieve the binary data, respectively. Also, use `useBinaryData` to set the body type to binary data.
  
  Upon sending the request, if the header `Content-Type` is not already set and binary data is being used, Request will set it to `application/octet-stream`.
  
If you want to know which body type Request will use, use the method `getBodyType`, which returns a BodyType (an enum within the Request class) of one of the following:

```java
BodyType.FORM_DATA
BodyType.X_WWW_FORM_URLENCODED
BodyType.RAW
BodyType.JSON
BodyType.BINARY
```

#### Sending the Request

Request intentionally makes it very simply to setup and send requests using the Request object. Once the request is setup (request properties are set, body is added, etc.), sending it is just a matter of calling a single method. When one of these methods is called, Request will create a new URLConnection internally, and initialize it using the properties you have set. The body is then added to the request, the request is sent, and as soon as a response is received, a Response object is constructed and returned.

Request supports GET, POST, PUT, and DELETE requests.

* To send a GET request, use the method `GET`:

  ```java
  Response response = request.GET();
  ```
  Alternatively, if you only require a simple GET request without any configuring of the properties, you can use the static `GET` method, which takes as a parameter a `String` that is the URL or a `URL` object:
  
  ```java
  Response response = Request.get("http://www.google.com"); // response from String
  ```
  
  ```java
  URL url = new URL("http://www.google.com");
  Response response = Request.GET(url); // response from URL object
  ```
  
* To send a POST request, use the method `POST`:

  ```java
  Response response = request.POST();
  ```
  
* To send a PUT request, use the method `PUT`:

  ```java
  Response response = request.PUT();
  ```
  
* To send a DELETE request, use the method `DELETE`:

  ```java
  Response response = request.DELETE();
  ```

You can also programmatically set the method using `setMethod`, which takes as a parameter a `String` that is the method name (case insensitive) or a `RequestMethod` type (an enum within the Request class):

```java
request.setMethod("post"); // method from String
request.setMethod(RequestMethod.POST); // method from RequestMethod
```

Then, after setting the method, you can use `send` to send a request of that method type:

```java
Response response = request.send();
```

**Note:** the default method is GET.

-
### The Response Object

This immutable class provides methods to access the response of a request, and attempts to automatically parse it into useful forms such as JSON and HTML data types.

#### The Head

To get the values of a specific header, use the method `getHeaderField`:

```java
List<String> values = response.getHeaderField("Transfer-Encoding");
```

To get a map of all the headers to their values, use `getHeaderFields`:

```java
Map<String, List<String>> headerMap = response.getHeaderFields();
```

You can also use `getStatusLine` to get the status line (the first line of the response), `getStatusCode` to get the status of the response, `getDate` to get the time and date that the response was sent, and `getURL` to get the URL the response came from.

#### The Body

##### Getting the Response

To get the binary data of the body of the response, use the method `getBinaryData`:

```java
byte[] bytes = response.getBinaryData();
```

More likely, though, you would want to use `getText` to return a String version of the binary data:

```java
String text = response.getText();
```

Request will automatically attempt to parse the response body into JSON, using a library called **JSON-java**. You can check to see if the response was JSON data, using `isJsonObject` and `isJsonArray`, and if it is, you can retrieve that data using `getJsonObject` and `getJsonArray`, which return a `JSONObject` and `JSONArray`, respectively, or null if it is not actually JSON data:

```java
JSONObject obj = response.getJsonObject(); // if it's a JSON object
JSONArray arr = response.getJsonArray(); // if it's a JSON array
```

Request will also attempt to parse the response body into HTML, using a library called **jsoup**. You can check if it is HTML with the method `isHtml`, and if it is, you can retrieve that data using `getHtml`, which returns a Jsoup `HTMLDocument`, or null if it is not actually HTML data:

```java
HTMLDocument html = response.getHtml(); // if it's HTML
```

Access to the original data in binary and text form, as well as parsed JSON and HTML data types is an integral part of this library, and reduces the work required by the user significantly.

##### Saving as a File

If you want to save the response to a file, use the method `saveAsFile`, which takes as a parameter a `String` that is the file path or a `File` object:

```java
response.saveAsFile("response.txt"); // saved using file path
response.saveAsFile(new File("response.txt")); // saved using a File object
```

This method saves the binary data directly, instead of the text, so it can be used to save requests containing binary files such as images, etc.:

```java
response.saveAsFile("image.jpg");
```

-
The documentation for **JSON-java** and **jsoup** can be found below:

* [JSON-java documentation](https://github.com/stleary/JSON-java)
* [jsoup documentation](https://jsoup.org/)

-
## License

Copyright &copy; 2016 [Eric Wadkins](http://www.ericwadkins.com/)

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
