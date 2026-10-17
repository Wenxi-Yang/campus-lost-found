package cn.campus.lostfound;

import io.jsonwebtoken.Claims;
import java.util.*;
import javax.servlet.http.*;
import org.springframework.stereotype.Component;

@Component
public class Auth {

  private final Store s;
  private final JwtTokens tokens;

  public Auth(Store s, JwtTokens tokens) {
    this.s = s;
    this.tokens = tokens;
  }

  public boolean authenticated(HttpServletRequest request) {
    return tokens.raw(request) != null;
  }

  // 校验签名与有效期后仍检查数据库，禁用账号和角色变更立即生效。
  public Map<String, Object> user(HttpServletRequest request) {
    Claims claims = tokens.read(request);
    long id;
    try {
      id = Long.parseLong(claims.getSubject());
    } catch (NumberFormatException e) {
      throw new ApiError.Failure(401, "登录凭证无效");
    }
    Map<String, Object> u = s.one(
      "SELECT id,student_number,name,phone,role,enabled,created_at FROM app_user WHERE id=?",
      id
    );
    if (!Store.yes(u.get("enabled"))) throw new ApiError.Failure(403, "账号已被禁用");
    String role = JwtTokens.role(u.get("role"));
    if (!role.equals(claims.get("role", String.class))) throw new ApiError.Failure(
      401,
      "账号权限已变更，请重新登录"
    );
    u.put("role", role);
    return u;
  }

  public Map<String, Object> admin(HttpServletRequest request) {
    Map<String, Object> u = user(request);
    if (!"admin".equals(u.get("role"))) throw ApiError.denied();
    return u;
  }

  public void owner(Map<String, Object> u, Map<String, Object> row) {
    if (
      !"admin".equals(u.get("role")) && Store.id(u, "id") != Store.id(row, "user_id")
    ) throw ApiError.denied();
  }
}
