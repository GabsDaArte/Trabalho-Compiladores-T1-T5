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
            //leitura do arquivo
            CharStream cs = CharStreams.fromFileName(arquivoEntrada);

            //Lexer
            LALexer lexer = new LALexer(cs);

            //Token stream
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            //parser
            LAParser parser = new LAParser(tokens);

            //Remove listeners padrão
            parser.removeErrorListeners();

            //adiciona o seu listener customizado
            parser.addErrorListener(new CustomErrorListener(writer));

            parser.programa();

            writer.println("Fim da compilacao");

        } catch (Exception e) {

        }

        writer.close();
    }
}