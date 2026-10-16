package com.goldenpetiscaria.zeropapel.integracoes.ifood;

import com.goldenpetiscaria.zeropapel.integracoes.ifood.client.IfoodAutenticador;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.client.IfoodEventoClient;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.client.IfoodLojaClient;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.config.IfoodConfig;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.dto.response.EventoIfoodResponse;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.dto.response.LojaIfoodResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "IFOOD_TESTE_REAL", matches = "true")
class IfoodApiRealTest {

    private static final String URL_BASE_DE_PRODUCAO = "https://merchant-api.ifood.com.br";

    @Test
    @DisplayName("chama o iFood de verdade: obtem token e lista as lojas do aplicativo (somente leitura)")
    void obtemTokenEListaAsLojas() throws IOException {
        Properties arquivoEnv = lerArquivoEnv();
        IfoodConfig config = new IfoodConfig();
        RestClient restClient = config.ifoodRestClient(valor(arquivoEnv, "IFOOD_BASE_URL", URL_BASE_DE_PRODUCAO));
        IfoodAutenticador autenticador = config.ifoodAutenticador(restClient,
                valor(arquivoEnv, "IFOOD_CLIENT_ID", ""), valor(arquivoEnv, "IFOOD_CLIENT_SECRET", ""));
        IfoodLojaClient lojaClient = config.ifoodLojaClient(restClient, autenticador);

        String token = autenticador.obterToken();
        List<LojaIfoodResponse> lojas = lojaClient.listarLojas();

        assertThat(token).isNotBlank();
        System.out.println("IFOOD_REAL lojas=" + lojas.size());
        for (LojaIfoodResponse loja : lojas) {
            System.out.println("IFOOD_REAL loja id=" + loja.id() + " name=" + loja.name()
                    + " corporateName=" + loja.corporateName());
        }
    }

    @Test
    @DisplayName("chama o iFood de verdade: consulta os eventos pendentes sem confirmar o recebimento")
    void consultaOsEventosSemConfirmar() throws IOException {
        Properties arquivoEnv = lerArquivoEnv();
        IfoodConfig config = new IfoodConfig();
        RestClient restClient = config.ifoodRestClient(valor(arquivoEnv, "IFOOD_BASE_URL", URL_BASE_DE_PRODUCAO));
        IfoodAutenticador autenticador = config.ifoodAutenticador(restClient,
                valor(arquivoEnv, "IFOOD_CLIENT_ID", ""), valor(arquivoEnv, "IFOOD_CLIENT_SECRET", ""));
        IfoodEventoClient eventoClient = config.ifoodEventoClient(restClient, autenticador);

        List<EventoIfoodResponse> eventos = eventoClient.buscarEventos();

        System.out.println("IFOOD_REAL eventos=" + eventos.size());
        for (EventoIfoodResponse evento : eventos) {
            System.out.println("IFOOD_REAL evento code=" + evento.code() + " fullCode=" + evento.fullCode()
                    + " createdAt=" + evento.createdAt());
        }
    }

    private Properties lerArquivoEnv() throws IOException {
        Properties propriedades = new Properties();
        Path arquivo = Path.of(".env");
        if (Files.exists(arquivo)) {
            try (Reader leitor = Files.newBufferedReader(arquivo, StandardCharsets.UTF_8)) {
                propriedades.load(leitor);
            }
        }
        return propriedades;
    }

    private String valor(Properties arquivoEnv, String nome, String padrao) {
        String doAmbiente = System.getenv(nome);
        if (doAmbiente != null && !doAmbiente.isBlank()) {
            return doAmbiente;
        }
        return arquivoEnv.getProperty(nome, padrao).trim();
    }
}
