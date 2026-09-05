package com.goldenpetiscaria.zeropapel.common.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Encaminha as rotas da SPA (Vue Router em history mode) para o index.html gerado pelo Vite.
 *
 * As chamadas de API têm prefixos próprios (/auth, /pedidos, /itens, /categorias,
 * /plataformas, /formasDePagamento, /fechamento, /dashboard, /usuarios) e não passam por aqui.
 * Ao adicionar uma nova rota de tela no front, inclua o caminho abaixo.
 */
@Controller
public class SpaForwardController {

    @GetMapping({"/", "/login", "/app/**"})
    public String encaminharParaSpa() {
        return "forward:/index.html";
    }
}
