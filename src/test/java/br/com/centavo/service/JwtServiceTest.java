package br.com.centavo.service;

import br.com.centavo.support.Fixtures;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret";
    private static final String ISSUER = "centavo-api";

    private final JwtService jwtService = new JwtService();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
    }

    @Test
    void generatedTokenValidatesToTheUserEmail() {
        String token = jwtService.generateToken(Fixtures.user(1L, "ana@centavo.test"));

        assertThat(jwtService.validateToken(token)).isEqualTo("ana@centavo.test");
    }

    // O BFF usa o exp do token como validade do cookie de sessão.
    @Test
    void generatedTokenExpiresInOneDay() {
        DecodedJWT decoded = JWT.decode(jwtService.generateToken(Fixtures.user(1L, "ana@centavo.test")));

        Instant expected = Instant.now().plus(Duration.ofDays(1));
        assertThat(decoded.getExpiresAtAsInstant())
                .isBetween(expected.minusSeconds(60), expected.plusSeconds(60));
        assertThat(decoded.getClaim("name").asString()).isEqualTo("Ana");
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        String forged = token(ISSUER, Instant.now().plusSeconds(60), "another-secret");

        assertThatThrownBy(() -> jwtService.validateToken(forged))
                .isInstanceOf(SignatureVerificationException.class);
    }

    @Test
    void rejectsExpiredToken() {
        String expired = token(ISSUER, Instant.now().minusSeconds(60), SECRET);

        assertThatThrownBy(() -> jwtService.validateToken(expired))
                .isInstanceOf(TokenExpiredException.class);
    }

    @Test
    void rejectsTokenFromAnotherIssuer() {
        String foreign = token("someone-else", Instant.now().plusSeconds(60), SECRET);

        assertThatThrownBy(() -> jwtService.validateToken(foreign))
                .isInstanceOf(JWTVerificationException.class);
    }

    private static String token(String issuer, Instant expiresAt, String secret) {
        return JWT.create()
                .withIssuer(issuer)
                .withSubject("ana@centavo.test")
                .withExpiresAt(expiresAt)
                .sign(Algorithm.HMAC256(secret));
    }
}
