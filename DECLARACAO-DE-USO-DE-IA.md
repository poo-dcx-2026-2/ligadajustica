# Declaração de uso de ferramentas de IA

Preencha e mantenha atualizado até a entrega final. A ausência da declaração, ou uma
declaração que não corresponde ao histórico do repositório, é tratada como problema de
autoria.

Usar IA não reduz a nota. Não conseguir explicar, testar e adaptar o que foi entregue,
sim.

## Equipe

| Nome                   | Matrícula   |
|------------------------|-------------|
| Eduardo Ferreira Grisi | 20250114025 |
| Guilherme da Silva Virginio| 20250144946|

Inclua uma linha para cada integrante da equipe.

## Uso declarado

Uma linha por uso relevante. Se não houve uso, escreva "Não houve uso de ferramentas de IA".

| Data | Ferramenta | Finalidade | Arquivos/trechos afetados | O que foi revisado e alterado por vocês |
|---|---|---|---|---|
| 2026-10-02 | OpenAI Codex | Modelar usuários por matrícula, implementar cadastro, busca e listagem, e ampliar testes de isolamento e casos inválidos | `src/main/java/br/ufpb/dcx/poo/biblioteca/dominio/Usuario.java`; `src/main/java/br/ufpb/dcx/poo/biblioteca/inicial/UsuariosEmMemoria.java`; `src/test/java/br/ufpb/dcx/poo/biblioteca/UsuarioTest.java`; `src/test/java/br/ufpb/dcx/poo/biblioteca/FabricaTest.java` |Codex executado mvn -B verify: 29 testes aprovados, sem falhas, erros ou testes ignorados. A equipe revisou manualmente o projeto e os testes; com apoio do Codex, as falhas identificadas foram corrigidas. A equipe conferiu as alterações e validou o resultado com os testes.
| 2026-11-13 | OpenAI Codex | Implementar serviços de empréstimo, devolução e reserva; baixa e situação de usuários; persistência segura; extensão de classificação etária; acrescentar testes e atualizar design | `src/main/java/br/ufpb/dcx/poo/biblioteca/dominio/`; `src/main/java/br/ufpb/dcx/poo/biblioteca/inicial/`; `src/main/java/br/ufpb/dcx/poo/biblioteca/extensao/quadrinhos/`; `src/test/java/br/ufpb/dcx/poo/biblioteca/Entrega2Test.java`; `PersistenciaBibliotecaTest.java`; `QuadrinhosExtensionTest.java`; `docs/decisoes.md`; `docs/modelo.puml`; `docs/modelo.png`; `README.md`; `.gitignore` | Codex implementou as mudanças e executou `mvn -B verify`: 53 testes aprovados, sem falhas, erros ou testes ignorados. A equipe ainda deve revisar e adaptar as alterações e registrar suas revisões por PR conforme a rubrica. |

## Compromisso

Ao entregar, a equipe declara que:

- entende cada trecho do código entregue e consegue explicá-lo oralmente;
- testou o que foi gerado, e não apenas verificou que compila;
- adaptou o que foi gerado ao contrato e às decisões de design do projeto;
- está ciente de que cada integrante fará uma alteração individual em sala, sem consulta,
  na defesa da Entrega 3.

Assinaturas (preencher por cada integrante, após revisão; nome, data e assinatura):
Eduardo Grisi 02/10/26
Guilherme Virginio 02/10/26
