update plataforma
set entrega = 0
where nome = 'Balcão'
  and not exists (select 1 from (select id from plataforma where entrega = 0) balcoes);

insert into plataforma (nome, entrega)
select 'Balcão', 0
from dual
where not exists (select 1 from plataforma where entrega = 0)
  and not exists (select 1 from plataforma where nome = 'Balcão');

insert into canal_de_venda (loja_id, plataforma_id, taxa_percentual, ativo)
select l.id, b.id, 0, 1
from loja l
    cross join (select min(id) as id from plataforma where entrega = 0) b
where b.id is not null
  and not exists (
      select 1
      from canal_de_venda c
      where c.loja_id = l.id
        and c.plataforma_id = b.id
  );
