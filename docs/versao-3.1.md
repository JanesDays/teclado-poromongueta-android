# Teclado Poromongûetá — versão 3.1

Artigo de consulta sobre tudo o que mudou nesta versão: o que é, por que foi feito, como funciona e onde está no código. Para a lista curta de mudanças veja o [CHANGELOG](../CHANGELOG.md).

- **Versão:** 3.1 (`versionName "3.1"`, `versionCode 4`)
- **Pacote:** `com.rick.tecladoporomongueta`
- **Android:** mínimo 7.0 (API 24), compilado para o Android 16 (API 36)
- **Idealização do projeto:** MBo'esara Esãîã Tremembé
- **Desenvolvimento:** Rick Janes Matos Dias

## Em uma frase

A 3.1 troca a base do teclado por uma mais moderna e segura, corrige o desalinhamento das fileiras e acrescenta emoji, voz, próxima palavra, dicionário do usuário, configurações, tema claro/escuro e a troca de teclado segurando o espaço, **sem alterar nenhuma letra especial nem o layout das teclas**.

## 1. O que não mudou (de propósito)

Os arquivos `key_poro.xml`, `key_abc.xml`, `key_symbols*.xml` e todos os `popup_*.xml` ficaram **exatamente como estavam**. As letras da língua (`û`, `î`, `ï`, `ỹ`, `õ`, `č̣` com o ponto embaixo, `mb`, `nd`, `ng`, `'`) e os popups de tecla longa continuam iguais. Tudo foi construído em volta deles.

## 2. Teclado novo por baixo

O teclado usava a classe `KeyboardView`, que o Android marcou como obsoleta. Ela foi substituída por um teclado desenhado pelo próprio app:

| Peça | Função |
|---|---|
| `KeyboardLayout.kt` | Lê os mesmos `key_*.xml` e calcula a posição de cada tecla |
| `KeyPainter.kt` | Desenha as teclas |
| `PoroKeyboardView.kt` | Toques (inclusive vários dedos ao mesmo tempo), repetição ao apagar, toque longo, deslizar para escolher no popup |
| `PopupKeysView.kt` | O popup com as variações de uma letra |
| `KeyCodes.kt` | Os códigos das teclas especiais, em um só lugar |

Por que mudar: a classe antiga pode sumir em versões futuras do Android e a Play Store exige apps atualizados. A nova lê o mesmo formato, então o resultado visual é o mesmo.

### O desalinhamento (corrigido)

Na primeira versão da nova base, a fileira `a s nd g î k ï` ficava estreita e encostada à esquerda. A causa: o teclado antigo respeitava `keyWidth="14%p"` escrito na própria fileira, e o novo ignorava. Agora respeita. Também foi copiado o jeito do teclado antigo de espaçar as teclas: fileira que cabe mantém o tamanho, e fileira que passa da largura encolhe até ocupar a tela.

### Altura e barra de navegação

Antes havia 50dp fixos embaixo do teclado. Agora o espaço acompanha a barra de navegação do aparelho (mínimo de 28dp), para os botões do sistema não cobrirem as letras. Verificado no celular: não cobre.

## 3. Novidades para quem digita

- **Segurar a barra de espaço** abre a lista de teclados instalados (igual ao Gboard), para trocar de teclado. A tecla do globo também fica na barra do sistema. Pedido do antropólogo do projeto.
- **Emoji:** botão 😊 no topo; painel com categorias e recentes.
- **Voz:** botão de microfone usando o reconhecimento de voz do Android. O app não pede permissão de microfone. O reconhecimento é em português; a língua Poromongûetá ainda não é reconhecida por voz.
- **Próxima palavra:** depois do espaço, sugere palavras que a pessoa costuma digitar em seguida.
- **Sugestões que ignoram acentos:** digitar `kue` sugere `kûekatu`.
- **Meu dicionário:** tela para consultar o dicionário, adicionar palavras, importar, compartilhar e salvar arquivos (até 5000 palavras próprias).
- **Configurações:** sugestões ligadas/desligadas, vibração, altura das teclas (80% a 130%) e tema (claro, escuro ou seguir o sistema; padrão escuro).
- **Tema claro/escuro:** vale para o app e para o teclado, com as cores em `values/colors.xml` e `values-night/colors.xml` (nomes `poro_*`).

A barra de sugestões mostra **só as palavras em Poromongûetá**. Uma versão com a tradução embaixo da sugestão chegou a ser feita e foi removida a pedido.

## 4. Segurança e privacidade

- O histórico de palavras e a previsão da próxima palavra ficam em arquivos **criptografados** (AES-256-GCM, chave no Android Keystore). O histórico antigo, em texto puro, é migrado e apagado. Se a chave falhar, o dado é descartado e o teclado continua funcionando.
- Só aprende palavras formadas por letras (nada com números) e só sugere uma palavra aprendida depois de 2 usos.
- Em campos de senha, e-mail e endereço, o teclado nem acompanha o que é digitado.
- `allowBackup="false"`: o histórico não vai para o backup na nuvem.
- O app não tem permissão de internet.
- Versão de publicação com minificação e redução de recursos (R8) ligadas.
- `.gitignore` protege chaves de assinatura (`*.jks`, `*.keystore`, `keystore.properties`).
- Política de privacidade em [PRIVACY.md](../PRIVACY.md) (falta definir o e-mail de contato).

## 5. Como o dicionário funciona

As sugestões vêm de três fontes, nesta ordem: palavras do usuário, palavras aprendidas e o dicionário do app (`app/src/main/assets/dictionary.txt`, ~242 palavras). Para acrescentar palavras, veja [COMO_ADICIONAR_PALAVRAS.md](COMO_ADICIONAR_PALAVRAS.md).

## 6. Qualidade

- 47+ testes automáticos (normalização de texto, dicionário, sugestões, frequência, próxima palavra, dicionário do usuário e cálculo do layout do teclado). Rode com `./gradlew testDebugUnitTest`.
- Lint sem erros.
- Testado no emulador (Android 16) e no celular do Rick.

## 7. Como gerar o APK

```
./gradlew assembleDebug
```
O arquivo sai em `app/build/outputs/apk/debug/app-debug.apk` (APK de teste, instala direto). Para a Play Store será preciso uma chave de assinatura e um pacote AAB.

## 8. O que ainda falta

- E-mail de contato na política de privacidade.
- Escolher a licença do projeto.
- Conferir os direitos e o conteúdo do dicionário.
- Receber do pesquisador a lista maior de palavras.
- Chave de assinatura, AAB e ficha da Play Store.
- Testar o tema claro e outros tamanhos de tela em mais aparelhos.
