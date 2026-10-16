package com.goldenpetiscaria.zeropapel.common.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {
    @GetMapping({"/", "/login", "/app/**"})
    public String encaminharParaSpa() {
        return "forward:/index.html";
    }
}
