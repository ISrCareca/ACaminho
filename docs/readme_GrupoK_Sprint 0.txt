Grupo K — A caminho
Notas do Sprint 0

Equipa:
Gabriel Costa fica como Scrum Master. Simão Cerejo, Gabriel Rocha, Diogo Pereira ficam como developers.

Ideia do produto:
Queremos uma plataforma TVDE, em formato de MVP (Minimum Viable Product), com âmbito realista para os sprints da Unidade Curricular. A viagem deixa de estar partida por várias aplicações. O passageiro pede o serviço, escolhe o motorista e percebe o preço. O motorista segue o percurso dentro da própria aplicação e, antes de aceitar, vê uma estimativa de rentabilidade. Por cima disto existe uma área de gestão que acompanha utilizadores, motoristas, veículos e viagens.

Quem entra na plataforma:

Passageiro. Cria conta e autentica-se, indica origem e destino e faz o pedido. Pode escolher o motorista pelas avaliações ou por já confiar nessa pessoa. Durante o serviço vê a localização do motorista e o estado da viagem, e consulta o valor estimado com o preço explicado.

Motorista. Cria conta e autentica-se, trata do perfil e dos dados do veículo, recebe pedidos e decide se aceita. O percurso e a navegação ficam na aplicação, sem saltar para outra. Antes de aceitar consulta o valor esperado, os custos e a margem estimada. Depois acompanha ganhos e custos.

Gestão. Acompanha quatro conjuntos de dados: utilizadores, motoristas, veículos e viagens.

O que as aplicações testadas já fazem e o que deixam de fora:

Uber. O tempo até ao destino aparece logo, a utilização é intuitiva, o motorista organiza o horário e o preço fica fechado antes da viagem. O passageiro sabe quanto vai pagar antes de confirmar. O lado fraco é outro: a aplicação olha mais para o que já aconteceu do que para a decisão de aceitar ou não aquela viagem. Rentabilidade, navegação e gestão em tempo real não cabem no mesmo fluxo, e o motorista muda de aplicação a meio do trabalho. A atribuição do motorista é automática, por isso o passageiro não escolhe. Consultámos Uber Driver, da Uber Technologies, em 6/10/2026.

Bolt. Também mostra o tempo de chegada, deixa organizar o horário e apresenta o preço exato antes da viagem, para quem conduz e para quem pede. O limite é o mesmo da Uber: serve mais para seguir a atividade depois de ela ocorrer do que para calcular a margem antes de aceitar. Navegação e gestão da viagem estão separadas, por isso continua a ser preciso alternar entre aplicações. Na nossa utilização pareceu menos intuitiva, e o passageiro também não escolhe o motorista. Consultámos a página de motoristas da Bolt Technology (https://bolt.eu/pt-pt/driver/) em 5/10/2026.

VLOZ. Aqui o passageiro escolhe o motorista pelas avaliações, em vez de ficar com a pessoa que o sistema lhe atribui. O tempo de chegada, o preço anterior à viagem e a flexibilidade de horário também existem. Mesmo assim, rentabilidade, navegação e gestão em tempo real continuam fora de um único fluxo, e o motorista ainda precisa de outras aplicações para fazer a viagem de ponta a ponta. A referência que usámos foi App VLOZ, The Drop Company.

Porque ficámos com as três:
Não havia motivo para descartar nenhuma. Uber e Bolt são as que o passageiro e o motorista já usam, e foi nelas que a falta de escolha do motorista e a falta de uma conta simples de margem ficaram claras. A VLOZ trata melhor essa escolha, mas não fecha a navegação dentro da aplicação nem a estimativa de custo, receita e benefício na mesma viagem.

Leituras que usámos para além das aplicações:
Chen et al. mostram que a maneira de atribuir viagens altera o ganho por hora do motorista. Sun et al. (2020) comparam duas formas de distribuir pedidos, uma em que todas as viagens ficam visíveis e outra em que a viagem vai para os motoristas mais próximos, e defendem uma atribuição simples e transparente, assente sobretudo na distância. Para meter a navegação dentro da aplicação vimos o Navigation SDK da Google e o da Mapbox. A localização GPS é dado pessoal: o tratamento segue o RGPD (União Europeia, 2016), com minimização dos dados. Entrou também a lei portuguesa do TVDE, a Lei n.º 45/2018, alterada pela Lei n.º 59/2026.

O que falta no que já existe
Não encontrámos uma solução que junte, de uma vez, o pedido e a atribuição, a navegação dentro da aplicação, e a estimativa de margem do motorista com o preço explicado ao passageiro. Na Uber e na Bolt o passageiro continua sem escolher quem o transporta. Tratamos isto como hipótese, apoiada nos testes e na documentação pública. A confirmação fica para uma análise mais sistemática no Sprint 1.

Como pensamos montar o MVP::
Base de dados relacional e uma API entre os componentes. O serviço de mapas ainda está em aberto. Google ou Mapbox decide-se no Sprint 1, olhando ao custo, à documentação e à facilidade de integração.

Como o documento foi fechado

Arranque, 05/10/2026. Distribuímos o trabalho do Sprint 0. Gabriel Costa, como Scrum Master, acompanhou o documento. Simão Cerejo, Gabriel Rocha e Diogo Pereira trataram da análise e da redação. Nessa sessão o projeto ficou com o nome A caminho e a proposta começou a ser preenchida. Toda a gente entrou na discussão e no texto. O Scrum Master reviu.

Fecho, 07/10/2026. Voltámos a sentar-nos para tirar as últimas dúvidas. Entraram os passos do Sprint 1: user stories com critérios de aceitação, modelo de custos e de atribuição de viagens, modelo de dados relacional, escolha do serviço de mapas e divisão de tarefas. O documento foi formatado, o Scrum Master fez a revisão final e a entrega seguiu em pdf.
