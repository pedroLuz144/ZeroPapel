create table historico_pedido (
    id bigint not null auto_increment,
    pedido_id bigint not null,
    acao varchar(20) not null,
    usuario_id bigint,
    data_hora datetime(6) not null,
    detalhe varchar(500) not null,
    primary key (id),
    constraint fk_historico_pedido_usuario foreign key (usuario_id) references usuarios (id)
) engine=InnoDB;

create index idx_historico_pedido_pedido_id on historico_pedido (pedido_id);
