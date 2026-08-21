package org.gatuninhas.api.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades de JWT lidas de "app.jwt.*" (ver application.yml).
 * Os secrets DEVEM ser sobrescritos via variavel de ambiente em qualquer
 * ambiente real (dev-docker, homologacao, producao) - os defaults aqui
 * existem apenas para a aplicacao nao falhar ao subir sem configuracao.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String accessSecret,
        String refreshSecret,
        long accessExpirationMs,
        long refreshExpirationMs) {
}
