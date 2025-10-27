package com.interview.im.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息实体
 * 
 * @author interview
 * @since 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("message")
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 发送者ID
     */
    @TableField("sender_id")
    private Long senderId;

    /**
     * 接收者ID
     */
    @TableField("receiver_id")
    private Long receiverId;

    /**
     * 群组ID（群聊消息）
     */
    @TableField("group_id")
    private Long groupId;

    /**
     * 消息类型：1-文本，2-图片，3-语音，4-视频，5-文件，6-位置，7-表情
     */
    @TableField("message_type")
    private Integer messageType;

    /**
     * 消息内容
     */
    @TableField("content")
    private String content;

    /**
     * 消息状态：0-发送中，1-已发送，2-已送达，3-已读，4-发送失败
     */
    @TableField("status")
    private Integer status;

    /**
     * 是否撤回：0-未撤回，1-已撤回
     */
    @TableField("is_recalled")
    private Integer isRecalled;

    /**
     * 撤回时间
     */
    @TableField("recall_time")
    private LocalDateTime recallTime;

    /**
     * 是否置顶：0-未置顶，1-已置顶
     */
    @TableField("is_pinned")
    private Integer isPinned;

    /**
     * 是否@所有人：0-否，1-是
     */
    @TableField("is_at_all")
    private Integer isAtAll;

    /**
     * @的用户ID列表（JSON格式）
     */
    @TableField("at_user_ids")
    private String atUserIds;

    /**
     * 回复的消息ID
     */
    @TableField("reply_message_id")
    private Long replyMessageId;

    /**
     * 扩展信息（JSON格式）
     */
    @TableField("extra_info")
    private String extraInfo;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 创建人
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 更新人
     */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    /**
     * 逻辑删除：0-未删除，1-已删除
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /**
     * 版本号
     */
    @Version
    @TableField("version")
    private Integer version;
}
