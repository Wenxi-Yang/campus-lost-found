package cn.campus.lostfound;

import java.sql.Timestamp;
import java.util.*;
import javax.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AdminController {

  private final Store s;
  private final Auth auth;
  private static final String NOTICES =
    "SELECT n.*,u.name publisher_name,i.title item_title,c.name category_name FROM notice n JOIN app_user u ON n.publisher_id=u.id LEFT JOIN item i ON n.item_id=i.id LEFT JOIN category c ON n.category_id=c.id ";

  public AdminController(Store s, Auth auth) {
    this.s = s;
    this.auth = auth;
  }

  @GetMapping("/notices")
  public Object notices(
    @RequestParam(defaultValue = "false") boolean all,
    HttpServletRequest request
  ) {
    if (all) {
      auth.admin(request);
      return s.db.queryForList(NOTICES + " ORDER BY n.published_at DESC,n.id DESC");
    }
    return s.db.queryForList(
      NOTICES +
      " WHERE n.enabled=TRUE AND n.published_at<=CURRENT_TIMESTAMP AND n.starts_at<=CURRENT_TIMESTAMP AND (n.expires_at IS NULL OR n.expires_at>CURRENT_TIMESTAMP) ORDER BY n.published_at DESC,n.id DESC"
    );
  }

  @GetMapping("/admin/users")
  public Object users(HttpServletRequest request) {
    auth.admin(request);
    return s.db.queryForList(
      "SELECT id,student_number,name,phone,role,enabled,created_at,updated_at FROM app_user ORDER BY id"
    );
  }

  @PutMapping("/admin/users/{id}")
  @Transactional
  public Object user(
    @PathVariable long id,
    @RequestBody Map<String, Object> b,
    HttpServletRequest request
  ) {
    Map<String, Object> u = auth.admin(request);
    boolean enabled = Input.bool(b, "enabled");
    Map<String, Object> target = s.one("SELECT * FROM app_user WHERE id=?", id);
    if ("admin".equals(target.get("role"))) throw ApiError.bad("管理员账号不允许停用");
    s.db.update(
      "UPDATE app_user SET enabled=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",
      enabled,
      id
    );
    return Collections.singletonMap("message", "账号状态已更新");
  }

  @GetMapping("/admin/claims")
  public Object claims(HttpServletRequest request) {
    auth.admin(request);
    return s.db.queryForList(
      "SELECT c.*,i.title,u.name applicant_name,u.phone applicant_phone FROM claim c JOIN item i ON c.item_id=i.id JOIN app_user u ON c.user_id=u.id ORDER BY CASE WHEN c.status='pending' THEN 0 ELSE 1 END,c.created_at DESC,c.id DESC"
    );
  }

  // 审核必须在同一事务内按物品→申请的顺序加锁，避免同一物品被重复认领。
  @PutMapping("/admin/claims/{id}/review")
  @Transactional
  public Object review(
    @PathVariable long id,
    @RequestBody Map<String, Object> b,
    HttpServletRequest request
  ) {
    Map<String, Object> u = auth.admin(request), c = s.one("SELECT * FROM claim WHERE id=?", id);
    Map<String, Object> item = s.one("SELECT * FROM item WHERE id=? FOR UPDATE", c.get("item_id"));
    c = s.one("SELECT * FROM claim WHERE id=? FOR UPDATE", id);
    if (!"pending".equals(c.get("status"))) throw ApiError.bad("该申请已审核");
    if (!"open".equals(item.get("status"))) throw ApiError.bad("物品已完结或关闭");
    String status = Input.choice(b, "status", "approved", "rejected"), reply = Input.text(
      b,
      "reply",
      1,
      500
    );
    if (
      "approved".equals(status) && !"confirmed".equals(c.get("publisher_status"))
    ) throw ApiError.bad("请先由该物品的招领人确认，再通过审核");
    s.db.update(
      "UPDATE claim SET status=?,reply=?,reviewer_id=?,reviewed_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE id=?",
      status,
      reply,
      u.get("id"),
      id
    );
    if ("approved".equals(status)) {
      s.db.update(
        "UPDATE item SET status='finished',finished_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE id=?",
        item.get("id")
      );
      s.db.update(
        "UPDATE claim SET status='rejected',reply='该物品已通过其他认领申请',reviewer_id=?,reviewed_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE item_id=? AND status='pending'",
        u.get("id"),
        item.get("id")
      );
    }
    return Collections.singletonMap("message", "审核完成，物品状态已同步");
  }

  @GetMapping("/admin/categories")
  public Object categories(HttpServletRequest request) {
    auth.admin(request);
    return s.db.queryForList("SELECT * FROM category ORDER BY sort_no,id");
  }

  private Object[] categoryFields(Map<String, Object> b) {
    String name = Input.text(b, "name", 1, 50);
    int sort;
    try {
      sort = Integer.parseInt(Objects.toString(b.get("sort_no"), "0"));
    } catch (Exception e) {
      throw ApiError.bad("排序必须为整数");
    }
    return new Object[] { name, sort, Input.bool(b, "enabled") };
  }

  @PostMapping("/admin/categories")
  public Object createCategory(@RequestBody Map<String, Object> b, HttpServletRequest request) {
    auth.admin(request);
    return Collections.singletonMap(
      "id",
      s.insert("INSERT INTO category(name,sort_no,enabled) VALUES(?,?,?)", categoryFields(b))
    );
  }

  @PutMapping("/admin/categories/{id}")
  public Object updateCategory(
    @PathVariable long id,
    @RequestBody Map<String, Object> b,
    HttpServletRequest request
  ) {
    auth.admin(request);
    s.one("SELECT id FROM category WHERE id=?", id);
    List<Object> a = new ArrayList<>(Arrays.asList(categoryFields(b)));
    a.add(id);
    s.db.update(
      "UPDATE category SET name=?,sort_no=?,enabled=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",
      a.toArray()
    );
    return Collections.singletonMap("message", "分类已保存");
  }

  @DeleteMapping("/admin/categories/{id}")
  public Object deleteCategory(@PathVariable long id, HttpServletRequest request) {
    auth.admin(request);
    s.one("SELECT id FROM category WHERE id=?", id);
    s.db.update("DELETE FROM category WHERE id=?", id);
    return Collections.singletonMap("message", "分类已删除");
  }

  private Object[] noticeFields(Map<String, Object> b) {
    String title = Input.text(b, "title", 2, 100), content = Input.text(
      b,
      "content",
      5,
      10000
    ), kind = Input.choice(b, "kind", "system", "reminder", "result");
    Long item = Input.optionalId(b, "item_id"), category = Input.optionalId(b, "category_id");
    if (item != null) s.one("SELECT id FROM item WHERE id=?", item);
    if (category != null) s.one("SELECT id FROM category WHERE id=?", category);
    if (!"system".equals(kind) && item == null && category == null) throw ApiError.bad(
      "招领提醒和认领结果公告必须关联物品或分类"
    );
    Timestamp published = Input.time(b, "published_at", true), start = Input.time(
      b,
      "starts_at",
      true
    ), end = Input.time(b, "expires_at", false);
    if (end != null && (!end.after(start) || !end.after(published))) throw ApiError.bad(
      "截止时间必须晚于发布时间和生效时间"
    );
    return new Object[] {
      title,
      content,
      kind,
      item,
      category,
      Input.bool(b, "enabled"),
      published,
      start,
      end,
    };
  }

  @PostMapping("/admin/notices")
  public Object createNotice(@RequestBody Map<String, Object> b, HttpServletRequest request) {
    Map<String, Object> u = auth.admin(request);
    List<Object> a = new ArrayList<>(Arrays.asList(noticeFields(b)));
    a.add(u.get("id"));
    return Collections.singletonMap(
      "id",
      s.insert(
        "INSERT INTO notice(title,content,kind,item_id,category_id,enabled,published_at,starts_at,expires_at,publisher_id) VALUES(?,?,?,?,?,?,?,?,?,?)",
        a.toArray()
      )
    );
  }

  @PutMapping("/admin/notices/{id}")
  public Object updateNotice(
    @PathVariable long id,
    @RequestBody Map<String, Object> b,
    HttpServletRequest request
  ) {
    auth.admin(request);
    s.one("SELECT id FROM notice WHERE id=?", id);
    List<Object> a = new ArrayList<>(Arrays.asList(noticeFields(b)));
    a.add(id);
    s.db.update(
      "UPDATE notice SET title=?,content=?,kind=?,item_id=?,category_id=?,enabled=?,published_at=?,starts_at=?,expires_at=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",
      a.toArray()
    );
    return Collections.singletonMap("message", "公告已保存");
  }

  @DeleteMapping("/admin/notices/{id}")
  public Object deleteNotice(@PathVariable long id, HttpServletRequest request) {
    auth.admin(request);
    s.db.update("DELETE FROM notice WHERE id=?", id);
    return Collections.singletonMap("message", "公告已删除");
  }

  @GetMapping("/stats")
  public Object stats() {
    Map<String, Object> r = new LinkedHashMap<>();
    r.put("total", s.db.queryForObject("SELECT COUNT(*) FROM item", Long.class));
    r.put("open", s.db.queryForObject("SELECT COUNT(*) FROM item WHERE status='open'", Long.class));
    r.put(
      "finished",
      s.db.queryForObject("SELECT COUNT(*) FROM item WHERE status='finished'", Long.class)
    );
    return r;
  }

  @GetMapping("/admin/stats")
  public Object adminStats(HttpServletRequest request) {
    auth.admin(request);
    Map<String, Object> r = new LinkedHashMap<>();
    r.put(
      "pending",
      s.db.queryForObject("SELECT COUNT(*) FROM claim WHERE status='pending'", Long.class)
    );
    r.put("users", s.db.queryForObject("SELECT COUNT(*) FROM app_user", Long.class));
    r.put(
      "categories",
      s.db.queryForList(
        "SELECT c.name,COUNT(i.id) total FROM category c LEFT JOIN item i ON i.category_id=c.id GROUP BY c.id,c.name ORDER BY total DESC"
      )
    );
    r.put("types", s.db.queryForList("SELECT type,COUNT(*) total FROM item GROUP BY type"));
    r.put("statuses", s.db.queryForList("SELECT status,COUNT(*) total FROM item GROUP BY status"));
    return r;
  }
}
