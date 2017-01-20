package org.blazer.userservice.core.model;

public class ChangeNode {

	private String separator;

	private String initString;

	private String[] array;

	private Integer index = 0;

	public ChangeNode(String initString) {
		this.initString = initString;
		this.separator = ",";
		init();
	}

	public ChangeNode(String initString, String separator) {
		this.initString = initString;
		this.separator = separator;
		init();
	}

	private void init() {
		array = initString.split(separator);
	}

	public String next() {
		if ((index + 1) == array.length) {
			index = 0;
		} else {
			++index;
		}
		return get();
	}

	public String next(int index) {
		this.index = index;
		return get(index);
	}

	public String get() {
		return array[index];
	}

	public String get(int index) {
		return array[index];
	}

	public int size() {
		return array.length;
	}

}
