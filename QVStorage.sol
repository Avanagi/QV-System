// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract QVStorage {

    struct Vote {
        uint256 userId;      // ID пользователя
        uint256 pollId;      // ID опроса
        uint256 optionId;    // ID выбранного варианта
        uint256 voteCount;   // Количество отданных голосов
        uint256 cost;        // Затраченная стоимость (в QV кредитах)
        uint256 timestamp;   // Временная метка блока
    }

    // Хэш-таблица для быстрого поиска: ID голоса -> Данные голоса
    mapping(uint256 => Vote) public votes;

    // Событие для логгирования в EVM
    event VoteSaved(uint256 indexed voteId, uint256 indexed userId, uint256 pollId);

    function saveVote(
        uint256 _voteId,
        uint256 _userId,
        uint256 _pollId,
        uint256 _optionId,
        uint256 _voteCount,
        uint256 _cost
    ) public {

        // Если голос с таким ID есть, его timestamp будет больше 0.
        require(votes[_voteId].timestamp == 0, "Vote already exists!");

        // Запись данных в хранилище (State)
        votes[_voteId] = Vote({
            userId: _userId,
            pollId: _pollId,
            optionId: _optionId,
            voteCount: _voteCount,
            cost: _cost,
            timestamp: block.timestamp
        });

        // Вызов события (запись в логи блокчейна)
        emit VoteSaved(_voteId, _userId, _pollId);
    }
}