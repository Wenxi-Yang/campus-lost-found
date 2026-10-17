<template>
  <section>
    <div class="section-heading">
      <div>
        <span class="eyebrow">CAMPUS MANAGEMENT</span>
        <h1>管理工作台</h1>
        <p class="muted">维护校园信息，及时处理认领申请。</p>
      </div>
      <el-button icon="el-icon-refresh" @click="load">刷新数据</el-button>
    </div>
    <div class="metric-grid">
      <div class="metric">
        <span>待审核认领</span><strong>{{ stats.pending || 0 }}</strong>
      </div>
      <div class="metric">
        <span>注册用户</span><strong>{{ stats.users || 0 }}</strong>
      </div>
      <div class="metric">
        <span>公告记录</span><strong>{{ notices.length }}</strong>
      </div>
    </div>
    <el-tabs v-model="tab" class="panel admin-tabs"
      ><el-tab-pane label="认领审核" name="claims"
        ><p class="hint">
          招领人确认后才能通过审核；管理员可对无效申请直接驳回。审核通过仅完结归档，保留申请记录。
        </p>
        <el-table :data="claims" empty-text="暂无认领申请"
          ><el-table-column prop="title" label="关联物品" min-width="130" /><el-table-column
            prop="applicant_name"
            label="申请人"
            width="100"
          /><el-table-column prop="description" label="特征说明" min-width="220" /><el-table-column
            label="招领人确认"
            width="150"
            ><template slot-scope="s">{{
              confirmation(s.row.publisher_status)
            }}</template></el-table-column
          ><el-table-column
            prop="publisher_reply"
            label="招领人说明"
            min-width="180"
          /><el-table-column
            prop="publisher_confirmed_at"
            label="确认时间"
            width="170"
          /><el-table-column label="状态" width="100"
            ><template slot-scope="s"
              ><el-tag :type="tag(s.row.status)">{{ label(s.row.status) }}</el-tag></template
            ></el-table-column
          ><el-table-column prop="created_at" label="提交时间" width="170" /><el-table-column
            prop="reviewed_at"
            label="审核时间"
            width="170"
          /><el-table-column prop="reply" label="审核意见" min-width="160" /><el-table-column
            label="操作"
            width="150"
            fixed="right"
            ><template slot-scope="s"
              ><template v-if="s.row.status === 'pending'"
                ><el-button
                  type="text"
                  :disabled="s.row.publisher_status !== 'confirmed'"
                  @click="review(s.row, 'approved')"
                  >通过</el-button
                ><el-button type="text" class="danger" @click="review(s.row, 'rejected')"
                  >驳回</el-button
                ></template
              ><span v-else>已处理</span></template
            ></el-table-column
          ></el-table
        ></el-tab-pane
      ><el-tab-pane label="全部物品" name="items"
        ><div class="toolbar">
          <el-input
            v-model="itemKeyword"
            placeholder="搜索标题 / 描述"
            clearable
            @keyup.enter.native="loadItems"
          /><el-button
            @click="
              itemPage = 1;
              loadItems();
            "
            >查询</el-button
          >
        </div>
        <el-table :data="items"
          ><el-table-column prop="title" label="物品" min-width="200" /><el-table-column
            prop="publisher_name"
            label="发布人"
            width="100"
          /><el-table-column label="类型" width="80"
            ><template slot-scope="s">{{
              s.row.type === "lost" ? "失物" : "招领"
            }}</template></el-table-column
          ><el-table-column label="状态" width="110"
            ><template slot-scope="s">{{ label(s.row.status) }}</template></el-table-column
          ><el-table-column prop="occurred_at" label="发生时间" width="170" /><el-table-column
            label="操作"
            min-width="240"
            ><template slot-scope="s"
              ><el-button type="text" @click="$emit('detail', s.row.id)">详情</el-button
              ><el-button v-if="s.row.status === 'open'" type="text" @click="finish(s.row)"
                >完结</el-button
              ><el-button v-if="s.row.status !== 'closed'" type="text" @click="closeItem(s.row)"
                >关闭</el-button
              ><el-button type="text" class="danger" @click="remove('/items/' + s.row.id)"
                >删除</el-button
              ></template
            ></el-table-column
          ></el-table
        ><el-pagination
          layout="prev,pager,next,total"
          :current-page.sync="itemPage"
          :page-size="20"
          :total="itemTotal"
          @current-change="loadItems" /></el-tab-pane
      ><el-tab-pane label="用户管理" name="users"
        ><el-table :data="users"
          ><el-table-column prop="student_number" label="学号 / 账号" /><el-table-column
            prop="name"
            label="姓名" /><el-table-column prop="phone" label="手机号" /><el-table-column
            label="角色"
            ><template slot-scope="s">{{
              s.row.role === "admin" ? "管理员" : "学生"
            }}</template></el-table-column
          ><el-table-column prop="created_at" label="注册时间" width="180" /><el-table-column
            label="账号状态"
            ><template slot-scope="s"
              ><el-switch
                :value="!!s.row.enabled"
                :disabled="s.row.role === 'admin'"
                active-text="启用"
                @change="
                  toggleUser(s.row, $event)
                " /></template></el-table-column></el-table></el-tab-pane
      ><el-tab-pane label="分类维护" name="categories"
        ><el-button type="primary" size="small" @click="editCategory(null)">新增分类</el-button
        ><el-table :data="allCategories"
          ><el-table-column prop="name" label="分类名称" /><el-table-column
            prop="sort_no"
            label="排序"
          /><el-table-column label="状态"
            ><template slot-scope="s">{{
              s.row.enabled ? "启用" : "停用"
            }}</template></el-table-column
          ><el-table-column label="操作"
            ><template slot-scope="s"
              ><el-button type="text" @click="editCategory(s.row)">编辑</el-button
              ><el-button
                type="text"
                class="danger"
                @click="remove('/admin/categories/' + s.row.id)"
                >删除</el-button
              ></template
            ></el-table-column
          ></el-table
        ></el-tab-pane
      ><el-tab-pane label="公告管理" name="notices"
        ><p class="hint">
          公告关联发布管理员、物品及分类。仅启用且处于有效时间范围的公告会在首页显示。
        </p>
        <el-button
          type="primary"
          size="small"
          @click="
            notice = null;
            noticeVisible = true;
          "
          >发布公告</el-button
        ><el-table :data="notices"
          ><el-table-column prop="title" label="标题" min-width="180" /><el-table-column
            prop="publisher_name"
            label="发布人"
            width="100"
          /><el-table-column prop="item_title" label="关联物品" min-width="150" /><el-table-column
            prop="category_name"
            label="关联分类"
            width="110"
          /><el-table-column prop="published_at" label="发布时间" width="170" /><el-table-column
            prop="starts_at"
            label="生效时间"
            width="170"
          /><el-table-column
            prop="expires_at"
            label="截止时间（空=长期）"
            width="170"
          /><el-table-column prop="updated_at" label="更新时间" width="170" /><el-table-column
            label="状态"
            width="80"
            ><template slot-scope="s">{{
              s.row.enabled ? "启用" : "停用"
            }}</template></el-table-column
          ><el-table-column label="操作" width="120" fixed="right"
            ><template slot-scope="s"
              ><el-button
                type="text"
                @click="
                  notice = s.row;
                  noticeVisible = true;
                "
                >编辑</el-button
              ><el-button type="text" class="danger" @click="remove('/admin/notices/' + s.row.id)"
                >删除</el-button
              ></template
            ></el-table-column
          ></el-table
        ></el-tab-pane
      ><el-tab-pane label="查询统计" name="stats"
        ><h3>物品分类分布</h3>
        <el-table :data="stats.categories || []"
          ><el-table-column prop="name" label="分类" /><el-table-column
            prop="total"
            label="物品数量"
        /></el-table>
        <div class="form-grid">
          <div>
            <h3>类型统计</h3>
            <p v-for="r in stats.types" :key="r.type">
              {{ r.type === "lost" ? "失物" : "招领" }}：{{ r.total }}
            </p>
          </div>
          <div>
            <h3>状态统计</h3>
            <p v-for="r in stats.statuses" :key="r.status">{{ label(r.status) }}：{{ r.total }}</p>
          </div>
        </div></el-tab-pane
      ></el-tabs
    ><notice-editor
      :visible="noticeVisible"
      :notice="notice"
      :categories="allCategories"
      @close="noticeVisible = false"
      @saved="
        noticeVisible = false;
        load();
      "
    /><el-dialog title="分类维护" :visible.sync="categoryVisible" width="440px"
      ><el-form label-position="top"
        ><el-form-item label="分类名称"
          ><el-input v-model="category.name" maxlength="50" /></el-form-item
        ><el-form-item label="排序（较小的靠前）"
          ><el-input-number v-model="category.sort_no" :min="0" :max="9999" /></el-form-item
        ><el-form-item label="启用"><el-switch v-model="category.enabled" /></el-form-item
        ><el-button type="primary" :loading="saving" @click="saveCategory">保存</el-button></el-form
      ></el-dialog
    >
  </section>
</template>
<script>
import api from "../api";
import NoticeEditor from "./NoticeEditor.vue";
export default {
  components: { NoticeEditor },
  data: () => ({
    tab: "claims",
    stats: {},
    claims: [],
    users: [],
    items: [],
    itemKeyword: "",
    itemPage: 1,
    itemTotal: 0,
    allCategories: [],
    notices: [],
    notice: null,
    noticeVisible: false,
    category: {},
    categoryVisible: false,
    saving: false,
  }),
  created() {
    this.load();
  },
  methods: {
    confirmation(s) {
      return (
        {
          pending: "待招领人确认",
          confirmed: "已确认",
          rejected: "招领人已拒绝",
          legacy: "历史申请",
        }[s] || s
      );
    },
    label(s) {
      return (
        {
          open: "进行中",
          finished: "已完结",
          closed: "已关闭",
          pending: "待审核",
          approved: "已通过",
          rejected: "已驳回",
        }[s] || s
      );
    },
    tag(s) {
      return { pending: "warning", approved: "success", rejected: "danger" }[s] || "info";
    },
    async load() {
      try {
        const r = await Promise.all([
          api.get("/admin/stats"),
          api.get("/admin/claims"),
          api.get("/admin/users"),
          api.get("/admin/categories"),
          api.get("/notices", { params: { all: true } }),
        ]);
        [this.stats, this.claims, this.users, this.allCategories, this.notices] = r;
        await this.loadItems();
        this.$emit("changed");
      } catch (e) {
        this.$message.error(e.message);
      }
    },
    async loadItems() {
      try {
        const r = await api.get("/items", {
          params: { keyword: this.itemKeyword, page: this.itemPage, size: 20 },
        });
        this.items = r.rows;
        this.itemTotal = r.total;
      } catch (e) {
        this.$message.error(e.message);
      }
    },
    async review(row, status) {
      try {
        const r = await this.$prompt(
          status === "approved" ? "请填写交接安排或审核说明" : "请填写驳回原因",
          "审核认领申请",
          {
            inputType: "textarea",
            inputValidator: (v) => !!v && v.trim().length > 0 && v.length <= 500,
            inputErrorMessage: "请输入1~500字的审核意见",
            confirmButtonText: "确认审核",
          }
        );
        await api.put("/admin/claims/" + row.id + "/review", { status, reply: r.value });
        this.$message.success("审核完成");
        await this.load();
      } catch (e) {
        if (e instanceof Error) this.$message.error(e.message);
      }
    },
    async toggleUser(row, enabled) {
      try {
        await api.put("/admin/users/" + row.id, { enabled });
        await this.load();
      } catch (e) {
        this.$message.error(e.message);
      }
    },
    async finish(row) {
      await this.itemStatus(row, "finished", "确认已找到或已认领？待审申请将同步驳回。");
    },
    async closeItem(row) {
      await this.itemStatus(row, "closed", "确认关闭该信息？待审申请将同步驳回。");
    },
    async itemStatus(row, status, message) {
      try {
        await this.$confirm(message, "更新物品状态");
        await api.put("/items/" + row.id + "/status", { status });
        await this.load();
      } catch (e) {
        if (e instanceof Error) this.$message.error(e.message);
      }
    },
    async remove(url) {
      try {
        await this.$confirm(
          "确认删除？物品删除会同时删除其认领记录，关联公告将保留并解除物品关联。",
          "删除确认",
          { type: "warning" }
        );
        await api.delete(url);
        this.$message.success("已删除");
        await this.load();
      } catch (e) {
        if (e instanceof Error) this.$message.error(e.message);
      }
    },
    editCategory(row) {
      this.category = Object.assign({ name: "", sort_no: 0, enabled: true }, row || {});
      this.categoryVisible = true;
    },
    async saveCategory() {
      this.saving = true;
      try {
        if (this.category.id) await api.put("/admin/categories/" + this.category.id, this.category);
        else await api.post("/admin/categories", this.category);
        this.categoryVisible = false;
        await this.load();
      } catch (e) {
        this.$message.error(e.message);
      } finally {
        this.saving = false;
      }
    },
  },
};
</script>
