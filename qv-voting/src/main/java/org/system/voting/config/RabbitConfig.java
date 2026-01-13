package org.system.voting.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_NAME = "saga-exchange";

    public static final String SUCCESS_QUEUE = "voting-success-queue";
    public static final String FAIL_QUEUE = "voting-fail-queue";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue successQueue() {
        return new Queue(SUCCESS_QUEUE);
    }

    @Bean
    public Queue failQueue() {
        return new Queue(FAIL_QUEUE);
    }

    @Bean
    public Binding bindingSuccess(Queue successQueue, TopicExchange exchange) {
        return BindingBuilder.bind(successQueue).to(exchange).with("wallet.reserved");
    }

    @Bean
    public Binding bindingFail(Queue failQueue, TopicExchange exchange) {
        return BindingBuilder.bind(failQueue).to(exchange).with("wallet.failed");
    }

    @Bean
    public Jackson2JsonMessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }
}