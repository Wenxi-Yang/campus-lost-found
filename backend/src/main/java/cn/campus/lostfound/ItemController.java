package cn.campus.lostfound;

import java.sql.Timestamp;
import java.util.*;
import javax.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ItemController {

  private final Store s;
  private final Auth auth;
  static final String ITEM =
    "SELECT i.*,u.name publisher_name,c.name category_name FROM item i JOIN app_user u ON i.user_id=u.id JOIN category c ON i.category_id=c.id ";

  public ItemController(Store s, Auth auth) {
    this.s = s;
    this.auth = auth;
  }

  @GetMapping("/categories")
  public Object categories() {
    return s.db.queryForList("SELECT * FROM category WHERE enabled=TRUE ORDER BY sort_no,id");
  }

  @GetMapping("/items")
  public Object list(
    @RequestParam(defaultValue = "") String keyword,
    @RequestParam(defaultValue = "") String location,
    @RequestParam(required = false) Long category_id,
    @RequestParam(defaultValue = "") String type,
    @RequestParam(defaultValue = "") String status,
    @RequestParam(defaultValue = "") String occurred_from,
    @RequestParam(defaultValue = "") String occurred_until,
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "12") int size,
    @RequestParam(defaultValue = "false") boolean mine,
    HttpServletRequest request
  ) {
    page = Math.max(1, page);
    size = Math.min(100, Math.max(1, size));
    StringBuilder w = new StringBuilder(" WHERE 1=1");
    List<Object> a = new ArrayList<>();
    if (keyword.length() > 100) throw ApiError.bad("搜索关键词最多100字");
    if (!keyword.trim().isEmpty()) {
      String[] terms = keyword.trim().split("\\s+");
      if (terms.length > 8) throw ApiError.bad("最多同时输入8个关键词");
      for (String term : terms) {
        w.append(
          " AND (LOWER(i.title) LIKE LOWER(?) ESCAPE '!' OR LOWER(i.description) LIKE LOWER(?) ESCAPE '!')"
        );
        String pattern = "%" + term.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        a.add(pattern);
        a.add(pattern);
      }
    }
    Map<String, Object> times = new HashMap<>();
    times.put("occurred_from", occurred_from);
    times.put("occurred_until", occurred_until);
    Timestamp from = Input.time(times, "occurred_from", false), until = Input.time(
      times,
      "occurred_until",
      false
    );
    if (from != null && until != null && from.after(until)) throw ApiError.bad(
      "开始时间不能晚于结束时间"
    );
    if (from != null) {
      w.append(" AND i.occurred_at>=?");
      a.add(from);
    }
    if (until != null) {
      w.append(" AND i.occurred_at<=?");
      a.add(until);
    }
    if (!location.isEmpty()) {
      w.append(" AND i.location LIKE ?");
      a.add("%" + location + "%");
    }
    if (category_id != null) {
      w.append(" AND i.category_id=?");
      a.add(category_id);
    }
    if (!type.isEmpty()) {
      w.append(" AND i.type=?");
      a.add(type);
    }
    if (!status.isEmpty()) {
      w.append(" AND i.status=?");
      a.add(status);
    }
    if (mine) {
      w.append(" AND i.user_id=?");
      a.add(auth.user(request).get("id"));
    }
    long total = s.db.queryForObject("SELECT COUNT(*) FROM item i" + w, Long.class, a.toArray());
    a.add(size);
    a.add((long) (page - 1) * size);
    List<Map<String, Object>> rows = s.db.queryForList(
      ITEM + w + " ORDER BY i.created_at DESC,i.id DESC LIMIT ? OFFSET ?",
      a.toArray()
    );
    rows.forEach(r -> r.remove("contact"));
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("rows", rows);
    result.put("total", total);
    return result;
  }

  @GetMapping("/items/{id}")
  public Object detail(@PathVariable long id, HttpServletRequest request) {
    Map<String, Object> r = s.one(ITEM + " WHERE i.id=?", id);
    if (!auth.authenticated(request)) r.remove("contact"); else auth.user(request);
    return r;
  }

  private Object[] fields(Map<String, Object> b) {
    String title = Input.text(b, "title", 2, 100), type = Input.choice(
      b,
      "type",
      "lost",
      "found"
    ), location = Input.text(b, "location", 1, 100), description = Input.text(
      b,
      "description",
      5,
      5000
    ), contact = Input.text(b, "contact", 3, 100);
    long category = Input.number(b, "category_id");
    if (
      !Store.yes(s.one("SELECT enabled FROM category WHERE id=?", category).get("enabled"))
    ) throw ApiError.bad("分类已停用");
    Timestamp occurred = Input.time(b, "occurred_at", true);
    if (occurred.after(new Timestamp(System.currentTimeMillis() + 60000))) throw ApiError.bad(
      "丢失或拾获时间不能晚于当前时间"
    );
    return new Object[] { title, type, category, location, occurred, description, contact };
  }

  @PostMapping("/items")
  public Object create(@RequestBody Map<String, Object> b, HttpServletRequest request) {
    Map<String, Object> u = auth.user(request);
    List<Object> a = new ArrayList<>(Arrays.asList(fields(b)));
    a.add(u.get("id"));
    return Collections.singletonMap(
      "id",
      s.insert(
        "INSERT INTO item(title,type,category_id,location,occurred_at,description,contact,user_id) VALUES(?,?,?,?,?,?,?,?)",
        a.toArray()
      )
    );
  }

  @PutMapping("/items/{id}")
  @Transactional
  public Object edit(
    @PathVariable long id,
    @RequestBody Map<String, Object> b,
    HttpServletRequest request
  ) {
    Map<String, Object> u = auth.user(request), old = s.one(
      "SELECT * FROM item WHERE id=? FOR UPDATE",
      id
    );
    auth.owner(u, old);
    if (!"open".equals(old.get("status"))) throw ApiError.bad("仅进行中的信息可以编辑");
    Object[] fields = fields(b);
    if (
      !old.get("type").equals(fields[1]) &&
      s.db.queryForObject("SELECT COUNT(*) FROM claim WHERE item_id=?", Integer.class, id) > 0
    ) throw ApiError.bad("已有申请，不能更改信息类型");
    List<Object> a = new ArrayList<>(Arrays.asList(fields));
    a.add(id);
    s.db.update(
      "UPDATE item SET title=?,type=?,category_id=?,location=?,occurred_at=?,description=?,contact=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",
      a.toArray()
    );
    return detail(id, request);
  }

  @PutMapping("/items/{id}/status")
  @Transactional
  public Object status(
    @PathVariable long id,
    @RequestBody Map<String, Object> b,
    HttpServletRequest request
  ) {
    Map<String, Object> u = auth.user(request), r = s.one(
      "SELECT * FROM item WHERE id=? FOR UPDATE",
      id
    );
    auth.owner(u, r);
    String v = Input.choice(b, "status", "open", "finished", "closed");
    String old = (String) r.get("status");
    if (old.equals(v)) return Collections.singletonMap("message", "状态未改变");
    if ("closed".equals(old)) throw ApiError.bad("已关闭的信息不可重新开启");
    if ("finished".equals(old) && !"closed".equals(v)) throw ApiError.bad("已完结的信息仅可关闭");
    s.db.update(
      "UPDATE item SET status=?,finished_at=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",
      v,
      "finished".equals(v) ? new Timestamp(System.currentTimeMillis()) : r.get("finished_at"),
      id
    );
    if (!"open".equals(v)) s.db.update(
      "UPDATE claim SET status='rejected',reply='物品已完结或关闭',reviewer_id=?,reviewed_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE item_id=? AND status='pending'",
      u.get("id"),
      id
    );
    return Collections.singletonMap("message", "状态已更新");
  }

  @DeleteMapping("/items/{id}")
  @Transactional
  public Object delete(@PathVariable long id, HttpServletRequest request) {
    Map<String, Object> u = auth.user(request), r = s.one(
      "SELECT * FROM item WHERE id=? FOR UPDATE",
      id
    );
    auth.owner(u, r);
    s.db.update("DELETE FROM item WHERE id=?", id);
    return Collections.singletonMap("message", "已删除");
  }

  // 提交时锁定物品，和审核/关闭操作串行，防止对已完结物品插入待审申请。
  @PostMapping("/items/{id}/claims")
  @Transactional
  public Object claim(
    @PathVariable long id,
    @RequestBody Map<String, Object> b,
    HttpServletRequest request
  ) {
    Map<String, Object> u = auth.user(request), r = s.one(
      "SELECT * FROM item WHERE id=? FOR UPDATE",
      id
    );
    if (Store.id(u, "id") == Store.id(r, "user_id")) throw ApiError.bad("不能认领自己发布的物品");
    if (!"found".equals(r.get("type")) || !"open".equals(r.get("status"))) throw ApiError.bad(
      "只能认领进行中的招领物品"
    );
    String desc = Input.text(b, "description", 10, 3000);
    return Collections.singletonMap(
      "id",
      s.insert(
        "INSERT INTO claim(item_id,user_id,description) VALUES(?,?,?)",
        id,
        u.get("id"),
        desc
      )
    );
  }

  @GetMapping("/claims/incoming")
  public Object incoming(HttpServletRequest request) {
    Map<String, Object> u = auth.user(request);
    return s.db.queryForList(
      "SELECT c.*,i.title,i.status item_status,u.name applicant_name FROM claim c JOIN item i ON c.item_id=i.id JOIN app_user u ON c.user_id=u.id WHERE i.user_id=? ORDER BY c.created_at DESC,c.id DESC",
      u.get("id")
    );
  }

  @PutMapping("/claims/{id}/confirm")
  @Transactional
  public Object confirm(
    @PathVariable long id,
    @RequestBody Map<String, Object> b,
    HttpServletRequest request
  ) {
    Map<String, Object> u = auth.user(request), c = s.one("SELECT * FROM claim WHERE id=?", id);
    Map<String, Object> item = s.one("SELECT * FROM item WHERE id=? FOR UPDATE", c.get("item_id"));
    c = s.one("SELECT * FROM claim WHERE id=? FOR UPDATE", id);
    if (Store.id(u, "id") != Store.id(item, "user_id")) throw ApiError.denied();
    if (
      !"pending".equals(c.get("status")) || !"pending".equals(c.get("publisher_status"))
    ) throw ApiError.bad("该申请已处理，不能重复确认");
    if (!"open".equals(item.get("status"))) throw ApiError.bad("物品已完结或关闭");
    String decision = Input.choice(b, "decision", "confirmed", "rejected"), reply = Input.text(
      b,
      "reply",
      1,
      500
    );
    s.db.update(
      "UPDATE claim SET publisher_status=?,publisher_reply=?,publisher_confirmed_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE id=?",
      decision,
      reply,
      id
    );
    if ("rejected".equals(decision)) s.db.update(
      "UPDATE claim SET status='rejected',reply=?,updated_at=CURRENT_TIMESTAMP WHERE id=?",
      reply,
      id
    );
    return Collections.singletonMap(
      "message",
      "confirmed".equals(decision) ? "已确认，申请转交管理员审核" : "已拒绝该申请"
    );
  }

  @GetMapping("/claims/mine")
  public Object myClaims(HttpServletRequest request) {
    Map<String, Object> u = auth.user(request);
    return s.db.queryForList(
      "SELECT c.*,i.title,i.status item_status FROM claim c JOIN item i ON c.item_id=i.id WHERE c.user_id=? ORDER BY c.created_at DESC,c.id DESC",
      u.get("id")
    );
  }

  @DeleteMapping("/claims/{id}")
  @Transactional
  public Object cancel(@PathVariable long id, HttpServletRequest request) {
    Map<String, Object> u = auth.user(request), c = s.one("SELECT * FROM claim WHERE id=?", id);
    s.one("SELECT id FROM item WHERE id=? FOR UPDATE", c.get("item_id"));
    c = s.one("SELECT * FROM claim WHERE id=? FOR UPDATE", id);
    if (Store.id(c, "user_id") != Store.id(u, "id")) throw ApiError.denied();
    if (!"pending".equals(c.get("status"))) throw ApiError.bad("只能撤回待审核申请");
    s.db.update("DELETE FROM claim WHERE id=?", id);
    return Collections.singletonMap("message", "申请已撤回");
  }
}
