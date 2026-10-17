package cn.campus.lostfound;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.fasterxml.jackson.databind.*;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import javax.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(
  properties = {
    "spring.datasource.url=jdbc:h2:mem:tests;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "app.bootstrap-admin-password=Admin12345",
    "app.demo=false",
    "app.jwt-secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
  }
)
@ActiveProfiles("test")
@AutoConfigureMockMvc
class WorkflowTest {

  @Autowired
  MockMvc mvc;

  @Autowired
  ObjectMapper json;

  @Autowired
  JdbcTemplate db;

  Cookie admin, alice, bob;
  long category;

  Map<String, Object> map(Object... pairs) {
    Map<String, Object> m = new LinkedHashMap<>();
    for (int i = 0; i < pairs.length; i += 2) m.put((String) pairs[i], pairs[i + 1]);
    return m;
  }

  MvcResult call(String method, String url, Cookie session, Object body, int code)
    throws Exception {
    MockHttpServletRequestBuilder r;
    switch (method) {
      case "POST":
        r = post(url);
        break;
      case "PUT":
        r = put(url);
        break;
      case "DELETE":
        r = delete(url);
        break;
      default:
        r = get(url);
    }
    r.header("X-Requested-With", "campus");
    if (session != null) r.cookie(session);
    if (body != null) r.contentType("application/json").content(json.writeValueAsBytes(body));
    MvcResult result = mvc.perform(r).andReturn();
    assertEquals(code, result.getResponse().getStatus(), result.getResponse().getContentAsString());
    return result;
  }

  JsonNode data(MvcResult r) throws Exception {
    return json.readTree(r.getResponse().getContentAsByteArray());
  }

  void register(String account, String phone) throws Exception {
    call(
      "POST",
      "/api/auth/register",
      null,
      map("student_number", account, "phone", phone, "name", account, "password", "Student123"),
      200
    );
  }

  Cookie login(String account, String pass) throws Exception {
    MvcResult r = call(
      "POST",
      "/api/auth/login",
      null,
      map("account", account, "password", pass),
      200
    );
    String header = r.getResponse().getHeader("Set-Cookie");
    assertTrue(header.contains("HttpOnly"));
    assertTrue(header.contains("SameSite=Lax"));
    return new Cookie(
      JwtTokens.COOKIE,
      header.substring(header.indexOf('=') + 1, header.indexOf(';'))
    );
  }

  void confirm(long claim) throws Exception {
    call(
      "PUT",
      "/api/claims/" + claim + "/confirm",
      alice,
      map("decision", "confirmed", "reply", "Unique features match"),
      200
    );
  }

  @BeforeEach
  void setup() throws Exception {
    db.update("DELETE FROM notice");
    db.update("DELETE FROM claim");
    db.update("DELETE FROM item");
    db.update("DELETE FROM app_user");
    db.update("DELETE FROM category");
    db.update(
      "INSERT INTO app_user(student_number,name,phone,password,role) VALUES('admin','Admin','13900000000',?,'admin')",
      new BCryptPasswordEncoder().encode("Admin12345")
    );
    db.update("INSERT INTO category(name) VALUES('Electronics')");
    category = db.queryForObject("SELECT id FROM category", Long.class);
    register("20260001", "13800000001");
    register("20260002", "13800000002");
    alice = login("20260001", "Student123");
    bob = login("20260002", "Student123");
    admin = login("admin", "Admin12345");
  }

  Map<String, Object> itemBody() {
    return map(
      "title",
      "Library headphones",
      "type",
      "found",
      "category_id",
      category,
      "location",
      "Library",
      "occurred_at",
      "2026-01-01 10:00:00",
      "description",
      "Headphones with a distinct protective case",
      "contact",
      "13800000001"
    );
  }

  long item() throws Exception {
    return data(call("POST", "/api/items", alice, itemBody(), 200)).get("id").asLong();
  }

  long claim(long item, Cookie session) throws Exception {
    return data(
      call(
        "POST",
        "/api/items/" + item + "/claims",
        session,
        map("description", "My headphones have a blue case and initials"),
        200
      )
    )
      .get("id")
      .asLong();
  }

  Map<String, Object> notice(long item, String start, String end) {
    return map(
      "title",
      "Lost and found reminder",
      "content",
      "Please check your item and apply for review",
      "kind",
      "reminder",
      "item_id",
      item,
      "category_id",
      category,
      "enabled",
      true,
      "published_at",
      "2026-01-01 00:00:00",
      "starts_at",
      start,
      "expires_at",
      end
    );
  }

  @Test
  void registrationValidationAndPhoneLogin() throws Exception {
    register("20260003", "13800000003");
    call(
      "POST",
      "/api/auth/register",
      null,
      map(
        "student_number",
        "20260003",
        "phone",
        "13800000004",
        "name",
        "Test",
        "password",
        "Student123"
      ),
      409
    );
    call(
      "POST",
      "/api/auth/register",
      null,
      map("student_number", "20260005", "phone", "123", "name", "Test", "password", "Student123"),
      400
    );
    assertNotNull(login("13800000003", "Student123"));
    call("POST", "/api/auth/login", null, map("account", "20260003", "password", "wrong"), 401);
    assertTrue(
      db
        .queryForObject(
          "SELECT password FROM app_user WHERE student_number='20260003'",
          String.class
        )
        .startsWith("$2")
    );
  }

  @Test
  void authorizationAndOwnership() throws Exception {
    long i = item();
    call("POST", "/api/items", null, itemBody(), 401);
    call("GET", "/api/admin/users", bob, null, 403);
    call("PUT", "/api/items/" + i, bob, itemBody(), 403);
    call("DELETE", "/api/items/" + i, bob, null, 403);
    call("GET", "/api/claims/mine", null, null, 401);
  }

  @Test
  void publicSearchAndContactVisibility() throws Exception {
    long i = item();
    JsonNode list = data(
      call(
        "GET",
        "/api/items?keyword=headphones&location=Library&category_id=" +
        category +
        "&status=open&type=found",
        null,
        null,
        200
      )
    );
    assertEquals(1, list.get("total").asInt());
    assertFalse(list.get("rows").get(0).has("contact"));
    assertFalse(data(call("GET", "/api/items/" + i, null, null, 200)).has("contact"));
    assertTrue(data(call("GET", "/api/items/" + i, bob, null, 200)).has("contact"));
    assertEquals(0, data(call("GET", "/api/items?mine=true", bob, null, 200)).get("total").asInt());
  }

  @Test
  void claimApprovalCompletesItemAndRejectsOtherPending() throws Exception {
    long i = item(), c = claim(i, bob);
    confirm(c);
    register("20260003", "13800000003");
    Cookie other = login("20260003", "Student123");
    claim(i, other);
    call(
      "PUT",
      "/api/admin/claims/" + c + "/review",
      admin,
      map("status", "approved", "reply", "Please collect at service center"),
      200
    );
    assertEquals(
      "finished",
      db.queryForObject("SELECT status FROM item WHERE id=?", String.class, i)
    );
    assertEquals(
      1,
      db.queryForObject(
        "SELECT COUNT(*) FROM claim WHERE item_id=? AND status='approved'",
        Integer.class,
        i
      )
    );
    assertEquals(
      1,
      db.queryForObject(
        "SELECT COUNT(*) FROM claim WHERE item_id=? AND status='rejected'",
        Integer.class,
        i
      )
    );
    call(
      "PUT",
      "/api/admin/claims/" + c + "/review",
      admin,
      map("status", "approved", "reply", "Again"),
      400
    );
  }

  @Test
  void claimConstraintsAndWithdrawal() throws Exception {
    long i = item();
    call(
      "POST",
      "/api/items/" + i + "/claims",
      alice,
      map("description", "Cannot claim an item I posted"),
      400
    );
    long c = claim(i, bob);
    call(
      "POST",
      "/api/items/" + i + "/claims",
      bob,
      map("description", "Duplicate application to same item"),
      409
    );
    call("DELETE", "/api/claims/" + c, alice, null, 403);
    call("DELETE", "/api/claims/" + c, bob, null, 200);
    assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM claim", Integer.class));
    Map<String, Object> b = itemBody();
    b.put("type", "lost");
    long lost = data(call("POST", "/api/items", alice, b, 200)).get("id").asLong();
    call(
      "POST",
      "/api/items/" + lost + "/claims",
      bob,
      map("description", "Cannot claim a lost item post"),
      400
    );
  }

  @Test
  void rejectionDoesNotFinishAndClosingRejectsPending() throws Exception {
    long i = item(), c = claim(i, bob);
    call(
      "PUT",
      "/api/admin/claims/" + c + "/review",
      admin,
      map("status", "rejected", "reply", "Features do not match"),
      200
    );
    assertEquals("open", db.queryForObject("SELECT status FROM item WHERE id=?", String.class, i));
    register("20260003", "13800000003");
    claim(i, login("20260003", "Student123"));
    call("PUT", "/api/items/" + i + "/status", alice, map("status", "closed"), 200);
    assertEquals(
      0,
      db.queryForObject("SELECT COUNT(*) FROM claim WHERE status='pending'", Integer.class)
    );
    call("PUT", "/api/items/" + i + "/status", admin, map("status", "open"), 400);
  }

  @Test
  void announcementRelationsAndTimeWindow() throws Exception {
    long i = item();
    call("POST", "/api/admin/notices", admin, notice(i, "2026-01-01 00:00:00", null), 200);
    call("POST", "/api/admin/notices", admin, notice(i, "2099-01-01 00:00:00", null), 200);
    call(
      "POST",
      "/api/admin/notices",
      admin,
      notice(i, "2026-01-01 00:00:00", "2026-01-02 00:00:00"),
      200
    );
    Map<String, Object> b = notice(i, "2026-01-01 00:00:00", null);
    b.put("enabled", false);
    call("POST", "/api/admin/notices", admin, b, 200);
    JsonNode rows = data(call("GET", "/api/notices", null, null, 200));
    assertEquals(1, rows.size());
    assertEquals(i, rows.get(0).get("item_id").asLong());
    assertEquals("Admin", rows.get(0).get("publisher_name").asText());
    assertEquals(4, data(call("GET", "/api/notices?all=true", admin, null, 200)).size());
    call("GET", "/api/notices?all=true", bob, null, 403);
  }

  @Test
  void announcementValidationAndForeignKeys() throws Exception {
    long i = item();
    Map<String, Object> b = notice(i, "2026-01-03 00:00:00", "2026-01-02 00:00:00");
    call("POST", "/api/admin/notices", admin, b, 400);
    b = notice(i, "2026-01-01 00:00:00", null);
    b.put("item_id", null);
    b.put("category_id", null);
    call("POST", "/api/admin/notices", admin, b, 400);
    b.put("item_id", 999999);
    call("POST", "/api/admin/notices", admin, b, 404);
    call("POST", "/api/admin/notices", bob, notice(i, "2026-01-01 00:00:00", null), 403);
  }

  @Test
  void disabledAccountLosesExistingJwt() throws Exception {
    long uid = db.queryForObject(
      "SELECT id FROM app_user WHERE student_number='20260002'",
      Long.class
    );
    call("PUT", "/api/admin/users/" + uid, admin, map("enabled", false), 200);
    call("GET", "/api/auth/me", bob, null, 403);
    call(
      "POST",
      "/api/auth/login",
      null,
      map("account", "20260002", "password", "Student123"),
      403
    );
  }

  @Test
  void deletingItemCascadesClaimsAndPreservesNotice() throws Exception {
    long i = item();
    claim(i, bob);
    call("POST", "/api/admin/notices", admin, notice(i, "2026-01-01 00:00:00", null), 200);
    call("DELETE", "/api/items/" + i, alice, null, 200);
    assertEquals(0, db.queryForObject("SELECT COUNT(*) FROM claim", Integer.class));
    assertNull(db.queryForObject("SELECT item_id FROM notice", Long.class));
    assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM notice", Integer.class));
    call("DELETE", "/api/admin/categories/" + category, admin, null, 409);
  }

  @Test
  void writesRequireCustomHeader() throws Exception {
    MvcResult r = mvc
      .perform(
        post("/api/auth/login")
          .contentType("application/json")
          .content(json.writeValueAsBytes(map("account", "admin", "password", "Admin12345")))
      )
      .andReturn();
    assertEquals(403, r.getResponse().getStatus());
  }

  @Test
  void simultaneousApprovalsCannotDoubleClaim() throws Exception {
    long i = item(), a = claim(i, bob);
    register("20260003", "13800000003");
    long b = claim(i, login("20260003", "Student123"));
    confirm(a);
    confirm(b);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    CountDownLatch start = new CountDownLatch(1);
    try {
      List<Future<Integer>> results = new ArrayList<>();
      for (long id : new long[] { a, b }) results.add(
        pool.submit(() -> {
          start.await();
          return mvc
            .perform(
              put("/api/admin/claims/" + id + "/review")
                .cookie(admin)
                .header("X-Requested-With", "campus")
                .contentType("application/json")
                .content(json.writeValueAsBytes(map("status", "approved", "reply", "Verified")))
            )
            .andReturn()
            .getResponse()
            .getStatus();
        })
      );
      start.countDown();
      List<Integer> codes = Arrays.asList(
        results.get(0).get(15, TimeUnit.SECONDS),
        results.get(1).get(15, TimeUnit.SECONDS)
      );
      assertTrue(codes.contains(200));
      assertTrue(codes.contains(400));
      assertEquals(
        1,
        db.queryForObject("SELECT COUNT(*) FROM claim WHERE status='approved'", Integer.class)
      );
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void jwtRolesTamperingExpiryAndLogout() throws Exception {
    String[] parts = bob.getValue().split("\\.");
    JsonNode payload = json.readTree(Base64.getUrlDecoder().decode(parts[1]));
    assertEquals("user", payload.get("role").asText());
    assertTrue(payload.get("exp").asLong() > payload.get("iat").asLong());
    assertNotNull(payload.get("jti"));
    String fakePayload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
      .replace("\"user\"", "\"admin\"");
    Cookie forged = new Cookie(
      JwtTokens.COOKIE,
      parts[0] +
      "." +
      Base64
        .getUrlEncoder()
        .withoutPadding()
        .encodeToString(fakePayload.getBytes(StandardCharsets.UTF_8)) +
      "." +
      parts[2]
    );
    call("GET", "/api/admin/users", forged, null, 401);
    long uid = db.queryForObject(
      "SELECT id FROM app_user WHERE student_number='20260002'",
      Long.class
    );
    byte[] key = Base64.getDecoder().decode("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
    String expired = Jwts
      .builder()
      .setIssuer("campus-lost-found")
      .setSubject(Long.toString(uid))
      .claim("role", "user")
      .setId("expired-test")
      .setIssuedAt(new Date(System.currentTimeMillis() - 7200000))
      .setExpiration(new Date(System.currentTimeMillis() - 3600000))
      .signWith(Keys.hmacShaKeyFor(key), SignatureAlgorithm.HS256)
      .compact();
    call("GET", "/api/auth/me", new Cookie(JwtTokens.COOKIE, expired), null, 401);
    String wrongRole = Jwts
      .builder()
      .setIssuer("campus-lost-found")
      .setSubject(Long.toString(uid))
      .claim("role", "admin")
      .setId("role-mismatch")
      .setIssuedAt(new Date())
      .setExpiration(new Date(System.currentTimeMillis() + 60000))
      .signWith(Keys.hmacShaKeyFor(key), SignatureAlgorithm.HS256)
      .compact();
    call("GET", "/api/admin/users", new Cookie(JwtTokens.COOKIE, wrongRole), null, 401);
    assertEquals(
      200,
      mvc
        .perform(get("/api/admin/users").header("Authorization", "Bearer " + admin.getValue()))
        .andReturn()
        .getResponse()
        .getStatus()
    );
    call("POST", "/api/auth/logout", bob, null, 200);
    call("GET", "/api/auth/me", bob, null, 401);
    assertFalse(
      db
        .queryForObject(
          "SELECT password FROM app_user WHERE student_number='20260001'",
          String.class
        )
        .equals(
          db.queryForObject(
            "SELECT password FROM app_user WHERE student_number='20260002'",
            String.class
          )
        )
    );
  }

  @Test
  void publisherMustConfirmBeforeAdminApproval() throws Exception {
    long i = item(), c = claim(i, bob);
    call(
      "PUT",
      "/api/admin/claims/" + c + "/review",
      admin,
      map("status", "approved", "reply", "Not yet confirmed"),
      400
    );
    call(
      "PUT",
      "/api/claims/" + c + "/confirm",
      bob,
      map("decision", "confirmed", "reply", "Not my posted item"),
      403
    );
    call(
      "PUT",
      "/api/claims/" + c + "/confirm",
      admin,
      map("decision", "confirmed", "reply", "Admin cannot impersonate publisher"),
      403
    );
    assertEquals(1, data(call("GET", "/api/claims/incoming", alice, null, 200)).size());
    assertEquals(0, data(call("GET", "/api/claims/incoming", bob, null, 200)).size());
    confirm(c);
    assertNotNull(
      db.queryForObject(
        "SELECT publisher_confirmed_at FROM claim WHERE id=?",
        java.sql.Timestamp.class,
        c
      )
    );
    call(
      "PUT",
      "/api/claims/" + c + "/confirm",
      alice,
      map("decision", "confirmed", "reply", "Repeated confirmation"),
      400
    );
    call(
      "PUT",
      "/api/admin/claims/" + c + "/review",
      admin,
      map("status", "approved", "reply", "Confirmed and verified"),
      200
    );
    assertEquals(
      "finished",
      db.queryForObject("SELECT status FROM item WHERE id=?", String.class, i)
    );
    assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM claim WHERE id=?", Integer.class, c));
    assertEquals(
      0,
      data(call("GET", "/api/items?status=open", null, null, 200)).get("total").asInt()
    );
  }

  @Test
  void publisherRejectionPreservesReasonWithoutAdminImpersonation() throws Exception {
    long c = claim(item(), bob);
    String reason = String.join("", Collections.nCopies(500, "x"));
    call(
      "PUT",
      "/api/claims/" + c + "/confirm",
      alice,
      map("decision", "rejected", "reply", reason),
      200
    );
    assertEquals(
      "rejected",
      db.queryForObject("SELECT status FROM claim WHERE id=?", String.class, c)
    );
    assertEquals(
      reason,
      db.queryForObject("SELECT publisher_reply FROM claim WHERE id=?", String.class, c)
    );
    assertNull(db.queryForObject("SELECT reviewer_id FROM claim WHERE id=?", Long.class, c));
    call(
      "PUT",
      "/api/admin/claims/" + c + "/review",
      admin,
      map("status", "approved", "reply", "Cannot approve rejected"),
      400
    );
  }

  @Test
  void chineseKeywordsAndOccurrenceTimeFilters() throws Exception {
    Map<String, Object> b = itemBody();
    b.put("title", "图书馆拾到黑色雨伞");
    b.put("description", "黑色长柄雨伞，手柄贴有蓝色标签");
    call("POST", "/api/items", alice, b, 200);
    assertEquals(
      1,
      data(call("GET", "/api/items?keyword=雨伞", null, null, 200)).get("total").asInt()
    );
    assertEquals(
      1,
      data(call("GET", "/api/items?keyword=黑色 雨伞", null, null, 200)).get("total").asInt()
    );
    assertEquals(
      0,
      data(call("GET", "/api/items?keyword=黑色 耳机", null, null, 200)).get("total").asInt()
    );
    assertEquals(
      1,
      data(
        call(
          "GET",
          "/api/items?occurred_from=2026-01-01 09:00:00&occurred_until=2026-01-01 11:00:00",
          null,
          null,
          200
        )
      )
        .get("total")
        .asInt()
    );
    assertEquals(
      0,
      data(call("GET", "/api/items?occurred_from=2026-01-02 00:00:00", null, null, 200))
        .get("total")
        .asInt()
    );
    call(
      "GET",
      "/api/items?occurred_from=2026-01-02 00:00:00&occurred_until=2026-01-01 00:00:00",
      null,
      null,
      400
    );
    assertEquals(
      0,
      data(call("GET", "/api/items?keyword=%", null, null, 200)).get("total").asInt()
    );
  }

  @Autowired
  SchemaUpgrade upgrade;

  @Test
  void upgradingOldSchemaPreservesDataAndIsIdempotent() throws Exception {
    long c = claim(item(), bob);
    db.update("UPDATE claim SET status='approved' WHERE id=?", c);
    db.execute("ALTER TABLE claim DROP COLUMN publisher_status");
    db.execute("ALTER TABLE claim DROP COLUMN publisher_reply");
    db.execute("ALTER TABLE claim DROP COLUMN publisher_confirmed_at");
    db.update("UPDATE app_user SET role='student' WHERE student_number='20260002'");
    upgrade.run();
    assertEquals(
      "legacy",
      db.queryForObject("SELECT publisher_status FROM claim WHERE id=?", String.class, c)
    );
    assertEquals(
      "user",
      db.queryForObject("SELECT role FROM app_user WHERE student_number='20260002'", String.class)
    );
    upgrade.run();
    assertEquals(1, db.queryForObject("SELECT COUNT(*) FROM claim WHERE id=?", Integer.class, c));
    assertNull(
      db.queryForObject(
        "SELECT publisher_confirmed_at FROM claim WHERE id=?",
        java.sql.Timestamp.class,
        c
      )
    );
  }
 @Test void demoSqlIsRepeatableAndLinked()throws Exception{
  org.springframework.jdbc.datasource.init.ResourceDatabasePopulator pop=new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(new org.springframework.core.io.ByteArrayResource(new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("../database/demo-data.sql")), java.nio.charset.StandardCharsets.UTF_8).replace("START TRANSACTION;", "").replace("COMMIT;", "").getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  pop.setSqlScriptEncoding("UTF-8");pop.execute(db.getDataSource());
  int items=db.queryForObject("SELECT COUNT(*) FROM item",Integer.class),claims=db.queryForObject("SELECT COUNT(*) FROM claim",Integer.class);
  assertEquals(4,items);assertEquals(2,claims);assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM notice WHERE item_id IS NOT NULL AND category_id IS NOT NULL",Integer.class));
  pop.execute(db.getDataSource());assertEquals(items,db.queryForObject("SELECT COUNT(*) FROM item",Integer.class));assertEquals(claims,db.queryForObject("SELECT COUNT(*) FROM claim",Integer.class));
 }
}
