package org.system.blockchain.contract;

import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;


public class QVStorage extends Contract {

    protected QVStorage(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider gasProvider) {
        super("", contractAddress, web3j, transactionManager, gasProvider);
    }

    public RemoteFunctionCall<TransactionReceipt> saveVote(
            BigInteger _voteId, BigInteger _userId, BigInteger _pollId,
            BigInteger _optionId, BigInteger _voteCount, BigInteger _cost) {

        final Function function = new Function(
                "saveVote",
                Arrays.<Type>asList(
                        new Uint256(_voteId),
                        new Uint256(_userId),
                        new Uint256(_pollId),
                        new Uint256(_optionId),
                        new Uint256(_voteCount),
                        new Uint256(_cost)
                ),
                Collections.<TypeReference<?>>emptyList());

        return executeRemoteCallTransaction(function);
    }

    public static RemoteCall<QVStorage> deploy(
            Web3j web3j, TransactionManager transactionManager,
            ContractGasProvider gasProvider, String bytecode) {

        return deployRemoteCall(QVStorage.class, web3j, transactionManager, gasProvider, bytecode, "");
    }

    public static QVStorage load(
            String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider gasProvider) {

        return new QVStorage(contractAddress, web3j, transactionManager, gasProvider);
    }
}