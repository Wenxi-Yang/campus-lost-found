package cn.campus.lostfound;

import java.util.*;
import javax.servlet.http.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final Store s;
  private final Auth auth;
  private final JwtTokens tokens;
  private final BCryptPasswordEncoder encoder;

  public AuthController(Store s, Auth auth, JwtTokens tokens, BCryptPasswordEncoder encoder) {
    this.s = s;
    this.auth = auth;
    this.tokens = tokens;
    this.encoder = encoder;
  }

  @PostMapping("/register")
  public Object register(@RequestBody Map<String, Object> b) {
    String student = Input.text(b, "student_number", 4, 30), name = Input.text(
      b,
      "name",
      1,
      50
    ), phone = Input.text(b, "phone", 11, 11), password = Input.text(b, "password", 8, 64);
    if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) throw ApiError.bad(
      "密码的UTF-8长度不能超过72字节"
    );
    if (!student.matches("[0-9A-Za-z]+")) throw ApiError.bad("学号只允许字母或数字");
    if (!phone.matches("1[3-9][0-9]{9}")) throw ApiError.bad("手机号格式不正确");
    if (
      s.db.queryForObject(
        "SELECT COUNT(*) FROM app_user WHERE phone=? OR student_number=?",
        Long.class,
        student,
        phone
      ) >
      0
    ) throw ApiError.bad("学号或手机号与现有登录账号冲突");
    long id = s.insert(
      "INSERT INTO app_user(student_number,name,phone,password,role) VALUES(?,?,?,?,'user')",
      student,
      name,
      phone,
      encoder.encode(password)
    );
    return Collections.singletonMap("id", id);
  }

  @PostMapping("/login")
  public Object login(
    @RequestBody Map<String, Object> b,
    HttpServletRequest request,
    HttpServletResponse response
  ) {
    String account = Input.text(b, "account", 1, 50), password = Input.text(b, "password", 1, 64);
    List<Map<String, Object>> users = s.db.queryForList(
      "SELECT * FROM app_user WHERE student_number=? OR phone=?",
      account,
      account
    );
    Map<String, Object> u = users.size() == 1 ? users.get(0) : null;
    if (
      u == null || !encoder.matches(password, (String) u.get("password"))
    ) throw new ApiError.Failure(401, "账号或密码错误");
    if (!Store.yes(u.get("enabled"))) throw new ApiError.Failure(403, "账号已被禁用");
    tokens.issue(u, request, response);
    u.remove("password");
    u.put("role", JwtTokens.role(u.get("role")));
    return u;
  }

  @GetMapping("/me")
  public Object me(HttpServletRequest request) {
    return auth.user(request);
  }

  @PostMapping("/logout")
  public Object logout(HttpServletRequest request, HttpServletResponse response) {
    tokens.logout(request, response);
    return Collections.singletonMap("message", "已退出");
  }
}
