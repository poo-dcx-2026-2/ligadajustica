package br.ufpb.dcx.poo.biblioteca.dominio;

import java.time.LocalDate;

import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException;

/** Reserva identificável que pode aguardar, receber um exemplar ou ser encerrada. */
public final class Reserva {
    public enum Situacao { ATIVA, ATENDIDA, CANCELADA }

    private final String id;
    private final Item item;
    private final Usuario usuario;
    private final LocalDate dataDaReserva;
    private Situacao situacao = Situacao.ATIVA;
    private String tomboSeparado;

    public Reserva(String id, Item item, Usuario usuario, LocalDate dataDaReserva) {
        if (id == null || id.isBlank() || item == null || usuario == null || dataDaReserva == null) {
            throw new DadosInvalidosException("Reserva exige id, item, usuário e data.");
        }
        this.id = id;
        this.item = item;
        this.usuario = usuario;
        this.dataDaReserva = dataDaReserva;
    }

    public String getId() { return id; }
    public Item getItem() { return item; }
    public Usuario getUsuario() { return usuario; }
    public LocalDate getDataDaReserva() { return dataDaReserva; }
    public Situacao getSituacao() { return situacao; }
    public String getTomboSeparado() { return tomboSeparado; }
    public boolean isAtiva() { return situacao == Situacao.ATIVA; }

    public void separarExemplar(String tombo) {
        if (!isAtiva() || tombo == null || tombo.isBlank()) {
            throw new IllegalStateException("A reserva não pode receber esse exemplar.");
        }
        tomboSeparado = tombo;
    }

    public void liberarExemplar() { tomboSeparado = null; }

    public void atender() {
        if (!isAtiva()) throw new IllegalStateException("A reserva não está ativa.");
        situacao = Situacao.ATENDIDA;
        tomboSeparado = null;
    }

    public void cancelar() {
        if (!isAtiva()) throw new IllegalStateException("A reserva não está ativa.");
        situacao = Situacao.CANCELADA;
        tomboSeparado = null;
    }

    public void restaurarEstado(Situacao situacao, String tomboSeparado) {
        if (situacao == null || (situacao != Situacao.ATIVA && tomboSeparado != null)) {
            throw new DadosInvalidosException("Estado de reserva inconsistente.");
        }
        this.situacao = situacao;
        this.tomboSeparado = tomboSeparado;
    }
}
