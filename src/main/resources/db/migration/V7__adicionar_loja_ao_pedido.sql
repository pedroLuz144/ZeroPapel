alter table pedidos add column loja_id bigint null;

update pedidos
set loja_id = (select min(id) from loja)
where loja_id is null;

alter table pedidos modify column loja_id bigint not null;

alter table pedidos add constraint fk_pedidos_loja foreign key (loja_id) references loja (id);

create index idx_pedidos_loja_id on pedidos (loja_id);
