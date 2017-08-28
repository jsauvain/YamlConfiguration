package ch.jooel.config;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class YamlConfigurationFactoryTest {

	private YamlConfigurationFactory factory = new YamlConfigurationFactory();

	@Test
	public void loadNormalObject() throws Exception {
		TestConfigClass configClass = factory.load("src/test/resources/config.yml", TestConfigClass.class);
		assertEquals("Hello world", configClass.getString());
		assertEquals(true, configClass.isBool());
		assertEquals(5636, configClass.getNumber());
		assertEquals(43.78, configClass.getDbl(), 0.1);
		System.out.println(configClass);
	}

	@Test
	public void loadBigObject() throws Exception {
		BigTestConfigClass configClass = factory.load("src/test/resources/big-config.yml", BigTestConfigClass.class);
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
		ExtendedObject object = factory.load("src/test/resources/extended.yml", ExtendedObject.class);
		System.out.println(object);
		assertEquals("ben", object.getUsername());
		assertEquals(20, object.getAge());
		assertEquals("abc", object.getPassword());
	}

	@Test
	public void testInvalidConfig() throws Exception {
		try {
			factory.load("src/test/resources/invalid.yml", TestConfigClass.class);
		} catch (ConfigurationParsingException ex) {
			assertTrue("Got exception", true);
			System.out.println(ex.getMessage());
		}
	}

	@Test
	public void testSaveConfig() throws Exception {
		BigTestConfigClass object = new BigTestConfigClass("Ein String", true, 272, 239.31, new SmallObject("Kevin", 12));
		factory.save(object, "abc/config.yml");
	}

}