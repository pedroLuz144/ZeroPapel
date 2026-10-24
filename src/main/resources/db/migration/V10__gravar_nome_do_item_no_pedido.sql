alter table itens_pedido add column nome_item varchar(255) null;

update itens_pedido ip
    join cardapio c on c.id = ip.item_id
set ip.nome_item = c.nome
where ip.nome_item is null;

alter table itens_pedido modify column nome_item varchar(255) not null;

alter table itens_pedido modify column item_id bigint null;
