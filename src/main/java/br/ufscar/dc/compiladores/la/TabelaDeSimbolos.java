package br.ufscar.dc.compiladores.la;

import java.util.*;

public class TabelaDeSimbolos {

    public enum TipoLA {
        INTEIRO, REAL, LITERAL, LOGICO, INVALIDO
    }

    private Map<String, TipoLA> tabela = new HashMap<>();

    public boolean existe(String nome) {
        return tabela.containsKey(nome);
    }

    public void adicionar(String nome, TipoLA tipo) {
        tabela.put(nome, tipo);
    }

    public TipoLA verificar(String nome) {
        return tabela.getOrDefault(nome, TipoLA.INVALIDO);
    }
}