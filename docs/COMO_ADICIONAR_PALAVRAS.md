# Como adicionar palavras ao Teclado Poromongueta

O teclado sugere palavras enquanto a pessoa digita. As sugestões vêm de três lugares, nesta ordem:

1. **Palavras do usuário** (adicionadas ou importadas no app).
2. **Palavras aprendidas** (as que a pessoa já digitou 2 vezes ou mais).
3. **Dicionário do app** (`app/src/main/assets/dictionary.txt`).

## Formato do arquivo

Um arquivo de texto simples (UTF-8), **uma palavra por linha**. Só a palavra é obrigatória. Se quiser guardar mais informações, acrescente colunas separadas por **TAB** (a tecla Tab, não espaços):

```
palavra <TAB> tradução <TAB> classe gramatical <TAB> exemplo
```

Exemplo:

```
# Linhas que começam com # são comentários
kûekatu	bom, bonito
karuk	comida	substantivo
ka'a
```

- Pode usar as letras especiais do alfabeto (`û`, `î`, `ï`, `ỹ`, `õ`, `'` etc.).
- A barra de sugestões mostra só a palavra em Poromonguetá. As colunas extras (tradução, classe, exemplo) ficam guardadas e aparecem na tela "Meu dicionário".
- Linhas em branco são ignoradas.

## Opção 1: importar pelo app (sem mexer no código)

1. O pesquisador envia o arquivo `.txt` ou `.tsv` no formato acima.
2. No app: **Meu dicionário** > **Importar** e escolher o arquivo.
3. As palavras passam a ser sugeridas e ficam só naquele aparelho (até 5000 palavras).

Para repassar a outras pessoas: **Meu dicionário** > **Compartilhar** ou **Salvar arquivo** gera o mesmo formato.

## Opção 2: colocar no dicionário do app (vale para todos)

1. Abra `app/src/main/assets/dictionary.txt`.
2. Acrescente as linhas no mesmo formato (pode misturar palavras com e sem tradução).
3. Gere uma nova versão do app (aumente `versionCode` e `versionName` em `app/build.gradle`).

Dica: peça ao pesquisador para conferir a grafia antes de entrar no dicionário do app, porque ela vai para todos os usuários.
