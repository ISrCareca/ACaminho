# ACaminho (nome provisório) — App TVDE

Projeto de Laboratório de Programação. Plataforma TVDE com app de passageiro, app de motorista e painel de administração.
Stack: Android nativo (Kotlin + Jetpack Compose), Firebase (Auth + Firestore), arquitetura MVVM.

Referência visual dos ecrãs: `docs/mockup.png`.

## Estado: Fase 1 — Fundação

- [x] Estrutura do projeto, tema e navegação
- [x] Registo e login (email + palavra-passe), com escolha Passageiro / Motorista
- [x] Perfil guardado no Firestore e encaminhamento por papel (passageiro, motorista, admin)
- [x] Motorista: interruptor Online/Offline guardado no Firestore
- [ ] Fase 2: passageiro (mapa, origem/destino, estimativa de preço, pedido)
- [ ] Fase 3: motorista (receber pedido, aceitar/rejeitar, navegação integrada)
- [ ] Fase 4: financeiro (resumo, ganhos, custos, rentabilidade)
- [ ] Fase 5: administração
- [ ] Fase 6: validações, testes e demonstração

## Como arrancar

1. Instala o Android Studio (versão recente, com JDK 17).
2. Abre a pasta do projeto (`File > Open`). Se o Android Studio pedir, aceita descarregar o Gradle 8.9 e sincroniza.
3. Cria o projeto no [Firebase Console](https://console.firebase.google.com):
   - Adiciona uma app Android com o package `pt.kaminho.app`.
   - Descarrega o `google-services.json` e coloca-o em `app/` (está no `.gitignore`, não o publiques).
   - Em **Authentication > Sign-in method**, ativa **Email/Password**.
   - Em **Firestore Database**, cria a base de dados e, em **Rules**, cola o conteúdo de `firestore.rules`.
4. Sincroniza o Gradle e corre a app num emulador ou telemóvel.

## Estrutura

```
app/src/main/java/pt/ACaminho/app/
├── data/
│   ├── model/        User, Driver, Vehicle, Place, Trip
│   ├── repository/   AuthRepository, DriverRepository
│   └── firebase/     tradução de erros do Firebase
├── navigation/       AppNavigation (rotas e redirecionamento por papel)
├── ui/               auth, passenger, driver, admin, components, theme
├── viewmodel/        AuthViewModel, DriverViewModel
└── MainActivity.kt
```

## Notas

- O administrador não se regista pela app: cria-se à mão no Firestore (`users/{uid}` com `role: "ADMIN"`) e as regras impedem que alguém se atribua esse papel.
- Sem `google-services.json` o projeto compila, mas a app falha ao arrancar.
