package blockchain;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class BlockChain {
    private final static int MIN_TIME_BETWEEN_BLOCKS = 5;
    private final static int MAX_TIME_BETWEEN_BLOCKS = 60;
//    final List<String> directory = new ArrayList<>(List.of("Nick", "Bob", "Alice", "ShoesShop", "FastFood", "CarShop", "Worker1", "Worker2", "Worker3", "Director1", "CarPartShop", "GamingShop", "BeautyShop"));
        final List<String> directory = new ArrayList<>(List.of("Nick", "Bob", "Alice"));
    final List<Block> chain = new ArrayList<>();
    final BlockingQueue<Transaction> transactions = new LinkedBlockingQueue<>();
    final AtomicInteger messageId = new AtomicInteger(0);
    final Signature signature;
    List<Transaction> previousTransactions = new ArrayList<>();
    int pow = 0;

    public BlockChain() throws NoSuchAlgorithmException {
        signature = Signature.getInstance("Ed25519");
    }

    public int getNextMessageId() {
        return messageId.incrementAndGet();
    }

    public void addBlock(Block block) {
        Block previous = null;
        if (!chain.isEmpty()) {
            previous = chain.get(chain.size() - 1);
        }
        if (blockIsInvalid(block, previous)) {
            System.out.println("Invalid block");
            return;
        }
        if (block.time() > MAX_TIME_BETWEEN_BLOCKS) pow--;
        else if (block.time() < MIN_TIME_BETWEEN_BLOCKS) pow++;
        synchronized (transactions) {
            chain.add(block);
            previousTransactions = new ArrayList<>();
            transactions.drainTo(previousTransactions);
        }
    }

    public Block createBlock(String miner) {
        Block previous = null;
        if (!chain.isEmpty()) {
            previous = chain.get(chain.size() - 1);
        }
        return new Block(previous, pow, miner, previousTransactions);
    }

    public boolean validate() {
        var temp = pow;
        try {
            pow = 0;
            Block parent = null;
            Map<String, Integer> balances = new HashMap<>();
            for (Block block : chain) {
                balances.put(block.miner, balances.getOrDefault(block.miner, 0) + 100);
                block.transactions().forEach(transaction -> {
                    balances.put(transaction.from(), balances.getOrDefault(transaction.from(), 0) - transaction.amount());
                    balances.put(transaction.to(), balances.getOrDefault(transaction.to(), 0) + transaction.amount());
                });
                if (blockIsInvalid(block, parent)) return false;
                if (block.time() > MAX_TIME_BETWEEN_BLOCKS) pow--;
                else if (block.time() < MIN_TIME_BETWEEN_BLOCKS) pow++;
                parent = block;
            }
            if (balances.entrySet().stream().filter(es -> es.getValue() < 0).peek(es -> System.out.println("Illegal balance %s %d".formatted(es.getKey(), es.getValue()))).count() != 0) {
                System.out.println("Illegal balance");
                return false;
            }
            return true;
        } finally {
            pow = temp;
        }
    }

    boolean blockIsInvalid(Block block, Block previous) {
        int parentId;
        String previousHash;
        List<Transaction> previousTransactions;
        if (previous == null) {
            parentId = 0;
            previousHash = "0";
            previousTransactions = List.of();
        } else {
            parentId = previous.id;
            previousHash = previous.hash();
            previousTransactions = previous.transactions();
        }
        if (block.id <= parentId) {
            System.out.println("Block id is not higher than previous block id");
            return true;
        }
        if (!block.previousHash.equals(previousHash)) {
            System.out.println("Previous hash doesn't match");
            return true;
        }
        if (block.transactions().stream().mapToInt(Transaction::id).distinct().count() != block.transactions().size()) {
            System.out.println("Message ids are not unique");
            return true;
        }
        int maxPreviousId = previousTransactions.stream().mapToInt(Transaction::id).reduce(0, Integer::max);
        if (block.transactions().stream().mapToInt(Transaction::id).anyMatch(id -> id <= maxPreviousId)) {
            System.out.println("Message ids are not higher than ids of previous block's messages");
            return true;
        }
        return !block.hash().startsWith("0".repeat(pow));
    }

    public boolean transaction(Transaction transaction) throws NoSuchAlgorithmException, InvalidKeyException {
        var s = Signature.getInstance("Ed25519");
        try {
            s.initVerify(transaction.publicKey());
            s.update(transaction.from().getBytes(StandardCharsets.UTF_8));
            s.update(transaction.to().getBytes(StandardCharsets.UTF_8));
            s.update(ByteBuffer.allocate(4).putInt(transaction.id()));
            s.update(ByteBuffer.allocate(4).putInt(transaction.amount()));
            s.verify(transaction.signature());
            synchronized (transactions) {
                if (chain.get(chain.size() - 1).transactions().stream().mapToInt(Transaction::id).reduce(0, Integer::max) >= transaction.id()) {
                    System.out.println("Bad ID");
                    return false;
                }
                transactions.add(transaction);
            }
        } catch (SignatureException e) {
            System.out.println("Invalid signature for transaction: " + transaction.message());
            return false;
        } catch (InvalidKeyException e) {
            System.out.println("Invalid key for transaction: " + transaction.message());
            return false;
        }
        return true;
    }

    public int getBalance(String person) {
        var mining = chain.stream().mapToInt(block -> (block.miner.equals(person) ? 100 : 0)).sum();
        var spending = chain.stream().mapToInt(block -> block.transactions().stream().filter(t -> t.from().equals(person)).mapToInt(Transaction::amount).sum()).sum();
        var receiving = chain.stream().mapToInt(block -> block.transactions().stream().filter(t -> t.to().equals(person)).mapToInt(Transaction::amount).sum()).sum();
        var spendingPrevious = previousTransactions.stream().filter(t -> t.from().equals(person)).mapToInt(Transaction::amount).sum();
//        var receivingPrevious = previousTransactions.stream().filter(t -> t.to().equals(person)).mapToInt(Transaction::amount).sum();
        var spendingCurrent = transactions.stream().filter(t -> t.from().equals(person)).mapToInt(Transaction::amount).sum();
//        var receivingCurrent = transactions.stream().filter(t -> t.to().equals(person)).mapToInt(Transaction::amount).sum();
        int balance = mining - spending - spendingPrevious - spendingCurrent + receiving;
        return balance;
    }
}
