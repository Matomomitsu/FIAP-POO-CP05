package br.com.fiap.petfiap.service;

import br.com.fiap.petfiap.exception.HorarioOcupadoException;
import br.com.fiap.petfiap.model.Atendimento;
import br.com.fiap.petfiap.model.Banho;
import br.com.fiap.petfiap.repository.AtendimentoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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
public class AgendaAgendamentoConcorrenciaTest {

    @Mock
    private AtendimentoRepository repository;

    @InjectMocks
    private AgendaService service;

    @Test
    public void deveSalvarSomenteUmAgendamentoQuandoDuasChamadasDisputaremOMesmoHorario() throws Exception {
        // Arrange: a primeira gravacao fica pendente enquanto a segunda chamada entra.
        LocalDateTime horario = LocalDateTime.now().plusDays(1);
        Banho primeiro = new Banho(1, new String("Rex"), "PEQUENO", "Ana", horario);
        Banho segundo = new Banho(2, new String("Rex"), "PEQUENO", "Ana", horario);
        List<Atendimento> salvos = new CopyOnWriteArrayList<>();
        CountDownLatch primeiraGravacao = new CountDownLatch(1);
        CountDownLatch liberarGravacao = new CountDownLatch(1);
        AtomicInteger gravacoes = new AtomicInteger();
        AtomicReference<Thread> segundaThread = new AtomicReference<>();
        when(repository.findByPetNome("Rex")).thenAnswer(invocation -> List.copyOf(salvos));
        when(repository.save(any(Atendimento.class))).thenAnswer(invocation -> {
            Atendimento atendimento = invocation.getArgument(0);
            if (gravacoes.incrementAndGet() == 1) {
                primeiraGravacao.countDown();
                ConcorrenciaTestSupport.aguardar(liberarGravacao);
            }
            salvos.add(atendimento);
            return atendimento;
        });
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            // Act
            Future<Atendimento> primeira = executor.submit(() -> service.agendar(primeiro));
            ConcorrenciaTestSupport.aguardar(primeiraGravacao);
            Future<Atendimento> segunda = executor.submit(() -> {
                segundaThread.set(Thread.currentThread());
                return service.agendar(segundo);
            });
            ConcorrenciaTestSupport.aguardarSegundaOperacao(segunda, segundaThread, service);
            liberarGravacao.countDown();

            // Assert
            assertEquals(primeiro, primeira.get(5, TimeUnit.SECONDS));
            ExecutionException recusa = assertThrows(ExecutionException.class,
                    () -> segunda.get(5, TimeUnit.SECONDS));
            assertInstanceOf(HorarioOcupadoException.class, recusa.getCause());
            assertEquals(1, salvos.size());
            verify(repository, times(1)).save(any(Atendimento.class));
        } finally {
            liberarGravacao.countDown();
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
