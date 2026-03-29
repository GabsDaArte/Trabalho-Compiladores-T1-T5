# Trabalho-Compiladores-T1-T5
###### Membros: 
824391 - Gabriel Somensi Duarte

820744 - Griselda Karen Sillerico Justo

## Pré-requisitos
Para compilar e executar este projeto, você precisará ter instalado em sua máquina:
* **Java SDK** (versão 11 ou superior - Recomendado 17)
* **Maven** (versão 3.6 ou superior)

## Como Compilar
1. Abra o terminal.
2. Navegue até a pasta raiz do projeto (onde se encontra o arquivo `pom.xml`).
3. Execute o comando abaixo para limpar builds anteriores, gerar as classes do ANTLR e compilar o código Java:

`mvn clean compile`

## Como Executar
O programa requer obrigatoriamente dois argumentos passados via linha de comando:
1. O caminho completo para o arquivo de texto com o código-fonte de entrada.
2. O caminho completo para o arquivo de texto onde a saída (tokens ou erros) será salva.

Para executar utilizando o Maven, utilize o comando abaixo, substituindo os caminhos pelos diretórios reais da sua máquina:

`mvn exec:java -Dexec.mainClass="br.ufscar.dc.compiladores.la.Principal" -Dexec.args="C:\caminho\para\entrada.txt C:\caminho\para\saida.txt"`
