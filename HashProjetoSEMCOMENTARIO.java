import java.util.Random;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.io.*;
import java.util.Map;
import java.util.HashMap;
import java.util.Comparator;
import java.util.Collections;

public class HashProjetoSEMCOMENTARIO {

    public static class Registro {
        private final int codigo;

        public Registro(int codigo) {
            this.codigo = codigo;
        }

        public int getCodigo() {
            return codigo;
        }
    }

    public interface TabelaHash {
        int getTamanho();
        long inserir(Registro registro);
        long buscar(int chave);
        long getTotalColisoes();
        String getAnaliseColisaoExtra();
    }

    public static class NoEncadeamento {
        Registro registro;
        NoEncadeamento proximo;

        public NoEncadeamento(Registro registro) {
            this.registro = registro;
            this.proximo = null;
        }
    }

    public static class TabelaHashEncadeamento implements TabelaHash {
        private final NoEncadeamento[] tabela;
        private final int tamanho;
        private long totalColisoes = 0;
        private int[] tamanhosListas;
        private static final double A = 0.6180339887;

        public TabelaHashEncadeamento(int tamanho) {
            this.tamanho = tamanho;
            this.tabela = new NoEncadeamento[tamanho];
            this.tamanhosListas = new int[tamanho];
        }

        public int getTamanho() {
            return tamanho;
        }

        private int hash(int chave) {
            double x = chave * A;
            double fracionario = x - Math.floor(x);
            return (int) (tamanho * fracionario);
        }

        public long inserir(Registro registro) {
            int chave = registro.getCodigo();
            int indice = hash(chave);
            long colisoes = 0;

            if (tabela[indice] == null) {
                tabela[indice] = new NoEncadeamento(registro);
                tamanhosListas[indice] = 1;
            } else {
                NoEncadeamento atual = tabela[indice];
                NoEncadeamento anterior = null;

                while (atual != null && atual.registro.getCodigo() < chave) {
                    anterior = atual;
                    atual = atual.proximo;
                    colisoes++;
                }

                NoEncadeamento novoNo = new NoEncadeamento(registro);

                if (anterior == null) {
                    novoNo.proximo = tabela[indice];
                    tabela[indice] = novoNo;
                } else {
                    anterior.proximo = novoNo;
                    novoNo.proximo = atual;
                }

                tamanhosListas[indice]++;
                totalColisoes += colisoes;
            }
            return colisoes;
        }

        public long buscar(int chave) {
            int indice = hash(chave);
            long colisoes = 0;
            NoEncadeamento atual = tabela[indice];

            while (atual != null) {
                if (atual.registro.getCodigo() == chave) {
                    return colisoes;
                }
                atual = atual.proximo;
                colisoes++;
            }
            return colisoes;
        }

        public long getTotalColisoes() {
            return totalColisoes;
        }

        public String getAnaliseColisaoExtra() {
            int[] maiores = new int[] {0, 0, 0};

            for (int tamanho : tamanhosListas) {
                if (tamanho > maiores[0]) {
                    maiores[2] = maiores[1];
                    maiores[1] = maiores[0];
                    maiores[0] = tamanho;
                } else if (tamanho > maiores[1]) {
                    maiores[2] = maiores[1];
                    maiores[1] = tamanho;
                } else if (tamanho > maiores[2]) {
                    maiores[2] = tamanho;
                }
            }
            return "Top3 listas: " + maiores[0] + " | " + maiores[1] + " | " + maiores[2];
        }
    }

    public static class TabelaHashDuploHash implements TabelaHash {
        private final Registro[] tabela;
        private final int tamanho;
        private long totalColisoes = 0;
        private int elementosInseridos = 0;

        public TabelaHashDuploHash(int tamanho) {
            this.tamanho = tamanho;
            this.tabela = new Registro[tamanho];
        }

        public int getTamanho() {
            return tamanho;
        }

        private int h1(int chave) {
            int r = chave % tamanho;
            return r < 0 ? r + tamanho : r;
        }

        private int h2(int chave) {
            int r = chave % (tamanho - 1);
            if (r < 0) r += (tamanho - 1);
            return 1 + r;
        }

        public long inserir(Registro registro) {
            if (elementosInseridos == tamanho) {
                return -1;
            }

            int chave = registro.getCodigo();
            long colisoes = 0;
            int i = 0;

            while (i < tamanho) {
                long hashCalculado = (long)h1(chave) + (long)i * h2(chave);
                int indice = (int)(hashCalculado % tamanho);
                if (indice < 0) indice += tamanho;

                if (tabela[indice] == null) {
                    tabela[indice] = registro;
                    elementosInseridos++;
                    totalColisoes += colisoes;
                    return colisoes;
                }

                colisoes++;
                i++;
            }
            return -1;
        }

        public long buscar(int chave) {
            long colisoes = 0;
            int i = 0;

            while (i < tamanho) {
                long hashCalculado = (long)h1(chave) + (long)i * h2(chave);
                int indice = (int)(hashCalculado % tamanho);
                if (indice < 0) indice += tamanho;

                if (tabela[indice] == null) {
                    return colisoes;
                }

                if (tabela[indice].getCodigo() == chave) {
                    return colisoes;
                }

                colisoes++;
                i++;
            }
            return colisoes;
        }

        public long getTotalColisoes() {
            return totalColisoes;
        }

        public String getAnaliseColisaoExtra() {
            int menorGap = tamanho;
            int maiorGap = 0;
            int totalGap = 0;
            int contagemGaps = 0;
            int ultimaPosicaoOcupada = -1;

            for (int i = 0; i < tamanho; i++) {
                if (tabela[i] != null) {
                    if (ultimaPosicaoOcupada != -1) {
                        int gap = i - ultimaPosicaoOcupada - 1;
                        if (gap < menorGap) menorGap = gap;
                        if (gap > maiorGap) maiorGap = gap;
                        totalGap += gap;
                        contagemGaps++;
                    }
                    ultimaPosicaoOcupada = i;
                }
            }

            if (elementosInseridos > 1) {
                int primeiraPosicao = getPrimeiraPosicaoOcupada();
                if(primeiraPosicao != -1 && ultimaPosicaoOcupada != -1 && primeiraPosicao != ultimaPosicaoOcupada) {
                    int gapCircular = tamanho - 1 - ultimaPosicaoOcupada + primeiraPosicao;
                    if (gapCircular < menorGap) menorGap = gapCircular;
                    if (gapCircular > maiorGap) maiorGap = gapCircular;
                    totalGap += gapCircular;
                    contagemGaps++;
                }
            }

            double mediaGap = contagemGaps > 0 ? (double) totalGap / contagemGaps : 0;

            if (menorGap == tamanho) menorGap = 0;

            return String.format(Locale.US, "Gaps: min=%d | max=%d | média=%.2f",
                    menorGap, maiorGap, mediaGap);
        }

        private int getPrimeiraPosicaoOcupada() {
            for (int i = 0; i < tamanho; i++) {
                if (tabela[i] != null) {
                    return i;
                }
            }
            return -1;
        }
    }

    public static class TabelaHashSondagemQuadratica implements TabelaHash {
        private final Registro[] tabela;
        private final int tamanho;
        private long totalColisoes = 0;
        private int elementosInseridos = 0;
        private static final int C1 = 1;
        private static final int C2 = 1;

        public TabelaHashSondagemQuadratica(int tamanho) {
            this.tamanho = tamanho;
            this.tabela = new Registro[tamanho];
        }

        public int getTamanho() {
            return tamanho;
        }

        private int h1(int chave) {
            int r = chave % tamanho;
            return r < 0 ? r + tamanho : r;
        }

        public long inserir(Registro registro) {
            if (elementosInseridos == tamanho) {
                return -1;
            }

            int chave = registro.getCodigo();
            long colisoes = 0;
            int i = 0;

            while (i < tamanho) {
                long hashCalculado = (long)h1(chave) + C1 * i + (long)C2 * i * i;
                int indice = (int)(hashCalculado % tamanho);
                if (indice < 0) indice += tamanho;

                if (tabela[indice] == null) {
                    tabela[indice] = registro;
                    elementosInseridos++;
                    totalColisoes += colisoes;
                    return colisoes;
                }

                colisoes++;
                i++;
            }
            return -1;
        }

        public long buscar(int chave) {
            long colisoes = 0;
            int i = 0;

            while (i < tamanho) {
                long hashCalculado = (long)h1(chave) + C1 * i + (long)C2 * i * i;
                int indice = (int)(hashCalculado % tamanho);
                if (indice < 0) indice += tamanho;

                if (tabela[indice] == null) {
                    return colisoes;
                }

                if (tabela[indice].getCodigo() == chave) {
                    return colisoes;
                }

                colisoes++;
                i++;
            }
            return colisoes;
        }

        public long getTotalColisoes() {
            return totalColisoes;
        }

        public String getAnaliseColisaoExtra() {
            int menorGap = tamanho;
            int maiorGap = 0;
            int totalGap = 0;
            int contagemGaps = 0;
            int ultimaPosicaoOcupada = -1;

            for (int i = 0; i < tamanho; i++) {
                if (tabela[i] != null) {
                    if (ultimaPosicaoOcupada != -1) {
                        int gap = i - ultimaPosicaoOcupada - 1;
                        if (gap < menorGap) menorGap = gap;
                        if (gap > maiorGap) maiorGap = gap;
                        totalGap += gap;
                        contagemGaps++;
                    }
                    ultimaPosicaoOcupada = i;
                }
            }

            if (elementosInseridos > 1) {
                int primeiraPosicao = getPrimeiraPosicaoOcupada();
                if(primeiraPosicao != -1 && ultimaPosicaoOcupada != -1 && primeiraPosicao != ultimaPosicaoOcupada) {
                    int gapCircular = tamanho - 1 - ultimaPosicaoOcupada + primeiraPosicao;
                    if (gapCircular < menorGap) menorGap = gapCircular;
                    if (gapCircular > maiorGap) maiorGap = gapCircular;
                    totalGap += gapCircular;
                    contagemGaps++;
                }
            }

            double mediaGap = contagemGaps > 0 ? (double) totalGap / contagemGaps : 0;

            if (menorGap == tamanho) menorGap = 0;

            return String.format(Locale.US, "Gaps: min=%d | max=%d | média=%.2f",
                    menorGap, maiorGap, mediaGap);
        }

        private int getPrimeiraPosicaoOcupada() {
            for (int i = 0; i < tamanho; i++) {
                if (tabela[i] != null) {
                    return i;
                }
            }
            return -1;
        }
    }

    public static class GeradorDados {
        private static final long SEED = 42;
        private static final int MIN_CODIGO = 100000000;
        private static final int MAX_CODIGO = 999999999;

        public static Registro[] gerarConjunto(int tamanho) {
            Registro[] dados = new Registro[tamanho];
            Random random = new Random(SEED);

            for (int i = 0; i < tamanho; i++) {
                int codigo = random.nextInt(MAX_CODIGO - MIN_CODIGO + 1) + MIN_CODIGO;
                dados[i] = new Registro(codigo);
            }
            return dados;
        }
    }

    private static final int[] TAMANHOS_VETOR = {1000, 10000, 100000};
    private static final int[] TAMANHOS_DADOS = {10000, 100000, 1000000};

    public static void main(String[] args) {

        List<String> csvData = new ArrayList<>();

        String csvHeader = "estrategia,tamanho_vetor,qtde_dados,insercao_ms,busca_ms,colisoes_insercao,colisoes_busca,extra";
        csvData.add(csvHeader);

        Registro[][] conjuntosDados = new Registro[TAMANHOS_DADOS.length][];
        for (int i = 0; i < TAMANHOS_DADOS.length; i++) {
            System.out.println("Gerando conjunto de " + TAMANHOS_DADOS[i] + " elementos...");
            conjuntosDados[i] = GeradorDados.gerarConjunto(TAMANHOS_DADOS[i]);
        }
        System.out.println();

        for (int tamVetor : TAMANHOS_VETOR) {
            for (int i = 0; i < TAMANHOS_DADOS.length; i++) {
                int tamDados = TAMANHOS_DADOS[i];
                Registro[] dados = conjuntosDados[i];

                TabelaHash[] tabelas = new TabelaHash[] {
                        new TabelaHashEncadeamento(tamVetor),
                        new TabelaHashDuploHash(tamVetor),
                        new TabelaHashSondagemQuadratica(tamVetor)
                };

                String[] nomesTabelas = new String[] {
                        "Encadeamento (Multiplicacao)",
                        "Duplo Hashing (Rehashing)",
                        "Sondagem Quadratica (Rehashing)"
                };

                if (tamDados > tamVetor) {
                    System.out.printf("--- CENÁRIO CRÍTICO (Load Factor > 1.0) - Vetor: %-7d | Dados: %-7d ---%n", tamVetor, tamDados);
                    System.out.println("PULANDO: Duplo Hashing e Sondagem Quadrática (inviável com mais dados que slots).");

                    TabelaHash tabelaEncadeamento = tabelas[0];
                    String nomeEncadeamento = nomesTabelas[0];
                    String linhaCsvEnc = testarTabelaHash(nomeEncadeamento, tabelaEncadeamento, dados, tamDados);
                    csvData.add(linhaCsvEnc);
                    System.out.println();
                    continue;
                }

                for (int j = 0; j < tabelas.length; j++) {
                    TabelaHash tabela = tabelas[j];
                    String nome = nomesTabelas[j];

                    String linhaCsv = testarTabelaHash(nome, tabela, dados, tamDados);
                    csvData.add(linhaCsv);
                }
            }
        }

        escreverCSV(csvData, "out/resultados.csv");

        System.out.println("\n" + "-".repeat(100));
        System.out.println("RESUMO DE DESEMPENHO DAS TABELAS HASH (PARA RELATÓRIO)");
        System.out.println("-".repeat(100));

        imprimirTabelaComparacao(csvData);

        System.out.println("\n--- EXECUÇÃO CONCLUÍDA ---");
        System.out.println("O arquivo CSV COMPLETO foi gerado com sucesso em: out/resultados.csv");
        System.out.println("Utilize este arquivo para a construção dos gráficos em ferramentas externas (como Excel/Sheets).");
    }

    private static String testarTabelaHash(String nome, TabelaHash tabela, Registro[] dados, int tamDados) {
        long totalColisoesInsercao = 0;

        long inicioInsercao = System.nanoTime();
        for (Registro reg : dados) {
            long colisoes = tabela.inserir(reg);
            if (colisoes >= 0) {
                totalColisoesInsercao += colisoes;
            }
        }
        long fimInsercao = System.nanoTime();
        long tempoInsercaoNS = fimInsercao - inicioInsercao;

        long inicioBusca = System.nanoTime();
        long totalColisoesBusca = 0;
        for (Registro reg : dados) {
            totalColisoesBusca += tabela.buscar(reg.getCodigo());
        }
        long fimBusca = System.nanoTime();
        long tempoBuscaNS = fimBusca - inicioBusca;

        double insMs = tempoInsercaoNS / 1_000_000.0;
        double busMs = tempoBuscaNS / 1_000_000.0;

        System.out.printf("Tamanho Vetor: %-7d | Dados: %-8d | Estratégia: %-30s | Tempo Ins: %.3f ms | Colisões Totais: %d%n",
                tabela.getTamanho(), tamDados, nome, insMs, totalColisoesInsercao);

        String extra = tabela.getAnaliseColisaoExtra();
        if (extra == null) extra = "";
        extra = extra.replace(',', ';');

        return String.format(Locale.US,
                "%s,%d,%d,%.3f,%.3f,%d,%d,%s",
                nome,
                tabela.getTamanho(),
                tamDados,
                insMs,
                busMs,
                totalColisoesInsercao,
                totalColisoesBusca,
                extra);
    }

    private static void escreverCSV(List<String> linhas, String nomeArquivo) {
        try {
            File diretorioOut = new File("out");
            if (!diretorioOut.exists()) {
                diretorioOut.mkdirs();
            }

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(nomeArquivo))) {
                for (String linha : linhas) {
                    bw.write(linha);
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao escrever arquivo CSV: Verifique se o diretório 'out' existe ou se há permissão de escrita.");
            System.err.println("Detalhe do Erro: " + e.getMessage());
        }
    }

    private static void imprimirTabelaComparacao(List<String> csvData) {
        if (csvData.size() <= 1) {
            System.out.println("Não há dados suficientes para gerar a tabela de comparação.");
            return;
        }

        Map<String, List<String[]>> resultadosPorCenario = new HashMap<>();

        for (int i = 1; i < csvData.size(); i++) {
            String linha = csvData.get(i);
            String[] partes = linha.split(",");

            if (partes.length < 8) continue;

            String chaveCenario = partes[1] + "-" + partes[2];
            resultadosPorCenario.computeIfAbsent(chaveCenario, k -> new ArrayList<>()).add(partes);
        }

        List<String> chavesOrdenadas = new ArrayList<>(resultadosPorCenario.keySet());
        Collections.sort(chavesOrdenadas, (k1, k2) -> {
            String[] p1 = k1.split("-");
            String[] p2 = k2.split("-");
            int tamVetor1 = Integer.parseInt(p1[0]);
            int tamVetor2 = Integer.parseInt(p2[0]);
            if (tamVetor1 != tamVetor2) return Integer.compare(tamVetor1, tamVetor2);
            return Integer.compare(Integer.parseInt(p1[1]), Integer.parseInt(p2[1]));
        });

        for (String chaveCenario : chavesOrdenadas) {
            List<String[]> resultados = resultadosPorCenario.get(chaveCenario);

            int tamVetor = Integer.parseInt(chaveCenario.split("-")[0]);
            int tamDados = Integer.parseInt(chaveCenario.split("-")[1]);
            double alpha = (double) tamDados / tamVetor;

            System.out.println("\n" + "=".repeat(100));
            System.out.printf("| CENÁRIO: Vetor (m) = %-8d | Dados (n) = %-8d | Load Factor (α) = %.2f %-36s|%n",
                    tamVetor, tamDados, alpha, (alpha > 1.0 ? "(ALTO RISCO DE COLISÃO)" : ""));
            System.out.println("=".repeat(100));

            System.out.printf("| %-35s | %-12s | %-12s | %-12s | %-12s | %-15s |%n",
                    "ESTRATÉGIA", "INS. (ms)", "BUSCA (ms)", "COL. INS. (K)", "COL. BUS. (K)", "ANÁLISE EXTRA");
            System.out.println("|" + "-".repeat(37) + "+" + "-".repeat(14) + "+" + "-".repeat(14) + "+" + "-".repeat(14) + "+" + "-".repeat(14) + "+" + "-".repeat(17) + "|");

            resultados.sort(Comparator.comparing(r -> r[0]));

            for (String[] res : resultados) {
                String estrategia = res[0];

                if (res.length == 8) {
                    double insMs = Double.parseDouble(res[3]);
                    double busMs = Double.parseDouble(res[4]);
                    double colInsK = Double.parseDouble(res[5]) / 1000.0;
                    double colBusK = Double.parseDouble(res[6]) / 1000.0;
                    String extra = res[7].replace(';', ',');

                    System.out.printf("| %-35s | %-12.3f | %-12.3f | %-12.2f | %-12.2f | %-15s |%n",
                            estrategia, insMs, busMs, colInsK, colBusK, extra);
                }
            }

            if (alpha > 1.0) {
                String na = "N/A";
                System.out.printf("| %-35s | %-12s | %-12s | %-12s | %-12s | %-15s |%n",
                        "Duplo Hashing (Rehashing)", na, na, na, na, na);
                System.out.printf("| %-35s | %-12s | %-12s | %-12s | %-12s | %-15s |%n",
                        "Sondagem Quadratica (Rehashing)", na, na, na, na, na);
            }
            System.out.println("-".repeat(100));
        }
    }
}