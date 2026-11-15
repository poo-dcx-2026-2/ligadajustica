package br.ufpb.dcx.poo.biblioteca.dominio;

import java.time.LocalDate;
import java.time.DateTimeException;

import br.ufpb.dcx.poo.biblioteca.contrato.EmprestimoService;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusEmprestimo;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException;

/** Empréstimo com identidade, prazo e transição de devolução próprios. */
public final class Emprestimo {
    private final String id;
    private final Exemplar exemplar;
    private final Usuario usuario;
    private final LocalDate dataDoEmprestimo;
    private final LocalDate dataPrevistaDeDevolucao;
    private LocalDate dataDaDevolucao;

    public Emprestimo(String id, Exemplar exemplar, Usuario usuario, LocalDate data) {
        exigirTexto(id, "id");
        if (exemplar == null || usuario == null || data == null) {
            throw new DadosInvalidosException("Empréstimo exige exemplar, usuário e data.");
        }
        this.id = id;
        this.exemplar = exemplar;
        this.usuario = usuario;
        this.dataDoEmprestimo = data;
        try {
            this.dataPrevistaDeDevolucao = data.plusDays(EmprestimoService.PRAZO_EM_DIAS);
        } catch (DateTimeException e) {
            throw new DadosInvalidosException("A data do empréstimo não permite calcular o prazo.");
        }
    }

    public String getId() { return id; }
    public Exemplar getExemplar() { return exemplar; }
    public Usuario getUsuario() { return usuario; }
    public LocalDate getDataDoEmprestimo() { return dataDoEmprestimo; }
    public LocalDate getDataPrevistaDeDevolucao() { return dataPrevistaDeDevolucao; }
    public LocalDate getDataDaDevolucao() { return dataDaDevolucao; }
    public boolean isAtivo() { return dataDaDevolucao == null; }

    public StatusEmprestimo getStatus() {
        return isAtivo() ? StatusEmprestimo.ATIVO : StatusEmprestimo.DEVOLVIDO;
    }

    public void devolver(LocalDate data) {
        if (data == null) {
            throw new DadosInvalidosException("A data da devolução é obrigatória.");
        }
        if (!isAtivo()) {
            throw new IllegalStateException("O empréstimo já foi devolvido.");
        }
        if (data.isBefore(dataDoEmprestimo)) {
            throw new DadosInvalidosException("A devolução não pode anteceder o empréstimo.");
        }
        dataDaDevolucao = data;
    }

    private static void exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new DadosInvalidosException("O campo " + campo + " é obrigatório.");
        }
    }
}
