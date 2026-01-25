package com.im.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("im_user")
public class User implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private String username;
    private String password; // Hashed
    private String avatar;
    private Date createTime;
    private Date updateTime;
}
