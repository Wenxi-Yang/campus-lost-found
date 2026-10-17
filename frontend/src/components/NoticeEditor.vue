<template>
  <el-dialog
    title="公告管理"
    :visible="visible"
    @close="$emit('close')"
    width="680px"
    :close-on-click-modal="false"
    ><p class="hint">
      系统通知用于服务安排；招领提醒和认领结果须关联物品或分类，帮助同学直接找到相关信息。
    </p>
    <el-form label-position="top" @submit.native.prevent="save"
      ><el-form-item label="标题"><el-input v-model="form.title" maxlength="100" /></el-form-item>
      <div class="form-grid">
        <el-form-item label="公告用途"
          ><el-select v-model="form.kind"
            ><el-option label="系统通知" value="system" /><el-option
              label="招领提醒"
              value="reminder" /><el-option
              label="认领结果"
              value="result" /></el-select></el-form-item
        ><el-form-item label="是否启用"><el-switch v-model="form.enabled" /></el-form-item>
      </div>
      <el-form-item label="公告内容"
        ><el-input v-model="form.content" type="textarea" :rows="4" maxlength="10000"
      /></el-form-item>
      <div class="form-grid">
        <el-form-item label="关联物品（搜索标题，最多显示100项）"
          ><el-select
            v-model="form.item_id"
            filterable
            remote
            clearable
            :remote-method="search"
            placeholder="搜索关联物品"
            ><el-option
              v-for="i in items"
              :key="i.id"
              :label="i.title + ' #' + i.id"
              :value="i.id" /></el-select></el-form-item
        ><el-form-item label="关联分类"
          ><el-select v-model="form.category_id" clearable
            ><el-option
              v-for="c in categories"
              :key="c.id"
              :label="c.name"
              :value="c.id" /></el-select
        ></el-form-item>
      </div>
      <div class="form-grid">
        <el-form-item label="发布时间"
          ><el-date-picker
            v-model="form.published_at"
            type="datetime"
            value-format="yyyy-MM-dd HH:mm:ss" /></el-form-item
        ><el-form-item label="生效时间"
          ><el-date-picker
            v-model="form.starts_at"
            type="datetime"
            value-format="yyyy-MM-dd HH:mm:ss"
        /></el-form-item>
      </div>
      <el-form-item label="截止时间（留空为长期有效）"
        ><el-date-picker
          v-model="form.expires_at"
          type="datetime"
          clearable
          value-format="yyyy-MM-dd HH:mm:ss" /></el-form-item
      ><el-button @click="$emit('close')">取消</el-button
      ><el-button native-type="submit" type="primary" :loading="saving"
        >保存公告</el-button
      ></el-form
    ></el-dialog
  >
</template>
<script>
import api from "../api";
const now = () => {
  const d = new Date();
  return (
    d.getFullYear() +
    "-" +
    String(d.getMonth() + 1).padStart(2, "0") +
    "-" +
    String(d.getDate()).padStart(2, "0") +
    " " +
    String(d.getHours()).padStart(2, "0") +
    ":" +
    String(d.getMinutes()).padStart(2, "0") +
    ":" +
    String(d.getSeconds()).padStart(2, "0")
  );
};
export default {
  props: ["visible", "notice", "categories"],
  data: () => ({ form: {}, items: [], saving: false }),
  watch: {
    async visible(v) {
      if (v) {
        this.form = Object.assign(
          {
            title: "",
            content: "",
            kind: "system",
            enabled: true,
            item_id: null,
            category_id: null,
            published_at: now(),
            starts_at: now(),
            expires_at: null,
          },
          this.notice || {}
        );
        await this.search("");
        if (this.form.item_id && !this.items.some((i) => i.id === this.form.item_id)) {
          try {
            this.items.push(await api.get("/items/" + this.form.item_id));
          } catch (e) {
            this.$message.error(e.message);
          }
        }
      }
    },
  },
  methods: {
    async search(keyword) {
      try {
        this.items = (await api.get("/items", { params: { keyword, size: 100 } })).rows;
      } catch (e) {
        this.$message.error(e.message);
      }
    },
    async save() {
      this.saving = true;
      try {
        if (this.form.id) await api.put("/admin/notices/" + this.form.id, this.form);
        else await api.post("/admin/notices", this.form);
        this.$message.success("公告已保存");
        this.$emit("saved");
      } catch (e) {
        this.$message.error(e.message);
      } finally {
        this.saving = false;
      }
    },
  },
};
</script>
