package br.ufpb.dcx.poo.biblioteca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.ufpb.dcx.poo.biblioteca.contrato.Biblioteca;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusExemplar;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoNaoEncontradoException;
import br.ufpb.dcx.poo.biblioteca.extensao.quadrinhos.BibliotecaQuadrinhos;
import br.ufpb.dcx.poo.biblioteca.extensao.quadrinhos.ClassificacaoIndicativaException;

class QuadrinhosExtensionTest {
    @Test
    @DisplayName("empréstimo respeita a classificação indicativa e não deixa estado parcial")
    void verificaIdadeMinima() throws Exception {
        Biblioteca biblioteca = Fabrica.novaBiblioteca();
        BibliotecaQuadrinhos quadrinhos = (BibliotecaQuadrinhos) biblioteca;
        biblioteca.acervo().cadastrarItem("HQ", "Liga", "DC", "Quadrinhos", 2020);
        biblioteca.acervo().adicionarExemplar("HQ", "T1");
        biblioteca.usuarios().cadastrarUsuario("U1", "Ana");
        quadrinhos.quadrinhos().definirClassificacaoIndicativa("HQ", 14);
        quadrinhos.quadrinhos().definirIdadeUsuario("U1", 13);

        assertThrows(ClassificacaoIndicativaException.class,
                () -> biblioteca.emprestimos().emprestar("T1", "U1", LocalDate.of(2026, 10, 1)));
        assertEquals(StatusExemplar.DISPONIVEL,
                biblioteca.acervo().listarExemplares("HQ").get(0).status());
        assertEquals(0, biblioteca.emprestimos().listarEmprestimosDoUsuario("U1").size());

        quadrinhos.quadrinhos().definirIdadeUsuario("U1", 14);
        biblioteca.emprestimos().emprestar("T1", "U1", LocalDate.of(2026, 10, 1));
        assertEquals(1, biblioteca.usuarios().buscarUsuario("U1").emprestimosAtivos());
    }

    @Test
    @DisplayName("idade ausente é recusada somente para edição com classificação configurada")
    void idadeObrigatoriaQuandoHaClassificacao() throws Exception {
        Biblioteca biblioteca = Fabrica.novaBiblioteca();
        BibliotecaQuadrinhos quadrinhos = (BibliotecaQuadrinhos) biblioteca;
        biblioteca.acervo().cadastrarItem("HQ", "Liga", "DC", "Quadrinhos", 2020);
        biblioteca.acervo().adicionarExemplar("HQ", "T1");
        biblioteca.usuarios().cadastrarUsuario("U1", "Ana");
        quadrinhos.quadrinhos().definirClassificacaoIndicativa("HQ", 10);
        assertThrows(ClassificacaoIndicativaException.class,
                () -> biblioteca.emprestimos().emprestar("T1", "U1", LocalDate.of(2026, 10, 1)));
    }

    @Test
    @DisplayName("serviço autoral valida idade e referências sem alterar o contrato")
    void validaCadastroDaRegra() throws Exception {
        Biblioteca biblioteca = Fabrica.novaBiblioteca();
        BibliotecaQuadrinhos quadrinhos = (BibliotecaQuadrinhos) biblioteca;
        biblioteca.acervo().cadastrarItem("HQ", "Liga", "DC", "Quadrinhos", 2020);
        biblioteca.usuarios().cadastrarUsuario("U1", "Ana");

        assertThrows(DadosInvalidosException.class,
                () -> quadrinhos.quadrinhos().definirIdadeUsuario("U1", 121));
        assertThrows(DadosInvalidosException.class,
                () -> quadrinhos.quadrinhos().definirClassificacaoIndicativa("HQ", 19));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> quadrinhos.quadrinhos().definirIdadeUsuario("UX", 18));
        assertThrows(RecursoNaoEncontradoException.class,
                () -> quadrinhos.quadrinhos().definirClassificacaoIndicativa("HX", 12));
    }
}
