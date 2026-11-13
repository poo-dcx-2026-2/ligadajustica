# Sistema de Gestão de Biblioteca/Acervo

Projeto incremental da disciplina de **Programação Orientada a Objetos** — DCX/CCAE/UFPB.

Este projeto implementa o contrato comum das Entregas 1 e 2: cadastro e consulta do acervo, empréstimos, devoluções, reservas e persistência local. Os relatórios continuam reservados para a Entrega 3.

---

## Começando

Você precisa de **JDK 21** e **Maven**. No IntelliJ IDEA, abra a pasta do projeto e ele reconhece o `pom.xml` sozinho.

```bash
mvn -B verify      # compila e roda os testes
mvn -B test        # só os testes
```

Se tudo estiver certo, o Maven termina com `BUILD SUCCESS` e nenhum teste falhando.

---

## Mapa do projeto

```
src/main/java/br/ufpb/dcx/poo/biblioteca/
├── contrato/          ← CONGELADO. Não edite nada aqui.
│   ├── Biblioteca.java              ponto único de acesso aos serviços
│   ├── AcervoService.java           itens e exemplares
│   ├── UsuarioService.java          usuários
│   ├── EmprestimoService.java       empréstimos, devoluções e reservas
│   ├── RelatorioService.java        consultas e importação em lote
│   ├── *View.java                   o que as consultas devolvem
│   ├── Status*.java                 estados de exemplar e de empréstimo
│   └── excecoes/                    a hierarquia de erros do sistema
│
├── Fabrica.java       ← nome e assinatura congelados; o corpo é seu
│
├── dominio/           ← entidades Item, Exemplar, Usuario, Emprestimo e Reserva
├── extensao/quadrinhos/ ← serviço e regra de classificação etária da equipe
│
└── inicial/           ← serviços em memória, fachada e persistência
    ├── AcervoEmMemoria.java
    ├── UsuariosEmMemoria.java
    ├── EmprestimosEmMemoria.java
    ├── PersistenciaBiblioteca.java
    ├── RelatoriosNaoImplementados.java     (Entrega 3)
    └── BibliotecaInicial.java

src/test/java/…       ← seus testes; comece pelos que já estão aqui
dados/                ← arquivos de exemplo
.github/workflows/    ← a integração contínua, já configurada
```

---

## As duas regras

**1. O pacote `contrato` é congelado.** Não renomeie, não altere assinaturas, não acrescente nem remova métodos. Os testes de correção são escritos contra esses tipos: se você mudar qualquer coisa ali, eles não compilam e a entrega não pode ser avaliada.

**2. `Fabrica.novaBiblioteca()` precisa continuar funcionando.** É por esse método que os testes obtêm o seu sistema. Você vai trocar o que ele devolve — não troque o nome, o pacote nem a assinatura. Cada chamada precisa devolver uma instância nova e independente.

Fora isso, **tudo é seu**. O pacote `inicial` não é modelo: é matéria-prima. Você pode reescrevê-lo inteiro.

---

## O que já funciona e o que falta

| Serviço | Situação |
|---|---|
| `AcervoService` | Cadastro, consultas por título e categoria, exemplares e baixa implementados. |
| `UsuarioService` | Cadastro, busca, listagem, desativação e reativação implementados. |
| `EmprestimoService` | Empréstimos, devoluções, consultas, reservas e cancelamentos implementados. |
| `RelatorioService` | Esqueleto. Entrega 3. |
| `Biblioteca.salvar/carregar` | Estado completo salvo e restaurado por classe de persistência separada. |

Os métodos de relatório continuam como esqueleto para a Entrega 3.

## Persistência

Os dados são gravados em `dados/biblioteca.dat`, no diretório de execução. Para testes ou outro local, defina a propriedade de sistema `biblioteca.dados` com o caminho do diretório. `carregar()` trata diretório/arquivo ausente como estado vazio e rejeita arquivo inválido sem substituir o estado atual. Os arquivos de execução ficam fora do Git.

## Modelo de domínio e decisões

O fonte PlantUML está em [`docs/modelo.puml`](docs/modelo.puml), e a imagem correspondente está em [`docs/modelo.png`](docs/modelo.png).
As decisões sobre composição, responsabilidades GRASP, fachada, persistência e reservas na desativação estão em [`docs/decisoes.md`](docs/decisoes.md).

---

## Justificativa das coleções

O acervo usa `Map<String, Item>` para localizar cada item pelo código sem percorrer todos os cadastros. Um segundo `Map<String, Exemplar>` indexa os tombos no acervo inteiro e permite recusar duplicidade entre itens diferentes. Os exemplares também pertencem ao seu `Item`, que mantém a relação e devolve uma cópia da lista.

Os usuários ficam em `Map<String, Usuario>`, porque a matrícula é sua identidade e as operações mais comuns buscam ou recusam duplicidade por matrícula. As consultas ordenadas constroem listas de `View` e não expõem os mapas internos.

## Relato do defeito encontrado

A busca inicial do item comparava códigos com `==`, que verifica se duas referências apontam para o mesmo objeto, em vez de comparar o texto. Reproduzimos o erro cadastrando o código `L1` e buscando com `new String("L1")`: apesar do mesmo conteúdo, a busca dizia que o item não existia. O teste de regressão `codigoComMesmoValorEmOutraInstancia` falhou antes da correção e passou depois que a busca passou a usar a chave do `Map`, que compara Strings pelo valor.

## Um aviso honesto

O ponto de partida continha decisões de design questionáveis e um defeito de comportamento não coberto pelos testes originais. O defeito já foi reproduzido, coberto por um teste de regressão e corrigido; o relato acima registra esse processo. A suíte verde continua sendo evidência dos cenários testados, não prova absoluta de ausência de defeitos.

---

## Extensão autoral

Sua equipe escolhe um acervo próprio: jogos, filmes, quadrinhos, instrumentos, obras locais, recursos de laboratório. A extensão vive **por fora** do contrato — novos tipos, novos serviços, novas regras — e precisa incluir ao menos **uma regra de negócio própria**, documentada abaixo, que os testes de correção não conhecem e que você demonstra na defesa.

### Nossa extensão

**Acervo:** quadrinhos da Liga da Justiça. **Regra própria:** um usuário só pode tomar emprestada uma edição se sua idade cadastrada for igual ou maior que a classificação indicativa daquela edição. A regra e o serviço autoral ficam em `extensao/quadrinhos`, fora do pacote congelado `contrato`. Use `BibliotecaQuadrinhos.quadrinhos()` para definir a classificação de um item e a idade de um usuário. Itens sem classificação configurada não recebem essa restrição; para item classificado, idade ausente ou abaixo do mínimo impede o empréstimo.

```java
import java.time.LocalDate;

import br.ufpb.dcx.poo.biblioteca.Fabrica;
import br.ufpb.dcx.poo.biblioteca.contrato.Biblioteca;
import br.ufpb.dcx.poo.biblioteca.extensao.quadrinhos.BibliotecaQuadrinhos;

Biblioteca biblioteca = Fabrica.novaBiblioteca();
BibliotecaQuadrinhos extensao = (BibliotecaQuadrinhos) biblioteca;
// Depois de cadastrar o item, o exemplar e o usuário:
extensao.quadrinhos().definirClassificacaoIndicativa("HQ-001", 14);
extensao.quadrinhos().definirIdadeUsuario("2026001", 16);
String id = biblioteca.emprestimos().emprestar("T-001", "2026001", LocalDate.of(2026, 11, 13));
```

---

## Uso de ferramentas de IA

O uso é permitido como apoio, desde que declarado em [`DECLARACAO-DE-USO-DE-IA.md`](DECLARACAO-DE-USO-DE-IA.md). Você continua responsável por explicar, testar e adaptar todo o código entregue — inclusive em uma alteração feita presencialmente, sem consulta, na defesa da Entrega 3.

---

## Equipe

| Nome | Matrícula | GitHub |
|---|---|---|
| | | |
| | | |
