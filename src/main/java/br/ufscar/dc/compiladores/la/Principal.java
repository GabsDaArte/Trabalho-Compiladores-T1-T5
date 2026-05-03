package br.ufscar.dc.compiladores.la;

import org.antlr.v4.runtime.*;
import java.io.*;


public class Principal {

    public static void main(String[] args) throws Exception {

        // Verifica argumentos
        if (args.length != 2) {
            System.out.println("Uso: java -jar <jar> <entrada> <saida>");
            return;
        }

        String arquivoEntrada = args[0];
        String arquivoSaida = args[1];

        PrintWriter writer = new PrintWriter(arquivoSaida);

        try {
            // leitura do arquivo
            CharStream cs = CharStreams.fromFileName(arquivoEntrada);

            // Lexer
            LALexer lexer = new LALexer(cs);

            // Token stream
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            // Parser
            LAParser parser = new LAParser(tokens);

            // Remove listeners padrão
            parser.removeErrorListeners();

            // Listener customizado
            CustomErrorListener listener = new CustomErrorListener(writer);
            parser.addErrorListener(listener);

            // Parsing (gera árvore)
            LAParser.ProgramaContext arvore = parser.programa();

            // Só faz análise semântica se NÃO teve erro sintático
            if (!listener.erroSintatico) {

                AnalisadorSemantico semantico = new AnalisadorSemantico();
                semantico.visit(arvore);

                // imprime erros semânticos
                for (String erro : semantico.getErros()) {
                    writer.println(erro);
                }
            }

            writer.println("Fim da compilacao");

        } catch (Exception e) {
            // pode deixar vazio como estava
        }

        writer.close();
    }
}