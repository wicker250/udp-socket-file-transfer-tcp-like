import java.io.BufferedOutputStream;
import java.io.File;
import java.net.DatagramPacket;

import java.net.DatagramSocket;
import java.io.FileOutputStream;

import java.io.IOException;

import java.util.Scanner;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;


public class UDPClient {

    private static final int PORTA = 9891;
    private static final int TAMANHO_CABECALHO = 35;  // 3 bytes para "ARQ" + 32 bytes para o contador
    private static final int TAMANHO_DADOS = 512;
    private static final int TAMANHO_PACOTE = TAMANHO_CABECALHO + TAMANHO_DADOS; // 512  + 32 =547 
    //path salvar arquivo 
    private static final String CAMINHO_ARQUIVO = "C:\\Users\\Afonso\\Desktop\\receber\\download.txt";

    public static void main(String[] args) {
        try ( DatagramSocket socket = new DatagramSocket() ;  Scanner scanner = new Scanner(System.in)) {
            InetAddress enderecoServidor = InetAddress.getByName("localhost");

         // Timeout de 15 seconds 
            socket.setSoTimeout(150000);    

            // Solicitar menu
             System.out.println("Solicitado a arquivos do Servidor");
           
             solicitarMenu(socket, enderecoServidor);

            
             System.out.println(" Digite o nome do arquivo para fazer o download no Server \nfoto1 \nfoto2");
            
             String nomeArquivo = scanner.nextLine().trim();

            System.out.println("Solicitando download do arquivo: " + nomeArquivo);
            solicitarDownload(socket,enderecoServidor,nomeArquivo);
            Map<Integer, byte[]> pacotesRecebidos = new HashMap<>();

            int ultimoIndice = -1;
            boolean recebendo = true;
           
            while (recebendo) {
                try {
                    byte[] buffer = new byte[TAMANHO_PACOTE];
                    DatagramPacket pacote = new DatagramPacket(buffer , buffer.length);
                    socket.receive(pacote);

                    String prefixo = new String(buffer, 0, 3);
                    if (prefixo.equals("ARQ")) {
                        int indicePacote = Integer.parseInt(new String(buffer , 3 , 32).trim()) - 1; //-1 ajustar indice
                        int tamanhoDados = pacote.getLength() -  TAMANHO_CABECALHO ;

                        byte[] dados = new byte[tamanhoDados];
                        System.arraycopy(buffer, TAMANHO_CABECALHO, dados, 0, tamanhoDados);

                        pacotesRecebidos.put(indicePacote, dados);
                        ultimoIndice = Math.max(ultimoIndice, indicePacote);
                        System.out.println("Pacote capturado indice " + indicePacote + " , tamanho de dados " + tamanhoDados + " bytes");
                    } else if (prefixo.equals("FIM")) {
                        recebendo = false;
                         System.out.println("Recepção do arquivo concluída");
                   
                    } else if (prefixo.equals("ERR")) {
                        System.out.println("Error : O Servidor não possuí o arquivo pedido");
                        return;
                    }
                } catch (IOException e) {
                    System.out.println("Tempo limite de espera atingido, encerrando recepção ");
                    recebendo = false;
                }
            }

            System.out.println("Numero total de pacotes esperados: " + (ultimoIndice + 1));
            
            System.out.println("Numero de pacotes  recebidos: " + pacotesRecebidos.size());

            // Solicitar pacotes faltantes e aguardar resposta
            for (int i = 0; i <= ultimoIndice; i++) {
                if (!pacotesRecebidos.containsKey(i)) {
                    System.out.println("Solicitando pacote faltante: índice " + (i + 1));
                    solicitarPacoteFaltante(socket, enderecoServidor, i + 1);

                    // Receber o pacote faltante
                   
                    
                    try {
                        byte[] buffer = new byte[TAMANHO_PACOTE];
                        
                        	DatagramPacket pacoteFaltante = new DatagramPacket(buffer, buffer.length);
                       
                        socket.receive(pacoteFaltante);

                        	String prefixo = new String(buffer, 0, 3);
                        if (prefixo.equals("ARQ")) {
                        	 int indicePacote = Integer.parseInt(new String(buffer, 3, 32).trim()) - 1;
                             int tamanhoDados = pacoteFaltante.getLength() - TAMANHO_CABECALHO;

                            byte[] dados = new byte[tamanhoDados];
                            System.arraycopy(buffer, TAMANHO_CABECALHO, dados, 0, tamanhoDados);

                            pacotesRecebidos.put(indicePacote, dados);
                            System.out.println("Pacote faltante recebido e armazenado: índice " + indicePacote + ", tamanho de dados: " + tamanhoDados + " bytes.");
                        }
                    } catch (IOException e) {
                        System.out.println("Erro ao receber pacote faltante: " + e.getMessage());
                    }
                }
            }

            // Montar arquivo final e salvar com nome fixo
            System.out.println("Iniciando o processo de salvamento do arquivo...");
            boolean sucesso = salvarArquivo(pacotesRecebidos, ultimoIndice + 1);
            if (sucesso) {
                System.out.println("Arquivo salvo com sucesso em: " + CAMINHO_ARQUIVO);
            } else {
                System.err.println("Erro ao salvar o arquivo, verifique o caminho da pasta ");
            }
        } catch (Exception e) {
             System.err.println("Erro no cliente: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void solicitarMenu(DatagramSocket socket, InetAddress endereco) throws IOException {
        byte[] buffer = "MEN".getBytes();
         DatagramPacket pacote = new DatagramPacket(buffer, buffer.length, endereco, PORTA);
         socket.send(pacote);
        System.out.println("Solicitação de menu enviada ao servidor.");
    }

    private static void solicitarDownload(DatagramSocket socket, InetAddress endereco, String nomeArquivo) throws IOException {
        byte[] buffer = ("GET" + nomeArquivo).getBytes();
        DatagramPacket pacote = new DatagramPacket(buffer, buffer.length, endereco, PORTA);
        socket.send(pacote);
        System.out.println("Solicitação de download enviada ao servidor");
    }

    private static void solicitarPacoteFaltante(DatagramSocket socket, InetAddress endereco, int indice) throws IOException {
        String mensagem = "FAL" + String.format("%032d", indice);
         byte[] buffer = mensagem.getBytes();
         DatagramPacket pacote = new DatagramPacket(buffer, buffer.length, endereco, PORTA);
         socket.send(pacote);
        System.out.println(" Solicitação de pacote faltante enviada para índice: " + indice);
    }

    private static boolean salvarArquivo(Map<Integer, byte[]> pacotes, int numPacotes) {
    		File arquivo = new File(CAMINHO_ARQUIVO);
    			try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(arquivo))) {
            for (int i = 0; i < numPacotes; i++) {
                byte[] dados = pacotes.get(i);
                
                if (dados != null) {
                    bos.write(dados);
                    System.out.println("Dados do pacote " + i + " escritos no arquivo");
                } else {
                    System.out.println("Pacote faltante na posição: " + i);
                    return false;
                }
            }
            System.out.println("Arquivo escrito com sucesso");
            return true;
          } catch (IOException e) {
            System.err.println("Erro ao salvar o arquivo: " + e.getMessage());
            
            return false;
        }
    }
}
