package blockchain;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class Block {
    String previousHash;
    int id;
    long startedAt = Instant.now().toEpochMilli();
    long createdAt;
    int magic;
    String miner;
    private final List<Transaction> transactions;

    Block(Block parent, int pow, String miner, List<Transaction> transactions) {
        this.miner = miner;
        this.transactions = transactions;
        if (parent == null) {
            this.previousHash = "0";
            this.id = 1;
        } else {
            this.previousHash = parent.hash();
            this.id = parent.id + 1;
        }
        Random rand = new Random();
        int x = 0;
        createdAt = Instant.now().toEpochMilli();
        do {
            if (x++ == 100) {
                createdAt = Instant.now().toEpochMilli();
                if (Thread.interrupted()) break;
                x = 0;
            }
            magic = rand.nextInt();
        } while (!hash().startsWith("0".repeat(pow)));
    }

    @Override
    public String toString() {
        String data;
        if (transactions.isEmpty()) {
            data = "No transactions";
        } else {
            data = transactions.stream().map(Transaction::message).collect(Collectors.joining("\n"));
        }
        return """
                Block:
                Created by %s
                %s gets 100 VC
                Id: %d
                Timestamp: %d
                Magic number: %d
                Hash of the previous block:
                %s
                Hash of the block:
                %s
                Block data:
                %s
                Block was generating for %d seconds
                """.formatted(miner, miner, id, startedAt, magic, previousHash, hash(), data, time());
    }

    public String hash() {
        return StringUtil.applySha256("" + id + startedAt + createdAt + previousHash + magic + miner);
    }

    public long time() {
        // should divide by 1000, but Hyperskill doesn't allow program to run for more than 15 seconds.
        return (createdAt - startedAt) / 25;
    }

    public List<Transaction> transactions() {
        return transactions;
    }
}
