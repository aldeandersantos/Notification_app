# Notificação App

Um aplicativo Flutter para capturar notificações de aplicativos selecionados e enviá-las para um webhook.

## Funcionalidades

- Lista todos os aplicativos instalados no dispositivo
- Permite selecionar quais aplicativos terão suas notificações monitoradas
- Configuração de URL do webhook para envio das notificações
- Captura notificações em tempo real
- Interface amigável e intuitiva

## Como usar

1. Instale o aplicativo
2. Ao abrir pela primeira vez, clique no botão de segurança (ícone de cadeado) no canto inferior direito
3. Nas configurações do sistema que serão abertas:
   - Conceda permissão para acessar as notificações
   - Ative o "Acesso às notificações" para este aplicativo
4. Volte ao aplicativo
5. Clique no ícone de link na barra superior para configurar a URL do webhook
6. Selecione os aplicativos que você deseja monitorar marcando as caixas de seleção

## Formato das notificações

As notificações são enviadas para o webhook no seguinte formato JSON:

```json
{
  "packageName": "com.exemplo.app",
  "title": "Título da notificação",
  "text": "Texto da notificação",
  "timestamp": 1234567890
}
```

## Requisitos

- Android 5.0 (API 21) ou superior
- Permissão de acesso às notificações
- Conexão com a internet para envio das notificações

## Desenvolvimento

Este aplicativo foi desenvolvido usando:

- Flutter
- Kotlin para o serviço de notificações Android
- Bibliotecas:
  - device_apps: ^2.2.0
  - http: ^1.1.0
  - shared_preferences: ^2.2.2
  - permission_handler: ^11.0.1
