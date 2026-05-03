package br.ufscar.dc.compiladores.la;

import java.util.*;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * Analisador Semântico para a linguagem LA.
 * Estende o visitor para percorrer a árvore de análise sintática
 * e valida as regras semânticas de escopo e tipagem estipuladas.
 */
public class AnalisadorSemantico extends LAParserBaseVisitor<TabelaDeSimbolos.TipoLA> {

    TabelaDeSimbolos tabela = new TabelaDeSimbolos();
    List<String> erros = new ArrayList<>();

    public List<String> getErros() {
        return erros;
    }

    // DECLARAÇÃO DE VARIÁVEIS E ESCOPO
    @Override
    public TabelaDeSimbolos.TipoLA visitVariavel(LAParser.VariavelContext ctx) {

        TabelaDeSimbolos.TipoLA tipo = getTipo(ctx.tipo().getText());

        // Verifica se o tipo da variável declarada existe
        if (tipo == TabelaDeSimbolos.TipoLA.INVALIDO) {
            erros.add("Linha " + ctx.start.getLine() + ": tipo " + ctx.tipo().getText() + " nao declarado");
        }

        // Para cada variável na linha, verifica se existe na tabela de símbolos
        for (LAParser.IdentificadorContext idCtx : ctx.identificador()) {
            String nome = idCtx.getText();

            // Evita a redeclaração da mesma variável no mesmo escopo
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

        // Antes de usar qualquer variável, verifica que ela foi declarada antes
        if (!tabela.existe(nome)) {
            erros.add("Linha " + ctx.start.getLine() + ": identificador " + nome + " nao declarado");
        }

        return tabela.verificar(nome);
    }

    // ATRIBUIÇÃO
    @Override
    public TabelaDeSimbolos.TipoLA visitCmdAtribuicao(LAParser.CmdAtribuicaoContext ctx) {

        String nome = ctx.identificador().getText();

        // Erro para variável não declarada
        if (!tabela.existe(nome)) {
            erros.add("Linha " + ctx.start.getLine() + ": identificador " + nome + " nao declarado");
            return null;
        }

        TabelaDeSimbolos.TipoLA tipoVar = tabela.verificar(nome);
        TabelaDeSimbolos.TipoLA tipoExpr = visit(ctx.expressao());

        boolean compativel = false;

        // Tipos exatos sempre são perfeitamente compatíveis
        if (tipoVar == tipoExpr) {
            compativel = true;
        }
        // Permite atribuição e conversão entre inteiros e reais
        else if ((tipoVar == TabelaDeSimbolos.TipoLA.INTEIRO || tipoVar == TabelaDeSimbolos.TipoLA.REAL) &&
                (tipoExpr == TabelaDeSimbolos.TipoLA.INTEIRO || tipoExpr == TabelaDeSimbolos.TipoLA.REAL)) {
            compativel = true;
        }

        if (!compativel) {
            erros.add("Linha " + ctx.start.getLine() + ": atribuicao nao compativel para " + nome);
        }

        return null;
    }

    // EXPRESSÃO (bem simplificado)
    @Override
    public TabelaDeSimbolos.TipoLA visitParcela_unario(LAParser.Parcela_unarioContext ctx) {

        // Retorna o tipo base lido diretamente do token numérico
        if (ctx.NUM_INT() != null) return TabelaDeSimbolos.TipoLA.INTEIRO;
        if (ctx.NUM_REAL() != null) return TabelaDeSimbolos.TipoLA.REAL;

        // Se for uma variável, busca o tipo salvo dela na tabela
        if (ctx.identificador() != null) {
            return visit(ctx.identificador());
        }

        // Trata o desempacotamento de expressões agrupadas por parênteses
        if (!ctx.expressao().isEmpty()) {
            return visit(ctx.expressao(0));
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

        // Percorre as somas/subtrações resolvendo a tipagem final da operação
        for (int i = 1; i < ctx.termo().size(); i++) {
            TabelaDeSimbolos.TipoLA prox = visit(ctx.termo(i));

            // Se ambos são numéricos, a operação é válida
            if ((tipo == TabelaDeSimbolos.TipoLA.INTEIRO || tipo == TabelaDeSimbolos.TipoLA.REAL) &&
                    (prox == TabelaDeSimbolos.TipoLA.INTEIRO || prox == TabelaDeSimbolos.TipoLA.REAL)) {

                // Se houver qualquer real na conta, o resultado final é promovido para real
                if (tipo == TabelaDeSimbolos.TipoLA.REAL || prox == TabelaDeSimbolos.TipoLA.REAL) {
                    tipo = TabelaDeSimbolos.TipoLA.REAL;
                } else {
                    tipo = TabelaDeSimbolos.TipoLA.INTEIRO;
                }
            }

            // Permite '+' agir como concatenação de strings
            else if (tipo == TabelaDeSimbolos.TipoLA.LITERAL && prox == TabelaDeSimbolos.TipoLA.LITERAL) {
                tipo = TabelaDeSimbolos.TipoLA.LITERAL;
            }

            else {
                tipo = TabelaDeSimbolos.TipoLA.INVALIDO;
            }
        }

        return tipo;
    }


    @Override
    public TabelaDeSimbolos.TipoLA visitFator(LAParser.FatorContext ctx) {
        return visitChildren(ctx);
    }

    // VISITAS LÓGICAS E RELACIONAIS
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

        // Operações relacionais (>, <, ==) precisam que ambos os lados sejam válidos para não quebrar
        if (t1 == TabelaDeSimbolos.TipoLA.INVALIDO || t2 == TabelaDeSimbolos.TipoLA.INVALIDO) {
            return TabelaDeSimbolos.TipoLA.INVALIDO;
        }

        return TabelaDeSimbolos.TipoLA.LOGICO;
    }


    @Override
    public TabelaDeSimbolos.TipoLA visitTermo(LAParser.TermoContext ctx) {

        TabelaDeSimbolos.TipoLA tipo = visit(ctx.fator(0));

        // Percorre as multiplicações/divisões resolvendo a tipagem
        for (int i = 1; i < ctx.fator().size(); i++) {
            TabelaDeSimbolos.TipoLA prox = visit(ctx.fator(i));

            if ((tipo == TabelaDeSimbolos.TipoLA.INTEIRO || tipo == TabelaDeSimbolos.TipoLA.REAL) &&
                    (prox == TabelaDeSimbolos.TipoLA.INTEIRO || prox == TabelaDeSimbolos.TipoLA.REAL)) {

                if (tipo == TabelaDeSimbolos.TipoLA.REAL || prox == TabelaDeSimbolos.TipoLA.REAL) {
                    tipo = TabelaDeSimbolos.TipoLA.REAL;
                } else {
                    tipo = TabelaDeSimbolos.TipoLA.INTEIRO;
                }
            }

            else if (tipo == TabelaDeSimbolos.TipoLA.LITERAL && prox == TabelaDeSimbolos.TipoLA.LITERAL) {
                tipo = TabelaDeSimbolos.TipoLA.LITERAL;
            }

            else {
                tipo = TabelaDeSimbolos.TipoLA.INVALIDO;
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