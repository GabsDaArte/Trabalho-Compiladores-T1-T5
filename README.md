# Trabalho-Compiladores-T1-T5
###### Membros: 
824391 - Gabriel Somensi Duarte

820744 - Griselda Karen Sillerico Justo

## Pré-requisitos
Para compilar e executar este projeto, você precisará ter instalado em sua máquina:
* **Java SDK** (versão 11 ou superior - Recomendado 17)
* **Maven** (versão 3.6 ou superior)

## Como Compilar
A compilação e a geração do arquivo executável (`.jar`) são feitas pelo Maven.

1. Abra o terminal.
2. Navegue até a pasta raiz do projeto (onde se encontra o arquivo `pom.xml`).
3. Execute o comando abaixo para limpar builds anteriores, gerar as classes do ANTLR e empacotar o compilador:

`mvn clean package`

Após o sucesso deste comando, um arquivo chamado `Trabalho-Compiladores-1.0-SNAPSHOT-jar-with-dependencies.jar` será gerado dentro da pasta `target/`.

## Como Executar
O programa requer obrigatoriamente dois argumentos passados via linha de comando:
1. O caminho completo para o arquivo de texto com o código-fonte de entrada.
2. O caminho completo para o arquivo de texto onde a saída (tokens ou erros) será salva.

Para executar o analisador, utilize o comando abaixo:

`java -jar target/Trabalho-Compiladores-1.0-SNAPSHOT-jar-with-dependencies.jar entrada.txt saida.txt`

*(Nota: Substitua `entrada.txt` e `saida.txt` pelos caminhos dos arquivos na sua máquina).*