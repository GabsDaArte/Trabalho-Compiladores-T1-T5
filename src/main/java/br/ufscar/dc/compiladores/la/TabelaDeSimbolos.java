package br.ufscar.dc.compiladores.la;

import java.util.*;

public class TabelaDeSimbolos {

    public enum TipoLA {
        INTEIRO,
        REAL,
        LITERAL,
        LOGICO,
        PONTEIRO_INTEIRO,
        PONTEIRO_REAL,
        PONTEIRO_LOGICO,
        PONTEIRO_LITERAL,
        REGISTRO,
        INVALIDO
    }

    public enum Categoria {
        VARIAVEL,
        FUNCAO,
        PROCEDIMENTO,
        TIPO
    }

    public static class EntradaTabela {

        String nome;
        TipoLA tipo;
        Categoria categoria;

        public List<TipoLA> parametros;

        EntradaTabela(String nome,
                      TipoLA tipo,
                      Categoria categoria){

            this.nome = nome;
            this.tipo = tipo;
            this.categoria = categoria;

            this.parametros = new ArrayList<>();
        }
    }

    private Stack<Map<String,EntradaTabela>> pilhaEscopos;

    public TabelaDeSimbolos(){

        pilhaEscopos = new Stack<>();

        novoEscopo();
    }

    public void novoEscopo(){

        pilhaEscopos.push(
                new HashMap<>()
        );
    }

    public void abandonarEscopo(){

        pilhaEscopos.pop();
    }

    public boolean existeNoEscopoAtual(String nome){

        return pilhaEscopos.peek()
                .containsKey(nome);
    }

    public boolean existe(String nome){

        for(int i=pilhaEscopos.size()-1;i>=0;i--){

            if(pilhaEscopos.get(i)
                    .containsKey(nome)){

                return true;
            }
        }

        return false;
    }

    public void adicionar(String nome,
                           TipoLA tipo,
                           Categoria categoria){

        pilhaEscopos.peek().put(
                nome,
                new EntradaTabela(
                        nome,
                        tipo,
                        categoria
                )
        );
    }

    public TipoLA verificar(String nome){

        for(int i=pilhaEscopos.size()-1;i>=0;i--){

            if(pilhaEscopos.get(i)
                    .containsKey(nome)){

                return pilhaEscopos
                        .get(i)
                        .get(nome)
                        .tipo;
            }
        }

        return TipoLA.INVALIDO;
    }

    public EntradaTabela getEntrada(String nome){

        for(int i=pilhaEscopos.size()-1;i>=0;i--){

            if(pilhaEscopos.get(i)
                    .containsKey(nome)){

                return pilhaEscopos
                        .get(i)
                        .get(nome);
            }
        }

        return null;
    }
}