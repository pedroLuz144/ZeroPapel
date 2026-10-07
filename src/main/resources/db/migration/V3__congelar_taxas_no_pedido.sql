alter table pedidos add column taxa_plataforma_percentual decimal(5,2) null;
alter table pedidos add column taxa_pagamento_percentual decimal(5,2) null;

update pedidos p
    join plataforma pl on pl.id = p.plataforma_id
set p.taxa_plataforma_percentual = pl.taxa_percentual
where p.taxa_plataforma_percentual is null;

update pedidos p
    join forma_de_pagamento fp on fp.id = p.forma_de_pagamento_id
set p.taxa_pagamento_percentual = fp.taxa_percentual
where p.taxa_pagamento_percentual is null;

alter table pedidos modify column taxa_plataforma_percentual decimal(5,2) not null;
alter table pedidos modify column taxa_pagamento_percentual decimal(5,2) not null;
