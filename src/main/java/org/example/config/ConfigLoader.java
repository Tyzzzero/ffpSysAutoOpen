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
 * 配置加载器
 * @author Tyzzzero
 */
public class ConfigLoader {
    private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);
    private static final String CONFIG_FILE = "config.properties";

    public static <T> T loadConfig(Class<T> configClass) {
        log.info("开始加载配置文件: {}", CONFIG_FILE);
        Properties properties = new Properties();
        try (InputStream inputStream = configClass.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (inputStream == null) {
                throw new IllegalStateException("找不到配置文件: " + CONFIG_FILE);
            }
            
            try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
            
            T config = configClass.getDeclaredConstructor().newInstance();
            for (Field field : configClass.getDeclaredFields()) {
                String propertyName = field.getName();
                String propertyValue = properties.getProperty(propertyName);
                
                if (propertyValue == null) {
                    log.warn("配置项 {} 未设置，使用默认值", propertyName);
                    continue;
                }
                
                try {
                    field.setAccessible(true);
                    setFieldValue(config, field, propertyValue);
                    log.debug("配置项 {} 加载成功: {}", propertyName, propertyValue);
                } catch (Exception e) {
                    log.error("配置项 {} 加载失败: {}", propertyName, e.getMessage(), e);
                    throw e;
                }
            }
            
            // 验证必填配置项
            if (config instanceof Config) {
                ((Config) config).validate();
            }
            
            log.info("配置文件加载完成");
            return config;
        } catch (Exception e) {
            log.error("配置文件加载失败: {}", e.getMessage(), e);
            throw new IllegalStateException("配置文件加载失败", e);
        }
    }

    private static <T> void setFieldValue(T config, Field field, String propertyValue) throws Exception {
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
            @SuppressWarnings("unchecked")
            Class<? extends Enum> enumType = (Class<? extends Enum>) fieldType;
            Enum<?> enumValue = Enum.valueOf(enumType, propertyValue);
            field.set(config, enumValue);
        } else {
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType) {
                log.warn("暂不支持泛型类型配置: {}", field.getName());
            } else {
                field.set(config, fieldType.cast(propertyValue));
            }
        }
    }
}