package ch.jooel.config;

public interface ConfigurationLoader {

	<T> T load(Class<T> clazz, String path);

}
