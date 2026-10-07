create index idx_pedidos_horario_pedido on pedidos (horario_pedido);
create index idx_pedidos_status on pedidos (status);
create index idx_fechamentos_caixa_periodo on fechamentos_caixa (de, ate);
create index idx_refresh_tokens_expiracao on refresh_tokens (expiracao);
