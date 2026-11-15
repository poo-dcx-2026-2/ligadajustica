# Decisões de design — Entrega 2

## Composição em vez de herança

Um `Item` compõe seus `Exemplar`s: cada exemplar tem tombo, status e ciclo de vida próprios, mas não faz sentido existir sem o item ao qual pertence. Rejeitamos modelar `Exemplar` como subclasse de `Item`, pois um exemplar não é substituível por uma edição do catálogo e herdaria atributos que não identificam uma cópia física. `Emprestimo` e `Reserva` também mantêm referências aos objetos de domínio envolvidos, em vez de copiar seus dados para estruturas soltas.

## GRASP Information Expert: empréstimo

Antes, a operação ficaria responsável por calcular e validar todas as datas. Agora `Emprestimo` conhece a data de retirada, calcula o vencimento usando a constante do contrato e valida a devolução antes de alterar seu estado. Rejeitamos concentrar essa lógica em `EmprestimosEmMemoria`; a entidade tem os dados necessários, o que aumenta coesão e reduz acoplamento do serviço a detalhes de prazo.

## GRASP Information Expert: usuário

`Usuario` mantém a quantidade de empréstimos ativos e executa suas transições de ativação. A desativação consulta esse estado na própria entidade e impede a operação quando há empréstimo em aberto. Rejeitamos manter esse contador somente no serviço, pois isso espalharia atualização e validação entre cadastro, empréstimo, devolução e persistência. A operação que devolve um exemplar atualiza o usuário correspondente; o carregamento reconstrói o contador a partir dos empréstimos ativos.

## GRASP Controller e Facade

`BibliotecaInicial` é a fachada que implementa `Biblioteca`, monta os serviços e injeta neles um único `ContextoBiblioteca`. Os clientes começam por `Fabrica.novaBiblioteca()` e acessam os serviços pela fachada; não recebem os mapas internos nem as classes de persistência. Rejeitamos permitir que a camada de interface futura acesse entidades e coleções diretamente, porque isso ampliaria o acoplamento e dificultaria trocar a implementação em memória.

## GRASP Information Expert: fila de reservas

Cada `Reserva` possui identidade, situação e, quando atendida parcialmente pela fila, o tombo separado. O contexto mantém a ordem de registro e coordena a atribuição do exemplar mais antigo ainda aguardando. Rejeitamos representar reservas como pares de texto sem identidade, pois isso não permitiria atender, cancelar ou persistir corretamente cada reserva.

## Persistência separada do domínio

`PersistenciaBiblioteca` lê e grava registros em `dados/biblioteca.dat`; nenhuma entidade de domínio realiza E/S. O carregamento reconstrói primeiro um contexto temporário e só substitui o estado ativo quando todas as linhas e referências são válidas. A escrita usa arquivo temporário e troca atômica quando o sistema de arquivos permite. Rejeitamos atualizar as entidades conforme cada linha é lida, pois um arquivo corrompido deixaria a biblioteca parcialmente carregada.

## Reservas de usuário desativado

Ao desativar um usuário sem empréstimos ativos, cancelamos suas reservas ainda ativas. Se uma delas já tinha exemplar separado, a cópia passa à próxima reserva sem exemplar separado; se não houver outra, volta a disponível. Escolhemos cancelar para que uma reserva não bloqueie exemplares para uma conta que não pode emprestá-los. Rejeitamos manter essas reservas suspensas, o que exigiria uma regra adicional de retomada e ordenação quando o usuário fosse reativado.

## Extensão autoral de quadrinhos

Idade do usuário e classificação indicativa do item são metadados opcionais das entidades de domínio, configurados pelo serviço autoral em `extensao/quadrinhos`. `RegraClassificacaoEtaria` aplica a restrição antes de criar o empréstimo; quando o item tem classificação, idade ausente ou abaixo dela impede a retirada. O contrato congelado não ganha campos nem métodos, e itens sem classificação continuam usando as regras comuns. Essa extensão fica separada para que o núcleo possa ser testado contra o contrato e a regra própria possa ter seus próprios testes.
