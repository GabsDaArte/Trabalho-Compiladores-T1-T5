package br.ufscar.dc.compiladores.la;
import org.antlr.v4.runtime.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;


public class Principal {

    public static void main(String[] args) throws IOException {

        // Verifica se os argumentos foram passados corretamente
        if (args.length != 2) {
            System.out.println("Uso: java -jar <jar> <arquivo_entrada> <arquivo_saida>");
            return;
        }

        String arquivoEntrada = args[0];
        String arquivoSaida = args[1];

        // Leitura do arquivo de entrada
        CharStream cs = CharStreams.fromFileName(arquivoEntrada);

        // Instancia o lexer gerado pelo ANTLR
        LALexer lexer = new LALexer(cs);

        // Escrita no arquivo de saída
        BufferedWriter writer = new BufferedWriter(new FileWriter(arquivoSaida));

        Token token;

        while ((token = lexer.nextToken()).getType() != Token.EOF) {

            String texto = token.getText();
            String tipo = LALexer.VOCABULARY.getSymbolicName(token.getType());

            // =============================
            // TRATAMENTO DE ERROS
            // =============================

            if (tipo.equals("ERRO")) {
                writer.write("Linha " + token.getLine() + ": " + texto + " - simbolo nao identificado\n");
                writer.close();
                return;
            }

            if (tipo.equals("CADEIA_NAO_FECHADA")) {
                writer.write("Linha " + token.getLine() + ": cadeia literal nao fechada\n");
                writer.close();
                return;
            }

            if (tipo.equals("COMENTARIO_NAO_FECHADO")) {
                writer.write("Linha " + token.getLine() + ": comentario nao fechado\n");
                writer.close();
                return;
            }

            // =============================
            // FORMATAÇÃO DA SAÍDA
            // =============================

            // Tokens que imprimem como <'texto','texto'>
            if (tipo.equals("PALAVRA_CHAVE") ||
                    tipo.equals("OP_REL") ||
                    tipo.equals("OP_ARIT") ||
                    tipo.equals("DELIM") ||
                    tipo.equals("ABREPAR") ||
                    tipo.equals("FECHAPAR")) {

                writer.write("<'" + texto + "','" + texto + "'>\n");

            } else {
                // Tokens como IDENT, NUMINT, NUMREAL, CADEIA
                writer.write("<'" + texto + "'," + tipo + ">\n");
            }
        }

        writer.close();
    }
}