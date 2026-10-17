<template>
  <div>
    <p class="hint">
      这里接收你发布的招领物品的申请。核对特征后确认，申请将转交管理员审核；不符合的申请可填写原因后拒绝。
    </p>
    <el-table :data="claims" empty-text="暂未收到认领申请"
      ><el-table-column prop="title" label="物品" min-width="160" /><el-table-column
        prop="applicant_name"
        label="申请人"
        width="110"
      /><el-table-column prop="description" label="特征说明" min-width="230" /><el-table-column
        label="招领人确认"
        width="150"
        ><template slot-scope="s">{{
          confirmation(s.row.publisher_status)
        }}</template></el-table-column
      ><el-table-column prop="publisher_reply" label="确认说明" min-width="180" /><el-table-column
        prop="publisher_confirmed_at"
        label="确认时间"
        width="170"
      /><el-table-column label="申请状态" width="100"
        ><template slot-scope="s">{{ status(s.row.status) }}</template></el-table-column
      ><el-table-column label="操作" width="200" fixed="right"
        ><template slot-scope="s"
          ><el-button type="text" @click="$emit('detail', s.row.item_id)">详情</el-button
          ><template v-if="s.row.status === 'pending' && s.row.publisher_status === 'pending'"
            ><el-button type="text" :disabled="saving" @click="confirm(s.row, 'confirmed')"
              >确认归属</el-button
            ><el-button
              type="text"
              class="danger"
              :disabled="saving"
              @click="confirm(s.row, 'rejected')"
              >拒绝</el-button
            ></template
          ></template
        ></el-table-column
      ></el-table
    >
  </div>
</template>
<script>
import api from "../api";
export default {
  props: ["claims"],
  data: () => ({ saving: false }),
  methods: {
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
    status(s) {
      return { pending: "待处理", approved: "已通过", rejected: "已驳回" }[s] || s;
    },
    async confirm(row, decision) {
      try {
        const r = await this.$prompt(
          decision === "confirmed" ? "请说明核对情况和后续交接安排" : "请说明物品特征不符的原因",
          "招领人确认",
          {
            inputType: "textarea",
            inputValidator: (v) => !!v && v.trim().length > 0 && v.length <= 500,
            inputErrorMessage: "请填写1~500字说明",
            confirmButtonText: decision === "confirmed" ? "确认并转交管理员" : "拒绝申请",
          }
        );
        this.saving = true;
        await api.put("/claims/" + row.id + "/confirm", { decision, reply: r.value });
        this.$message.success(decision === "confirmed" ? "已转交管理员审核" : "已拒绝该申请");
        this.$emit("changed");
      } catch (e) {
        if (e instanceof Error) this.$message.error(e.message);
      } finally {
        this.saving = false;
      }
    },
  },
};
</script>
