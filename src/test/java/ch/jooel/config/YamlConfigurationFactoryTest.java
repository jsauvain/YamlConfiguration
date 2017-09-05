package ch.jooel.config;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class YamlConfigurationFactoryTest {

	private YamlConfigurationFactory factory = new YamlConfigurationFactory();

	@Test
	public void loadNormalObject() throws Exception {
		TestConfigClass configClass = factory.load(TestConfigClass.class, "src/test/resources/config.yml");
		assertEquals("Hello world", configClass.getString());
		assertEquals(true, configClass.isBool());
		assertEquals(5636, configClass.getNumber());
		assertEquals(43.78, configClass.getDbl(), 0.1);
		System.out.println(configClass);
	}

	@Test
	public void loadBigObject() throws Exception {
		BigTestConfigClass configClass = factory.load(BigTestConfigClass.class, "src/test/resources/big-config.yml");
		System.out.println(configClass);
		assertEquals("Hello world", configClass.getString());
		assertEquals(true, configClass.isBool());
		assertEquals(5636, configClass.getNumber());
		assertEquals(43.78, configClass.getDbl(), 0.1);
		assertEquals(20, configClass.getSmallObject().getAge());
		assertEquals("ben", configClass.getSmallObject().getUsername());
	}

	@Test
	public void testExtendedObject() throws Exception {
		ExtendedObject object = factory.load(ExtendedObject.class, "src/test/resources/extended.yml");
		System.out.println(object);
		assertEquals("ben", object.getUsername());
		assertEquals(20, object.getAge());
		assertEquals("abc", object.getPassword());
	}

	@Test
	public void testInvalidConfig() throws Exception {
		try {
			factory.load(TestConfigClass.class, "src/test/resources/invalid.yml");
			assertFalse(true);
		} catch (ConfigurationParsingException ex) {
			assertTrue("Got exception", true);
			System.out.println(ex.getMessage());
		}
	}

	@Test
	public void testSaveConfig() throws Exception {
		BigTestConfigClass object = new BigTestConfigClass("Ein String", true, 272, 239.31, new SmallObject("Kevin", 12));
		File file = new File("target/testing");
		file.mkdirs();
		factory.save(object, "target/testing/config.yml");
		assertEquals(factory.load(BigTestConfigClass.class, "target/testing/config.yml"), object);
	}

	@Test
	public void testSaveDefaults() throws Exception {
		factory.saveDefaults(DefaultConfigClass.class, "target/testing/default.yml");
		assertEquals(factory.load(DefaultConfigClass.class, "target/testing/default.yml"), new DefaultConfigClass());
	}

	@Test
	public void testUpdateConfig() throws Exception {
		OldConfig config = new OldConfig(4, "Hello World");
		new File("target/testing").mkdirs();
		factory.save(config, "target/testing/update.yml");
		NewConfig newConfig = factory.updateConfig(NewConfig.class, "target/testing/update.yml");
		assertEquals(newConfig, new NewConfig(4, "Hello World", false));
	}
}