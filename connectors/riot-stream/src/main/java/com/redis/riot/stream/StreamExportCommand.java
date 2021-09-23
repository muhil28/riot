package com.redis.riot.stream;

import com.redis.riot.AbstractTransferCommand;
import com.redis.riot.FlushingTransferOptions;
import com.redis.riot.RiotStepBuilder;
import com.redis.riot.stream.kafka.KafkaItemWriter;
import com.redis.riot.stream.processor.AvroProducerProcessor;
import com.redis.riot.stream.processor.JsonProducerProcessor;
import io.lettuce.core.StreamMessage;
import io.lettuce.core.XReadArgs.StreamOffset;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.redis.StreamItemReader;
import org.springframework.core.convert.converter.Converter;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
@Command(name = "export", description = "Import Redis streams into Kafka topics")
public class StreamExportCommand extends AbstractTransferCommand {

    @CommandLine.Mixin
    private FlushingTransferOptions flushingTransferOptions = new FlushingTransferOptions();
    @SuppressWarnings("unused")
    @Parameters(arity = "0..*", description = "One ore more streams to read from", paramLabel = "STREAM")
    private String[] streams;
    @CommandLine.Mixin
    private KafkaOptions options = new KafkaOptions();
    @Option(names = "--offset", description = "XREAD offset (default: ${DEFAULT-VALUE})", paramLabel = "<string>")
    private String offset = "0-0";
    @SuppressWarnings("unused")
    @Option(names = "--topic", description = "Target topic key (default: same as stream)", paramLabel = "<string>")
    private String topic;

    @Override
    protected Flow flow(StepBuilderFactory stepBuilderFactory) {
        Assert.isTrue(!ObjectUtils.isEmpty(streams), "No stream specified");
        List<Step> steps = new ArrayList<>();
        for (String stream : streams) {
            StreamItemReader reader = reader(StreamOffset.from(stream, offset));
            StepBuilder stepBuilder = stepBuilderFactory.get(stream + "-stream-export-step");
            RiotStepBuilder<StreamMessage<String, String>, ProducerRecord<String, Object>> step = riotStep(stepBuilder, "Exporting from " + stream);
            steps.add(step.reader(reader).processor(processor()).writer(writer()).flushingOptions(flushingTransferOptions).build().build());
        }
        return flow(steps.toArray(new Step[0]));
    }

    private StreamItemReader reader(StreamOffset<String> offset) {
        return new StreamItemReader.StreamItemReaderBuilder(getRedisOptions().client(), offset).build();
    }

    private KafkaItemWriter<String> writer() {
        Map<String, Object> producerProperties = options.producerProperties();
        log.debug("Creating Kafka writer with producer properties {}", producerProperties);
        return KafkaItemWriter.<String>builder().kafkaTemplate(new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerProperties))).build();
    }

    private ItemProcessor<StreamMessage<String, String>, ProducerRecord<String, Object>> processor() {
        if (options.getSerde() == KafkaOptions.SerDe.JSON) {
            return new JsonProducerProcessor(topicConverter());
        }
        return new AvroProducerProcessor(topicConverter());
    }

    private Converter<StreamMessage<String, String>, String> topicConverter() {
        if (topic == null) {
            return StreamMessage::getStream;
        }
        return s -> topic;
    }

}
