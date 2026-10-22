package com.quantumledger.app.transfer;

import com.quantumledger.app.account.Account;
import com.quantumledger.app.account.AccountService;
import com.quantumledger.app.ledger.LedgerTransactionType;
import com.quantumledger.app.posting.PostingLine;
import com.quantumledger.app.posting.PostingRequest;
import com.quantumledger.app.posting.PostingResult;
import com.quantumledger.app.posting.PostingService;
import com.quantumledger.app.user.User;
import com.quantumledger.app.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class TransferService {

    public record TransferCommand(Long senderId, String receiverCountryCode, String receiverPhone, long amountMinor) { }

    public record TransferReceipt(Transfer transfer, String receiverFirstName) { }

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final UserService userService;
    private final AccountService accountService;
    private final PostingService postingService;
    private final TransferRepository transferRepository;
    private final TransferProperties properties;

    public TransferService(UserService userService,
                           AccountService accountService,
                           PostingService postingService,
                           TransferRepository transferRepository,
                           TransferProperties properties) {

        this.userService = userService;
        this.accountService = accountService;
        this.postingService = postingService;
        this.transferRepository = transferRepository;
        this.properties = properties;
    }

    @Transactional
    public TransferReceipt transfer(TransferCommand command) {
        long amount = command.amountMinor();
        if(amount < properties.minAmountMinor() || amount > properties.maxAmountMinor()) {
            throw new TransferRejectedException(TransferRejection.AMOUNT_OUT_OF_RANGE);
        }

        User sender = userService.getActiveUser(command.senderId());

        User receiver = userService.findByPhone(command.receiverCountryCode(), command.receiverPhone())
                .orElseThrow(ReceiverNotFoundException::new);

        if(receiver.getUserId().equals(sender.getUserId())) {
            throw new TransferRejectedException(TransferRejection.CANNOT_TRANSFER_TO_SELF);
        }

        if(receiver.isDeleted() || !"ACTIVE".equals(receiver.getStatus())) {
            throw new TransferRejectedException(TransferRejection.RECEIVER_UNAVAILABLE);
        }

        Account senderWallet = accountService.getWallet(sender.getUserId());
        Account receiverWallet = accountService.getWallet(receiver.getUserId());
        String currency = senderWallet.getCurrency();

        PostingResult posting =  postingService.post(new PostingRequest(
                LedgerTransactionType.TRANSFER, "P2P transfer", currency,
                List.of(PostingLine.debit(senderWallet.getAccountId(), amount),
                        PostingLine.credit(receiverWallet.getAccountId(), amount))
        ));

        Transfer transfer = transferRepository.save(Transfer.completed(
                sender.getUserId(), receiver.getUserId(),
                senderWallet.getAccountId(), receiverWallet.getAccountId(),
                amount, currency, posting.ledgerTransactionId()
        ));

        log.info("Transfer {} completed: amount={} ledgerTransaction={}",
                transfer.getTransferRef(), amount, posting.ledgerTransactionId());

        return new TransferReceipt(transfer, receiver.getFirstName());
    }
}
