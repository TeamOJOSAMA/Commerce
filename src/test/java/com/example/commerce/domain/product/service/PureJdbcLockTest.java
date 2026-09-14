package com.example.commerce.domain.product.service;

import java.sql.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class  PureJdbcLockTest {

    private static final String URL = "jdbc:mysql://localhost:3306/commerce_locktest?createDatabaseIfNotExist=true";
    private static final String USER = "root";
    private static final String PASSWORD = "12345678";

    public static void main(String[] args) throws Exception {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS lock_test (id BIGINT PRIMARY KEY, stock INT)");
            stmt.execute("DELETE FROM lock_test");
            stmt.execute("INSERT INTO lock_test (id, stock) VALUES (1, 100)");
        }

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch lockAcquired = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);

        executor.submit(() -> {
            try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
                conn.setAutoCommit(false);
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("SELECT * FROM lock_test WHERE id = 1 FOR UPDATE");
                    System.out.println("[A] 락 획득, 3초 유지");
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

                try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT * FROM lock_test WHERE id = 1")) {
                    while (rs.next()) {
                        System.out.println("[B] 조회 성공, stock=" + rs.getInt("stock"));
                    }
                }

                long elapsed = System.currentTimeMillis() - start;
                System.out.println("========================================");
                System.out.println("[B] 단순 조회 소요 시간: " + elapsed + "ms");
                System.out.println(elapsed < 1000 ? "결과: 락에 영향 없음 (정상)" : "결과: 락에 막힘 (문제 확인)");
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