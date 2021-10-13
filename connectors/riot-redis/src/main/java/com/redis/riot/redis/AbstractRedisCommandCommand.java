package com.redis.riot.redis;

import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.repeat.RepeatStatus;

import com.redis.lettucemod.api.StatefulRedisModulesConnection;
import com.redis.lettucemod.api.sync.RedisModulesCommands;
import com.redis.riot.AbstractRiotCommand;

import picocli.CommandLine.Command;

@Command
public abstract class AbstractRedisCommandCommand extends AbstractRiotCommand {

    @Override
    protected Flow flow(StepBuilderFactory stepBuilderFactory) {

        return flow(stepBuilderFactory.get(commandName() + "-step").tasklet((contribution, chunkContext) -> {
            try (StatefulRedisModulesConnection<String, String> connection = getRedisOptions().connect()) {
                RedisModulesCommands<String, String> commands = connection.sync();
                execute(commands);
                return RepeatStatus.FINISHED;
            }
        }).build());
    }

    protected abstract void execute(RedisModulesCommands<String, String> commands) throws InterruptedException;


}
