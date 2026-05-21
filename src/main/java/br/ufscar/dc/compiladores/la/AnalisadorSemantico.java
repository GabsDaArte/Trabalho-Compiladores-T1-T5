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

    private boolean dentroFuncao = false;

    HashMap<String, Map<String, TabelaDeSimbolos.TipoLA>> tiposCustomizadosRegistros = new HashMap<>();

    public List<String> getErros() {
        return erros;
    }

    // DECLARAÇÕES LOCAIS E TIPOS DE USUÁRIO
    @Override
    public TabelaDeSimbolos.TipoLA visitDeclaracao_local(LAParser.Declaracao_localContext ctx) {
        if (ctx.T_TIPO() != null) {
            String nomeTipo = ctx.IDENT().getText();
            TabelaDeSimbolos.TipoLA tipoDaVariavel = getTipo(ctx.tipo().getText());
            tabela.adicionar(nomeTipo, tipoDaVariavel != TabelaDeSimbolos.TipoLA.INVALIDO ? tipoDaVariavel : TabelaDeSimbolos.TipoLA.REGISTRO, TabelaDeSimbolos.Categoria.TIPO);

            // Registra os campos do tipo criado pelo usuário
            if (ctx.tipo().registro() != null) {
                Map<String, TabelaDeSimbolos.TipoLA> campos = new HashMap<>();
                for (LAParser.VariavelContext varCtx : ctx.tipo().registro().variavel()) {
                    TabelaDeSimbolos.TipoLA tipoCampo = getTipo(varCtx.tipo().getText());
                    for (LAParser.IdentificadorContext idCampo : varCtx.identificador()) {
                        campos.put(idCampo.getText(), tipoCampo);
                    }
                }
                tiposCustomizadosRegistros.put(nomeTipo, campos);
            }

        } else if (ctx.T_CONSTANTE() != null) {
            String nome = ctx.IDENT().getText();
            TabelaDeSimbolos.TipoLA tipo = getTipo(ctx.tipo_basico().getText());
            if (tabela.existeNoEscopoAtual(nome)) {
                erros.add("Linha " + ctx.start.getLine() + ": identificador " + nome + " ja declarado anteriormente");
            } else {
                tabela.adicionar(nome, tipo, TabelaDeSimbolos.Categoria.VARIAVEL);
            }
        } else {
            visitVariavel(ctx.variavel());
        }
        return null;
    }

    // DECLARAÇÃO DE VARIÁVEIS
    @Override
    public TabelaDeSimbolos.TipoLA visitVariavel(LAParser.VariavelContext ctx) {
        TabelaDeSimbolos.TipoLA tipo = getTipo(ctx.tipo().getText());

        if (tipo == TabelaDeSimbolos.TipoLA.INVALIDO) {
            erros.add("Linha " + ctx.start.getLine() + ": tipo " + ctx.tipo().getText() + " nao declarado");
        }

        for (LAParser.IdentificadorContext idCtx : ctx.identificador()) {
            String nomeOriginal = idCtx.getText();
            String nomeLimpo = obterNomeLimpo(idCtx); // Remove os [ ]

            if (tabela.existeNoEscopoAtual(nomeLimpo)) {
                erros.add("Linha " + idCtx.start.getLine() + ": identificador " + nomeOriginal + " ja declarado anteriormente");
            } else {
                // Adiciona a variável na tabela sem os colchetes
                tabela.adicionar(nomeLimpo, tipo, TabelaDeSimbolos.Categoria.VARIAVEL);

                // Achata os registros embutidos
                if (ctx.tipo().registro() != null) {
                    for (LAParser.VariavelContext varCtx : ctx.tipo().registro().variavel()) {
                        TabelaDeSimbolos.TipoLA tipoCampo = getTipo(varCtx.tipo().getText());
                        for (LAParser.IdentificadorContext idCampo : varCtx.identificador()) {
                            String nomeCampoLimpo = obterNomeLimpo(idCampo);
                            tabela.adicionar(nomeLimpo + "." + nomeCampoLimpo, tipoCampo, TabelaDeSimbolos.Categoria.VARIAVEL);
                        }
                    }
                }
                // Achata os tipos customizados
                else if (tiposCustomizadosRegistros.containsKey(ctx.tipo().getText())) {
                    Map<String, TabelaDeSimbolos.TipoLA> campos = tiposCustomizadosRegistros.get(ctx.tipo().getText());
                    for (Map.Entry<String, TabelaDeSimbolos.TipoLA> entry : campos.entrySet()) {
                        tabela.adicionar(nomeLimpo + "." + entry.getKey(), entry.getValue(), TabelaDeSimbolos.Categoria.VARIAVEL);
                    }
                }
            }
        }
        return null;
    }

    // IDENTIFICADOR USADO
    @Override
    public TabelaDeSimbolos.TipoLA visitIdentificador(LAParser.IdentificadorContext ctx) {
        String nomeOriginal = ctx.getText();
        String nomeLimpo = obterNomeLimpo(ctx);

        if (!tabela.existe(nomeLimpo)) {
            erros.add("Linha " + ctx.start.getLine() + ": identificador " + nomeOriginal + " nao declarado");
            return TabelaDeSimbolos.TipoLA.INVALIDO;
        }

        return tabela.verificar(nomeLimpo);
    }

    // COMANDO ATRIBUIÇÃO
    @Override
    public TabelaDeSimbolos.TipoLA visitCmdAtribuicao(LAParser.CmdAtribuicaoContext ctx) {
        String nomeOriginal = ctx.identificador().getText(); // Com colchetes
        String nomeLimpo = obterNomeLimpo(ctx.identificador()); // Sem colchetes

        if (!tabela.existe(nomeLimpo)) {
            erros.add("Linha " + ctx.start.getLine() + ": identificador " + nomeOriginal + " nao declarado");
            return null;
        }

        TabelaDeSimbolos.TipoLA tipoVar = tabela.verificar(nomeLimpo);

        // Fallback dinâmico para registros
        if (tipoVar == TabelaDeSimbolos.TipoLA.INVALIDO && ctx.identificador().IDENT().size() > 1) {
            String nomeBase = ctx.identificador().IDENT(0).getText();
            TabelaDeSimbolos.TipoLA tipoDaBase = tabela.verificar(nomeBase);
            if (tipoDaBase == TabelaDeSimbolos.TipoLA.REGISTRO) {
                tipoVar = visit(ctx.expressao());
            }
        }

        TabelaDeSimbolos.TipoLA tipoExpr = visit(ctx.expressao());
        boolean compativel = false;

        if (ctx.T_CIRCUNFLEXO() != null) {
            if (tipoVar == TabelaDeSimbolos.TipoLA.PONTEIRO_INTEIRO) tipoVar = TabelaDeSimbolos.TipoLA.INTEIRO;
            else if (tipoVar == TabelaDeSimbolos.TipoLA.PONTEIRO_REAL) tipoVar = TabelaDeSimbolos.TipoLA.REAL;
            else if (tipoVar == TabelaDeSimbolos.TipoLA.PONTEIRO_LITERAL) tipoVar = TabelaDeSimbolos.TipoLA.LITERAL;
            else if (tipoVar == TabelaDeSimbolos.TipoLA.PONTEIRO_LOGICO) tipoVar = TabelaDeSimbolos.TipoLA.LOGICO;
        }

        if (tipoVar == tipoExpr) {
            compativel = true;
        } else if ((tipoVar == TabelaDeSimbolos.TipoLA.INTEIRO || tipoVar == TabelaDeSimbolos.TipoLA.REAL) &&
                (tipoExpr == TabelaDeSimbolos.TipoLA.INTEIRO || tipoExpr == TabelaDeSimbolos.TipoLA.REAL)) {
            compativel = true;
        }

        if (!compativel) {
            String nomeErro = (ctx.T_CIRCUNFLEXO() != null ? "^" : "") + nomeOriginal;
            erros.add("Linha " + ctx.start.getLine() + ": atribuicao nao compativel para " + nomeErro);
        }

        return null;
    }

    // PARCELA UNÁRIO
    @Override
    public TabelaDeSimbolos.TipoLA visitParcela_unario(LAParser.Parcela_unarioContext ctx) {
        if (ctx.NUM_INT() != null) return TabelaDeSimbolos.TipoLA.INTEIRO;
        if (ctx.NUM_REAL() != null) return TabelaDeSimbolos.TipoLA.REAL;

        // Se for uma chamada de função (possui IDENT e '()')
        if (ctx.IDENT() != null && !ctx.expressao().isEmpty()) {
            String nome = ctx.IDENT().getText();

            if (!tabela.existe(nome)) {
                erros.add("Linha " + ctx.start.getLine() + ": identificador " + nome + " nao declarado");
                return TabelaDeSimbolos.TipoLA.INVALIDO;
            }

            TabelaDeSimbolos.EntradaTabela entrada = tabela.getEntrada(nome);
            List<TabelaDeSimbolos.TipoLA> argumentos = new ArrayList<>();

            for (var expr : ctx.expressao()) {
                argumentos.add(visit(expr));
            }

            // Valida quantidade de parâmetros
            if (argumentos.size() != entrada.parametros.size()) {
                erros.add("Linha " + ctx.start.getLine() + ": incompatibilidade de parametros na chamada de " + nome);
                return TabelaDeSimbolos.TipoLA.INVALIDO;
            }

            // Valida os tipos de cada parâmetro (ordem e tipo)
            for (int i = 0; i < argumentos.size(); i++) {
                TabelaDeSimbolos.TipoLA arg = argumentos.get(i);
                TabelaDeSimbolos.TipoLA param = entrada.parametros.get(i);

                boolean compativel = (arg == param);

                if (!compativel) {
                    erros.add("Linha " + ctx.start.getLine() + ": incompatibilidade de parametros na chamada de " + nome);
                    return null;
                }
            }

            return entrada.tipo;
        }

        if (ctx.identificador() != null) {
            return visit(ctx.identificador());
        }
        if (!ctx.expressao().isEmpty()) {
            return visit(ctx.expressao(0));
        }

        return TabelaDeSimbolos.TipoLA.INVALIDO;
    }

    // PARCELA NÃO UNÁRIO (ENDEREÇO E PONTEIRO)
    @Override
    public TabelaDeSimbolos.TipoLA visitParcela_nao_unario(LAParser.Parcela_nao_unarioContext ctx) {
        if (ctx.CADEIA() != null) return TabelaDeSimbolos.TipoLA.LITERAL;

        // Mapeia o endereço '&' para o tipo de ponteiro correspondente
        if (ctx.T_ENDERECO() != null) {
            String nomeVar = ctx.identificador().getText();
            TabelaDeSimbolos.TipoLA tipoDaVar = tabela.verificar(nomeVar);

            if (tipoDaVar == TabelaDeSimbolos.TipoLA.INTEIRO) return TabelaDeSimbolos.TipoLA.PONTEIRO_INTEIRO;
            if (tipoDaVar == TabelaDeSimbolos.TipoLA.REAL) return TabelaDeSimbolos.TipoLA.PONTEIRO_REAL;
            if (tipoDaVar == TabelaDeSimbolos.TipoLA.LITERAL) return TabelaDeSimbolos.TipoLA.PONTEIRO_LITERAL;
            if (tipoDaVar == TabelaDeSimbolos.TipoLA.LOGICO) return TabelaDeSimbolos.TipoLA.PONTEIRO_LOGICO;
        }
        return TabelaDeSimbolos.TipoLA.INVALIDO;
    }

    // CHAMADA DE PROCEDIMENTOS
    @Override
    public TabelaDeSimbolos.TipoLA visitCmdChamada(LAParser.CmdChamadaContext ctx) {
        String nome = ctx.IDENT().getText();

        if (!tabela.existe(nome)) {
            erros.add("Linha " + ctx.start.getLine() + ": identificador " + nome + " nao declarado");
            return null;
        }

        TabelaDeSimbolos.EntradaTabela entrada = tabela.getEntrada(nome);
        List<TabelaDeSimbolos.TipoLA> argumentos = new ArrayList<>();

        for (var expr : ctx.expressao()) {
            argumentos.add(visit(expr));
        }

        // Valida quantidade, ordem e tipos
        if (argumentos.size() != entrada.parametros.size()) {
            erros.add("Linha " + ctx.start.getLine() + ": incompatibilidade de parametros na chamada de " + nome);
            return null;
        }

        for (int i = 0; i < argumentos.size(); i++) {
            TabelaDeSimbolos.TipoLA arg = argumentos.get(i);
            TabelaDeSimbolos.TipoLA param = entrada.parametros.get(i);

            boolean compativel = (arg == param);
            if (!compativel && (param == TabelaDeSimbolos.TipoLA.INTEIRO || param == TabelaDeSimbolos.TipoLA.REAL) &&
                    (arg == TabelaDeSimbolos.TipoLA.INTEIRO || arg == TabelaDeSimbolos.TipoLA.REAL)) {
                compativel = true;
            }

            if (!compativel) {
                erros.add("Linha " + ctx.start.getLine() + ": incompatibilidade de parametros na chamada de " + nome);
                return null;
            }
        }
        return null;
    }

    // EXPRESSÕES ARITMÉTICAS
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
            } else {
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
            } else if (tipo == TabelaDeSimbolos.TipoLA.LITERAL && prox == TabelaDeSimbolos.TipoLA.LITERAL) {
                tipo = TabelaDeSimbolos.TipoLA.LITERAL;
            } else {
                tipo = TabelaDeSimbolos.TipoLA.INVALIDO;
            }
        }

        return tipo;
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

        // Operações relacionais (>, <, ==) precisam que ambos os lados sejam válidos
        if (t1 == TabelaDeSimbolos.TipoLA.INVALIDO || t2 == TabelaDeSimbolos.TipoLA.INVALIDO) {
            return TabelaDeSimbolos.TipoLA.INVALIDO;
        }

        return TabelaDeSimbolos.TipoLA.LOGICO;
    }

    // DECLARAÇÃO DE FUNÇÕES E PROCEDIMENTOS
    @Override
    public TabelaDeSimbolos.TipoLA visitDeclaracao_global(LAParser.Declaracao_globalContext ctx) {
        String nome = ctx.IDENT().getText();

        // Verifica duplicidade
        if (tabela.existeNoEscopoAtual(nome)) {
            erros.add("Linha " + ctx.start.getLine() + ": identificador " + nome + " ja declarado anteriormente");
            return null;
        }

        TabelaDeSimbolos.Categoria categoria;
        TabelaDeSimbolos.TipoLA tipoRetorno = TabelaDeSimbolos.TipoLA.INVALIDO;

        if (ctx.tipo_estendido() != null) {
            categoria = TabelaDeSimbolos.Categoria.FUNCAO;
            dentroFuncao = true;
            tipoRetorno = getTipo(ctx.tipo_estendido().getText());
        } else {
            categoria = TabelaDeSimbolos.Categoria.PROCEDIMENTO;
            dentroFuncao = false;
        }

        tabela.adicionar(nome, tipoRetorno, categoria);

        TabelaDeSimbolos.EntradaTabela entrada = tabela.getEntrada(nome);

        // Cria escopo interno
        tabela.novoEscopo();

        // Visita parâmetros
        if (ctx.parametros() != null) {
            for (var parametro : ctx.parametros().parametro()) {
                TabelaDeSimbolos.TipoLA tipoParametro = getTipo(parametro.tipo_estendido().getText());
                for (var id : parametro.identificador()) {
                    entrada.parametros.add(tipoParametro);
                }
            }
        }

        // Percorre comandos internos
        visitChildren(ctx);

        // Sai do escopo
        tabela.abandonarEscopo();

        dentroFuncao = false;

        return null;
    }

    // PARÂMETROS E RETORNOS
    @Override
    public TabelaDeSimbolos.TipoLA visitParametro(LAParser.ParametroContext ctx) {
        TabelaDeSimbolos.TipoLA tipo = getTipo(ctx.tipo_estendido().getText());

        for (var id : ctx.identificador()) {
            String nomeOriginal = id.getText();
            String nomeLimpo = obterNomeLimpo(id);

            if (tabela.existeNoEscopoAtual(nomeLimpo)) {
                erros.add("Linha " + id.start.getLine() + ": identificador " + nomeOriginal + " ja declarado anteriormente");
            } else {
                // Adiciona o parâmetro principal
                tabela.adicionar(nomeLimpo, tipo, TabelaDeSimbolos.Categoria.VARIAVEL);

                // Achata os campos caso o parâmetro seja um registro (tipo customizado)
                String nomeTipoBase = ctx.tipo_estendido().getText().replace("^", "");

                if (tiposCustomizadosRegistros.containsKey(nomeTipoBase)) {
                    Map<String, TabelaDeSimbolos.TipoLA> campos = tiposCustomizadosRegistros.get(nomeTipoBase);
                    for (Map.Entry<String, TabelaDeSimbolos.TipoLA> entry : campos.entrySet()) {
                        tabela.adicionar(nomeLimpo + "." + entry.getKey(), entry.getValue(), TabelaDeSimbolos.Categoria.VARIAVEL);
                    }
                }
            }
        }

        return null;
    }

    @Override
    public TabelaDeSimbolos.TipoLA visitCmdRetorne(LAParser.CmdRetorneContext ctx) {
        if (!dentroFuncao) {
            erros.add("Linha " + ctx.start.getLine() + ": comando retorne nao permitido nesse escopo");
        }
        return null;
    }

    // MÉTODOS UTILITÁRIOS
    private String obterNomeLimpo(LAParser.IdentificadorContext ctx) {
        StringBuilder nome = new StringBuilder();
        for (int i = 0; i < ctx.IDENT().size(); i++) {
            nome.append(ctx.IDENT(i).getText());
            if (i != ctx.IDENT().size() - 1) {
                nome.append(".");
            }
        }
        return nome.toString();
    }

    private TabelaDeSimbolos.TipoLA getTipo(String tipo) {
        if (tipo.startsWith("registro")) return TabelaDeSimbolos.TipoLA.REGISTRO;

        switch (tipo) {
            case "inteiro": return TabelaDeSimbolos.TipoLA.INTEIRO;
            case "real": return TabelaDeSimbolos.TipoLA.REAL;
            case "literal": return TabelaDeSimbolos.TipoLA.LITERAL;
            case "logico": return TabelaDeSimbolos.TipoLA.LOGICO;
            case "^inteiro": return TabelaDeSimbolos.TipoLA.PONTEIRO_INTEIRO;
            case "^real": return TabelaDeSimbolos.TipoLA.PONTEIRO_REAL;
            case "^literal": return TabelaDeSimbolos.TipoLA.PONTEIRO_LITERAL;
            case "^logico": return TabelaDeSimbolos.TipoLA.PONTEIRO_LOGICO;
        }

        // Se o tipo não for básico, procura se ele foi criado pelo usuário (ex: tipo endereco)
        if (tabela.existe(tipo)) {
            TabelaDeSimbolos.EntradaTabela entrada = tabela.getEntrada(tipo);
            if (entrada.categoria == TabelaDeSimbolos.Categoria.TIPO) {
                return entrada.tipo;
            }
        }

        return TabelaDeSimbolos.TipoLA.INVALIDO;
    }
}