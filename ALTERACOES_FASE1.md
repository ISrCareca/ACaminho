# ACaminho — correções da Fase 1

## Alterações incluídas

1. **Aceitação concorrente de viagens**
   - `acceptTrip` lê a viagem e o documento do motorista na mesma transação.
   - A viagem só é aceite se ainda estiver `REQUESTED` e o motorista estiver `ONLINE`.
   - A transação altera a viagem para `ACCEPTED` e o motorista para `ON_TRIP` em conjunto. Duas aceitações concorrentes pelo mesmo motorista entram em conflito na Firestore e uma delas tem de falhar/repetir e revalidar o estado.
   - As regras da Firestore verificam o estado anterior `ONLINE` e o estado resultante `ON_TRIP`.

2. **Recuperação da viagem do passageiro**
   - Ao abrir o ecrã, a app procura uma viagem ativa do passageiro.
   - Se encontrar uma, restabelece o listener em tempo real e volta a carregar os dados do motorista.
   - O motorista já recuperava a viagem ativa ao carregar o ecrã; essa lógica foi preservada.

3. **Regras Firestore mais restritas para viagens**
   - O passageiro só cria viagens para si, inicialmente `REQUESTED` e sem motorista atribuído.
   - Motoristas leem pedidos `REQUESTED` e as viagens que lhes foram atribuídas.
   - As atualizações de estado estão limitadas às transições previstas.
   - Só os participantes podem ler as viagens, com a exceção dos motoristas que consultam pedidos disponíveis.
   - As regras de `users`, `drivers`, `vehicles` e `ratings` existentes foram preservadas, com validação adicional na aceitação.

4. **Cancelar viagem**
   - O passageiro pode cancelar um pedido enquanto está `REQUESTED`.
   - O motorista pode cancelar uma viagem em `ACCEPTED` ou `DRIVER_ARRIVED`.
   - Depois de `IN_PROGRESS`, a app não apresenta cancelamento: a viagem deve ser concluída pelo fluxo normal.
   - O cancelamento grava `CANCELLED`, `cancelledAt` e `cancelledBy`. Se o motorista cancelar antes do início, volta a `ONLINE` e retoma os pedidos.

## Instalação

1. Faz uma cópia de segurança do teu projeto atual.
2. Abre esta pasta no Android Studio ou copia os ficheiros para o projeto existente.
3. Em `app/src/main/java/pt/kaminho/app/`, substitui os ficheiros Kotlin que constam no pacote.
4. Na Firebase Console, abre **Firestore Database → Regras**, substitui as regras pelo conteúdo de `firestore.rules` e publica.
5. Executa **Sync Project with Gradle Files** e depois **Build → Make Project**.

## Testes manuais obrigatórios

- Dois motoristas tentam aceitar o mesmo pedido: apenas um deve conseguir.
- O mesmo motorista tenta aceitar dois pedidos quase ao mesmo tempo: apenas um deve ser aceite; o motorista não deve ficar com duas viagens.
- Fechar e reabrir a app do passageiro com uma viagem `REQUESTED`, `ACCEPTED`, `DRIVER_ARRIVED` ou `IN_PROGRESS`: a viagem deve reaparecer.
- Passageiro cancela um pedido `REQUESTED`: estado `CANCELLED` e o motorista continua a receber outros pedidos.
- Motorista cancela uma viagem `ACCEPTED` ou `DRIVER_ARRIVED`: estado `CANCELLED` e motorista regressa a `ONLINE`.
- Tentar atualizar uma viagem para um estado não permitido deve resultar em `permission-denied`.

## Limitação de validação

O código foi alterado estaticamente, mas não foi possível executar uma compilação Gradle nesta sessão porque o ZIP original não inclui `gradlew` e não há uma instalação local de Gradle disponível. As regras também devem ser exercitadas no Firebase Emulator Suite ou no projeto de teste antes de serem consideradas validadas.
