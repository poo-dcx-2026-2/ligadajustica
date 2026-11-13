package br.ufpb.dcx.poo.biblioteca.inicial;

import br.ufpb.dcx.poo.biblioteca.contrato.AcervoService;
import br.ufpb.dcx.poo.biblioteca.contrato.Biblioteca;
import br.ufpb.dcx.poo.biblioteca.contrato.EmprestimoService;
import br.ufpb.dcx.poo.biblioteca.contrato.RelatorioService;
import br.ufpb.dcx.poo.biblioteca.contrato.UsuarioService;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.PersistenciaException;
import br.ufpb.dcx.poo.biblioteca.extensao.quadrinhos.BibliotecaQuadrinhos;
import br.ufpb.dcx.poo.biblioteca.extensao.quadrinhos.QuadrinhosService;
import br.ufpb.dcx.poo.biblioteca.extensao.quadrinhos.RegraClassificacaoEtaria;

/**
 * Reúne os quatro serviços em um único ponto de acesso.
 *
 * <p>É a fachada e o único ponto de montagem dos serviços. Cada serviço recebe o
 * mesmo contexto, sem expor suas coleções à interface cliente.</p>
 */
public class BibliotecaInicial implements BibliotecaQuadrinhos {

    private final ContextoBiblioteca contexto = new ContextoBiblioteca();
    private final RegraClassificacaoEtaria regraClassificacaoEtaria =
            new RegraClassificacaoEtaria();
    private final AcervoEmMemoria acervo = new AcervoEmMemoria(contexto);
    private final UsuariosEmMemoria usuarios = new UsuariosEmMemoria(contexto);
    private final EmprestimosEmMemoria emprestimos = new EmprestimosEmMemoria(
            contexto, regraClassificacaoEtaria);
    private final QuadrinhosService quadrinhos = new QuadrinhosService(
            contexto.itens::get, contexto.usuarios::get);
    private final RelatoriosNaoImplementados relatorios = new RelatoriosNaoImplementados();
    private final PersistenciaBiblioteca persistencia = new PersistenciaBiblioteca();

    @Override
    public AcervoService acervo() { return acervo; }

    @Override
    public UsuarioService usuarios() { return usuarios; }

    @Override
    public EmprestimoService emprestimos() { return emprestimos; }

    @Override
    public QuadrinhosService quadrinhos() { return quadrinhos; }

    @Override
    public RelatorioService relatorios() { return relatorios; }

    @Override
    public void salvar() throws PersistenciaException {
        persistencia.salvar(contexto);
    }

    @Override
    public void carregar() throws PersistenciaException {
        ContextoBiblioteca carregado = persistencia.carregar();
        contexto.substituirPor(carregado);
    }
}
