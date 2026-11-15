package br.ufpb.dcx.poo.biblioteca.dominio;

import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.OperacaoNaoPermitidaException;

public final class Usuario {
    private final String matricula;
    private final String nome;
    private boolean ativo = true;
    private int emprestimosAtivos;
    private Integer idade;

    public Usuario(String matricula, String nome) {
        exigirTexto(matricula, "matricula");
        exigirTexto(nome, "nome");
        this.matricula = matricula;
        this.nome = nome;
    }

    public String getMatricula() { return matricula; }
    public String getNome() { return nome; }
    public boolean isAtivo() { return ativo; }
    public int getEmprestimosAtivos() { return emprestimosAtivos; }
    public Integer getIdade() { return idade; }

    public void definirIdade(int idade) {
        if (idade < 0 || idade > 120) {
            throw new DadosInvalidosException("A idade deve estar entre 0 e 120 anos.");
        }
        this.idade = idade;
    }

    public void restaurarIdade(Integer idade) {
        if (idade == null) this.idade = null;
        else definirIdade(idade);
    }

    public void desativar() throws OperacaoNaoPermitidaException {
        if (emprestimosAtivos > 0) {
            throw new OperacaoNaoPermitidaException(
                    "Não é possível desativar usuário com empréstimos ativos.");
        }
        ativo = false;
    }

    public void reativar() { ativo = true; }

    public void registrarEmprestimo() { emprestimosAtivos++; }

    public void registrarDevolucao() {
        if (emprestimosAtivos <= 0) {
            throw new IllegalStateException("O usuário não possui empréstimo ativo.");
        }
        emprestimosAtivos--;
    }

    public void restaurarEstado(boolean ativo, int emprestimosAtivos) {
        if (emprestimosAtivos < 0) {
            throw new DadosInvalidosException("Quantidade de empréstimos inválida.");
        }
        this.ativo = ativo;
        this.emprestimosAtivos = emprestimosAtivos;
    }

    private static void exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new DadosInvalidosException("O campo " + campo + " é obrigatório.");
        }
    }
}
