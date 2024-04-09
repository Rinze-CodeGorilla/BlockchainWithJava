package blockchain;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Random;
import java.util.concurrent.*;

public class Spender {
    static Random random = new Random();
    private final ScheduledExecutorService executor;
    final private KeyPair kp;
    final String person;
    ScheduledFuture<?> f;

    public Spender(BlockChain blockChain, String person) {
        this.person = person;
        final KeyPairGenerator kpg;
        try {
            kpg = KeyPairGenerator.getInstance("Ed25519");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        kp = kpg.generateKeyPair();
        final Signature s;
        try {
            s = Signature.getInstance("Ed25519");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        try {
            s.initSign(kp.getPrivate());
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }

        executor = Executors.newSingleThreadScheduledExecutor();
        f = executor.scheduleAtFixedRate(
                () -> {
                    int balance = blockChain.getBalance(person);
                    if (balance <= 0) {
                        return;
                    }
                    var id = blockChain.getNextMessageId();
                    byte[] signature = null;
                    int amount = random.nextInt(0, balance) + 1;
                    String to;
                    do {
                        to = blockChain.directory.get(random.nextInt(blockChain.directory.size()));
                    } while(to.equals(person));

                    try {
                        s.update(person.getBytes(StandardCharsets.UTF_8));
                        s.update(to.getBytes(StandardCharsets.UTF_8));
                        s.update(ByteBuffer.allocate(4).putInt(id));
                        s.update(ByteBuffer.allocate(4).putInt(amount));
                        signature = s.sign();
                    } catch (SignatureException e) {
                        System.out.println("Error signing message");
                        throw new RuntimeException(e);
                    }
                    var t = new Transaction(
                            person,
                            to,
                            amount,
                            signature,
                            kp.getPublic(),
                            id
                    );
                    try {
                        if (!blockChain.transaction(t)) {
                            System.out.println("My transaction was rejected :( " + t.message());
                        }
                    } catch (NoSuchAlgorithmException e) {
                        System.out.println("NoSuchAlgorithmException");
                        throw new RuntimeException(e);
                    } catch (InvalidKeyException e) {
                        System.out.println("InvalidKeyException");
                        throw new RuntimeException(e);
                    }
                },
                random.nextLong(100), random.nextLong(500, 1000), TimeUnit.MILLISECONDS);
    }

    public void done() {
        executor.shutdownNow();
    }
}
