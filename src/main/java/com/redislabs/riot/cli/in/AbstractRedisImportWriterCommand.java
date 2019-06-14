package com.redislabs.riot.cli.in;

import java.util.Map;

import org.springframework.batch.item.ItemWriter;

import com.redislabs.riot.redis.writer.AbstractRedisDataStructureItemWriter;
import com.redislabs.riot.redis.writer.JedisWriter;
import com.redislabs.riot.redis.writer.LettuceAsyncWriter;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;

public abstract class AbstractRedisImportWriterCommand extends AbstractImportWriterCommand {

	@Override
	protected ItemWriter<Map<String, Object>> writer() {
		AbstractRedisDataStructureItemWriter itemWriter = redisItemWriter();
		itemWriter.setConverter(redisConverter());
		switch (getRoot().getDriver()) {
		case Lettuce:
			return new LettuceAsyncWriter<StatefulRedisConnection<String, String>, RedisAsyncCommands<String, String>>(
					getRoot().lettucePool(), itemWriter);
		default:
			return new JedisWriter(getRoot().jedisPool(), itemWriter);
		}
	}

	protected abstract AbstractRedisDataStructureItemWriter redisItemWriter();

	public String getTargetDescription() {
		return getDataStructure() + " \"" + getKeyspaceDescription() + "\"";
	}

	protected abstract String getDataStructure();

	protected String getKeyspaceDescription() {
		String description = getKeyspace() == null ? "" : getKeyspace();
		for (String key : getKeys()) {
			description += getSeparator() + "<" + key + ">";
		}
		return description;
	}

}
