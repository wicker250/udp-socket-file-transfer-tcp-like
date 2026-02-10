import java.io.BufferedInputStream;
import java.io.File;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;


public class UDPServer {

		private static final int PORTA = 9891;
		private static final int TAMANHO_PACOTE = 512;
		
		private static final int TAMANHO_CABECALHO = 35; // 3 bytes para "ARQ" + 32 bytes para o contador
		private static final int TAMANHO_TOTAL_PACOTE = TAMANHO_CABECALHO + TAMANHO_PACOTE; // Total: 547 bytes

		 private static byte[] dadosArquivo1;
		 private static byte[] dadosArquivo2;
		 private static String nomeArquivoAtual = "";

    public static void main(String[] args) {
        try (DatagramSocket socketServidor = new DatagramSocket(PORTA)) {
            System.out.println("Servidor UDP iniciado na porta " + PORTA);

            // Carrega arquivos em vetores ao iniciar o servidor
           
            System.out.println("Carregando arquivos...");
            
            byte[] arquivoFoto1 = carregarArquivo("C:\\Users\\Afonso\\Desktop\\enviar\\foto1.txt");
            byte[] arquivoFoto2 = carregarArquivo("C:\\Users\\Afonso\\Desktop\\enviar\\foto2.txt");

            dadosArquivo1 = adicionarCabecalhos(arquivoFoto1);
            dadosArquivo2 = adicionarCabecalhos(arquivoFoto2);
            
            System.out.println("Arquivos carregados e cabeçalhos adicionados");

            while (true) {
                 System.out.println("\n Aguardando solicitações do cliente");
                 byte[] bufferRecebido = new byte[1024];
                
                DatagramPacket pacoteRecebido = new DatagramPacket(bufferRecebido, bufferRecebido.length);
                
                socketServidor.receive(pacoteRecebido);

                String mensagem = new String(pacoteRecebido.getData(), 0 , pacoteRecebido.getLength());
                
                InetAddress enderecoCliente = pacoteRecebido.getAddress();
                 int portaCliente = pacoteRecebido.getPort();

                System.out.println("Mensagem recebida do cliente : "  +  mensagem);

                if (mensagem.equals("MEN")) {
                    System.out.println("Enviando lista de arquivos para o cliente");
                    
                    enviarMensagem("Disponíveis: foto1, foto2", enderecoCliente, portaCliente, socketServidor);
                } else if (mensagem.startsWith("GET")) {
                     nomeArquivoAtual = mensagem.substring(3).trim();
                     System.out.println("Solicitação de download recebida para o arquivo: "  +  nomeArquivoAtual);

                    if (nomeArquivoAtual.equals("foto1")) {
                        System.out.println("Enviando arquivo foto1 para o cliente");
                        enviarArquivo(dadosArquivo1, enderecoCliente, portaCliente, socketServidor);
                    } else if (nomeArquivoAtual.equals("foto2")) {
                        System.out.println("Enviando arquivo foto2 para o cliente");
                        enviarArquivo(dadosArquivo2, enderecoCliente, portaCliente, socketServidor);
                    } else {
                        //TAG ERR
                    	System.out.println("Arquivo solicitado não encontrado: " + nomeArquivoAtual);
                        enviarMensagem("ERR", enderecoCliente, portaCliente, socketServidor);
                    }
                } else if (mensagem.startsWith("FAL")) {
                    int indicePacote = Integer.parseInt(mensagem.substring(3).trim()) - 1;
                    System.out.println("Solicitação de pacote faltante recebida para índice: " + indicePacote);

                    // Verifica qual arquivo está sendo transferido e reenvia o pacote solicitado
                     if (nomeArquivoAtual.equals("foto1")) {
                        enviarPacoteEspecifico(dadosArquivo1, indicePacote, enderecoCliente,  portaCliente,  socketServidor);
                    } else if (nomeArquivoAtual.equals("foto2")) {
                        enviarPacoteEspecifico(dadosArquivo2,  indicePacote,  enderecoCliente,  portaCliente, socketServidor);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erro no servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static byte[] carregarArquivo(String caminho) throws IOException {
        File arquivo = new File(caminho);
        byte[] dados = new byte[(int) arquivo.length()];
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(arquivo))) {
            bis.read(dados);
        }
        System.out.println("Arquivo carregado: " + caminho + ",  tamanho: " + dados.length + " bytes");
        return dados;
    }

    	private static byte[] adicionarCabecalhos(byte[] dados) {
    		int numPacotes = (int) Math.ceil((double) dados.length / TAMANHO_PACOTE);
    		List<byte[]> pacotes = new ArrayList<>();

          for (int i = 0; i < numPacotes; i++) {
              int inicio = i * TAMANHO_PACOTE;
              int comprimento = Math.min(TAMANHO_PACOTE, dados.length - inicio);

              byte[] pacote = new byte[TAMANHO_TOTAL_PACOTE];
              System.arraycopy("ARQ".getBytes(), 0, pacote, 0 , 3);
             String contador = String.format("%032d ",  i +  1);
            
             System.arraycopy(contador.getBytes(), 0, pacote, 3, 32);
           
            System.arraycopy(dados, inicio, pacote, TAMANHO_CABECALHO, comprimento);

            pacotes.add(pacote);
        }

        System.out.println("Cabeçalhos adicionados aos pacotes, total de pacotes: " + pacotes.size());

        int tamanhoFinal = pacotes.size() * TAMANHO_TOTAL_PACOTE;
        byte[] dadosComCabecalho = new byte[tamanhoFinal];
        int pos = 0;
        for (byte[] pacote : pacotes) {
            System.arraycopy(pacote, 0, dadosComCabecalho, pos, pacote.length);
            pos += pacote.length;
        }

        return dadosComCabecalho;
    }

    private static void enviarMensagem(String mensagem, InetAddress endereco, int porta, DatagramSocket socket) throws IOException {
        byte[] buffer = mensagem.getBytes();
        DatagramPacket pacote = new DatagramPacket(buffer, buffer.length, endereco, porta);
        socket.send(pacote);
        System.out.println("Mensagem enviada ao cliente: " + mensagem);
    }

    private static void enviarArquivo(byte[] dados, InetAddress endereco, int porta, DatagramSocket socket) throws IOException {
        
    	for (int i = 0; i < dados.length; i += TAMANHO_TOTAL_PACOTE) {
             int tamanho = Math.min(TAMANHO_TOTAL_PACOTE, dados.length - i);
         
             int indicePacote = i / TAMANHO_TOTAL_PACOTE + 1;

            // Simula a perda dos pacotes 4 e 5
            if (indicePacote == 4 || indicePacote == 5) {
                System.out.println("Simulando perda do pacote: índice " + indicePacote);
                continue; //    Pula o envio do pacote 4 e 5
            }

            byte[] pacote = new byte[tamanho];
            System.arraycopy(dados, i, pacote, 0, tamanho);

            DatagramPacket pacoteEnvio = new DatagramPacket(pacote, pacote.length, endereco, porta);
            socket.send(pacoteEnvio);
            System.out.println("Pacote enviado ao cliente, índice: " + indicePacote + ", tamanho: " + tamanho + " bytes.");

            // Delay de 20 milissegundos entre envios para evitar sobrecarga
            try {
                Thread.sleep(4);
            } catch (InterruptedException e) {
                System.err.println("Erro ao adicionar delay: " + e.getMessage());
            }
        }

        // Enviar a mensagem de término "FIM"
        
        enviarMensagem("FIM", endereco, porta, socket);
        System.out.println("Envio do arquivo concluído ");
    }

    private static void enviarPacoteEspecifico(byte[] dados, int indice, InetAddress endereco, int porta, DatagramSocket socket) throws IOException {
        int posicaoInicial = indice * TAMANHO_TOTAL_PACOTE;
        if ( posicaoInicial >= dados.length) {
            System.out.println(" Índice de pacote faltante inválido: " + indice);
            return;
        }

        int comprimento = Math.min(TAMANHO_TOTAL_PACOTE, dados.length - posicaoInicial);
          byte[] pacote = new byte[comprimento];
        System.arraycopy(dados, posicaoInicial, pacote, 0, comprimento);


         DatagramPacket pacoteEnvio = new DatagramPacket(pacote, pacote.length, endereco, porta);
         socket.send(pacoteEnvio);
         System.out.println("Pacote faltante reenviado índice: " + (indice + 1));
    }
}
