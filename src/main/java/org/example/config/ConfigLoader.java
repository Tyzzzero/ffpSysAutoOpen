package org.example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * @author Tyzzzero
 */
public class ConfigLoader {
    private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);

    public static <T> T loadConfig(Class<T> configClass) {
        Properties properties = new Properties();
        try (InputStream inputStream = configClass.getClassLoader().getResourceAsStream("config.properties");
             Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            properties.load(reader);
            T config = configClass.getDeclaredConstructor().newInstance();
            for (Field field : configClass.getDeclaredFields()) {
                String propertyName = field.getName();
                String propertyValue = properties.getProperty(propertyName);
                if (propertyValue != null) {
                    field.setAccessible(true);
                    Class<?> fieldType = field.getType();
                    if (fieldType == int.class || fieldType == Integer.class) {
                        field.set(config, Integer.parseInt(propertyValue));
                    } else if (fieldType == double.class || fieldType == Double.class) {
                        field.set(config, Double.parseDouble(propertyValue));
                    } else if (fieldType == boolean.class || fieldType == Boolean.class) {
                        field.set(config, Boolean.parseBoolean(propertyValue));
                    } else if (fieldType == long.class || fieldType == Long.class) {
                        field.set(config, Long.parseLong(propertyValue));
                    } else if (fieldType == String.class) {
                        field.set(config, propertyValue);
                    } else if (fieldType.isEnum()) {
                        // 处理枚举类型
                        Class<? extends Enum> enumType = (Class<? extends Enum>) fieldType;
                        Enum<?> enumValue = Enum.valueOf(enumType, propertyValue);
                        field.set(config, enumValue);
                    } else {
                        // 处理其他类型，使用 Class.cast 方法
                        Type genericType = field.getGenericType();
                        if (genericType instanceof ParameterizedType) {
                            // 处理泛型类型，这里简单处理，仅作示例，可根据实际情况完善
                            // 例如对于 List<String> 等泛型类型，可能需要进一步解析泛型参数等操作来正确设置值
                            continue;
                        } else {
                            field.set(config, fieldType.cast(propertyValue));
                        }
                    }
                }
            }
            return config;
        } catch (Exception e) {
            log.error("ConfigLoader error: {}", e.getStackTrace());
            return null;
        }
    }
}