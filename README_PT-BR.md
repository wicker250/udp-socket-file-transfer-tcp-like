# Transferência Confiável de Arquivo via UDP (Recursos “tipo TCP”) — Java

Este projeto foi desenvolvido para a disciplina de **Redes de Computadores**. A ideia é implementar uma forma simples de **transferência confiável de arquivos** **sobre UDP**, adicionando conceitos parecidos com o TCP, como **sequenciamento, detecção de perda e retransmissão**.

O servidor divide o arquivo em blocos fixos, adiciona um cabeçalho com **número de sequência** e envia os pacotes via UDP. O cliente armazena os pacotes recebidos, identifica quais sequências faltaram, solicita retransmissão e, no final, remonta o arquivo na ordem correta.

---

## Funcionalidades (TCP-like sobre UDP)

- **Sequenciamento de pacotes** (cada pacote de dados possui número de sequência)
- **Detecção de perdas** (o cliente verifica quais índices não chegaram)
- **Retransmissão seletiva** (o cliente pede apenas os pacotes faltantes)
- **Reordenação e remontagem** (salva o arquivo final na sequência correta)
- **Tratamento de timeout** (encerra a espera após um tempo configurado)
- **Controle simples de envio** (pequeno delay no servidor para evitar rajadas)

---

## Protocolo (Camada de Aplicação)

### Tipos de mensagens

Cliente → Servidor
- `MEN` : solicita lista de arquivos (menu)
- `GET<nome>` : solicita download (ex.: `GETfoto1`, `GETfoto2`)
- `FAL<seq>` : solicita um pacote faltante (seq formatado com 32 dígitos)

Servidor → Cliente
- `Disponíveis: ...` : resposta ao `MEN`
- `ARQ<seq><dados>` : pacote de dados
- `FIM` : fim da transferência
- `ERR` : arquivo inexistente

### Formato do pacote

- **Tamanho do cabeçalho:** 35 bytes  
  - `ARQ` (3 bytes)
  - contador/seq (32 bytes, formatado)
- **Tamanho do payload:** 512 bytes
- **Tamanho total do pacote:** 547 bytes

O número de sequência no cabeçalho é **base 1** (o cliente armazena como índice base 0 internamente).

---

## Estrutura do Projeto

- `UDPServer.java` — carrega arquivos, fragmenta em pacotes, envia via UDP e reenvia pacotes faltantes
- `UDPClient.java` — solicita o arquivo, recebe pacotes, pede os faltantes e salva o resultado

---

## Como Executar

### Requisitos
- Java 8+ (funciona melhor com Java 11/17)

### 1) Configurar caminhos dos arquivos

O projeto usa **caminhos fixos (Windows)**. Ajuste se necessário:

**Servidor**
- Pasta de entrada (arquivos para enviar):
  - `C:\Users\Afonso\Desktop\enviar\foto1.txt`
  - `C:\Users\Afonso\Desktop\enviar\foto2.txt`

**Cliente**
- Arquivo de saída (resultado do download):
  - `C:\Users\Afonso\Desktop\receber\download.txt`

> Dica: garanta que a pasta de saída exista, senão o cliente não conseguirá salvar o arquivo.

### 2) Compilar

```bash
javac UDPServer.java UDPClient.java
```

### 3) Iniciar o servidor

```bash
java UDPServer
```

### 4) Iniciar o cliente (outro terminal)

```bash
java UDPClient
```

O cliente irá:
1. solicitar o menu (`MEN`)
2. pedir o nome do arquivo (`foto1` ou `foto2`)
3. receber, detectar faltas, solicitar retransmissão e salvar em `download.txt`

---

## Teste de Perda de Pacotes

O servidor simula perda de pacotes propositalmente pulando os pacotes **4 e 5** no primeiro envio.
Isso serve para validar a lógica de retransmissão.

---

## Limitações / Melhorias Futuras

- Implementar **ACKs** e **janela deslizante** (mais próximo do TCP real)
- Adicionar **checksum/CRC** para validar integridade
- Melhorar a sinalização de fim (ex.: enviar o total de pacotes)
- Suportar nomes arbitrários de arquivos (não apenas `foto1` e `foto2`)
- Tornar caminhos configuráveis por argumentos (sem hardcode)

---

## Licença

Uso educacional.
