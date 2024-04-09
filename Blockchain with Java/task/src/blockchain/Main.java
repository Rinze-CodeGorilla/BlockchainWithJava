package blockchain;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class Main {
    public static void main(String[] args) throws ExecutionException, InterruptedException, NoSuchAlgorithmException {
        BlockChain chain = new BlockChain();
        ExecutorService e = Executors.newFixedThreadPool(20);
        List<Callable<Block>> tasks = new ArrayList<>(20);
        for (int i = 0; i < 20; i++) {
            var m = new Miner(chain);
            tasks.add(m::mine);
            chain.directory.add(m.person);
        }
        Function<String, Spender> createSpender = p -> new Spender(chain, p);
        var spenders = chain.directory.stream().map(createSpender).toList();
        final int BLOCK_COUNT = 15;
        for (int i = 0; i < BLOCK_COUNT; i++) {
            Block mined = e.invokeAny(tasks);
            int oldPow = chain.pow;
            chain.addBlock(mined);
            System.out.print(mined);
            if (chain.pow > oldPow) {
                System.out.println("N was increased to " + chain.pow);
            } else if (chain.pow < oldPow) {
                System.out.println("N was decreased by 1");
            } else {
                System.out.println("N stays the same");
            }
            System.out.println();
        }
        spenders.forEach(Spender::done);
        e.shutdown();
//        spenders.forEach(s -> {
//            try {
//                System.out.println("Stopping " + s.person);
//                System.out.println(s.f.get());
//            } catch (Exception ex) {
//                System.out.println(ex.getMessage());
//            }
//        });
        System.out.println(chain.validate());
    }
}
