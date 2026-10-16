package com.goldenpetiscaria.zeropapel.pedido.enumerator;

public enum StatusPedido {
    EM_ABERTO(1, false),
    ACEITO(2, false),
    EM_PREPARO(3, false),
    PRONTO(4, false),
    EM_ROTA(5, false),
    CONCLUIDO(6, true),
    CANCELADO(7, true);

    private final int ordemNoCicloDeVida;
    private final boolean terminal;

    StatusPedido(int ordemNoCicloDeVida, boolean terminal) {
        this.ordemNoCicloDeVida = ordemNoCicloDeVida;
        this.terminal = terminal;
    }

    public boolean ehTerminal() {
        return terminal;
    }

    public boolean podeAvancarPara(StatusPedido novo) {
        if (terminal || novo == null) {
            return false;
        }
        return novo == CANCELADO || novo.ordemNoCicloDeVida > ordemNoCicloDeVida;
    }
}
