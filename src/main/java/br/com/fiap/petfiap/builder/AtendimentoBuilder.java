package br.com.fiap.petfiap.builder;

import br.com.fiap.petfiap.factory.AtendimentoFactory;
import br.com.fiap.petfiap.model.Atendimento;

import java.time.LocalDateTime;
import java.util.Set;

// Padrao Builder (Aula 14): monta um atendimento complexo passo a passo,
// sem construtor gigante no controller.
public class AtendimentoBuilder {

    private static final Set<String> PORTES_VALIDOS = Set.of("PEQUENO", "MEDIO", "GRANDE");

    private String tipo;
    private String petNome;
    private String petPorte;
    private String tutorNome;
    private LocalDateTime dataHora;

    public AtendimentoBuilder comTipo(String tipo) {
        this.tipo = tipo;
        return this;
    }

    public AtendimentoBuilder comPet(String petNome, String petPorte) {
        this.petNome = petNome;
        this.petPorte = petPorte;
        return this;
    }

    public AtendimentoBuilder comTutor(String tutorNome) {
        this.tutorNome = tutorNome;
        return this;
    }

    public AtendimentoBuilder comDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
        return this;
    }

    // Valida os dados do pet antes de criar o atendimento.
    public Atendimento construir(int protocolo) {
        if (petNome == null || petNome.isBlank()) {
            throw new IllegalArgumentException("Nome do pet obrigatorio");
        }
        if (petPorte == null || petPorte.isBlank()) {
            throw new IllegalArgumentException("Porte do pet obrigatorio");
        }
        if (!PORTES_VALIDOS.contains(petPorte)) {
            throw new IllegalArgumentException("Porte do pet invalido: " + petPorte);
        }
        return AtendimentoFactory.criar(protocolo, tipo, petNome, petPorte, tutorNome, dataHora);
    }
}
