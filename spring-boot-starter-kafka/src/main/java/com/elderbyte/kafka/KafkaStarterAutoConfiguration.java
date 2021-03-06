package com.elderbyte.kafka;

import com.elderbyte.kafka.admin.DefaultKafkaAdminConfiguration;
import com.elderbyte.kafka.config.KafkaClientProperties;
import com.elderbyte.kafka.consumer.DefaultJsonConsumerConfiguration;
import com.elderbyte.kafka.consumer.factory.KafkaListenerFactoryConfiguration;
import com.elderbyte.kafka.consumer.processing.ManagedProcessorFactoryConfiguration;
import com.elderbyte.kafka.producer.DefaultJsonKafkaTemplateConfiguration;
import com.elderbyte.kafka.producer.KafkaProducerConfiguration;
import com.elderbyte.kafka.serialisation.json.ElderKafkaJsonSerializer;
import com.elderbyte.kafka.streams.ElderKafkaStreamsConfiguration;
import com.elderbyte.kafka.topics.ElderKakfaNewTopicsConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import( {
        ElderKakfaNewTopicsConfig.class,
        KafkaStarterAutoConfiguration.InnerKafkaConfiguration.class,
        KafkaProducerConfiguration.class,
        ManagedProcessorFactoryConfiguration.class,
        KafkaListenerFactoryConfiguration.class
})
public class KafkaStarterAutoConfiguration {

    @Bean
    public KafkaClientProperties kafkaClientProperties(){
        return new KafkaClientProperties();
    }

    @Configuration
    @ConditionalOnProperty(value = "kafka.client.enabled", havingValue = "true", matchIfMissing = true)
    @Import( {
            DefaultKafkaAdminConfiguration.class,
            DefaultJsonKafkaTemplateConfiguration.class,
            DefaultJsonConsumerConfiguration.class,
            ElderKafkaStreamsConfiguration.class
    })
    public static class InnerKafkaConfiguration {

        @Autowired
        private ObjectMapper mapper;

        @Bean
        public ElderKafkaJsonSerializer springKafkaJsonSerializer(){
            return new ElderKafkaJsonSerializer(mapper);
        }

    }


}
