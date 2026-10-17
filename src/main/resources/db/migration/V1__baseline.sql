create table categorias (
    id bigint not null auto_increment,
    nome varchar(255) not null,
    primary key (id),
    constraint uk_categorias_nome unique (nome)
) engine=InnoDB;

create table cardapio (
    id bigint not null auto_increment,
    nome varchar(255) not null,
    descricao varchar(255),
    preco decimal(38,2) not null,
    status enum ('ATIVO','INATIVO') not null,
    categoria_id bigint not null,
    primary key (id),
    constraint fk_cardapio_categoria foreign key (categoria_id) references categorias (id)
) engine=InnoDB;

create table usuarios (
    id bigint not null auto_increment,
    nome varchar(255) not null,
    usuario varchar(255) not null,
    senha varchar(255) not null,
    cargo enum ('GERENTE','OPERADOR') not null,
    ativo bit not null,
    primary key (id),
    constraint uk_usuarios_usuario unique (usuario)
) engine=InnoDB;

create table refresh_tokens (
    id bigint not null auto_increment,
    token varchar(255) not null,
    usuario_id bigint,
    expiracao datetime(6),
    primary key (id),
    constraint uk_refresh_tokens_token unique (token),
    constraint uk_refresh_tokens_usuario unique (usuario_id),
    constraint fk_refresh_tokens_usuario foreign key (usuario_id) references usuarios (id)
) engine=InnoDB;

create table plataforma (
    id bigint not null auto_increment,
    nome varchar(255) not null,
    taxa_percentual decimal(38,2) not null,
    entrega boolean default true not null,
    primary key (id),
    constraint uk_plataforma_nome unique (nome)
) engine=InnoDB;

create table forma_de_pagamento (
    id bigint not null auto_increment,
    nome varchar(255) not null,
    taxa_percentual decimal(38,2) not null,
    primary key (id),
    constraint uk_forma_de_pagamento_nome unique (nome)
) engine=InnoDB;

create table pedidos (
    id bigint not null auto_increment,
    nome_cliente varchar(255),
    horario_pedido datetime(6) not null,
    status varchar(20) default 'CONCLUIDO' not null,
    valor decimal(38,2) not null,
    plataforma_id bigint not null,
    forma_de_pagamento_id bigint not null,
    primary key (id),
    constraint ck_pedidos_status check (status in ('EM_ABERTO','ACEITO','EM_PREPARO','PRONTO','EM_ROTA','CONCLUIDO','CANCELADO')),
    constraint fk_pedidos_plataforma foreign key (plataforma_id) references plataforma (id),
    constraint fk_pedidos_forma_de_pagamento foreign key (forma_de_pagamento_id) references forma_de_pagamento (id)
) engine=InnoDB;

create table itens_pedido (
    id bigint not null auto_increment,
    pedido_id bigint not null,
    item_id bigint not null,
    quantidade integer not null,
    preco_unitario decimal(38,2) not null,
    primary key (id),
    constraint fk_itens_pedido_pedido foreign key (pedido_id) references pedidos (id),
    constraint fk_itens_pedido_item foreign key (item_id) references cardapio (id)
) engine=InnoDB;

create table fechamentos_caixa (
    id bigint not null auto_increment,
    de datetime(6) not null,
    ate datetime(6) not null,
    gerado_em datetime(6) not null,
    gerado_por_id bigint not null,
    total_pedidos integer not null,
    faturamento_bruto decimal(38,2) not null,
    total_taxas decimal(38,2) not null,
    faturamento_liquido decimal(38,2) not null,
    ticket_medio decimal(38,2) not null,
    primary key (id),
    constraint fk_fechamentos_caixa_gerado_por foreign key (gerado_por_id) references usuarios (id)
) engine=InnoDB;
