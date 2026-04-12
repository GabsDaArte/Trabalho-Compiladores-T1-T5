parser grammar LAParser;

// Importa o vocabulário (tokens) do LALexer
options { tokenVocab=LALexer; }

// Raiz das regras
programa : declaracoes T_ALGORITMO corpo T_FIM_ALGORITMO EOF ;

declaracoes : decl_local_global* ;

decl_local_global : declaracao_local | declaracao_global ;

declaracao_local : T_DECLARE variavel
                 | T_CONSTANTE IDENT T_DOISPONTOS tipo_basico T_IGUAL valor_constante
                 | T_TIPO IDENT T_DOISPONTOS tipo ;

variavel : identificador (T_VIRGULA identificador)* T_DOISPONTOS tipo ;

identificador : IDENT (T_PONTO IDENT)* dimensao ;

dimensao : (T_ABRE_COL exp_aritmetica T_FECHA_COL)* ;

tipo : registro | tipo_estendido ;

tipo_basico : T_LITERAL | T_INTEIRO | T_REAL | T_LOGICO ;

tipo_basico_ident : tipo_basico | IDENT ;

tipo_estendido : T_CIRCUNFLEXO? tipo_basico_ident ;

valor_constante : CADEIA | NUM_INT | NUM_REAL | T_VERDADEIRO | T_FALSO ;

registro : T_REGISTRO variavel* T_FIM_REGISTRO ;

declaracao_global : T_PROCEDIMENTO IDENT T_ABRE_PAR parametros? T_FECHA_PAR declaracao_local* cmd* T_FIM_PROCEDIMENTO
                  | T_FUNCAO IDENT T_ABRE_PAR parametros? T_FECHA_PAR T_DOISPONTOS tipo_estendido declaracao_local* cmd* T_FIM_FUNCAO ;

parametro : T_VAR? identificador (T_VIRGULA identificador)* T_DOISPONTOS tipo_estendido ;

parametros : parametro (T_VIRGULA parametro)* ;

corpo : declaracao_local* cmd* ;

cmd : cmdLeia | cmdEscreva | cmdSe | cmdCaso | cmdPara | cmdEnquanto | cmdFaca | cmdAtribuicao | cmdChamada| cmdRetorne ;

cmdLeia : T_LEIA T_ABRE_PAR T_CIRCUNFLEXO? identificador (T_VIRGULA T_CIRCUNFLEXO? identificador)* T_FECHA_PAR ;
cmdEscreva : T_ESCREVA T_ABRE_PAR expressao (T_VIRGULA expressao)* T_FECHA_PAR ;
cmdSe : T_SE expressao T_ENTAO cmd* (T_SENAO cmd*)? T_FIM_SE ;
cmdCaso : T_CASO exp_aritmetica T_SEJA selecao (T_SENAO cmd*)? T_FIM_CASO ;
cmdPara : T_PARA IDENT T_ATRIBUICAO exp_aritmetica T_ATE exp_aritmetica T_FACA cmd* T_FIM_PARA ;
cmdEnquanto : T_ENQUANTO expressao T_FACA cmd* T_FIM_ENQUANTO ;
cmdFaca : T_FACA cmd* T_ATE expressao ;
cmdAtribuicao : T_CIRCUNFLEXO? identificador T_ATRIBUICAO expressao ;
cmdChamada : IDENT T_ABRE_PAR expressao (T_VIRGULA expressao)* T_FECHA_PAR ;
cmdRetorne : T_RETORNE expressao ;

selecao : item_selecao* ;

item_selecao : constantes T_DOISPONTOS cmd* ;

constantes : numero_intervalo (T_VIRGULA numero_intervalo)* ;

numero_intervalo : op_unario? NUM_INT (T_INTERVALO op_unario? NUM_INT)? ;

op_unario : T_MENOS ;

exp_aritmetica : termo (op1 termo)* ;

termo : fator (op2 fator)* ;

fator : parcela (op3 parcela)* ;

op1 : T_MAIS | T_MENOS ;
op2 : T_MULT | T_DIV ;
op3 : T_MOD ;

parcela : op_unario? parcela_unario | parcela_nao_unario ;

parcela_unario : T_CIRCUNFLEXO? identificador
               | IDENT T_ABRE_PAR expressao (T_VIRGULA expressao)* T_FECHA_PAR
               | NUM_INT
               | NUM_REAL
               | T_ABRE_PAR expressao T_FECHA_PAR ;

parcela_nao_unario : T_ENDERECO identificador | CADEIA ;

exp_relacional : exp_aritmetica (op_relacional exp_aritmetica)? ;

op_relacional : T_IGUAL | T_DIFERENTE | T_MAIOR_IGUAL | T_MENOR_IGUAL | T_MAIOR | T_MENOR ;

expressao : termo_logico (op_logico_1 termo_logico)* ;

termo_logico : fator_logico (op_logico_2 fator_logico)* ;
fator_logico : T_NAO? parcela_logica;
parcela_logica : ( T_VERDADEIRO | T_FALSO )
               | exp_relacional ;

op_logico_1 : T_OU ;
op_logico_2 : T_E ;