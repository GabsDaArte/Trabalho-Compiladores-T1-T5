package br.ufscar.dc.compiladores.la;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import java.io.PrintWriter;
import java.util.BitSet;

public class CustomErrorListener implements ANTLRErrorListener {
    PrintWriter pw;

    public CustomErrorListener(PrintWriter pw) {
        this.pw = pw;
    }

    @Override
    public void reportAmbiguity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, boolean exact, BitSet ambigAlts, ATNConfigSet configs) { }

    @Override
    public void reportAttemptingFullContext(Parser recognizer, DFA dfa, int startIndex, int stopIndex, BitSet conflictingAlts, ATNConfigSet configs) { }

    @Override
    public void reportContextSensitivity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, int prediction, ATNConfigSet configs) { }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        Token t = (Token) offendingSymbol;
        String texto = t.getText();

        // 1. Tenta identificar se o erro veio da fase Léxica (T1)
        if (t.getType() == LALexer.ERRO) {
            pw.println("Linha " + line + ": " + texto + " - simbolo nao identificado");
        } else if (t.getType() == LALexer.CADEIA_NAO_FECHADA) {
            pw.println("Linha " + line + ": cadeia literal nao fechada");
        } else if (t.getType() == LALexer.COMENTARIO_NAO_FECHADO) {
            pw.println("Linha " + line + ": comentario nao fechado");
        }
        // 2. Se não for léxico, é um erro de sintaxe puro (T2)
        else {
            if (texto.equals("<EOF>")) {
                texto = "EOF";
            }
            pw.println("Linha " + line + ": erro sintatico proximo a " + texto);
        }

        pw.println("Fim da compilacao");

        // Joga uma exceção para o Parser parar a execução no primeiro erro que encontrar!
        throw new RuntimeException("ParseError");
    }
}