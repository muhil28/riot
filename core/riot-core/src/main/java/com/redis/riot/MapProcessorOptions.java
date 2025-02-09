package com.redis.riot;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.ObjectUtils;

import com.redis.lettucemod.RedisModulesUtils.GeoLocation;
import com.redis.riot.convert.RegexNamedGroupsExtractor;
import com.redis.riot.processor.CompositeItemStreamItemProcessor;
import com.redis.riot.processor.FilteringProcessor;
import com.redis.riot.processor.MapAccessor;
import com.redis.riot.processor.MapProcessor;
import com.redis.riot.processor.SpelProcessor;

import picocli.CommandLine.Option;

public class MapProcessorOptions {

	private static final Logger log = LoggerFactory.getLogger(MapProcessorOptions.class);

	@Option(arity = "1..*", names = "--process", description = "SpEL expressions in the form field1=\"exp\" field2=\"exp\"...", paramLabel = "<f=exp>")
	private Map<String, Expression> spelFields;
	@Option(arity = "1..*", names = "--var", description = "Register a variable in the SpEL processor context.", paramLabel = "<v=exp>")
	private Map<String, Expression> variables;
	@Option(names = "--date", description = "Processor date format (default: ${DEFAULT-VALUE}).", paramLabel = "<string>")
	private String dateFormat = new SimpleDateFormat().toPattern();
	@Option(arity = "1..*", names = "--filter", description = "Discard records using SpEL boolean expressions.", paramLabel = "<exp>")
	private String[] filters;
	@Option(arity = "1..*", names = "--regex", description = "Extract named values from source field using regex.", paramLabel = "<f=exp>")
	private Map<String, String> regexes;

	public Map<String, Expression> getSpelFields() {
		return spelFields;
	}

	public void setSpelFields(Map<String, Expression> spelFields) {
		this.spelFields = spelFields;
	}

	public Map<String, Expression> getVariables() {
		return variables;
	}

	public void setVariables(Map<String, Expression> variables) {
		this.variables = variables;
	}

	public String getDateFormat() {
		return dateFormat;
	}

	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}

	public String[] getFilters() {
		return filters;
	}

	public void setFilters(String[] filters) {
		this.filters = filters;
	}

	public Map<String, String> getRegexes() {
		return regexes;
	}

	public void setRegexes(Map<String, String> regexes) {
		this.regexes = regexes;
	}

	public Optional<ItemProcessor<Map<String, Object>, Map<String, Object>>> processor(RedisOptions redisOptions) {
		List<ItemProcessor<Map<String, Object>, Map<String, Object>>> processors = new ArrayList<>();
		if (!ObjectUtils.isEmpty(spelFields)) {
			StandardEvaluationContext context = new StandardEvaluationContext();
			context.setVariable("date", dateFormat);
			processors.add(new SpelProcessor(redisOptions, context(), spelFields));
		}
		if (!ObjectUtils.isEmpty(regexes)) {
			Map<String, Converter<String, Map<String, String>>> fields = new LinkedHashMap<>();
			regexes.forEach((f, r) -> fields.put(f, RegexNamedGroupsExtractor.of(r)));
			processors.add(new MapProcessor(fields));
		}
		if (!ObjectUtils.isEmpty(filters)) {
			processors.add(new FilteringProcessor(filters));
		}
		return CompositeItemStreamItemProcessor.delegates(processors.toArray(ItemProcessor[]::new));
	}

	private EvaluationContext context() {
		StandardEvaluationContext context = new StandardEvaluationContext();
		context.setVariable("date", new SimpleDateFormat(dateFormat));
		if (variables != null) {
			for (Entry<String, Expression> variable : variables.entrySet()) {
				context.setVariable(variable.getKey(), variable.getValue().getValue(context));
			}
		}
		try {
			Method geoMethod = GeoLocation.class.getDeclaredMethod("toString", String.class, String.class);
			context.registerFunction("geo", geoMethod);
		} catch (Exception e) {
			log.warn("Could not register geo function", e);
		}
		context.setPropertyAccessors(Collections.singletonList(new MapAccessor()));
		return context;
	}

}
