package br.ufpb.dcx.poo.biblioteca.extensao.quadrinhos;

import br.ufpb.dcx.poo.biblioteca.contrato.Biblioteca;

/** Fachada ampliada da equipe; mantém intacto o contrato comum da disciplina. */
public interface BibliotecaQuadrinhos extends Biblioteca {
    QuadrinhosService quadrinhos();
}
