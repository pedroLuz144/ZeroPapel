alter table pedidos add column fechado bit not null default 0;

update pedidos p
set p.fechado = 1
where exists (
    select 1 from fechamentos_caixa f
    where p.horario_pedido between f.de and f.ate
);
