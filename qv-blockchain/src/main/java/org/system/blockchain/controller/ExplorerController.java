package org.system.blockchain.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.system.blockchain.dto.TransactionDto;
import org.system.blockchain.service.ExplorerService;

import java.util.List;

@RestController
@RequestMapping("/api/blockchain")
@RequiredArgsConstructor
public class ExplorerController {

    private final ExplorerService explorerService;

    @GetMapping("/explorer")
    public List<TransactionDto> getExplorerData() throws Exception {
        return explorerService.getAllTransactions();
    }
}