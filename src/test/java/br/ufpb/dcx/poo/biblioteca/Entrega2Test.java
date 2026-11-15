package br.ufpb.dcx.poo.biblioteca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.ufpb.dcx.poo.biblioteca.contrato.Biblioteca;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusEmprestimo;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusExemplar;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.ExemplarIndisponivelException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.LimiteDeEmprestimosExcedidoException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.OperacaoNaoPermitidaException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoNaoEncontradoException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.UsuarioInativoException;

class Entrega2Test {
    private static final LocalDate DIA = LocalDate.of(2026, 10, 1);
    private Biblioteca biblioteca;

    @BeforeEach
    void preparar() throws Exception {
        biblioteca = Fabrica.novaBiblioteca();
        biblioteca.acervo().cadastrarItem("HQ1", "Liga", "DC", "Quadrinhos", 2020);
        biblioteca.acervo().adicionarExemplar("HQ1", "T1");
        biblioteca.usuarios().cadastrarUsuario("U1", "Ana");
        biblioteca.usuarios().cadastrarUsuario("U2", "Bia");
        biblioteca.usuarios().cadastrarUsuario("U3", "Caio");
    }

    @Test
    @DisplayName("busca por categoria ignora caixa e ordena pelo título")
    void buscaPorCategoria() throws Exception {
        biblioteca.acervo().cadastrarItem("HQ2", "aventura", "DC", "QUADRINHOS", 2021);
        biblioteca.acervo().cadastrarItem("L1", "Livro", "Autor", "romance", 2022);
        assertEquals(List.of("aventura", "Liga"), biblioteca.acervo()
                .buscarPorCategoria("quadrinhos").stream().map(v -> v.titulo()).toList());
        assertEquals(List.of(), biblioteca.acervo().buscarPorCategoria("ficção"));
        assertThrows(DadosInvalidosException.class, () -> biblioteca.acervo().buscarPorCategoria(" "));
    }

    @Test
    @DisplayName("baixa muda status e totais, e erros não alteram o acervo")
    void baixaDeExemplar() throws Exception {
        biblioteca.acervo().baixarExemplar("T1");
        assertEquals(StatusExemplar.INDISPONIVEL,
                biblioteca.acervo().listarExemplares("HQ1").get(0).status());
        assertEquals(1, biblioteca.acervo().buscarItem("HQ1").totalDeExemplares());
        assertEquals(0, biblioteca.acervo().buscarItem("HQ1").exemplaresDisponiveis());
        assertThrows(OperacaoNaoPermitidaException.class,
                () -> biblioteca.acervo().baixarExemplar("T1"));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.acervo().baixarExemplar("inexistente"));
        assertThrows(DadosInvalidosException.class, () -> biblioteca.acervo().baixarExemplar(" "));
    }

    @Test
    @DisplayName("exemplar emprestado não pode ser baixado")
    void naoBaixaExemplarEmprestado() throws Exception {
        biblioteca.emprestimos().emprestar("T1", "U1", DIA);
        assertThrows(OperacaoNaoPermitidaException.class,
                () -> biblioteca.acervo().baixarExemplar("T1"));
        assertEquals(StatusExemplar.EMPRESTADO,
                biblioteca.acervo().listarExemplares("HQ1").get(0).status());
        assertEquals(1, biblioteca.usuarios().buscarUsuario("U1").emprestimosAtivos());
    }

    @Test
    @DisplayName("empréstimo registra prazo, altera exemplar e acompanha devolução")
    void emprestarEDevolver() throws Exception {
        String id = biblioteca.emprestimos().emprestar("T1", "U1", DIA);
        var emprestimo = biblioteca.emprestimos().listarEmprestimosDoUsuario("U1").get(0);
        assertEquals(id, emprestimo.id());
        assertEquals(DIA.plusDays(14), emprestimo.dataPrevistaDeDevolucao());
        assertEquals(StatusEmprestimo.ATIVO, emprestimo.status());
        assertEquals(1, biblioteca.usuarios().buscarUsuario("U1").emprestimosAtivos());
        assertEquals(StatusExemplar.EMPRESTADO,
                biblioteca.acervo().listarExemplares("HQ1").get(0).status());

        biblioteca.emprestimos().devolver("T1", DIA.plusDays(3));
        emprestimo = biblioteca.emprestimos().listarEmprestimosDoUsuario("U1").get(0);
        assertEquals(StatusEmprestimo.DEVOLVIDO, emprestimo.status());
        assertEquals(DIA.plusDays(3), emprestimo.dataDaDevolucao());
        assertEquals(0, biblioteca.usuarios().buscarUsuario("U1").emprestimosAtivos());
        assertEquals(StatusExemplar.DISPONIVEL,
                biblioteca.acervo().listarExemplares("HQ1").get(0).status());
    }

    @Test
    @DisplayName("limite de empréstimos é três e falha não deixa resíduo")
    void limiteDeEmprestimos() throws Exception {
        biblioteca.acervo().adicionarExemplar("HQ1", "T2");
        biblioteca.acervo().adicionarExemplar("HQ1", "T3");
        biblioteca.acervo().adicionarExemplar("HQ1", "T4");
        biblioteca.emprestimos().emprestar("T1", "U1", DIA);
        biblioteca.emprestimos().emprestar("T2", "U1", DIA);
        biblioteca.emprestimos().emprestar("T3", "U1", DIA);
        assertThrows(LimiteDeEmprestimosExcedidoException.class,
                () -> biblioteca.emprestimos().emprestar("T4", "U1", DIA));
        assertEquals(3, biblioteca.usuarios().buscarUsuario("U1").emprestimosAtivos());
        assertEquals(StatusExemplar.DISPONIVEL,
                biblioteca.acervo().listarExemplares("HQ1").get(3).status());
    }

    @Test
    @DisplayName("valida entradas, existência, atividade e disponibilidade ao emprestar")
    void errosAoEmprestar() throws Exception {
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().emprestar(" ", "U1", DIA));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().emprestar("T1", null, DIA));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().emprestar("T1", "U1", null));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.emprestimos().emprestar("TX", "U1", DIA));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.emprestimos().emprestar("T1", "UX", DIA));
        biblioteca.usuarios().desativarUsuario("U1");
        assertThrows(UsuarioInativoException.class,
                () -> biblioteca.emprestimos().emprestar("T1", "U1", DIA));
        biblioteca.usuarios().reativarUsuario("U1");
        biblioteca.emprestimos().emprestar("T1", "U2", DIA);
        assertThrows(ExemplarIndisponivelException.class,
                () -> biblioteca.emprestimos().emprestar("T1", "U1", DIA));
    }

    @Test
    @DisplayName("devolução recusa ausência de empréstimo e data anterior")
    void errosAoDevolver() throws Exception {
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().devolver("T1", null));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.emprestimos().devolver("TX", DIA));
        assertThrows(OperacaoNaoPermitidaException.class,
                () -> biblioteca.emprestimos().devolver("T1", DIA));
        biblioteca.emprestimos().emprestar("T1", "U1", DIA);
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().devolver("T1", DIA.minusDays(1)));
        assertEquals(1, biblioteca.usuarios().buscarUsuario("U1").emprestimosAtivos());
    }

    @Test
    @DisplayName("atraso começa no dia seguinte ao vencimento")
    void consultaAtrasados() throws Exception {
        biblioteca.emprestimos().emprestar("T1", "U1", DIA);
        assertThrows(DadosInvalidosException.class, () -> biblioteca.emprestimos().listarAtrasados(null));
        assertEquals(0, biblioteca.emprestimos().listarAtrasados(DIA.plusDays(14)).size());
        var atrasados = biblioteca.emprestimos().listarAtrasados(DIA.plusDays(15));
        assertEquals(1, atrasados.size());
        assertEquals(StatusEmprestimo.ATRASADO, atrasados.get(0).status());
    }

    @Test
    @DisplayName("lista empréstimos do usuário por data e valida matrícula")
    void listaEmprestimos() throws Exception {
        biblioteca.acervo().adicionarExemplar("HQ1", "T2");
        biblioteca.emprestimos().emprestar("T1", "U1", DIA);
        biblioteca.emprestimos().emprestar("T2", "U1", DIA.plusDays(1));
        biblioteca.emprestimos().devolver("T1", DIA.plusDays(2));
        var lista = biblioteca.emprestimos().listarEmprestimosDoUsuario("U1");
        assertEquals(List.of("T2", "T1"), lista.stream().map(v -> v.tombo()).toList());
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().listarEmprestimosDoUsuario(" "));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.emprestimos().listarEmprestimosDoUsuario("UX"));
    }

    @Test
    @DisplayName("no empate de datas o empréstimo registrado por último aparece primeiro")
    void empateNaDataDeEmprestimo() throws Exception {
        biblioteca.acervo().adicionarExemplar("HQ1", "T2");
        biblioteca.emprestimos().emprestar("T1", "U1", DIA);
        biblioteca.emprestimos().emprestar("T2", "U1", DIA);
        assertEquals(List.of("T2", "T1"), biblioteca.emprestimos()
                .listarEmprestimosDoUsuario("U1").stream().map(v -> v.tombo()).toList());
    }

    @Test
    @DisplayName("reserva entra na fila e a devolução separa o exemplar para a mais antiga")
    void cicloDeReserva() throws Exception {
        biblioteca.emprestimos().emprestar("T1", "U1", DIA);
        String primeira = biblioteca.emprestimos().reservar("HQ1", "U2", DIA.plusDays(1));
        String segunda = biblioteca.emprestimos().reservar("HQ1", "U3", DIA.plusDays(1));
        assertEquals(List.of(1, 2), biblioteca.emprestimos().listarReservas("HQ1")
                .stream().map(v -> v.posicaoNaFila()).toList());
        biblioteca.emprestimos().devolver("T1", DIA.plusDays(2));
        assertEquals(StatusExemplar.RESERVADO,
                biblioteca.acervo().listarExemplares("HQ1").get(0).status());
        assertThrows(ExemplarIndisponivelException.class,
                () -> biblioteca.emprestimos().emprestar("T1", "U3", DIA.plusDays(2)));
        biblioteca.emprestimos().emprestar("T1", "U2", DIA.plusDays(2));
        assertEquals(List.of(segunda), biblioteca.emprestimos().listarReservas("HQ1")
                .stream().map(v -> v.id()).toList());
        biblioteca.emprestimos().devolver("T1", DIA.plusDays(3));
        biblioteca.emprestimos().emprestar("T1", "U3", DIA.plusDays(3));
        assertEquals(List.of(), biblioteca.emprestimos().listarReservas("HQ1"));
        assertNotEquals(primeira, segunda);
    }

    @Test
    @DisplayName("reserva só é aceita sem exemplar disponível e sem duplicar usuário na fila")
    void errosAoReservar() throws Exception {
        assertThrows(OperacaoNaoPermitidaException.class,
                () -> biblioteca.emprestimos().reservar("HQ1", "U1", DIA));
        biblioteca.emprestimos().emprestar("T1", "U1", DIA);
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().reservar(" ", "U2", DIA));
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().reservar("HQ1", "U2", null));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.emprestimos().reservar("HX", "U2", DIA));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.emprestimos().reservar("HQ1", "UX", DIA));
        biblioteca.usuarios().desativarUsuario("U3");
        assertThrows(UsuarioInativoException.class,
                () -> biblioteca.emprestimos().reservar("HQ1", "U3", DIA));
        String id = biblioteca.emprestimos().reservar("HQ1", "U2", DIA);
        assertThrows(OperacaoNaoPermitidaException.class,
                () -> biblioteca.emprestimos().reservar("HQ1", "U2", DIA));
        assertEquals(id, biblioteca.emprestimos().listarReservas("HQ1").get(0).id());
        assertThrows(DadosInvalidosException.class,
                () -> biblioteca.emprestimos().listarReservas(" "));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.emprestimos().listarReservas("HX"));
    }

    @Test
    @DisplayName("cancelar reserva liberada passa exemplar à próxima pessoa da fila")
    void cancelarReserva() throws Exception {
        biblioteca.emprestimos().emprestar("T1", "U1", DIA);
        String primeira = biblioteca.emprestimos().reservar("HQ1", "U2", DIA);
        String segunda = biblioteca.emprestimos().reservar("HQ1", "U3", DIA);
        biblioteca.emprestimos().devolver("T1", DIA.plusDays(1));
        biblioteca.emprestimos().cancelarReserva(primeira);
        assertEquals("U3", biblioteca.emprestimos().listarReservas("HQ1").get(0).matricula());
        assertEquals(StatusExemplar.RESERVADO,
                biblioteca.acervo().listarExemplares("HQ1").get(0).status());
        biblioteca.emprestimos().cancelarReserva(segunda);
        assertEquals(StatusExemplar.DISPONIVEL,
                biblioteca.acervo().listarExemplares("HQ1").get(0).status());
        assertThrows(DadosInvalidosException.class, () -> biblioteca.emprestimos().cancelarReserva(" "));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.emprestimos().cancelarReserva(primeira));
    }

    @Test
    @DisplayName("reserva atendida deixa de existir para cancelamento")
    void reservaAtendidaNaoPodeSerCancelada() throws Exception {
        biblioteca.emprestimos().emprestar("T1", "U1", DIA);
        String id = biblioteca.emprestimos().reservar("HQ1", "U2", DIA);
        biblioteca.emprestimos().devolver("T1", DIA.plusDays(1));
        biblioteca.emprestimos().emprestar("T1", "U2", DIA.plusDays(1));
        assertEquals(List.of(), biblioteca.emprestimos().listarReservas("HQ1"));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.emprestimos().cancelarReserva(id));
    }

    @Test
    @DisplayName("desativação impede empréstimos ativos e cancela reservas ao desativar")
    void desativacaoEReativacao() throws Exception {
        assertThrows(DadosInvalidosException.class, () -> biblioteca.usuarios().desativarUsuario(" "));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.usuarios().desativarUsuario("UX"));
        biblioteca.emprestimos().emprestar("T1", "U1", DIA);
        assertThrows(OperacaoNaoPermitidaException.class,
                () -> biblioteca.usuarios().desativarUsuario("U1"));
        biblioteca.emprestimos().devolver("T1", DIA.plusDays(1));
        biblioteca.usuarios().desativarUsuario("U1");
        biblioteca.usuarios().desativarUsuario("U1");
        assertThrows(DadosInvalidosException.class, () -> biblioteca.usuarios().reativarUsuario(" "));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> biblioteca.usuarios().reativarUsuario("UX"));
        biblioteca.usuarios().reativarUsuario("U1");
        assertEquals(true, biblioteca.usuarios().buscarUsuario("U1").ativo());
    }

    @Test
    @DisplayName("desativar cancela reserva separada e entrega o exemplar à próxima pessoa")
    void desativarPromoveFilaDeReservas() throws Exception {
        biblioteca.emprestimos().emprestar("T1", "U2", DIA);
        biblioteca.emprestimos().reservar("HQ1", "U1", DIA);
        String proxima = biblioteca.emprestimos().reservar("HQ1", "U3", DIA);
        biblioteca.emprestimos().devolver("T1", DIA.plusDays(1));

        biblioteca.usuarios().desativarUsuario("U1");

        assertEquals(1, biblioteca.emprestimos().listarReservas("HQ1").size());
        assertEquals(proxima, biblioteca.emprestimos().listarReservas("HQ1").get(0).id());
        assertEquals(StatusExemplar.RESERVADO,
                biblioteca.acervo().listarExemplares("HQ1").get(0).status());
    }

    @Test
    @DisplayName("baixa de exemplar reservado devolve a reserva ao início da fila")
    void baixarExemplarReservado() throws Exception {
        biblioteca.emprestimos().emprestar("T1", "U1", DIA);
        String reserva = biblioteca.emprestimos().reservar("HQ1", "U2", DIA);
        biblioteca.emprestimos().devolver("T1", DIA.plusDays(1));
        biblioteca.acervo().baixarExemplar("T1");
        assertEquals(StatusExemplar.INDISPONIVEL,
                biblioteca.acervo().listarExemplares("HQ1").get(0).status());
        assertEquals(reserva, biblioteca.emprestimos().listarReservas("HQ1").get(0).id());
    }
}
