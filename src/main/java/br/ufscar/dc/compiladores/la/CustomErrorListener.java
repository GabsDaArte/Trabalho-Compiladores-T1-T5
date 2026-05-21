package br.ufscar.dc.compiladores.la;

import org.antlr.v4.runtime.*;
import java.io.PrintWriter;


public class CustomErrorListener extends BaseErrorListener {
    PrintWriter pw;
    boolean erroSintatico = false;

    public CustomErrorListener(PrintWriter pw) {
        this.pw = pw;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine, String msg,
                            RecognitionException e) {

        if (erroSintatico) return; // evita repetir erro

        Token t = (Token) offendingSymbol;
        String texto = t.getText();

        if (t.getType() == LALexer.ERRO) {
            pw.println("Linha " + line + ": " + texto + " - simbolo nao identificado");
        } else if (t.getType() == LALexer.CADEIA_NAO_FECHADA) {
            pw.println("Linha " + line + ": cadeia literal nao fechada");
        } else if (t.getType() == LALexer.COMENTARIO_NAO_FECHADO) {
            pw.println("Linha " + line + ": comentario nao fechado");
        } else {
            if (texto.equals("<EOF>")) texto = "EOF";
            pw.println("Linha " + line + ": erro sintatico proximo a " + texto);
        }

        erroSintatico = true;
    }
}