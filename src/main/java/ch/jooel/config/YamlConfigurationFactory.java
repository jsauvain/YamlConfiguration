package ch.jooel.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;

public class YamlConfigurationFactory implements ConfigurationLoader {

	private ObjectMapper mapper;

	public YamlConfigurationFactory() {
		mapper = new ObjectMapper();
		mapper.registerModule(new GuavaModule());
		mapper.registerModule(new AfterburnerModule());
		mapper.registerModule(new ParameterNamesModule());
		mapper.registerModule(new Jdk8Module());
		mapper.registerModule(new JavaTimeModule());
		mapper.setPropertyNamingStrategy(new PropertyNamingStrategy());
		mapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
	}

	public void save(Object config, String file) {
		File fl = new File(file);
		try {
			fl.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try (OutputStream outputStream = new FileOutputStream(file)) {
			JsonNode node = mapper.valueToTree(config);
			JsonGenerator generator = new YAMLFactory().createGenerator(outputStream);
			mapper.writeTree(generator, node);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public <T> T load(String path, Class<T> clazz) {
		try (InputStream inputStream = new FileInputStream(path)) {
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
