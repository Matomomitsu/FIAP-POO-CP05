package br.com.fiap.petfiap.service;

import br.com.fiap.petfiap.exception.AtendimentoNaoEncontradoException;
import br.com.fiap.petfiap.exception.HorarioOcupadoException;
import br.com.fiap.petfiap.model.Atendimento;
import br.com.fiap.petfiap.repository.AtendimentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

// Regras de agenda do PetFiap: agendar, concluir e cancelar atendimentos.
@Service
public class AgendaService {

    private final AtendimentoRepository repository;

    @Autowired
    public AgendaService(AtendimentoRepository repository) {
        this.repository = repository;
    }

    // No bean singleton, consulta, validacao e gravacao compartilham o mesmo monitor.
    // A segunda requisicao so consulta depois de a primeira gravacao terminar.
    public synchronized Atendimento agendar(Atendimento novo) {
        if (novo.getDataHora() == null || novo.getDataHora().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Data e hora devem ser informadas e nao podem estar no passado");
        }
        List<Atendimento> atendimentosDoPet = repository.findByPetNome(novo.getPetNome());
        for (Atendimento existente : atendimentosDoPet) {
            if (existente.getPetNome().equals(novo.getPetNome()) && existente.getDataHora().equals(novo.getDataHora())
                    && "AGENDADO".equals(existente.getStatus())) {
                throw new HorarioOcupadoException(
                        "Pet " + novo.getPetNome() + " ja possui atendimento agendado nesse horario");
            }
        }
        return repository.save(novo);
    }

    // Busca pelo id; nunca retorna null, o orElseThrow garante a excecao.
    public Atendimento buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new AtendimentoNaoEncontradoException("Atendimento nao encontrado: " + id));
    }

    // Conclui o atendimento (status AGENDADO -> CONCLUIDO).
    public Atendimento concluir(Long id) {
        Atendimento atendimento = buscarPorId(id);
        atendimento.concluir();
        return repository.save(atendimento);
    }

    // Cancela o atendimento (status AGENDADO -> CANCELADO).
    public Atendimento cancelar(Long id) {
        Atendimento atendimento = buscarPorId(id);
        atendimento.cancelar();
        return repository.save(atendimento);
    }

    // Lista os atendimentos de um pet.
    public List<Atendimento> buscarPorPet(String petNome) {
        return repository.findByPetNome(petNome);
    }
}
