package br.ufscar.dc.compiladores.la;

import java.util.*;
import org.antlr.v4.runtime.tree.TerminalNode;

public class AnalisadorSemantico extends LAParserBaseVisitor<TabelaDeSimbolos.TipoLA> {

    TabelaDeSimbolos tabela = new TabelaDeSimbolos();
    List<String> erros = new ArrayList<>();

    public List<String> getErros() {
        return erros;
    }

    // DECLARAÇÃO DE VARIÁVEIS
    @Override
    public TabelaDeSimbolos.TipoLA visitVariavel(LAParser.VariavelContext ctx) {

        TabelaDeSimbolos.TipoLA tipo = getTipo(ctx.tipo().getText());

        // ERRO: tipo não declarado
        if (tipo == TabelaDeSimbolos.TipoLA.INVALIDO) {
            erros.add("Linha " + ctx.start.getLine() + ": tipo " + ctx.tipo().getText() + " nao declarado");
        }

        for (LAParser.IdentificadorContext idCtx : ctx.identificador()) {
            String nome = idCtx.getText();

            if (tabela.existe(nome)) {
                erros.add("Linha " + idCtx.start.getLine() + ": identificador " + nome + " ja declarado anteriormente");
            } else {
                tabela.adicionar(nome, tipo);
            }
        }

        return null;
    }

    // IDENTIFICADOR USADO
    @Override
    public TabelaDeSimbolos.TipoLA visitIdentificador(LAParser.IdentificadorContext ctx) {

        String nome = ctx.getText();

        if (!tabela.existe(nome)) {
            erros.add("Linha " + ctx.start.getLine() + ": identificador " + nome + " nao declarado");
        }

        return tabela.verificar(nome);
    }

    // ATRIBUIÇÃO
    @Override
    public TabelaDeSimbolos.TipoLA visitCmdAtribuicao(LAParser.CmdAtribuicaoContext ctx) {

        String nome = ctx.identificador().getText();


        // ERRO 1: variável não declarada
        if (!tabela.existe(nome)) {
            erros.add("Linha " + ctx.start.getLine() + ": identificador " + nome + " nao declarado");
            return null;
        }

        TabelaDeSimbolos.TipoLA tipoVar = tabela.verificar(nome);
        TabelaDeSimbolos.TipoLA tipoExpr = visit(ctx.expressao());

        // ERRO 2: tipo incompatível
        if (tipoVar != tipoExpr) {
            erros.add("Linha " + ctx.start.getLine() + ": atribuicao nao compativel para " + nome);
        }


        return null;
    }

    // EXPRESSÃO (bem simplificado)
    @Override
    public TabelaDeSimbolos.TipoLA visitParcela_unario(LAParser.Parcela_unarioContext ctx) {

        if (ctx.NUM_INT() != null) return TabelaDeSimbolos.TipoLA.INTEIRO;
        if (ctx.NUM_REAL() != null) return TabelaDeSimbolos.TipoLA.REAL;


        if (ctx.identificador() != null) {
            return visit(ctx.identificador()); // 🔥 CORREÇÃO AQUI
        }


        return TabelaDeSimbolos.TipoLA.INVALIDO;
    }

    @Override
    public TabelaDeSimbolos.TipoLA visitParcela_nao_unario(LAParser.Parcela_nao_unarioContext ctx) {
        if (ctx.CADEIA() != null) return TabelaDeSimbolos.TipoLA.LITERAL;
        return TabelaDeSimbolos.TipoLA.INVALIDO;
    }



    @Override
    public TabelaDeSimbolos.TipoLA visitExp_aritmetica(LAParser.Exp_aritmeticaContext ctx) {

        TabelaDeSimbolos.TipoLA tipo = visit(ctx.termo(0));

        for (int i = 1; i < ctx.termo().size(); i++) {
            TabelaDeSimbolos.TipoLA prox = visit(ctx.termo(i));

            // regra: inteiro/real com inteiro/real -> ok
            if ((tipo == TabelaDeSimbolos.TipoLA.INTEIRO || tipo == TabelaDeSimbolos.TipoLA.REAL) &&
                    (prox == TabelaDeSimbolos.TipoLA.INTEIRO || prox == TabelaDeSimbolos.TipoLA.REAL)) {

                if (tipo == TabelaDeSimbolos.TipoLA.REAL || prox == TabelaDeSimbolos.TipoLA.REAL) {
                    tipo = TabelaDeSimbolos.TipoLA.REAL;
                } else {
                    tipo = TabelaDeSimbolos.TipoLA.INTEIRO;
                }

            } else {
                // qualquer combinação inválida
                tipo = TabelaDeSimbolos.TipoLA.INVALIDO;
            }
        }

        return tipo;
    }


    @Override
    public TabelaDeSimbolos.TipoLA visitFator(LAParser.FatorContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public TabelaDeSimbolos.TipoLA visitExpressao(LAParser.ExpressaoContext ctx) {

        TabelaDeSimbolos.TipoLA tipo = visit(ctx.termo_logico(0));

        for (int i = 1; i < ctx.termo_logico().size(); i++) {
            TabelaDeSimbolos.TipoLA prox = visit(ctx.termo_logico(i));

            if (tipo != prox) {
                tipo = TabelaDeSimbolos.TipoLA.INVALIDO;
            }
        }

        return tipo;
    }

    @Override
    public TabelaDeSimbolos.TipoLA visitTermo_logico(LAParser.Termo_logicoContext ctx) {

        TabelaDeSimbolos.TipoLA tipo = visit(ctx.fator_logico(0));

        for (int i = 1; i < ctx.fator_logico().size(); i++) {
            TabelaDeSimbolos.TipoLA prox = visit(ctx.fator_logico(i));

            if (tipo != prox) {
                tipo = TabelaDeSimbolos.TipoLA.INVALIDO;
            }
        }

        return tipo;
    }

    @Override
    public TabelaDeSimbolos.TipoLA visitFator_logico(LAParser.Fator_logicoContext ctx) {
        return visit(ctx.parcela_logica());
    }

    @Override
    public TabelaDeSimbolos.TipoLA visitParcela_logica(LAParser.Parcela_logicaContext ctx) {

        if (ctx.exp_relacional() != null) {
            return visit(ctx.exp_relacional());
        }

        return TabelaDeSimbolos.TipoLA.LOGICO;
    }

    @Override
    public TabelaDeSimbolos.TipoLA visitExp_relacional(LAParser.Exp_relacionalContext ctx) {

        if (ctx.exp_aritmetica().size() == 1) {
            return visit(ctx.exp_aritmetica(0));
        }

        TabelaDeSimbolos.TipoLA t1 = visit(ctx.exp_aritmetica(0));
        TabelaDeSimbolos.TipoLA t2 = visit(ctx.exp_aritmetica(1));

        if (t1 == TabelaDeSimbolos.TipoLA.INVALIDO || t2 == TabelaDeSimbolos.TipoLA.INVALIDO) {
            return TabelaDeSimbolos.TipoLA.INVALIDO;
        }

        return TabelaDeSimbolos.TipoLA.LOGICO;
    }


    @Override
    public TabelaDeSimbolos.TipoLA visitTermo(LAParser.TermoContext ctx) {

        TabelaDeSimbolos.TipoLA tipo = visit(ctx.fator(0));

        for (int i = 1; i < ctx.fator().size(); i++) {
            TabelaDeSimbolos.TipoLA t = visit(ctx.fator(i));

            if (tipo != t) {
                return TabelaDeSimbolos.TipoLA.INVALIDO;
            }
        }

        return tipo;
    }



    // MAPEAR TIPO
    private TabelaDeSimbolos.TipoLA getTipo(String tipo) {
        switch (tipo) {
            case "inteiro": return TabelaDeSimbolos.TipoLA.INTEIRO;
            case "real": return TabelaDeSimbolos.TipoLA.REAL;
            case "literal": return TabelaDeSimbolos.TipoLA.LITERAL;
            case "logico": return TabelaDeSimbolos.TipoLA.LOGICO;
            default: return TabelaDeSimbolos.TipoLA.INVALIDO;
        }
    }
}