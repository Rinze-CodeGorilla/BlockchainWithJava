package blockchain;

import java.util.concurrent.atomic.AtomicInteger;

public class Miner {
    volatile static AtomicInteger nextId = new AtomicInteger(1);
    BlockChain chain;
    String person;

    public Miner(BlockChain chain) {
        this.chain = chain;
        this.person = "miner" + nextId.getAndIncrement();
    }

    public Block mine() {
        return chain.createBlock(person);
    }
}
