package com.redis.riot.redis;

import java.util.Map;

import com.redis.spring.batch.writer.RedisOperation;
import com.redis.spring.batch.writer.operation.Sadd;

import picocli.CommandLine.Command;

@Command(name = "sadd", description = "Add members to a set")
public class SaddCommand extends AbstractCollectionCommand {

	@Override
	public RedisOperation<String, String, Map<String, Object>> operation() {
		return Sadd.<String, String, Map<String, Object>>key(key()).member(member()).build();
	}

}
