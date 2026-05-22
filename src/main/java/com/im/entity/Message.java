package com.im.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("im_message")
public class Message implements Serializable {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String msgId;
    private String fromUserId;
    private String toUserId; // or GroupId
    private String content;
    private Integer type; // 1: Single, 2: Group
    private Integer msgType; // 1: Text, 2: Image, 3: Audio
    private Integer status; // 0: Sent, 1: Delivered, 2: Read
    private Date createTime;
    
    // Quoting fields
    private String quoteMsgId;
    private String quoteSender;
    private String quoteContent;
}
