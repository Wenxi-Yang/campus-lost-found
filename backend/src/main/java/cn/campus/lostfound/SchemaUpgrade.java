package cn.campus.lostfound;

import java.sql.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.stereotype.Component;

/** 为上一版已有数据库非破坏地补充字段，保留原有用户、物品和申请。 */
@Component
@Order(0)
public class SchemaUpgrade implements CommandLineRunner {

  private final Store s;

  public SchemaUpgrade(Store s) {
    this.s = s;
  }

  private boolean hasColumn(String column) {
    return Boolean.TRUE.equals(
      s.db.execute(
        (ConnectionCallback<Boolean>) c -> {
          try (ResultSet r = c.getMetaData().getColumns(c.getCatalog(), null, "claim", column)) {
            return r.next();
          }
        }
      )
    );
  }

  public void run(String... args) {
    boolean legacy = !hasColumn("publisher_status");
    if (legacy) s.db.execute(
      "ALTER TABLE claim ADD COLUMN publisher_status VARCHAR(16) NOT NULL DEFAULT 'pending'"
    );
    if (!hasColumn("publisher_reply")) s.db.execute(
      "ALTER TABLE claim ADD COLUMN publisher_reply VARCHAR(500)"
    );
    if (!hasColumn("publisher_confirmed_at")) s.db.execute(
      "ALTER TABLE claim ADD COLUMN publisher_confirmed_at TIMESTAMP NULL"
    );
    if (legacy) s.db.update("UPDATE claim SET publisher_status='legacy' WHERE status<>'pending'");
    s.db.update("UPDATE app_user SET role='user' WHERE role='student'");
    s.db.execute("ALTER TABLE app_user ALTER COLUMN role SET DEFAULT 'user'");
  }
}
