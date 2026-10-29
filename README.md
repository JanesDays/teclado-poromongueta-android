# Teclado Poromongûetá para Android

Teclado virtual para Android com suporte à língua indígena Poromongûetá, com foco em
preservação linguística, inclusão digital e acessibilidade.

## Sobre o projeto
Teclado customizado, nativo, baseado em `InputMethodService`, que permite digitar os
caracteres e as estruturas específicas da língua Poromongûetá (por exemplo û, î, ï, ã, ẽ, ỹ,
`'`, `mb`, `nd`, `ng`).

## Funcionalidades
- Três modos: Poromongûetá, alfabético (ABC) e símbolos.
- Pressione e segure uma tecla para ver as variações com caracteres especiais.
- Prévia da tecla pressionada (bolha animada).
- **Barra de sugestões** com palavras do dicionário Poromongûetá e das palavras que você já digitou.
  - A busca ignora maiúsculas e acentos: digitar `kuekatu` sugere `kûekatu`.
  - A sugestão mantém a caixa do que foi digitado (`Ana` → `Anaûé`).
- **Aprendizado local**: as palavras mais usadas passam a aparecer primeiro. Tudo fica só no aparelho.
- **Privacidade**: não há permissão de internet; o teclado não sugere nem aprende em campos de
  senha, e-mail ou endereço, nem quando o app pede modo anônimo. O histórico não vai para o backup na nuvem.
- Shift com toque simples (uma letra) e toque duplo (Caps Lock), mais maiúscula automática no início de frase.
- Tecla Enter respeita a ação do campo (Buscar, Enviar, Próximo...).
- Apagar com repetição ao segurar, vibração ao tocar nas teclas (segue a configuração do sistema).
- Tela inicial com atalhos para ativar e escolher o teclado, campo de teste e botão para limpar as palavras aprendidas.
- **Emoji**: painel com categorias e recentes (botão 😊 no topo do teclado).
- **Voz**: botão de microfone que usa o reconhecimento de voz do Android (o app não pede permissão de microfone).
  O reconhecimento é em português; a língua Poromongûetá ainda não é reconhecida por voz.
- **Próxima palavra**: depois de um espaço, sugere as palavras que você costuma digitar em seguida.
- **Meu dicionário**: consulta o dicionário, adiciona palavras próprias, importa, compartilha e salva arquivos de palavras.
  Formato de linha: `palavra<TAB>tradução<TAB>classe<TAB>exemplo` (só a palavra é obrigatória).
- Histórico de digitação **criptografado** no aparelho (AES-256, Android Keystore).
- **Trocar de teclado**: segure a barra de espaço para abrir a lista de teclados instalados (como no Gboard).
- **Configurações**: sugestões de palavras, vibração, altura das teclas (80% a 130%) e tema (claro, escuro ou seguir o sistema).

## Como instalar e usar
1. Abra o projeto no Android Studio e execute o módulo `app` (ou instale o APK).
2. Abra o app **Teclado Poromongueta** e toque em **Ativar teclado**; habilite-o na lista.
3. Toque em **Escolher teclado** e selecione o Poromongueta.

## Estrutura
| Caminho | Conteúdo |
|---|---|
| `app/src/main/java/.../PoromonguetaKeyboard.kt` | Serviço do teclado (entrada, shift, sugestões) |
| `.../PoroKeyboardView.kt`, `KeyboardLayout.kt`, `PopupKeysView.kt`, `KeyPreviewPopup.kt` | Teclado desenhado em Canvas, leitura dos `key_*.xml`, popups de long-press e prévia |
| `.../DictionaryManager.kt`, `UserDictionary.kt`, `LearningManager.kt`, `NextWordManager.kt`, `SuggestionManager.kt`, `EncryptedTextStorage.kt` | Dicionário, aprendizado criptografado e sugestões |
| `.../EmojiPanelView.kt`, `VoiceInputActivity.kt`, `DictionaryActivity.kt`, `SettingsActivity.kt`, `KeyboardSettings.kt` | Emoji, voz, tela do dicionário e configurações |
| `app/src/main/res/xml/key_*.xml`, `popup_*.xml` | Layouts das teclas e dos popups |
| `app/src/main/assets/dictionary.txt` | Dicionário base (uma palavra por linha, UTF-8 sem BOM) |
| `docs/` | Imagens de referência do layout e notas de versão |
| `design/` | Ícones e artes originais |

## Testes
```
./gradlew testDebugUnitTest
```
Cobrem normalização de texto, dicionário e sugestões.

## Documentos
- [Versão 3.1: artigo completo](docs/versao-3.1.md)
- [Versão 3.0: notas](docs/versao-3.0.md)
- [CHANGELOG](CHANGELOG.md)
- [Como adicionar palavras](docs/COMO_ADICIONAR_PALAVRAS.md)
- [Política de privacidade](PRIVACY.md)

## Status do projeto
Versão 3.1, funcional e em testes no celular. Ainda não publicada na Play Store.

## Licença
Projeto de uso educacional e cultural. Uma licença formal ainda precisa ser definida junto à comunidade.
