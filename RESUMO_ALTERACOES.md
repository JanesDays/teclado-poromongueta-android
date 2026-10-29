# Resumo das Alterações — Teclado Poromongueta

## Commits enviados

### `a0cf45b` — Sistema de Sugestões Inteligentes
**11 arquivos alterados, +661 linhas**

#### Arquivos criados
| Arquivo | Descrição |
|---------|-----------|
| `app/src/main/assets/dictionary.txt` | Dicionário base com ~242 palavras do Poromonguetá extraída |
| `DictionaryManager.kt` | Carrega dicionário 1x em memória. Busca por `startsWith()` case-insensitive |
| `LearningManager.kt` | SharedPreferences: salva palavras, conta frequência de uso |
| `SuggestionManager.kt` | Combina dicionário + aprendidas, ordena por frequência |
| `SuggestionBarView.kt` | View personalizada com 3 slots de sugestão clicáveis |
| `res/layout/suggestion_bar.xml` | Layout da barra (3 TextViews com divisórias) |

#### Arquivos modificados
| Arquivo | O que mudou |
|---------|-------------|
| `PoromonguetaKeyboard.kt` | Integração completa: `currentWord`, managers, barra de sugestões, substituição parcial |
| `keyboard_view.xml` | Agora é um LinearLayout com SuggestionBarView + CustomKeyboardView |
| `key_poro.xml` | Tecla `!#1` corrigida de `-10` para `-12` (abre símbolos). 'p' duplicado removido da linha 2 |
| `key_abc.xml` | Sem alterações funcionais |
| `key_symbols2.xml` | **Bugfix**: tag `<Key` faltando na linha 72 (crash ao abrir página 2) |

#### Correções aplicadas
- `applySuggestion` agora salva a palavra escolhida (não o prefixo digitado)
- `Handler(Looper.getMainLooper())` — compatível com Android 15
- `currentWord` reseta sempre ao iniciar campo (removeu `if !restarting`)
- `suggestDelayed()` e `extractLastWord()` — código morto removido
- `dictionary.txt` deduplicado (306 → 242 palavras únicas)

### `37b9e7e` — Imagens de Referência
**9 imagens de layout do teclado adicionadas**

---

## Funcionalidades Implementadas

1. **Barra de Sugestões** — 3 slots acima do teclado, atualiza em tempo real
2. **Autocomplete por prefixo** — `startsWith()` ignorando maiúsculas/minúsculas
3. **Substituição parcial** — tocar na sugestão remove o prefixo e insere a palavra + espaço
4. **Detecção da palavra atual** — `currentWord` atualizado ao digitar, apagar, espaço, enter ou pontuação
5. **Aprendizado local** — SharedPreferences salva palavras ao finalizar (2+ caracteres)
6. **Priorização por frequência** — palavras mais usadas aparecem primeiro
7. **`'` e `-` tratados como parte da palavra** — essencial para `po'õ`, `mba'e`, `mo-îatïk`
8. **Performance** — dicionário carregado 1x em `onCreate()`

---

### `874d5a1` — v1.0: KeyPreviewPopup, ícone personalizado e correções
**34 arquivos alterados, +313 linhas**

#### Arquivos criados
| Arquivo | Descrição |
|---------|-----------|
| `KeyPreviewPopup.kt` | PopupWindow reutilizável com animação scale/fade para preview de tecla |
| `res/drawable/ic_close.xml` | Vector drawable do X (substitui `@android:drawable/ic_menu_close_clear_cancel`) |
| `res/drawable/key_preview_bg.xml` | Background arredondado (`#3A3A3A`, raio 10dp) do preview |
| `res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png` | Ícone personalizado (extraído do .ico original) |
| `res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher_round.png` | Ícone redondo |
| `versao-1.0.md` | Documentação de versão (hoje `docs/versao-3.0.md`) |

#### Arquivos modificados
| Arquivo | O que mudou |
|---------|-------------|
| `CustomKeyboardView.kt` | KeyPreviewPopup integrado via `onTouchEvent` (ACTION_DOWN/MOVE/UP); long-press popup reposicionado sobre a tecla (não mais em 0,0); import `Gravity` e `View` adicionados |
| `MainActivity.kt` | Botão X (`btnFechar`) wired a `finish()` |
| `PoromonguetaKeyboard.kt` | `onText()` implementado (commitText) para caracteres multi-código |
| `ic_launcher_foreground.xml` | Alterado para `<bitmap>` referenciando mipmap PNG |
| `activity_main.xml` | `src` trocado para `@drawable/ic_close`; constraints corrigidas (criadorProjeto → subtitulo chain) |
| `key_preview.xml` | Layout do popup (TextView 32sp bold) |
| `popup_c.xml` | `č̣` agora com `codes="269,803"` (adicionado combining dot U+0323) |

#### Arquivos removidos
| Arquivo | Motivo |
|---------|--------|
| `res/mipmap-anydpi-v26/ic_launcher.xml` | Ícone adaptativo removido (causava loop de referência) |
| `res/mipmap-anydpi-v26/ic_launcher_round.xml` | Idem |
| `res/mipmap-*/ic_launcher.webp` | Substituído por PNGs do ícone personalizado |
| `res/mipmap-*/ic_launcher_round.webp` | Substituído por PNGs do ícone personalizado |

#### Correções aplicadas
- Preview não crasha com `codes` vazio (guard `key.codes.isEmpty()`)
- Popup de tecla longa abre na posição da tecla pressionada
- `onText()` agora comita o texto recebido (antes era ignorado)
- Botão X não crasha em Android 14+ (drawable próprio em vez de recurso interno)
- `č̣` insere o caractere completo com combining dot abaixo
- Constraints do activity_main.xml não sobrepõem mais os textos
- `.gitignore` agora ignora `*.apk` e `*.ico`

---

## V3.1 — Reescrita do teclado, emoji, voz, segurança e configurações
Commitada e enviada ao GitHub (`origin/main`). Artigo completo em `docs/versao-3.1.md`. Testes do Claude só no emulador (`emulator-5554`); o celular físico nunca recebe instalação feita pelo Claude. Rick testou o APK no celular e aprovou (altura, barra de navegação, emojis e o resto).

### Segurança
- Histórico de palavras e próxima palavra em arquivos criptografados (AES-256-GCM, Android Keystore); histórico antigo em texto puro é migrado e apagado.
- Só aprende palavras só de letras (sem números), e só sugere depois de 2 usos.
- Campos de senha, e-mail e URL: o teclado nem acompanha o texto.
- `allowBackup="false"`; `.gitignore` protege chaves de assinatura (`*.jks`, `*.keystore`, `keystore.properties` etc.).
- Minify e shrinkResources ligados no release (R8 testado).
- `PRIVACY.md` criado; o contato está como `[DEFINIR E-MAIL DE CONTATO DO PROJETO]`.

### Teclado
- `KeyboardView` (obsoleto) substituído por um teclado próprio em Canvas: `KeyboardLayout`, `KeyPainter`, `PoroKeyboardView`, `PopupKeysView`. Lê os mesmos `key_*.xml` e `popup_*.xml`, **que não foram alterados** (letras especiais intactas).
- Multitoque, repetição ao apagar, popup de tecla longa, deslizar para escolher, Shift/Caps Lock, auto-maiúscula, ação do Enter do campo.
- **Alinhamento corrigido:** o teclado antigo respeitava `keyWidth` na `<Row>` (a linha `a s nd g î k ï` usa `14%p`) e o novo ignorava. Agora respeita. Linhas que cabem deixam o espaço antes de cada tecla; linhas que passam da largura encolhem até a tela inteira.
- Espaço inferior segue a barra de navegação (insets) + 12dp, mínimo 28dp (`MIN_BOTTOM_DP` em `PoromonguetaKeyboard.kt`), no lugar dos 50dp fixos.
- Tecla do globo (trocar de teclado) fica na barra do sistema.
- **Segurar o espaço** abre o seletor de teclados (`onSpaceLongPress` → `showInputMethodPicker`), pedido do antropólogo do projeto; referência em `docs/referencia/Muda-teclado.jpeg` (seletor do Gboard). Testado no emulador.

### Recursos novos
- **Emoji:** painel com categorias e recentes (`EmojiRepository`, `EmojiPanelView`, `assets/emoji.txt`).
- **Voz:** reconhecimento de voz do sistema, sem permissão de microfone (`VoiceInputActivity`, `VoiceBridge`).
- **Próxima palavra:** `NextWordPredictor`, `NextWordManager`.
- **Meu dicionário:** `DictionaryActivity` (buscar, adicionar, importar, compartilhar, salvar arquivo; até 5000 palavras).
- **Configurações:** `SettingsActivity` + `KeyboardSettings` (sugestões ligadas/desligadas, vibração, altura das teclas de 80% a 130%).
- **Tema** claro / escuro / seguir o sistema (padrão: escuro). Cores em `values/colors.xml` e `values-night/colors.xml` (`poro_*`); o teclado usa um contexto com o tema escolhido (`themedContext`), refeito ao abrir se o tema mudou. Testado nos dois temas no emulador.
- A barra de sugestões mostra **só as palavras** em Poromonguetá, sem tradução (decisão do Rick: a tradução chegou a ser feita e foi removida).
- Busca ignora acentos (ex.: `kue` acha `kûekatu`), mantendo as letras especiais.

### Projeto
- 47+ testes unitários passando; lint com 0 erros.
- Ícone adaptativo (fundo creme `#FFF8EC`); imagens movidas para `docs/layout-referencia/`; arte em `design/`.
- Docs: `README.md`, `CHANGELOG.md`, `PRIVACY.md`.
- Versão no `build.gradle`: `versionName "3.1"`, `versionCode 4`.
- Decisão do Rick: manter o `applicationId` `com.rick.tecladoporomongueta` e o nome dele no código.

### Sobre o dicionário
- A função principal é **sugerir palavras prontas enquanto se digita** (já funciona, a partir de `dictionary.txt`). A tela "Meu dicionário" é só um extra para consultar e adicionar palavras.
- Decisão do Rick: não colocar referências/créditos do autor; o alfabeto veio do pesquisador e antropólogo MBo'esara Esãîã Tremembé, parceiro do projeto.

### Lições para as próximas sessões
- Compilar com `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"` e `./gradlew testDebugUnitTest assembleDebug lintDebug`.
- Nas edições de código, evitar escapes unicode escritos à mão; o BOM usa `TextNormalizer.BOM`.
- No emulador, para o teclado aparecer: `settings put secure show_ime_with_hard_keyboard 1` e tocar no campo de texto.
- A `SettingsActivity` e a `DictionaryActivity` não são exportadas: só abrem pelos botões do app.

## Próximos Passos Possíveis
**Precisam de decisão ou ação do Rick**
- [ ] Definir o e-mail de contato em `PRIVACY.md`.
- [ ] Escolher a licença do projeto.
- [ ] Conferir os direitos e o conteúdo do dicionário (origem: PDF).
- [ ] Gerar a chave de assinatura (keystore) e o AAB assinado.
- [ ] Ficha da Play Store: textos, capturas, política de privacidade publicada.

**Melhorias técnicas**
- [ ] Receber do pesquisador a lista de palavras e importar (guia em `docs/COMO_ADICIONAR_PALAVRAS.md`).
- [ ] Abrir contribuição da comunidade para o dicionário.
- [ ] Testes de interface (Espresso) e testar em mais tamanhos de tela e rotação.
- [ ] Revisar o alinhamento do painel de emoji com a barra de navegação em outros aparelhos.
- [ ] Conferir o tema claro em um celular de verdade (barra de navegação, contraste das teclas).
- [ ] Mais opções de configuração (intensidade da vibração, tamanho da letra).
