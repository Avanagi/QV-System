package org.system.voting.entity;

public enum VoteStatus {
    PENDING,   // Голос принят, ждем оплаты
    CONFIRMED, // Оплачено, голос учтен
    REJECTED   // Недостаточно средств, голос отменен
}