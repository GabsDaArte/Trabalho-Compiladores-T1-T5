lexer grammar LALexer;

// Palavras Chave
PALAVRA_CHAVE:
    'algoritmo' | 'declare' | 'literal' | 'inteiro' | 'leia' | 'escreva' | 'fim_algoritmo' |

    // Palavras chave para trabalhos futuros
    'real' | 'logico' | 'se' | 'entao' | 'senao' | 'fim_se' |
    'enquanto' | 'faca' | 'fim_enquanto' |
    'para' | 'ate' | 'fim_para' |
    'procedimento' | 'fim_procedimento' |
    'funcao' | 'fim_funcao' | 'retorne' |
    'tipo' | 'registro' | 'fim_registro' |
    'constante' | 'falso' | 'verdadeiro' | 'nao' | 'e' | 'ou' ;

fragment
// Dígito numérico
DIGITO: '0'..'9';

// Números inteiros
NUMINT: ('+'|'-')? DIGITO+;

// Números reais
NUMREAL: ('+'|'-')? DIGITO+ '.' DIGITO+;

// Variáveis
IDENT: [a-zA-Z][a-zA-Z0-9_]*;

// Strings
CADEIA: '"' ~('\n'|'\r'|'"')* '"';

// Skip para comentários fechados corretamente
COMENTARIO: '{' ~('\n'|'\r'|'}')* '}' { skip(); };

// Skip de espaços em branco
WS: (' '|'\t'|'\r'|'\n') { skip(); };

// Operadores Relacionais
OP_REL: '>' | '>=' | '<' | '<=' | '<>' | '=' ;

// Operadores Aritméticos
OP_ARIT: '+' | '-' | '*' | '/' ;

// Delimitadores
DELIM: ':' | ',' | ';' | '.' ;

// Abre Parênteses
ABREPAR: '(';

// Fecha Parênteses
FECHAPAR: ')';

// Tratamento de Erros Léxicos
// Procura chaves que não fecham na mesma linha
COMENTARIO_NAO_FECHADO: '{' ~('\n'|'\r'|'}')* '\n';

// Procura aspas que não fecham na mesma linha
CADEIA_NAO_FECHADA: '"' ~('\n'|'\r'|'"')* '\n';

// Captura símbolos inválido (ex: ~ , @ , #)
ERRO: .;