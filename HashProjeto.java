import java.util.Random;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.io.*;
import java.util.Map;
import java.util.HashMap;
import java.util.Comparator;
import java.util.Collections;

/**
 * Classe principal que contém a lógica de implementação, teste e orquestração
 * de diferentes estratégias de Tabelas Hash.
 */
public class HashProjeto {

    // ======================================================================
    // 1. ESTRUTURAS DE DADOS BASE
    // ======================================================================

    /**
     * Classe simples para representar um registro (item) a ser armazenado
     * na Tabela Hash. Cada registro possui um código único de 9 dígitos.
     */
    public static class Registro {
        private final int codigo;

        public Registro(int codigo) {
            this.codigo = codigo;
        }

        public int getCodigo() {
            return codigo;
        }
    }

    /**
     * Interface que define o contrato básico que todas as implementações
     * de Tabela Hash devem seguir.
     */
    public interface TabelaHash {
        int getTamanho();
        long inserir(Registro registro);
        long buscar(int chave);
        long getTotalColisoes();
        String getAnaliseColisaoExtra(); // Para métricas específicas de cada técnica
    }

    /**
     * Estrutura de Nó utilizada exclusivamente pela Tabela Hash com Encadeamento.
     */
    public static class NoEncadeamento {
        Registro registro;
        NoEncadeamento proximo;

        public NoEncadeamento(Registro registro) {
            this.registro = registro;
            this.proximo = null;
        }
    }

    // ======================================================================
    // 2. IMPLEMENTAÇÕES DE TABELAS HASH
    // ======================================================================

    /**
     * Implementação da Tabela Hash usando a técnica de Encadeamento Separado.
     * Utiliza o Método da Multiplicação para a função hash.
     */
    public static class TabelaHashEncadeamento implements TabelaHash {
        private final NoEncadeamento[] tabela;
        private final int tamanho;
        private long totalColisoes = 0;
        private int[] tamanhosListas; // Armazena o tamanho de cada lista para análise extra
        private static final double A = 0.6180339887; // Constante para Hash Multiplicativo (proporção áurea)

        public TabelaHashEncadeamento(int tamanho) {
            this.tamanho = tamanho;
            this.tabela = new NoEncadeamento[tamanho];
            this.tamanhosListas = new int[tamanho];
        }

        public int getTamanho() {
            return tamanho;
        }

        /**
         * Função Hash (Método da Multiplicação): h(chave) = floor(m * (chave * A mod 1)).
         * Recebe o código do registro (chave) e calcula o índice dentro do tamanho da tabela.
         */
        private int hash(int chave) {
            double x = chave * A;
            double fracionario = x - Math.floor(x); // Pega a parte fracionária
            return (int) (tamanho * fracionario);
        }

        /**
         * Insere um registro na tabela, tratando colisões com encadeamento.
         * A inserção é feita de forma ordenada dentro da lista encadeada.
         * O valor retornado é o número de colisões (comparações) ocorridas nesta inserção.
         */
        public long inserir(Registro registro) {
            int chave = registro.getCodigo();
            int indice = hash(chave);
            long colisoes = 0;

            if (tabela[indice] == null) {
                // Inserção direta (sem colisão na posição inicial)
                tabela[indice] = new NoEncadeamento(registro);
                tamanhosListas[indice] = 1;
            } else {
                // Inserção com colisão, navegando na lista encadeada
                NoEncadeamento atual = tabela[indice];
                NoEncadeamento anterior = null;

                // Percorre a lista para encontrar o ponto de inserção ordenado
                while (atual != null && atual.registro.getCodigo() < chave) {
                    anterior = atual;
                    atual = atual.proximo;
                    colisoes++; // Colisão = uma comparação na lista encadeada
                }

                NoEncadeamento novoNo = new NoEncadeamento(registro);

                if (anterior == null) { // Inserção na cabeça da lista
                    novoNo.proximo = tabela[indice];
                    tabela[indice] = novoNo;
                } else { // Inserção no meio ou fim
                    anterior.proximo = novoNo;
                    novoNo.proximo = atual;
                }

                tamanhosListas[indice]++;
                totalColisoes += colisoes;
            }
            return colisoes;
        }

        /**
         * Busca um registro. A chave é o código a ser buscado.
         * O valor retornado é o número de colisões (comparações) necessárias para encontrar ou não o item.
         */
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

        /**
         * Retorna uma análise extra, mostrando o tamanho das 3 maiores listas
         * encadeadas, indicando o pior cenário de colisão.
         */
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

    /**
     * Implementação da Tabela Hash usando a técnica de Duplo Hashing (Rehashing).
     * Essa técnica de endereçamento aberto só funciona com Load Factor <= 1.0.
     */
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

        /** Primeira função hash (Hash 1), geralmente resto da divisão. */
        private int h1(int chave) {
            int r = chave % tamanho;
            return r < 0 ? r + tamanho : r;
        }

        /** Segunda função hash (Hash 2), usada para o salto (step).
         * O divisor (tamanho - 1) garante que o resultado não seja zero e
         * que haja uma distribuição diferente de h1.
         */
        private int h2(int chave) {
            int r = chave % (tamanho - 1);
            if (r < 0) r += (tamanho - 1);
            return 1 + r; // Retorna um valor entre 1 e tamanho-1
        }

        /**
         * Insere um registro usando a fórmula de Duplo Hashing:
         * h(chave, i) = (h1(chave) + i * h2(chave)) mod tamanho.
         * O valor retornado é o número de colisões (tentativas) ocorridas nesta inserção.
         */
        public long inserir(Registro registro) {
            if (elementosInseridos == tamanho) {
                return -1; // Tabela cheia
            }

            int chave = registro.getCodigo();
            long colisoes = 0;
            int i = 0; // Número de tentativas (sondas)

            while (i < tamanho) {
                // Cálculo do índice. O uso de 'long' evita overflow em cálculos intermediários.
                long hashCalculado = (long)h1(chave) + (long)i * h2(chave);
                int indice = (int)(hashCalculado % tamanho);
                if (indice < 0) indice += tamanho; // Garantindo que o índice seja positivo

                if (tabela[indice] == null) {
                    // Posição vazia encontrada
                    tabela[indice] = registro;
                    elementosInseridos++;
                    totalColisoes += colisoes;
                    return colisoes;
                }

                colisoes++;
                i++;
            }
            return -1; // Inserção falhou
        }

        /**
         * Busca um registro. A chave é o código a ser buscado.
         * A busca usa a mesma sequência de Duplo Hashing da inserção.
         * O valor retornado é o número de colisões (tentativas) até encontrar o item ou uma posição nula.
         */
        public long buscar(int chave) {
            long colisoes = 0;
            int i = 0;

            while (i < tamanho) {
                long hashCalculado = (long)h1(chave) + (long)i * h2(chave);
                int indice = (int)(hashCalculado % tamanho);
                if (indice < 0) indice += tamanho;

                if (tabela[indice] == null) {
                    // Posição nula indica que o item nunca foi inserido ou foi removido (não implementado)
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

        /**
         * Retorna uma análise extra sobre a distribuição dos dados na tabela
         * (clusterização), calculando o tamanho mínimo, máximo e médio dos gaps (espaços vazios).
         */
        public String getAnaliseColisaoExtra() {
            int menorGap = tamanho;
            int maiorGap = 0;
            int totalGap = 0;
            int contagemGaps = 0;
            int ultimaPosicaoOcupada = -1;

            // Percorre linearmente a tabela
            for (int i = 0; i < tamanho; i++) {
                if (tabela[i] != null) {
                    if (ultimaPosicaoOcupada != -1) {
                        int gap = i - ultimaPosicaoOcupada - 1; // Espaço entre dois itens
                        if (gap < menorGap) menorGap = gap;
                        if (gap > maiorGap) maiorGap = gap;
                        totalGap += gap;
                        contagemGaps++;
                    }
                    ultimaPosicaoOcupada = i;
                }
            }

            // Tratamento do gap circular (do último ocupado para o primeiro)
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

        // Método auxiliar para encontrar a primeira posição ocupada (para gap circular)
        private int getPrimeiraPosicaoOcupada() {
            for (int i = 0; i < tamanho; i++) {
                if (tabela[i] != null) {
                    return i;
                }
            }
            return -1;
        }
    }

    /**
     * Implementação da Tabela Hash usando a técnica de Sondagem Quadrática (Rehashing).
     * Essa técnica de endereçamento aberto só funciona com Load Factor <= 0.5.
     */
    public static class TabelaHashSondagemQuadratica implements TabelaHash {
        private final Registro[] tabela;
        private final int tamanho;
        private long totalColisoes = 0;
        private int elementosInseridos = 0;
        private static final int C1 = 1; // Constante 1 da sonda quadrática
        private static final int C2 = 1; // Constante 2 da sonda quadrática

        public TabelaHashSondagemQuadratica(int tamanho) {
            this.tamanho = tamanho;
            this.tabela = new Registro[tamanho];
        }

        public int getTamanho() {
            return tamanho;
        }

        /** Função Hash primária: resto da divisão (modular). */
        private int h1(int chave) {
            int r = chave % tamanho;
            return r < 0 ? r + tamanho : r;
        }

        /**
         * Insere um registro usando a fórmula de Sondagem Quadrática:
         * h(chave, i) = (h1(chave) + C1*i + C2*i^2) mod tamanho.
         * O valor retornado é o número de colisões (tentativas) ocorridas nesta inserção.
         */
        public long inserir(Registro registro) {
            if (elementosInseridos == tamanho) {
                return -1; // Tabela cheia
            }

            int chave = registro.getCodigo();
            long colisoes = 0;
            int i = 0; // Número de tentativas (sondas)

            while (i < tamanho) {
                // Cálculo do índice. O uso de 'long' evita overflow em cálculos intermediários.
                long hashCalculado = (long)h1(chave) + C1 * i + (long)C2 * i * i;
                int indice = (int)(hashCalculado % tamanho);
                if (indice < 0) indice += tamanho;

                if (tabela[indice] == null) {
                    // Posição vazia encontrada
                    tabela[indice] = registro;
                    elementosInseridos++;
                    totalColisoes += colisoes;
                    return colisoes;
                }

                colisoes++;
                i++;
            }
            return -1; // Inserção falhou
        }

        /**
         * Busca um registro. A chave é o código a ser buscado.
         * A busca usa a mesma sequência de Sondagem Quadrática.
         * O valor retornado é o número de colisões (tentativas) até encontrar o item ou uma posição nula.
         */
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

        /**
         * Retorna uma análise extra sobre a distribuição dos dados na tabela
         * (clusterização), calculando o tamanho mínimo, máximo e médio dos gaps.
         * A lógica é a mesma usada no Duplo Hashing.
         */
        public String getAnaliseColisaoExtra() {
            int menorGap = tamanho;
            int maiorGap = 0;
            int totalGap = 0;
            int contagemGaps = 0;
            int ultimaPosicaoOcupada = -1;

            // Percorre linearmente a tabela
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

            // Tratamento do gap circular
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

        // Método auxiliar para encontrar a primeira posição ocupada
        private int getPrimeiraPosicaoOcupada() {
            for (int i = 0; i < tamanho; i++) {
                if (tabela[i] != null) {
                    return i;
                }
            }
            return -1;
        }
    }

    // ======================================================================
    // 3. GERAÇÃO DE DADOS
    // ======================================================================

    /**
     * Classe utilitária para gerar conjuntos de dados aleatórios.
     * O uso de uma SEED fixa garante que os mesmos dados sejam gerados em
     * todas as execuções, o que é fundamental para a validação dos testes.
     */
    public static class GeradorDados {
        private static final long SEED = 42;
        private static final int MIN_CODIGO = 100000000; // 9 dígitos
        private static final int MAX_CODIGO = 999999999;

        /**
         * Gera um vetor de Registros. O parâmetro `tamanho` é o número de registros a serem gerados.
         * Retorna um vetor de Registros.
         */
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

    // 4. Parâmetros de Teste
    private static final int[] TAMANHOS_VETOR = {1000, 10000, 100000}; // Tamanhos (m) da Tabela Hash
    private static final int[] TAMANHOS_DADOS = {10000, 100000, 1000000}; // Quantidade (n) de Registros a inserir

    // ======================================================================
    // 5. MAIN E ORQUESTRAÇÃO DOS TESTES
    // ======================================================================

    /**
     * Ponto de entrada do programa. Configura e executa todos os cenários de teste,
     * coleta os resultados e exporta para CSV e para o console.
     */
    public static void main(String[] args) {

        List<String> csvData = new ArrayList<>();

        String csvHeader = "estrategia,tamanho_vetor,qtde_dados,insercao_ms,busca_ms,colisoes_insercao,colisoes_busca,extra";
        csvData.add(csvHeader);

        // 5.1. Pré-Geração de Dados
        Registro[][] conjuntosDados = new Registro[TAMANHOS_DADOS.length][];
        for (int i = 0; i < TAMANHOS_DADOS.length; i++) {
            System.out.println("Gerando conjunto de " + TAMANHOS_DADOS[i] + " elementos...");
            conjuntosDados[i] = GeradorDados.gerarConjunto(TAMANHOS_DADOS[i]);
        }
        System.out.println();

        // 5.2. Loop Principal de Teste
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

                // 5.3. Otimização: Pula Rehashing se Load Factor (alfa) > 1.0
                if (tamDados > tamVetor) {
                    System.out.printf("--- CENÁRIO CRÍTICO (Load Factor > 1.0) - Vetor: %-7d | Dados: %-7d ---%n", tamVetor, tamDados);
                    System.out.println("PULANDO: Duplo Hashing e Sondagem Quadrática (inviável com mais dados que slots).");

                    // Roda apenas o Encadeamento (índice 0)
                    TabelaHash tabelaEncadeamento = tabelas[0];
                    String nomeEncadeamento = nomesTabelas[0];
                    String linhaCsvEnc = testarTabelaHash(nomeEncadeamento, tabelaEncadeamento, dados, tamDados);
                    csvData.add(linhaCsvEnc);
                    System.out.println();
                    continue;
                }

                // 5.4. Executa os testes para todas as 3 estratégias
                for (int j = 0; j < tabelas.length; j++) {
                    TabelaHash tabela = tabelas[j];
                    String nome = nomesTabelas[j];

                    String linhaCsv = testarTabelaHash(nome, tabela, dados, tamDados);
                    csvData.add(linhaCsv);
                }
            }
        }

        // 5.5. Exportação e Resumo
        escreverCSV(csvData, "out/resultados.csv");

        System.out.println("\n" + "-".repeat(100));
        System.out.println("RESUMO DE DESEMPENHO DAS TABELAS HASH (PARA RELATÓRIO)");
        System.out.println("-".repeat(100));

        // NOVA CHAMADA: Imprime a tabela formatada
        imprimirTabelaComparacao(csvData);

        // Confirmação final
        System.out.println("\n--- EXECUÇÃO CONCLUÍDA ---");
        System.out.println("O arquivo CSV COMPLETO foi gerado com sucesso em: out/resultados.csv");
        System.out.println("Utilize este arquivo para a construção dos gráficos em ferramentas externas (como Excel/Sheets).");
    }

    // ======================================================================
    // 6. MÉTODOS AUXILIARES DE TESTE E GERAÇÃO DE SAÍDA
    // ======================================================================

    /**
     * Executa o teste de inserção e busca para uma Tabela Hash específica,
     * medindo tempo e colisões. A String retornada é formatada como linha CSV com todos os resultados.
     */
    private static String testarTabelaHash(String nome, TabelaHash tabela, Registro[] dados, int tamDados) {
        long totalColisoesInsercao = 0;

        // 6.1. TEMPO DE INSERÇÃO e COLISÕES NA INSERÇÃO
        long inicioInsercao = System.nanoTime();
        for (Registro reg : dados) {
            long colisoes = tabela.inserir(reg);
            if (colisoes >= 0) {
                totalColisoesInsercao += colisoes;
            }
        }
        long fimInsercao = System.nanoTime();
        long tempoInsercaoNS = fimInsercao - inicioInsercao;

        // 6.2. TEMPO DE BUSCA e COLISÕES NA BUSCA
        long inicioBusca = System.nanoTime();
        long totalColisoesBusca = 0;
        for (Registro reg : dados) {
            totalColisoesBusca += tabela.buscar(reg.getCodigo());
        }
        long fimBusca = System.nanoTime();
        long tempoBuscaNS = fimBusca - inicioBusca;

        // Conversão de nano para mili segundos
        double insMs = tempoInsercaoNS / 1_000_000.0;
        double busMs = tempoBuscaNS / 1_000_000.0;

        // 6.3. Imprime a saída no console
        System.out.printf("Tamanho Vetor: %-7d | Dados: %-8d | Estratégia: %-30s | Tempo Ins: %.3f ms | Colisões Totais: %d%n",
                tabela.getTamanho(), tamDados, nome, insMs, totalColisoesInsercao);


        // 6.4. Formata a linha CSV
        String extra = tabela.getAnaliseColisaoExtra();
        if (extra == null) extra = "";
        // Substitui vírgulas por ponto e vírgula no campo extra para não quebrar o CSV
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

    /**
     * Escreve os resultados de todos os testes em um arquivo CSV, criando
     * a pasta 'out/' se necessário.
     */
    private static void escreverCSV(List<String> linhas, String nomeArquivo) {
        try {
            // Tenta criar o diretório 'out' se ele não existir
            File diretorioOut = new File("out");
            if (!diretorioOut.exists()) {
                diretorioOut.mkdirs();
            }

            // Escreve o arquivo
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

    /**
     * NOVO MÉTODO: Imprime uma tabela de comparação formatada para o console
     * baseada nos dados coletados (csvData).
     */
    private static void imprimirTabelaComparacao(List<String> csvData) {
        if (csvData.size() <= 1) {
            System.out.println("Não há dados suficientes para gerar a tabela de comparação.");
            return;
        }

        // Estrutura para agrupar os resultados por cenário (tamanhoVetor, qtdeDados)
        Map<String, List<String[]>> resultadosPorCenario = new HashMap<>();

        for (int i = 1; i < csvData.size(); i++) {
            String linha = csvData.get(i);
            String[] partes = linha.split(",");

            // Colunas: 0: estrategia, 1: tamanho_vetor, 2: qtde_dados, 3: insercao_ms, 4: busca_ms, 5: colisoes_insercao, 6: colisoes_busca, 7: extra
            if (partes.length < 8) continue;

            String chaveCenario = partes[1] + "-" + partes[2];
            resultadosPorCenario.computeIfAbsent(chaveCenario, k -> new ArrayList<>()).add(partes);
        }

        // Ordena os cenários para uma saída mais organizada
        List<String> chavesOrdenadas = new ArrayList<>(resultadosPorCenario.keySet());
        Collections.sort(chavesOrdenadas, (k1, k2) -> {
            String[] p1 = k1.split("-");
            String[] p2 = k2.split("-");
            int tamVetor1 = Integer.parseInt(p1[0]);
            int tamVetor2 = Integer.parseInt(p2[0]);
            if (tamVetor1 != tamVetor2) return Integer.compare(tamVetor1, tamVetor2);
            return Integer.compare(Integer.parseInt(p1[1]), Integer.parseInt(p2[1]));
        });

        // 1. Itera e imprime cada cenário
        for (String chaveCenario : chavesOrdenadas) {
            List<String[]> resultados = resultadosPorCenario.get(chaveCenario);

            int tamVetor = Integer.parseInt(chaveCenario.split("-")[0]);
            int tamDados = Integer.parseInt(chaveCenario.split("-")[1]);
            double alpha = (double) tamDados / tamVetor;

            // Título do cenário
            System.out.println("\n" + "=".repeat(100));
            System.out.printf("| CENÁRIO: Vetor (m) = %-8d | Dados (n) = %-8d | Load Factor (α) = %.2f %-36s|%n",
                    tamVetor, tamDados, alpha, (alpha > 1.0 ? "(ALTO RISCO DE COLISÃO)" : ""));
            System.out.println("=".repeat(100));

            // Cabeçalho da tabela
            System.out.printf("| %-35s | %-12s | %-12s | %-12s | %-12s | %-15s |%n",
                    "ESTRATÉGIA", "INS. (ms)", "BUSCA (ms)", "COL. INS. (K)", "COL. BUS. (K)", "ANÁLISE EXTRA");
            System.out.println("|" + "-".repeat(37) + "+" + "-".repeat(14) + "+" + "-".repeat(14) + "+" + "-".repeat(14) + "+" + "-".repeat(14) + "+" + "-".repeat(17) + "|");


            // Ordena as estratégias para que Duplo e Quadrática venham antes ou depois de Encadeamento
            resultados.sort(Comparator.comparing(r -> r[0]));

            for (String[] res : resultados) {
                String estrategia = res[0];

                // Se o resultado é uma linha real (não um cenário pulado)
                if (res.length == 8) {
                    double insMs = Double.parseDouble(res[3]);
                    double busMs = Double.parseDouble(res[4]);
                    // Colisões são grandes, converte para Milhares (K)
                    double colInsK = Double.parseDouble(res[5]) / 1000.0;
                    double colBusK = Double.parseDouble(res[6]) / 1000.0;
                    String extra = res[7].replace(';', ','); // Volta ; para , para melhor leitura

                    System.out.printf("| %-35s | %-12.3f | %-12.3f | %-12.2f | %-12.2f | %-15s |%n",
                            estrategia, insMs, busMs, colInsK, colBusK, extra);
                }
            }

            // Garante que Rehashing é marcado como N/A se alpha > 1.0
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