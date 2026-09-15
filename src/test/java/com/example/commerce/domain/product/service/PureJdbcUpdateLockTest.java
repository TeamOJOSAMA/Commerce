package com.example.commerce.domain.product.service;

import java.sql.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PureJdbcUpdateLockTest {

    private static final String URL = "jdbc:mysql://localhost:3306/commerce_locktest?createDatabaseIfNotExist=true";
    private static final String USER = "root";
    private static final String PASSWORD = "12345678";

    public static void main(String[] args) throws Exception {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS lock_test");
            stmt.execute("CREATE TABLE lock_test (id BIGINT PRIMARY KEY, stock INT, view_count INT)");
            stmt.execute("INSERT INTO lock_test (id, stock, view_count) VALUES (1, 100, 0)");
        }

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch lockAcquired = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);

        executor.submit(() -> {
            try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
                conn.setAutoCommit(false);
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("SELECT * FROM lock_test WHERE id = 1 FOR UPDATE");
                    System.out.println("[A] 재고 락 획득, 3초 유지");
                    lockAcquired.countDown();
                    Thread.sleep(3000);
                    conn.commit();
                    System.out.println("[A] 커밋 완료, 락 해제");
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                done.countDown();
            }
        });

        executor.submit(() -> {
            try {
                lockAcquired.await();
                long start = System.currentTimeMillis();

                try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
                    conn.setAutoCommit(true);
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate("UPDATE lock_test SET view_count = view_count + 1 WHERE id = 1");
                    }
                }

                long elapsed = System.currentTimeMillis() - start;
                System.out.println("========================================");
                System.out.println("[B] 조회수 UPDATE 소요 시간: " + elapsed + "ms");
                System.out.println(elapsed < 1000
                        ? "결과: 재고 락에 영향받지 않음"
                        : "결과: 재고 락 때문에 UPDATE가 대기");
                System.out.println("========================================");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                done.countDown();
            }
        });

        done.await();
        executor.shutdown();
    }
}