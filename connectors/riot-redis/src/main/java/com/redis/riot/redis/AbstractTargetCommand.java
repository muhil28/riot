package com.redis.riot.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;

import com.redis.riot.AbstractTransferCommand;
import com.redis.riot.RedisOptions;
import com.redis.riot.RedisReaderOptions;
import com.redis.riot.RiotStep;
import com.redis.riot.RiotStep.Builder;
import com.redis.riot.TransferOptions;
import com.redis.spring.batch.DataStructure;
import com.redis.spring.batch.RedisItemReader;
import com.redis.spring.batch.compare.KeyComparisonItemWriter;
import com.redis.spring.batch.compare.KeyComparisonLogger;
import com.redis.spring.batch.compare.KeyComparisonResults;
import com.redis.spring.batch.reader.DataStructureValueReader;
import com.redis.spring.batch.support.RedisConnectionBuilder;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import picocli.CommandLine;

public abstract class AbstractTargetCommand extends AbstractTransferCommand {

	private static final Logger log = LoggerFactory.getLogger(AbstractTargetCommand.class);

	private static final String COMPARE_MESSAGE_ASCII = " >%,d T%,d ≠%,d ⧗%,d <%,d";
	private static final String COMPARE_MESSAGE_COLOR = " \u001b[31m>%,d \u001b[33mT%,d \u001b[35m≠%,d \u001b[36m⧗%,d\u001b[0m";
	private static final String VERIFICATION_NAME = "verification";

	@CommandLine.ArgGroup(exclusive = false, heading = "Target Redis connection options%n")
	protected RedisOptions targetRedisOptions = new RedisOptions();
	@CommandLine.ArgGroup(exclusive = false, heading = "Reader options%n")
	protected RedisReaderOptions readerOptions = new RedisReaderOptions();
	@CommandLine.Mixin
	private CompareOptions compareOptions = new CompareOptions();

	public RedisOptions getTargetRedisOptions() {
		return targetRedisOptions;
	}

	public RedisReaderOptions getReaderOptions() {
		return readerOptions;
	}

	public CompareOptions getCompareOptions() {
		return compareOptions;
	}

	@Override
	protected JobBuilder configureJob(JobBuilder job) {
		return super.configureJob(job).listener(new JobExecutionListenerSupport() {

			@Override
			public void afterJob(JobExecution jobExecution) {
				targetRedisOptions.shutdown();
			}
		});
	}

	protected Step verificationStep() throws Exception {
		RedisItemReader<String, DataStructure<String>> sourceReader = readerOptions
				.configureScanReader(reader(getRedisOptions(), StringCodec.UTF8).dataStructure())
				.jobRunner(getJobRunner()).build();
		log.debug("Creating key comparator with TTL tolerance of {} seconds", compareOptions.getTtlTolerance());
		DataStructureValueReader<String, String> targetValueReader = dataStructureValueReader(targetRedisOptions,
				StringCodec.UTF8);
		KeyComparisonItemWriter writer = KeyComparisonItemWriter.valueReader(targetValueReader)
				.tolerance(compareOptions.getTtlToleranceDuration()).build();
		if (compareOptions.isShowDiffs()) {
			writer.addListener(new KeyComparisonLogger(LoggerFactory.getLogger(getClass())));
		}
		Builder<DataStructure<String>, DataStructure<String>> riotStep = RiotStep.reader(sourceReader).writer(writer)
				.name(VERIFICATION_NAME).taskName("Verifying").max(this::initialMax)
				.message(() -> extraMessage(writer.getResults()));
		SimpleStepBuilder<DataStructure<String>, DataStructure<String>> step = step(riotStep.build());
		step.listener(new StepExecutionListenerSupport() {
			@Override
			public ExitStatus afterStep(StepExecution stepExecution) {
				if (stepExecution.getStatus().isUnsuccessful()) {
					return null;
				}
				if (writer.getResults().isOK()) {
					log.info("Verification completed - all OK");
					return ExitStatus.COMPLETED;
				}
				try {
					Thread.sleep(transferOptions.getProgressUpdateIntervalMillis());
				} catch (InterruptedException e) {
					log.debug("Verification interrupted");
					Thread.currentThread().interrupt();
					return ExitStatus.STOPPED;
				}
				KeyComparisonResults results = writer.getResults();
				log.warn("Verification failed: OK={} Missing={} Values={} TTLs={} Types={}", results.getOK(),
						results.getMissing(), results.getValue(), results.getTTL(), results.getType());
				return new ExitStatus(ExitStatus.FAILED.getExitCode(), "Verification failed");
			}
		});
		return step.build();
	}

	protected Long initialMax() {
		com.redis.spring.batch.RedisScanSizeEstimator.Builder estimator = estimator()
				.match(readerOptions.getScanMatch()).sampleSize(readerOptions.getSampleSize());
		readerOptions.getScanType().ifPresent(estimator::type);
		try {
			return estimator.build().call();
		} catch (Exception e) {
			log.warn("Could not estimate scan size", e);
			return null;
		}
	}

	private <K, V> DataStructureValueReader<K, V> dataStructureValueReader(RedisOptions redisOptions,
			RedisCodec<K, V> codec) {
		RedisConnectionBuilder<K, V, ?> builder = new RedisConnectionBuilder<>(redisOptions.client(), codec);
		return new DataStructureValueReader<>(builder.connectionSupplier(), readerOptions.poolConfig(),
				builder.async());
	}

	private String extraMessage(KeyComparisonResults results) {
		return String.format(extraMessageFormat(), results.getMissing(), results.getType(), results.getValue(),
				results.getTTL());
	}

	private String extraMessageFormat() {
		if (transferOptions.getProgress() == TransferOptions.Progress.COLOR) {
			return COMPARE_MESSAGE_COLOR;
		}
		return COMPARE_MESSAGE_ASCII;
	}

}
