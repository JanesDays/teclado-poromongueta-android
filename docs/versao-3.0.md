# Teclado Poromongûetá — v3.0

## Funcionalidades
- Teclado físico virtual com layouts **PORO**, **ABC** e **SÍMBOLOS**
- Suporte a caracteres especiais da língua Poromongûetá (č̣, ẽ, ĩ, ã, ũ, õ, ng, mb, nd, etc.)
- **Key Preview**: bolha flutuante sobre a tecla ao pressionar (com animação scale/fade)
- **Popups de caracteres**: pressione e segure uma tecla para ver variantes (ex.: c → ç, ć, ĉ, č, č̣)
- **Barra de sugestões**: sugere palavras do dicionário + palavras aprendidas
- **Aprendizado**: palavras digitadas são salvas automaticamente e aparecem como sugestões
- Tecla Shift, tecla de espaço, tecla Enter, backspace com repeat

## Melhorias e Correções (v3.0)
- Ícone personalizado do teclado (extraído do .ico original, gerado para todas as densidades)
- Popup de preview não intercepta toques (isTouchable = false)
- Popup de preview posicionado corretamente dentro da janela do IME
- Botão X na MainActivity para fechar a tela de ajuda
- Corrigido popup de tecla longa — agora abre sobre a tecla em vez de (0,0)
- Corrigido crash no botão X (substituído recurso interno do Android por drawable próprio)
- Corrigido caractere č̣ para incluir combining dot (U+0323)
- Implementado onText() para processar caracteres multi-código corretamente
- Adicionado guard contra ArrayIndexOutOfBounds no preview
- Corrigidas constraints sobrepostas no layout da MainActivity

## Arquivos do Projeto
- pp/src/main/java/com/rick/tecladoporomongueta/PoromonguetaKeyboard.kt — IME principal
- pp/src/main/java/com/rick/tecladoporomongueta/CustomKeyboardView.kt — View do teclado com preview
- pp/src/main/java/com/rick/tecladoporomongueta/KeyPreviewPopup.kt — Popup de preview animado
- pp/src/main/java/com/rick/tecladoporomongueta/SuggestionManager.kt — Gerenciador de sugestões
- pp/src/main/java/com/rick/tecladoporomongueta/LearningManager.kt — Aprendizado de palavras
- pp/src/main/java/com/rick/tecladoporomongueta/DictionaryManager.kt — Dicionário estático
- pp/src/main/res/xml/key_poro.xml — Layout PORO
- pp/src/main/res/xml/key_abc.xml — Layout ABC
- pp/src/main/res/xml/key_symbols.xml — Layout símbolos
- pp/src/main/res/xml/popup_*.xml — Popups de caracteres (28 arquivos)

## Compatibilidade
- Android 8.0+ (API 24)
- MinSdk: 24
- TargetSdk: 34

## Observações
- KeyboardView/Keyboard (pacote android.inputmethodservice) são deprecated desde API 29 mas continuam funcionais
- Nenhuma permissão especial necessária
- Apenas português (Brasil)
