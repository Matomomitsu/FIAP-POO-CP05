package br.com.fiap.petfiap.service;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;

final class ConcorrenciaTestSupport {

    private ConcorrenciaTestSupport() {
    }

    static void aguardar(CountDownLatch sinal) throws InterruptedException {
        if (!sinal.await(5, TimeUnit.SECONDS)) {
            throw new AssertionError("Operacao nao chegou ao ponto esperado");
        }
    }

    static void aguardarSegundaOperacao(Future<?> segunda, AtomicReference<Thread> thread, Object service) {
        // A versao corrigida bloqueia no monitor do service; a antiga termina cedo.
        // Assim o teste força a disputa sem depender de um sleep arbitrario.
        await().pollInterval(Duration.ofMillis(10)).atMost(Duration.ofSeconds(5))
                .until(() -> segunda.isDone() || bloqueadaNoService(thread.get(), service));
    }

    private static boolean bloqueadaNoService(Thread thread, Object service) {
        if (thread == null) {
            return false;
        }
        ThreadInfo info = ManagementFactory.getThreadMXBean().getThreadInfo(thread.getId());
        return info != null && info.getThreadState() == Thread.State.BLOCKED
                && info.getLockInfo() != null
                && info.getLockInfo().getIdentityHashCode() == System.identityHashCode(service);
    }
}
