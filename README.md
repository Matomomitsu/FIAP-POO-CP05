# Checkpoint 5 — Bug Hunt PetFiap

API de agendamentos de pet shop e clínica veterinária, desenvolvida em Java com Spring Boot e Spring Data JPA. O projeto recebido foi corrigido sem reescrever sua estrutura e sem adicionar dependências ao `pom.xml`.

## Identificação

**Grupo:** Entrega individual

| Integrante | RM | Turma |
|---|---|---|
| Mateus Scandiuzzi Valente Tomomitsu | 561565 | 2CCPW |

| Campo | Resultado |
|---|---|
| Bugs do enunciado corrigidos | 12 / 12 |
| Validações adicionais corrigidas | 2 / 2 |
| Total de ajustes de Clean Code | 6 / 6 |
| Testes novos do enunciado | 6 / 6 |
| Testes das validações adicionais | 3 métodos parametrizados, 12 cenários |
| Suíte final | 38 testes, 0 falhas, 0 erros, 0 ignorados |
| Suíte original preservada | 20 testes, sem alterações |

## Parte 1 — Bugs encontrados

A numeração corresponde aos commits `fix: bugNN`. As linhas indicadas são referências aproximadas na versão final; o primeiro commit preserva o código recebido para comparação.

| # | Sintoma observado | Causa raiz (arquivo e linha aproximada) | Correção aplicada | Conceito da disciplina |
|---|---|---|---|---|
| bug01 | O Builder recebia `Rex`, mas o atendimento era criado com nome `null`. | [AtendimentoBuilder.java:24](src/main/java/br/com/fiap/petfiap/builder/AtendimentoBuilder.java#L24): `petNome = petNome` atribuía o parâmetro a ele mesmo. | Usar `this.petNome = petNome` para preencher o atributo da instância. | Atributos, parâmetros, encapsulamento e uso de `this`. |
| bug02 | A construção aceitava nome ou porte ausentes, contrariando dois testes originais. | [AtendimentoBuilder.java:40](src/main/java/br/com/fiap/petfiap/builder/AtendimentoBuilder.java#L40): `construir()` chamava a Factory sem validar os dados obrigatórios do pet. | Validar nome e porte, incluindo `null` e texto em branco, antes da criação; lançar `IllegalArgumentException` com mensagem clara. | Builder, validação centralizada e fail fast. |
| bug03 | A Factory devolvia `Banho` ao receber o tipo `TOSA`. | [AtendimentoFactory.java:18](src/main/java/br/com/fiap/petfiap/factory/AtendimentoFactory.java#L18): o case `TOSA` instanciava a classe errada. | Instanciar `Tosa` nesse case. | Factory e polimorfismo. |
| bug04 | A consulta veterinária perdia os dados do pet e do tutor; protocolo, data e status também não eram inicializados pelo construtor completo. | [ConsultaVeterinaria.java:20](src/main/java/br/com/fiap/petfiap/model/ConsultaVeterinaria.java#L20): o construtor chamava `super()` e descartava os argumentos. | Encaminhar todos os argumentos para o construtor de `Atendimento`. | Herança e encadeamento de construtores. |
| bug05 | Duas chamadas devolviam instâncias distintas e os protocolos saíam como `1, 1, 1`. | [GeradorProtocolo.java:16](src/main/java/br/com/fiap/petfiap/model/GeradorProtocolo.java#L16): o objeto criado não era guardado em `instancia`; o acesso e o contador também não tinham sincronização. | Guardar a instância criada e sincronizar `getInstancia()` e `proximo()`, preservando uma sequência compartilhada sob concorrência. | Singleton, estado compartilhado e sincronização. |
| bug06 | Um horário igual, representado por outro objeto `LocalDateTime`, escapava da verificação e chegava ao `save()`. No teste, isso acabava gerando `NullPointerException` em vez de `HorarioOcupadoException`. | [AgendaService.java:31](src/main/java/br/com/fiap/petfiap/service/AgendaService.java#L31): nome e data eram comparados por referência com `==`. | Comparar os valores com `.equals()` e recusar o conflito antes de salvar. | Igualdade de objetos, Strings e regras de negócio. |
| bug07 | Buscar um ID inexistente retornava `null`, mesmo com `orElseThrow()` no método. | [AgendaService.java:41](src/main/java/br/com/fiap/petfiap/service/AgendaService.java#L41): um `catch (Exception)` capturava a exceção de domínio e devolvia `null`. | Remover a captura genérica e deixar `AtendimentoNaoEncontradoException` chegar ao controller. | Exceções unchecked, propagação e tratamento específico. |
| bug08 | O banho pequeno custava R$ 100,00 e o grande R$ 60,00. | [Banho.java:32](src/main/java/br/com/fiap/petfiap/model/Banho.java#L32): os valores dos portes pequeno e grande estavam invertidos. | Aplicar R$ 60,00, R$ 80,00 e R$ 100,00 para pequeno, médio e grande. | Regras de negócio nas subclasses e testes de valores. |
| bug09 | A duração da tosa era 30 minutos quando acessada pela referência `Atendimento`. | [Tosa.java:47](src/main/java/br/com/fiap/petfiap/model/Tosa.java#L47): `getDuracaoMinutos(String porte)` era uma sobrecarga, não uma sobrescrita do método sem parâmetros. | Corrigir a assinatura para `getDuracaoMinutos()` e adicionar `@Override`; retornar 60 minutos. | Sobrescrita, sobrecarga e despacho polimórfico. |
| bug10 | Era possível cancelar um atendimento concluído ou cancelar novamente um já cancelado. | [Atendimento.java:66](src/main/java/br/com/fiap/petfiap/model/Atendimento.java#L66): `cancelar()` mudava o status sem verificar o estado atual. | Permitir cancelamento apenas em `AGENDADO`; nos outros estados, lançar `StatusInvalidoException` sem alterar o objeto. | Transições de estado e exceções de domínio. |
| bug11 | Um agendamento no passado chegava ao repository; o teste recebia `NullPointerException` em vez da validação esperada. | [AgendaService.java:25](src/main/java/br/com/fiap/petfiap/service/AgendaService.java#L25): não existia validação temporal antes da consulta de conflitos. | Recusar data ausente ou passada com `IllegalArgumentException` antes de qualquer interação com o repository. | Fail fast, validação e isolamento com Mockito. |
| bug12 | Mesmo com a suíte verde, um POST válido retornava HTTP 500 ao persistir: `IdentifierGenerationException`. | [Atendimento.java:16](src/main/java/br/com/fiap/petfiap/model/Atendimento.java#L16): o campo `id` tinha apenas `@Id`, sem geração automática; o fluxo de criação não fornecia ID manual. | Adicionar `@GeneratedValue(strategy = GenerationType.IDENTITY)` e verificar a criação real usando H2. O ID do banco permanece separado do protocolo do Singleton. | JPA, chave primária gerada e diferença entre teste unitário e integração. |

## Parte 2 — Ajustes de Clean Code

Cada ajuste possui seu próprio commit `refactor: cleanNN`.

| # | Onde estava | Princípio ou boa prática violada | O que mudei |
|---|---|---|---|
| clean01 | Parâmetros de `AtendimentoFactory.criar()` e variáveis do laço de `AgendaService.agendar()`. | Nomes como `p`, `t`, `n`, `po`, `tu`, `d` e `a` escondiam o significado dos dados. | Usar `protocolo`, `tipo`, `petNome`, `petPorte`, `tutorNome`, `dataHora`, `atendimentosDoPet` e `existente`. |
| clean02 | Preços, pontos e durações em `Banho`, `Tosa`, `ConsultaVeterinaria` e `Atendimento`. | Números soltos dificultavam a leitura e a manutenção das regras. | Extrair constantes privadas com nomes como `PRECO_PEQUENO`, `PONTOS_FIDELIDADE` e `DURACAO_MINUTOS`, mantendo os valores do contrato. |
| clean03 | Impressão do recibo em `AgendaService.agendar()`. | Responsabilidade única: o serviço de agenda também formatava e emitia um recibo no console. | Remover a emissão do recibo e retornar diretamente o atendimento salvo pelo repository. |
| clean04 | `System.out.println()` no construtor de `GeradorProtocolo`. | Saída de depuração como efeito colateral de construir um objeto de domínio. | Remover a mensagem de criação, deixando o gerador responsável por instância e numeração. |
| clean05 | `calcularDescontoFidelidade()` e comentários de funcionalidades futuras no controller. | Código morto e implementação antecipada de uma regra sem uso ou contrato. | Remover o método privado não utilizado e seus comentários de planejamento. |
| clean06 | Dependências injetadas diretamente nos campos de `AgendaService` e `AtendimentoController`. | Dependências obrigatórias ficavam ocultas na construção e os objetos podiam nascer com campos nulos fora do Spring. | Injetar pelo construtor anotado com `@Autowired` e declarar os campos `final`; o Mockito continua injetando os mocks nos testes. |

## Parte 3 — Testes novos: regras sem cobertura

Os seis testes foram adicionados em classes novas, sem editar nenhuma das sete classes de teste recebidas. Cada um tem seu próprio commit `test: testeNN`, segue AAA e cobre um comportamento do contrato. As verificações dos três portes pertencem à mesma regra de preço.

| # | Teste escrito (classe.método) | Regra coberta | Resultado ao escrever |
|---|---|---|---|
| teste01 | `BanhoContratoTest.deveCobrarPrecoDoBanhoQuandoPorteForPequenoMedioOuGrande` | Banho custa R$ 60,00 / R$ 80,00 / R$ 100,00 conforme o porte. | Vermelho: revelou o bug08, com os preços pequeno e grande invertidos. |
| teste02 | `TosaContratoTest.deveDurar60MinutosQuandoAtendimentoForTosa` | Tosa dura 60 minutos, inclusive pelo tipo abstrato `Atendimento`. | Vermelho: revelou o bug09; a chamada devolvia os 30 minutos herdados. |
| teste03 | `AgendaCancelamentoContratoTest.deveRecusarCancelamentoQuandoAtendimentoJaEstiverConcluido` | Atendimento concluído não pode ser cancelado nem salvo com outro status. | Vermelho: revelou o bug10; usa repository mockado e verifica que `save()` não é chamado. |
| teste04 | `AgendaAgendamentoContratoTest.deveRecusarAgendamentoQuandoDataHoraEstiverNoPassado` | Data passada deve ser recusada antes de consultar ou salvar no banco. | Vermelho: revelou o bug11; `verifyNoInteractions(repository)` protege a validação antecipada. |
| teste05 | `ConsultaVeterinariaContratoTest.deveCobrar150ReaisQuandoConsultaTiverQualquerPorte` | Consulta custa R$ 150,00 independentemente do porte. | Verde de primeira: a regra de preço fixo já estava correta. |
| teste06 | `AtendimentoStatusContratoTest.deveRecusarConclusaoQuandoAtendimentoEstiverCancelado` | Não é permitido concluir um atendimento cancelado, e seu estado deve ser preservado. | Verde de primeira: `concluir()` já recusava estados diferentes de `AGENDADO`. |

## Parte 4 — Perguntas de reflexão

### 1. A suíte como contrato

Executei a suíte original e confirmei os 20 testes, com exatamente 9 falhas.  
No Builder, `expected: <Rex> but was: <null>` levou à atribuição `petNome = petNome`, que não preenchia o atributo.  
Na Factory, a diferença entre a classe esperada `Tosa` e a classe recebida `Banho` apontou diretamente para o case errado.  
O conflito de horário mostrava uma `NullPointerException`, mas a causa estava antes dela: o `==` deixava a tentativa chegar ao `save()`.  
Corrigi essas causas no código de produção e rodei a suíte após cada alteração, mantendo os testes recebidos intactos.  
Os testes automatizam as expectativas e verificam exceções e interações com o repository; com curl, seria preciso montar e conferir manualmente cada cenário novamente.  
A verificação da API com H2 complementou a suíte e revelou o problema de ID que os mocks não poderiam reproduzir.

### 2. Mock e injeção de dependência

Em produção, o Spring cria a implementação de `AtendimentoRepository` e entrega essa dependência ao `AgendaService`.  
O código recebido fazia isso por um campo com `@Autowired`; na versão final, a anotação está no construtor e o campo é `final`.  
Nos testes, `@ExtendWith(MockitoExtension.class)` inicializa o Mockito, e `@Mock` cria um repository falso.  
O `@InjectMocks` constrói o serviço real usando esse mock, portanto as regras de agenda continuam sendo executadas de verdade.  
Em `AgendaServiceTest`, `when(repository.findById(99L)).thenReturn(Optional.empty())` prepara a ausência de um atendimento sem consultar o Oracle.  
Em `AgendaAgendamentoContratoTest`, `verifyNoInteractions(repository)` prova que a data passada é recusada antes de acessar a persistência.  
Como não há contexto Spring nem implementação real do repository nesses testes, eles não precisam de conexão, senha ou servidor.

### 3. `==` versus `.equals()`

O `==` verifica se duas referências apontam para o mesmo objeto; ele não compara automaticamente o conteúdo de objetos.  
O código da agenda usava esse operador tanto no nome do pet quanto em `LocalDateTime`, então valores iguais podiam escapar da regra.  
Literais como `"Rex"` podem compartilhar a mesma referência pelo pool de Strings, dando a impressão de que a comparação está correta.  
Uma String criada de outra forma não precisa compartilhar essa referência, mesmo que também contenha `Rex`.  
O teste original já recriava o horário com `LocalDateTime.parse(...)`: mesmo valor, outra instância, expondo o problema.  
Troquei as duas comparações por `.equals()`, mantendo a condição de que apenas um atendimento `AGENDADO` bloqueia o horário.  
Assim, o conflito é detectado pelo valor dos dados e a tentativa é recusada sem salvar uma duplicata.

### 4. Sobrescrita versus sobrecarga

Em `Atendimento`, o método definido era `getDuracaoMinutos()`, sem parâmetros, com duração padrão de 30 minutos.  
Em `Tosa`, havia `getDuracaoMinutos(String porte)`, que retornava 60, mas tinha uma assinatura diferente.  
Isso é sobrecarga: os dois métodos coexistem, e a chamada sem argumento continua usando a implementação herdada.  
Por isso, o resumo do atendimento informava 30 minutos mesmo quando o objeto real era uma tosa.  
A sobrescrita exige a mesma assinatura; corrigi o método da subclasse para `getDuracaoMinutos()` e acrescentei `@Override`.  
Se `@Override` estivesse no método com parâmetro desde o início, o compilador avisaria que ele não sobrescrevia nenhum método da superclasse.  
O novo teste usa uma referência `Atendimento` para uma instância de `Tosa` e verifica os 60 minutos, protegendo o uso polimórfico da API.

### 5. Singleton manual versus bean do Spring

O `GeradorProtocolo` precisa compartilhar a mesma instância para que todos os pedidos de protocolo avancem um único contador.  
O construtor já era privado, mas `getInstancia()` criava e devolvia um objeto sem guardá-lo, reiniciando o contador a cada chamada.  
Passei a atribuir o objeto ao campo estático `instancia`; os testes passaram a confirmar a identidade e a sequência `1, 2, 3`.  
Também sincronizei a obtenção da instância e o incremento, evitando que requisições concorrentes criem instâncias ou números repetidos.  
Já `AgendaService`, anotado com `@Service`, tem sua instância criada e reutilizada pelo container Spring no escopo singleton padrão.  
Essa garantia é por contexto Spring e não torna qualquer estado mutável automaticamente seguro para concorrência.  
O gerador manual mantém sua sequência apenas durante a execução dessa aplicação; reiniciar o processo ou executar várias instâncias exigiria outra estratégia para uma numeração persistente global.

### 6. Cobertura de testes: onde parar?

Os seis testes acrescentados tiveram quatro resultados vermelhos e dois verdes de primeira, mas todos têm uma regra concreta para proteger.  
O teste do preço fixo da consulta evita que uma alteração futura introduza cobrança por porte onde o contrato exige R$ 150,00.  
O teste de conclusão de cancelado evita que a validação de `concluir()` seja relaxada e permita uma transição proibida.  
Com prazo limitado, eu priorizaria as regras centrais: preços, conflitos de agenda, estados e validações que impedem persistência inválida.  
Manteria os caminhos felizes e os erros relevantes, incluindo a prova de que o repository não é acionado em situações recusadas.  
Não trataria 100% de cobertura de linhas como garantia de correção, pois uma asserção fraca pode executar código sem conferir seu comportamento.  
O bug12 mostrou o limite dos testes unitários: mesmo com todos verdes, foi necessário verificar a integração real entre API, JPA e banco.

## Parte 5 — Execução e verificação

### Como executar os testes

Requisitos: JDK 17 ou superior e Maven instalado. No Eclipse, importar a raiz como **Maven → Existing Maven Projects** e executar **Run As → JUnit Test** na pasta `src/test/java`.

```sh
mvn test
mvn verify
```

A primeira execução do Maven pode baixar dependências. Depois de resolvidas, os testes unitários executam sem banco, sem rede e sem subir o Spring.

### Como executar a API sem Oracle

O H2 já fazia parte das dependências recebidas. Depois de `mvn verify`, este comando funciona no PowerShell sem editar `application.properties`:

```powershell
java -jar target/petfiap-0.0.1-SNAPSHOT.jar --server.port=8080 '--spring.datasource.url=jdbc:h2:mem:petfiap' --spring.datasource.driver-class-name=org.h2.Driver --spring.datasource.username=sa '--spring.datasource.password=' --spring.jpa.hibernate.ddl-auto=create-drop
```

O banco dessa execução é temporário e seus dados não são mantidos após encerrar a aplicação. Para Oracle, fornecer as credenciais localmente por `SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD`. O arquivo versionado continua com `SEU_RM` e `SUA_SENHA`.

Exemplo de agendamento com uma data futura calculada no momento da execução:

```powershell
$horario = (Get-Date).AddDays(1).ToString('yyyy-MM-ddTHH:mm')
$atendimento = Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/atendimentos?tipo=BANHO&petNome=Rex&porte=PEQUENO&tutorNome=Ana&dataHora=$horario"
Invoke-RestMethod -Uri "http://localhost:8080/api/atendimentos/$($atendimento.id)/resumo"
```

### Evidências da entrega

Verificação realizada com JDK 21.0.11 e Maven 3.9.15:

| Verificação | Resultado |
|---|---|
| Estado original | 20 testes, 9 falhas, 0 erros |
| Estado final — `mvn test` | 38 testes, 0 falhas, 0 erros, 0 ignorados |
| SHA-256 das sete classes de teste originais | Idênticos aos arquivos recebidos |
| `pom.xml` e `application.properties` versionados | Preservados como recebidos; as credenciais locais não fazem parte dos commits |
| Histórico do enunciado | Primeiro commit do estado original; 12 commits `fix`, 6 `refactor` e 6 `test`, um por item |
| Histórico das validações adicionais | 2 commits `fix` e 2 commits `test`, separados por caso |
| API com H2 em memória | Inicialização e persistência reais verificadas na porta 18085 |
| Nove criações válidas: três serviços × três portes | HTTP 201, IDs gerados, status `AGENDADO` e protocolos sequenciais |
| Resumos dos nove atendimentos | Preços, pontos e durações conforme o contrato |
| Pet com horário ocupado | HTTP 409, sem inclusão de duplicata |
| Conclusão e cancelamento | HTTP 200 nas transições permitidas; HTTP 409 nas proibidas e repetidas |
| ID inexistente | HTTP 404 na busca, no resumo, na conclusão e no cancelamento |
| Data passada, tipo inexistente, nome/porte vazios e data malformada | HTTP 400 |
| API com Oracle | 60 verificações do contrato passaram; os 11 registros temporários da verificação foram removidos |
| Novas validações na API com H2 | 48 verificações HTTP passaram: 27 entradas inválidas recusadas sem persistência e 9 criações válidas preservadas |

A API foi verificada com H2 e com Oracle. As duas validações adicionais foram verificadas na nova versão usando H2 em memória. A validação de data passada ficou em `AgendaService.agendar()`, onde o contrato exige recusa antes de acessar o banco, permitindo construir e consultar registros históricos.

O histórico permite observar a evolução: as sete primeiras correções eliminaram as nove falhas originais; quatro testes novos revelaram quatro outros bugs; a revisão do mapeamento JPA e a chamada real à API confirmaram o bug12. Os dois testes novos que passaram de primeira foram mantidos para prevenir regressões.

### Validações adicionais encontradas na API

Além dos 12 bugs do enunciado, a verificação com Oracle mostrou duas entradas indevidas que recebiam HTTP 201. As validações ficam em `AtendimentoBuilder.construir()`, antes da Factory e da persistência; o controller já converte `IllegalArgumentException` em HTTP 400.

| # | Sintoma e causa raiz | Correção | Testes de regressão |
|---|---|---|---|
| bug13 | Um porte como `GIGANTE` era aceito, pois o Builder só verificava ausência e as subclasses tratavam qualquer outro valor como porte grande. | Validar o porte contra o conjunto imutável `PEQUENO`, `MEDIO` e `GRANDE`; recusar qualquer outro valor com mensagem clara. | `AtendimentoBuilderPorteTest`: quatro valores não reconhecidos recusados e os três portes permitidos preservados. |
| bug14 | Nome de tutor vazio era salvo, pois `construir()` não validava `tutorNome`. | Recusar tutor nulo, vazio ou composto apenas por espaços em branco antes de criar o atendimento. | `AtendimentoBuilderTutorTest`: tutor nulo, vazio, com espaços, tabulação ou quebra de linha recusado. |

Os testes adicionais seguem AAA e foram executados antes das correções: os quatro cenários de porte inválido e os cinco de tutor inválido falharam inicialmente. Depois das correções, os 12 novos cenários e os 26 testes anteriores passaram, totalizando 38 execuções. Os 20 testes recebidos permanecem intactos.

A verificação HTTP da nova versão cobriu os dois casos em `BANHO`, `TOSA` e `CONSULTA`: entradas inválidas recebem 400 e não geram registros; os três portes válidos com tutor preenchido continuam sendo aceitos. Para carregar as alterações na execução aberta no IntelliJ, reiniciar `PetFiapApplication`.
