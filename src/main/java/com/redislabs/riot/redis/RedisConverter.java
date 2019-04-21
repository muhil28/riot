
package com.redislabs.riot.redis;

import java.util.Map;
import java.util.StringJoiner;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

import lombok.Setter;

public class RedisConverter {

	public static final String KEY_SEPARATOR = ":";

	private ConversionService converter = new DefaultConversionService();
	@Setter
	private String keyspace;
	@Setter
	private String[] keys;

	public String id(Map<String, Object> item) {
		return joinFields(item, keys);
	}

	public String getKeyspace() {
		return keyspace;
	}

	public String[] getKeys() {
		return keys;
	}

	public String key(Map<String, Object> item) {
		return key(id(item));
	}

	public String key(String id) {
		if (id == null) {
			return keyspace;
		}
		if (keyspace == null) {
			return id;
		}
		return keyspace + RedisConverter.KEY_SEPARATOR + id;
	}

	public String joinFields(Map<String, Object> item, String[] fields) {
		if (fields == null || fields.length == 0) {
			return null;
		}
		StringJoiner joiner = new StringJoiner(KEY_SEPARATOR);
		for (String field : fields) {
			joiner.add(converter.convert(item.get(field), String.class));
		}
		return joiner.toString();
	}

	public <T> T convert(Object source, Class<T> targetType) {
		return converter.convert(source, targetType);
	}

}
