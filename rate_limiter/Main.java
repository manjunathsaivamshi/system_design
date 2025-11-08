package rate_limiter;

import java.util.*;
import java.time.*;

class Main {
    private final int capacity;
    private final int refillTime; // seconds
    private final int refillAmount;
    private final Map<String, Token> db;

    Main(int capacity, int refillTime, int refillAmount) {
        this.capacity = capacity;
        this.refillTime = refillTime;
        this.refillAmount = refillAmount;
        this.db = new HashMap<>();
    }

    static class Token {
        public int tokens;
        public LocalDateTime lastRefillTime;

        Token(int tokens, LocalDateTime ts) {
            this.tokens = tokens;
            this.lastRefillTime = ts;
        }
    }

    private Token createBucket() {
        return new Token(this.capacity, LocalDateTime.now());
    }

    private void refillBucket(Token bucket) {
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(bucket.lastRefillTime, now);
        long secondsPassed = duration.getSeconds();

        if (secondsPassed >= refillTime) {
            long refills = secondsPassed / refillTime;
            int tokensToAdd = (int) (refills * refillAmount);
            bucket.tokens = Math.min(capacity, bucket.tokens + tokensToAdd);
            bucket.lastRefillTime = now;
        }
    }

    public boolean handleRequest(String key) {
        Token bucket = db.getOrDefault(key, createBucket());
        refillBucket(bucket);

        if (bucket.tokens > 0) {
            bucket.tokens--;
            db.put(key, bucket);
            System.out.println("✅ Request allowed | Tokens left: " + bucket.tokens);
            return true;
        } else {
            System.out.println("⛔ Request denied | Tokens exhausted");
            db.put(key, bucket);
            return false;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Main limiter = new Main(5, 10, 2); // 5 tokens, refill 2 every 10 seconds
        String key = "user1";

        for (int i = 0; i < 10; i++) {
            limiter.handleRequest(key);
            Thread.sleep(2000); // simulate 2s gap
        }
    }
}
