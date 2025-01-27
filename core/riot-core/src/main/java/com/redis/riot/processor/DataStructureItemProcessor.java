package com.redis.riot.processor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.core.convert.converter.Converter;

import com.redis.riot.convert.CollectionToStringMapConverter;
import com.redis.riot.convert.RegexNamedGroupsExtractor;
import com.redis.riot.convert.StreamToStringMapConverter;
import com.redis.riot.convert.StringToStringMapConverter;
import com.redis.riot.convert.ZsetToStringMapConverter;
import com.redis.spring.batch.DataStructure;
import com.redis.spring.batch.DataStructure.Type;

import io.lettuce.core.ScoredValue;
import io.lettuce.core.StreamMessage;

public class DataStructureItemProcessor implements ItemProcessor<DataStructure<String>, Map<String, Object>> {

	private final Converter<String, Map<String, String>> keyFieldsExtractor;
	private Converter<Map<String, String>, Map<String, String>> hashConverter = s -> s;
	private StreamToStringMapConverter streamConverter = new StreamToStringMapConverter();
	private CollectionToStringMapConverter listConverter = new CollectionToStringMapConverter();
	private CollectionToStringMapConverter setConverter = new CollectionToStringMapConverter();
	private ZsetToStringMapConverter zsetConverter = new ZsetToStringMapConverter();
	private Converter<String, Map<String, String>> stringConverter = new StringToStringMapConverter();
	private Converter<Object, Map<String, String>> defaultConverter = s -> null;

	public DataStructureItemProcessor(Converter<String, Map<String, String>> keyFieldsExtractor) {
		this.keyFieldsExtractor = keyFieldsExtractor;
	}

	public void setHashConverter(Converter<Map<String, String>, Map<String, String>> hashConverter) {
		this.hashConverter = hashConverter;
	}

	public void setStreamConverter(StreamToStringMapConverter streamConverter) {
		this.streamConverter = streamConverter;
	}

	public void setListConverter(CollectionToStringMapConverter listConverter) {
		this.listConverter = listConverter;
	}

	public void setSetConverter(CollectionToStringMapConverter setConverter) {
		this.setConverter = setConverter;
	}

	public void setZsetConverter(ZsetToStringMapConverter zsetConverter) {
		this.zsetConverter = zsetConverter;
	}

	public void setStringConverter(Converter<String, Map<String, String>> stringConverter) {
		this.stringConverter = stringConverter;
	}

	public void setDefaultConverter(Converter<Object, Map<String, String>> defaultConverter) {
		this.defaultConverter = defaultConverter;
	}

	@Override
	public Map<String, Object> process(DataStructure<String> item) {
		if (item.getType() == null) {
			return null;
		}
		if (item.getKey() == null) {
			return null;
		}
		Map<String, String> stringMap = keyFieldsExtractor.convert(item.getKey());
		if (stringMap == null) {
			return null;
		}
		Map<String, Object> map = new HashMap<>(stringMap);
		Map<String, String> valueMap = map(item);
		if (valueMap != null) {
			map.putAll(valueMap);
		}
		return map;
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> map(DataStructure<String> item) {
		switch (Type.of(item.getType())) {
		case HASH:
			return hashConverter.convert((Map<String, String>) item.getValue());
		case LIST:
			return listConverter.convert((List<String>) item.getValue());
		case SET:
			return setConverter.convert((Set<String>) item.getValue());
		case ZSET:
			return zsetConverter.convert((List<ScoredValue<String>>) item.getValue());
		case STREAM:
			return streamConverter.convert((List<StreamMessage<String, String>>) item.getValue());
		case STRING:
			return stringConverter.convert((String) item.getValue());
		default:
			return defaultConverter.convert(item.getValue());
		}
	}

	public static DataStructureItemProcessor of(String keyRegex) {
		RegexNamedGroupsExtractor keyFieldsExtractor = RegexNamedGroupsExtractor.of(keyRegex);
		return new DataStructureItemProcessor(keyFieldsExtractor);
	}

}
