package org.demo.propstoyaml;

import java.util.ArrayList;
import java.util.List;

public class ConversionStatus {

	static ConversionStatus EMPTY = new ConversionStatus();

	public static final int OK = 0;
	public static final int WARNING = 1;
	public static final int ERROR = 2;

	private int severity = 0;
	private List<ConversionMessage> entries = new ArrayList<>();

	static class ConversionMessage {

		private int severity;
		private String message;

		public ConversionMessage(int severity, String message) {
			this.severity = severity;
			this.message = message;
		}

		public int getSeverity() {
			return severity;
		}

		public String getMessage() {
			return message;
		}
	}

	void addError(String message) {
		entries.add(new ConversionMessage(ERROR, message));
		if (severity < ERROR) {
			severity = ERROR;
		}
	}

	void addWarning(String message) {
		entries.add(new ConversionMessage(WARNING, message));
		if (severity < WARNING) {
			severity = WARNING;
		}
	}

	public List<ConversionMessage> getEntries() {
		return entries;
	}

	public int getSeverity() {
		return severity;
	}

}
