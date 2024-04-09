package blockchain;

import java.security.PublicKey;

public record Transaction(String from, String to, int amount, byte[] signature, PublicKey publicKey, int id) {
    public String message() {
        return "%s sent %d VC to %s".formatted(from, amount, to);
    }
}
