lexer grammar LALexer;

// Palavras chave
T_ALGORITMO : 'algoritmo';
T_DECLARE : 'declare';
T_LITERAL : 'literal';
T_INTEIRO : 'inteiro';
T_REAL : 'real';
T_LOGICO : 'logico';
T_LEIA : 'leia';
T_ESCREVA : 'escreva';
T_FIM_ALGORITMO : 'fim_algoritmo';
T_SE : 'se';
T_ENTAO : 'entao';
T_SENAO : 'senao';
T_FIM_SE : 'fim_se';
T_ENQUANTO : 'enquanto';
T_FACA : 'faca';
T_FIM_ENQUANTO : 'fim_enquanto';
T_PARA : 'para';
T_ATE : 'ate';
T_FIM_PARA : 'fim_para';
T_PROCEDIMENTO : 'procedimento';
T_FIM_PROCEDIMENTO : 'fim_procedimento';
T_FUNCAO : 'funcao';
T_FIM_FUNCAO : 'fim_funcao';
T_RETORNE : 'retorne';
T_TIPO : 'tipo';
T_REGISTRO : 'registro';
T_FIM_REGISTRO : 'fim_registro';
T_CONSTANTE : 'constante';
T_FALSO : 'falso';
T_VERDADEIRO : 'verdadeiro';
T_NAO : 'nao';
T_E : 'e';
T_OU : 'ou';
T_CASO : 'caso';
T_SEJA : 'seja';
T_FIM_CASO : 'fim_caso';
T_VAR : 'var';

// Operadores e Delimitadores
T_MAIOR_IGUAL : '>=';
T_MENOR_IGUAL : '<=';
T_DIFERENTE : '<>';
T_MAIOR : '>';
T_MENOR : '<';
T_IGUAL : '=';
T_MAIS : '+';
T_MENOS : '-';
T_MULT : '*';
T_DIV : '/';
T_MOD : '%';

T_DOISPONTOS : ':';
T_VIRGULA : ',';
T_PONTO_VIRGULA : ';';
T_INTERVALO : '..';
T_PONTO : '.';
T_ABRE_COL : '[';
T_FECHA_COL : ']';
T_CIRCUNFLEXO : '^';
T_ENDERECO : '&';
T_ATRIBUICAO : '<-';
T_ABRE_PAR : '(';
T_FECHA_PAR : ')';

// REgras léxicas básicas e tokens (Trabalho 1)
fragment DIGITO: '0'..'9';

NUM_INT: DIGITO+;
NUM_REAL: DIGITO+ '.' DIGITO+;

IDENT: [a-zA-Z][a-zA-Z0-9_]*;

CADEIA: '"' ~('\r'|'\n'|'"')* '"';

COMENTARIO: '{' ~('\r'|'\n'|'}')* '}' { skip(); };

WS: (' '|'\t'|'\r'|'\n') { skip(); };

// Tratamento para erros
COMENTARIO_NAO_FECHADO: '{' ~('\r'|'\n'|'}')* ('\r'? '\n' | EOF) ;
CADEIA_NAO_FECHADA: '"' ~('\r'|'\n'|'"')* ('\r'? '\n' | EOF) ;
ERRO: . ;