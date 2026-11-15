package br.ufpb.dcx.poo.biblioteca.inicial;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import br.ufpb.dcx.poo.biblioteca.contrato.StatusExemplar;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.PersistenciaException;
import br.ufpb.dcx.poo.biblioteca.contrato.excecoes.RecursoDuplicadoException;
import br.ufpb.dcx.poo.biblioteca.dominio.Emprestimo;
import br.ufpb.dcx.poo.biblioteca.dominio.Exemplar;
import br.ufpb.dcx.poo.biblioteca.dominio.Item;
import br.ufpb.dcx.poo.biblioteca.dominio.Reserva;
import br.ufpb.dcx.poo.biblioteca.dominio.Usuario;

/** Leitura e escrita do estado completo, fora das entidades de domínio. */
final class PersistenciaBiblioteca {
    private static final String ARQUIVO = "biblioteca.dat";

    Path diretorioDeDados() {
        String configurado = System.getProperty("biblioteca.dados");
        return configurado == null ? Path.of("dados") : Path.of(configurado);
    }

    void salvar(ContextoBiblioteca contexto) throws PersistenciaException {
        Path diretorio = diretorioDeDados();
        Path temporario = null;
        try {
            Files.createDirectories(diretorio);
            Path destino = diretorio.resolve(ARQUIVO);
            temporario = Files.createTempFile(diretorio, "biblioteca-", ".tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(temporario, StandardCharsets.UTF_8)) {
                writer.write("BIBLIOTECA\t2");
                writer.newLine();
                for (Item item : contexto.itens.values()) {
                    escrever(writer, "I", texto(item.getCodigo()), texto(item.getTitulo()),
                            texto(item.getAutoria()), texto(item.getCategoria()),
                            Integer.toString(item.getAno()), numero(item.getClassificacaoIndicativa()));
                }
                for (Exemplar exemplar : contexto.exemplares.values()) {
                    escrever(writer, "X", texto(exemplar.getTombo()),
                            texto(exemplar.getItem().getCodigo()), exemplar.getStatus().name());
                }
                for (Usuario usuario : contexto.usuarios.values()) {
                    escrever(writer, "U", texto(usuario.getMatricula()), texto(usuario.getNome()),
                            Boolean.toString(usuario.isAtivo()), numero(usuario.getIdade()));
                }
                for (Emprestimo emprestimo : contexto.emprestimos.values()) {
                    escrever(writer, "L", texto(emprestimo.getId()),
                            texto(emprestimo.getExemplar().getTombo()),
                            texto(emprestimo.getUsuario().getMatricula()),
                            emprestimo.getDataDoEmprestimo().toString(),
                            emprestimo.getDataDaDevolucao() == null ? "~"
                                    : emprestimo.getDataDaDevolucao().toString());
                }
                for (Reserva reserva : contexto.reservas.values()) {
                    escrever(writer, "R", texto(reserva.getId()),
                            texto(reserva.getItem().getCodigo()),
                            texto(reserva.getUsuario().getMatricula()),
                            reserva.getDataDaReserva().toString(), reserva.getSituacao().name(),
                            texto(reserva.getTomboSeparado()));
                }
            }
            try {
                Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            apagarTemporario(temporario, e);
            throw comCausa("Não foi possível salvar os dados da biblioteca.", e);
        }
    }

    ContextoBiblioteca carregar() throws PersistenciaException {
        Path arquivo = diretorioDeDados().resolve(ARQUIVO);
        if (Files.notExists(arquivo)) return new ContextoBiblioteca();
        try {
            List<String[]> linhas = new ArrayList<>();
            try (BufferedReader reader = Files.newBufferedReader(arquivo, StandardCharsets.UTF_8)) {
                String primeira = reader.readLine();
                if (!"BIBLIOTECA\t2".equals(primeira)) {
                    throw new IllegalArgumentException("Cabeçalho ausente ou versão desconhecida.");
                }
                String linha;
                while ((linha = reader.readLine()) != null) {
                    if (linha.isBlank()) throw new IllegalArgumentException("Linha vazia no arquivo.");
                    linhas.add(linha.split("\\t", -1));
                }
            }
            return reconstruir(linhas);
        } catch (IOException e) {
            throw comCausa("Não foi possível carregar os dados da biblioteca.", e);
        } catch (RuntimeException e) {
            throw comCausa("Arquivo de dados corrompido; o estado atual foi preservado.", e);
        }
    }

    private ContextoBiblioteca reconstruir(List<String[]> linhas) {
        ContextoBiblioteca carregado = new ContextoBiblioteca();
        List<String[]> exemplares = new ArrayList<>();
        List<String[]> emprestimos = new ArrayList<>();
        List<String[]> reservas = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (String[] linha : linhas) {
            switch (linha[0]) {
                case "I" -> {
                    tamanho(linha, 7);
                    Item item = new Item(valor(linha[1]), valor(linha[2]),
                            valor(linha[3]), valor(linha[4]), Integer.parseInt(linha[5]));
                    item.restaurarClassificacaoIndicativa(inteiroOpcional(linha[6]));
                    if (carregado.itens.putIfAbsent(item.getCodigo(), item) != null)
                        throw new IllegalArgumentException("Código de item duplicado.");
                }
                case "X" -> { tamanho(linha, 4); exemplares.add(linha); }
                case "U" -> {
                    tamanho(linha, 5);
                    Usuario usuario = new Usuario(valor(linha[1]), valor(linha[2]));
                    usuario.restaurarEstado(Boolean.parseBoolean(estritoBoolean(linha[3])), 0);
                    usuario.restaurarIdade(inteiroOpcional(linha[4]));
                    if (carregado.usuarios.putIfAbsent(usuario.getMatricula(), usuario) != null)
                        throw new IllegalArgumentException("Matrícula duplicada.");
                }
                case "L" -> { tamanho(linha, 6); emprestimos.add(linha); }
                case "R" -> { tamanho(linha, 7); reservas.add(linha); }
                default -> throw new IllegalArgumentException("Tipo de registro desconhecido.");
            }
        }
        for (String[] linha : exemplares) {
            String tombo = valor(linha[1]);
            Item item = carregado.itens.get(valor(linha[2]));
            if (item == null) throw new IllegalArgumentException("Exemplar referencia item inexistente.");
            Exemplar exemplar = new Exemplar(tombo, item);
            exemplar.setStatus(StatusExemplar.valueOf(linha[3]));
            try {
                item.adicionarExemplar(exemplar);
            } catch (RecursoDuplicadoException e) {
                throw new IllegalArgumentException("Tombo duplicado dentro do item.", e);
            }
            if (carregado.exemplares.putIfAbsent(tombo, exemplar) != null)
                throw new IllegalArgumentException("Tombo duplicado.");
        }
        for (String[] linha : emprestimos) {
            String id = valor(linha[1]);
            Exemplar exemplar = carregado.exemplares.get(valor(linha[2]));
            Usuario usuario = carregado.usuarios.get(valor(linha[3]));
            if (exemplar == null || usuario == null)
                throw new IllegalArgumentException("Empréstimo referencia recurso inexistente.");
            if (!ids.add(id)) throw new IllegalArgumentException("Identificador duplicado.");
            Emprestimo emprestimo = new Emprestimo(id, exemplar, usuario, LocalDate.parse(linha[4]));
            if (!"~".equals(linha[5])) emprestimo.devolver(LocalDate.parse(linha[5]));
            if (emprestimo.isAtivo()) usuario.registrarEmprestimo();
            carregado.emprestimos.put(id, emprestimo);
        }
        for (String[] linha : reservas) {
            String id = valor(linha[1]);
            Item item = carregado.itens.get(valor(linha[2]));
            Usuario usuario = carregado.usuarios.get(valor(linha[3]));
            if (item == null || usuario == null)
                throw new IllegalArgumentException("Reserva referencia recurso inexistente.");
            if (!ids.add(id)) throw new IllegalArgumentException("Identificador duplicado.");
            Reserva reserva = new Reserva(id, item, usuario, LocalDate.parse(linha[4]));
            String tombo = valor(linha[6]);
            Reserva.Situacao situacao = Reserva.Situacao.valueOf(linha[5]);
            if (tombo != null) {
                Exemplar exemplar = carregado.exemplares.get(tombo);
                if (exemplar == null || exemplar.getItem() != item)
                    throw new IllegalArgumentException("Reserva referencia tombo inexistente ou de outro item.");
            }
            reserva.restaurarEstado(situacao, tombo);
            carregado.reservas.put(id, reserva);
        }
        validarConsistencia(carregado);
        return carregado;
    }

    private void validarConsistencia(ContextoBiblioteca c) {
        for (Exemplar exemplar : c.exemplares.values()) {
            long ativos = c.emprestimos.values().stream().filter(e -> e.isAtivo()
                    && e.getExemplar() == exemplar).count();
            long separados = c.reservas.values().stream().filter(r -> r.isAtiva()
                    && exemplar.getTombo().equals(r.getTomboSeparado())).count();
            if ((exemplar.getStatus() == StatusExemplar.EMPRESTADO) != (ativos == 1)
                    || ativos > 1) throw new IllegalArgumentException("Estado de empréstimo inconsistente.");
            if ((exemplar.getStatus() == StatusExemplar.RESERVADO) != (separados == 1)
                    || separados > 1) throw new IllegalArgumentException("Estado de reserva inconsistente.");
        }
        for (Reserva reserva : c.reservas.values()) {
            if (reserva.isAtiva() && !reserva.getUsuario().isAtivo())
                throw new IllegalArgumentException("Usuário inativo possui reserva ativa.");
        }
        for (Usuario usuario : c.usuarios.values()) {
            if (!usuario.isAtivo() && usuario.getEmprestimosAtivos() > 0)
                throw new IllegalArgumentException("Usuário inativo possui empréstimo ativo.");
        }
    }

    private static void escrever(BufferedWriter writer, String... campos) throws IOException {
        writer.write(String.join("\t", campos));
        writer.newLine();
    }

    private static String texto(String valor) {
        return valor == null ? "~" : Base64.getUrlEncoder().withoutPadding()
                .encodeToString(valor.getBytes(StandardCharsets.UTF_8));
    }

    private static String numero(Integer valor) {
        return valor == null ? "~" : valor.toString();
    }

    private static Integer inteiroOpcional(String valor) {
        return "~".equals(valor) ? null : Integer.valueOf(valor);
    }

    private static String valor(String codificado) {
        return "~".equals(codificado) ? null : new String(
                Base64.getUrlDecoder().decode(codificado), StandardCharsets.UTF_8);
    }

    private static void tamanho(String[] linha, int esperado) {
        if (linha.length != esperado) throw new IllegalArgumentException("Linha malformada.");
    }

    private static String estritoBoolean(String valor) {
        if (!"true".equals(valor) && !"false".equals(valor))
            throw new IllegalArgumentException("Boolean inválido.");
        return valor;
    }

    private static PersistenciaException comCausa(String mensagem, Throwable causa) {
        PersistenciaException excecao = new PersistenciaException(mensagem);
        excecao.initCause(causa);
        return excecao;
    }

    private static void apagarTemporario(Path temporario, IOException falhaOriginal) {
        if (temporario == null) return;
        try {
            Files.deleteIfExists(temporario);
        } catch (IOException falhaNaLimpeza) {
            falhaOriginal.addSuppressed(falhaNaLimpeza);
        }
    }
}
