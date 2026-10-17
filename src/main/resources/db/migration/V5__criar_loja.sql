create table loja (
    id bigint not null auto_increment,
    nome varchar(255) not null,
    ativo bit not null default 1,
    primary key (id),
    constraint uk_loja_nome unique (nome)
) engine=InnoDB;

insert into loja (nome, ativo) values ('Loja principal', 1);
