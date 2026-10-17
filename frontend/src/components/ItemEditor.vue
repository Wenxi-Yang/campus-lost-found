<template>
  <el-dialog
    :title="form.id ? '编辑物品信息' : '发布失物 / 招领'"
    :visible="visible"
    @close="$emit('close')"
    width="620px"
    :close-on-click-modal="false"
    ><el-form label-position="top" @submit.native.prevent="save"
      ><el-form-item label="信息类型"
        ><el-radio-group v-model="form.type"
          ><el-radio-button label="lost">我丢失了物品</el-radio-button
          ><el-radio-button label="found">我拾到了物品</el-radio-button></el-radio-group
        ></el-form-item
      ><el-form-item label="物品标题"
        ><el-input
          v-model="form.title"
          maxlength="100"
          show-word-limit
          placeholder="例如：图书馆拾到一副无线耳机"
      /></el-form-item>
      <div class="form-grid">
        <el-form-item label="物品分类"
          ><el-select v-model="form.category_id" placeholder="选择分类"
            ><el-option
              v-for="c in categories"
              :key="c.id"
              :label="c.name"
              :value="c.id" /></el-select></el-form-item
        ><el-form-item label="丢失 / 拾获时间"
          ><el-date-picker
            v-model="form.occurred_at"
            type="datetime"
            value-format="yyyy-MM-dd HH:mm:ss"
            placeholder="选择实际发生时间"
        /></el-form-item>
      </div>
      <el-form-item label="地点"
        ><el-input
          v-model="form.location"
          maxlength="100"
          placeholder="具体到楼栋、楼层或教室" /></el-form-item
      ><el-form-item label="物品描述"
        ><el-input
          v-model="form.description"
          type="textarea"
          :rows="4"
          maxlength="5000"
          show-word-limit
          placeholder="描述外观和发现经过，保留部分独有特征用于认领核验" /></el-form-item
      ><el-form-item label="联系方式（登录后可见）"
        ><el-input
          v-model="form.contact"
          maxlength="100"
          placeholder="手机号或其他便于联系的方式" /></el-form-item
      ><el-button @click="$emit('close')">取消</el-button
      ><el-button native-type="submit" type="primary" :loading="saving"
        >保存信息</el-button
      ></el-form
    ></el-dialog
  >
</template>
<script>
import api from "../api";
export default {
  props: ["visible", "item", "categories"],
  data: () => ({ form: {}, saving: false }),
  watch: {
    visible(v) {
      if (v)
        this.form = Object.assign(
          {
            type: "lost",
            title: "",
            location: "",
            category_id: null,
            occurred_at: "",
            description: "",
            contact: "",
          },
          this.item || {}
        );
    },
  },
  methods: {
    async save() {
      if (
        !this.form.title ||
        !this.form.category_id ||
        !this.form.occurred_at ||
        !this.form.description ||
        !this.form.contact ||
        !this.form.location
      )
        return this.$message.warning("请完整填写物品信息");
      this.saving = true;
      try {
        if (this.form.id) await api.put("/items/" + this.form.id, this.form);
        else await api.post("/items", this.form);
        this.$message.success("物品信息已保存");
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
