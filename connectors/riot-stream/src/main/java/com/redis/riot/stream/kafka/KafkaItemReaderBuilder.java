/*
 * Copyright 2019 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.redis.riot.stream.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * A builder implementation for the {@link KafkaItemReader}.
 *
 * @author Mathieu Ouellet
 * @author Mahmoud Ben Hassine
 * @since 4.2
 * @see KafkaItemReader
 */
public class KafkaItemReaderBuilder<K, V> {

	private static final String PROPERTY_MUST_BE_PROVIDED = " property must be provided";

	private Properties consumerProperties;

	private String topic;

	private List<Integer> partitions = new ArrayList<>();

	private boolean saveState = true;

	private String name;

	/**
	 * Configure if the state of the
	 * {@link org.springframework.batch.item.ItemStreamSupport} should be persisted
	 * within the {@link org.springframework.batch.item.ExecutionContext} for
	 * restart purposes.
	 * 
	 * @param saveState defaults to true
	 * @return The current instance of the builder.
	 */
	public KafkaItemReaderBuilder<K, V> saveState(boolean saveState) {
		this.saveState = saveState;
		return this;
	}

	/**
	 * The name used to calculate the key within the
	 * {@link org.springframework.batch.item.ExecutionContext}. Required if
	 * {@link #saveState(boolean)} is set to true.
	 * 
	 * @param name name of the reader instance
	 * @return The current instance of the builder.
	 * @see org.springframework.batch.item.ItemStreamSupport#setName(String)
	 */
	public KafkaItemReaderBuilder<K, V> name(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Configure the underlying consumer properties.
	 * <p>
	 * <strong>{@code consumerProperties} must contain the following keys:
	 * 'bootstrap.servers', 'group.id', 'key.deserializer' and 'value.deserializer'
	 * </strong>
	 * </p>
	 * .
	 * 
	 * @param consumerProperties properties of the consumer
	 * @return The current instance of the builder.
	 */
	public KafkaItemReaderBuilder<K, V> consumerProperties(Properties consumerProperties) {
		this.consumerProperties = consumerProperties;
		return this;
	}

	/**
	 * A list of partitions to manually assign to the consumer.
	 * 
	 * @param partitions list of partitions to assign to the consumer
	 * @return The current instance of the builder.
	 */
	public KafkaItemReaderBuilder<K, V> partitions(Integer... partitions) {
		return partitions(Arrays.asList(partitions));
	}

	/**
	 * A list of partitions to manually assign to the consumer.
	 * 
	 * @param partitions list of partitions to assign to the consumer
	 * @return The current instance of the builder.
	 */
	public KafkaItemReaderBuilder<K, V> partitions(List<Integer> partitions) {
		this.partitions = partitions;
		return this;
	}

	/**
	 * A topic name to manually assign to the consumer.
	 * 
	 * @param topic name to assign to the consumer
	 * @return The current instance of the builder.
	 */
	public KafkaItemReaderBuilder<K, V> topic(String topic) {
		this.topic = topic;
		return this;
	}

	public KafkaItemReader<K, V> build() {
		if (this.saveState) {
			Assert.hasText(this.name, "A name is required when saveState is set to true");
		}
		Assert.notNull(consumerProperties, "Consumer properties must not be null");
		Assert.isTrue(consumerProperties.containsKey(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG),
				ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG + PROPERTY_MUST_BE_PROVIDED);
		Assert.isTrue(consumerProperties.containsKey(ConsumerConfig.GROUP_ID_CONFIG),
				ConsumerConfig.GROUP_ID_CONFIG + PROPERTY_MUST_BE_PROVIDED);
		Assert.isTrue(consumerProperties.containsKey(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG),
				ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG + PROPERTY_MUST_BE_PROVIDED);
		Assert.isTrue(consumerProperties.containsKey(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG),
				ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG + PROPERTY_MUST_BE_PROVIDED);
		Assert.hasLength(topic, "Topic name must not be null or empty");
		Assert.isTrue(!partitions.isEmpty(), "At least one partition must be provided");

		KafkaItemReader<K, V> reader = new KafkaItemReader<>(this.consumerProperties, this.topic, this.partitions);
		reader.setSaveState(this.saveState);
		reader.setName(this.name);
		return reader;
	}
}
