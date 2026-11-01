# GymTrack

Aplicativo Android nativo para registrar treinos de musculação. Desenvolvido em Kotlin, com persistência local e interface em português.

## Funcionalidades

- Fichas de treino A a E, com exercícios e nomes personalizáveis.
- Registro e acompanhamento de séries, cargas e exercícios concluídos.
- Histórico de sessões, gráfico de evolução por volume ou 1RM estimada e cronômetro de descanso.
- Exportação e restauração de backup JSON com verificação de integridade.
- Funcionamento offline; o manifesto não solicita permissão de acesso à internet.

## Requisitos

- Android Studio com suporte a projetos Android.
- JDK 17.
- Android SDK Platform 34 e ferramentas de compilação correspondentes.
- Dispositivo ou emulador Android 7.0 (API 24) ou superior para executar o app.

O projeto usa Gradle 8.7 pelo Gradle Wrapper e Android Gradle Plugin 8.3.2. As dependências são obtidas dos repositórios Google Maven e Maven Central.

## Instalação e configuração

1. Clone o repositório e abra a pasta do projeto no Android Studio:

   ```bash
   git clone <URL_DO_REPOSITORIO>
   cd gym-app
   ```

2. No Android Studio, aguarde a sincronização do Gradle e instale o Android SDK Platform 34 se solicitado. Se necessário, selecione o JDK 17 em **Settings > Build, Execution, Deployment > Build Tools > Gradle**.

3. Para compilar pela linha de comando, configure o caminho do Android SDK em `local.properties` (ou defina `ANDROID_HOME`/`ANDROID_SDK_ROOT`). Exemplo local:

   ```properties
   sdk.dir=/caminho/para/o/Android/Sdk
   ```

   `local.properties` é específico de cada computador e não deve ser enviado ao repositório. O app não exige chaves de API, serviços externos ou outras credenciais para compilar e executar.

## Compilar, testar e executar

Na raiz do projeto:

```bash
# Executar os testes unitários
./gradlew test

# Gerar APK de depuração
./gradlew assembleDebug

# Instalar em um dispositivo/emulador conectado via ADB
./gradlew installDebug
```

O APK de depuração gerado fica em `app/build/outputs/apk/debug/app-debug.apk`. As tarefas Gradle também podem ser executadas pelo painel **Gradle** do Android Studio.

## Segurança e arquivos locais

Dados e backups podem conter informações pessoais de treino. Não publique backups, arquivos de assinatura Android, chaves, credenciais, configurações locais ou artefatos de build. O `.gitignore` do projeto exclui esses arquivos comuns. O backup JSON inclui uma soma de verificação para validar sua integridade; ela não deve ser tratada como assinatura de origem confiável.

Para gerar uma versão de lançamento assinada, configure a assinatura em ambiente local seguro. Não armazene senhas ou arquivos de chave no código-fonte nem os envie ao GitHub.
