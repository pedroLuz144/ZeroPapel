create table ifood_evento (
    id bigint not null auto_increment,
    evento_id varchar(64) not null,
    codigo varchar(64),
    pedido_id varchar(255),
    merchant_id varchar(255),
    criado_em datetime(6),
    payload varchar(8000) not null,
    recebido_em datetime(6) not null,
    situacao varchar(20) not null,
    tentativas int not null default 0,
    processado_em datetime(6),
    detalhe varchar(500),
    primary key (id),
    constraint uk_ifood_evento_evento_id unique (evento_id)
) engine=InnoDB;

create index idx_ifood_evento_situacao_criado_em on ifood_evento (situacao, criado_em);
