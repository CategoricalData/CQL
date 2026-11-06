package catdata;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DeadLockDetector}. The check runs directly rather than on its schedule,
 * whose thread cannot be stopped, and the console handler only on no threads, since it opens a
 * dialog otherwise.
 */
class DeadLockDetectorTest {

  /**
   * Two daemon threads that take two locks in opposite orders, interruptibly, so that they can be
   * released once the check has seen them.
   */
  private static List<Thread> deadlock() {
    ReentrantLock a = new ReentrantLock();
    ReentrantLock b = new ReentrantLock();
    CountDownLatch bothHeld = new CountDownLatch(2);
    List<Thread> threads = new ArrayList<>();
    ReentrantLock[][] orders = {{a, b}, {b, a}};
    for (int i = 0; i < orders.length; i++) {
      ReentrantLock[] order = orders[i];
      Thread t =
          new Thread(
              () -> {
                try {
                  order[0].lockInterruptibly();
                  bothHeld.countDown();
                  bothHeld.await();
                  order[1].lockInterruptibly();
                } catch (InterruptedException ex) {
                  Thread.currentThread().interrupt();
                }
              },
              "deadlock-detector-test-" + i);
      t.setDaemon(true);
      t.start();
      threads.add(t);
    }
    return threads;
  }

  private static Set<String> names(ThreadInfo[] infos) {
    return Arrays.stream(infos).map(ThreadInfo::getThreadName).collect(Collectors.toSet());
  }

  private static boolean deadlocked(List<Thread> threads) {
    ThreadMXBean mbean = ManagementFactory.getThreadMXBean();
    long[] ids = mbean.findDeadlockedThreads();
    if (ids == null) {
      return false;
    }
    Set<String> found = names(mbean.getThreadInfo(ids));
    return threads.stream().allMatch(t -> found.contains(t.getName()));
  }

  @Test
  void handsTheDeadlockedThreadsToItsHandler() throws Exception {
    List<ThreadInfo[]> seen = new ArrayList<>();
    DeadLockDetector detector = new DeadLockDetector(seen::add, 1, TimeUnit.HOURS);
    List<Thread> threads = deadlock();
    try {
      Instant end = Instant.now().plus(Duration.ofSeconds(10));
      while (!deadlocked(threads) && Instant.now().isBefore(end)) {
        Thread.onSpinWait();
      }
      assertTrue(deadlocked(threads), "the two threads never deadlocked");
      detector.deadlockCheck.run();
      assertEquals(1, seen.size());
      Set<String> found = names(seen.get(0));
      for (Thread t : threads) {
        assertTrue(found.contains(t.getName()), found.toString());
      }
      assertNotNull(seen.get(0)[0].getLockName());
    } finally {
      for (Thread t : threads) {
        t.interrupt();
        t.join(TimeUnit.SECONDS.toMillis(10));
      }
    }
  }

  @Test
  void ignoresAMissingListOfThreads() {
    assertDoesNotThrow(() -> new DeadLockDetector.DeadlockConsoleHandler().handleDeadlock(null));
  }
}
