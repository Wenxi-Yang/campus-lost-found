package cn.campus.lostfound;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.SecretKey;
import javax.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/** 签名JWT存入HttpOnly Cookie，也支持标准Authorization Bearer请求。 */
@Component
public class JwtTokens {

  public static final String COOKIE = "campus_token";
  private final SecretKey key;
  private final JwtParser parser;
  private final boolean secure;
  private final Map<String, Long> revoked = new ConcurrentHashMap<>();
  private static final long LIFETIME = 3600000L;

  public JwtTokens(
    @Value("${app.jwt-secret:}") String secret,
    @Value("${app.jwt-cookie-secure:false}") boolean secure
  ) {
    // 未配置时每次启动产生随机密钥，重启需重新登录；生产部署可通过环境变量固定密钥。
    this.key =
      secret.isEmpty()
        ? Keys.secretKeyFor(SignatureAlgorithm.HS256)
        : Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    this.parser =
      Jwts.parserBuilder().setSigningKey(key).requireIssuer("campus-lost-found").build();
    this.secure = secure;
  }

  public String raw(HttpServletRequest request) {
    String h = request.getHeader("Authorization");
    if (h != null) {
      if (!h.startsWith("Bearer ")) throw new ApiError.Failure(401, "登录凭证格式不正确");
      return h.substring(7);
    }
    if (request.getCookies() != null) for (Cookie c : request.getCookies()) if (
      COOKIE.equals(c.getName())
    ) return c.getValue();
    return null;
  }

  public Claims read(HttpServletRequest request) {
    String token = raw(request);
    if (token == null) throw new ApiError.Failure(401, "请先登录");
    return verify(token);
  }

  public Claims verify(String token) {
    try {
      Jws<Claims> jwt = parser.parseClaimsJws(token);
      if (
        !SignatureAlgorithm.HS256.getValue().equals(jwt.getHeader().getAlgorithm())
      ) throw new IllegalArgumentException();
      Claims c = jwt.getBody();
      if (
        c.getExpiration() == null ||
        c.getIssuedAt() == null ||
        c.getId() == null ||
        c.getSubject() == null
      ) throw new IllegalArgumentException();
      if (
        !Arrays.asList("user", "admin").contains(c.get("role", String.class))
      ) throw new IllegalArgumentException();
      if (revoked.containsKey(c.getId())) throw new IllegalArgumentException();
      return c;
    } catch (JwtException | IllegalArgumentException e) {
      throw new ApiError.Failure(401, "登录凭证无效或已过期，请重新登录");
    }
  }

  public void issue(
    Map<String, Object> user,
    HttpServletRequest request,
    HttpServletResponse response
  ) {
    revoke(request);
    long now = System.currentTimeMillis();
    String token = Jwts
      .builder()
      .setIssuer("campus-lost-found")
      .setSubject(user.get("id").toString())
      .claim("role", role(user.get("role")))
      .setId(UUID.randomUUID().toString())
      .setIssuedAt(new Date(now))
      .setExpiration(new Date(now + LIFETIME))
      .signWith(key, SignatureAlgorithm.HS256)
      .compact();
    cookie(response, token, LIFETIME / 1000);
  }

  public void logout(HttpServletRequest request, HttpServletResponse response) {
    revoke(request);
    cookie(response, "", 0);
  }

  private void revoke(HttpServletRequest request) {
    long now = System.currentTimeMillis();
    revoked.entrySet().removeIf(e -> e.getValue() <= now);
    String token = raw(request);
    if (token != null) try {
      Claims c = verify(token);
      revoked.put(c.getId(), c.getExpiration().getTime());
    } catch (ApiError.Failure ignored) {}
  }

  private void cookie(HttpServletResponse response, String value, long seconds) {
    response.addHeader(
      "Set-Cookie",
      ResponseCookie
        .from(COOKIE, value)
        .path("/")
        .httpOnly(true)
        .secure(secure)
        .sameSite("Lax")
        .maxAge(seconds)
        .build()
        .toString()
    );
  }

  public static String role(Object role) {
    return "admin".equals(role) ? "admin" : "user";
  }
}
