package ch.jooel.config;

public interface ConfigurationLoader {

	<T> T load(String path, Class<T> clazz);

}
