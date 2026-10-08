alter table pedidos
    add column plataforma_pedido_id varchar(255) null,
    add constraint uk_pedidos_plataforma_pedido_id unique (plataforma_id, plataforma_pedido_id);
