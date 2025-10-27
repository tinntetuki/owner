package com.interview.im.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息DTO
 * 
 * @author interview
 * @since 2024-01-01
 */
@Data
public class MessageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID
     */
    private Long id;

    /**
     * 发送者ID
     */
    private Long senderId;

    /**
     * 接收者ID
     */
    private Long receiverId;

    /**
     * 群组ID（群聊消息）
     */
    private Long groupId;

    /**
     * 消息类型：1-文本，2-图片，3-语音，4-视频，5-文件，6-位置，7-表情
     */
    @NotNull(message = "消息类型不能为空")
    private Integer messageType;

    /**
     * 消息内容
     */
    @NotNull(message = "消息内容不能为空")
    private String content;

    /**
     * 消息状态：0-发送中，1-已发送，2-已送达，3-已读，4-发送失败
     */
    private Integer status;

    /**
     * 是否撤回：0-未撤回，1-已撤回
     */
    private Integer isRecalled;

    /**
     * 撤回时间
     */
    private LocalDateTime recallTime;

    /**
     * 是否置顶：0-未置顶，1-已置顶
     */
    private Integer isPinned;

    /**
     * 是否@所有人：0-否，1-是
     */
    private Integer isAtAll;

    /**
     * @的用户ID列表（JSON格式）
     */
    private String atUserIds;

    /**
     * 回复的消息ID
     */
    private Long replyMessageId;

    /**
     * 扩展信息（JSON格式）
     */
    private String extraInfo;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
