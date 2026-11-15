package br.ufpb.dcx.poo.biblioteca.inicial;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import br.ufpb.dcx.poo.biblioteca.contrato.EmprestimoService;
import br.ufpb.dcx.poo.biblioteca.contrato.EmprestimoView;
import br.ufpb.dcx.poo.biblioteca.contrato.ReservaView;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusEmprestimo;
import br.ufpb.dcx.poo.biblioteca.contrato.StatusExemplar;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.DadosInvalidosException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.ExemplarIndisponivelException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.LimiteDeEmprestimosExcedidoException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.OperacaoNaoPermitidaException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoNaoEncontradoException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.UsuarioInativoException;
import br.ufpb.dcx.poo.biblioteca.dominio.Emprestimo;
import br.ufpb.dcx.poo.biblioteca.dominio.Exemplar;
import br.ufpb.dcx.poo.biblioteca.dominio.Item;
import br.ufpb.dcx.poo.biblioteca.dominio.Reserva;
import br.ufpb.dcx.poo.biblioteca.dominio.Usuario;
import br.ufpb.dcx.poo.biblioteca.extensao.quadrinhos.RegraClassificacaoEtaria;

/** Coordena as operações entre objetos do domínio e mantém a ordem das filas. */
public final class EmprestimosEmMemoria implements EmprestimoService {
    private final ContextoBiblioteca contexto;
    private final RegraClassificacaoEtaria regraClassificacaoEtaria;

    public EmprestimosEmMemoria() {
        this(new ContextoBiblioteca(), new RegraClassificacaoEtaria());
    }

    EmprestimosEmMemoria(ContextoBiblioteca contexto) {
        this(contexto, new RegraClassificacaoEtaria());
    }

    EmprestimosEmMemoria(ContextoBiblioteca contexto,
                         RegraClassificacaoEtaria regraClassificacaoEtaria) {
        this.contexto = contexto;
        this.regraClassificacaoEtaria = regraClassificacaoEtaria;
    }

    @Override
    public String emprestar(String tombo, String matricula, LocalDate data)
            throws RecursoNaoEncontradoException, ExemplarIndisponivelException,
                   UsuarioInativoException, LimiteDeEmprestimosExcedidoException {
        exigirTexto(tombo, "tombo");
        exigirTexto(matricula, "matricula");
        if (data == null) throw new DadosInvalidosException("A data do empréstimo é obrigatória.");

        Exemplar exemplar = contexto.exemplares.get(tombo);
        if (exemplar == null) {
            throw new RecursoNaoEncontradoException("Exemplar não encontrado: " + tombo);
        }
        Usuario usuario = contexto.usuarios.get(matricula);
        if (usuario == null) {
            throw new RecursoNaoEncontradoException("Usuário não encontrado: " + matricula);
        }
        if (!usuario.isAtivo()) {
            throw new UsuarioInativoException("Usuário inativo: " + matricula);
        }

        Reserva reservaAtendida = null;
        if (exemplar.getStatus() == StatusExemplar.RESERVADO) {
            reservaAtendida = contexto.reservaDoExemplar(exemplar);
            if (reservaAtendida == null || reservaAtendida.getUsuario() != usuario) {
                throw new ExemplarIndisponivelException("Exemplar reservado para outro usuário.");
            }
        } else if (exemplar.getStatus() != StatusExemplar.DISPONIVEL) {
            throw new ExemplarIndisponivelException("Exemplar indisponível: " + tombo);
        }
        if (usuario.getEmprestimosAtivos() >= LIMITE_DE_EMPRESTIMOS) {
            throw new LimiteDeEmprestimosExcedidoException(
                    "Usuário já atingiu o limite de empréstimos ativos.");
        }
        regraClassificacaoEtaria.validar(usuario, exemplar.getItem());

        String id = novoId("E-");
        Emprestimo emprestimo = new Emprestimo(id, exemplar, usuario, data);
        if (reservaAtendida != null) reservaAtendida.atender();
        exemplar.setStatus(StatusExemplar.EMPRESTADO);
        usuario.registrarEmprestimo();
        contexto.emprestimos.put(id, emprestimo);
        return id;
    }

    @Override
    public void devolver(String tombo, LocalDate data)
            throws RecursoNaoEncontradoException, OperacaoNaoPermitidaException {
        exigirTexto(tombo, "tombo");
        if (data == null) throw new DadosInvalidosException("A data da devolução é obrigatória.");
        Exemplar exemplar = contexto.exemplares.get(tombo);
        if (exemplar == null) {
            throw new RecursoNaoEncontradoException("Exemplar não encontrado: " + tombo);
        }
        Emprestimo ativo = null;
        for (Emprestimo candidato : contexto.emprestimos.values()) {
            if (candidato.getExemplar() == exemplar && candidato.isAtivo()) {
                ativo = candidato;
                break;
            }
        }
        if (ativo == null) {
            throw new OperacaoNaoPermitidaException("O exemplar não possui empréstimo ativo.");
        }
        ativo.devolver(data); // valida a data antes de qualquer alteração de estado
        ativo.getUsuario().registrarDevolucao();
        contexto.proximaReservaAguardando(exemplar.getItem(), exemplar);
    }

    @Override
    public List<EmprestimoView> listarEmprestimosDoUsuario(String matricula)
            throws RecursoNaoEncontradoException {
        exigirTexto(matricula, "matricula");
        Usuario usuario = contexto.usuarios.get(matricula);
        if (usuario == null) {
            throw new RecursoNaoEncontradoException("Usuário não encontrado: " + matricula);
        }
        List<Emprestimo> encontrados = new ArrayList<>();
        for (Emprestimo emprestimo : contexto.emprestimos.values()) {
            if (emprestimo.getUsuario() == usuario) encontrados.add(emprestimo);
        }
        encontrados.sort(Comparator.comparing(Emprestimo::getDataDoEmprestimo).reversed());
        // LinkedHashMap mantém a ordem de registro; inverte-se cada grupo de datas iguais.
        for (int inicio = 0; inicio < encontrados.size();) {
            int fim = inicio + 1;
            while (fim < encontrados.size() && encontrados.get(inicio).getDataDoEmprestimo()
                    .equals(encontrados.get(fim).getDataDoEmprestimo())) fim++;
            java.util.Collections.reverse(encontrados.subList(inicio, fim));
            inicio = fim;
        }
        List<EmprestimoView> resultado = new ArrayList<>();
        for (Emprestimo emprestimo : encontrados) resultado.add(paraView(emprestimo, false));
        return resultado;
    }

    @Override
    public List<EmprestimoView> listarAtrasados(LocalDate data) {
        if (data == null) throw new DadosInvalidosException("A data de consulta é obrigatória.");
        List<Emprestimo> encontrados = new ArrayList<>();
        for (Emprestimo emprestimo : contexto.emprestimos.values()) {
            if (emprestimo.isAtivo()
                    && emprestimo.getDataPrevistaDeDevolucao().isBefore(data)) {
                encontrados.add(emprestimo);
            }
        }
        encontrados.sort(Comparator.comparing(Emprestimo::getDataPrevistaDeDevolucao));
        List<EmprestimoView> resultado = new ArrayList<>();
        for (Emprestimo emprestimo : encontrados) resultado.add(paraView(emprestimo, true));
        return resultado;
    }

    @Override
    public String reservar(String codigoDoItem, String matricula, LocalDate data)
            throws RecursoNaoEncontradoException, OperacaoNaoPermitidaException {
        exigirTexto(codigoDoItem, "codigoDoItem");
        exigirTexto(matricula, "matricula");
        if (data == null) throw new DadosInvalidosException("A data da reserva é obrigatória.");
        Item item = contexto.itens.get(codigoDoItem);
        if (item == null) {
            throw new RecursoNaoEncontradoException("Item não encontrado: " + codigoDoItem);
        }
        Usuario usuario = contexto.usuarios.get(matricula);
        if (usuario == null) {
            throw new RecursoNaoEncontradoException("Usuário não encontrado: " + matricula);
        }
        if (!usuario.isAtivo()) {
            throw new UsuarioInativoException("Usuário inativo: " + matricula);
        }
        for (Exemplar exemplar : item.getExemplares()) {
            if (exemplar.getStatus() == StatusExemplar.DISPONIVEL) {
                throw new OperacaoNaoPermitidaException(
                        "Não é possível reservar item com exemplar disponível.");
            }
        }
        for (Reserva reserva : contexto.reservasAtivasDoItem(item)) {
            if (reserva.getUsuario() == usuario) {
                throw new OperacaoNaoPermitidaException(
                        "O usuário já possui reserva ativa para este item.");
            }
        }
        String id = novoId("R-");
        contexto.reservas.put(id, new Reserva(id, item, usuario, data));
        return id;
    }

    @Override
    public List<ReservaView> listarReservas(String codigoDoItem)
            throws RecursoNaoEncontradoException {
        exigirTexto(codigoDoItem, "codigoDoItem");
        Item item = contexto.itens.get(codigoDoItem);
        if (item == null) {
            throw new RecursoNaoEncontradoException("Item não encontrado: " + codigoDoItem);
        }
        List<ReservaView> resultado = new ArrayList<>();
        int posicao = 0;
        for (Reserva reserva : contexto.reservasAtivasDoItem(item)) {
            resultado.add(new ReservaView(reserva.getId(), item.getCodigo(),
                    reserva.getUsuario().getMatricula(), reserva.getDataDaReserva(), ++posicao));
        }
        return resultado;
    }

    @Override
    public void cancelarReserva(String idDaReserva) throws RecursoNaoEncontradoException {
        exigirTexto(idDaReserva, "idDaReserva");
        Reserva reserva = contexto.reservas.get(idDaReserva);
        if (reserva == null || !reserva.isAtiva()) {
            throw new RecursoNaoEncontradoException("Reserva ativa não encontrada: " + idDaReserva);
        }
        String tombo = reserva.getTomboSeparado();
        reserva.cancelar();
        if (tombo != null) {
            Exemplar exemplar = contexto.exemplares.get(tombo);
            contexto.proximaReservaAguardando(reserva.getItem(), exemplar);
        }
    }

    private EmprestimoView paraView(Emprestimo emprestimo, boolean atrasado) {
        StatusEmprestimo status = atrasado ? StatusEmprestimo.ATRASADO : emprestimo.getStatus();
        return new EmprestimoView(emprestimo.getId(), emprestimo.getExemplar().getTombo(),
                emprestimo.getUsuario().getMatricula(), emprestimo.getDataDoEmprestimo(),
                emprestimo.getDataPrevistaDeDevolucao(), emprestimo.getDataDaDevolucao(), status);
    }

    private String novoId(String prefixo) {
        String id;
        do { id = prefixo + UUID.randomUUID(); }
        while (contexto.emprestimos.containsKey(id) || contexto.reservas.containsKey(id));
        return id;
    }

    private static void exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new DadosInvalidosException("O campo " + campo + " é obrigatório.");
        }
    }
}
