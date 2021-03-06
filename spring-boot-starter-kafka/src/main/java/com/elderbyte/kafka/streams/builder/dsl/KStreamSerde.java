package com.elderbyte.kafka.streams.builder.dsl;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;

public class KStreamSerde<K,V> {

    /***************************************************************************
     *                                                                         *
     * Fields                                                                  *
     *                                                                         *
     **************************************************************************/

    private final Serde<K> keySerde;
    private final Serde<V> valueSerde;

    /***************************************************************************
     *                                                                         *
     * Constructor                                                             *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a new ElSerde
     */
    public KStreamSerde(
            Serde<K> keySerde,
            Serde<V> valueSerde
    ) {
        this.keySerde = keySerde;
        this.valueSerde = valueSerde;
    }

    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    public Serde<K> key(){
        return keySerde;
    };

    public Serde<V> value(){
        return valueSerde;
    };

    public Grouped<K, V> grouped(){
        return Grouped.with(keySerde, valueSerde);
    }

    public <VO> Joined<K, V, VO> joined(Serde<VO> otherValueSerde){
        return Joined.with(
                keySerde,
                valueSerde,
                otherValueSerde
        );
    }

    public Materialized<K, V, KeyValueStore<Bytes, byte[]>> materialized(ElMat matConfig){

        var mat = matConfig.getStoreName()
                .map(this::materialized)
                .orElse(materialized());

        if(matConfig.isCachingEnabled()){
            mat.withCachingEnabled();
        }else{
            mat.withCachingDisabled();
        }

        if(matConfig.isLoggingEnabled()){
            mat.withLoggingEnabled(matConfig.getTopicConfig());
        }else{
            mat.withLoggingDisabled();
        }

        if(matConfig.getRetention() != null){
            mat.withRetention(matConfig.getRetention());
        }

        return mat;
    }

    public Materialized<K, V, KeyValueStore<Bytes, byte[]>> materialized(String storeName){
        return Materialized.<K, V, KeyValueStore<Bytes, byte[]>>as(storeName)
                .withKeySerde(keySerde)
                .withValueSerde(valueSerde);
    }

    public Materialized<K, V, KeyValueStore<Bytes, byte[]>> materialized(){
        return Materialized.with(keySerde, valueSerde);
    }

    public Produced<K, V> produced() {
        return Produced.with(keySerde, valueSerde);
    }

    public Consumed<K, V> consumed() {
        return Consumed.with(
                keySerde,
                valueSerde
        );
    }

    public <KR, VR> KStreamSerde<KR, V> withKey(KStreamSerde<KR, VR> serde) {
        return withKey(serde.keySerde);
    }

    public <KR> KStreamSerde<KR, V> withKey(Serde<KR> keySerde) {
        return new KStreamSerde<>(keySerde, valueSerde);
    }

    public <KAny, VR> KStreamSerde<K, VR> withValue(KStreamSerde<KAny, VR> serde) {
        return withValue(serde.valueSerde);
    }

    public <VR> KStreamSerde<K, VR> withValue(Serde<VR> value) {
        return new KStreamSerde<>(keySerde, value);
    }

    /***************************************************************************
     *                                                                         *
     * Private methods                                                         *
     *                                                                         *
     **************************************************************************/

}
