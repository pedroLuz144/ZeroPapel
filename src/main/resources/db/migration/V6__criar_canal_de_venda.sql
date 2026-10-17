create table canal_de_venda (
    id bigint not null auto_increment,
    loja_id bigint not null,
    plataforma_id bigint not null,
    merchant_id varchar(255),
    taxa_percentual decimal(5,2) not null,
    ativo bit not null default 1,
    primary key (id),
    constraint uk_canal_de_venda_loja_plataforma unique (loja_id, plataforma_id),
    constraint uk_canal_de_venda_merchant_id unique (merchant_id),
    constraint fk_canal_de_venda_loja foreign key (loja_id) references loja (id),
    constraint fk_canal_de_venda_plataforma foreign key (plataforma_id) references plataforma (id) on delete cascade
) engine=InnoDB;

create index idx_canal_de_venda_plataforma_id on canal_de_venda (plataforma_id);

insert into canal_de_venda (loja_id, plataforma_id, taxa_percentual, ativo)
select l.id, p.id, p.taxa_percentual, 1
from loja l
    cross join plataforma p;
