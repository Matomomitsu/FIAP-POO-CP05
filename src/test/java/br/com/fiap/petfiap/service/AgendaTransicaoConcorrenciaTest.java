package br.com.fiap.petfiap.service;

import br.com.fiap.petfiap.exception.StatusInvalidoException;
import br.com.fiap.petfiap.model.Atendimento;
import br.com.fiap.petfiap.model.Banho;
import br.com.fiap.petfiap.repository.AtendimentoRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AgendaTransicaoConcorrenciaTest {

    @Mock
    private AtendimentoRepository repository;

    @InjectMocks
    private AgendaService service;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void deveRecusarSegundaTransicaoEnquantoPrimeiraAindaEstaGravando(boolean concluirPrimeiro) throws Exception {
        // Arrange: cada leitura devolve uma copia, como em requisicoes JPA diferentes.
        LocalDateTime horario = LocalDateTime.now().plusDays(1);
        AtomicReference<String> statusPersistido = new AtomicReference<>("AGENDADO");
        CountDownLatch primeiraGravacao = new CountDownLatch(1);
        CountDownLatch liberarGravacao = new CountDownLatch(1);
        AtomicInteger gravacoes = new AtomicInteger();
        AtomicReference<Thread> segundaThread = new AtomicReference<>();
        when(repository.findById(1L)).thenAnswer(invocation -> {
            Banho copia = new Banho(1, "Rex", "PEQUENO", "Ana", horario);
            copia.setId(1L);
            copia.setStatus(statusPersistido.get());
            return Optional.of(copia);
        });
        when(repository.save(any(Atendimento.class))).thenAnswer(invocation -> {
            Atendimento atendimento = invocation.getArgument(0);
            if (gravacoes.incrementAndGet() == 1) {
                primeiraGravacao.countDown();
                ConcorrenciaTestSupport.aguardar(liberarGravacao);
            }
            statusPersistido.set(atendimento.getStatus());
            return atendimento;
        });
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            // Act: exercita ambas as ordens de conclusao/cancelamento.
            Future<Atendimento> primeira = executor.submit(() -> concluirPrimeiro
                    ? service.concluir(1L) : service.cancelar(1L));
            ConcorrenciaTestSupport.aguardar(primeiraGravacao);
            Future<Atendimento> segunda = executor.submit(() -> {
                segundaThread.set(Thread.currentThread());
                return concluirPrimeiro ? service.cancelar(1L) : service.concluir(1L);
            });
            ConcorrenciaTestSupport.aguardarSegundaOperacao(segunda, segundaThread, service);
            liberarGravacao.countDown();

            // Assert
            String statusEsperado = concluirPrimeiro ? "CONCLUIDO" : "CANCELADO";
            assertEquals(statusEsperado, primeira.get(5, TimeUnit.SECONDS).getStatus());
            ExecutionException recusa = assertThrows(ExecutionException.class,
                    () -> segunda.get(5, TimeUnit.SECONDS));
            assertInstanceOf(StatusInvalidoException.class, recusa.getCause());
            assertEquals(statusEsperado, statusPersistido.get());
            verify(repository, times(1)).save(any(Atendimento.class));
        } finally {
            liberarGravacao.countDown();
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
