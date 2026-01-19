package org.system.voting.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_NAME = "saga-exchange";
    public static final String ARCHIVE_QUEUE = "voting-archive-queue";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue archiveQueue() {
        return new Queue(ARCHIVE_QUEUE);
    }

    @Bean
    public Binding bindingArchive(Queue archiveQueue, TopicExchange exchange) {
        return BindingBuilder.bind(archiveQueue).to(exchange).with("vote.archived");
    }

    @Bean
    public Jackson2JsonMessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }
}