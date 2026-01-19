package org.system.blockchain.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.system.blockchain.config.BlockchainRabbitConfig;
import org.system.blockchain.service.BlockchainService;
import org.system.common.event.FundsReservedEvent;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class BlockchainListener {

    private final BlockchainService blockchainService;
    private final StringRedisTemplate redisTemplate;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("blockchain-queue"),
            exchange = @Exchange(value = BlockchainRabbitConfig.EXCHANGE_NAME, type = ExchangeTypes.TOPIC),
            key = "wallet.reserved"
    ))
    public void handleConfirmedVote(FundsReservedEvent event,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                    @Header(AmqpHeaders.REDELIVERED) boolean redelivered) {

        log.info("📨 RABBIT MSG: ID={}, Redelivered={}, EventVoteId={}",
                deliveryTag, redelivered, event.getVoteId());
        String idempotencyKey = "blockchain:processed:" + event.getVoteId();
        Boolean isNew = redisTemplate.opsForValue()
                .setIfAbsent(idempotencyKey, "1", Duration.ofHours(24));

        if (Boolean.FALSE.equals(isNew)) {
            log.warn("ДУБЛИКАТ: Транзакция для голоса {} уже была отправлена в блокчейн. Пропускаем.", event.getVoteId());
            return;
        }

        log.info("Получен новый голос ID: {}. Начинаем запись в блокчейн...", event.getVoteId());

        blockchainService.writeVoteToBlockchain(
                event.getVoteId(),
                event.getUserId(),
                event.getProjectId(),
                event.getVoteCount(),
                event.getCost(),
                event.getPollTitle(),
                event.getOptionText(),
                event.getPollId()
        );
    }
}