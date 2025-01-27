package com.redis.riot.file.test;

import java.util.Iterator;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

import com.redis.riot.file.FileImportCommand;
import com.redis.riot.file.FileImportOptions;

class RiotFileTests {

	@Test
	void testJSONImport() throws UnexpectedInputException, ParseException, NonTransientResourceException, Exception {
		FileImportCommand command = FileImportCommand.builder().build();
		Iterator<Map<String, Object>> iterator = command.read("https://storage.googleapis.com/jrx/beers.json");
		Assertions.assertTrue(iterator.hasNext());
		Map<String, Object> beer1 = iterator.next();
		Assertions.assertEquals(13, beer1.size());
		int count = 1;
		while (iterator.hasNext()) {
			iterator.next();
			count++;
		}
		Assertions.assertEquals(4432, count);
	}

	@Test
	void testCSVImport() throws UnexpectedInputException, ParseException, NonTransientResourceException, Exception {
		FileImportCommand command = FileImportCommand.builder()
				.options(FileImportOptions.builder().header(true).build()).build();
		Iterator<Map<String, Object>> iterator = command.read("https://storage.googleapis.com/jrx/beers.csv");
		Assertions.assertTrue(iterator.hasNext());
		Map<String, Object> beer1 = iterator.next();
		Assertions.assertEquals(7, beer1.size());
		int count = 1;
		while (iterator.hasNext()) {
			iterator.next();
			count++;
		}
		Assertions.assertEquals(2410, count);
	}

}
