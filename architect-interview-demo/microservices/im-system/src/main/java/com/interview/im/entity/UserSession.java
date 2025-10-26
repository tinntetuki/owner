package com.interview.im.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户会话实体
 * 
 * @author interview
 * @since 2024-01-01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("user_session")
public class UserSession implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 会话类型：1-单聊，2-群聊
     */
    @TableField("session_type")
    private Integer sessionType;

    /**
     * 对方用户ID（单聊）
     */
    @TableField("target_user_id")
    private Long targetUserId;

    /**
     * 群组ID（群聊）
     */
    @TableField("group_id")
    private Long groupId;

    /**
     * 会话名称
     */
    @TableField("session_name")
    private String sessionName;

    /**
     * 会话头像
     */
    @TableField("session_avatar")
    private String sessionAvatar;

    /**
     * 最后一条消息ID
     */
    @TableField("last_message_id")
    private Long lastMessageId;

    /**
     * 最后一条消息内容
     */
    @TableField("last_message_content")
    private String lastMessageContent;

    /**
     * 最后一条消息时间
     */
    @TableField("last_message_time")
    private LocalDateTime lastMessageTime;

    /**
     * 未读消息数量
     */
    @TableField("unread_count")
    private Integer unreadCount;

    /**
     * 是否置顶：0-未置顶，1-已置顶
     */
    @TableField("is_pinned")
    private Integer isPinned;

    /**
     * 是否免打扰：0-否，1-是
     */
    @TableField("is_muted")
    private Integer isMuted;

    /**
     * 是否删除：0-未删除，1-已删除
     */
    @TableField("is_deleted")
    private Integer isDeleted;

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
