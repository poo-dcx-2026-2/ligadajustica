package br.ufpb.dcx.poo.biblioteca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.ufpb.dcx.poo.biblioteca.contrato.Biblioteca;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusExemplar;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.PersistenciaException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoNaoEncontradoException;
import br.ufpb.dcx.poo.biblioteca.extensao.quadrinhos.BibliotecaQuadrinhos;

class PersistenciaBibliotecaTest {
    @TempDir
    Path diretorio;

    private String propriedadeAnterior;

    @BeforeEach
    void configurarDiretorio() {
        propriedadeAnterior = System.getProperty("biblioteca.dados");
        System.setProperty("biblioteca.dados", diretorio.toString());
    }

    @AfterEach
    void restaurarPropriedade() {
        if (propriedadeAnterior == null) System.clearProperty("biblioteca.dados");
        else System.setProperty("biblioteca.dados", propriedadeAnterior);
    }

    @Test
    @DisplayName("salvar e carregar preservam catálogo, usuários, empréstimos e fila")
    void idaEVolta() throws Exception {
        Biblioteca origem = Fabrica.novaBiblioteca();
        origem.acervo().cadastrarItem("HQ", "Liga", "DC", "Quadrinhos", 2020);
        origem.acervo().adicionarExemplar("HQ", "T1");
        origem.acervo().adicionarExemplar("HQ", "T2");
        origem.usuarios().cadastrarUsuario("U1", "Ana");
        origem.usuarios().cadastrarUsuario("U2", "Bia");
        origem.usuarios().cadastrarUsuario("U3", "Caio");
        origem.usuarios().cadastrarUsuario("U4", "Duda");
        BibliotecaQuadrinhos extensaoOrigem = (BibliotecaQuadrinhos) origem;
        extensaoOrigem.quadrinhos().definirClassificacaoIndicativa("HQ", 16);
        extensaoOrigem.quadrinhos().definirIdadeUsuario("U1", 18);
        extensaoOrigem.quadrinhos().definirIdadeUsuario("U2", 18);
        extensaoOrigem.quadrinhos().definirIdadeUsuario("U3", 16);
        LocalDate dia = LocalDate.of(2026, 10, 1);
        String emprestimoDevolvido = origem.emprestimos().emprestar("T1", "U1", dia);
        String emprestimoAtivo = origem.emprestimos().emprestar("T2", "U2", dia.plusDays(1));
        String reservaSeparada = origem.emprestimos().reservar("HQ", "U3", dia.plusDays(3));
        String reservaCancelada = origem.emprestimos().reservar("HQ", "U4", dia.plusDays(3));
        origem.emprestimos().devolver("T1", dia.plusDays(4));
        origem.usuarios().desativarUsuario("U4");
        origem.salvar();

        Biblioteca carregada = Fabrica.novaBiblioteca();
        carregada.acervo().cadastrarItem("RESIDUO", "Não deve ficar", "X", "X", 0);
        carregada.carregar();

        assertEquals(origem.acervo().listarItens(), carregada.acervo().listarItens());
        assertEquals(origem.acervo().listarExemplares("HQ"), carregada.acervo().listarExemplares("HQ"));
        assertEquals(origem.usuarios().listarUsuarios(), carregada.usuarios().listarUsuarios());
        assertEquals(origem.emprestimos().listarEmprestimosDoUsuario("U1"),
                carregada.emprestimos().listarEmprestimosDoUsuario("U1"));
        assertEquals(origem.emprestimos().listarEmprestimosDoUsuario("U2"),
                carregada.emprestimos().listarEmprestimosDoUsuario("U2"));
        assertEquals(reservaSeparada, carregada.emprestimos().listarReservas("HQ").get(0).id());
        assertEquals(StatusExemplar.RESERVADO,
                carregada.acervo().listarExemplares("HQ").stream()
                        .filter(e -> e.tombo().equals("T1")).findFirst().orElseThrow().status());
        assertEquals(false, carregada.usuarios().buscarUsuario("U4").ativo());
        assertEquals(1, carregada.usuarios().buscarUsuario("U2").emprestimosAtivos());
        assertEquals(0, carregada.usuarios().buscarUsuario("U1").emprestimosAtivos());
        assertThrows(RecursoNaoEncontradoException.class,
                () -> carregada.emprestimos().cancelarReserva(reservaCancelada));

        String novo = carregada.emprestimos().emprestar("T1", "U3", dia.plusDays(5));
        assertNotEquals(emprestimoDevolvido, novo);
        assertNotEquals(emprestimoAtivo, novo);
        assertNotEquals(reservaSeparada, novo);
    }

    @Test
    @DisplayName("arquivo ausente equivale a estado vazio e substitui os dados em memória")
    void arquivoAusenteLimpaEstado() throws Exception {
        Biblioteca biblioteca = Fabrica.novaBiblioteca();
        biblioteca.acervo().cadastrarItem("X", "Temporário", "A", "B", 1);
        biblioteca.usuarios().cadastrarUsuario("U", "Temporário");

        biblioteca.carregar();

        assertEquals(0, biblioteca.acervo().listarItens().size());
        assertEquals(0, biblioteca.usuarios().listarUsuarios().size());
    }

    @Test
    @DisplayName("arquivo corrompido lança PersistenciaException sem alterar o estado atual")
    void arquivoCorrompidoNaoSubstituiEstado() throws Exception {
        Biblioteca biblioteca = Fabrica.novaBiblioteca();
        biblioteca.acervo().cadastrarItem("ATUAL", "Preservado", "A", "B", 1);
        Files.writeString(diretorio.resolve("biblioteca.dat"),
                "BIBLIOTECA\t2\nI\tQQ\tQg\t~\t~\tNAO-E-ANO\t~\n");

        PersistenciaException erro = assertThrows(PersistenciaException.class, biblioteca::carregar);

        assertNotNull(erro.getCause());
        assertEquals("Preservado", biblioteca.acervo().buscarItem("ATUAL").titulo());
        assertEquals(1, biblioteca.acervo().listarItens().size());
    }

    @Test
    @DisplayName("falha de escrita vira PersistenciaException com a causa preservada")
    void falhaDeEscritaPreservaCausa() throws Exception {
        Path arquivoNoLugarDoDiretorio = diretorio.resolve("nao-e-diretorio");
        Files.writeString(arquivoNoLugarDoDiretorio, "arquivo");
        System.setProperty("biblioteca.dados", arquivoNoLugarDoDiretorio.toString());

        PersistenciaException erro = assertThrows(PersistenciaException.class,
                () -> Fabrica.novaBiblioteca().salvar());

        assertNotNull(erro.getCause());
    }
}
