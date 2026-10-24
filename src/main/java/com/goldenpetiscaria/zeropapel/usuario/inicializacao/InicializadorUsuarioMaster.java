package com.goldenpetiscaria.zeropapel.usuario.inicializacao;

import com.goldenpetiscaria.zeropapel.usuario.enumerator.ResultadoUsuarioMaster;
import com.goldenpetiscaria.zeropapel.usuario.service.UsuarioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InicializadorUsuarioMaster implements ApplicationRunner {

    static final String NOME_PADRAO = "Administrador";

    private final UsuarioService usuarioService;
    private final String usuario;
    private final String senha;
    private final String nome;

    public InicializadorUsuarioMaster(
            UsuarioService usuarioService,
            @Value("${admin.usuario:}") String usuario,
            @Value("${admin.senha:}") String senha,
            @Value("${admin.nome:}") String nome
    ) {
        this.usuarioService = usuarioService;
        this.usuario = usuario;
        this.senha = senha;
        this.nome = nome;
    }

    @Override
    public void run(ApplicationArguments args) {
        String nomeDeExibicao = nome == null || nome.isBlank() ? NOME_PADRAO : nome.strip();
        ResultadoUsuarioMaster resultado =
                usuarioService.criarUsuarioMasterSeNaoHouverUsuarios(nomeDeExibicao, usuario, senha);

        switch (resultado) {
            case CRIADO -> log.info(
                    "Usuário master criado com o login={} e cargo GERENTE; troque a senha depois do primeiro login",
                    usuario.strip());
            case CREDENCIAIS_AUSENTES -> log.warn(
                    "Não há nenhum usuário cadastrado e o usuário master não foi criado: "
                            + "defina ADMIN_USUARIO e ADMIN_SENHA (e, se quiser, ADMIN_NOME) no .env ou no ambiente "
                            + "e suba a aplicação de novo");
            case CREDENCIAIS_INVALIDAS -> log.error(
                    "Não há nenhum usuário cadastrado e o usuário master não foi criado: "
                            + "ADMIN_USUARIO ou ADMIN_SENHA não atendem às regras do cadastro; corrija e suba de novo");
            case JA_EXISTEM_USUARIOS -> log.debug("Já existem usuários cadastrados; usuário master ignorado");
        }
    }
}
