package org.system.wallet.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WalletRabbitConfig {

    public static final String EXCHANGE_NAME = "saga-exchange";

    public static final String REWARD_QUEUE = "wallet-reward-queue";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue rewardQueue() {
        return new Queue(REWARD_QUEUE);
    }

    @Bean
    public Binding bindingReward(Queue rewardQueue, TopicExchange exchange) {
        return BindingBuilder.bind(rewardQueue).to(exchange).with("vote.archived");
    }

    @Bean
    public Jackson2JsonMessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }
}