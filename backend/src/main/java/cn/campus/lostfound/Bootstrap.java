package cn.campus.lostfound;

import java.sql.Timestamp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(10)
public class Bootstrap implements CommandLineRunner {

  private final Store s;
  private final BCryptPasswordEncoder encoder;

  @Value("${app.bootstrap-admin-password:}")
  private String password;

  @Value("${app.demo:false}")
  private boolean demo;

  public Bootstrap(Store s, BCryptPasswordEncoder encoder) {
    this.s = s;
    this.encoder = encoder;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (s.db.queryForObject("SELECT COUNT(*) FROM category", Long.class) == 0) {
      String[] names = { "证件卡片", "电子数码", "学习用品", "生活用品", "衣物配饰", "其他物品" };
      for (int i = 0; i < names.length; i++) s.db.update(
        "INSERT INTO category(name,sort_no) VALUES(?,?)",
        names[i],
        i
      );
    }
    if (s.db.queryForObject("SELECT COUNT(*) FROM app_user WHERE role='admin'", Long.class) == 0) {
      if (
        password.length() < 8 ||
        password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72
      ) throw new IllegalStateException(
        "首次启动请设置环境变量 ADMIN_PASSWORD（至少8位），以创建管理员"
      );
      s.db.update(
        "INSERT INTO app_user(student_number,name,phone,password,role) VALUES('admin','校园服务中心','13900000000',?,'admin')",
        encoder.encode(password)
      );
    }
    if (
      demo &&
      s.db.queryForObject(
        "SELECT COUNT(*) FROM app_user WHERE student_number='20260001'",
        Long.class
      ) ==
      0
    ) {
      long user = s.insert(
        "INSERT INTO app_user(student_number,name,phone,password,role) VALUES('20260001','林同学','13800000001',?,'user')",
        encoder.encode("Student123")
      );
      long category =
        ((Number) s.one("SELECT id FROM category WHERE name='电子数码'").get("id")).longValue();
      long item = s.insert(
        "INSERT INTO item(user_id,category_id,title,type,location,occurred_at,description,contact) VALUES(?,?,?,'found',?,?,?,?)",
        user,
        category,
        "图书馆拾到一副无线耳机",
        "图书馆二楼自习区",
        new Timestamp(System.currentTimeMillis() - 3600000),
        "一副装在保护盒里的无线耳机。请在认领时说明品牌、颜色和保护盒特征。",
        "校园服务中心，请登录后联系"
      );
      s.db.update(
        "INSERT INTO item(user_id,category_id,title,type,location,occurred_at,description,contact) VALUES(?,?,?,'lost',?,?,?,?)",
        user,
        category,
        "寻找黑色计算器",
        "教学楼 A 座",
        new Timestamp(System.currentTimeMillis() - 7200000),
        "自习结束后遗失的黑色科学计算器，背面有姓名标签。",
        "13800000001"
      );
      long admin =
        ((Number) s.one("SELECT id FROM app_user WHERE role='admin'").get("id")).longValue();
      s.db.update(
        "INSERT INTO notice(publisher_id,item_id,category_id,title,content,kind,published_at,starts_at) VALUES(?,?,?,?,?,'reminder',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",
        admin,
        item,
        category,
        "图书馆拾获物品认领提醒",
        "请先核对物品描述，再提交能够证明归属的特征信息。招领人确认并经管理员审核通过后联系服务中心办理交接。"
      );
      s.db.update(
        "INSERT INTO notice(publisher_id,title,content,kind,published_at,starts_at) VALUES(?,?,?,'system',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",
        admin,
        "校园失物招领平台使用说明",
        "公告栏用于发布服务通知、招领提醒和认领结果；关联公告可直接进入物品详情。请勿在公开描述中填写证件号码等隐私信息。"
      );
    }
  }
}
