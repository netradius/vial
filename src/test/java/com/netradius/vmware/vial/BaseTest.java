package com.netradius.vmware.vial;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Properties;

/**
 * @author Erik R. Jensen
 */
@Slf4j
public abstract class BaseTest {

	@Before
	public void init() throws IOException, IllegalAccessException {
		Properties props = new Properties();
		props.load(getClass().getResourceAsStream("/test-config.properties"));
		Class<?> clz = getClass();
		for (Field field : clz.getDeclaredFields()) {
			if (field.getAnnotation(Config.class) != null) {
				log.trace("Field " + field.getName() + " is annotated with @Config");
				field.setAccessible(true);
				String value = props.getProperty(field.getName());
				field.set(this, value);
			}
		}
	}
}
