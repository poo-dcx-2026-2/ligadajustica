package br.ufpb.dcx.poo.biblioteca.extensao.quadrinhos;

import java.util.function.Function;

import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoNaoEncontradoException;
import br.ufpb.dcx.poo.biblioteca.dominio.Item;
import br.ufpb.dcx.poo.biblioteca.dominio.Usuario;

/** Casos de uso próprios do acervo de quadrinhos, fora do contrato congelado. */
public final class QuadrinhosService {
    private final Function<String, Item> buscarItem;
    private final Function<String, Usuario> buscarUsuario;

    public QuadrinhosService(Function<String, Item> buscarItem,
                             Function<String, Usuario> buscarUsuario) {
        this.buscarItem = buscarItem;
        this.buscarUsuario = buscarUsuario;
    }

    public void definirClassificacaoIndicativa(String codigoDoItem, int idadeMinima)
            throws RecursoNaoEncontradoException {
        exigirTexto(codigoDoItem, "codigoDoItem");
        Item item = buscarItem.apply(codigoDoItem);
        if (item == null) throw new RecursoNaoEncontradoException(
                "Item não encontrado: " + codigoDoItem);
        item.definirClassificacaoIndicativa(idadeMinima);
    }

    public void definirIdadeUsuario(String matricula, int idade)
            throws RecursoNaoEncontradoException {
        exigirTexto(matricula, "matricula");
        Usuario usuario = buscarUsuario.apply(matricula);
        if (usuario == null) throw new RecursoNaoEncontradoException(
                "Usuário não encontrado: " + matricula);
        usuario.definirIdade(idade);
    }

    private static void exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new DadosInvalidosException("O campo " + campo + " é obrigatório.");
        }
    }
}
