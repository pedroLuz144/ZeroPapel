alter table canal_de_venda
    add column tratamento_taxa_entrega varchar(20) not null default 'DESCONTAR';

alter table pedidos
    add column taxa_entrega decimal(10,2) not null default 0,
    add column tratamento_taxa_entrega varchar(20) not null default 'IGNORAR';
