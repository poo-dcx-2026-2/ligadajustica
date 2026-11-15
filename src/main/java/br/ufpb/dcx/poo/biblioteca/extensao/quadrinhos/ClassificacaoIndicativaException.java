package br.ufpb.dcx.poo.biblioteca.extensao.quadrinhos;

import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.ExemplarIndisponivelException;

/** Empréstimo vedado porque a idade cadastrada não atende à classificação. */
public final class ClassificacaoIndicativaException extends ExemplarIndisponivelException {
    private static final long serialVersionUID = 1L;

    public ClassificacaoIndicativaException(String mensagem) { super(mensagem); }
}
