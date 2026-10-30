# ACaminho — App TVDE

O **ACaminho** é um projeto desenvolvido no âmbito da unidade curricular de Laboratório de Programação. Consiste numa aplicação Android para o setor TVDE, com funcionalidades destinadas a passageiros, motoristas e administradores.

O objetivo é centralizar a experiência de utilização numa única plataforma, simplificando a gestão de viagens e disponibilizando ferramentas de acompanhamento e análise da atividade dos motoristas.

**Tecnologias utilizadas:**
- Android nativo
- Kotlin
- Jetpack Compose
- Firebase Authentication
- Cloud Firestore
- Arquitetura MVVM

Referência visual dos ecrãs: `docs/mockup.png`.

## Estado do projeto

<text>**Fase 1 — Fundação (concluída)**</text>

- Estrutura inicial do projeto, tema e navegação.
- Registo e início de sessão com email e palavra-passe.
- Escolha do tipo de utilizador: Passageiro ou Motorista.
- Armazenamento dos perfis no Cloud Firestore.
- Encaminhamento dos utilizadores de acordo com o respetivo papel (PASSENGER, DRIVER ou ADMIN).
- Interruptor de estado Online/Offline do motorista, com persistência no Firestore.

**Fases seguintes — Planeadas**

- **Fase 2 — Passageiro:** mapa, definição de origem e destino, estimativa de preço e pedido de viagem.
- **Fase 3 — Motorista:** receção e gestão de pedidos, aceitação de viagens e navegação integrada.
- **Fase 4 — Gestão financeira:** resumo da atividade, ganhos, despesas e indicadores de rentabilidade.
- **Fase 5 — Administração:** gestão de utilizadores, motoristas, veículos e viagens.
- **Fase 6 — Validação e entrega:** testes, validação das funcionalidades e preparação da demonstração final.

## Como executar o projeto

### Pré-requisitos

- Android Studio (versão recente).
- JDK 17.
- Emulador Android ou dispositivo físico.
- Projeto Firebase configurado.

### Configuração do Firebase

1. Acede à [Firebase Console](https://console.firebase.google.com/) e cria um projeto.
2. Adiciona uma aplicação Android com o seguinte identificador:

   `pt.kaminho.app`

3. Descarrega o ficheiro `google-services.json`.
4. Coloca o ficheiro na pasta `app/` do projeto.
5. Em **Authentication → Sign-in method**, ativa o método **Email/Password**.
6. Em **Firestore Database**, cria a base de dados.
7. Configura as regras de segurança do Firestore utilizando o ficheiro `firestore.rules` existente no projeto.

### Executar a aplicação

1. Abre o Android Studio.
2. Seleciona **File → Open** e escolhe a pasta do projeto.
3. Aguarda pela sincronização do Gradle.
4. Se necessário, aceita a instalação das dependências solicitadas pelo Android Studio.
5. Seleciona um emulador ou dispositivo Android.
6. Executa a aplicação através de **Run**.

## Estrutura do projeto

```text
app/src/main/java/pt/kaminho/app/
├── data/
│   ├── model/        # Modelos: User, Driver, Vehicle, Place e Trip
│   ├── repository/   # Repositórios: AuthRepository e DriverRepository
│   └── firebase/     # Tratamento de erros do Firebase
├── navigation/       # Navegação e encaminhamento por perfil
├── ui/
│   ├── auth/         # Autenticação
│   ├── passenger/    # Interface do passageiro
│   ├── driver/       # Interface do motorista
│   ├── admin/        # Interface de administração
│   ├── components/   # Componentes reutilizáveis
│   └── theme/        # Tema e estilos
├── viewmodel/        # AuthViewModel e DriverViewModel
└── MainActivity.kt
```

## Notas importantes

- O ficheiro `google-services.json` não deve ser publicado no repositório Git.
- O projeto necessita de configuração Firebase para funcionar corretamente.
- O perfil de administrador não está disponível através do registo normal da aplicação. A atribuição desse papel deve ser efetuada por um processo administrativo controlado e seguro.
- As funcionalidades identificadas como planeadas ainda não devem ser consideradas implementadas.

## Equipa

**Grupo K — ACaminho**

A função de Scrum Master é rotativa, sendo atribuída a um elemento diferente em cada sprint.

| Sprint | Scrum Master |
|---|---|
| Sprint 0 | Gabriel Costa |
| Sprint 1 | Simão Cerejo |
| Sprint 2 | Gabriel Rocha |
| Sprint 3 | Diogo Pereira |

**Equipa de desenvolvimento:**
- Gabriel Costa
- Simão Cerejo
- Gabriel Rocha
- Diogo Pereira
