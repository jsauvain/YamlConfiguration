package ch.jooel.config;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.error.Mark;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;

public class ConfigurationParsingException extends ConfigurationException {
	private static final long serialVersionUID = 1L;

	static class Builder {
		private static final int MAX_SUGGESTIONS = 5;

		private String summary;
		private String detail = "";
		private List<JsonMappingException.Reference> fieldPath = Collections.emptyList();
		private int line = -1;
		private int column = -1;
		private Exception cause = null;
		private List<String> suggestions = new ArrayList<>();
		private String suggestionBase = null;
		private boolean suggestionsSorted = false;

		Builder(String summary) {
			this.summary = summary;
		}

		public String getSummary() {
			return summary.trim();
		}

		public String getDetail() {
			return detail.trim();
		}

		public boolean hasDetail() {
			return detail != null && !detail.isEmpty();
		}

		public List<JsonMappingException.Reference> getFieldPath() {
			return fieldPath;
		}

		public boolean hasFieldPath() {
			return fieldPath != null && !fieldPath.isEmpty();
		}

		public int getLine() {
			return line;
		}

		public int getColumn() {
			return column;
		}

		public boolean hasLocation() {
			return line > -1 && column > -1;
		}

		public List<String> getSuggestions() {

			if (suggestionsSorted || !hasSuggestionBase()) {
				return suggestions;
			}

			suggestions.sort(new LevenshteinComparator(getSuggestionBase()));
			suggestionsSorted = true;

			return suggestions;
		}

		public boolean hasSuggestions() {
			return suggestions != null && !suggestions.isEmpty();
		}

		public String getSuggestionBase() {
			return suggestionBase;
		}

		public boolean hasSuggestionBase() {
			return suggestionBase != null && !suggestionBase.isEmpty();
		}

		public Exception getCause() {
			return cause;
		}

		public boolean hasCause() {
			return cause != null;
		}

		Builder setCause(Exception cause) {
			this.cause = cause;
			return this;
		}

		Builder setDetail(String detail) {
			this.detail = detail;
			return this;
		}

		Builder setFieldPath(List<JsonMappingException.Reference> fieldPath) {
			this.fieldPath = fieldPath;
			return this;
		}

		Builder setLocation(JsonLocation location) {
			return location == null
					? this
					: setLocation(location.getLineNr(), location.getColumnNr());
		}

		Builder setLocation(Mark mark) {
			return mark == null
					? this
					: setLocation(mark.getLine(), mark.getColumn());
		}

		Builder setLocation(int line, int column) {
			this.line = line;
			this.column = column;
			return this;
		}

		Builder addSuggestion(String suggestion) {
			this.suggestionsSorted = false;
			this.suggestions.add(suggestion);
			return this;
		}

		Builder addSuggestions(Collection<String> suggestions) {
			this.suggestionsSorted = false;
			this.suggestions.addAll(suggestions);
			return this;
		}

		Builder setSuggestionBase(String base) {
			this.suggestionBase = base;
			this.suggestionsSorted = false;
			return this;
		}

		ConfigurationParsingException build(String path) {
			final StringBuilder sb = new StringBuilder(getSummary());
			if (hasFieldPath()) {
				sb.append(" at: ").append(buildPath(getFieldPath()));
			} else if (hasLocation()) {
				sb.append(" at line: ").append(getLine() + 1)
						.append(", column: ").append(getColumn() + 1);
			}

			if (hasDetail()) {
				sb.append("; ").append(getDetail());
			}

			if (hasSuggestions()) {
				final List<String> suggestions = getSuggestions();
				sb.append(NEWLINE).append("    Did you mean?:").append(NEWLINE);
				final Iterator<String> it = suggestions.iterator();
				int i = 0;
				while (it.hasNext() && i < MAX_SUGGESTIONS) {
					sb.append("      - ").append(it.next());
					i++;
					if (it.hasNext()) {
						sb.append(NEWLINE);
					}
				}

				final int total = suggestions.size();
				if (i < total) {
					sb.append("        [").append(total - i).append(" more]");
				}
			}

			return hasCause()
					? new ConfigurationParsingException(path, sb.toString(), getCause())
					: new ConfigurationParsingException(path, sb.toString());
		}

		private String buildPath(Iterable<JsonMappingException.Reference> path) {
			final StringBuilder sb = new StringBuilder();
			if (path != null) {
				final Iterator<JsonMappingException.Reference> it = path.iterator();
				while (it.hasNext()) {
					final JsonMappingException.Reference reference = it.next();
					final String name = reference.getFieldName();

					// append either the field name or list index
					if (name == null) {
						sb.append('[').append(reference.getIndex()).append(']');
					} else {
						sb.append(name);
					}

					if (it.hasNext()) {
						sb.append('.');
					}
				}
			}
			return sb.toString();
		}

		protected static class LevenshteinComparator implements Comparator<String>, Serializable {
			private static final long serialVersionUID = 1L;

			private String base;

			public LevenshteinComparator(String base) {
				this.base = base;
			}

			@Override
			public int compare(String a, String b) {

				if (a.equals(b)) {
					return 0; // comparing the same value; don't bother
				} else if (a.equals(base)) {
					return -1; // a is equal to the base, so it's always first
				} else if (b.equals(base)) {
					return 1; // b is equal to the base, so it's always first
				}

				return Integer.compare(StringUtils.getLevenshteinDistance(a, base),
						StringUtils.getLevenshteinDistance(b, base));
			}

			private void writeObject(ObjectOutputStream stream) throws IOException {
				stream.defaultWriteObject();
			}

			private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
				stream.defaultReadObject();
			}
		}
	}

	static Builder builder(String brief) {
		return new Builder(brief);
	}

	private ConfigurationParsingException(String path, String msg) {
		super(path, ImmutableSet.of(msg));
	}

	private ConfigurationParsingException(String path, String msg, Throwable cause) {
		super(path, ImmutableSet.of(msg), cause);
	}

}
