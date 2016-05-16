package com.ericwadkins.request;

import java.io.File;
import java.nio.charset.Charset;

/**
 * An immutable form data object. This class holds form data to be used in the
 * body of a request containing multipart/form-data.
 * 
 * @author ericwadkins
 */
public class FormData {

	/**
	 * The type of data contained in each part of the multipart form-data.
	 * 
	 * @author ericwadkins
	 */
	public static enum FormDataType {
		FIELD, RAW_FILE, BINARY_FILE
	}
	
	// Data storage
	private String string;
	private File file;
	private FormDataType dataType;
	private Charset charset = Charset.forName("utf-8");
	
	/**
	 * Constructs a form data object of the field type with the specified
	 * charset.
	 * 
	 * @param string the value of the field
	 * @param charset the charset
	 */
	public FormData(String string, Charset charset) {
		this.string = string;
		dataType = FormDataType.FIELD;
		this.charset = charset;
	}
	
	/**
	 * Constructs a form data object of the specified file type and charset.
	 * 
	 * @param file the file
	 * @param dataType the type of file
	 * @param charset the charset
	 */
	public FormData(File file, FormDataType dataType, Charset charset) {
		this.file = file;
		this.dataType = dataType;
		this.charset = charset;
	}
	
	/**
	 * Checks if this form data is a field.
	 * 
	 * @return true if this is a field, false otherwise
	 */
	public boolean isField() {
		return string != null;
	}

	
	/**
	 * Checks if this form data is a file.
	 * 
	 * @return true if this is a file, false otherwise
	 */
	public boolean isFile() {
		return file != null;
	}

	/**
	 * Returns the string value of this field.
	 * 
	 * @return the value
	 */
	public String getField() {
		return string;
	}
	
	/**
	 * Returns the file value of this field.
	 * 
	 * @return the file
	 */
	public File getFile() {
		return file;
	}
	
	/**
	 * Returns the data type of this field.
	 * 
	 * @return the data type
	 */
	public FormDataType getType() {
		return dataType;
	}
	
	/**
	 * Returns the charset of this field.
	 * 
	 * @return the charset
	 */
	public Charset getCharset() {
		return charset;
	}
	
}
