package br.ufpb.dcx.poo.biblioteca.inicial;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.ufpb.dcx.poo.biblioteca.contrato.StatusExemplar;
import br.ufpb.dcx.poo.biblioteca.dominio.Emprestimo;
import br.ufpb.dcx.poo.biblioteca.dominio.Exemplar;
import br.ufpb.dcx.poo.biblioteca.dominio.Item;
import br.ufpb.dcx.poo.biblioteca.dominio.Reserva;
import br.ufpb.dcx.poo.biblioteca.dominio.Usuario;

/** Estado compartilhado pelos serviços que a fachada conecta. */
final class ContextoBiblioteca {
    final Map<String, Item> itens = new LinkedHashMap<>();
    final Map<String, Exemplar> exemplares = new LinkedHashMap<>();
    final Map<String, Usuario> usuarios = new LinkedHashMap<>();
    final Map<String, Emprestimo> emprestimos = new LinkedHashMap<>();
    final Map<String, Reserva> reservas = new LinkedHashMap<>();

    List<Reserva> reservasAtivasDoItem(Item item) {
        List<Reserva> resultado = new ArrayList<>();
        for (Reserva reserva : reservas.values()) {
            if (reserva.isAtiva() && reserva.getItem() == item) resultado.add(reserva);
        }
        return resultado;
    }

    Reserva reservaDoExemplar(Exemplar exemplar) {
        for (Reserva reserva : reservas.values()) {
            if (reserva.isAtiva() && exemplar.getTombo().equals(reserva.getTomboSeparado())) {
                return reserva;
            }
        }
        return null;
    }

    Reserva proximaReservaAguardando(Item item, Exemplar exemplar) {
        for (Reserva reserva : reservas.values()) {
            if (reserva.isAtiva() && reserva.getItem() == item
                    && reserva.getTomboSeparado() == null) {
                reserva.separarExemplar(exemplar.getTombo());
                exemplar.setStatus(StatusExemplar.RESERVADO);
                return reserva;
            }
        }
        exemplar.setStatus(StatusExemplar.DISPONIVEL);
        return null;
    }

    void cancelarReservasDoUsuario(Usuario usuario) {
        for (Reserva reserva : new ArrayList<>(reservas.values())) {
            if (reserva.isAtiva() && reserva.getUsuario() == usuario) {
                String tombo = reserva.getTomboSeparado();
                reserva.cancelar();
                if (tombo != null) {
                    Exemplar exemplar = exemplares.get(tombo);
                    proximaReservaAguardando(reserva.getItem(), exemplar);
                }
            }
        }
    }

    void substituirPor(ContextoBiblioteca carregado) {
        itens.clear(); itens.putAll(carregado.itens);
        exemplares.clear(); exemplares.putAll(carregado.exemplares);
        usuarios.clear(); usuarios.putAll(carregado.usuarios);
        emprestimos.clear(); emprestimos.putAll(carregado.emprestimos);
        reservas.clear(); reservas.putAll(carregado.reservas);
    }
}
