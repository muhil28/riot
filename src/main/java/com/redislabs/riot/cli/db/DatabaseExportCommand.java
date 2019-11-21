package com.redislabs.riot.cli.db;

import java.util.Map;

import org.springframework.batch.item.ItemWriter;

import com.redislabs.picocliredis.RedisOptions;
import com.redislabs.riot.cli.ExportCommand;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;

@Command(name = "db-export", description = "Export to database")
public class DatabaseExportCommand extends ExportCommand {

	@ArgGroup(exclusive = false, heading = "Database writer options%n", order = 3)
	private DatabaseWriterOptions options = new DatabaseWriterOptions();

	@Override
	protected ItemWriter<Map<String, Object>> writer(RedisOptions redisConnectionOptions) {
		return options.writer();
	}

}
