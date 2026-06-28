package com.huxi.java.ai.langchain4j.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("doctor")
public class Doctor {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String department;
    private String title;
    private String specialty;
}
