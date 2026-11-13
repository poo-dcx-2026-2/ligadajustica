package br.ufpb.dcx.poo.biblioteca.extensao.quadrinhos;

import br.ufpb.dcx.poo.biblioteca.dominio.Item;
import br.ufpb.dcx.poo.biblioteca.dominio.Usuario;

/** Regra autoral: quem não alcança a classificação não pode retirar a edição. */
public final class RegraClassificacaoEtaria {
    public void validar(Usuario usuario, Item item) throws ClassificacaoIndicativaException {
        Integer idadeMinima = item.getClassificacaoIndicativa();
        if (idadeMinima == null) return;
        Integer idade = usuario.getIdade();
        if (idade == null || idade < idadeMinima) {
            throw new ClassificacaoIndicativaException(
                    "A idade do usuário não atende à classificação indicativa do item.");
        }
    }
}
