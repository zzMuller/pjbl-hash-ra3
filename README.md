# Relatório — Análise de Tabelas Hash em Java

> **Disciplina / RA3** — Implementação e análise de desempenho de diferentes tabelas hash em Java, com variações de função hash e estratégias de tratamento de colisão.

---

## 1) Implementação
O projeto foi desenvolvido em **Java 21**, e segue o modelo solicitado pelo professor, contendo duas versões do código:
- `HashProjeto.java` — versão **com comentários explicativos** detalhando o funcionamento de cada bloco.
- `HashProjetoSEMCOMENTARIO.java` — versão **sem comentários**, usada como prova de autoria.

Durante a execução, o programa gera automaticamente o arquivo `resultados.csv`, onde são registrados os dados de desempenho das três funções hash e estratégias de colisão.

### Estrutura das pastas
```
main/
├── HashProjeto.java
├── HashProjetoSEMCOMENTARIO.java
├── README.md
└── resultados.csv
```

---

## 2) Escolha das funções hash
Foram utilizadas **três funções hash distintas**, escolhidas por sua simplicidade de implementação e bom comportamento em dispersão:

1. **Método da Multiplicação** (usado no Encadeamento)  
   → Calcula o índice com base na parte fracionária da multiplicação da chave por uma constante irracional (A = 0.6180339887).  
   → Proporciona boa distribuição mesmo para tabelas de tamanho não primo.

2. **Método da Divisão (modular)** (usado no Rehashing)  
   → Realiza o cálculo `h(k) = k % m`, ajustando para índices positivos.  
   → É simples e eficiente, ideal para tabelas cujo tamanho é número primo.

3. **Duplo Hashing e Sondagem Quadrática** (usados no Rehashing)  
   → São variações do endereçamento aberto que tratam colisões de forma mais eficiente.  
   → O duplo hashing calcula um novo salto a cada colisão, evitando clusters.  
   → A sondagem quadrática incrementa a posição usando `i²`, reduzindo colisões consecutivas.

---

## 3) Tamanhos dos conjuntos
Para garantir escalabilidade e análise de desempenho em diferentes níveis de carga, foram usados três tamanhos de tabela (`m`) e três tamanhos de conjuntos de dados (`n`):

| Tamanho da Tabela (m) | Conjunto de Dados (n) | Relação n/m | Situação |
|------------------------|-----------------------|--------------|-----------|
| 1.000                 | 10.000               | 10x          | Alta densidade (teste de limite) |
| 10.000                | 100.000              | 10x          | Carga pesada |
| 100.000               | 1.000.000            | 10x          | Carga máxima |

> A geração dos dados foi feita com **seed fixa (42)**, garantindo que todos os testes usassem os mesmos valores de entrada para validação.

---

## 4) Resultados e comparativos
Os resultados foram exportados no arquivo **`resultados.csv`**, contendo as seguintes colunas:

```
estrategia, tamanho_vetor, qtde_dados, insercao_ms, busca_ms, colisoes_insercao, colisoes_busca, extra
```

Cada linha representa um teste completo com uma das funções hash aplicadas a um conjunto de dados. Os principais indicadores medidos foram:
- **Tempo de inserção (ms)** — tempo total para inserir todos os elementos.
- **Tempo de busca (ms)** — tempo total para procurar todos os elementos inseridos.
- **Número de colisões na inserção e na busca.**
- **Análise extra:** tamanho das 3 maiores listas (encadeamento) e gaps médios (rehashing).

> Como o professor solicitou, a análise foi feita **com base no CSV**. Gráficos não foram adicionados, mantendo o relatório puramente analítico.

---

## 5) Explicação de quem foi melhor e por quê
Durante os testes, observou-se que:

- O **Encadeamento** apresentou **maior tolerância a cargas altas**, funcionando corretamente mesmo quando o número de elementos ultrapassava o tamanho da tabela (`n > m`).  
  - Apesar disso, o número de colisões foi bem mais alto, já que muitos itens precisaram ser adicionados em listas.
  - Ideal para cenários onde a quantidade de dados pode crescer além da capacidade prevista.

- O **Duplo Hashing** e a **Sondagem Quadrática** tiveram **melhores tempos de inserção e busca** enquanto o fator de carga (`n/m`) era menor que 1.  
  - Quando `n > m`, ambas as estratégias se tornaram inviáveis, pois dependem de espaços vazios no vetor.  
  - O Duplo Hashing teve uma leve vantagem por reduzir clusters de colisões, pois o passo varia conforme `h2(k)`.

**Conclusão comparativa:**
- Melhor desempenho geral (tempo e colisão baixa): **Duplo Hashing**, quando `n ≤ m`.
- Melhor robustez sob carga alta (`n > m`): **Encadeamento**.
- Sondagem Quadrática: desempenho intermediário, boa alternativa quando se deseja simplicidade sem necessidade de segunda função hash.

---

## 6) Conclusão
O projeto atendeu a todos os requisitos propostos:
- Foram implementadas **três funções hash diferentes**.
- Foram testadas **três estratégias de colisão (encadeamento, duplo hash, sondagem quadrática)**.
- Foram utilizadas **três escalas de tamanho (m e n)**.
- O programa gera automaticamente o **CSV com todos os resultados**.
- Os resultados foram **analisados comparativamente** de forma textual e objetiva, conforme solicitado.

**Resumo final:**
- O **Duplo Hashing** foi o mais eficiente em tempo quando a tabela não estava sobrecarregada.
- O **Encadeamento** foi o mais estável e seguro sob alta carga.
- O **Sondagem Quadrática** apresentou resultados regulares e previsíveis.

---

