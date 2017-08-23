package ch.jooel.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.RequiredArgsConstructor;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class YamlConfigurationFactory implements ConfigurationLoader {

	public <T> T load(String path, Class<T> clazz) {
		try (InputStream inputStream = new FileInputStream(path)) {
			ObjectMapper mapper = new ObjectMapper();
			mapper.registerModule(new GuavaModule());
			mapper.registerModule(new AfterburnerModule());
			mapper.registerModule(new ParameterNamesModule());
			mapper.registerModule(new Jdk8Module());
			mapper.registerModule(new JavaTimeModule());
			mapper.setPropertyNamingStrategy(new PropertyNamingStrategy());
			mapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
			JsonNode node = mapper.readTree(createParser(inputStream));
			if (node == null)
				throw new RuntimeException("Configuration at " + path + " must not be empty");
			return mapper.readValue(new TreeTraversingParser(node), clazz);
		} catch (UnrecognizedPropertyException e) {
			final List<String> properties = e.getKnownPropertyIds().stream()
					.map(Object::toString)
					.collect(Collectors.toList());
			throw ConfigurationParsingException.builder("Unrecognized field")
					.setFieldPath(e.getPath())
					.setLocation(e.getLocation())
					.addSuggestions(properties)
					.setSuggestionBase(e.getPropertyName())
					.setCause(e)
					.build(path);
		} catch (InvalidFormatException e) {
			final String sourceType = e.getValue().getClass().getSimpleName();
			final String targetType = e.getTargetType().getSimpleName();
			throw ConfigurationParsingException.builder("Incorrect type of value")
					.setDetail("is of type: " + sourceType + ", expected: " + targetType)
					.setLocation(e.getLocation())
					.setFieldPath(e.getPath())
					.setCause(e)
					.build(path);
		} catch (JsonMappingException e) {
			throw ConfigurationParsingException.builder("Failed to parse configuration")
					.setDetail(e.getMessage())
					.setFieldPath(e.getPath())
					.setLocation(e.getLocation())
					.setCause(e)
					.build(path);
		} catch (IOException ex) {
			throw new RuntimeException("Cannot load config");
		}
	}

	private JsonParser createParser(InputStream inputStream) throws IOException {
		return new YAMLFactory().createParser(inputStream);
	}
}
