package ch.jooel.config;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This API will store objects into yaml and also read objects from yaml.
 * If you are updating your config, just call {@link #updateConfig(Class, String)}.
 * This will write the new values into the config
 * If you call {@link #saveDefaults(Class, String)} it will store the default values in this class.
 * As example {@code private int number = 5} will store "5"
 * <p>
 * IMPORTANT: The configuration class MUST have getters and setters
 */
@Slf4j
public class YamlConfigurationFactory implements ConfigurationLoader {

	private ObjectMapper mapper;
	private JsonFactory factory = new YAMLFactory();


	public YamlConfigurationFactory() {
		mapper = new ObjectMapper();
		mapper.registerModule(new GuavaModule());
		mapper.registerModule(new AfterburnerModule());
		mapper.registerModule(new ParameterNamesModule());
		mapper.registerModule(new Jdk8Module());
		mapper.registerModule(new JavaTimeModule());
		mapper.setPropertyNamingStrategy(new PropertyNamingStrategy());
		mapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
	}

	/**
	 * Use this method to store the default values of a class.
	 * As example {@code private int number = 5} will store "number: 5"
	 * see {@link #save(Object, String)}
	 *
	 * @param clazz the class of the object
	 * @param file  the file it should store to
	 */
	public void saveDefaults(Class clazz, String file) {
		try {
			save(clazz.newInstance(), file);
		} catch (InstantiationException | IllegalAccessException e) {
			log.error("Cannot create a new instance, there must be an empty constructor", e);
		}
	}

	/**
	 * Use this method to update a given config.
	 * It will load the config into the given class and store it back to the file.
	 * This affects that the new values in the object will be stored
	 *
	 * @param clazz the class of the object
	 * @param file  the file it should store to
	 * @return an instance of the given class with the values from the yaml file
	 */
	public <T> T updateConfig(Class<T> clazz, String file) {
		T object = load(clazz, file);
		save(object, file);
		return object;
	}

	/**
	 * Use this method to save a object into a file
	 *
	 * @param config the object to be stored
	 * @param file   the file it should be stored to
	 */
	public <T> void save(T config, String file) {
		try {
			new File(file).createNewFile();
		} catch (IOException e) {
			log.error("cannot create new file", e);
		}
		try (OutputStream outputStream = new FileOutputStream(file)) {
			JsonNode node = mapper.valueToTree(config);
			JsonGenerator generator = factory.createGenerator(outputStream);
			mapper.writeTree(generator, node);
		} catch (JsonProcessingException e) {
			throw ConfigurationParsingException.builder("Json processing exception")
					.setDetail(e.getMessage())
					.setLocation(e.getLocation())
					.setCause(e)
					.build(file);
		} catch (Exception e) {
			log.error("General Exception", e);
		}
	}

	/**
	 * Use this method to load a yaml file into an object
	 *
	 * @param clazz the class of the object which should be generated
	 * @param path  the path of the yaml file
	 * @return an instance of the given class with the values from the yaml file
	 */
	@Override
	public <T> T load(Class<T> clazz, String path) {
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

	/**
	 * This will copy a resource file
	 * @param resourcePath resource path
	 * @param path final path
	 */
	public void copyFromResource(String resourcePath, String path) {
		try (InputStream is = this.getClass().getResourceAsStream(resourcePath);
			 OutputStream os = new FileOutputStream(path)) {
			IOUtils.copy(is, os);
		} catch (IOException ex) {
			log.error("Cannot copy resource file", ex);
		}
	}

	/**
	 * This will copy a resource file
	 *
	 * @param resourcePath resource path
	 * @param output       final output file
	 */
	public void copyFromResource(String resourcePath, File output) {
		try (InputStream is = this.getClass().getResourceAsStream(resourcePath);
			 OutputStream os = new FileOutputStream(output)) {
			IOUtils.copy(is, os);
		} catch (IOException ex) {
			log.error("Cannot copy resource file", ex);
		}
	}


	private JsonParser createParser(InputStream inputStream) throws IOException {
		return factory.createParser(inputStream);
	}
}
