package com.elderbyte.kafka.consumer.factory;

import com.elderbyte.kafka.consumer.configuration.AutoOffsetReset;
import com.elderbyte.kafka.consumer.processing.Processor;
import com.elderbyte.kafka.metrics.MetricsContext;
import com.elderbyte.kafka.serialisation.SpringKafkaJsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.config.ContainerProperties;

import java.util.List;


/**
 * Holds the complete configuration of a kafka listener
 */
public class KafkaListenerBuilderImpl<K,V> implements KafkaListenerBuilder<K,V>, KafkaListenerConfiguration<K,V> {

    /***************************************************************************
     *                                                                         *
     * Fields                                                                  *
     *                                                                         *
     **************************************************************************/

    private final ManagedListenerBuilder managedListenerBuilder;

    private final ObjectMapper mapper;

    private final ContainerProperties containerProperties;
    private final Deserializer<K> keyDeserializer;
    private final Deserializer<V> valueDeserializer;


    private AutoOffsetReset autoOffsetReset = AutoOffsetReset.latest;
    private MetricsContext metricsContext = MetricsContext.from("", "");
    private boolean skipOnError = false;
    private int blockingRetries = 1;

    private Processor<List<ConsumerRecord<K, V>>> processor;
    private boolean batch = false;

    /***************************************************************************
     *                                                                         *
     * Constructor                                                             *
     *                                                                         *
     **************************************************************************/

    KafkaListenerBuilderImpl(
            ManagedListenerBuilder managedListenerBuilder,
            ContainerProperties containerProperties,
            ObjectMapper mapper,
            Deserializer<K> keyDeserializer,
            Deserializer<V> valueDeserializer
    )
    {
        this.managedListenerBuilder = managedListenerBuilder;
        this.containerProperties = containerProperties;
        this.mapper = mapper;
        this.keyDeserializer = keyDeserializer;
        this.valueDeserializer = valueDeserializer;

        autoCommit(false);
    }

    /***************************************************************************
     *                                                                         *
     * Builder API                                                             *
     *                                                                         *
     **************************************************************************/

    @Override
    public <NV> KafkaListenerBuilder<K,NV> jsonValue(Class<NV> valueClazz){
        return valueDeserializer(new SpringKafkaJsonDeserializer<>(valueClazz, mapper));
    }

    @Override
    public <NK> KafkaListenerBuilder<NK,V> jsonKey(Class<NK> keyClazz){
        return keyDeserializer(new SpringKafkaJsonDeserializer<>(keyClazz, mapper));
    }

    @Override
    public KafkaListenerBuilder<K,String> stringValue(){
        return valueDeserializer(new StringDeserializer());
    }

    @Override
    public KafkaListenerBuilder<String,V> stringKey(){
        return keyDeserializer(new StringDeserializer());
    }

    @Override
    public <NV> KafkaListenerBuilder<K,NV> valueDeserializer(Deserializer<NV> valueDeserializer){
        return new KafkaListenerBuilderImpl<>(
                this.managedListenerBuilder,
                this.containerProperties,
                this.mapper,
                this.keyDeserializer,
                valueDeserializer
        ).apply(this);
    }

    @Override
    public <NK> KafkaListenerBuilder<NK,V> keyDeserializer(Deserializer<NK> keyDeserializer){
        return new KafkaListenerBuilderImpl<>(
                this.managedListenerBuilder,
                this.containerProperties,
                this.mapper,
                keyDeserializer,
                this.valueDeserializer
        ).apply(this);
    }

    @Override
    public KafkaListenerBuilder<K,V>  autoOffsetReset(AutoOffsetReset autoOffsetReset) {
        this.autoOffsetReset = autoOffsetReset;
        return this;
    }

    @Override
    public KafkaListenerBuilder<K,V> consumerGroup(String groupId){
        this.containerProperties.setGroupId(groupId);
        return this;
    }

    @Override
    public KafkaListenerBuilder<K,V> consumerId(String clientId){
        this.containerProperties.setClientId(clientId);
        return this;
    }

    @Override
    public KafkaListenerBuilder<K,V> metrics(MetricsContext metricsContext){
        this.metricsContext = metricsContext;
        return this;
    }

    @Override
    public KafkaListenerBuilder<K,V> ignoreErrors() {
        this.skipOnError = true;
        return this;
    }

    @Override
    public KafkaListenerBuilder<K, V> blockingRetries(int retries) {
        this.blockingRetries = retries;
        return this;
    }

    @Override
    public KafkaListenerBuilder<K, V> autoCommit(boolean autoCommit) {
        this.containerProperties.setAckMode(autoCommit ? AbstractMessageListenerContainer.AckMode.BATCH : AbstractMessageListenerContainer.AckMode.MANUAL);
        return this;
    }


    public KafkaListenerBuilder<K,V> apply(KafkaListenerConfiguration<?,?> prototype){
        this.autoOffsetReset = prototype.getAutoOffsetReset();
        this.metricsContext = prototype.getMetricsContext();
        this.blockingRetries = prototype.getBlockingRetries();
        this.skipOnError = prototype.isIgnoreErrors();
        return this;
    }

    /***************************************************************************
     *                                                                         *
     * Builder end API                                                         *
     *                                                                         *
     **************************************************************************/


    public void startProcess(Processor<ConsumerRecord<K, V>> processor){
        startProcessing(batch -> {
            for(var e : batch){
                processor.proccess(e);
            }
        });
    }

    public void startProcessBatch(Processor<List<ConsumerRecord<K, V>>> processor){
        this.batch = true;
        startProcessing(processor);
    }

    private void startProcessing(Processor<List<ConsumerRecord<K, V>>> processor){
        this.processor = processor;
        managedListenerBuilder.buildListenerContainer(this)
                .start();
    }

    /***************************************************************************
     *                                                                         *
     * Configuration                                                           *
     *                                                                         *
     **************************************************************************/

    @Override
    public ContainerProperties getContainerProperties() {
        return containerProperties;
    }

    @Override
    public boolean isManualAck(){
        return getContainerProperties().getAckMode() == AbstractMessageListenerContainer.AckMode.MANUAL
                || getContainerProperties().getAckMode() == AbstractMessageListenerContainer.AckMode.MANUAL_IMMEDIATE;
    }

    @Override
    public Deserializer<K> getKeyDeserializer() {
        return keyDeserializer;
    }

    @Override
    public Deserializer<V> getValueDeserializer() {
        return valueDeserializer;
    }

    @Override
    public Processor<List<ConsumerRecord<K, V>>> getProcessor() {
        return processor;
    }

    @Override
    public boolean isBatch() {
        return batch;
    }

    @Override
    public AutoOffsetReset getAutoOffsetReset() {
        return autoOffsetReset;
    }

    @Override
    public MetricsContext getMetricsContext() {
        return metricsContext;
    }

    @Override
    public boolean isIgnoreErrors() {
        return skipOnError;
    }

    @Override
    public int getBlockingRetries() {
        return blockingRetries;
    }
}