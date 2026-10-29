# Changelog

## 3.1
### Segurança
- Histórico de palavras e previsão da próxima palavra em arquivos criptografados (AES-GCM, Android Keystore); o histórico antigo (texto puro) é migrado e apagado.
- Só aprende palavras formadas por letras (nada com números), e só sugere uma palavra aprendida depois de 2 usos.
- Em campos de senha, e-mail e URL o teclado nem acompanha o texto digitado.
- Backup desativado (`allowBackup="false"`).
- `.gitignore` protege chaves de assinatura; dependências atualizadas.

### Novo
- Tema claro, escuro ou seguindo o sistema (Configurações). O padrão é o escuro, como era antes.
- `docs/COMO_ADICIONAR_PALAVRAS.md`: formato do arquivo de palavras (com tradução) e como importar.
- Segurar a barra de espaço abre o seletor de teclados instalados (como no Gboard), para trocar de teclado.
- Tela de Configurações: sugestões de palavras, vibração e altura das teclas (80% a 130%).
- O espaço inferior do teclado segue a barra de navegação do sistema, em vez de 50dp fixos.
- Corrigido o desalinhamento: linhas com `keyWidth` próprio (como a `a s nd g î k ï`) agora ocupam a largura certa, e o espaçamento segue o teclado antigo.
- Painel de emoji (categorias, recentes), botão no topo do teclado.
- Digitação por voz pelo reconhecimento de voz do sistema (sem permissão de microfone).
- Previsão da próxima palavra (aprendida só no aparelho).
- Tela "Meu dicionário": consulta, palavras próprias, importar, compartilhar e salvar arquivo.
- Formato de dicionário com tradução, classe e exemplo (colunas separadas por tabulação, opcionais).
- Apagar remove a letra com todos os acentos de uma vez; apagar contínuo acelera.
- Deslizar sobre uma tecla até a variante e soltar também seleciona.

### Alterado
- O `KeyboardView` (obsoleto) foi substituído por uma view própria (`PoroKeyboardView`), que lê os mesmos `key_*.xml` e `popup_*.xml`.
- Teclas com mais de um código (como `č̣`) agora digitam todos os caracteres.

## 1.1
### Adicionado
- Shift com Caps Lock (toque duplo) e maiúscula automática no início de frase.
- Busca de sugestões sem distinção de acentos (`kuekatu` encontra `kûekatu`).
- Sugestão preserva a caixa digitada.
- Tela inicial com ativação guiada, campo de teste e botão para limpar palavras aprendidas.
- Vibração ao tocar nas teclas.
- Testes unitários de normalização, dicionário e sugestões.
- `PRIVACY.md` e `CHANGELOG.md`.

### Corrigido
- A primeira palavra do dicionário nunca era sugerida (BOM invisível no arquivo).
- Palavra atual desincronizada quando o cursor mudava por fora do teclado.
- Enter ignorava a ação do campo (Buscar, Enviar...) e não enviava a soltura da tecla.
- Teclado aprendia palavras de campos de senha, e-mail e URL.
- Apagar não removia texto selecionado.
- Popups ficavam abertos ao esconder o teclado.

### Alterado
- Dicionário com busca binária; aprendizado com cache em memória e limite de 2000 palavras.
- Códigos de tecla nomeados em `KeyCodes`.
- O histórico de palavras fica fora do backup na nuvem.
- Release com R8 e redução de recursos.
- Imagens de referência movidas para `docs/layout-referencia/`; artes para `design/`.

Os layouts das teclas e os caracteres especiais não foram alterados.

## 1.0
Ver [docs/versao-1.0.md](docs/versao-1.0.md).
