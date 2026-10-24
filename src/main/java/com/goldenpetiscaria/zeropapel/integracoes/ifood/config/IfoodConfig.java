package com.goldenpetiscaria.zeropapel.integracoes.ifood.config;

import com.goldenpetiscaria.zeropapel.integracoes.ifood.client.IfoodAutenticador;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.client.IfoodEventoClient;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.client.IfoodLojaClient;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.client.IfoodPedidoClient;
import com.goldenpetiscaria.zeropapel.integracoes.ifood.translator.IfoodPedidoTranslator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;

@Configuration
@EnableScheduling
public class IfoodConfig {

    private static final Duration TEMPO_LIMITE_DE_CONEXAO = Duration.ofSeconds(5);
    private static final Duration TEMPO_LIMITE_DE_LEITURA = Duration.ofSeconds(15);

    @Bean
    public RestClient ifoodRestClient(@Value("${ifood.base-url}") String urlBase) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(TEMPO_LIMITE_DE_CONEXAO)
                .build();
        JdkClientHttpRequestFactory fabricaDeRequisicao = new JdkClientHttpRequestFactory(httpClient);
        fabricaDeRequisicao.setReadTimeout(TEMPO_LIMITE_DE_LEITURA);
        return RestClient.builder()
                .baseUrl(urlBase)
                .requestFactory(fabricaDeRequisicao)
                .build();
    }

    @Bean
    public IfoodAutenticador ifoodAutenticador(RestClient ifoodRestClient,
                                               @Value("${ifood.client-id:}") String clientId,
                                               @Value("${ifood.client-secret:}") String clientSecret) {
        return new IfoodAutenticador(ifoodRestClient, clientId, clientSecret, Clock.systemUTC());
    }

    @Bean
    public IfoodLojaClient ifoodLojaClient(RestClient ifoodRestClient, IfoodAutenticador ifoodAutenticador) {
        return new IfoodLojaClient(ifoodRestClient, ifoodAutenticador);
    }

    @Bean
    public IfoodEventoClient ifoodEventoClient(RestClient ifoodRestClient, IfoodAutenticador ifoodAutenticador) {
        return new IfoodEventoClient(ifoodRestClient, ifoodAutenticador);
    }

    @Bean
    public IfoodPedidoClient ifoodPedidoClient(RestClient ifoodRestClient, IfoodAutenticador ifoodAutenticador) {
        return new IfoodPedidoClient(ifoodRestClient, ifoodAutenticador);
    }

    @Bean
    public IfoodPedidoTranslator ifoodPedidoTranslator() {
        return new IfoodPedidoTranslator(ZoneId.systemDefault());
    }
}
