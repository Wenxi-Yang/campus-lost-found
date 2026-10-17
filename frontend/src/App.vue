<template>
  <div id="campus-app">
    <header class="topbar">
      <router-link class="brand" to="/"
        ><span class="brand-mark"><i class="el-icon-location-outline" /></span
        ><span>校园寻物<small>CAMPUS LOST & FOUND</small></span></router-link
      >
      <nav>
        <router-link to="/" exact>寻物大厅</router-link
        ><router-link to="/notices">公告栏</router-link
        ><router-link v-if="user" to="/mine">个人中心</router-link
        ><router-link v-if="isAdmin" to="/admin">管理后台</router-link>
      </nav>
      <div class="account">
        <template v-if="user"
          ><span class="account-name">{{ user.name }}</span
          ><el-button size="small" @click="logout">退出</el-button></template
        ><template v-else
          ><router-link to="/login">登录</router-link
          ><el-button size="small" type="primary" @click="$router.push('/register')"
            >注册账号</el-button
          ></template
        >
      </div>
    </header>
    <main class="container" v-loading="loading">
      <div v-if="error" class="error-banner">
        {{ error }} <el-button type="text" @click="refresh">重新加载</el-button>
      </div>
      <template v-if="page === 'home'"
        ><section class="hero">
          <div>
            <span class="eyebrow">每一件遗失，都值得被找回</span>
            <h1>让失物回到<br />熟悉的人手里<span class="hero-dot">。</span></h1>
            <p>发布一条线索，留住一份安心。<br />在这里寻找遗失物品，也让拾获的物品找到主人。</p>
            <div class="hero-actions">
              <el-button type="primary" icon="el-icon-edit-outline" @click="publish"
                >发布失物 / 招领</el-button
              ><el-button
                @click="
                  filters.type = 'found';
                  search();
                "
                >看看拾获物品 <i class="el-icon-right"
              /></el-button>
            </div>
          </div>
          <div class="hero-note">
            <span class="note-number">01 / 校园互助</span><i class="el-icon-discover" />
            <h3>线索有去处<br />找回有回音</h3>
            <p>发布 → 查询 → 认领 → 审核完结</p>
          </div>
        </section>
        <section class="stat-strip">
          <div>
            <strong>{{ stats.total || 0 }}</strong
            ><span>条校园物品信息</span>
          </div>
          <div>
            <strong>{{ stats.open || 0 }}</strong
            ><span>件物品正在寻找</span>
          </div>
          <div>
            <strong>{{ stats.finished || 0 }}</strong
            ><span>条信息已完结</span>
          </div>
          <span class="stat-caption"><i class="el-icon-connection" /> 连接善意，让校园更温暖</span>
        </section>
        <div class="home-layout">
          <section>
            <div class="section-heading">
              <div>
                <span class="eyebrow">LOST & FOUND</span>
                <h2>寻物大厅</h2>
              </div>
              <span class="muted">共 {{ total }} 条信息</span>
            </div>
            <div class="panel filters">
              <div class="search-line">
                <el-input
                  v-model="filters.keyword"
                  prefix-icon="el-icon-search"
                  placeholder="搜索名称或描述，多个关键词用空格分隔"
                  clearable
                  @keyup.enter.native="search"
                /><el-button type="primary" @click="search">搜索</el-button>
              </div>
              <div class="filter-line">
                <el-select v-model="filters.type" clearable placeholder="全部类型" @change="search"
                  ><el-option label="失物 · 我在寻找" value="lost" /><el-option
                    label="招领 · 等待主人"
                    value="found" /></el-select
                ><el-select
                  v-model="filters.category_id"
                  clearable
                  placeholder="全部分类"
                  @change="search"
                  ><el-option
                    v-for="c in categories"
                    :key="c.id"
                    :label="c.name"
                    :value="c.id" /></el-select
                ><el-select
                  v-model="filters.status"
                  clearable
                  placeholder="全部状态"
                  @change="search"
                  ><el-option label="进行中" value="open" /><el-option
                    label="已完结"
                    value="finished" /><el-option label="已关闭" value="closed" /></el-select
                ><el-input
                  v-model="filters.location"
                  prefix-icon="el-icon-location-outline"
                  clearable
                  placeholder="地点"
                  @keyup.enter.native="search"
                />
              </div>
            </div>
            <div class="time-filter">
              <el-date-picker
                v-model="filters.occurred_from"
                type="datetime"
                value-format="yyyy-MM-dd HH:mm:ss"
                placeholder="丢失 / 拾获时间：从"
                @change="search"
                clearable
              /><el-date-picker
                v-model="filters.occurred_until"
                type="datetime"
                value-format="yyyy-MM-dd HH:mm:ss"
                placeholder="丢失 / 拾获时间：至"
                @change="search"
                clearable
              />
            </div>
            <p class="hint">默认展示进行中的信息；可切换状态查看完结记录。</p>
            <div class="item-grid">
              <article
                v-for="i in items"
                :key="i.id"
                class="item-card"
                tabindex="0"
                @click="detail(i.id)"
                @keyup.enter="detail(i.id)"
              >
                <div class="card-top">
                  <span :class="['type-chip', i.type]">{{
                    i.type === "lost" ? "失物寻找" : "拾物招领"
                  }}</span
                  ><span class="muted status-dot">{{ label(i.status) }}</span>
                </div>
                <div class="item-category">
                  <i :class="categoryIcon(i.category_name)" /><span>{{ i.category_name }}</span>
                </div>
                <h3>{{ i.title }}</h3>
                <p class="card-description">{{ i.description }}</p>
                <div class="card-meta">
                  <span><i class="el-icon-location-outline" /> {{ i.location }}</span
                  ><span><i class="el-icon-time" /> {{ i.occurred_at }}</span>
                </div>
                <div class="card-bottom">
                  <span>{{ i.publisher_name }} 发布</span
                  ><span>查看详情 <i class="el-icon-top-right" /></span>
                </div>
              </article>
            </div>
            <el-empty
              v-if="!items.length && !loading"
              description="暂无匹配信息，试试其他关键词或发布一条线索"
            /><el-pagination
              background
              layout="prev,pager,next,total"
              :page-size="12"
              :total="total"
              :current-page.sync="currentPage"
              @current-change="loadItems"
            />
          </section>
          <aside>
            <div class="section-heading">
              <h2>校园公告</h2>
              <router-link to="/notices">全部 <i class="el-icon-right" /></router-link>
            </div>
            <div class="panel notice-board">
              <div class="notice-intro">
                <i class="el-icon-bell" />
                <p>服务通知、招领提醒、认领结果<br /><span>关联公告可直接查看物品或分类</span></p>
              </div>
              <article class="notice-mini" v-for="n in notices.slice(0, 4)" :key="n.id">
                <span class="notice-kind">{{ kind(n.kind) }}</span>
                <h3>
                  <button class="text-button" @click="selectedNotice = n">{{ n.title }}</button>
                </h3>
                <time>{{ n.published_at }}</time
                ><el-button v-if="n.item_id" type="text" @click="detail(n.item_id)"
                  >查看关联物品 <i class="el-icon-right" /></el-button
                ><el-button
                  v-else-if="n.category_id"
                  type="text"
                  @click="filterCategory(n.category_id)"
                  >查看相关分类</el-button
                >
              </article>
              <el-empty v-if="!notices.length" description="暂无生效公告" :image-size="60" />
            </div>
            <div class="tips">
              <i class="el-icon-sunny" />
              <h3>认领前，留一点细心</h3>
              <p>核对时间与地点，填写独有特征。交接时当面确认，请妥善保护个人隐私。</p>
            </div>
          </aside>
        </div></template
      >
      <section v-else-if="page === 'notices'">
        <div class="section-heading">
          <div>
            <span class="eyebrow">CAMPUS BULLETIN</span>
            <h1>公告栏</h1>
            <p class="muted">了解服务安排，关注招领提醒与认领结果。</p>
          </div>
        </div>
        <div class="notice-list">
          <article class="panel notice-full" v-for="n in notices" :key="n.id">
            <span class="notice-kind">{{ kind(n.kind) }}</span>
            <h2>{{ n.title }}</h2>
            <p class="preserve">{{ n.content }}</p>
            <div class="notice-times">
              发布人：{{ n.publisher_name }} · 发布时间：{{ n.published_at }}<br />生效：{{
                n.starts_at
              }}
              · 截止：{{ n.expires_at || "长期有效" }} · 更新：{{ n.updated_at }}
            </div>
            <el-button v-if="n.item_id" type="primary" plain @click="detail(n.item_id)"
              >关联物品：{{ n.item_title }}</el-button
            ><el-button v-if="n.category_id" @click="filterCategory(n.category_id)"
              >相关分类：{{ n.category_name }}</el-button
            >
          </article>
          <el-empty v-if="!notices.length" description="暂无生效公告" />
        </div>
      </section>
      <section v-else-if="page === 'login' || page === 'register'" class="auth-layout">
        <div class="auth-copy">
          <span class="eyebrow">在这里，连接每一份善意</span>
          <h1>找回的不只是物品，<br />还有安心。</h1>
          <p>使用学号或手机号登录，发布线索、提交认领，<br />在个人中心随时查看进展。</p>
          <i class="el-icon-key" />
        </div>
        <div class="panel auth-form">
          <span class="eyebrow">{{ page === "login" ? "WELCOME BACK" : "JOIN CAMPUS" }}</span>
          <h2>{{ page === "login" ? "欢迎回来" : "创建学生账号" }}</h2>
          <el-form label-position="top" @submit.native.prevent="submitAuth"
            ><template v-if="page === 'register'"
              ><el-form-item label="学号"
                ><el-input
                  v-model="authForm.student_number"
                  maxlength="30"
                  autocomplete="username" /></el-form-item
              ><el-form-item label="姓名"
                ><el-input v-model="authForm.name" maxlength="50" /></el-form-item
              ><el-form-item label="手机号"
                ><el-input v-model="authForm.phone" maxlength="11" /></el-form-item></template
            ><el-form-item v-else label="学号或手机号"
              ><el-input v-model="authForm.account" autocomplete="username" /></el-form-item
            ><el-form-item label="密码"
              ><el-input
                v-model="authForm.password"
                show-password
                maxlength="64"
                autocomplete="current-password"
                :placeholder="
                  page === 'register' ? '请输入8~64位密码' : '请输入密码'
                " /></el-form-item
            ><el-button type="primary" native-type="submit" :loading="saving" class="full-width">{{
              page === "login" ? "登录" : "注册账号"
            }}</el-button></el-form
          >
          <p class="muted">
            {{ page === "login" ? "还没有账号？" : "已有账号？" }}
            <router-link :to="page === 'login' ? '/register' : '/login'">{{
              page === "login" ? "立即注册" : "去登录"
            }}</router-link>
          </p>
        </div>
      </section>
      <section v-else-if="page === 'mine' && user">
        <div class="section-heading">
          <div>
            <span class="eyebrow">MY CAMPUS</span>
            <h1>{{ user.name }}的个人中心</h1>
            <p class="muted">{{ user.student_number }} · {{ user.phone }}</p>
          </div>
          <el-button type="primary" @click="publish">发布新信息</el-button>
        </div>
        <el-tabs v-model="mineTab" class="panel"
          ><el-tab-pane label="我的发布" name="items"
            ><el-table :data="myItems"
              ><el-table-column prop="title" label="物品" min-width="180" /><el-table-column
                label="状态"
                width="100"
                ><template slot-scope="s">{{ label(s.row.status) }}</template></el-table-column
              ><el-table-column
                prop="occurred_at"
                label="丢失 / 拾获时间"
                width="170"
              /><el-table-column prop="created_at" label="发布时间" width="170" /><el-table-column
                label="操作"
                min-width="250"
                ><template slot-scope="s"
                  ><el-button type="text" @click="detail(s.row.id)">详情</el-button
                  ><el-button v-if="s.row.status === 'open'" type="text" @click="editItem(s.row.id)"
                    >编辑</el-button
                  ><el-button
                    v-if="s.row.status === 'open'"
                    type="text"
                    @click="changeStatus(s.row, 'finished')"
                    >完结</el-button
                  ><el-button
                    v-if="s.row.status !== 'closed'"
                    type="text"
                    @click="changeStatus(s.row, 'closed')"
                    >关闭</el-button
                  ><el-button type="text" class="danger" @click="deleteItem(s.row.id)"
                    >删除</el-button
                  ></template
                ></el-table-column
              ></el-table
            ><el-pagination
              layout="prev,pager,next,total"
              :current-page.sync="minePage"
              :page-size="20"
              :total="mineTotal"
              @current-change="loadMine" /></el-tab-pane
          ><el-tab-pane label="收到的认领申请" name="incoming"
            ><incoming-claims
              :claims="incomingClaims"
              @detail="detail"
              @changed="loadMine" /></el-tab-pane
          ><el-tab-pane label="我的认领申请" name="claims"
            ><p class="hint">
              申请先交给招领人确认，确认后由管理员审核。下方分别显示两步的处理结果。
            </p>
            <el-table :data="myClaims"
              ><el-table-column prop="title" label="物品" min-width="150" /><el-table-column
                prop="description"
                label="特征说明"
                min-width="180"
              /><el-table-column label="审核状态" width="110"
                ><template slot-scope="s"
                  ><el-tag
                    :type="
                      s.row.status === 'approved'
                        ? 'success'
                        : s.row.status === 'pending'
                        ? 'warning'
                        : 'danger'
                    "
                    >{{ label(s.row.status) }}</el-tag
                  ></template
                ></el-table-column
              ><el-table-column label="招领人确认" width="150"
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
              /><el-table-column prop="reply" label="审核意见" min-width="180" /><el-table-column
                prop="created_at"
                label="申请时间"
                width="170"
              /><el-table-column prop="reviewed_at" label="审核时间" width="170" /><el-table-column
                label="操作"
                width="130"
                ><template slot-scope="s"
                  ><el-button type="text" @click="detail(s.row.item_id)">查看</el-button
                  ><el-button
                    v-if="s.row.status === 'pending'"
                    type="text"
                    @click="cancelClaim(s.row.id)"
                    >撤回</el-button
                  ></template
                ></el-table-column
              ></el-table
            ></el-tab-pane
          ></el-tabs
        >
      </section>
      <admin-panel
        v-else-if="page === 'admin' && isAdmin"
        @detail="detail"
        @changed="loadPublic"
      /><el-empty v-else description="请登录后访问此页面"
        ><el-button type="primary" @click="$router.push('/login')">去登录</el-button></el-empty
      >
    </main>
    <footer>
      <span>校园寻物 · 校园失物招领管理系统</span><span>让信息有记录，让寻找有方向</span>
    </footer>
    <item-editor
      :visible="editorVisible"
      :item="editingItem"
      :categories="categories"
      @close="editorVisible = false"
      @saved="
        editorVisible = false;
        refresh();
      "
    /><el-dialog title="物品详情" :visible.sync="detailVisible" width="680px"
      ><template v-if="selectedItem"
        ><div class="detail-heading">
          <span :class="['type-chip', selectedItem.type]">{{
            selectedItem.type === "lost" ? "失物寻找" : "拾物招领"
          }}</span
          ><el-tag>{{ label(selectedItem.status) }}</el-tag>
        </div>
        <h2>{{ selectedItem.title }}</h2>
        <el-descriptions :column="1" border
          ><el-descriptions-item label="分类">{{ selectedItem.category_name }}</el-descriptions-item
          ><el-descriptions-item label="地点">{{ selectedItem.location }}</el-descriptions-item
          ><el-descriptions-item label="丢失 / 拾获时间">{{
            selectedItem.occurred_at
          }}</el-descriptions-item
          ><el-descriptions-item label="发布人">{{
            selectedItem.publisher_name
          }}</el-descriptions-item
          ><el-descriptions-item label="发布时间">{{
            selectedItem.created_at
          }}</el-descriptions-item
          ><el-descriptions-item label="更新时间">{{
            selectedItem.updated_at
          }}</el-descriptions-item
          ><el-descriptions-item v-if="selectedItem.finished_at" label="完结时间">{{
            selectedItem.finished_at
          }}</el-descriptions-item
          ><el-descriptions-item label="联系方式">{{
            selectedItem.contact || "登录后可查看联系方式"
          }}</el-descriptions-item></el-descriptions
        >
        <p class="preserve detail-description">{{ selectedItem.description }}</p>
        <template
          v-if="
            selectedItem.type === 'found' &&
            selectedItem.status === 'open' &&
            (!user || user.id !== selectedItem.user_id)
          "
          ><el-divider>确认是你的物品？</el-divider
          ><el-input
            v-model="claimDescription"
            type="textarea"
            :rows="3"
            maxlength="3000"
            placeholder="请填写10字以上的独有特征、丢失经过等归属证明"
          /><el-button class="claim-button" type="primary" :loading="saving" @click="submitClaim"
            >提交认领申请</el-button
          >
          <p class="hint">招领人确认后由管理员审核，可在个人中心查看进度。</p></template
        ></template
      ></el-dialog
    ><el-dialog
      title="校园公告"
      :visible="!!selectedNotice"
      @close="selectedNotice = null"
      width="640px"
      ><template v-if="selectedNotice"
        ><span class="notice-kind">{{ kind(selectedNotice.kind) }}</span>
        <h2>{{ selectedNotice.title }}</h2>
        <p class="preserve">{{ selectedNotice.content }}</p>
        <p class="notice-times">
          发布：{{ selectedNotice.publisher_name }} · {{ selectedNotice.published_at
          }}<br />生效：{{ selectedNotice.starts_at }}<br />截止：{{
            selectedNotice.expires_at || "长期有效"
          }}<br />更新：{{ selectedNotice.updated_at }}
        </p>
        <el-button
          v-if="selectedNotice.item_id"
          type="primary"
          @click="detail(selectedNotice.item_id)"
          >查看关联物品</el-button
        ><el-button
          v-if="selectedNotice.category_id"
          @click="
            filterCategory(selectedNotice.category_id);
            selectedNotice = null;
          "
          >查看相关分类</el-button
        ></template
      ></el-dialog
    >
  </div>
</template>
<script>
import api from "./api";
import ItemEditor from "./components/ItemEditor.vue";
import AdminPanel from "./components/AdminPanel.vue";
import IncomingClaims from "./components/IncomingClaims.vue";
export default {
  components: { ItemEditor, AdminPanel, IncomingClaims },
  data: () => ({
    user: null,
    loading: false,
    error: "",
    saving: false,
    categories: [],
    stats: {},
    notices: [],
    items: [],
    total: 0,
    currentPage: 1,
    filters: {
      keyword: "",
      type: "",
      category_id: "",
      location: "",
      status: "open",
      occurred_from: "",
      occurred_until: "",
    },
    authForm: { account: "", student_number: "", name: "", phone: "", password: "" },
    editorVisible: false,
    editingItem: null,
    detailVisible: false,
    selectedItem: null,
    claimDescription: "",
    selectedNotice: null,
    mineTab: "items",
    myItems: [],
    myClaims: [],
    incomingClaims: [],
    minePage: 1,
    mineTotal: 0,
  }),
  computed: {
    page() {
      return this.$route.name || "home";
    },
    isAdmin() {
      return this.user && this.user.role === "admin";
    },
  },
  watch: {
    $route() {
      this.authForm.password = "";
      this.routeLoad();
    },
  },
  async created() {
    try {
      this.user = await api.get("/auth/me");
    } catch (e) {
      if (e.status !== 401) this.error = e.message;
    }
    await this.refresh();
  },
  methods: {
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
    confirmation(s) {
      return (
        {
          pending: "待招领人确认",
          confirmed: "招领人已确认",
          rejected: "招领人已拒绝",
          legacy: "历史申请（无确认记录）",
        }[s] || s
      );
    },
    kind(k) {
      return { system: "系统通知", reminder: "招领提醒", result: "认领结果" }[k] || k;
    },
    categoryIcon(n) {
      return /电子/.test(n)
        ? "el-icon-headset"
        : /证件/.test(n)
        ? "el-icon-postcard"
        : /学习/.test(n)
        ? "el-icon-notebook-2"
        : /衣物/.test(n)
        ? "el-icon-shopping-bag-1"
        : "el-icon-box";
    },
    async loadPublic() {
      try {
        [this.categories, this.stats, this.notices] = await Promise.all([
          api.get("/categories"),
          api.get("/stats"),
          api.get("/notices"),
        ]);
      } catch (e) {
        this.error = e.message;
      }
    },
    async refresh() {
      this.loading = true;
      this.error = "";
      try {
        await this.loadPublic();
        await this.routeLoad();
      } finally {
        this.loading = false;
      }
    },
    async routeLoad() {
      try {
        if (this.page === "home") await this.loadItems();
        if (this.page === "notices") this.notices = await api.get("/notices");
        if (this.page === "mine" && this.user) await this.loadMine();
      } catch (e) {
        this.error = e.message;
      }
    },
    async loadItems() {
      try {
        const r = await api.get("/items", {
          params: { ...this.filters, page: this.currentPage, size: 12 },
        });
        this.items = r.rows;
        this.total = r.total;
      } catch (e) {
        this.error = e.message;
      }
    },
    search() {
      this.currentPage = 1;
      this.loadItems();
    },
    async filterCategory(id) {
      this.filters = {
        keyword: "",
        type: "",
        category_id: id,
        location: "",
        status: "open",
        occurred_from: "",
        occurred_until: "",
      };
      this.currentPage = 1;
      if (this.page !== "home") await this.$router.push("/");
      else await this.loadItems();
    },
    async submitAuth() {
      this.saving = true;
      try {
        if (this.page === "register") {
          await api.post("/auth/register", this.authForm);
          this.$message.success("注册成功，请登录");
          this.authForm.account = this.authForm.student_number;
          await this.$router.push("/login");
        } else {
          this.user = await api.post("/auth/login", this.authForm);
          this.$message.success("登录成功");
          await this.$router.push(this.isAdmin ? "/admin" : "/");
          await this.refresh();
        }
      } catch (e) {
        this.$message.error(e.message);
      } finally {
        this.saving = false;
        this.authForm.password = "";
      }
    },
    async logout() {
      try {
        await api.post("/auth/logout");
        this.user = null;
        this.selectedItem = null;
        this.detailVisible = false;
        await this.$router.push("/").catch(() => {});
        await this.refresh();
      } catch (e) {
        this.$message.error(e.message);
      }
    },
    requireLogin() {
      if (this.user) return true;
      this.$message.info("请先登录");
      this.detailVisible = false;
      this.$router.push("/login");
      return false;
    },
    publish() {
      if (this.requireLogin()) {
        this.editingItem = null;
        this.editorVisible = true;
      }
    },
    async editItem(id) {
      try {
        this.editingItem = await api.get("/items/" + id);
        this.editorVisible = true;
      } catch (e) {
        this.$message.error(e.message);
      }
    },
    async detail(id) {
      try {
        this.selectedItem = await api.get("/items/" + id);
        this.claimDescription = "";
        this.detailVisible = true;
      } catch (e) {
        this.$message.error(e.message);
      }
    },
    async submitClaim() {
      if (!this.requireLogin()) return;
      this.saving = true;
      try {
        await api.post("/items/" + this.selectedItem.id + "/claims", {
          description: this.claimDescription,
        });
        this.$message.success("申请已提交，请在个人中心查看审核进度");
        this.detailVisible = false;
        if (this.page === "mine") await this.loadMine();
      } catch (e) {
        this.$message.error(e.message);
      } finally {
        this.saving = false;
      }
    },
    async loadMine() {
      const r = await api.get("/items", { params: { mine: true, page: this.minePage, size: 20 } });
      this.myItems = r.rows;
      this.mineTotal = r.total;
      [this.myClaims, this.incomingClaims] = await Promise.all([
        api.get("/claims/mine"),
        api.get("/claims/incoming"),
      ]);
    },
    async changeStatus(row, status) {
      try {
        await this.$confirm(
          "确认" + (status === "finished" ? "完结" : "关闭") + "这条信息？待审申请将同步驳回。",
          "更新状态"
        );
        await api.put("/items/" + row.id + "/status", { status });
        await this.refresh();
      } catch (e) {
        if (e instanceof Error) this.$message.error(e.message);
      }
    },
    async deleteItem(id) {
      try {
        await this.$confirm(
          "确认删除该物品及其认领记录？关联公告会保留并解除物品关联。",
          "删除确认",
          { type: "warning" }
        );
        await api.delete("/items/" + id);
        await this.refresh();
      } catch (e) {
        if (e instanceof Error) this.$message.error(e.message);
      }
    },
    async cancelClaim(id) {
      try {
        await this.$confirm("确认撤回该认领申请？", "撤回申请");
        await api.delete("/claims/" + id);
        await this.loadMine();
      } catch (e) {
        if (e instanceof Error) this.$message.error(e.message);
      }
    },
  },
};
</script>
